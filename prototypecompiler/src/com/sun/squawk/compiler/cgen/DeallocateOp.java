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
 * Class representing a deallocate instruction.
 *
 * @author   Nik Shaylor
 */
public class DeallocateOp extends Instruction {

    /**
     * The local variable.
     */
    XLocal local;

    /**
     * Constructor.
     *
     * @param local the local to deallocate
     */
    public DeallocateOp(XLocal local) {
        super(VOID);
        this.local = local;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitDeallocateOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" DeallocateOp ");
        local.print(out);
    }
}