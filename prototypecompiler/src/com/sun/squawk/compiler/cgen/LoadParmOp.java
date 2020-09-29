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
 * Class representing an instruction that returns the value of a parameter word.
 *
 * @author   Nik Shaylor
 */
public class LoadParmOp extends Instruction {

    /**
     * The stack local variable for the input operand.
     */
    StackLocal in;

    /**
     * Constructor.
     *
     * @param p1 the instruction producing the input operand
     */
    public LoadParmOp(Instruction p1) {
        super(INT);
        in = new StackLocal(p1.type());
        p1.setTarget(in);
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitLoadParmOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" LoadParmOp ");
        in.print(out);
    }
}