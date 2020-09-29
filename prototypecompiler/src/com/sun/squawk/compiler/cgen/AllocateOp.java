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
 * Class representing an allocate instruction.
 *
 * @author   Nik Shaylor
 */
public class AllocateOp extends Instruction {

    /**
     * The local variable.
     */
    protected XLocal local;

    /**
     * The original type (used to check MP and IP).
     */
    protected Type originalType;

    /**
     * Constructor.
     *
     * @param local the local to allocate
     * @param originalType the original type
     */
    public AllocateOp(XLocal local, Type originalType) {
        super(VOID);
        this.local = local;
        this.originalType = originalType;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitAllocateOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" AllocateOp ");
        local.print(out);
    }

}