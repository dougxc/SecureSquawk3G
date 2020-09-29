/*
 * @(#)Operand2.java                     1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * This class represents the second operand expression of an ARM instruction such as move
 * or compare. It is an abstraction used to represent any of the 11 possible
 * options for <shifter_operand>.
 *
 * @author   David Liu
 * @version  1.00
 */
public class Operand2 implements Constants {
    private Operand2(int type, int imm, Register value, Register shift) {
        this.type = type;
        this.imm = imm;
        this.val = value;
        this.sft = shift;
    }

    /**
     * Creates and returns an "immediate" operand.
     *
     * @param imm8r immediate value
     */
    public static Operand2 imm(int imm8r) {
        return new Operand2(OPER2_IMM, imm8r, null, null);
    }

    /**
     * Creates and returns a "register" operand.
     *
     * @param reg register value
     * @return the new operand
     */
    public static Operand2 reg(Register reg) {
        return new Operand2(OPER2_REG, 0, reg, null);
    }

    /**
     * Creates and returns a "logical shift left by immediate" operand.
     *
     * @param reg register whose value is to be shifted
     * @param sft value of the shift (0 - 31)
     * @return the new operand
     */
    public static Operand2 lsl(Register reg, int sft) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(sft >= 0 && sft <= 31, "invalid shift value");
        }

        return new Operand2(OPER2_LSL_IMM, sft, reg, null);
    }

    /**
     * Creates and returns a "logical shift left by register" operand.
     *
     * @param reg register whose value is to be shifted
     * @param sft value of the shift
     * @return the new operand
     */
    public static Operand2 lsl(Register reg, Register sft) {
        return new Operand2(OPER2_LSL_REG, 0, reg, sft);
    }

    /**
     * Creates and returns a "logical shift right by immediate" operand.
     *
     * @param reg register whose value is to be shifted
     * @param sft value of the shift (1 - 32)
     * @return the new operand
     */
    public static Operand2 lsr(Register reg, int sft) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(sft >= 1 && sft <= 32, "invalid shift value");
        }

        return new Operand2(OPER2_LSR_IMM, sft, reg, null);
    }

    /**
     * Creates and returns a "logical shift right by register" operand.
     *
     * @param reg register whose value is to be shifted
     * @param sft register containing the value of the shift
     * @return the new operand
     */
    public static Operand2 lsr(Register reg, Register sft) {
        return new Operand2(OPER2_LSR_REG, 0, reg, sft);
    }

    /**
     * Creates and returns an "arithmetic shift right by immediate" operand.
     *
     * @param reg register whose value is to be shifted
     * @param sft value of the shift (1 - 32)
     * @return the new operand
     */
    public static Operand2 asr(Register reg, int sft) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(sft >= 1 && sft <= 32, "invalid shift value");
        }

        return new Operand2(OPER2_ASR_IMM, sft, reg, null);
    }

    /**
     * Creates and returns an "arithmetic shift right by register" operand.
     *
     * @param reg register whose value is to be shifted
     * @param sft register containing the value of the shift
     * @return the new operand
     */
    public static Operand2 asr(Register reg, Register sft) {
        return new Operand2(OPER2_ASR_REG, 0, reg, sft);
    }

    /**
     * Creates and returns a "rotate right by immediate" operand.
     *
     * @param reg register whose value is to be rotated
     * @param rot value of the rotation
     * @return the new operand
     */
    public static Operand2 ror(Register reg, int rot) {
        return new Operand2(OPER2_ROR_IMM, rot, reg, null);
    }

    /**
     * Creates and returns a "rotate right by register" operand.
     *
     * @param reg register whose value is to be rotated
     * @param sft register containing the value of the rotation
     * @return Operand2
     */
    public static Operand2 ror(Register reg, Register sft) {
        return new Operand2(OPER2_ROR_REG, 0, reg, sft);
    }

    /**
     * Creates and returns a "rotate right with extend" operand.
     *
     * @param reg register whose value is shifted right by one bit
     * @return the new operand
     */
    public static Operand2 rrx(Register reg) {
        return new Operand2(OPER2_RRX, 0, reg, null);
    }

    private int type;

    /**
     * Returns the type code of this operand expression. The type codes are defined in the ]
     * {@link Constants} interface.
     *
     * @return type code for this operand
     */
    public int getType() {
        return type;
    }

    private int imm;

    /**
     * Returns the immediate value used in this operand expression. Only values that can be
     * represented as a combination of an 8-bit immediate and a rotation of an even number of bits
     * are valid.
     *
     * @return the immediate value
     */
    public int getImm() {
        return imm;
    }

    public int getPackedImm8() {
        for (int rotate_imm = 0; rotate_imm < 32; rotate_imm += 2) {
            int imm8 = (imm << rotate_imm) | (imm >>> (32 - rotate_imm));
            if ((imm8 & 0xff) == imm8) {
                return imm8;
            }
        }

        Assert.that(false, "should not reach here");
        return 0;
    }

    public int getPackedImmRot() {
        for (int rotate_imm = 0; rotate_imm < 32; rotate_imm += 2) {
            int imm8 = (imm << rotate_imm) | (imm >>> (32 - rotate_imm));
            if ((imm8 & 0xff) == imm8) {
                return rotate_imm / 2;
            }
        }

        Assert.that(false, "should not reach here");
        return 0;
    }

    private Register val;

    /**
     * Returns the register whose value is to be used in this operand expression.
     *
     * @return register that will be used in this operand expression
     */
    public Register getReg() {
        return val;
    }

    private Register sft;

    /**
     * Returns the register whose value contains the value of the shift in this operand expression.
     *
     * @return register containing the value of the shift
     */
    public Register getShift() {
        return sft;
    }
}
