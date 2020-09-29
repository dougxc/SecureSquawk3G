/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

import java.io.*;
import java.util.*;

import com.sun.squawk.io.connections.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.util.*;
import com.sun.squawk.util.Arrays;
import com.sun.squawk.vm.CID;
import com.sun.squawk.util.Vector;    // Version without synchronization

/*if[TRUSTED]*/
import com.sun.squawk.csp.*;
import com.sun.squawk.csp.key.*;
import com.sun.squawk.csp.digest.*;
/*end[TRUSTED]*/

/**
 * The <code>ClassFileLoader</code> class provides the functionality
 * by which class definitions are loaded from class files using a class path.
 *
 * @author  Doug Simon
 * @author  Andrew Crouch
 */
public final class ClassFileLoader implements Context {


    /*---------------------------------------------------------------------------*\
     *                          Constructor and fields                           *
    \*---------------------------------------------------------------------------*/

/*if[TRUSTED]*/

    /*
     * The number of bytes that the UTF8 string "Trusted" occupies within
     * the class file.
     */
    final static int TRUSTED_UTF8_ENTRY_SIZE = (1 + 2 + 7);

    /*
     * The number of bytes from the start of the class file where the
     * constant pool entry count is located.
     */
    final static int CONSTANT_POOL_COUNT_OFFSET = 8;

    /*
     * The number of bytes an attribute_info structure occupies (exluding attribute data).
     *    u2 attribute_name_index;
     *    u4 attribute_length;
     */
    final static int ATTRIBUTE_HEADER_SIZE = (2 + 4);

    /*
     *  The constant pool used to store trusted attributes of the class being loaded.
     */
    private TrustedConstantPool tpool;
/*end[TRUSTED]*/

    /**
     * Creates the class file loader.
     */
    public ClassFileLoader(ClasspathConnection classPath) {
        this.classPath = classPath;
    }

    /**
     * The connection that is used to find the class files.
     */
    private final ClasspathConnection classPath;

    /**
     * The class file being loaded.
     */
    private ClassFile cf;

    /**
     * The class being loaded.
     */
    private Klass klass;

    /**
     * The class file reader.
     */
    private ClassFileReader cfr;

    /**
     * The contant pool of the class being loaded.
     */
    private ConstantPool pool;


/*if[TRUSTED]*/

    /* Provides access to the array of interfaces associated with this klass */
    private Klass[] interfaces;

    /* Provides access to the fields and methods of this klass */
    private ClassFileField[] fieldTable;
    private ClassFileMethod[] methodTable;
    private ClassFileField[][] fieldTables;
    private ClassFileMethod[][] methodTables;


    /* Allow random access to the input stream */
    private IndexedInputStream trustedIS;

    /* Provide  access to this class' super class within the trusted attribute methods */
    private Klass superKlass;
/*end[TRUSTED]*/

    /*---------------------------------------------------------------------------*\
     *                             Loading methods                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the file path for a given class name. The file path is constructed
     * from the given fully qualified name of the class with each (sub)package
     * corresponding to a (sub)directory.
     *
     * @param   name  the class name to process
     * @return  the file path for the class file for the class named by <code>name</code>
     */
    public static String getClassFilePath(String name) {
        String fileName = name.replace('.', '/');

        /*
         * Special transformation of "java/lang/Class" into "java/lang/Klass"
         */
        if (fileName.equals("java/lang/Class")) {
            fileName = "java/lang/Klass";
        }
        return fileName + ".class";
    }

    /**
     * Gets the file path for a given class. The file path is constructed
     * from the fully qualified name of the class with each (sub)package
     * corresponding to a (sub)directory.
     *
     * @param   klass  the class to process
     * @return  the file path for the class file for <code>klass</code>
     */
    public static String getClassFilePath(Klass klass) {
        return getClassFilePath(klass.getInternalName());
    }

/*if[TRUSTED]*/

    private InputStream getTrustedInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
        int b;
        while ((b = is.read()) != -1) {
            baos.write(b);
        }
        byte[] data = baos.toByteArray();
        trustedIS = new IndexedInputStream(data, 0, data.length);
        return trustedIS;
    }
/*end[TRUSTED]*/

    /**
     * Loads the definition of a class from its class file.
     *
     * @param cf  the class file definition to load
     */
    public void load(ClassFile cf) {
        this.cf = cf;
        this.klass = cf.getDefinedClass();
        Assert.that(Klass.State.isBefore(klass.getState(), Klass.State.LOADED));

        String classFilePath = getClassFilePath(klass);
        InputStream is = null;
        try {
            if (classPath == null) {
                throw new IOException("null class path");
            }

            is = classPath.openInputStream(classFilePath);

/*if[TRUSTED]*/
            is = getTrustedInputStream(is);
/*end[TRUSTED]*/

            load(classFilePath, is);
        } catch (IOException ioe) {
//ioe.printStackTrace();
            Translator.throwNoClassDefFoundError(prefix(ioe.toString()));
        } catch (LinkageError le) {
//le.printStackTrace();
            klass.changeState(Klass.State.ERROR);
            throw le;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
//ex.printStackTrace();
                    Assert.shouldNotReachHere();
                }
            }
        }
    }

    /**
     * Loads a class from a class file input stream, filling in the
     * corresponding fields of the <code>ClassFile</code> object.
     *
     * @param   fileName  the file from which the class is being loaded
     * @param   is        the input stream into the class file
     * @throws LinkageException
     */
    private void load(String fileName, InputStream is) {

        /*
         * Write trace message
         */
        if (Klass.DEBUG && Tracer.isTracing("loading", klass.getName())) {
            Tracer.traceln("[loading " +  klass + "]");
        }

        /*
         * Wrap the input stream in a ClassFileReader
         */
        cfr = new ClassFileReader(is, fileName);

        if (klass.getState() == Klass.State.LOADING) {
            Translator.throwClassCircularityError(klass.toString());
        }

        /*
         * Transition the class from "defined" to "loading"
         */
        Assert.that(klass.getState() == Klass.State.DEFINED);
        klass.changeState(Klass.State.LOADING);

        /*
         * Read the magic values
         */
        loadMagicValues();

        /*
         * Read the constant pool
         */
        loadConstantPool();
        cf.setConstantPool(pool);

        /*
         * Read the class information
         */
        Klass superClass = loadClassInfo();

        /*
         * Read the interface definitions
         */
        Klass[] interfaces = loadInterfaces();
/*if[TRUSTED]*/
        this.interfaces = interfaces;
        this.superKlass = superClass;
/*end[TRUSTED]*/
        Assert.that(interfaces != null);

        if (Klass.DEBUG && Tracer.isTracing("classinfo", klass.getName())) {
            Tracer.traceln("class: "+klass.getInternalName());
            //if (klass.getSuperclass() != null) {
            //    Tracer.traceln("  extends: "+klass.getSuperclass().getInternalName());
            //}
            if (superClass != null) {
                Tracer.traceln("  extends: "+superClass.getInternalName());
            }
            for (int i = 0 ; i < interfaces.length ; i++) {
                Tracer.traceln("  implements: "+interfaces[i].getInternalName());
            }
        }

        /*
         * Read the field definitions
         */
        ClassFileField[][] fieldTables = { ClassFileField.NO_FIELDS, ClassFileField.NO_FIELDS };
/*if[TRUSTED]*/
        this.fieldTables = fieldTables;
/*end[TRUSTED]*/
        loadFields(fieldTables);

        /*
         * Read the method definitions
         */
        ClassFileMethod[][] methodTables = { ClassFileMethod.NO_METHODS, ClassFileMethod.NO_METHODS  };
/*if[TRUSTED]*/
        this.methodTables = methodTables;
/*end[TRUSTED]*/
        loadMethods(methodTables);

        /*
         * Read the extra attributes
         */
        String sourceFile = loadExtraAttributes();

        /*
         * Ensure there are no extra bytes
         */
        cfr.readEOF();

        /*
         * Close the input stream
         */
        cfr.close();


        /*
         * Transition the class from "loading" to "loaded"
         */
        Assert.that(klass.getState() == Klass.State.LOADING);

        if (klass.isSquawkNative()) {
            fieldTables[0] = ClassFileField.NO_FIELDS;
            fieldTables[1] = ClassFileField.NO_FIELDS;
        }

/*if[TRUSTED]*/
        /**
         * Since some fields and methods may have been modified, we need to separate
         * them into static and instance.
         */
        this.interfaces = interfaces;
        this.superKlass = superClass;
/*end[TRUSTED]*/



        klass.setClassFileDefinition(
                                      superClass,
                                      interfaces,
                                      methodTables[0],
                                      methodTables[1],
                                      fieldTables[0],
                                      fieldTables[1],
                                      sourceFile
                                    );

        klass.changeState(Klass.State.LOADED);
/*if[TRUSTED]*/
       /**
        * Verify subclass privileges once we have everything loaded.  java.lang.Object is at
        * the top of the hierachy, hence it does not have a superclass.  Privileges are granted
        * to object by the platform owner.
        */
        if (klass != Klass.OBJECT) {
            TrustedKlass.verifySubclassPrivileges(klass);
        }

/*end[TRUSTED]*/



        /*
         * Write trace message
         */
        if (Klass.DEBUG && Tracer.isTracing("loading", klass.getName())) {
            Tracer.traceln("[loaded " +  klass + "]");
        }
    }


    /*---------------------------------------------------------------------------*\
     *                       Class file header loading                           *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class file magic values.
     */
    private void loadMagicValues() {
        int magic = cfr.readInt("magic");
        int minor = cfr.readUnsignedShort("minor");
        int major = cfr.readUnsignedShort("major");
        if (magic != 0xCAFEBABE) {
            throw cfr.formatError("Bad magic value = "+Integer.toHexString(magic));
        }
        /*
         * Support JDK1.3 and 1.4 classfiles
         */
        if (!((major == 45 /*&& minor == 3*/) || (major == 46 && minor == 0) || (major == 47 && minor == 0) || (major == 48 && minor == 0) )) {
            throw cfr.formatError("Bad class file version number: " + major + ":" + minor);
        }
    }


    /*---------------------------------------------------------------------------*\
     *                          Constant pool loading                            *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the constant pool.
     */
    private void loadConstantPool() {
        pool = new ConstantPool(cfr, cf.getDefinedClass());
    }


    /*---------------------------------------------------------------------------*\
     *                   Super class and access flags loading                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class information.
     *
     * @return  the super type of the class being loaded
     */
    private Klass loadClassInfo() {
        int modifiers = cfr.readUnsignedShort("cls-flags");
        int classIndex = cfr.readUnsignedShort("cls-index");
        int superIndex = cfr.readUnsignedShort("cls-super index");

        modifiers = pool.verifyClassModifiers(modifiers);

        /*
         * Loading the constant pool will have created the 'thisClass' object.
         */
        Klass thisClass = pool.getKlass(classIndex);
        if (thisClass != klass) {
            /*
             * VMSpec 5.3.5:
             *
             *   Otherwise, if the purported representation does not actually
             *   represent a class named N, loading throws an instance of
             *   NoClassDefFoundError or an instance of one of its
             *   subclasses.
             */
             Translator.throwNoClassDefFoundError(prefix("'this_class' indicates wrong type"));
        }

        if (klass == null) {
            throw cfr.formatError("invalid 'this_class' item");
        }

        /*
         * Set the modifiers
         */
        modifiers &= Modifier.getJVMClassModifiers();
        modifiers |= klass.getModifiers();
        klass.updateModifiers(modifiers);

        if (superIndex != 0) {
            Klass superClass = pool.getKlass(superIndex);
            Assert.that(superClass != null);

            /*
             * If this is an interface class, its superclass must be
             * java.lang.Object.
             */
            if (klass.isInterface() && superClass != Klass.OBJECT) {
                throw cfr.formatError(
                    "interface class must inherit from java.lang.Object");
            }

            /*
             * Now ensure the super class is resolved
             */
            superClass = pool.getResolvedClass(superIndex, this);

            /*
             * Cannot inherit from an array class.
             */
            if (superClass.isArray()) {
                throw cfr.formatError("cannot inherit from array class");
            }

            /*
             * The superclass cannot be an interface. From the
             * JVM Spec section 5.3.5:
             *
             *   If the class of interface named as the direct
             *   superclass of C is in fact an interface, loading
             *   throws an IncompatibleClassChangeError.
             */
            if (superClass.isInterface()) {
                throw cfr.formatError("cannot extend an interface class");
            }

            /*
             * The superclass cannot be final.
             * Inheriting from a final class is a VerifyError according
             * to J2SE JVM behaviour. There is no explicit
             * documentation in the JVM Spec.
             */
            if (superClass.isFinal()) {
                Translator.throwVerifyError(prefix("cannot extend a final class"));
            }

            return superClass;
        } else if (klass != Klass.OBJECT) {
            throw cfr.formatError("class must have super-type");
        } else {
            return null;
        }
    }


    /*---------------------------------------------------------------------------*\
     *                          Interface loading                                *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class's interfaces.
     *
     * @return  the interfaces implemented by the class being loaded
     */
    private Klass[] loadInterfaces() {
        int count = cfr.readUnsignedShort("i/f-count");
        if (count == 0) {
            return Klass.NO_CLASSES;
        }
        Klass[] interfaces = new Klass[count];
        for (int i = 0; i < count; i++) {
            Klass iface = pool.getResolvedClass(cfr.readUnsignedShort("i/f-index"), null);
            if (!iface.isInterface()) {
                Translator.throwIncompatibleClassChangeError(prefix("cannot implement non-interface class"));
            }
            interfaces[i] = iface;
        }
        return interfaces;
    }


    /*---------------------------------------------------------------------------*\
     *                             Field loading                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class's fields.
     *
     * @param   fieldTables  the 2-element array in which the table of
     *                 instance fields will be returned at index 0 and the
     *                 table of static fields at index 1
     */
    private void loadFields(ClassFileField[][] fieldTables) {

        /*
         * Get count of fields
         */
        final int count = cfr.readUnsignedShort("fld-count");
        if (count == 0) {
            return;
        }

        /*
         * Allocate vector to collect the fields
         */
        Vector instanceFields = new Vector(count);
        Vector staticFields = new Vector(count);
/*if[TRUSTED]*/
        Vector allFields = new Vector(count);
/*end[TRUSTED]*/

        /*
         * Read in all the fields
         */
        for (int i = 0; i < count; i++) {
            ClassFileField field = loadField();

            /*
             * Verify that there are no duplicate fields.
             */
            verifyFieldIsUnique(instanceFields, field);
            verifyFieldIsUnique(staticFields, field);
/*if[TRUSTED]*/
            verifyFieldIsUnique(allFields, field);
/*end[TRUSTED]*/


            /*
             * Add the field to the appropriate collection
             */
            if (field.isStatic()) {
                staticFields.addElement(field);
            } else {
                instanceFields.addElement(field);
            }

/*if[TRUSTED]*/
            allFields.addElement(field);
/*end[TRUSTED]*/


        }

/*if[TRUSTED]*/
        this.fieldTable = getFieldTable(allFields);
/*end[TRUSTED]*/

        /*
         * Partition the fields into the static and instance field tables.
         */
        fieldTables[0] = getFieldTable(instanceFields);
        fieldTables[1] = getFieldTable(staticFields);

        /*
         * Sort the instance fields by size in decreasing order. This ensures
         * that the fields will be aligned according to their data size. It
         * also provides a simple form of object packing
         */
        if (fieldTables[0].length > 1) {
            sortFields(fieldTables[0]);
        }
    }

    /**
     * Copies a vector of <code>ClassFileField</code>s into an array of
     * <code>ClassFileField</code>s.
     *
     * @param   fields  the vector of <code>ClassFileField</code>s to copy
     * @return  an array of <code>ClassFileField</code>s corresponding to the
     *                  contents of <code>fields</code>
     */
    private ClassFileField[] getFieldTable(Vector fields) {
        if (fields.isEmpty()) {
            return ClassFileField.NO_FIELDS;
        } else {
            ClassFileField[] table = new ClassFileField[fields.size()];
            fields.copyInto(table);
            return table;
        }
    }

    /**
     * Verifies that a given field does not match any of the fields in a
     * given collection of fields.
     *
     * @param  fields  the collection of fields to test
     * @param  field   the field to match
     */
    private void verifyFieldIsUnique(Vector fields, ClassFileField field) {
        for (Enumeration e = fields.elements(); e.hasMoreElements(); ) {
            ClassFileField f = (ClassFileField)e.nextElement();
            if (f.getName().equals(field.getName()) && f.getType() == field.getType()) {
                throw cfr.formatError("duplicate field found");
            }
        }
    }

    /**
     * Sorts an array of fields by the data size of their types in
     * descending order.
     *
     * @param fields  the array of fields to sort
     */
    private void sortFields(ClassFileField[] fields) {
        Arrays.sort(fields, new Comparer() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }

                Klass t1 = ((ClassFileField)o1).getType();
                Klass t2 = ((ClassFileField)o2).getType();

                /*
                 * Sort by data size of field's type
                 */
                if (t1.getDataSize() < t2.getDataSize()) {
                    return 1;
                } else if (t1.getDataSize() > t2.getDataSize()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

    /**
     * Loads one of the class's fields.
     *
     * @return  the loaded field
     */
    private ClassFileField loadField() {
        int modifiers = cfr.readUnsignedShort("fld-flags");
        int nameIndex = cfr.readUnsignedShort("fld-nameIndex");
        int descriptorIndex = cfr.readUnsignedShort("fld-descIndex");
        int attributesCount = cfr.readUnsignedShort("fld-AttbCount");
        int constantValueIndex  = 0;

        String fieldName = pool.getUtf8(nameIndex);
        String fieldSig  = pool.getUtf8(descriptorIndex);

        pool.verifyFieldModifiers(modifiers, klass.getModifiers());
        pool.verifyName(fieldName, ConstantPool.ValidNameFormat.FIELD);
        Klass fieldType = pool.verifyFieldType(fieldSig);

        modifiers &= Modifier.getJVMFieldModifiers();

        /*
         * Process the field's attributes
         */
        for (int j = 0; j < attributesCount; j++) {
            int    attributeNameIndex = cfr.readUnsignedShort("fld-att-nameIndex");
            int    attributeLength    = cfr.readInt("fld-att-length");
            String attributeName      = pool.getUtf8(attributeNameIndex);

            if (attributeName.equals("ConstantValue")) {
                if (attributeLength != 2) {
                    throw cfr.formatError("ConstantValue attribute length is not 2");
                }
                if (constantValueIndex != 0) {
                    throw cfr.formatError("duplicate ConstantValue attribute");
                }

                /*
                 * Get the variable initialzation value
                 */
                constantValueIndex = cfr.readUnsignedShort("fld-ConstantValue");
                if (constantValueIndex == 0) {
                    throw cfr.formatError("bad ConstantValue index");
                }

                /*
                 * A field_info structure for a non-static field that has a ConstantValue
                 * attribute must be silently ignored.
                 */
                if ((modifiers & Modifier.STATIC) == 0) {
                    //throw in.classFormatError("ConstantValue attribute for non-static field " + fieldName);
                    constantValueIndex = 0;
                }
            } else {
                while (attributeLength-- > 0) {
                    cfr.readByte(null); // Ignore this attribute
                }
            }
        }

        /*
         * Get the constant value attribute (if any). The value itself is
         * currently discarded as it is expected that the field is either
         * initialized in <clinit> or it is a primitive constant that is
         * inlined everywhere it is accessed. This is not completely correct
         * and will have to be fixed as there is at least one TCK test
         * (i.e. "javasoft.sqe.tests.vm.classfmt.atr.atrcvl004.atrcvl00401m1")
         * that expects some compile-time constant static fields to be
         * initialized from "ConstantValue" attributes as they are
         * subsequently accessed by 'getstatic' (i.e. the access was not
         * inlined) and the class contains no <clinit> method.
         *
         * UPDATE: To pass the above mentioned TCK test, the constant value is
         * now retained for the lifetime of a translation unit.
         */
        Object constantValue = getFieldConstantValue(fieldType, constantValueIndex);

        /*
         * Create the field
         */
        ClassFileField field;
        if (constantValue != null && !(constantValue instanceof String)) {
            modifiers |= Modifier.CONSTANT;
            long value = 0;;
            if (constantValue instanceof Integer) {
                value = ((Integer)constantValue).intValue();
            } else if (constantValue instanceof Long) {
                value = ((Long)constantValue).longValue();
/*if[FLOATS]*/
            } else if (constantValue instanceof Double) {
                value = Double.doubleToLongBits(((Double)constantValue).doubleValue());
            } else if (constantValue instanceof Float) {
                value = Float.floatToIntBits(((Float)constantValue).floatValue());
/*end[FLOATS]*/
            } else {
                Assert.shouldNotReachHere("Unknown constant value type: " + constantValue);
            }

            field = new ClassFileConstantField(fieldName, modifiers, fieldType, value);
        } else {
            field = new ClassFileField(fieldName, modifiers, fieldType);
        }



        /*
         * Tracing
         */
        if (Klass.DEBUG && Tracer.isTracing("classinfo", klass.getName() + "." + fieldName)) {
            String constantStr = constantValue == null ? "" : "  (constantValue=" + constantValue + ")";
            String staticStr = (field.isStatic()) ? "static " : "";
            Tracer.traceln("  field: " + staticStr + fieldType.getName() + " " + fieldName + constantStr);
        }

        return field;
    }

    /**
     * Gets the object corresponding to the ConstantValue attribute for a field
     * if it has one.
     *
     * @param   fieldType the type of the field currently being loaded
     * @param   constantValueIndex the index of the ConstantValue attribute
     * @return  the value of the ConstantValue attribute or null if there is
     *                    no such attribute
     */
    private Object getFieldConstantValue(Klass fieldType, int constantValueIndex) {
        if (constantValueIndex != 0) {
            /*
             * Verify that the initial value is of the right klass for the field
             */
            switch (fieldType.getClassID()) {
                case CID.LONG:    return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_Long);
                case CID.FLOAT:   return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_Float);
                case CID.DOUBLE:  return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_Double);
                case CID.INT:     // fall through ...
                case CID.SHORT:   // fall through ...
                case CID.CHAR:    // fall through ...
                case CID.BYTE:    // fall through ...
                case CID.BOOLEAN: return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_Integer);
                case CID.STRING:  return pool.getEntry(constantValueIndex, ConstantPool.CONSTANT_String);
                default:          throw cfr.formatError("invalid ConstantValue attribute value");
            }
        } else {
            return null;
        }
    }


    /*---------------------------------------------------------------------------*\
     *                              Method loading                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class's methods.
     */
    private void loadMethods(ClassFileMethod[][] methodTables) {
        /*
         * Get count of methods and return if there are none
         */
        int count = cfr.readUnsignedShort("mth-count");
        if (count == 0 && (klass.isInterface() || klass.isAbstract())) {
            return;
        }

        /*
         * Allocate the method vector
         */
        Vector virtualMethods = new Vector(count);
        Vector staticMethods = new Vector(count);

/*if[TRUSTED]*/
        Vector allMethods = new Vector(count);
/*end[TRUSTED]*/


        /*
         * Flags whether or not a constructor was read
         */
        boolean hasConstructor = false;

        /*
         * Read in all the methods
         */
        for (int i = 0; i < count; i++) {
            ClassFileMethod method = loadMethod();
            if (!hasConstructor && Modifier.isConstructor(method.getModifiers())) {
                hasConstructor = true;
            }

            /*
             * Verify that there are no duplicate methods.
             */
            verifyMethodIsUnique(virtualMethods, method);
            verifyMethodIsUnique(staticMethods, method);

/*if[TRUSTED]*/
            verifyMethodIsUnique(allMethods, method);
/*end[TRUSTED]*/


            /*
             * Add the method to the appropriate collection
             */
            if (method.isStatic()) {
                staticMethods.addElement(method);
            } else {
                virtualMethods.addElement(method);
            }

/*if[TRUSTED]*/
            allMethods.addElement(method);
/*end[TRUSTED]*/


            //if (method.isNative() && method.getName().charAt(0) != '<') {
            //    com.sun.squawk.vm.Native.register(method.getFullyQualifiedName(klass));
            //}
        }

        /*
         * Synthesize a default constructor for a class which has no constructors
         */
        if (!hasConstructor && !klass.isAbstract() && !klass.isInterface()) {
            ClassFileMethod method = new ClassFileMethod(
                                                          "<init>",
                                                          Modifier.PUBLIC | Modifier.STATIC | Modifier.CONSTRUCTOR,
                                                          klass,
                                                          Klass.NO_CLASSES
                                                        );
            method.setCode(Code.SYNTHESIZED_DEFAULT_CONSTRUCTOR_CODE);
            staticMethods.addElement(method);
        }

        /*
         * Partition the methods into the static and virtual method tables.
         */
        methodTables[0] = getMethodTable(virtualMethods);
        methodTables[1] = getMethodTable(staticMethods);
        cf.setVirtualMethods(getCodeTable(virtualMethods));
        cf.setStaticMethods(getCodeTable(staticMethods));

/*if[TRUSTED]*/
        this.methodTable = getMethodTable(allMethods);
/*end[TRUSTED]*/

    }

    /**
     * Verifies that a given method does not match any of the methods in a
     * given collection of methods.
     *
     * @param  methods  the collection of methods to test
     * @param  method   the method to match
     */
    private void verifyMethodIsUnique(Vector methods, ClassFileMethod method) {
        for (Enumeration e = methods.elements(); e.hasMoreElements(); ) {
            ClassFileMethod m = (ClassFileMethod)e.nextElement();
            if (m.getName().equals(method.getName()) && Arrays.equals(m.getParameterTypes(), method.getParameterTypes()) && m.getReturnType() == method.getReturnType()) {
                throw cfr.formatError("duplicate method found");
            }
        }
    }

    /**
     * Copies a vector of <code>ClassFileMethod</code>s into an array of
     * <code>ClassFileMethod</code>s.
     *
     * @param   methods  the vector of <code>ClassFileMethod</code>s to copy
     * @return  an array of <code>ClassFileMethod</code>s corresponding to the
     *                   contents of <code>methods</code>
     */
    private ClassFileMethod[] getMethodTable(Vector methods) {
        if (methods.isEmpty()) {
            return ClassFileMethod.NO_METHODS;
        } else {
            ClassFileMethod[] table = new ClassFileMethod[methods.size()];
            methods.copyInto(table);
            return table;
        }
    }

    /**
     * Extracts the bytecode arrays from a vector of
     * <code>ClassFileMethod</code>s and copies them into an array of
     * <code>Code</code>s.
     *
     * @param   methods  the vector of <code>ClassFileMethod</code>s
     * @return  an array of <code>Code</code>s corresponding to the bytecode
     *                   arrays of the contents of <code>methods</code>. The
     *                   entries for abstract or native methods will be null
     */
    private Code[] getCodeTable(Vector methods) {
        if (methods.isEmpty()) {
            return Code.NO_CODE;
        } else {
            Code[] table = new Code[methods.size()];
            int index = 0;
            for (Enumeration e = methods.elements(); e.hasMoreElements(); ) {
                ClassFileMethod method = (ClassFileMethod)e.nextElement();
                if (!method.isAbstract() && !method.isNative()) {
                    byte[] code = method.getCode();
                    Assert.that(code != null);
                    table[index] = new Code(code);
                }
                ++index;
            }
            return table;
        }
    }

    /**
     * Loads one of the class's methods.
     *
     * @return  the loaded method
     */
    private ClassFileMethod loadMethod() {
        int modifiers = cfr.readUnsignedShort("mth-flags");
        int nameIndex = cfr.readUnsignedShort("mth-nameIndex");
        int descriptorIndex = cfr.readUnsignedShort("mth-descIndex");
        int attributesCount = cfr.readUnsignedShort("mth-AttbCount");
        boolean isDefaultInit = false;

        String methodName = pool.getUtf8(nameIndex);
        String methodSig  = pool.getUtf8(descriptorIndex);

        if (methodName.equals("<clinit>")) {
            /*
             * JVM Spec 4.6:
             *
             * Class and interface initialization methods are called
             * implicitly by the Java virtual machine; the value of their
             * access_flags item is ignored exception for the settings of the
             * ACC_STRICT flag.
             */
            modifiers = (modifiers & (Modifier.STRICT)) | Modifier.STATIC | Modifier.CLASSINITIALIZER;
        } else {
            pool.verifyMethodModifiers(modifiers, klass.getModifiers(), methodName.equals("<init>"));
            modifiers &= Modifier.getJVMMethodModifiers();
        }

        pool.verifyName(methodName, ConstantPool.ValidNameFormat.METHOD);
        MethodSignature methodSignature =
            pool.verifyMethodType(methodSig, (methodName.endsWith("init>")));

        /*
         * If this is a constructor, then change its return type to be the parent.
         */
        if (methodName.equals("<init>")) {
            Assert.that(methodSignature.returnType == Klass.VOID);
            methodSignature = methodSignature.modifyReturnType(klass);
            modifiers |= (Modifier.CONSTRUCTOR | Modifier.STATIC);
        }

        /*
         * Create the method structure
         */
        ClassFileMethod method = new ClassFileMethod(
                                                      methodName,
                                                      modifiers,
                                                      methodSignature.returnType,
                                                      methodSignature.parameterTypes
                                                    );

        /*
         * Process the method's attributes
         */
        boolean hasCodeAttribute = false;
        boolean hasExceptionTable = false;
        for (int j = 0; j < attributesCount; j++) {
            int    attributeNameIndex = cfr.readUnsignedShort("mth-att-nameIndex");
            int    attributeLength    = cfr.readInt("mth-att-length");
            String attributeName      = pool.getUtf8(attributeNameIndex);

            if (attributeName.equals("Code")) {
                if (hasCodeAttribute) {
                    throw cfr.formatError("duplicate Code attribute in method");
                }
                hasCodeAttribute = true;
                if (!method.isAbstract() && !method.isNative()) {
                    byte[] code = new byte[attributeLength];
                    cfr.readFully(code, "code");
                    method.setCode(code);
                } else {
                    cfr.skip(attributeLength, attributeName);
                }
            } else {
                if (attributeName.equals("Exceptions")) {
                    if (hasExceptionTable) {
                        throw cfr.formatError("duplicate Exceptions attribute in method");
                    }
                    hasExceptionTable = true;
                }
                while (attributeLength-- > 0) {
                    cfr.readByte(null); // Ignore this attribute
                }
            }
        }

        /*
         * Verify that the methods that require a Code attribute actually
         * have one and vice-versa.
         */
        if ((Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers)) == hasCodeAttribute) {
            if (hasCodeAttribute) {
                throw cfr.formatError("code attribute supplied for native or abstract method");
            } else {
                throw cfr.formatError("missing Code attribute for method");
            }
        }

        if (klass.isSquawkNative()) {
            method = new ClassFileMethod(methodName,
                                         modifiers | Modifier.NATIVE,
                                         methodSignature.returnType,
                                         methodSignature.parameterTypes
                                         );
        }

        /*
         * Tracing
         */
        if (Klass.DEBUG && Tracer.isTracing("classinfo", klass.getName() + "." + methodName)) {
            String staticStr = ((method.isStatic()) ? "static " : "");
            Tracer.traceln("  method: " + staticStr + klass.getName() + "." + methodName);
        }

        return method;
    }


    /*---------------------------------------------------------------------------*\
     *                           Class attribute loading                         *
    \*---------------------------------------------------------------------------*/

    /**
     * Loads the class's other attributes.
     *
     * @return  the value of the "SourceFile" attribute if there is one
     */
    private String loadExtraAttributes() {

/*if[TRUSTED]*/
        int attributeCountOffset = trustedIS.getCurrentIndex();

        if (Klass.DEBUG && Tracer.isTracing("svmload", klass.getName())) {
            Tracer.traceln("  ClassFile offset for attribute count : " + attributeCountOffset);
        }

/*end[TRUSTED]*/

        int attributesCount = cfr.readUnsignedShort("ex-count");
        String sourceFile = null;
        for (int i = 0; i < attributesCount; i++) {
            int attributeNameIndex = cfr.readUnsignedShort("ex-index");
            String attributeName   = pool.getUtf8(attributeNameIndex);
            int attributeLength    = cfr.readInt("ex-length");
            if(attributeName.equals("SourceFile")) {
                int index = cfr.readUnsignedShort("sourcefile-index");
                sourceFile = pool.getUtf8(index);
            }
/*if[TRUSTED]*/
            /* Check for Trusted attribute */
            else if (attributeName.equals("Trusted")) {

                /* The trusted attribute must be last */
                if (i != (attributesCount - 1)) {
                    throw cfr.formatError("The trusted attribute must appear last in attribute table");
                }

                if (Klass.DEBUG && Tracer.isTracing("svmload", klass.getName())) {
                    Tracer.traceln("  Trusted attribute found in: " + klass.getName());
                }

                loadTrustedAttribute(attributeLength, attributeCountOffset);
            }
/*end[TRUSTED]*/
            else {
                while (attributeLength-- > 0) {
                    cfr.readByte(null); // Ignore this attribute
                }
            }
        }
        return sourceFile;
    }


/*if[TRUSTED]*/

    /**
     * Loads the "Trusted" attribute.
     *
     * @param trustedAttributeLength     the length in bytes of the Trusted attribute (as is found in the Attribute header).
     * @param attributeCountOffset       the offset, in bytes, of the short value representing the number of attributes within
     * this class file.
     */
    private void loadTrustedAttribute(int trustedAttributeLength, int attributeCountOffset) {

        /* We are dealing with a trusted class */
        TrustedKlass tklass = (TrustedKlass)klass;

        /*
         *  Read in the constant pool associated with the Trusted Attribute.
         */
        if (Klass.DEBUG && Tracer.isTracing("svmload", klass.getName())) {
            Tracer.traceln("  Loading Trusted Constant Pool for : " + klass.getName());
        }
        tpool = new TrustedConstantPool(cfr, cf.getDefinedClass());

        /*
         * The value of cpExtraEntryOffset is the byte offset from
         * the start of the classfile, pointing to the entry in the constant
         * pool of the utf8 encoded string "Trusted".
         */
        int cpExtraEntryOffset = cfr.readInt("tcp-entry-offset");

        /*
         *  Load the cryptographic service provider associated with this trusted class.
         */
        loadCryptoServiceProvider();

        /*
         *   Load the access modifiers associated with this trusted class
         */
        loadTrustedAccessModifiers();

        /*
         *  Load the subclass and class resource access key for this klass.
         */
        loadTrustedKlassKeys();

        /*
         * At this point, the trusted class has it's keys, standard modifiers and trusted modifiers set,
         * so we can now verify that they have been set properly.  If there is an inconsistency found,
         * a class format error will be thrown
         */
        TrustedKlass.verifyTrustedClassFlags(tklass);

        /*
         *  Load the field accessibility options for this class
         */
        loadFieldAccessibility();

        /*
         *  Load the method accessibility options for this class
         */
        loadMethodAccessibility();

        /*
         *   Determine the number of each type of permit associated with this class.
         */
        int subclassPermitsCount = cfr.readUnsignedShort("sub-perm-count");
        int classResourceAccessPermitCount = cfr.readUnsignedShort("cra-perm-count");
        int refClassResourceAccessPermitsCount = cfr.readUnsignedShort("ref-cra-perm-count");

        /*
         *   Load the permits of the trusted class.
         */
        loadSubclassPermits(subclassPermitsCount);
        loadClassResourceAccessPermits(classResourceAccessPermitCount);
        loadReflectionClassResourceAccessPermits(refClassResourceAccessPermitsCount);

        /*
         * We are now at the point just before the domain keys and permits.
         * We need to reconstruct the contents that were signed to produce all the
         * permits attached to this class.
         *
         * For the subclassing and class resource permits, the content is the
         * original pre-wobulated class file.
         * Size:   ClassFile - ("Trusted" utf8 entry in constant pool + TrustedAttributeSize + Extra attribute info entry)
         * Modifications:   Extra Attribute Entries, Constant Pool Entries
         *
         *
         * For the domain permits, the content is the original pre-wobulated class
         * file appended with the contents of the Trusted attribute without the last
         * 2 items (i.e. the domain items).
         *
         * [ClassFile Struct]
         *    ...
         *    [Constant Pool Entries]
         *    [Constant Pool]
         *        ...
         *        "Trusted"
         *    [End Constant Pool]
         *    ...
         *    [Attribute Entries]
         *    [Attributes]
         *        ...
         *        [Trusted Attribute]
         *           ...
         *           [Domain Entries]
         *           [End Domain Entries]
         *        [End Trusted Attribute]
         *    [End Attributes]
         * [End ClassFile Struct]
         *
         */

        /*
         * Since the classfile has been loaded into a byte array, the classfile size is simply
         * the length of the array
         */
        int classfileSize = trustedIS.getBuffer().length;

        /* The trusted attribute adds this number of bytes to the plain class file */
        int totatTrustAttributeAdditions = trustedAttributeLength + ATTRIBUTE_HEADER_SIZE + TRUSTED_UTF8_ENTRY_SIZE;

        /* The start offset of the trusted attribute (including header) */
        int attributeStartOffset = classfileSize - (trustedAttributeLength + ATTRIBUTE_HEADER_SIZE);

        /*
         * We are currently just before the domains in the input stream. The remaining bytes contribute to the
         * domain entry size.
         */
        int domainEntriesSize = classfileSize - this.trustedIS.getCurrentIndex();

        /* Hence, the permits must add */
        int trustedAttributeSansDomainSize = trustedAttributeLength - domainEntriesSize;

        /* And start at offset */
        int permitStartOffset = classfileSize - trustedAttributeLength;

        /*
         * Byte arrays to store the digests of the plain class file, and that of the plain class file
         * with the trusted attribute sans the domain entries.  This is used to verify any permits the
         * class holds.
         */
        byte[] plainClassfileDigest = null;

        /*
         * The extraClassfileDigest is the hash of the original class file with the Trusted attribute
         * minus the domain entries.  This hash is used to verify the domain signature
         */
        byte[] extraClassfileDigest = null;

        /**
         * The message digest used to calculate the hashes of the class file to verify
         * digital signatures.
         */
        MessageDigest messageDigest = null;
        try {
            messageDigest = CSP.getInstance().getDigest();
        } catch (CSPException ex) {
            cfr.formatError(" could not load message digest algorithm: " + ex.getMessage());
        }

        if (Klass.DEBUG && Tracer.isTracing("svmload", klass.getName())) {
            Tracer.traceln("  Total class file size : \t\t\t\t" + classfileSize);
            Tracer.traceln("  Total size of Trusted attribute : \t\t\t" + totatTrustAttributeAdditions);
            Tracer.traceln("  Total size of Domain part of Trusted Attribute : \t" + domainEntriesSize);
            Tracer.traceln("  Start of trusted attribute offset : \t\t\t" + permitStartOffset);
        }

        if (cpExtraEntryOffset != 0) {

            /* If we deal directly with the buffer, it will not interfere with the current stream position */
            byte[] classfileBuffer = trustedIS.getBuffer();

            /* Adjust values for constant pool and attribute entry counts */
            adjustShortInByteArray(classfileBuffer, CONSTANT_POOL_COUNT_OFFSET, -1);
            adjustShortInByteArray(classfileBuffer, attributeCountOffset, -1);

            /* Digest up to the "Trusted" UTF8 string */
            messageDigest.update(classfileBuffer, 0, cpExtraEntryOffset);

            /*
             * The remaining bytes to digest are the classfile less the trusted attribute additions minus
             * what we have already read
             */
            int remainingBytes = classfileSize - totatTrustAttributeAdditions - cpExtraEntryOffset;

            /* Digest up to the "Trusted" attribute, skipping the UTF8 string */
            messageDigest.update(classfileBuffer, cpExtraEntryOffset + TRUSTED_UTF8_ENTRY_SIZE, remainingBytes);

            /* The contents at this point is the original class file. Compute the hash */
            plainClassfileDigest = messageDigest.digest();

            /* The digest resets at this point and must recalculate hash */
            messageDigest.update(classfileBuffer, 0, cpExtraEntryOffset);
            messageDigest.update(classfileBuffer, (cpExtraEntryOffset + TRUSTED_UTF8_ENTRY_SIZE), remainingBytes);
            messageDigest.update(classfileBuffer, permitStartOffset, trustedAttributeSansDomainSize);

            /* Compute the hash of this extra data including the original class file data */
            extraClassfileDigest = messageDigest.digest();

            /* Return class file to consistent state */
            adjustShortInByteArray(classfileBuffer, CONSTANT_POOL_COUNT_OFFSET, 1);
            adjustShortInByteArray(classfileBuffer, attributeCountOffset, 1);

        } else {

            /* If we deal directly with the buffer, it will not interfere with the current stream position */
            byte[] classfileBuffer = trustedIS.getBuffer();

            /* Adjust values for constant pool and attribute entry counts */
            adjustShortInByteArray(classfileBuffer, attributeCountOffset, -1);

            /* Digest right up to the start of the "Trusted" attribute */
            messageDigest.update(classfileBuffer, 0, attributeStartOffset);
            plainClassfileDigest = messageDigest.digest();

            /* The digest resets at this point and must recalculate hash */
            messageDigest.update(classfileBuffer, 0, attributeStartOffset);
            messageDigest.update(classfileBuffer, permitStartOffset, trustedAttributeSansDomainSize);

            /* Compute the hash of this extra data including the original class file data */
            extraClassfileDigest = messageDigest.digest();

            /* Return class file to consistent state */
            adjustShortInByteArray(classfileBuffer, attributeCountOffset, 1);
        }

        /*
         *  Load the domain entries from the class file, then verify them with supplied
         *  Signatures.
         */
        loadAndVerifyDomainEntries(extraClassfileDigest);

        /*
         * Store the hash of the class file associated with the class being loaded so that
         * the attached permits can be verified.
         */
        tklass.setClassfileHash(plainClassfileDigest);
    }







    /**
     * Loads the subclass and class resource access keys.  Key data is loaded
     * from the input stream, and decoded using the CSP.  Provided the keys appear
     * valid, the keys are added to the TrustedKlass being loaded.
     */
    private void loadTrustedKlassKeys() {

        TrustedKlass tklass = (TrustedKlass)klass;

        /* Load the keys */
        int subclassKey = cfr.readUnsignedShort("sub-key");
        int classResourceAccessKey = cfr.readUnsignedShort("cra-key");

        PublicKey subclassPublicKey = null;
        PublicKey craPublicKey = null;

        if (subclassKey != 0) {
            try {
                subclassPublicKey = CSP.getInstance().decodePublicKey(tpool.getPublicKey(subclassKey));
            } catch (CSPException e) {
                cfr.formatError(" error loading subclass key: " + e.getMessage());
            }
        }

        if (classResourceAccessKey != 0) {
            try {
                craPublicKey = CSP.getInstance().decodePublicKey(tpool.getPublicKey(classResourceAccessKey));
            } catch (CSPException e) {
                cfr.formatError(" error loading class resource access key: " + e.getMessage());
            }
        }

        tklass.setKeys(subclassPublicKey, craPublicKey);
    }



    /**
     * Load the default accessibility (with respect to U-classes) of
     * the methods in this class as well as the table of methods that
     * don't use this default setting.
     */
    private void loadMethodAccessibility() {
        boolean defaultMethodAccessibility = (cfr.readByte("def-meth-acc") != 0);
        int nonDefaultMethodCount = cfr.readUnsignedShort("non-def-meth-count");

        /*
         * Only interested in classes that have package external visibility (i.e.
         * public classes). Also, there can not be non-default settings when there
         * are no methods in the class.
         */
        if (nonDefaultMethodCount != 0 && (!klass.isPublic() || this.methodTable == null || this.methodTable.length == 0)) {
            cfr.formatError(" cannot specify non default methods if none publically available");
        }

        /*
         * Simply return if there are no methods in the class.
         */
        if (this.methodTable == null || this.methodTable.length == 0) {
            return;
        }

        /**
         * Go through each method, and add appropriate flags where necessary
         */
        int nonDefaultMethodIndex = -1;
        for (int index = 0; index != this.methodTable.length; index++) {

            ClassFileMethod method = this.methodTable[index];

            /**
             * Check the ordering of the non-default methods. If they are out
             * of order, then this is a class format error.
             */
            if (nonDefaultMethodCount != 0 && index > nonDefaultMethodIndex) {
                int nextNonDefaultMethodIndex = cfr.readUnsignedShort("non-def-method");

                /*
                 * Table must be sorted!
                 */
                if (nextNonDefaultMethodIndex < nonDefaultMethodIndex) {
                    cfr.formatError(" table of non-default methods must be sorted");
                }
                nonDefaultMethodIndex = nextNonDefaultMethodIndex;
                --nonDefaultMethodCount;
            }

            /*
             * We are only interested in public or protected fields
             */
            if (!Modifier.isPublic(method.getModifiers()) && !Modifier.isProtected(method.getModifiers())) {
                /**
                 * We cannot set a non-default method if it is not publically accessible in some way.
                 */
                if (nonDefaultMethodIndex == index) {
                    cfr.formatError(" a private method cannot be specified as non-default");
                }

                /**
                 * This method is inaccessible from Untrusted classes anyway. By default, the method
                 * will not have the allow untrusted access flag set.
                 */
                continue;
            }

            /**
             * We must rebuild the method, as the Modifier is final
             */
            if (nonDefaultMethodIndex == index) {
               setUntrustedMethodAccess(method, !defaultMethodAccessibility);
            } else {
               setUntrustedMethodAccess(method, defaultMethodAccessibility);
            }
        }

        /*
         * Ensure there are no extra entries in the non-defaults table.
         */
        if (nonDefaultMethodCount == 0) {
            return;
        }

        /**
         * If there is another non default method left, it is a class format error
         */
        cfr.formatError(" a private field cannot be specified as non-default");


    }



    /*
     * Load the default accessibility (with respect to U-classes) of
     * the fields in this class as well as the table of fields that
     * don't use this default setting.  At this point, none of this
     * is checked/validated.
     */
    private void loadFieldAccessibility() {

        boolean defaultFieldAccessibility = (cfr.readByte("def-field-acc") != 0);
        int nonDefaultFieldsCount = cfr.readUnsignedShort("non-def-field-count");

        /*
         * Only interested in classes that have package external visibility (i.e.
         * public classes). Also, there can not be non-default settings when there
         * are no fields in the class.
         */
        if (nonDefaultFieldsCount != 0 && (!klass.isPublic() || this.fieldTable == null || this.fieldTable.length == 0)) {
            cfr.formatError(" cannot specify non default fields if none publically available");
        }

        /*
         * Simply return if there are no fields in the class.
         */
        if (this.fieldTable == null || this.fieldTable.length == 0) {
            return;
        }

        /**
         * Go through each field, and add appropriate flags where necessary
         */
        int nonDefaultFieldIndex = -1;
        for (int index = 0; index != this.fieldTable.length; index++) {

            ClassFileField field = this.fieldTable[index];

            /**
             * Check the ordering of the non-default fields. If they are out
             * of order, then this is a class format error.
             */
            if (nonDefaultFieldsCount != 0 && index > nonDefaultFieldIndex) {
                int nextNonDefaultFieldIndex = cfr.readUnsignedShort("non-def-field");

                /*
                 * Table must be sorted!
                 */
                if (nextNonDefaultFieldIndex < nonDefaultFieldIndex) {
                    cfr.formatError(" table of non-default fields must be sorted");
                }
                nonDefaultFieldIndex = nextNonDefaultFieldIndex;
                --nonDefaultFieldsCount;
            }

            /*
             * We are only interested in public or protected fields
             */
            if (!Modifier.isPublic(field.getModifiers()) && !Modifier.isProtected(field.getModifiers())) {
                /**
                 * We cannot set a non-default field if it is not publically accessible in some way.
                 */
                if (nonDefaultFieldIndex == index) {
                    cfr.formatError(" a private field cannot be specified as non-default");
                }

                /**
                 * This field is inaccessible from Untrusted classes anyway. By default, the field
                 * will not have the allow untrusted access flag set.
                 */
                continue;
            }

            /**
             * We must rebuild the field, as the Modifier is final
             */
            if (nonDefaultFieldIndex == index) {
                setUntrustedFieldAccess(field, !defaultFieldAccessibility);
            } else {
                setUntrustedFieldAccess(field, defaultFieldAccessibility);
            }
        }

        /*
         * Ensure there are no extra entries in the non-defaults table.
         */
        if (nonDefaultFieldsCount == 0) {
            return;
        }

        /**
         * If there is another field left, it is a class format error
         */
        cfr.formatError(" a private field cannot be specified as non-default");
    }

    /**
     * If the allowUntrusted flag is set to true, a new <code>ClassFileField</code> is constructed based
     * on the supplied fieldToModify with the <code>Modifier</code> including the
     * TACC_ALLOW_UNTRUSTED_FIELD_METHOD flag set.
     *
     * @param fieldToModify      the basis on the new ClassFileField
     * @param allowUntrusted     whether the TACC_ALLOW_UNTRUSTED_FIELD_METHOD flag should be set
     * @return the ClassFileField with appropriate <code>Modifier</code> flags set
     */
    private void setUntrustedFieldAccess(ClassFileField fieldToModify, boolean allowUntrusted) {

        /**
         * If we don't need to set the flag to allow untrusted access, simply return original
         */
        if (!allowUntrusted) {
            return;
        }

        /**
         * Construct new modifier
         */
        int newModifier = fieldToModify.getModifiers() | Modifier.TACC_ALLOW_UNTRUSTED_FIELD_METHOD;

        /**
         * Create new field with appropriate flag
         */
        ClassFileField newField = new ClassFileField(fieldToModify.getName(), newModifier, fieldToModify.getType());

        /**
         * Update the global field table
         */
        ClassFileField[] searchArray;
        if(fieldToModify.isStatic()) {
            searchArray = this.fieldTables[0];
        } else {
            searchArray = this.fieldTables[1];
        }

        /**
         * We want to replace the previous entry before this field is applied to the klass definition
         */
        for(int i = 0; i < searchArray.length; i++) {
            if(searchArray[i] == fieldToModify) {
                searchArray[i] = newField;
                break;
            }
        }
    }


    /**
     * If the allowUntrusted flag is set to true, a new <code>ClassFileMethod</code> is constructed based
     * on the supplied methodToModify with the <code>Modifier</code> including the
     * TACC_ALLOW_UNTRUSTED_FIELD_METHOD flag set.
     *
     * @param methodToModify      the basis on the new ClassFileField
     * @param allowUntrusted      whether the TACC_ALLOW_UNTRUSTED_FIELD_METHOD flag should be set
     * @return the ClassFileMethod with appropriate <code>Modifier</code> flags set
     */
    private void setUntrustedMethodAccess(ClassFileMethod methodToModify, boolean allowUntrusted) {

        /**
         * If we don't need to set the flag to allow untrusted access, simply return original
         */
        if (!allowUntrusted) {
            return;
        }

        /**
         * Construct new modifier
         */
        int newModifier = methodToModify.getModifiers() | Modifier.TACC_ALLOW_UNTRUSTED_FIELD_METHOD;

        /**
         * Create new method with appropriate flag
         */
        ClassFileMethod newMethod = new ClassFileMethod(methodToModify.getName(), newModifier,
            methodToModify.getReturnType(), methodToModify.getParameterTypes());

        /**
         * Add any code associated with the method.
         */
        if(!methodToModify.isAbstract() && !methodToModify.isNative()) {
            newMethod.setCode(methodToModify.getCode());
        }

        /**
         * Update the global field table
         */
        ClassFileMethod[] searchArray;
        if(methodToModify.isStatic()) {
            searchArray = this.methodTables[0];
        } else {
            searchArray = this.methodTables[1];
        }

        /**
         * We want to replace the previous entry before this method is applied to the klass definition
         */
        for(int i = 0; i < searchArray.length; i++) {
            if(searchArray[i] == methodToModify) {
                searchArray[i] = newMethod;
                break;
            }
        }
    }


    /**
     * Load and verify the domain entries contained within the class file of the
     * class that is currently being loaded.
     *
     * @param extraClassfileDigest    the raw digest of the class file of the class
     * being loaded including Trusted attribute.
     */
    private void loadAndVerifyDomainEntries(byte[] extraClassfileDigest) {

        TrustedKlass tklass = (TrustedKlass)klass;

        /* Load the number of domains */
        int domainsCount = cfr.readUnsignedShort("domains-count");

        Domain[] domains = new Domain[domainsCount];

        /* Read in each domain */
        for(int i = 0; i != domainsCount; i++) {

            /* Read key */
            int keyLength = cfr.readUnsignedShort("dom-key-len");
            byte[] keyBytes = new byte[keyLength];
            cfr.readFully(keyBytes, "dom-key");

            PublicKey key = null;
            try {
                key = CSP.getInstance().decodePublicKey(keyBytes);
            } catch (CSPException e) {
                cfr.formatError(" error loading domain key: " + e.getMessage());
            }

            /* Read Signature */
            int signatureLength = cfr.readUnsignedShort("dom-sig-len");
            byte[] signatureBytes = new byte[signatureLength];
            cfr.readFully(signatureBytes, "dom-signature");

            try {

                if (!CSP.getInstance().verifyHash(extraClassfileDigest, signatureBytes, key)) {
                    cfr.formatError("Domain signature is invalid!");
                }

                if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                    Tracer.traceln(" Domain signature verified!");
                }

            } catch (CSPException e) {
                cfr.formatError("Domain signature invalid: " + e.getMessage());
            }

            /**
             * If all looks well, we can create the new domain object. The signature
             * is nulled because we have verified it, and it is now no longer needed.
             */
            domains[i] = new Domain(key, null);
        }

        tklass.addDomains(domains);
    }



    /**
     * Load the reflection class resource access permits granted to the class being loaded.
     *
     * @param refClassResourceAccessPermitsCount   the number of permits to load
     */
    private void loadReflectionClassResourceAccessPermits(int refClassResourceAccessPermitsCount) {

        Permit[] refClassResourceAccessPermits = null;
        if (refClassResourceAccessPermitsCount > 0) {
            refClassResourceAccessPermits = new Permit[refClassResourceAccessPermitsCount];
            for (int i = 0; i < refClassResourceAccessPermitsCount; i++) {
                int classIndex = cfr.readUnsignedShort("class-index");
                int permitIndex = cfr.readUnsignedShort("permit-index");
                refClassResourceAccessPermits[i] = new Permit(pool.getKlass(classIndex), tpool.getDigitalSignature(permitIndex));
            }

            /* Add subclass permits to trusted class */
            ((TrustedKlass)klass).addRefCRAPermits(refClassResourceAccessPermits);
        }
    }


    /**
     * Load the class resource access permits granted to the class being loaded.
     *
     * @param classResourceAccessPermitCount   the number of permits to load
     */
    private void loadClassResourceAccessPermits(int classResourceAccessPermitCount) {
        /* Load the class resource access permits */
        Permit[] classResourceAccessPermits = null;
        if (classResourceAccessPermitCount > 0) {
            classResourceAccessPermits = new Permit[classResourceAccessPermitCount];
            for (int i = 0; i < classResourceAccessPermitCount; i++) {
                int classIndex = cfr.readUnsignedShort("class-index");
                int permitIndex = cfr.readUnsignedShort("permit-index");
                classResourceAccessPermits[i] = new Permit(pool.getKlass(classIndex), tpool.getDigitalSignature(permitIndex));
            }
            /* Add subclass permits to trusted class */
            ((TrustedKlass)klass).addCRAPermits(classResourceAccessPermits);
        }
    }


    /**
     * Load the trusted modifiers associated with this class.  Perform basic check to ensure
     * modifiers appear valid.
     */
    private void loadTrustedAccessModifiers() {
        /*
         *  Read from class file the TrustedKlass access modifiers.
         */
        int accessFlags = cfr.readUnsignedShort("acc-flags");

        /* The accessFlags are converted to their representation in Modifier */
        int newAccessFlags = Modifier.convertToStandardModifier(accessFlags);

        /* Apply attributes */
        klass.updateModifiers(newAccessFlags);
    }

    /**
     * Load the subclass permits.
     *
     * Each entry in subclass_permits describes the privilege(s) granted to this
     * class to extend its superclass and implement any trusted interfaces.
     *
     * The subclass permit (if any) must be first.
     *
     * The interface implementation permits must have the same order as the ClassFile.interfaces table and
     * follows the subclass permit.
     *
     * java.lang.Object is assumed to have no permits, hence it's super class will never be resolved.
     *
     * @param subclassPermitsCount int   the number of subclass permits in the class file
     */
    private void loadSubclassPermits(int subclassPermitsCount) {

        Permit[] subclassPermits = null;
        if (subclassPermitsCount > 0) {
            subclassPermits = new Permit[subclassPermitsCount];

            int interfaceCount = (this.interfaces == null) ? 0 : this.interfaces.length;
            for (int i = 0; i < subclassPermitsCount; i++) {

                /*
                 * The value of interfaceIndex is the length of the ClassFile.interfaces
                 * table for the subclass permit (and should be the first entry in the subclass permit table).
                 * For an interface implementation permit, it must correspond to that interface's
                 * index in the ClassFile.interfaces table.
                 */
                int interfaceIndex = cfr.readUnsignedShort("int-index");

                /*
                 * The index into the trusted constant pool refering to a digital signature.
                 */
                int permitIndex = cfr.readUnsignedShort("permit-index");

                boolean isSubclassPermit = (i == 0 && interfaceIndex == interfaceCount);

                /*
                 * If this is not the subclass permit, then this class must have
                 * one or more interfaces and the interfaceIndex must be a
                 * valid index into the ifaceTable of the class.
                 */
                if (!isSubclassPermit) {
                    if (interfaceCount == 0 || interfaceIndex >= interfaceCount) {
                        cfr.formatError(" subclass permit interface reference out of range (index=" + interfaceIndex + ", interfacelength=" + interfaceCount);
                    }
                }

                if (isSubclassPermit) {
                    /* Subclass permit */
                    subclassPermits[i] = new Permit(this.superKlass, tpool.getDigitalSignature(permitIndex));

                } else {
                    if (this.interfaces[interfaceIndex].isArray()) {
                        cfr.formatError(" subclass permit cannot reference array type class");
                    }

                    /* Permit is for implementing interface */
                    subclassPermits[i] = new Permit(this.interfaces[interfaceIndex], tpool.getDigitalSignature(permitIndex));
                }
            }

            /* Add subclass permits to trusted class */
            ((TrustedKlass)klass).addSubclassPermits(subclassPermits);
        }
    }

    /**
     * Ensures that the Cryptographic Service Provider (CSP) of the class being loaded is
     * consistent with the CSP currently used for the rest of the system.
     * @see {@link CSP}
     */
    private void loadCryptoServiceProvider() {

        /* Load the cryptographic service provides index within the trusted constant pool */
        int cspIdentifierEntryOffset = cfr.readUnsignedShort("csp-ident");
        String cspIdentifier = tpool.getUtf8(cspIdentifierEntryOffset);

        /* Checks that each class is implementing the correct CSP */
        if (!CSP.getInstance().getIdentifier().equals(cspIdentifier)) {
            cfr.formatError("Unsupported cryptographic service provider : " + cspIdentifier + " for " + klass.getName());
        }
    }


    /**
     * Modifies the value of a <code>short</code> contained in a <code>byte[]</code> by a given value.
     *
     * @param array        the byte array containing the value to be modified
     * @param index        the start index of the short to be modified
     * @param delta        the value which should be added to the short
     */
    private void adjustShortInByteArray(byte[] array, int index, int delta) {
        int val = (((array[index] & 0xFF) << 8) | (array[index + 1] & 0xFF));
        val += delta;
        array[index] = (byte)((val >> 8) & 0xFF);
        array[index + 1] = (byte)(val & 0xFF);

        if (Klass.DEBUG && Tracer.isTracing("svmload", klass.getName())) {
            Tracer.traceln("  Modified short value at index " + index + " from " + (val - delta) + " to " + val);
        }
    }



    /**
     * Used to format a byte array for when tracing is used.  Returns a string
     * whos output is the hex representation of the supplied byte array with
     * <code>entriesPerLine</code> bytes per line.
     *
     * @param b                byte array to format
     * @param entriesPerLine   number of bytes to output per line
     * @return String          the formatted hex representation of the byte array
     */
    private String dumpArrayData(byte[] b, int entriesPerLine) {

        if (b == null) {
            return "";
        }

        String returnString = "";
        for (int i = 0; i < b.length; i++) {
            if (i % entriesPerLine == 0) {
                returnString += "\n";
            }

            String s = Integer.toHexString(b[i] & 0xFF).toUpperCase();
            String t = s;
            if (s.length() == 1) {
                t = "0" + s;
            }
            returnString += t + " ";
        }
        returnString += "\n";

        return returnString;
    }


/*end[TRUSTED]*/

    /*---------------------------------------------------------------------------*\
     *                           Context interface                               *
    \*---------------------------------------------------------------------------*/

    /**
     * {@inheritDoc}
     */
    public String prefix(String msg) {
        return klass.getName() + ": " + msg;
    }
}
