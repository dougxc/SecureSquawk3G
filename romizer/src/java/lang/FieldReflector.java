/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.util.Assert;

/**
 * This class is used to access the values of fields. Currently it only
 * supports reading the values of instance fields as that is all that is
 * required by the romizer.
 *
 * @author  Doug Simon
 */
class FieldReflector {

    private FieldReflector() {}


    /*---------------------------------------------------------------------------*\
     *                           Field value accessors                           *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the JVM reflected field corresponding to a Squawk reflected field.
     *
     * @param   field  the Squawk reflected field to convert
     * @return  the JVM reflected field corresponding to <code>field</code>
     */
    private static java.lang.reflect.Field jvmField(Field field) throws Exception {

//String kname = null;
//String fname = null;

//        try {
            Klass klass = field.getDefiningClass();
            String name = (klass == Klass.CLASS ? "java.lang.Klass" : klass.getName());
//kname = name;
            Class clazz = Class.forName(name);
            name = field.getName();
//fname = name;
            java.lang.reflect.Field jvmField = clazz.getDeclaredField(name);
            if (!jvmField.isAccessible()) {
                jvmField.setAccessible(true);
            }
            return jvmField;
//        } catch (Exception ex) {
//
//System.out.println(ex);
//System.out.println("Klass = "+kname);
//System.out.println("Field = "+fname);
//
//            Assert.shouldNotReachHere();
//            return null;
//        }
    }

    /**
     * Gets the value of an instance <code>byte</code> field.
     *
     * @param   object  the object to extract the <code>byte</code> value from
     * @param   field   the metadata for the field
     * @return  the value of <code>field</code> in <code>object</code>
     */
    public static int getByte(Object object, Field field) {
        try {
            java.lang.reflect.Field jvmField = jvmField(field);
            if (jvmField.getType() == Boolean.TYPE) {
                return (byte)(jvmField.getBoolean(object) ? 1 : 0);
            } else {
                return jvmField.getByte(object);
            }
        } catch (Exception ex) {
            Assert.shouldNotReachHere();
            return 0;
        }
    }

    /**
     * Gets the value of an instance <code>short</code> field.
     *
     * @param   object  the object to extract the <code>short</code> value from
     * @param   field   the metadata for the field
     * @return  the value of <code>field</code> in <code>object</code>
     */
    public static int getShort(Object object, Field field) {
        try {
            return jvmField(field).getShort(object);
        } catch (Exception ex) {
            Assert.shouldNotReachHere();
            return 0;
        }
    }

    /**
     * Gets the value of an instance <code>char</code> field.
     *
     * @param   object  the object to extract the <code>char</code> value from
     * @param   field   the metadata for the field
     * @return  the value of <code>field</code> in <code>object</code>
     */
    public static int getChar(Object object, Field field) {
        try {
            return jvmField(field).getChar(object);
        } catch (Exception ex) {
            Assert.shouldNotReachHere();
            return 0;
        }
    }

    /**
     * Gets the value of an instance <code>int</code> field.
     *
     * @param   object  the object to extract the <code>int</code> value from
     * @param   field   the metadata for the field
     * @return  the value of <code>field</code> in <code>object</code>
     */
    public static int getInt(Object object, Field field) {
        try {
            java.lang.reflect.Field jvmField = jvmField(field);
            if (jvmField.getType() == Float.TYPE) {
                return Float.floatToIntBits(jvmField.getFloat(object));
            } else {
                return jvmField.getInt(object);
            }
        } catch (Exception ex) {
            Assert.shouldNotReachHere();
            return 0;
        }
    }

    /**
     * Gets the value of an instance <code>long</code> field.
     *
     * @param   object  the object to extract the <code>long</code> value from
     * @param   field   the metadata for the field
     * @return  the value of <code>field</code> in <code>object</code>
     */
    public static long getLong(Object object, Field field) {
        try {
            java.lang.reflect.Field jvmField = jvmField(field);
            if (jvmField.getType() == Double.TYPE) {
                return Double.doubleToLongBits(jvmField.getDouble(object));
            } else {
                return jvmField.getLong(object);
            }
        } catch (Exception ex) {
            Assert.shouldNotReachHere();
            return 0;
        }
    }

    /**
     * Gets the value of an instance reference typed field.
     *
     * @param   object  the object to extract the reference typed value from
     * @param   field   the metadata for the field
     * @return  the value of <code>field</code> in <code>object</code>
     */
    public static Object getObject(Object object, Field field) {
        try {
            return jvmField(field).get(object);
        } catch (Exception ex) {
            Assert.shouldNotReachHere();
            return null;
        }
    }

    /**
     * Gets the value of an instance word typed field.
     *
     * @param   object  the object to extract the word typed value from
     * @param   field   the metadata for the field
     * @return  the value of <code>field</code> in <code>object</code>
     */
    public static UWord getUWord(Object object, Field field) {
        try {
            return (UWord)jvmField(field).get(object);
        } catch (Exception ex) {
            Assert.shouldNotReachHere();
            return null;
        }
    }

    /**
     * Gets the constant value of a static final int field.
     *
     * @param   field   the metadata for the field
     * @return  the value
     */
    public static int getConstantInt(Field field) {
        try {
            return jvmField(field).getInt(null);
        } catch (Exception ex) {
            return -1;
        }
    }

    /**
     * Gets the constant value of a static final byte field.
     *
     * @param   field   the metadata for the field
     * @return  the value
     */
    public static byte getConstantByte(Field field) {
        try {
            return jvmField(field).getByte(null);
        } catch (Exception ex) {
            return -1;
        }
    }

    /**
     * Gets the constant value of a static final long field.
     *
     * @param   field   the metadata for the field
     * @return  the value
     */
    public static long getConstantLong(Field field) {
        try {
            return jvmField(field).getLong(null);
        } catch (Exception ex) {
            return -1;
        }
    }

}
