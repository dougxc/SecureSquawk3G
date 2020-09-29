/*
 * @(#)Assert.java                      1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.x86;

/**
 * Relocation record.
 *
 * @author   Nik Shaylor
 * @version  1.00
 */
public class Relocator {

    /**
     * The position for this relocator in the code buffer.
     */
    protected int position;

    /**
     * The initial value to be patched.
     */
    protected int value = -1;

    /**
     * The next relocator.
     */
    protected Relocator next;

    /**
     * Create a relocator that will fixup a 4 byte word in the code buffer.
     *
     * @param position  the position in the code buffer
     * @param next      the next relocator in the chain
     */
    public Relocator(int position, Relocator next) {
        this.position = position;
        this.next     = next;
    }

    /**
     * Setup the initial value the word will be set to. This value is added to
     * the code relocation address before it is output.
     *
     * @param value  the initial value the word should be set to
     */
    public void setValue(int value) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(this.value == -1, "relocator bound more than once");
        }
        this.value = value;
    }

    /**
     * Get the position of the relocation in the code buffer.
     *
     * @return the code position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Emit the relocation into the code buffer.
     *
     * @param asm       the Assembler
     * @param address   the reclocation address for the code
     * @return          the relocation information
     */
    public int emit(Assembler asm, int address) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(this.value != -1, "relocator not bound");
        }
        asm.getCode().setCodePos(position);
        asm.emitInt(value + address);
        return (Assembler.RELOC_ABSOLUTE_INT << 24) | position;
    }

    /**
     * Get the next relocator in the chain.
     *
     * @return the next relocator or null if there is none
     */
    public Relocator getNext() {
        return next;
    }

}
