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
 * Class representing a dup instruction.
 *
 * @author   Nik Shaylor
 */
public class DupOp extends UnOp {

    /**
     * Flag that is true if the dup is the last one in a sequence.
     */
    boolean last;

    /**
     * Constructor.
     *
     * @param p1 the instruction producing the input operand
     * @param first a reference to the first DupOp, or null if this one is first
     */
    public DupOp(Instruction p1, DupOp first) {
        super(p1, first);
        this.last = first != null;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitDupOp(this);
        if (last) {
            emitter.freeLocal(in);
        }
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.printBase(out);
        out.print(" DupOp ");
        in.print(out);
    }

}