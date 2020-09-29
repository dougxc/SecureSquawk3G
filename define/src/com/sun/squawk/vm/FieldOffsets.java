/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

/**
 * This class contains the offsets and types for fields that must be directly accessed
 * by the VM or other Squawk tools such as the mapper. The romizer ensures that these
 * offsets and types are correct when it creates the image for the bootstrap suite. Each
 * offset is in terms of the data size of the type of the field. For example, the offset
 * to a field of type 'short' is in terms of 16-bits.
 * <p>
 * A simple version of the type is also encoded into the high 32 bits of each entry
 * all of which are longs. The types are restricted to byte, char, short, int, long,
 * float, double, and Object and are encoded using the corresponding CID.XXXXX value
 * places in the high 32 bits of the long.
 * <p>
 * This unfortunate encoding is needed if we want to keep the type and offset in one
 * place because these values are needed in parts of the VM that need to be executed
 * before normal object construction can take place. Two routines are provided to decode
 * the offset and type of a field, and the offset can also be obtained by casting the
 * field into an int.
 * <p>
 * The name of the constant must be composed of the name of the class that defines the
 * field (with '.'s replaced by '_'s) and the name of the field with a '$' separating them.
 */
public class FieldOffsets {

    private final static int  CIDSHIFT = 32;
    private final static long OOP   = ((long)CID.OBJECT) << CIDSHIFT;
    private final static long INT   = ((long)CID.INT)    << CIDSHIFT;
    private final static long SHORT = ((long)CID.SHORT)  << CIDSHIFT;

    /**
     * The offset of the 'self' field in in java.lang.Klass and the 'klass' field java.lang.ObjectAssociation
     * which must be identical.
     */
    public final static long java_lang_Klass$self = 0 + OOP;
    public final static long java_lang_ObjectAssociation$klass = java_lang_Klass$self;

    /**
     * The offset of the 'virtualMethods' field in java.lang.Klass and java.lang.ObjectAssociation.
     * which must be identical.
     */
    public final static long java_lang_Klass$virtualMethods = 1 + OOP;
    public final static long java_lang_ObjectAssociation$virtualMethods = java_lang_Klass$virtualMethods;

    /**
     * The offset of the 'staticMethods' field in java.lang.Klass.
     */
    public final static long java_lang_Klass$staticMethods = 2 + OOP;

    /**
     * The offset of the 'name' field in java.lang.Klass.
     */
    public final static long java_lang_Klass$name = 3 + OOP;

    /**
     * The offset of the 'componentType' field in java.lang.Klass.
     */
    public final static long java_lang_Klass$componentType = 4 + OOP;

    /**
     * The offset of the 'superType' field in java.lang.Klass.
     */
    public final static long java_lang_Klass$superType = 5 + OOP;

    /**
     * The offset of the 'objects' field in java.lang.Klass.
     */
    public final static long java_lang_Klass$objects = 8 + OOP;

    /**
     * The offset of the 'classID' field in java.lang.Klass.
     */
    public final static long java_lang_Klass$classID = (/*VAL*/false/*SQUAWK_64*/ ? 22 : 11) + INT;

    /**
     * The offset of the 'modifiers' field in java.lang.Klass.
     */
    public final static long java_lang_Klass$modifiers = (/*VAL*/false/*SQUAWK_64*/ ? 23 : 12) + INT;

    /**
     * The offset of the 'instanceSize' field in java.lang.Klass.
     */
    public final static long java_lang_Klass$instanceSize = (/*VAL*/false/*SQUAWK_64*/ ? 50 : 28) + SHORT;

    /**
     * The offset of the 'entryTable' field in com.sun.squawk.util.Hashtable.
     */
    public final static long com_sun_squawk_util_Hashtable$entryTable = 0 + OOP;

    /**
     * The offset of the 'isolate' field in java.lang.Thread.
     */
    public final static long java_lang_Thread$isolate = (/*VAL*/false/*SQUAWK_64*/ ? 0 : 2) + OOP;

    /**
     * The offset of the 'isolate' field in java.lang.Thread.
     */
    public final static long java_lang_Thread$stack = (/*VAL*/false/*SQUAWK_64*/ ? 1 : 3) + OOP;

    /**
     * Get the class ID of the field.
     *
     * @param field the field constant
     * @return the class ID
     */
    public static int getClassID(long field) {
        return (int)(field >> CIDSHIFT);
    }

    /**
     * Get the offset of the field.
     *
     * @param field the field constant
     * @return the offset
     */
    public static int getOffset(long field) {
        return (int)field;
    }
}

