/* **DO NOT EDIT THIS FILE** */
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
 * This class defines the bytecodes mnemonics used in the Squawk system.
 * It was automatically generated as a part of the build process
 * and should not be edited by hand.
 *
 * @author   Nik Shaylor, Doug Simon
 */
public final class Mnemonics {

    public final static String[] OPCODES = {
        "const_0",
        "const_1",
        "const_2",
        "const_3",
        "const_4",
        "const_5",
        "const_6",
        "const_7",
        "const_8",
        "const_9",
        "const_10",
        "const_11",
        "const_12",
        "const_13",
        "const_14",
        "const_15",
        "object_0",
        "object_1",
        "object_2",
        "object_3",
        "object_4",
        "object_5",
        "object_6",
        "object_7",
        "object_8",
        "object_9",
        "object_10",
        "object_11",
        "object_12",
        "object_13",
        "object_14",
        "object_15",
        "load_0",
        "load_1",
        "load_2",
        "load_3",
        "load_4",
        "load_5",
        "load_6",
        "load_7",
        "load_8",
        "load_9",
        "load_10",
        "load_11",
        "load_12",
        "load_13",
        "load_14",
        "load_15",
        "store_0",
        "store_1",
        "store_2",
        "store_3",
        "store_4",
        "store_5",
        "store_6",
        "store_7",
        "store_8",
        "store_9",
        "store_10",
        "store_11",
        "store_12",
        "store_13",
        "store_14",
        "store_15",
        "loadparm_0",
        "loadparm_1",
        "loadparm_2",
        "loadparm_3",
        "loadparm_4",
        "loadparm_5",
        "loadparm_6",
        "loadparm_7",
        "wide_m1",
        "wide_0",
        "wide_1",
        "wide_short",
        "wide_int",
        "escape",
        "escape_wide_m1",
        "escape_wide_0",
        "escape_wide_1",
        "escape_wide_short",
        "escape_wide_int",
        "catch",
        "const_null",
        "const_m1",
        "const_byte",
        "const_short",
        "const_char",
        "const_int",
        "const_long",
        "object",
        "load",
        "load_i2",
        "store",
        "store_i2",
        "loadparm",
        "loadparm_i2",
        "storeparm",
        "storeparm_i2",
        "inc",
        "dec",
        "incparm",
        "decparm",
        "goto",
        "if_eq_o",
        "if_ne_o",
        "if_cmpeq_o",
        "if_cmpne_o",
        "if_eq_i",
        "if_ne_i",
        "if_lt_i",
        "if_le_i",
        "if_gt_i",
        "if_ge_i",
        "if_cmpeq_i",
        "if_cmpne_i",
        "if_cmplt_i",
        "if_cmple_i",
        "if_cmpgt_i",
        "if_cmpge_i",
        "if_eq_l",
        "if_ne_l",
        "if_lt_l",
        "if_le_l",
        "if_gt_l",
        "if_ge_l",
        "if_cmpeq_l",
        "if_cmpne_l",
        "if_cmplt_l",
        "if_cmple_l",
        "if_cmpgt_l",
        "if_cmpge_l",
        "getstatic_i",
        "getstatic_o",
        "getstatic_l",
        "class_getstatic_i",
        "class_getstatic_o",
        "class_getstatic_l",
        "putstatic_i",
        "putstatic_o",
        "putstatic_l",
        "class_putstatic_i",
        "class_putstatic_o",
        "class_putstatic_l",
        "getfield_i",
        "getfield_b",
        "getfield_s",
        "getfield_c",
        "getfield_o",
        "getfield_l",
        "this_getfield_i",
        "this_getfield_b",
        "this_getfield_s",
        "this_getfield_c",
        "this_getfield_o",
        "this_getfield_l",
        "putfield_i",
        "putfield_b",
        "putfield_s",
        "putfield_o",
        "putfield_l",
        "this_putfield_i",
        "this_putfield_b",
        "this_putfield_s",
        "this_putfield_o",
        "this_putfield_l",
        "invokevirtual_i",
        "invokevirtual_v",
        "invokevirtual_l",
        "invokevirtual_o",
        "invokestatic_i",
        "invokestatic_v",
        "invokestatic_l",
        "invokestatic_o",
        "invokesuper_i",
        "invokesuper_v",
        "invokesuper_l",
        "invokesuper_o",
        "invokenative_i",
        "invokenative_v",
        "invokenative_l",
        "invokenative_o",
        "findslot",
        "extend",
        "invokeslot_i",
        "invokeslot_v",
        "invokeslot_l",
        "invokeslot_o",
        "return_v",
        "return_i",
        "return_l",
        "return_o",
        "tableswitch_i",
        "tableswitch_s",
        "extend0",
        "add_i",
        "sub_i",
        "and_i",
        "or_i",
        "xor_i",
        "shl_i",
        "shr_i",
        "ushr_i",
        "mul_i",
        "div_i",
        "rem_i",
        "neg_i",
        "i2b",
        "i2s",
        "i2c",
        "add_l",
        "sub_l",
        "mul_l",
        "div_l",
        "rem_l",
        "and_l",
        "or_l",
        "xor_l",
        "neg_l",
        "shl_l",
        "shr_l",
        "ushr_l",
        "l2i",
        "i2l",
        "throw",
        "pop_1",
        "pop_2",
        "monitorenter",
        "monitorexit",
        "class_monitorenter",
        "class_monitorexit",
        "arraylength",
        "new",
        "newarray",
        "newdimension",
        "class_clinit",
        "bbtarget_sys",
        "bbtarget_app",
        "instanceof",
        "checkcast",
        "aload_i",
        "aload_b",
        "aload_s",
        "aload_c",
        "aload_o",
        "aload_l",
        "astore_i",
        "astore_b",
        "astore_s",
        "astore_o",
        "astore_l",
        "lookup_i",
        "lookup_b",
        "lookup_s",
        "res_0",
/*if[FLOATS]*/
        "if_eq_f",
        "if_ne_f",
        "if_lt_f",
        "if_le_f",
        "if_gt_f",
        "if_ge_f",
        "if_cmpeq_f",
        "if_cmpne_f",
        "if_cmplt_f",
        "if_cmple_f",
        "if_cmpgt_f",
        "if_cmpge_f",
        "if_eq_d",
        "if_ne_d",
        "if_lt_d",
        "if_le_d",
        "if_gt_d",
        "if_ge_d",
        "if_cmpeq_d",
        "if_cmpne_d",
        "if_cmplt_d",
        "if_cmple_d",
        "if_cmpgt_d",
        "if_cmpge_d",
        "getstatic_f",
        "getstatic_d",
        "class_getstatic_f",
        "class_getstatic_d",
        "putstatic_f",
        "putstatic_d",
        "class_putstatic_f",
        "class_putstatic_d",
        "getfield_f",
        "getfield_d",
        "this_getfield_f",
        "this_getfield_d",
        "putfield_f",
        "putfield_d",
        "this_putfield_f",
        "this_putfield_d",
        "invokevirtual_f",
        "invokevirtual_d",
        "invokestatic_f",
        "invokestatic_d",
        "invokesuper_f",
        "invokesuper_d",
        "invokenative_f",
        "invokenative_d",
        "invokeslot_f",
        "invokeslot_d",
        "return_f",
        "return_d",
        "const_float",
        "const_double",
        "add_f",
        "sub_f",
        "mul_f",
        "div_f",
        "rem_f",
        "neg_f",
        "add_d",
        "sub_d",
        "mul_d",
        "div_d",
        "rem_d",
        "neg_d",
        "i2f",
        "l2f",
        "f2i",
        "f2l",
        "i2d",
        "l2d",
        "f2d",
        "d2i",
        "d2l",
        "d2f",
        "aload_f",
        "aload_d",
        "astore_f",
        "astore_d",
/*end[FLOATS]*/
    };
}
