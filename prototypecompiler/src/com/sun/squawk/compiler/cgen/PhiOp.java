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
 * Class representing a phi instruction (branch target).
 *
 * @author   Nik Shaylor
 */
public class PhiOp extends Instruction {

    /**
     * The branch target.
     */
    XLabel label;

    /**
     * Constructor.
     *
     * @param label the branch target
     */
    public PhiOp(XLabel label) {
        super(VOID);
        this.label = label;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitPhiOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" PhiOp ");
        label.print(out);
    }
}