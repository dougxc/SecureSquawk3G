/*
 * @(#)Address2.java                       1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * This class represents an ARM Addressing Mode 3 address, used for halfword, signed byte and
 * doubleword data transfer instructions.
 *
 * @author   David Liu
 * @version  1.00
 */
public class Address3 implements Constants {
    /**
     * Constructs a Mode 3 address.
     *
     * @param preIndexed true for pre-indexed mode, false for post-indexed
     * @param type type code for the addressing mode used
     * @param base base register
     * @param offset positive/negative 8-bit immediate offset
     * @param index index register
     * @param updateBase specifies whether the base register should be updated
     */
    private Address3(boolean preIndexed, int type, Register base, int offset, Register index, boolean updateBase) {
        Assert.that(offset >= -255 && offset <= 255, "invalid immediate offset");
        Assert.that(preIndexed || !updateBase, "only pre-indexed addresses can have the base register updated");

        this.preIndexed = preIndexed;
        this.type = type;
        this.base = base;
        this.offset = offset;
        this.index = index;
        this.updateBase = updateBase;
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <b>[Rn]</b>.
     *
     * @param base base register
     * @return the new address
     */
    public static Address3 pre(Register base) {
        return new Address3(true, ADDR_IMM, base, 0, NO_REG, false);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <b>[Rn, #+/-&lt;immed_8>]</b>.
     *
     * @param base base register
     * @param imm8 immediate offset
     * @return the new address
     */
    public static Address3 pre(Register base, int imm8) {
        return new Address3(true, ADDR_IMM, base, imm8, NO_REG, false);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <b>[Rn, #+/-Rm]<b>.
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address3 pre(Register base, int sign, Register index) {
        Assert.that(sign == 1 || sign == -1, "sign must be 1 or -1");
        return new Address3(true, ADDR_REG, base, sign, index, false);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <b>[Rn, #+/-&lt;immed_8>]!</b>.
     *
     * @param base base register
     * @param imm8 immediate offset
     * @return the new address
     */
    public static Address3 preW(Register base, int imm8) {
        return new Address3(true, ADDR_IMM, base, imm8, NO_REG, true);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <b>[Rn, #+/-Rm]!<b>.
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address3 preW(Register base, int sign, Register index) {
        Assert.that(sign == 1 || sign == -1, "sign must be 1 or -1");
        return new Address3(true, ADDR_REG, base, sign, index, true);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <b>[Rn], #+/-&lt;immed_8></b>.
     *
     * @param base base register
     * @param index index register
     * @return the new address
     */
    public static Address3 post(Register base, int imm8) {
        return new Address3(false, ADDR_IMM, base, imm8, NO_REG, false);
    }

    /**
     * Constructs and returns an Addressing Mode 3 address of the form <b>[Rn], +/-Rm</b>.
     *
     * @param base base register
     * @param sign whether the offset should be added to (1) or subtracted from (-1) the base
     * @param index index register
     * @return the new address
     */
    public static Address3 post(Register base, int sign, Register index) {
        Assert.that(sign == 1 || sign == -1, "sign must be 1 or -1");
        return new Address3(false, ADDR_REG, base, sign, index, false);
    }

    /**
     * Pre or post indexing.
     */
    private boolean preIndexed;

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
    private int type;

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
    private Register base;

    /**
     * Returns the base register for the address.
     *
     * @return base register
     */
    public Register getBaseReg() {
        return base;
    }

    /**
     * 8 bit immediate offset. This value can be positive or negative.
     */
    private int offset;

    /**
     * Returns the 12-bit immediate offset.
     *
     * @return immediate offset
     */
    public int getOffset() {
        return Math.abs (offset);
    }

    /**
     * Returns the sign of the index register or offset.
     *
     * @return true when the offset is added to the base, false when the offset is subtracted
     */
    public boolean getSign() {
        return offset >= 0;
    }

    /**
     * Index register.
     */
    private Register index;

    /**
     * Returns the index register for the address.
     *
     * @return index register
     */
    public Register getIndexReg () {
        return index;
    }

    /**
     * Determines if the base register is to be updated after data transfer.
     */
    private boolean updateBase;

    /**
     * Returns whether the base register is to be updated after data transfer.
     *
     * @return true if the base register is to be updated
     */
    public boolean getUpdateBase () {
        return updateBase;
    }
}
