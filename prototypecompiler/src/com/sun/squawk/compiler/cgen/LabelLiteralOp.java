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
 * Class representing a literal label instruction.
 *
 * @author   Nik Shaylor
 */
public class LabelLiteralOp extends Instruction {

    /**
     * The type of the literal.
     */
    XLabel label;

    /**
     * Constructor.
     *
     * @param label the literal value
     */
    public LabelLiteralOp(XLabel label) {
        super(REF);
        this.label = label;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitLabelLiteralOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" LabelLiteralOp ");
        label.print(out);
    }
}