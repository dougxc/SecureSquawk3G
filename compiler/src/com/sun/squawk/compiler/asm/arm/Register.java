/*
 * @(#)Register.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * This class abstracts over the registers of the ARM7 family of microprocessors.
 *
 * @author   David Liu
 * @version  1.00
 */
public class Register implements Constants {

    /**
     * The number of this register.
     */
    private int number;

    /**
     * The name of this register.
     */
    private String name;


    /**
     * Constructs a new register with the specified name and number.
     *
     * @param  number  number of the register
     */
    public Register(int number, String name) {
        this.number = number;
        this.name = name;
    }

    /**
     * Tests if this register has a valid number (for physical integer registers).
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
     * Compares this register with the specified object for equality. Two
     * registers are equal if and only if they have the same number.
     *
     * @param   obj  the reference object with which to compare
     * @return  whether or not the registers are equal
     */
    public boolean equals(Object obj) {
        return (obj instanceof Register) && (((Register)obj).number == number);
    }

    /**
     * Returns a hash code value for this object.
     *
     * @return the hash code
     */
    public int hashCode() {
        return number;
    }

    /**
     * Gets the name of the register.
     */
    public String toString() {
        return name;
    }

    /**
     * Prints textual information about this register.
     * This method is used for debugging purposes.
     */
    public void print() {
        System.err.print(name);
    }
}
