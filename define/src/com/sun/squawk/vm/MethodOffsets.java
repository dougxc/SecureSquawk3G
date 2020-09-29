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
 * This class contains the offsets for methods that must be directly accessed by the
 * VM or other Squawk tools such as the mapper. The romizer ensures that these offsets
 * are correct when it creates the image for the bootstrap suite. The offset for a method
 * is its index in the relevant table of methods.
 *
 * The name of the constant must be composed of the name of the class that defines the
 * method (with '.'s replaced by '_'s) and the name of the method with a '$' separating them.
 * Virtual methods must be prefixed with "virtual$" and to disambiguate overloaded methods,
 * the parameter types can be appended to the identifier, each prefixed with a '$'. E.g.:
 *
 *   Method                                |  Constant identifier
 *  ---------------------------------------+-------------------------------------------------
 *   Klass.getInternalName()               | virtual$java_lang_Klass$getInternalName
 *   static Klass.getInternalName(Klass)   | java_lang_Klass$getInternalName
 *   static Klass.isOop(Klass, int)        | java_lang_Klass$isOop$java_lang_Klass$int
 *   static Klass.isOop(Klass, int, char)  | java_lang_Klass$isOop$java_lang_Klass$int$char
 */
public class MethodOffsets {
    public final static int java_lang_VM$do_startup                         = 1;
    public final static int java_lang_VM$do_undefinedNativeMethod           = 2;
    public final static int java_lang_VM$do_callRun                         = 3;
    public final static int java_lang_VM$do_getStaticOop                    = 4;
    public final static int java_lang_VM$do_getStaticInt                    = 5;
    public final static int java_lang_VM$do_getStaticLong                   = 6;
    public final static int java_lang_VM$do_putStaticOop                    = 7;
    public final static int java_lang_VM$do_putStaticInt                    = 8;
    public final static int java_lang_VM$do_putStaticLong                   = 9;
    public final static int java_lang_VM$do_yield                           = 10;
    public final static int java_lang_VM$do_nullPointerException            = 11;
    public final static int java_lang_VM$do_arrayIndexOutOfBoundsException  = 12;
    public final static int java_lang_VM$do_arithmeticException             = 13;
    public final static int java_lang_VM$do_arrayOopStore                   = 14;
    public final static int java_lang_VM$do_findSlot                        = 15;
    public final static int java_lang_VM$do_monitorenter                    = 16;
    public final static int java_lang_VM$do_monitorexit                     = 17;
    public final static int java_lang_VM$do_instanceof                      = 18;
    public final static int java_lang_VM$do_checkcast                       = 19;
    public final static int java_lang_VM$do_lookup_b                        = 20;
    public final static int java_lang_VM$do_lookup_s                        = 21;
    public final static int java_lang_VM$do_lookup_i                        = 22;
    public final static int java_lang_VM$do_class_clinit                    = 23;
    public final static int java_lang_VM$do_new                             = 24;
    public final static int java_lang_VM$do_newarray                        = 25;
    public final static int java_lang_VM$do_newdimension                    = 26;
    public final static int java_lang_VM$do_lcmp                            = 27;
/*if[FLOATS]*/
    public final static int java_lang_VM$do_fcmpl                           = 28;
    public final static int java_lang_VM$do_fcmpg                           = 29;
    public final static int java_lang_VM$do_dcmpl                           = 30;
    public final static int java_lang_VM$do_dcmpg                           = 31;
/*end[FLOATS]*/
}

