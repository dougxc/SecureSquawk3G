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
 * Class representing a jump to a computed location instruction.
 *
 * @author   Nik Shaylor
 */
public class JumpOp extends Instruction {

    /**
     * The stack local variable for the input operand.
     */
    protected StackLocal in;

    /**
     * Constructor.
     *
     * @param addr the instruction producing the address
     */
    public JumpOp(Instruction addr) {
        super(VOID);
        in = new StackLocal(addr.type());
        addr.setTarget(in);
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        emitter.emitJumpOp(this);
        emitter.freeLocal(in);
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
        out.print(" JumpOp ");
        in.print(out);
    }

}