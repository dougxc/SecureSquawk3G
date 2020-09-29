/*
 * @(#)AbstractAssembler.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm;

import com.sun.squawk.compiler.asm.*;

/**
 * TODO
 *
 * @author   David Liu
 * @version  1.00
 */
public abstract class AbstractAssembler {

    /**
     * The code buffer that stores the generated instructions.
     */
    protected CodeBuffer code;

    /**
     * List of relocation records.
     */
/*TODO
     protected Relocator relocs;
*/

    /**
     * Count of unbound labels.
     */
    protected int unboundLabelCount;

    /**
     * Constructs a new assembler generating code into the specified buffer.
     *
     * @param  code  code buffer that stores the instructions
     */
    public AbstractAssembler(CodeBuffer code) {
        this.code = code;
    }

    /**
     * Allocate a new label.
     *
     * @return  a new label
     */
/*TODO
    public ALabel newLabel() {
        return new ALabel(this);
    }
*/

    /**
     * Adjust the number of unbound labels.
     *
     * @param  x  the amount to adjust by
     */
    public void unboundLabelCount(int x) {
        unboundLabelCount += x;
    }

    /**
     * Tests if the specified value can be represented with not more than 8
     * bits. This is true if and only if the value is inside the interval
     * [-128,128[.
     *
     * @param   x  the value to be tested
     * @return  whether or not the argument is an 8-bit value
     */
    protected static boolean is8bit(int x) {
        return (-128 <= x) && (x < 128);
    }

    /**
     * Tests if the specified value is within the range of one unsigned byte.
     * This is true if and only if the value is inside the interval [0,256[.
     *
     * @param   x  the value to be tested
     * @return  whether or not the argument is a byte
     */
    protected static boolean isByte(int x) {
        return (0 <= x) && (x < 256);
    }

    /**
     * Tests if the specified value is within the range of shift distances. This
     * is true if and only if the value is inside the interval [0,32[.
     *
     * @param   x  the value to be tested
     * @return  whether or not the argument is a valid shift distance
     */
    protected static boolean isShiftCount(int x) {
        return (0 <= x) && (x < 32);
    }

    /**
     * Returns the code buffer that contains the generated instructions.
     *
     * @return  the code buffer
     */
    public CodeBuffer getCode() {
        return code;
    }

    /**
     * Returns the address of the first byte of the code buffer.
     *
     * @return  start address of the code
     */
    public int getCodeBegin() {
        return code.getCodeBegin();
    }

    /**
     * Returns the current code generation position.
     *
     * @return  current code generation position
     */
    public int getCodePos() {
        return code.getCodePos();
    }

    /**
     * Returns the current code generation offset.
     *
     * @return  current code generation offset
     */
    public int getOffset() {
        return getCodePos() - getCodeBegin();
    }

    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @param  label   the label containing the displacement
     */
/*TODO
    public void emitLabel(ALabel label) {
        relocs = new Relocator(code.getCodePos(), relocs);
        label.addRelocator(relocs);
        emitInt(0xDEADBEEF);
    }
*/

    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @param  label   the label containing the displacement
     * @param  disp    the displacement when the label is null
     */
/*TODO
    protected void emitData(ALabel label, int disp) {
        if (label == null) {
            emitInt(disp);
        } else {
            emitLabel(label);
        }
    }
*/
}
