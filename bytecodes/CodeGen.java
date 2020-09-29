/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a very sad part of the Squawk JVM.
 */
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

public class CodeGen {

    public static final int PARM_N = 0;           // Nothing follows
    public static final int PARM_A = 1;           // Unsigned byte follows
    public static final int PARM_B = 2;           // Signed byte follows
    public static final int PARM_C = 3;           // Unsigned short follows
    public static final int PARM_S = 4;           // Signed short follows
    public static final int PARM_I = 5;           // int follows
    public static final int PARM_L = 6;           // long follows
    public static final int PARM_F = 7;           // float follows
    public static final int PARM_D = 8;           // double follows
    public static final int PARM_T = 9;           // Table follows

    public static int instLength[] = new int[] {
                                    1+0,     // Nothing follows
                                    1+1,     // Unsigned byte follows
                                    1+1,     // Signed byte follows
                                    1+2,     // Unsigned short follows
                                    1+2,     // Signed short follows
                                    1+4,     // int follows
                                    1+8,     // long follows
                                    1+4,     // float follows
                                    1+8,     // double follows
                                    1+0,     // Table follows
    };

    /**
     * Control flow value.
     */
    public final static int FLOW_NEXT   = 0, // The next bytecode is always executed after the current one.
                            FLOW_CHANGE = 1, // The bytecode changes the control flow.
                            FLOW_CALL   = 2; // The bytecode either calls a routine, or might throw an exception.

    public static Option option;
    public static int next        = 0;
    public static int type        = 0;
    public static int flow        = 0;
    public static boolean hasParm = false;
    public static String extra    = "";

    public static PrintStream out = System.out;

    public static StringBuffer instructionLengths = new StringBuffer();
    public static StringBuffer instructionStackEffects = new StringBuffer();

    public static int BYTECODE_COUNT;

    public static int FIRST_PARM_BYTECODE;
    public static int PARM_BYTECODE_COUNT;
    public static int FIRST_ESCAPE_PARM_BYTECODE;
    public static int ESCAPE_PARM_BYTECODE_COUNT;
    public static int FIRST_ESCAPE_WIDE_PSEUDOCODE;

    public static void main(String[] args) throws Exception {

        int optionNo = 0;
        if (args.length > 0) {
            optionNo = Integer.parseInt(args[0]);
        }

       /*
        * Options:
        *
        * 1 - Make OPC.java
        * 2 - Make Mnemonics.java
        * 3 - Make switch.c
        * 4 - Make Verifier.java
        * 5 - Make JitterSwitch.java
        * 6 - Make InterpreterSwitch.java
        * 7 - Make BytecodeRoutines.java
        */

        switch(optionNo) {
            case 1: option = new Option1(); break;
            case 2: option = new Option2(); break;
            case 3: option = new Option3(); break;
            case 4: option = new Option4(); break;
            case 5: option = new Option5(); break;
            case 6: option = new Option6(); break;
            case 7: option = new Option7(); break;
            default: throw new RuntimeException("unknown option: " + optionNo);
        }
        option.precommonpreamble();
        option.commonpreamble();
        option.preamble();

        //  name, innstruction format,  stack change in words,  control flow type

        // 0-15 bytecodes.

        make(16, "const",           PARM_N,  1,   FLOW_NEXT);
        make(16, "object",          PARM_N,  1,   FLOW_NEXT);
        make(16, "load",            PARM_N,  1,   FLOW_NEXT);
        make(16, "store",           PARM_N, -1,   FLOW_NEXT);

        // Load parm bytecodes

        make(8, "loadparm",         PARM_N,  1,   FLOW_NEXT);

        // Shift bytecodes

        make("wide_m1",             PARM_N,  0,   FLOW_CHANGE);
        make("wide_0",              PARM_N,  0,   FLOW_CHANGE);
        make("wide_1",              PARM_N,  0,   FLOW_CHANGE);
        make("wide_short",          PARM_N,  0,   FLOW_CHANGE);
        make("wide_int",            PARM_N,  0,   FLOW_CHANGE);
        make("escape",              PARM_N,  0,   FLOW_CHANGE);
        make("escape_wide_m1",      PARM_N,  0,   FLOW_CHANGE);
        make("escape_wide_0",       PARM_N,  0,   FLOW_CHANGE);
        make("escape_wide_1",       PARM_N,  0,   FLOW_CHANGE);
        make("escape_wide_short",   PARM_N,  0,   FLOW_CHANGE);
        make("escape_wide_int",     PARM_N,  0,   FLOW_CHANGE);

        // Simple bytecodes

        make("catch",               PARM_N,  1,   FLOW_NEXT);
        make("const_null",          PARM_N,  1,   FLOW_NEXT);
        make("const_m1",            PARM_N,  1,   FLOW_NEXT);
        make("const_byte",          PARM_B,  1,   FLOW_CHANGE);
        make("const_short",         PARM_S,  1,   FLOW_CHANGE);
        make("const_char",          PARM_C,  1,   FLOW_CHANGE);
        make("const_int",           PARM_I,  1,   FLOW_CHANGE);
        make("const_long",          PARM_L,  2,   FLOW_CHANGE);

        FIRST_PARM_BYTECODE = next;
        hasParm = true;

        make("object",              PARM_A,  1,   FLOW_NEXT);
        make("load",                PARM_A,  1,   FLOW_NEXT);
        make("load_i2",             PARM_A,  2,   FLOW_NEXT);
        make("store",               PARM_A, -1,   FLOW_NEXT);
        make("store_i2",            PARM_A, -2,   FLOW_NEXT);
        make("loadparm",            PARM_A,  1,   FLOW_NEXT);
        make("loadparm_i2",         PARM_A,  2,   FLOW_NEXT);
        make("storeparm",           PARM_A, -1,   FLOW_NEXT);
        make("storeparm_i2",        PARM_A, -2,   FLOW_NEXT);
        make("inc",                 PARM_A,  0,   FLOW_NEXT);
        make("dec",                 PARM_A,  0,   FLOW_NEXT);
        make("incparm",             PARM_A,  0,   FLOW_NEXT);
        make("decparm",             PARM_A,  0,   FLOW_NEXT);

        make("goto",                PARM_B,  0,   FLOW_CHANGE);
        make("if_eq_o",             PARM_B, -1,   FLOW_CHANGE);
        make("if_ne_o",             PARM_B, -1,   FLOW_CHANGE);
        make("if_cmpeq_o",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmpne_o",          PARM_B, -2,   FLOW_CHANGE);
        make("if_eq_i",             PARM_B, -1,   FLOW_CHANGE);
        make("if_ne_i",             PARM_B, -1,   FLOW_CHANGE);
        make("if_lt_i",             PARM_B, -1,   FLOW_CHANGE);
        make("if_le_i",             PARM_B, -1,   FLOW_CHANGE);
        make("if_gt_i",             PARM_B, -1,   FLOW_CHANGE);
        make("if_ge_i",             PARM_B, -1,   FLOW_CHANGE);
        make("if_cmpeq_i",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmpne_i",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmplt_i",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmple_i",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmpgt_i",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmpge_i",          PARM_B, -2,   FLOW_CHANGE);
        make("if_eq_l",             PARM_B, -2,   FLOW_CHANGE);
        make("if_ne_l",             PARM_B, -2,   FLOW_CHANGE);
        make("if_lt_l",             PARM_B, -2,   FLOW_CHANGE);
        make("if_le_l",             PARM_B, -2,   FLOW_CHANGE);
        make("if_gt_l",             PARM_B, -2,   FLOW_CHANGE);
        make("if_ge_l",             PARM_B, -2,   FLOW_CHANGE);
        make("if_cmpeq_l",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmpne_l",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmplt_l",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmple_l",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmpgt_l",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmpge_l",          PARM_B, -4,   FLOW_CHANGE);

        make("getstatic_i",         PARM_A,  0,   FLOW_CALL);
        make("getstatic_o",         PARM_A,  0,   FLOW_CALL);
        make("getstatic_l",         PARM_A,  1,   FLOW_CALL);

        make("class_getstatic_i",   PARM_A,  1,   FLOW_CALL);
        make("class_getstatic_o",   PARM_A,  1,   FLOW_CALL);
        make("class_getstatic_l",   PARM_A,  2,   FLOW_CALL);

        make("putstatic_i",         PARM_A, -2,   FLOW_CALL);
        make("putstatic_o",         PARM_A, -2,   FLOW_CALL);
        make("putstatic_l",         PARM_A, -3,   FLOW_CALL);

        make("class_putstatic_i",   PARM_A, -1,   FLOW_CALL);
        make("class_putstatic_o",   PARM_A, -1,   FLOW_CALL);
        make("class_putstatic_l",   PARM_A, -2,   FLOW_CALL);

        make("getfield_i",          PARM_A,  0,   FLOW_CALL);
        make("getfield_b",          PARM_A,  0,   FLOW_CALL);
        make("getfield_s",          PARM_A,  0,   FLOW_CALL);
        make("getfield_c",          PARM_A,  0,   FLOW_CALL);
        make("getfield_o",          PARM_A,  0,   FLOW_CALL);
        make("getfield_l",          PARM_A,  1,   FLOW_CALL);

        make("this_getfield_i",     PARM_A,  1,   FLOW_NEXT);
        make("this_getfield_b",     PARM_A,  1,   FLOW_NEXT);
        make("this_getfield_s",     PARM_A,  1,   FLOW_NEXT);
        make("this_getfield_c",     PARM_A,  1,   FLOW_NEXT);
        make("this_getfield_o",     PARM_A,  1,   FLOW_NEXT);
        make("this_getfield_l",     PARM_A,  2,   FLOW_NEXT);

        make("putfield_i",          PARM_A, -2,   FLOW_CALL);
        make("putfield_b",          PARM_A, -2,   FLOW_CALL);
        make("putfield_s",          PARM_A, -2,   FLOW_CALL);
        make("putfield_o",          PARM_A, -2,   FLOW_CALL);
        make("putfield_l",          PARM_A, -3,   FLOW_CALL);

        make("this_putfield_i",     PARM_A, -1,   FLOW_NEXT);
        make("this_putfield_b",     PARM_A, -1,   FLOW_NEXT);
        make("this_putfield_s",     PARM_A, -1,   FLOW_NEXT);
        make("this_putfield_o",     PARM_A, -1,   FLOW_NEXT);
        make("this_putfield_l",     PARM_A, -2,   FLOW_NEXT);

        make("invokevirtual_i",     PARM_A, -100, FLOW_CALL);
        make("invokevirtual_v",     PARM_A, -100, FLOW_CALL);
        make("invokevirtual_l",     PARM_A, -100, FLOW_CALL);
        make("invokevirtual_o",     PARM_A, -100, FLOW_CALL);

        make("invokestatic_i",      PARM_A, -100, FLOW_CALL);
        make("invokestatic_v",      PARM_A, -100, FLOW_CALL);
        make("invokestatic_l",      PARM_A, -100, FLOW_CALL);
        make("invokestatic_o",      PARM_A, -100, FLOW_CALL);

        make("invokesuper_i",       PARM_A, -100, FLOW_CALL);
        make("invokesuper_v",       PARM_A, -100, FLOW_CALL);
        make("invokesuper_l",       PARM_A, -100, FLOW_CALL);
        make("invokesuper_o",       PARM_A, -100, FLOW_CALL);

        make("invokenative_i",      PARM_A, -100, FLOW_CALL);
        make("invokenative_v",      PARM_A, -100, FLOW_CALL);
        make("invokenative_l",      PARM_A, -100, FLOW_CALL);
        make("invokenative_o",      PARM_A, -100, FLOW_CALL);

        make("findslot",            PARM_A, -2,   FLOW_CALL);
        make("extend",              PARM_A,  0,   FLOW_NEXT);

        PARM_BYTECODE_COUNT  = next - FIRST_PARM_BYTECODE;
        hasParm = false;

        make("invokeslot_i",        PARM_N, -100, FLOW_CALL);
        make("invokeslot_v",        PARM_N, -100, FLOW_CALL);
        make("invokeslot_l",        PARM_N, -100, FLOW_CALL);
        make("invokeslot_o",        PARM_N, -100, FLOW_CALL);

        make("return_v",            PARM_N,  0,   FLOW_CHANGE);
        make("return_i",            PARM_N, -1,   FLOW_CHANGE);
        make("return_l",            PARM_N, -2,   FLOW_CHANGE);
        make("return_o",            PARM_N, -1,   FLOW_CHANGE);

        make("tableswitch_i",       PARM_T, -1,   FLOW_CHANGE);
        make("tableswitch_s",       PARM_T, -1,   FLOW_CHANGE);

        make("extend0",             PARM_N,  0,   FLOW_NEXT);

        make("add_i",               PARM_N, -1,   FLOW_NEXT);
        make("sub_i",               PARM_N, -1,   FLOW_NEXT);
        make("and_i",               PARM_N, -1,   FLOW_NEXT);
        make("or_i",                PARM_N, -1,   FLOW_NEXT);
        make("xor_i",               PARM_N, -1,   FLOW_NEXT);
        make("shl_i",               PARM_N, -1,   FLOW_NEXT);
        make("shr_i",               PARM_N, -1,   FLOW_NEXT);
        make("ushr_i",              PARM_N, -1,   FLOW_NEXT);
        make("mul_i",               PARM_N, -1,   FLOW_NEXT);
        make("div_i",               PARM_N, -1,   FLOW_CALL);
        make("rem_i",               PARM_N, -1,   FLOW_CALL);
        make("neg_i",               PARM_N,  0,   FLOW_NEXT);
        make("i2b",                 PARM_N,  0,   FLOW_NEXT);
        make("i2s",                 PARM_N,  0,   FLOW_NEXT);
        make("i2c",                 PARM_N,  0,   FLOW_NEXT);
        make("add_l",               PARM_N, -2,   FLOW_NEXT);
        make("sub_l",               PARM_N, -2,   FLOW_NEXT);
        make("mul_l",               PARM_N, -2,   FLOW_NEXT);
        make("div_l",               PARM_N, -2,   FLOW_CALL);
        make("rem_l",               PARM_N, -2,   FLOW_CALL);
        make("and_l",               PARM_N, -2,   FLOW_NEXT);
        make("or_l",                PARM_N, -2,   FLOW_NEXT);
        make("xor_l",               PARM_N, -2,   FLOW_NEXT);
        make("neg_l",               PARM_N,  0,   FLOW_NEXT);
        make("shl_l",               PARM_N, -1,   FLOW_NEXT);
        make("shr_l",               PARM_N, -1,   FLOW_NEXT);
        make("ushr_l",              PARM_N, -1,   FLOW_NEXT);
        make("l2i",                 PARM_N, -1,   FLOW_NEXT);
        make("i2l",                 PARM_N,  1,   FLOW_NEXT);
        make("throw",               PARM_N, -1,   FLOW_CALL);
        make("pop_1",               PARM_N, -1,   FLOW_NEXT);
        make("pop_2",               PARM_N, -2,   FLOW_NEXT);
        make("monitorenter",        PARM_N, -1,   FLOW_CALL);
        make("monitorexit",         PARM_N, -1,   FLOW_CALL);
        make("class_monitorenter",  PARM_N,  0,   FLOW_CALL);
        make("class_monitorexit",   PARM_N,  0,   FLOW_CALL);
        make("arraylength",         PARM_N,  0,   FLOW_CALL);
        make("new",                 PARM_N, -1,   FLOW_CALL);
        make("newarray",            PARM_N, -1,   FLOW_CALL);
        make("newdimension",        PARM_N, -1,   FLOW_CALL);
        make("class_clinit",        PARM_N,  0,   FLOW_CALL);
        make("bbtarget_sys",        PARM_N,  0,   FLOW_NEXT);
        make("bbtarget_app",        PARM_N,  0,   FLOW_CALL);
        make("instanceof",          PARM_N, -1,   FLOW_CALL);
        make("checkcast",           PARM_N, -1,   FLOW_CALL);

        make("aload_i",             PARM_N, -1,   FLOW_CALL);
        make("aload_b",             PARM_N, -1,   FLOW_CALL);
        make("aload_s",             PARM_N, -1,   FLOW_CALL);
        make("aload_c",             PARM_N, -1,   FLOW_CALL);
        make("aload_o",             PARM_N, -1,   FLOW_CALL);
        make("aload_l",             PARM_N,  0,   FLOW_CALL);

        make("astore_i",            PARM_N, -3,   FLOW_CALL);
        make("astore_b",            PARM_N, -3,   FLOW_CALL);
        make("astore_s",            PARM_N, -3,   FLOW_CALL);
        make("astore_o",            PARM_N, -3,   FLOW_CALL);
        make("astore_l",            PARM_N, -4,   FLOW_CALL);

        make("lookup_i",            PARM_N, -1,   FLOW_CALL);
        make("lookup_b",            PARM_N, -1,   FLOW_CALL);
        make("lookup_s",            PARM_N, -1,   FLOW_CALL);

        if (next > 256) {
            System.err.println("Too many bytecodes "+next);
            System.exit(1);
        }

        if (256 - next <= 0) {
            extra += "public static final int RES_0=-1;\n";
            extra += "public static final int RES_0_COUNT=0;";
        } else {
            make(256 - next, "res",   PARM_N,  0, FLOW_CHANGE);
        }

        FIRST_ESCAPE_PARM_BYTECODE = next;
        hasParm = true;

        option.ifFLOATS();

        make("if_eq_f",             PARM_B, -1,   FLOW_CHANGE);
        make("if_ne_f",             PARM_B, -1,   FLOW_CHANGE);
        make("if_lt_f",             PARM_B, -1,   FLOW_CHANGE);
        make("if_le_f",             PARM_B, -1,   FLOW_CHANGE);
        make("if_gt_f",             PARM_B, -1,   FLOW_CHANGE);
        make("if_ge_f",             PARM_B, -1,   FLOW_CHANGE);
        make("if_cmpeq_f",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmpne_f",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmplt_f",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmple_f",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmpgt_f",          PARM_B, -2,   FLOW_CHANGE);
        make("if_cmpge_f",          PARM_B, -2,   FLOW_CHANGE);

        make("if_eq_d",             PARM_B, -2,   FLOW_CHANGE);
        make("if_ne_d",             PARM_B, -2,   FLOW_CHANGE);
        make("if_lt_d",             PARM_B, -2,   FLOW_CHANGE);
        make("if_le_d",             PARM_B, -2,   FLOW_CHANGE);
        make("if_gt_d",             PARM_B, -2,   FLOW_CHANGE);
        make("if_ge_d",             PARM_B, -2,   FLOW_CHANGE);
        make("if_cmpeq_d",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmpne_d",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmplt_d",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmple_d",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmpgt_d",          PARM_B, -4,   FLOW_CHANGE);
        make("if_cmpge_d",          PARM_B, -4,   FLOW_CHANGE);

        make("getstatic_f",         PARM_A,  0,   FLOW_CALL);
        make("getstatic_d",         PARM_A,  1,   FLOW_CALL);
        make("class_getstatic_f",   PARM_A,  1,   FLOW_CALL);
        make("class_getstatic_d",   PARM_A,  2,   FLOW_CALL);

        make("putstatic_f",         PARM_A, -2,   FLOW_CALL);
        make("putstatic_d",         PARM_A, -3,   FLOW_CALL);
        make("class_putstatic_f",   PARM_A, -1,   FLOW_CALL);
        make("class_putstatic_d",   PARM_A, -2,   FLOW_CALL);

        make("getfield_f",          PARM_A,  0,   FLOW_CALL);
        make("getfield_d",          PARM_A,  1,   FLOW_CALL);
        make("this_getfield_f",     PARM_A,  1,   FLOW_NEXT);
        make("this_getfield_d",     PARM_A,  2,   FLOW_NEXT);

        make("putfield_f",          PARM_A, -2,   FLOW_CALL);
        make("putfield_d",          PARM_A, -3,   FLOW_CALL);
        make("this_putfield_f",     PARM_A, -1,   FLOW_NEXT);
        make("this_putfield_d",     PARM_A, -2,   FLOW_NEXT);

        make("invokevirtual_f",     PARM_A, -100, FLOW_CALL);
        make("invokevirtual_d",     PARM_A, -100, FLOW_CALL);

        make("invokestatic_f",      PARM_A, -100, FLOW_CALL);
        make("invokestatic_d",      PARM_A, -100, FLOW_CALL);

        make("invokesuper_f",       PARM_A, -100, FLOW_CALL);
        make("invokesuper_d",       PARM_A, -100, FLOW_CALL);

        make("invokenative_f",      PARM_A, -100, FLOW_CALL);
        make("invokenative_d",      PARM_A, -100, FLOW_CALL);

        ESCAPE_PARM_BYTECODE_COUNT = next - FIRST_ESCAPE_PARM_BYTECODE;
        hasParm = false;

        make("invokeslot_f",        PARM_N, -100, FLOW_CALL);
        make("invokeslot_d",        PARM_N, -100, FLOW_CALL);

        make("return_f",            PARM_N, -1,   FLOW_CHANGE);
        make("return_d",            PARM_N, -2,   FLOW_CHANGE);

        make("const_float",         PARM_F,  1,   FLOW_CHANGE);
        make("const_double",        PARM_D,  2,   FLOW_CHANGE);

        make("add_f",               PARM_N, -1,   FLOW_NEXT);
        make("sub_f",               PARM_N, -1,   FLOW_NEXT);
        make("mul_f",               PARM_N, -1,   FLOW_NEXT);
        make("div_f",               PARM_N, -1,   FLOW_NEXT);
        make("rem_f",               PARM_N, -1,   FLOW_NEXT);
        make("neg_f",               PARM_N,  0,   FLOW_NEXT);
        make("add_d",               PARM_N, -2,   FLOW_NEXT);
        make("sub_d",               PARM_N, -2,   FLOW_NEXT);
        make("mul_d",               PARM_N, -2,   FLOW_NEXT);
        make("div_d",               PARM_N, -2,   FLOW_NEXT);
        make("rem_d",               PARM_N, -2,   FLOW_NEXT);
        make("neg_d",               PARM_N,  0,   FLOW_NEXT);
        make("i2f",                 PARM_N,  0,   FLOW_NEXT);
        make("l2f",                 PARM_N, -1,   FLOW_NEXT);
        make("f2i",                 PARM_N,  0,   FLOW_NEXT);
        make("f2l",                 PARM_N,  1,   FLOW_NEXT);
        make("i2d",                 PARM_N,  1,   FLOW_NEXT);
        make("l2d",                 PARM_N,  0,   FLOW_NEXT);
        make("f2d",                 PARM_N,  1,   FLOW_NEXT);
        make("d2i",                 PARM_N, -1,   FLOW_NEXT);
        make("d2l",                 PARM_N,  0,   FLOW_NEXT);
        make("d2f",                 PARM_N, -1,   FLOW_NEXT);

        make("aload_f",             PARM_N, -1,   FLOW_CALL);
        make("aload_d",             PARM_N,  0,   FLOW_CALL);
        make("astore_f",            PARM_N, -3,   FLOW_CALL);
        make("astore_d",            PARM_N, -4,   FLOW_CALL);

        option.endFLOATS();

        BYTECODE_COUNT = next;

        option.postamble();
    }


    static void make(int count, String name, int parms, int stack, int flow) {
        for (int i = 0 ; i < count ; i++) {
            make(name+"_"+i, parms, stack, flow);
        }
        extra += "public static final int "+name.toUpperCase()+"_0_COUNT="+count+";\n";
    }

    static void make(String name, int parms, int stack, int flo) {
        type = parms;
        flow = flo;
        append(name, instLength[parms], stack, instructionLengths, instructionStackEffects);
        make(name);
    }

    static void append(String name, int length, int stackEffect, StringBuffer lengths, StringBuffer stackEffects) {
        lengths.append((char)length);
        stackEffects.append((char)stackEffect);
    }

    static void make(String name) {
        option.make(name);
        next++;
    }

    static String space(int n, String name) {
        while(name.length() < n) {
            name += " ";
        }
        return name;
    }
}

/* ------------------------------------------------------------------------ *\
 *                         Template Option                                  *
\* ------------------------------------------------------------------------ */

abstract class Option {

    void precommonpreamble() {}

    void commonpreamble() {
        CodeGen.out.println("/* **DO NOT EDIT THIS FILE** */");
        CodeGen.out.println("/*");
        CodeGen.out.println(" * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.");
        CodeGen.out.println(" *");
        CodeGen.out.println(" * This software is the proprietary information of Sun Microsystems, Inc.");
        CodeGen.out.println(" * Use is subject to license terms.");
        CodeGen.out.println(" *");
        CodeGen.out.println(" * This is a part of the Squawk JVM.");
        CodeGen.out.println(" */");
        CodeGen.out.println("");
    }

    abstract void preamble();
    abstract void postamble();

    String space(int n, String s) {
        return CodeGen.space(n, s);
    }

    void make(String name) {
        bind(name);
        if (CodeGen.hasParm) {
            fetchByte(CodeGen.type == CodeGen.PARM_B ? "Byte" : "UByte");
            wide_bind(name);
        } else {
            fetchByte("None");
            wide_none();
        }
        pre(name);
        add(name);
        post(name);
        nl();
    }

    void bind(String s) {
    }

    void wide_bind(String s) {
    }

    void wide_none() {
    }

    void fetchByte(String parm) {
    }

    void pre(String s) {
    }

    void add(String s) {
    }

    void post(String s) {
    }

    void nl() {
    }

    void ifFLOATS() {
        CodeGen.out.println("/*if[FLOATS]*/");
    }

    void endFLOATS() {
        CodeGen.out.println("/*end[FLOATS]*/");
    }
}

/* ------------------------------------------------------------------------ *\
 *                      Option 1 - "OPC.java"                               *
\* ------------------------------------------------------------------------ */

class Option1 extends Option {

    Vector wides = new Vector();
    int wideFloatPoint = -1;

    void preamble() {
        CodeGen.out.println("package com.sun.squawk.vm;");
        CodeGen.out.println();
        CodeGen.out.println("/**");
        CodeGen.out.println(" * This class defines the bytecodes used in the Squawk system.");
        CodeGen.out.println(" * It was automatically generated as a part of the build process");
        CodeGen.out.println(" * and should not be edited by hand.");
        CodeGen.out.println(" *");
        CodeGen.out.println(" * @author   Nik Shaylor, Doug Simon");
        CodeGen.out.println(" */");
        CodeGen.out.println("public final class OPC {");
        CodeGen.out.println();
        CodeGen.out.println("    public final static int");
    }

    void bind(String name) {
        CodeGen.out.println(space(30, "        "+name.toUpperCase())+" = "+CodeGen.next+",");
        if (CodeGen.next > 255 && wideFloatPoint == -1) {
            wideFloatPoint = wides.size();
        }
    }

    void wide_bind(String name) {
        wides.addElement(space(30, "        "+name.toUpperCase()+"_WIDE")+" = ");
    }

    String ifFloats(int n, int m) {
        return "/*VAL*/false/*FLOATS*/ ? "+n+" : "+m;
//        return ""+n;
    }

    void postamble() {
        CodeGen.FIRST_ESCAPE_WIDE_PSEUDOCODE = CodeGen.BYTECODE_COUNT + wideFloatPoint;

        CodeGen.out.println("/*");
        CodeGen.out.println(" * Pseudo bytecodes.");
        CodeGen.out.println(" */");
        for (int i = 0 ; i < wides.size() ; i++) {
            String s = (String)wides.elementAt(i);
            if (i == wideFloatPoint) {
                ifFLOATS();
            }
            int n = CodeGen.BYTECODE_COUNT+i;
            int m = 256 + i;
            if (i < wideFloatPoint) {
                CodeGen.out.println(s+ifFloats(n, m)+",");
            } else {
                CodeGen.out.println(s+n+",");
            }
        }
        endFLOATS();
        CodeGen.out.println("    DUMMY=0");
        CodeGen.out.println(";");
        CodeGen.out.println("");

        CodeGen.out.println(CodeGen.extra);
        CodeGen.out.println("public static final int BYTECODE_COUNT               = " + ifFloats(CodeGen.BYTECODE_COUNT, 256) +";");
        CodeGen.out.println("public static final int FIRST_PARM_BYTECODE          = " + CodeGen.FIRST_PARM_BYTECODE +";");
        CodeGen.out.println("public static final int PARM_BYTECODE_COUNT          = " + CodeGen.PARM_BYTECODE_COUNT +";");
        CodeGen.out.println("public static final int FIRST_ESCAPE_PARM_BYTECODE   = " + ifFloats(CodeGen.FIRST_ESCAPE_PARM_BYTECODE, -1) +";");
        CodeGen.out.println("public static final int ESCAPE_PARM_BYTECODE_COUNT   = " + ifFloats(CodeGen.ESCAPE_PARM_BYTECODE_COUNT, 0) +";");
        CodeGen.out.println("public static final int FIRST_WIDE_PSEUDOCODE        = BYTECODE_COUNT;");
        CodeGen.out.println("public static final int FIRST_ESCAPE_WIDE_PSEUDOCODE = " + ifFloats(CodeGen.FIRST_ESCAPE_WIDE_PSEUDOCODE, -1) +";");
        CodeGen.out.println("public static final int PSEUDOCODE_COUNT             = " + ifFloats(wides.size(), wideFloatPoint) +";");
        CodeGen.out.println("public static final int WIDE_DELTA                   = FIRST_WIDE_PSEUDOCODE        - FIRST_PARM_BYTECODE;");
        CodeGen.out.println("public static final int ESCAPE_WIDE_DELTA            = FIRST_ESCAPE_WIDE_PSEUDOCODE - FIRST_ESCAPE_PARM_BYTECODE;");
        CodeGen.out.println("");
        CodeGen.out.println("public static final String LENGTH_TABLE = " + makeString(CodeGen.instructionLengths));
        CodeGen.out.println("public static final String STACK_EFFECT_TABLE = " + makeString(CodeGen.instructionStackEffects));
        CodeGen.out.println("");
        CodeGen.out.println("}");
    }

    String makeString(StringBuffer b) {
        String s = b.toString();
        b = new StringBuffer();
        b.append('"');
        for (int i = 0 ; i < s.length() ; i++) {
            char ch = s.charAt(i);
            char converted = (char)((byte)ch);
            if (converted != ch) {
                throw new RuntimeException("Adding a non-byte sized char to a StringOfBytes table");
            }
            b.append("\\u00");
            b.append(hex(ch>>4));
            b.append(hex(ch>>0));
        }
        b.append('"');
        b.append(';');
        return b.toString();
    }

    char hex(int i) {
        String hextable = "0123456789abcdef";
        return hextable.charAt(i&0xF);
    }

}


/* ------------------------------------------------------------------------ *\
 *                       Option 2 - "Mnemonics.java"                        *
\* ------------------------------------------------------------------------ */

class Option2 extends Option {

    void preamble() {
        CodeGen.out.println("package com.sun.squawk.vm;");
        CodeGen.out.println();
        CodeGen.out.println("/**");
        CodeGen.out.println(" * This class defines the bytecodes mnemonics used in the Squawk system.");
        CodeGen.out.println(" * It was automatically generated as a part of the build process");
        CodeGen.out.println(" * and should not be edited by hand.");
        CodeGen.out.println(" *");
        CodeGen.out.println(" * @author   Nik Shaylor, Doug Simon");
        CodeGen.out.println(" */");
        CodeGen.out.println("public final class Mnemonics {");
        CodeGen.out.println("");
        CodeGen.out.println("    public final static String[] OPCODES = {");
    }

    void make(String name) {
        CodeGen.out.println("        \""+name+"\",");
    }

    void postamble() {
        CodeGen.out.println("    };");
        CodeGen.out.println("}");
    }
}


/* ------------------------------------------------------------------------ *\
 *                         Option 3 - "switch.c"                            *
\* ------------------------------------------------------------------------ */

class Option3 extends Option {

    private   static Hashtable suffixes  = new Hashtable();
    private   static Hashtable types     = new Hashtable();
    private   static Hashtable functions = new Hashtable();
    protected static Vector    funlist   = new Vector();

    static {
        for (int i = 0 ; i < 256 ; i++) {
            suffixes.put(""+i, ""+i);   types.put(""+i, "int n");
        }
        suffixes.put("m1", "-1");       types.put("m1", "int n");
        suffixes.put("i", "INT");       types.put("i",  "Type t");
        suffixes.put("l", "LONG");      types.put("l",  "Type t");
        suffixes.put("f", "FLOAT");     types.put("f",  "Type t");
        suffixes.put("d", "DOUBLE");    types.put("d",  "Type t");
        suffixes.put("o", "OOP");       types.put("o",  "Type t");
        suffixes.put("b", "BYTE");      types.put("b",  "Type t");
        suffixes.put("s", "SHORT");     types.put("s",  "Type t");
        suffixes.put("c", "USHORT");    types.put("c",  "Type t");
        suffixes.put("v", "VOID");      types.put("v",  "Type t");
    }

    void preamble() {
        CodeGen.out.println("");
        CodeGen.out.println("        switch(opcode) {");
    }

    void fetchByte(String parm) {
        CodeGen.out.println("iparm"+parm+"();");
    }

    void bind(String name) {
        CodeGen.out.print(space(50, "            case OPC_"+name.toUpperCase()+": "));
    }

    void wide_bind(String name) {
        CodeGen.out.print(space(50, "            case OPC_"+name.toUpperCase()+"_WIDE: "));
    }

    void wide_none() {
        CodeGen.out.print(space(50, ""));
    }

    void pre(String name) {
    }

    void post(String name) {
        CodeGen.out.print("break;");
    }

    void ifFLOATS() {
        CodeGen.out.println("#ifdef java_lang_VM_doubleToLongBits");
    }

    void endFLOATS() {
        CodeGen.out.println("#endif");
    }

    void add(String name) {
        String parms = "";
        String type  = "";
        int p = name.lastIndexOf("_");
        if (p != -1) {
            String end = name.substring(p+1);
            String match = (String)suffixes.get(end);
            if (match != null) {
                parms = match;
                name  = name.substring(0, p);
                type  = (String)types.get(end);
            }
        }

        String cc = null;

        if (name.equals("if_eq"))    { cc = "1, EQ, "; }
        if (name.equals("if_ne"))    { cc = "1, NE, "; }
        if (name.equals("if_lt"))    { cc = "1, LT, "; }
        if (name.equals("if_le"))    { cc = "1, LE, "; }
        if (name.equals("if_gt"))    { cc = "1, GT, "; }
        if (name.equals("if_ge"))    { cc = "1, GE, "; }
        if (name.equals("if_cmpeq")) { cc = "2, EQ, "; }
        if (name.equals("if_cmpne")) { cc = "2, NE, "; }
        if (name.equals("if_cmplt")) { cc = "2, LT, "; }
        if (name.equals("if_cmple")) { cc = "2, LE, "; }
        if (name.equals("if_cmpgt")) { cc = "2, GT, "; }
        if (name.equals("if_cmpge")) { cc = "2, GE, "; }

        if (cc != null) {
            name = "if";
            parms = cc+parms;
            type = "int lth, int cc, "+type;
        }

        mainEntry(space(35, "do_"+mangle(name, parms)+";"));

        type = "("+type+")";
        if (functions.get(name+type) == null) {
            functions.put(name+type, name+type);
            funlist.addElement(name+type);
        }
    }

    String mangle(String name, String parms) {
        if (parms.equals("")) {
            if      (name.equals("object")) name += '0';
            else if (name.equals("load")) name += '0';
            else if (name.equals("store")) name += '0';
            else if (name.equals("loadparm")) name += '0';
            else if (name.equals("storeparm")) name += '0';
        }
        parms = "("+parms+")";
        return name+parms;
    }

    void mainEntry(String str) {
        CodeGen.out.print(str);
    }

    void nl() {
        CodeGen.out.println();
    }

    void postamble() {
        CodeGen.out.println("            default: shouldNotReachHere();");
        CodeGen.out.println("        }");
    }

}


/* ------------------------------------------------------------------------ *\
 *                       Option 4 - "Verifier.java"                         *
\* ------------------------------------------------------------------------ */

class Option4 extends Option3 {

    void precommonpreamble() {
        CodeGen.out.println("//if[J2ME.DEBUG]");
    }

    void preamble() {
        CodeGen.out.println("package com.sun.squawk.translator.ir;");
        CodeGen.out.println("import com.sun.squawk.vm.OPC;");
        CodeGen.out.println("import com.sun.squawk.util.Assert;");
        CodeGen.out.println();
        CodeGen.out.println("/**");
        CodeGen.out.println(" * This class defines the switch table used by the Squawk verifier.");
        CodeGen.out.println(" * It was automatically generated as a part of the build process");
        CodeGen.out.println(" * and should not be edited by hand.");
        CodeGen.out.println(" *");
        CodeGen.out.println(" * @author   Nik Shaylor");
        CodeGen.out.println(" */");
        CodeGen.out.println("class Verifier extends VerifierBase {");
        CodeGen.out.println("");
        CodeGen.out.println("    /**");
        CodeGen.out.println("     * Verify a bytecode.");
        CodeGen.out.println("     *");
        CodeGen.out.println("     * @param opcode the opcode to verify");
        CodeGen.out.println("     */");
        CodeGen.out.println("    protected void do_switch() {");
        CodeGen.out.println("");
        CodeGen.out.println("        switch(opcode) {");
    }

    void bind(String name) {
        String uname = name.toUpperCase();
        CodeGen.out.print(space(50, "            case OPC."+uname+":"));
    }

    void wide_bind(String name) {
        String uname = name.toUpperCase();
        CodeGen.out.print(space(50, "            case OPC."+uname+"_WIDE:"));
    }

    String mangle(String name, String parms) {
        parms = "("+parms+")";
        return name+parms;
    }

    void postamble() {
        CodeGen.out.println("            default: Assert.shouldNotReachHere(\"invalid opcode: \" + opcode);");
        CodeGen.out.println("        }");
        CodeGen.out.println("    }");
        CodeGen.out.println("}");
    }

    void ifFLOATS() {
        CodeGen.out.println("/*if[FLOATS]*/");
    }

    void endFLOATS() {
        CodeGen.out.println("/*end[FLOATS]*/");
    }

}


/* ------------------------------------------------------------------------ *\
 *                    Option 5 - "JitterSwitch.java"                        *
\* ------------------------------------------------------------------------ */

class Option5 extends Option4 {

    void precommonpreamble() {}

    void preamble() {
        CodeGen.out.println("package com.sun.squawk.vm;");
        CodeGen.out.println("");
        CodeGen.out.println("import com.sun.squawk.compiler.*;");
        CodeGen.out.println();
        CodeGen.out.println("/**");
        CodeGen.out.println(" * This class defines the switch table used by the Squawk jitter.");
        CodeGen.out.println(" * It was automatically generated as a part of the build process");
        CodeGen.out.println(" * and should not be edited by hand.");
        CodeGen.out.println(" *");
        CodeGen.out.println(" * @author   Nik Shaylor");
        CodeGen.out.println(" */");
        CodeGen.out.println("abstract public class JitterSwitch extends Common implements Types {");
        CodeGen.out.println("");
        CodeGen.out.println("    /**");
        CodeGen.out.println("     * The the immediate operand value of the current bytecode.");
        CodeGen.out.println("     */");
        CodeGen.out.println("    protected int iparm;");
        CodeGen.out.println("");
        CodeGen.out.println("    /**");
        CodeGen.out.println("     * Generate the native code for a bytecode.");
        CodeGen.out.println("     *");
        CodeGen.out.println("     * @param opcode the opcode to jit");
        CodeGen.out.println("     */");
        CodeGen.out.println("    protected void do_switch(int opcode) {");
        CodeGen.out.println("");
        CodeGen.out.println("        switch(opcode) {");
    }

    void postamble() {
        CodeGen.out.println("        }");
        CodeGen.out.println("    }");
        CodeGen.out.println("    abstract protected void iparmNone();");
        CodeGen.out.println("    abstract protected void iparmByte();");
        CodeGen.out.println("    abstract protected void iparmUByte();");
        CodeGen.out.println("}");
    }

}




/* ------------------------------------------------------------------------ *\
 *                 Option 6 - "InterpreterSwitch.java"                      *
\* ------------------------------------------------------------------------ */

class Option6 extends Option4 {

    void precommonpreamble() {}

    void pre(String name) {
        CodeGen.out.print("pre(");
        if (CodeGen.flow == CodeGen.FLOW_CHANGE) {
            CodeGen.out.print("FLOW_CHANGE);     ");
        } else if (CodeGen.flow == CodeGen.FLOW_CALL) {
            CodeGen.out.print("FLOW_CALL);       ");
        } else {
            CodeGen.out.print("FLOW_NEXT);       ");
        }
    }

    void post(String name) {
        CodeGen.out.print("post();");
    }

    void bind(String name) {
        CodeGen.out.print(space(50, "            bind(OPC."+name.toUpperCase()+");"));
    }

    void wide_bind(String name) {
        CodeGen.out.print(space(50, "            bind(OPC."+name.toUpperCase()+"_WIDE);"));
    }

    void fetchByte(String parm) {
        CodeGen.out.println("                      iparm"+parm+"();");
    }

    void preamble() {
        CodeGen.out.println("package com.sun.squawk.vm;");
        CodeGen.out.println("");
        CodeGen.out.println("import com.sun.squawk.compiler.*;");
        CodeGen.out.println();
        CodeGen.out.println("/**");
        CodeGen.out.println(" * This class defines the switch table used by the Squawk interpreter.");
        CodeGen.out.println(" * It was automatically generated as a part of the build process");
        CodeGen.out.println(" * and should not be edited by hand.");
        CodeGen.out.println(" *");
        CodeGen.out.println(" * @author   Nik Shaylor");
        CodeGen.out.println(" */");
        CodeGen.out.println("abstract public class InterpreterSwitch extends Common implements Types {");
        CodeGen.out.println("");
        CodeGen.out.println("    /**");
        CodeGen.out.println("     * Flags to show how the loading of the next bytecode should be done.");
        CodeGen.out.println("     */");
        CodeGen.out.println("    protected final static int FLOW_NEXT   = 0, // The next bytecode is always executed after the current one.");
        CodeGen.out.println("                               FLOW_CHANGE = 1, // The bytecode changes the control flow.");
        CodeGen.out.println("                               FLOW_CALL   = 2; // The bytecode either calls a routine, or might throw an exception.");
        CodeGen.out.println("");
        CodeGen.out.println("    /**");
        CodeGen.out.println("     * Create the bytecode interpreter.");
        CodeGen.out.println("     */");
        CodeGen.out.println("    protected void do_switch() {");
        CodeGen.out.println("");
        CodeGen.out.println("        {");
    }


    void postamble() {
        CodeGen.out.println("        }");
        CodeGen.out.println("    }");
        CodeGen.out.println("    abstract protected void pre(int code);");
        CodeGen.out.println("    abstract protected void post();");
        CodeGen.out.println("    abstract protected void bind(int opcode);");
        CodeGen.out.println("    abstract protected void iparmNone();");
        CodeGen.out.println("    abstract protected void iparmByte();");
        CodeGen.out.println("    abstract protected void iparmUByte();");
        CodeGen.out.println("}");
    }

}



/* ------------------------------------------------------------------------ *\
 *                   Option 7 - "BytecodeRoutines.java"                     *
\* ------------------------------------------------------------------------ */

class Option7 extends Option5 {

    void pre(String name) {
    }

    void post(String name) {
    }

    void bind(String name) {
    }

    void wide_bind(String name) {
    }

    void wide_none() {
    }

    void fetchByte(String parm) {
    }

    void mainEntry(String str) {
    }

    void nl() {
    }

    void preamble() {
        CodeGen.out.println("package com.sun.squawk.vm;");
        CodeGen.out.println("");
        CodeGen.out.println("import com.sun.squawk.compiler.*;");
        CodeGen.out.println();
        CodeGen.out.println("/**");
        CodeGen.out.println(" * This class defines the routines used by the Squawk interpreter and jitter.");
        CodeGen.out.println(" * It was automatically generated as a part of the build process");
        CodeGen.out.println(" * and should not be edited by hand.");
        CodeGen.out.println(" *");
        CodeGen.out.println(" * @author   Nik Shaylor");
        CodeGen.out.println(" */");
        CodeGen.out.println("abstract public class BytecodeRoutines implements Types {");
        CodeGen.out.println("");
    }

    void postamble() {
        for (Enumeration e = funlist.elements(); e.hasMoreElements(); ) {
            CodeGen.out.println("    abstract protected void do_"+e.nextElement()+";");
        }
        CodeGen.out.println("}");
    }
}

