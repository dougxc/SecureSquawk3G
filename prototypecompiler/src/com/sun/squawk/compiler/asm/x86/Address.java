/*
 * @(#)Address.java                     1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.x86;

/**
 * This class represents an address. It is an abstraction used to represent a
 * memory location using any of the x86 addressing modes with one object. A
 * register location is represented via {@link Register}, not via this class
 * for efficiency and simplicity reasons.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Address implements Constants {

    /**
     * The base register.
     */
    private Register base = NO_REG;

    /**
     * The index register.
     */
    private Register index = NO_REG;

    /**
     * The scaling factor.
     */
    private int scale = NO_SCALE;

    /**
     * The displacement.
     */
    private int disp;

    /**
     * The displacement if it was specified with a label.
     */
    private ALabel label;

    /**
     * Illegal
     */
    private Address() {
    }

    /**
     * Constructs an address of the form <b>disp</b>.
     *
     * @param  disp   the displacement
     */
    public Address(int disp) {
        this(NO_REG, NO_REG, NO_SCALE, disp);
    }

    /**
     * Constructs an address of the form <b>disp</b>.
     *
     * @param  label   the displacement
     */
    public Address(ALabel label) {
        this(NO_REG, NO_REG, NO_SCALE, label);
    }

    /**
     * Constructs an address of the form <b>[base][index*scale]</b>.
     *
     * @param  base   the base register
     * @param  index  the index register
     * @param  scale  the scaling factor
     */
    public Address(Register base, Register index, int scale) {
        this(base, index, scale, 0);
    }

    /**
     * Constructs an address of the form <b>disp[base]</b>.
     *
     * @param  base  the base register
     * @param  disp  the displacement
     */
    public Address(Register base, int disp) {
        this(base, NO_REG, NO_SCALE, disp);
    }

    /**
     * Constructs an address of the form <b>disp[base]</b>.
     *
     * @param  base  the base register
     * @param  label  the displacement label
     */
    public Address(Register base, ALabel label) {
        this(base, NO_REG, NO_SCALE, label);
    }

    /**
     * Constructs an address of the form <b>[base]</b>.
     *
     * @param  base  the base register
     */
    public Address(Register base) {
        this(base, NO_REG, NO_SCALE, 0);
    }

    /**
     * Constructs an address of the form <b>disp[index*scale]</b>.
     *
     * @param  index  the index register
     * @param  scale  the scaling factor
     * @param  disp   the displacement
     */
    public Address(Register index, int scale, int disp) {
        this(NO_REG, index, scale, disp);
    }

    /**
     * Constructs an address of the form <b>disp[index*scale]</b>.
     *
     * @param  index  the index register
     * @param  scale  the scaling factor
     * @param  label  the displacement label
     */
    public Address(Register index, int scale, ALabel label) {
        this(NO_REG, index, scale, label);
    }

    /**
     * Constructs an address of the form <b>disp[base][index*scale]</b>.
     *
     * @param  base   the base register
     * @param  index  the index register
     * @param  scale  the scaling factor
     * @param  disp   the displacement
     */
    public Address(Register base, Register index, int scale, int disp) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(index.equals(NO_REG) == (scale == NO_SCALE), "inconsistent address");
        }
        this.base  = base;
        this.index = index;
        this.scale = scale;
        this.disp  = disp;
    }

    /**
     * Constructs an address of the form <b>disp[base][index*scale]</b>.
     *
     * @param  base   the base register
     * @param  index  the index register
     * @param  scale  the scaling factor
     * @param  label  the displacement label
     */
    public Address(Register base, Register index, int scale, ALabel label) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(index.equals(NO_REG) == (scale == NO_SCALE), "inconsistent address");
        }
        this.base  = base;
        this.index = index;
        this.scale = scale;
        this.label = label;
    }

    /**
     * Returns the base register.
     *
     * @return  the base regsiter
     */
    public Register getBase() {
        return base;
    }

    /**
     * Returns the index register.
     *
     * @return  the index register
     */
    public Register getIndex() {
        return index;
    }

    /**
     * Returns the scaling factor.
     *
     * @return  the scaling factor
     */
    public int getScale() {
        return scale;
    }

    /**
     * Returns the label.
     *
     * @return  the label
     */
    public ALabel getLabel() {
        return label;
    }


    /**
     * Returns the displacement (only valid if the label is null).
     *
     * @return  the displacement
     */
    public int getDisp() {
        return disp;
    }

    /**
     * Tests if this address uses the specified register.
     *
     * @param   reg  register to look for
     * @return  whether or not this address uses the register
     */
    public boolean uses(Register reg) {
        return base.equals(reg) || index.equals(reg);
    }
}
