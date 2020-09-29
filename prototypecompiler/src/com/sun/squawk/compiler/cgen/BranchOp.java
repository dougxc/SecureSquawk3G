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
public class BranchOp extends Instruction {

    /**
     * The branch condition.
     */
    protected boolean cond;

    /**
     * The branch target label.
     */
    protected XLabel label;

    /**
     * The branch target absolute destination.
     */
    protected int dst;

    /**
     * The stack local variable for the input operand.
     */
    protected StackLocal in;

    /**
     * Constructor.
     *
     * @param p1 the contition input value
     * @param cond the contition of the branch
     */
    private BranchOp(Instruction p1, boolean cond) {
        super(VOID);
        if (p1 != null) {
            in = new StackLocal(p1.type());
            p1.setTarget(in);
        }
        this.cond  = cond;
    }


    /**
     * Constructor.
     *
     * @param label the branch target
     * @param p1 the contition input value
     * @param cond the contition of the branch
     */
    public BranchOp(XLabel label, Instruction p1, boolean cond) {
        this(p1, cond);
        this.label = label;
    }

    /**
     * Constructor.
     *
     * @param dst the absolute destination
     * @param p1 the contition input value
     * @param cond the contition of the branch
     */
    public BranchOp(int dst, Instruction p1, boolean cond) {
        this(p1, cond);
        this.dst = dst;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitBranchOp(this);
        if (isConditional()) {
            emitter.freeLocal(in);
        }
    }

    /**
     * Test to see if the branch is conditional.
     *
     * @return true if it is
     */
    public boolean isConditional() {
        return in != null;
    }

    /**
     * Test to see if the instrction can fall through to the next insatruction in sequence.
     *
     * @return true if it can
     */
    public boolean canFallThrough() {
        return isConditional();
    }

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" BranchOp ");
        if (isConditional()) {
            out.print(cond);
            out.print(" ");
            in.print(out);
            out.print(" ");
        }
        if (label != null) {
            label.print(out);
        } else {
            out.print(dst);
        }
    }

}