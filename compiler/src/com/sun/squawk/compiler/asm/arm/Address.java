/*
 * @(#)Address.java                       1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

public class Address implements Constants {
    /**
     * Constructs an ARM address.
     *
     * @param preIndexed true for pre-indexed mode, false for post-indexed
     * @param type type code for the addressing mode used
     * @param base base register
     * @param index index register
     * @param immOffset immediate offset (+/- 12-bit immediate) or sign for register index (1 or -1)
     * @param updateBase specifies whether the base register should be updated
     * @param scaleMode shift operation to use (LSL, LSR, ASR, ROR)
     * @param shift number of bits to shift
     */
    private Address(boolean preIndexed, int type, Register base, Register index, int immOffset, boolean updateBase, int scaleMode, int shift) {
        Assert.that(preIndexed || !updateBase, "base register can only be updated with pre-indexed addressing");
        Assert.that(type == ADDR_IMM || (immOffset == 1 || immOffset == -1), "immOffset must be 1 or -1 when used with register index");
        Assert.that(type != ADDR_SCALE || scaleMode != LSL || (shift >= 0 && shift <= 31), "invalid shift value");
        Assert.that(type != ADDR_SCALE || scaleMode != LSR || (shift >= 1 && shift <= 32), "invalid shift value");
        Assert.that(type != ADDR_SCALE || scaleMode != ASR || (shift >= 1 && shift <= 32), "invalid shift value");
        Assert.that(type != ADDR_SCALE || scaleMode != ROR || (shift >= 0 && shift <= 31), "invalid shift value");
        Assert.that((Math.abs(immOffset) & 0xFFF) == Math.abs(immOffset), "invalid immediate offset");

        this.preIndexed = preIndexed;
        this.type = type;
        this.base = base;
        this.index = index;
        this.immOffset = immOffset;
        this.updateBase = updateBase;
        this.scaleMode = scaleMode;
        this.shift = shift;
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>]</b>.
     *
     * @param base base register
     * @return the new address
     */
    public static Address pre(Register base) {
        return new Address (true, ADDR_IMM, base, NO_REG, 0, false, 0, 0);
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>, #+/-&lt;offset_12>]</b>.
     *
     * @param base base register
     * @param imm12 positive/negative immediate 12-bit offset
     * @return the new address
     */
    public static Address pre(Register base, int imm12) {
        return new Address (true, ADDR_IMM, base, NO_REG, imm12, false, 0, 0);
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>, +/-&lt;Rm>]</b>.
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address pre(Register base, int sign, Register index) {
        return new Address (true, ADDR_REG, base, index, sign, false, 0, 0);
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>, +/-&lt;Rm>, &lt;shift> #&lt;shift_imm>]</b>.
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @param shift shift operation to perform (LSL, LSR, ASR, ROR)
     * @param imm number of bits to shift (0 - 32 depending upon shift operation)
     * @return the new address
     */
    public static Address pre(Register base, int sign, Register index, int shift, int imm) {
        Assert.that (shift == LSL || shift == LSR || shift == ASR || shift == ROR,
                     "invalid shift operation specified");
        return new Address (true, ADDR_SCALE, base, index, sign, false, shift, imm);
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>, #+/-&lt;offset_12>]!</b>.
     *
     * @param base base register
     * @param imm12 postive/negative immediate 12-bit offset
     * @return the new address
     */
    public static Address preW(Register base, int imm12) {
        return new Address (true, ADDR_IMM, base, NO_REG, imm12, true, 0, 0);
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>, +/-&lt;Rm>]!</b>.
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address preW(Register base, int sign, Register index) {
        return new Address (true, ADDR_REG, base, index, sign, true, 0, 0);
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>, +/-&lt;Rm>, &lt;shift> #&lt;shift_imm>]!</b>.
     * Rotate with extend (RRX) is specified by shifting 0 bits using ROR.
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @param shift shift operation to perform (LSL, LSR, ASR, ROR)
     * @param imm number of bits to shift (0 - 32 depending upon shift operation)
     * @return the new address
     */
    public static Address preW(Register base, int sign, Register index, int shift, int imm) {
        return new Address (true, ADDR_SCALE, base, index, sign, true, shift, imm);
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>], #+/-&lt;offset_12></b>.
     *
     * @param base base register
     * @param imm12 positive/negative immediate 12-bit offset
     * @return the new address
     */
    public static Address post(Register base, int imm12) {
        return new Address (false, ADDR_IMM, base, NO_REG, imm12, false, 0, 0);
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>], +/-&lt;Rm></b>.
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address post(Register base, int sign, Register index) {
        return new Address (false, ADDR_REG, base, index, sign, false, 0, 0);
    }

    /**
     * Constructs an address of the form <b>[&lt;Rn>], +/-&lt;Rm>, &lt;shift> #&lt;shift_imm></b>.
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @param shift shift operation to perform (LSL, LSR, ASR, ROR)
     * @param imm number of bits to shift (0 - 32 depending upon shift operation)
     * @return the new address
     */
    public static Address post(Register base, int sign, Register index, int shift, int imm) {
        return new Address (false, ADDR_SCALE, base, index, sign, false, shift, imm);
    }

    /**
     * Pre or post indexing.
     */
    private boolean preIndexed = true;

    /**
     * Determines if the address is pre-indexed (true) or post-indexed (false).
     *
     * @return the indexing mode
     */
    public boolean getPreIndexed() {
        return preIndexed;
    }

    /**
     * Type code of the addressing mode.
     */
    private int type = ADDR_IMM;

    /**
     * Returns the type code of the addressing mode that this object represents.  Refer to the
     * ADDR_xxx constants in the {@link Constants} class.
     * @return the type code
     */
    public int getType() {
        return type;
    }

    /**
     * Base register.
     */
    private Register base = NO_REG;

    /**
     * Returns the base register for the address.
     *
     * @return base register
     */
    public Register getBaseReg() {
        return base;
    }

    /**
     * Index register.
     */
    private Register index = NO_REG;

    /**
     * Returns the index register for the address.
     *
     * @return index register
     */
    public Register getIndexReg () {
        return index;
    }

    /**
     * 12 bit immediate offset. This value can be positive or negative.
     */
    private int immOffset = 0;

    /**
     * Returns the 12-bit immediate offset.
     *
     * @return immediate offset
     */
    public int getOffset() {
        return Math.abs (immOffset);
    }

    /**
     * Returns the sign of the index register or offset.
     *
     * @return true when the offset is added to the base, false when the offset is subtracted
     */
    public boolean getSign() {
        return immOffset >= 0;
    }

    /**
     * Determines if the base register is to be updated after data transfer.
     */
    private boolean updateBase = false;

    /**
     * Returns whether the base register is to be updated after data transfer.
     *
     * @return true if the base register is to be updated
     */
    public boolean getUpdateBase () {
        return updateBase;
    }

    /**
     * Scaling mode.
     */
    private int scaleMode = 0;

    /**
     * Returns the scaling mode to be used.  See the <b>LSL</b>, <b>LSR</b>, <b>ASR</b> and <b>ROR</b>
     * constants in the {@link Constants} class.
     *
     * @return scaling mode used
     */
    public int getScaleMode () {
        return scaleMode;
    }

    /**
     * Number of bits to shift for the scaled modes.
     */
    private int shift = -1;

    /**
     * Returns the number of bits to shift for the scaled modes.
     *
     * @return number of bits to shift
     */
    public int getShift () {
        Assert.that(type == ADDR_SCALE, "shift only applies for scaled modes");

        return shift;
    }
}
