/*
 * @(#)Address.java                       1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * This class represents an ARM Addressing Mode 4 address, used for the Load/Store Multipile
 * instructions.
 *
 * @author   David Liu
 * @version  1.00
 */
public class Address4 implements Constants {
    /**
     * Constructs an address of the form <b>&lt;addressing_mode> &lt;Rn>{!}, &lt;registers></b>.
     *
     * @param mode addressing mode (see the ADDR_xx constants for Addressing Mode 4 in {@link Constants})
     * @param base base register used in the address
     * @param regs registers to be loaded/stored
     */
    public Address4(int mode, Register base, Register [] regs) {
        this(mode, base, false, regs);
    }

    /**
     * Constructs an address of the form <b>&lt;addressing_mode> &lt;Rn>{!}, &lt;registers></b>.
     *
     * @param mode addressing mode (see the ADDR_xx constants for Addressing Mode 4 in {@link Constants})
     * @param base base register used in the address
     * @param updateBase determines if the base register should be updated after executing the instruction
     * @param regs registers to be loaded/stored
     */
    public Address4(int mode, Register base, boolean updateBase, Register [] regs) {
        Assert.that(regs.length > 0, "at least one register must be specified for loading/storing");

        this.mode = mode;
        this.base = base;
        this.updateBase = updateBase;
        this.regsBits = 0;

        for (int i = 0; i < regs.length; i++) {
            regsBits |= (1 << regs [i].getNumber());
        }
    }

    /**
     * Addressing mode used.
     */
    private int mode;

    /**
     * Calculates and returns the value of the P bit for this address, which has two meanings:
     *
     *     p == 1    indicates that the word addressed by the base register is included in the
     *               range of memory locations accessed, lying at the top (u == 0) or bottom
     *               (u == 1) of that range.
     *
     *     p == 0    indicates that the word addressed by the base register is excluded from the
     *               range of memory locations accessed, and lies on word beyond the top of the
     *               range (u == 0) or one word below the bottom of the range (u == 1).
     *
     * @param l specifies if a load (l == 1) or store (l == 0) operation is to be performed
     * @return value of the P bit (0 or 1)
     */
    public int getPBit(int l) {
        return ((mode == ADDR_DB) || (mode == ADDR_IB) ||
                 ((l == 1) && (mode == ADDR_EA || mode == ADDR_ED)) ||
                 ((l == 0) && (mode == ADDR_FD || mode == ADDR_FA))) ? 1 : 0;
    }

    /**
     * Calculates and returns the value of the U bit for this address, which indicates that the
     * transfer is made upwards (u == 1) or downwards (u == 0) from the base register.
     *
     * @param l specifies if a load (l == 1) or store (l == 0) operation is to be performed
     * @return value of the U bit (0 or 1)
     */
    public int getUBit(int l) {
        return ((mode == ADDR_IA) || (mode == ADDR_IB) ||
                 ((l == 1) && (mode == ADDR_FD || mode == ADDR_ED)) ||
                 ((l == 0) && (mode == ADDR_EA || mode == ADDR_FA))) ? 1 : 0;
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
     * Whether the base register should be updated after the transfer.
     */
    private boolean updateBase;

    /**
     * Returns whether the base register should be updated after the transfer.
     *
     * @return 1 if the base register is to be updated, 0 otherwise
     */
    public int getUpdateBaseBit() {
        return updateBase ? 1 : 0;
    }

    /**
     * Bitfield specifying which registers are to be loaded/stored.
     */
    private int regsBits;

    /**
     * Returns the registers to be loaded or stored, encoded in a bitfield with R0 at bit 0 to
     * R15 at bit 15.
     *
     * @return bitfield specifying which registers are to be loaded/stored
     */
    public int getRegsBits() {
        return regsBits;
    }
}
