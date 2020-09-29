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
public class IntLiteralOp extends Instruction {

    /**
     * The value of the literal.
     */
    int value;

    /**
     * The name of the symbol need to be bound to (or zero if does not require linking).
     */
    String name;

    /**
     * Constructor.
     *
     * @param value the literal value
     * @param name the name of the symbol need to be bound to (or zero if does not require linking).
     */
    public IntLiteralOp(int value, String name) {
        super(INT);
        this.value = value;
        this.name  = name;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitIntLiteralOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" IntLiteralOp " + value);
    }
}