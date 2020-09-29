/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator;

import java.util.Enumeration;
import com.sun.squawk.util.*;
import com.sun.squawk.translator.ci.*;

/**
 * This represents a class that has not yet been loaded and linked.
 *
 * @author  Doug Simon
 */
public final class ClassFile {


    /*---------------------------------------------------------------------------*\
     *               Global constants for zero length arrays.                    *
    \*---------------------------------------------------------------------------*/

    /**
     * A zero length array of methods.
     */
    public static final Code[] NO_METHODS = {};

    /**
     * A zero length array of Objects.
     */
    public static final Object[] NO_OBJECTS = {};

    /**
     * A zero length array of Squawk bytecode methods.
     */
    public static final byte[][] NO_SUITE_METHODS = {};


    /*---------------------------------------------------------------------------*\
     *                           Fields of ClassFile                             *
    \*---------------------------------------------------------------------------*/

    /**
     * The class defined by this class file.
     */
    private final Klass definedClass;

    /**
     * The code for the virtual methods defined in this class file. The
     * elements corresponding to abstract and native methods will be null.
     */
    private Code[] virtualMethods;

    /**
     * The code for the static methods defined in this class file.
     */
    private Code[] staticMethods;

    /**
     * The constant pool of this class file.
     */
    private ConstantPool constantPool;


    /*---------------------------------------------------------------------------*\
     *                               Constructor                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Creates a new <code>ClassFile</code> instance.
     *
     * @param   klass      the class defined by this class file
     */
    public ClassFile(Klass klass) {
        this.definedClass = klass;
        this.staticMethods  = NO_METHODS;
        this.virtualMethods = NO_METHODS;
    }


    /*---------------------------------------------------------------------------*\
     *                                Setters                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Sets the constant pool for this class.
     *
     * @param constantPool  the constant pool for this class
     */
    public void setConstantPool(ConstantPool constantPool) {
        Assert.that(this.constantPool == null || this.constantPool == constantPool, "cannot reset the constant pool");
        this.constantPool = constantPool;
    }

    /**
     * Sets the virtual methods for this class.
     *
     * @param  methods  the virtual methods declared by this class
     */
    public void setVirtualMethods(Code[] methods) {
        Assert.that(this.virtualMethods == NO_METHODS, "cannot reset the virtual methods");
        this.virtualMethods = methods;
    }

    /**
     * Sets the static methods for this class.
     *
     * @param  methods  the static methods declared by this class
     */
    public void setStaticMethods(Code[] methods) {
        Assert.that(this.staticMethods == NO_METHODS, "cannot reset the static methods");
        this.staticMethods = methods;
    }


    /*---------------------------------------------------------------------------*\
     *                                Getters                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the class defined by this class file.
     *
     * @return  the class defined by this class file
     */
    public Klass getDefinedClass() {
        return definedClass;
    }

    /**
     * Gets the constant pool of this class.
     *
     * @return the constant pool of this class
     */
    public ConstantPool getConstantPool() {
        return constantPool;
    }


    /*---------------------------------------------------------------------------*\
     *                    Table of constant and class references                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Hashtable of constant objects.
     */
    private ArrayHashtable objectTable = new ArrayHashtable();

    /**
     * Index to the next available object table entry.
     */
    private int nextIndex;

    /**
     * Add an object to the object table.
     *
     * @param object the object to add
     */
    public void addConstantObject(Object object) {
        ObjectCounter counter = (ObjectCounter)objectTable.get(object);
        if (counter == null) {
            counter = new ObjectCounter(object, nextIndex++);
            objectTable.put(object, counter);
        } else {
            counter.inc();
        }
    }

    /**
     * Sort the object table according to the access count.
     */
    private void sortObjectTable() {
        ObjectCounter[] list = new ObjectCounter[objectTable.size()];
        Enumeration e = objectTable.elements();
        for (int i = 0 ; i < list.length ; i++) {
            list[i] = (ObjectCounter)e.nextElement();
        }
        Arrays.sort(list, new Comparer() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                ObjectCounter t1 = (ObjectCounter)o1;
                ObjectCounter t2 = (ObjectCounter)o2;
                if (t1.getCounter() < t2.getCounter()) {
                    return 1;
                } else if (t1.getCounter() > t2.getCounter()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        for (int i = 0 ; i < list.length ; i++) {
            list[i].setIndex(i);
        }
    }

    /**
     * Get the index of an object in the object table.
     *
     * @param object the object to index
     * @return the index
     * @throws NoSuchElementException if the object table does not contain <code>object</code>
     */
    public int getConstantObjectIndex(Object object) {
        ObjectCounter counter = (ObjectCounter)objectTable.get(object);
        if (counter == null) {
            throw new java.util.NoSuchElementException();
        }
        return counter.getIndex();
    }

    /**
     * Get the object array.
     *
     * @return the object array
     */
    private Object[] getConstantObjectArray() {
        Object[] list = new Object[objectTable.size()];
        Enumeration e = objectTable.elements();
        for (int i = 0 ; i < list.length ; i++) {
            list[i] = e.nextElement();
        }
        Arrays.sort(list, new Comparer() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                ObjectCounter t1 = (ObjectCounter)o1;
                ObjectCounter t2 = (ObjectCounter)o2;
                if (t1.getIndex() < t2.getIndex()) {
                    return -1;
                } else if (t1.getIndex() > t2.getIndex()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
//System.err.println(""+definedClass+" list = "+list.length);
        for (int i = 0 ; i < list.length ; i++) {
//System.err.println("    "+list[i]);
            list[i] = ((ObjectCounter)list[i]).getObject();
        }
        return list;
    }


    /*---------------------------------------------------------------------------*\
     *                       Class loading and converting                        *
    \*---------------------------------------------------------------------------*/

    /**
     * Converts a set of methods from their Java bytecode form to their
     * Squawk bytecode form.
     *
     * @param isStatic specifies static or virtual methods
     * @param phase the convertion phase number to perform (1 or 2) or 0 for both
     */
    private void convertMethods(boolean isStatic, int phase) {
        Code[] methodsCode = isStatic ? staticMethods : virtualMethods;
        for (int i = 0 ; i < methodsCode.length ; i++) {
            Method method = definedClass.getMethod(i, isStatic);
            Code code = methodsCode[i];
            if (!method.isAbstract() && !method.isNative()) {
                Assert.that(code != null);
                if (phase == 0 || phase == 1) {
                    code.convert(method, method.getOffset(), 1);
                }
                if (phase == 0 || phase == 2) {
                    code.convert(method, method.getOffset(), 2);
                    methodsCode[i] = null; // Allow GC
                }
//if (phase == 0) {
//    VM.println("Finished converting " + method);
//    System.gc();
//}
            }
        }
    }

    /**
     * Converts all the methods from their Java bytecode form to their
     * Squawk bytecode form.
     */
    private void convertMethods() {

        /*
         * If the translator is running in the romizer then there is
         * sufficient memory to build object tables so that more frequently
         * accessed objects come first. This requires having the IR
         * for all the methods in the class loaded at the same time.
         */
        if (VM.isHosted()) {
            convertMethods(true, 1);
            convertMethods(false, 1);
            sortObjectTable();
            convertMethods(true, 2);
            convertMethods(false, 2);
        } else {
            convertMethods(true, 0);
            convertMethods(false, 0);
        }
    }

    /**
     * Converts all the methods of this class from their Java bytecode form
     * to their Squawk bytecode form.
     */
    void convert() {
        int state = definedClass.getState();
        Assert.that(state == Klass.State.LOADED, "class must be loaded before conversion");
        Assert.that(!definedClass.isSynthetic(), "synthetic classes should not require conversion");
        Assert.that(!definedClass.isPrimitive(), "primitive types should not require conversion");

        /*
         * Convert this type's super class first
         */
        Klass superClass = definedClass.getSuperclass();
        if (superClass != null) {
            Translator.instance().convert(superClass);
        }

        /*
         * Write trace message
         */
        if (Klass.DEBUG && Tracer.isTracing("converting", definedClass.getName())) {
            Tracer.traceln("[converting " + definedClass + "]");
        }

        /*
         * Do the conversion
         */
        try {
            convertMethods();
        } catch (LinkageError e) {
            definedClass.changeState(Klass.State.ERROR);
            throw e;
        }
        definedClass.setObjectTable(getConstantObjectArray());
        definedClass.changeState(Klass.State.CONVERTED);

        /*
         * Write trace message
         */
        if (Klass.DEBUG && Tracer.isTracing("converting", definedClass.getName())) {
            Tracer.traceln("[converted " + definedClass + "]");
        }
    }

}


/**
 * Class used to keep track of the number of times a constant object is referenced in a class.
 */
class ObjectCounter {

    /**
     * The object being counter.
     */
    Object object;

    /**
     * The index of the object in the object table.
     */
    int index;

    /**
     * Use counter.
     */
    int counter;

    /**
     * Constructor.
     *
     * @param index the initial index
     */
    ObjectCounter(Object object, int index) {
        this.object = object;
        this.index  = index;
    }

    /**
     * Get the object being counted.
     *
     * @return the object
     */
    Object getObject() {
        return object;
    }

    /**
     * Get the index.
     *
     * @return the index
     */
    int getIndex() {
        return index;
    }

    /**
     * Set the index.
     *
     * @param index the index
     */
    void setIndex(int index) {
        this.index = index;
    }

    /**
     * Add 1 to the counter.
     */
    void inc() {
        counter++;
    }

    /**
     * Get the counter value.
     *
     * @return the value
     */
    int getCounter() {
        return counter;
    }

    /**
     * Gets a String representation.
     *
     * @return the string
     */
    public final String toString() {
        return "index = "+index+" counter = "+counter+" object = "+object;
    }
}
