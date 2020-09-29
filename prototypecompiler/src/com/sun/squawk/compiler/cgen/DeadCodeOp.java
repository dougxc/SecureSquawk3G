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
public class DeadCodeOp extends Instruction {

    /**
     * Constructor.
     */
    public DeadCodeOp() {
        super(VOID);
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitDeadCodeOp(this);
    }

    /**
     * Test to see if the instrction can fall through to the next insatruction in sequence.
     *
     * @return true if it can
     */
    public boolean canFallThrough() {
        return false;
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" DeadCodeOp ");
    }
}