/*
 * @(#)Register.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.x86;

//import javac1.Assert;

/**
 * This class abstracts over the registers of the x86 family of microprocessors.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Register implements Constants {

    /**
     * The number of this register.
     */
    private int number;

    /**
     * Constructs a new register with the specified name and number.
     *
     * @param  number  number of the register
     */
    public Register(int number) {
        this.number = number;
    }

    /**
     * Tests if this register has a valid number.
     *
     * @return  whether or not the register is valid
     */
    public boolean isValid() {
        return (0 <= number) && (number < NUM_REGISTERS);
    }

    /**
     * Returns the number of this register.
     *
     * @return  the number of this register
     */
    public int getNumber() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isValid(), "not a register");
        }
        return number;
    }

    /**
     * Returns whether or not this register has a byte variant.
     *
     * @return  whether or not this register has a byte variant
     */
    public boolean hasByteRegister() {
        return (0 <= number) && (number < 4);
    }

    /**
     * Compares this register with the specified object for equality. Two
     * registers are equal if and only if they have the same number.
     *
     * @param   obj  the reference object with which to compare
     * @return  whether or not the registers are equal
     */
    public boolean equals(Object obj) {
        return (obj instanceof Register) && (((Register)obj).number == number);
    }
}
