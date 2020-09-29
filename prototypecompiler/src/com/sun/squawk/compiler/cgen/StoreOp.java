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
 * Class representing a store instruction.
 *
 * @author   Nik Shaylor
 */
public class StoreOp extends Instruction {

    /**
     * The stack local variable for the input operand.
     */
    StackLocal in;

    /**
     * The local to load.
     */
    XLocal local;

    /**
     * Constructor.
     *
     * @param p1 the instruction producing the input value
     * @param local the local variable to store
     */
    public StoreOp(Instruction p1, XLocal local) {
        super(VOID);
        in = new StackLocal(p1.type());
        p1.setTarget(in);
        this.local = local;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitStoreOp(this);
        emitter.freeLocal(in);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" StoreOp ");
        in.print(out);
        out.print(" ");
        local.print(out);
    }
}