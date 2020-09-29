/*
 * @(#)CallRelocator.java                         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */
package com.sun.squawk.compiler.asm.x86;

/**
 * Class to record call/jmp site locations when fixup is required.
 */
public class CallRelocator extends Relocator {

    /**
     * Create a relocator for a call, jmp, or jcc instruction.
     *
     * @param size      the number of bytes to the address
     * @param dst       the absolute destination address
     * @param position  the offset into the code buffer
     * @param next      pointer to the next Relocator
     */
    public CallRelocator(int size, int dst, int position, Relocator next) {
        super(position+size, next);
        value = dst - 4 - position - size;
    }

    /**
     * Emit the relocation into the code buffer.
     *
     * @param asm       the Assembler
     * @param address   the reclocation address for the code
     * @return          the relocation information
     */
    public int emit(Assembler asm, int address) {
        asm.getCode().setCodePos(position);
        asm.emitInt(value - address);
        return (Assembler.RELOC_RELATIVE_INT << 24) | position;
    }

}
