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
 * Class representing an enter instruction.
 *
 * @author   Nik Shaylor
 */
public class EnterOp extends Instruction {

    /**
     * The special preamble code.
     */
    int specialPreamble;

    /**
     * Constructor.
     *
     * @param enforceFixedRegisterCall true when calls via a vtable must use a fixed register
     * @param specialPreamble the special premable code
     */
    public EnterOp(int specialPreamble) {
        super(VOID);
        this.specialPreamble = specialPreamble;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.reset();
        emitter.emitEnterOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" EnterOp ");
    }

}