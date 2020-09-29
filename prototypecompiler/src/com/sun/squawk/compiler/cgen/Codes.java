/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import com.sun.squawk.compiler.*;

/**
 * Class that specifies the internal opcode values.
 *
 * @author   Nik Shaylor
 */
public interface Codes {

    /**
     * Generic opcode definition.
     */
    public static final int OP_CVT  = 1  << 8,
                            OP_ADD  = 2  << 8,
                            OP_SUB  = 3  << 8,
                            OP_MUL  = 4  << 8,
                            OP_DIV  = 5  << 8,
                            OP_REM  = 6  << 8,
                            OP_SHL  = 7  << 8,
                            OP_SHR  = 8  << 8,
                            OP_USHR = 9  << 8,

                            OP_AND  = 10 << 8,
                            OP_OR   = 11 << 8,
                            OP_XOR  = 12 << 8,
                            OP_EQ   = 13 << 8,
                            OP_NE   = 14 << 8,
                            OP_LT   = 15 << 8,
                            OP_LE   = 16 << 8,
                            OP_GT   = 17 << 8,
                            OP_GE   = 18 << 8,
                            OP_NEG  = 19 << 8,
                            OP_COM  = 20 << 8,

                            OP_CON  = 21 << 8,
                            OP_LD   = 22 << 8,
                            OP_ST   = 23 << 8,
                            OP_LDX  = 24 << 8,
                            OP_STX  = 25 << 8,
                            OP_DUP  = 26 << 8,
                            OP_BT   = 27 << 8,
                            OP_BF   = 28 << 8,
                            OP_BR   = 29 << 8,
                            OP_CALL = 30 << 8,
                            OP_RET  = 31 << 8,
                            OP_COPY = 32 << 8;

    /**
     * Opcode subtype codes.
     */
    public static final int V = Type.Code_V,
                            R = Type.Code_R,
                            O = Type.Code_O,
                            I = Type.Code_I,
                            U = Type.Code_U,
                            L = Type.Code_L,
                            G = Type.Code_G,
                            F = Type.Code_F,
                            D = Type.Code_D,
                            B = Type.Code_B,
                            A = Type.Code_A,
                            S = Type.Code_S,
                            C = Type.Code_C;

}
