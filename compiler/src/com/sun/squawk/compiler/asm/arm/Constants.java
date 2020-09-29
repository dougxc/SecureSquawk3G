/*
 * @(#)Constants.java                         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * Definition of all the constants used in the assembler.
 *
 * @author   David Liu
 * @version  1.00
 *
 */
public interface Constants {
    /**
     * Conditional execution flags.
     */
    public static final int
        COND_EQ = 0x0, // Equal, Z set
        COND_NE = 0x1, // Not equal, Z clear
        COND_CS = 0x2, // Carry set/unsigned higher or same, C set
        COND_CC = 0x3, // Carry clear/unsigned lower, C clear
        COND_MI = 0x4, // Minus/negative, N set
        COND_PL = 0x5, // Plus/positive or zero, N clear
        COND_VS = 0x6, // Overflow, V set
        COND_VC = 0x7, // No overflow, V clear
        COND_HI = 0x8, // Unsigned higher, C set and Z clear
        COND_LS = 0x9, // Unsigned lower or same, C clear or Z set
        COND_GE = 0xA, // Signed greater than or equal, N == V
        COND_LT = 0xB, // Signed less than N != V
        COND_GT = 0xC, // Signed greater than, Z == 0 and N == V
        COND_LE = 0xD, // Signed less than or equal, Z == 1 or N != V
        COND_AL = 0xE, // Always (unconditional)
        COND_NV = 0xF; // Unsupported

    /**
     * Condition code mnemonics indexed by their opcodes.
     */
    public static final String [] CONDITION_MNEMONICS = new String [] {
        "eq", "ne", "cs", "cc", "mi", "pl", "vs", "vc",
        "hi", "ls", "ge", "lt", "gt", "le", "al", "nv"
    };

    /**
     * The total number of available physical registers. The constant value of this
     * field is <tt>16</tt>.
     */
    public static final int NUM_REGISTERS = 16;

    /**
     * A constant for one of the general-purpose registers.
     */
    public static final Register
        /* Dummy register */
        SBZ = new Register (0, "sbz"),
        NO_REG = SBZ,
        /* Unbanked registers */
        R0 = new Register (0, "r0"),
        R1 = new Register (1, "r1"),
        R2 = new Register (2, "r2"),
        R3 = new Register (3, "r3"),
        R4 = new Register (4, "r4"),
        R5 = new Register (5, "r5"),
        R6 = new Register (6, "r6"),
        R7 = new Register (7, "r7"),
        /* Banked registers */
        R8 = new Register (8, "r8"),
        R9 = new Register (9, "r9"),
        R10 = new Register (10, "r10"),
        /* Frame Pointer */
        FP = new Register (11, "fp"),
        R11 = FP,
        R12 = new Register (12, "r12"),
        /* Stack Pointer */
        SP = new Register (13, "sp"),
        R13 = SP,
        /* Link Register */
        LR = new Register (14, "lr"),
        R14 = LR,
        /* Program Counter */
        PC = new Register (15, "pc"),
        R15 = PC;

    /**
     * ARM registers indexed by the register number.
     */
    public static final Register [] REGISTERS = new Register [] {
        R0, R1, R2, R3, R4, R5, R6, R7,
        R8, R9, R10, R12, FP, SP, LR, PC
    };

    /**
     * Shifter operand types. These numbers are arbitrarily assigned for use within the assembler only.
     */
    public static final int
        OPER2_IMM = 1,      /* Immediate value */
        OPER2_REG = 2,      /* Register value */
        OPER2_LSL_IMM = 3,  /* Logical shift left immediate */
        OPER2_LSL_REG = 4,  /* Logical shift left register */
        OPER2_LSR_IMM = 5,  /* Logical shift right immediate */
        OPER2_LSR_REG = 6,  /* Logical shift right register */
        OPER2_ASR_IMM = 7,  /* Arithmetic shift right immediate */
        OPER2_ASR_REG = 8,  /* Arithmetic shift right register */
        OPER2_ROR_IMM = 9,  /* Rotate right immediate */
        OPER2_ROR_REG = 10, /* Rotate right register */
        OPER2_RRX = 11;     /* Rotate right extended */

    /**
     * Instruction opcodes for data processing and logic instructions.  Bits 0-4 of the following
     * specifies the opcode bits 21-24 in the instruction.
     */
    public static final int
        OPCODE_ADC = 0x5,
        OPCODE_ADD = 0x4,
        OPCODE_AND = 0x0,
        OPCODE_BIC = 0xe,
        OPCODE_EOR = 0x1,
        OPCODE_CMN = 0xb,
        OPCODE_CMP = 0xa,
        OPCODE_MOV = 0xd,
        OPCODE_MVN = 0xf,
        OPCODE_ORR = 0xc,
        OPCODE_RSB = 0x3,
        OPCODE_RSC = 0x7,
        OPCODE_SBC = 0x6,
        OPCODE_SUB = 0x2,
        OPCODE_TEQ = 0x9,
        OPCODE_TST = 0x8;

    /**
     * Mneumonics for data processing and logic instructions, indexed by their opcodes.
     */
    public static final String [] DATAPROC_INS = new String [] {
        "and", "eor", "sub", "rsb", "add", "adc", "sbc", "rsc",
        "tst", "teq", "cmp", "cmn", "orr", "mov", "bic", "mvn"
    };

    /**
     * Addressing Mode 2 and 3 constants.
     */
    public static final int
        ADDR_IMM = 1,   /* Immediate offset */
        ADDR_REG = 2,   /* Register offset */
        ADDR_SCALE = 3; /* Scaled register offset (mode 2 only) */

    /**
     * Address scaling mode opcodes for Mode 2.
     */
    public static final int
        LSL = 0, /* Logical shift left */
        LSR = 1, /* Logical shift right */
        ASR = 2, /* Arithmetic shift right */
        ROR = 3; /* Rotate right */

    /**
     * Addressing Mode 4 constants (Multiple Data Transfer).
     */
    public static final int
        /* Block load/store addressing modes */
        ADDR_IA = 0, /* Increment After */
        ADDR_IB = 1, /* Increment Before */
        ADDR_DA = 2, /* Decrement After */
        ADDR_DB = 3, /* Decrement Before */
        /* Stack pop/push addressing modes */
        ADDR_FD = 4, /* Full Descending */
        ADDR_ED = 5, /* Empty Descending */
        ADDR_FA = 6, /* Full Ascending */
        ADDR_EA = 7; /* Empty Ascending */
}
