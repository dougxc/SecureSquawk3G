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
 * Class representing a drop instruction.
 *
 * @author   Nik Shaylor
 */
public class DropOp extends Instruction {

    /**
     * The stack local variable for the input operand.
     */
    StackLocal in;

    /**
     * Constructor.
     *
     * @param p1 the input value
     */
    public DropOp(Instruction p1) {
        super(VOID);
        in = new StackLocal(p1.type());
        p1.setTarget(in);
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitDropOp(this);
        emitter.freeLocal(in);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" DropOp ");
        in.print(out);
    }
}