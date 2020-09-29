/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator;

import java.io.*;
import java.util.*;
import javax.microedition.io.Connector;

import com.sun.squawk.util.Assert;
import com.sun.squawk.io.connections.*;
import com.sun.squawk.translator.ci.*;
import com.sun.squawk.util.Hashtable; // Version without synchronization

/*if[TRANSLATOR.TCKERRORLOGGER]*/
import com.sun.squawk.translator.util.*;
/*end[TRANSLATOR.TCKERRORLOGGER]*/

/*if[J2ME.DEBUG]*/
import com.sun.squawk.util.ComputationTimer;
/*end[J2ME.DEBUG]*/


/**
 * The Translator class presents functionality for loading and linking
 * classes from class files (possibly bundled in jar files) into a
 * {@link Suite}.<p>
 *
 * The translator is designed to be run in an {@link java.lang.Isolate Isolate}.
 *
 * @author  Doug Simon
 */
public final class Translator implements TranslatorInterface {


    /*---------------------------------------------------------------------------*\
     *                   Translator reference for open connection                *
    \*---------------------------------------------------------------------------*/

    /**
     * The translator instance being used for a currently open translator
     * connection. This will be null when there is no open translator
     * connection.
     */
    private static Translator instance;

    /**
     * Gets the translator instance being used for the currently open translator
     * connection. This method will fail if there is no currently open
     * connection.
     *
     * @return  the open translator
     */
    public static Translator instance() {
        Assert.that(instance != null);
        return instance;
    }


    /*---------------------------------------------------------------------------*\
     *                     Implementation of TranslatorInterface                 *
    \*---------------------------------------------------------------------------*/

    /**
     * The suite context for the currently open translator.
     */
    private Suite suite;

    /**
     * The loader used to locate and load class files.
     */
    private ClasspathConnection classPath;

    /**
     * {@inheritDoc}
     */
    public void open(Suite suite) {
        Assert.that(instance == null, "translator already open");
        Assert.that(SuiteManager.getSuite(suite.getSuiteNumber()) == suite, "suite is not installed");
        instance = this;
        this.suite = suite;
        this.classFiles = new Hashtable();
        try {
            String url = "classpath://" +  suite.getClassPath();
            this.classPath = (ClasspathConnection)Connector.open(url);
        } catch (IOException ioe) {
            throwLinkageError("Error while setting class path from '"+ suite.getClassPath() + "': " + ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Klass findClass(String name, int classID, boolean enforceValidName) {
        if (enforceValidName && (name.indexOf('/') != -1 || !ConstantPool.isLegalName(name.replace('.', '/'), ConstantPool.ValidNameFormat.CLASS))) {
            return null;
        }
        return Translator.getClass(name, classID);
    }

    /**
     * {@inheritDoc}
     */
    public void load(Klass klass) {
        Assert.that(VM.isHosted() || VM.getCurrentIsolate().getOpenSuite() == suite);
        int state = klass.getState();
        if (Klass.State.isBefore(state, Klass.State.LOADED)) {
            if (klass.isArray()) {
                load(klass.getComponentType());
            } else {
                ClassFile classFile = getClassFile(klass);
                load(classFile);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void convert(Klass klass) {
        int state = klass.getState();
        if (Klass.State.isBefore(state, Klass.State.CONVERTED)) {
            if (klass.isArray()) {
                convert(Klass.OBJECT);
                klass.changeState(Klass.State.CONVERTED);
            } else {
                ClassFile classFile = getClassFile(klass);
                classFile.convert();
                classFiles.remove(klass.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        computeClosure();
        suite      = null;
        instance   = null;
        classPath  = null;
        classFiles = null;
    }


    /*---------------------------------------------------------------------------*\
     *                    Class lookup, creation and interning                   *
    \*---------------------------------------------------------------------------*/

    /**
     * The table of class files for classes.
     */
    private Hashtable classFiles;

    /**
     * Gets the array dimensionality indicated by a given class name.
     *
     * @return  the number of leading '['s in <code>name</code>
     */
    public static int countArrayDimensions(String name) {
        int dimensions = 0;
        while (name.charAt(dimensions) == '[') {
            dimensions++;
        }
        return dimensions;
    }

    /**
     * Gets the class file corresponding to a given instance class. The
     * <code>klass</code> must not yet be converted and it must not be a
     * {@link Klass#isSynthetic() synthetic} class.
     *
     * @param   klass  the instance class for which a class file is requested
     * @return  the class file for <code>klass</code>
     */
    static ClassFile getClassFile(Klass klass) {
        Hashtable classFiles = instance().classFiles;
        Assert.that(!klass.isSynthetic(), "synthethic class has no classfile");
        String name = klass.getName();
        ClassFile classFile = (ClassFile)classFiles.get(name);
        if (classFile == null) {
            classFile = new ClassFile(klass);
            classFiles.put(name, classFile);
        }
        return classFile;
    }

    /**
     * Gets a class corresponding to a given name. If the class does not
     * exist, it will be created and installed in the current suite. If the
     * class represents an array, then this method also ensures that the
     * class representing the component type of the array also exists.<p>
     *
     * The class will not be created if it already exists in the current
     * suite or any of the suites the current suite binds to.
     *
     * @param   name       the name of the class to get
     * @param   classID    the unique identifier to use when creating the
     *                     class or -1 if the next available unique identifier
     *                     is to be used
     */
    private static Klass getClass(String name, int classID) {

        /*
         * Special substitution: java.lang.Klass -> java.lang.Class
         */
        if (name.endsWith("java.lang.Klass")) {
            if (name.charAt(0) == '[') {
                name = name.substring(0, (name.length()-15)) + "java.lang.Class";
            } else {
                name = "java.lang.Class";
            }
        }

        /*
         * Look up current suite first
         */
        Suite suite = instance().suite;
        Klass klass = suite.lookup(name);
        if (klass == null) {
            /*
             * Now have to create the class
             */
            int sno = suite.getSuiteNumber();
            if (name.charAt(0) == '[') {
                String componentName = name.substring(1);
                Klass componentType = getClass(componentName, -1);
                if (classID == -1) {
                    classID = Klass.toClassID(sno, suite.getNextAvailableClassNumber());
                }

                klass = new Klass(name, componentType, classID);
            } else {
                if (classID == -1) {
                    classID = Klass.toClassID(sno, suite.getNextAvailableClassNumber());
                }

/*if[TRUSTED]*/
                klass = new TrustedKlass(name, null, classID);
/*else[TRUSTED]*/
//                klass = new Klass(name, null, classID);
/*end[TRUSTED]*/
            }
            suite.installClass(klass);
        }
        return klass;
    }

    /**
     * Gets a class corresponding to a given name. If the class does not
     * exist, it will be created and installed in the current suite. If the
     * class represents an array, then this method also ensures that the
     * class representing the component type of the array also exists.<p>
     *
     * The class will not be created if it already exists in the current
     * suite or any of the suites the current suite binds to.<p>
     *
     * If the value of <code>isFieldDescriptor</code> is true, then the format
     * of <code>name</code> is as specified in the JVM specification for
     * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#1169">field descriptors</a>.
     * Otherwise, the name is in Squawk {@link Klass#getInternalName() internal}
     * format.<p>
     *
     * @param   name               the name of the class to get
     * @param   isFieldDescriptor  if true, then <code>name</code> is in the format described above
     */
    public static Klass getClass(String name, boolean isFieldDescriptor) {

        /*
         * Convert a valid field descriptor into a class name in internal format
         */
        if (isFieldDescriptor) {
            int dimensions = countArrayDimensions(name);
            char first = name.charAt(dimensions);
            if (first != 'L') {
                Assert.that((name.length() - dimensions) == 1, "illegal field descriptor");
                Klass klass;
                switch (first) {
                    case 'I': klass = Klass.INT;     break;
                    case 'J': klass = Klass.LONG;    break;
                    case 'F': klass = Klass.FLOAT;   break;
                    case 'D': klass = Klass.DOUBLE;  break;
                    case 'Z': klass = Klass.BOOLEAN; break;
                    case 'C': klass = Klass.CHAR;    break;
                    case 'S': klass = Klass.SHORT;   break;
                    case 'B': klass = Klass.BYTE;    break;
                    case 'V': klass = Klass.VOID;    break;
                    default:
                        Assert.shouldNotReachHere();
                        return null;
                }
                if (dimensions != 0) {
                    return getClass(name.substring(0, dimensions) + klass.getInternalName(), -1);
                } else {
                    return klass;
                }
            } else {
                Assert.that(name.charAt(name.length()-1) == ';', "illegal field descriptor");

                /*
                 * Strip the 'L' and ';'
                 */
                String baseName = name.substring(dimensions + 1, name.length() - 1);

                /*
                 * Convert from JVM internal form to Squawk internal form
                 */
                baseName = baseName.replace('/', '.');

                if (dimensions != 0) {
                    name = name.substring(0, dimensions) + baseName;
                } else {
                    name = baseName;
                }
                return getClass(name, -1);
            }
        } else {
            return getClass(name, -1);
        }
    }


    /*---------------------------------------------------------------------------*\
     *                     Class loading and resolution                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads a class's defintion from a class file.
     *
     * @param  classFile  the class file definition to load
     */
    private static void load(final ClassFile classFile) {
/*if[J2ME.DEBUG]*/
        if (ComputationTimer.enabled) {
            ComputationTimer.time("loading", new ComputationTimer.Computation() {
                public Object run() {
                    new ClassFileLoader(instance().classPath).load(classFile);
                    return null;
                }
            });
            return;
        }
/*end[J2ME.DEBUG]*/
        new ClassFileLoader(instance().classPath).load(classFile);
    }

    /**
     * Load and converts the closure of classes in the current suite.
     */
    public void computeClosure() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int cno = 0 ; cno < suite.getClassCount() ; cno++) {
                Klass klass = suite.getKlass(cno);
                try {
                    if (Klass.State.isBefore(klass.getState(), Klass.State.LOADED)) {
                        load(klass);
                        changed = true;
                    }
                    if (Klass.State.isBefore(klass.getState(), Klass.State.CONVERTED)) {
                        convert(klass);
                        changed = true;
                    }
                } catch (LinkageError ex) {
                    if (!klass.isArray()) {
                        klass.changeState(Klass.State.ERROR);
                    }
/*if[TRANSLATOR.TCKERRORLOGGER]*/
                    if (TCKERRORLOGGER && TCKErrorLogger.isLogging()) {
                        TCKErrorLogger.log(klass, ex);
                    } else {
                        throw ex;
                    }
/*else[TRANSLATOR.TCKERRORLOGGER]*/
//                  throw ex;
/*end[TRANSLATOR.TCKERRORLOGGER]*/

                }
            }
        }
    }


    /*---------------------------------------------------------------------------*\
     *                           Reversable parameters                           *
    \*---------------------------------------------------------------------------*/

    public static final boolean REVERSE_PARAMETERS = /*VAL*/true/*REVERSE_PARAMETERS*/;

    /*---------------------------------------------------------------------------*\
     *                          Throwing LinkageErrors                           *
    \*---------------------------------------------------------------------------*/

    /*
     * Using the factory methods below means that the choice between using
     * more or less specific LinkageError classes can be contained here.
     */

    /**
     * Throws an error indicating that there was a general loading problem.
     *
     * @param  msg  the detailed error message
     */
    public static void throwLinkageError(String msg) {
        throw new LinkageError(msg);
    }

    /**
     * Throws an error representing a format error in a class file.
     *
     * @param   msg  the detailed error message
     */
    public static void throwClassFormatError(String msg) {
//        throw new ClassFormatError(msg);
        throw new LinkageError("ClassFormatError: " + msg);
    }

    /**
     * Throws an error indicating a class circularity error.
     *
     * @param  msg  the detailed error message
     */
    public static void throwClassCircularityError(String msg) {
//        throw new ClassCircularityError(msg);
        throw new LinkageError("ClassCircularityError: " + msg);
    }

    /**
     * Throws an error indicating a class file with an unsupported class
     * was found.
     *
     * @param  msg  the detailed error message
     */
    public static void throwUnsupportedClassVersionError(String msg) {
//        throw new UnsupportedClassVersionError(msg);
        throw new LinkageError("UnsupportedClassVersionError: " + msg);
    }

    /**
     * Throws an error indicating an incompatible class change has occurred
     * to some class definition.
     *
     * @param  msg  the detailed error message
     */
    public static void throwIncompatibleClassChangeError(String msg) {
//        throw new IncompatibleClassChangeError(msg);
        throw new LinkageError("IncompatibleClassChangeError: " + msg);
    }

    /**
     * Throws an error indicating a non-abstract class does not implement
     * all its inherited abstract methods.
     *
     * @param  msg  the detailed error message
     */
    public static void throwAbstractMethodError(String msg) {
//        throw new AbstractMethodError(msg);
        throw new LinkageError("AbstractMethodError: " + msg);
    }

    /**
     * Throws an error indicating a class attempts to access or modify a field,
     * or to call a method that it does not have access.
     *
     * @param  msg  the detailed error message
     */
    public static void throwIllegalAccessError(String msg) {
//        throw new IllegalAccessError(msg);
        throw new LinkageError("IllegalAccessError: " + msg);
    }

    /**
     * Throws an error indicating an application tries to access or modify a
     * specified field of an object, and that object no longer has that field.
     *
     * @param  msg  the detailed error message
     */
    public static void throwNoSuchFieldError(String msg) {
//        throw new NoSuchFieldError(msg);
        throw new LinkageError("NoSuchFieldError: " + msg);
    }

    /**
     * Throws an error indicating an application tries to call a specified
     * method of a class (either static or instance), and that class no
     * longer has a definition of that method.
     *
     * @param  msg  the detailed error message
     */
    public static void throwNoSuchMethodError(String msg) {
//        throw new NoSuchMethodError(msg);
        throw new LinkageError("NoSuchMethodError: " + msg);
    }

    /**
     * Throws an error indicating the translator tried to load in the
     * definition of a class and no definition of the class could be found.
     *
     * @param  msg  the detailed error message
     */
    public static void throwNoClassDefFoundError(String msg) {
        throw new NoClassDefFoundError(msg);
    }

    /**
     * Throws an error indicating that the verifier has detected that a class
     * file, though well formed, contains some sort of internal inconsistency
     * or security problem.
     *
     * @param  msg  the detailed error message
     */
    public static void throwVerifyError(String msg) {
//        throw new VerifyError(msg);
        throw new LinkageError("VerifyError: " + msg);
    }


    /*---------------------------------------------------------------------------*\
     *                      Conditional compilation flags                        *
    \*---------------------------------------------------------------------------*/

    /**
     * A flag that controls conditional use of the TCKErrorLogger class.
     */
    public static final boolean TCKERRORLOGGER = /*VAL*/false/*TRANSLATOR.TCKERRORLOGGER*/;
}
