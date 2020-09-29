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
 * Class representing a store parameter instruction.
 *
 * @author   Nik Shaylor
 */
public class StoreParmOp extends Instruction {

    /**
     * The stack local variable for the input operand.
     */
    StackLocal value;

    /**
     * The stack local variable for the parameter offset.
     */
    StackLocal index;

    /**
     * Constructor.
     *
     * @param p1 the instruction producing the input value
     * @param p2 the instruction producing the parameter offset
     */
    public StoreParmOp(Instruction p1, Instruction p2) {
        super(VOID);
        value = new StackLocal(p1.type());
        p1.setTarget(value);
        index = new StackLocal(p2.type());
        p2.setTarget(index);
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitStoreParmOp(this);
        emitter.freeLocal(value);
        emitter.freeLocal(index);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" StoreParmOp ");
        value.print(out);
        out.print(" ");
        index.print(out);
    }
}