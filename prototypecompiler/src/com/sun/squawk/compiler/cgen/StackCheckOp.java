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
 * Class representing a stack check instruction.
 *
 * @author   Nik Shaylor
 */
public class StackCheckOp extends Instruction {

    /**
     * The stack local variable for the the number of extra stack bytes.
     */
    StackLocal extraStack;

    /**
     * The stack local variable for the the number of extra local varible bytes.
     */
    StackLocal extraLocal;

    /**
     * Constructor.
     *
     * @param p1 the input value for the number of extra stack bytes
     * @param p2 the input value for the number of extra local varible bytes
     */
    public StackCheckOp(Instruction p1, Instruction p2) {
        super(VOID);
        extraStack = new StackLocal(p1.type());
        p1.setTarget(extraStack);
        extraLocal = new StackLocal(p2.type());
        p2.setTarget(extraLocal);

    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitStackCheckOp(this);
        emitter.freeLocal(extraLocal);
        emitter.freeLocal(extraStack);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" StackCheckOp ");
        extraLocal.print(out);
        out.print(" ");
        extraStack.print(out);
    }
}