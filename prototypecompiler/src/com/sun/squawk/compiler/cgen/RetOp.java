/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import java.io.PrintStream;
import com.sun.squawk.compiler.*;

/**
 * Class representing a ret instruction.
 *
 * @author   Nik Shaylor
 */
public class RetOp extends Instruction {

    /**
     * The stack local variable for the input operand.
     */
    StackLocal in;

    /**
     * Constructor.
     *
     * @param p1 the input value or null if there is none
     */
    public RetOp(Instruction p1) {
        super(p1 == null ? VOID : p1.type());
        if (p1 != null) {
            in = new StackLocal(p1.type());
            p1.setTarget(in);
        }
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitRetOp(this);
        if (in != null) {
            emitter.freeLocal(in);
        }
    }

    /**
     * Test to see if the instrction can fall through to the next insatruction in sequence.
     *
     * @return true if it can
     */
    public boolean canFallThrough() {
        return false;
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" RetOp ");
        if (in != null) {
            in.print(out);
        }
    }
}