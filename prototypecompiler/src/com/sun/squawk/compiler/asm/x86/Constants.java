/*
 * @(#)Obj.java                         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.x86;

/**
 * Definition of all the constants used in the assembler.
 */
public interface Constants {

    /**
     * A segment override prefix.
     */
    public static final int
            CS_SEGMENT = 0x2e, SS_SEGMENT = 0x36, DS_SEGMENT = 0x3e,
            ES_SEGMENT = 0x26, FS_SEGMENT = 0x64, GS_SEGMENT = 0x65;

    /**
     * A condition code constant.
     */
    public static final int
            OVERFLOW      = 0x0, NO_OVERFLOW   = 0x1, BELOW         = 0x2,
            CARRY_SET     = 0x2, ABOVE_EQUAL   = 0x3, CARRY_CLEAR   = 0x3,
            EQUAL         = 0x4, ZERO          = 0x4, NOT_EQUAL     = 0x5,
            NOT_ZERO      = 0x5, BELOW_EQUAL   = 0x6, ABOVE         = 0x7,
            NEGATIVE      = 0x8, POSITIVE      = 0x9, PARITY        = 0xa,
            NO_PARITY     = 0xb, LESS          = 0xc, GREATER_EQUAL = 0xd,
            LESS_EQUAL    = 0xe, GREATER       = 0xf, JUMP          = 0x10,
            CALL          = 0x11;

    /**
     * The size of the FPU state in words. The constant value of this field is <tt>27</tt>.
     */
    public static final int FPU_STATE_SIZE_IN_WORDS = 27;

    /**
     * The total number of available registers. The constant value of this field is <tt>8</tt>.
     */
    public static final int NUM_REGISTERS = 8;

    /**
     * A constant for one of the general-purpose registers.
     */
    public static final Register
            EAX = new Register(0), ECX = new Register(1), EDX = new Register(2),
            EBX = new Register(3), ESP = new Register(4), EBP = new Register(5),
            ESI = new Register(6), EDI = new Register(7);

    /**
     * A dummy register constant.
     */
    public static final Register NO_REG = new Register(-1);

    /**
     * A scaling factor constant.
     */
    public static final int NO_SCALE = -1, TIMES_1  =  0, TIMES_2  =  1, TIMES_4  =  2, TIMES_8  =  3;
}
