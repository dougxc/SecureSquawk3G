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
 * Class representing a literal instruction.
 *
 * @author   Nik Shaylor
 */
public class FloatLiteralOp extends Instruction {

    /**
     * The type of the literal.
     */
    float value;

    /**
     * Constructor.
     *
     * @param value the literal value
     */
    public FloatLiteralOp(float value) {
        super(FLOAT);
        this.value = value;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitFloatLiteralOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
/*if[FLOATS]*/
        out.print(" FloatLiteralOp " + value);
/*end[FLOATS]*/
    }
}