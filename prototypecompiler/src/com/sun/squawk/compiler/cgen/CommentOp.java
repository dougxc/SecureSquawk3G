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
 * Class representing a branch instruction.
 *
 * @author   Nik Shaylor
 */
public class CommentOp extends Instruction {

    /**
     * The stack variable for the input operand.
     */
    protected String str;

    /**
     * Constructor.
     *
     * @param str the comment string
     */
    public CommentOp(String str) {
        super(VOID);
        this.str = str;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitCommentOp(this);
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" CommentOp "+str);
    }

}