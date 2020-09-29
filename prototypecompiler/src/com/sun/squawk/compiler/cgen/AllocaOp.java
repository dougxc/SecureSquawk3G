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
 * Class representing a load instruction.
 *
 * @author   Nik Shaylor
 */
public class AllocaOp extends Instruction {

    /**
     * The stack local variable for the input operand.
     */
    StackLocal in;

    /**
     * Constructor.
     *
     * @param p1 the instruction producing the input operand
     */
    public AllocaOp(Instruction p1) {
        super(REF);
        in = new StackLocal(p1.type());
        p1.setTarget(in);
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitAllocaOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" AllocaOp ");
        in.print(out);
    }
}