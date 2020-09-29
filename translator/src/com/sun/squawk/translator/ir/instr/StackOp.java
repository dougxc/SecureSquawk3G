/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir.instr;

import com.sun.squawk.translator.ir.*;

/**
 * An instance of <code>StackOp</code> is a place holder in an IR for a point
 * at which there was a stack manipulation instruction in the input bytecode.
 * That is, it is a place holder for a <i>dup</i>, <i>dup_x1</i>, <i>dup_x2</i>,
 * <i>dup2</i>, <i>dup2_x1</i>, <i>dup2_x2</i> or <i>swap</i> instruction.
 *
 * @author  Doug Simon
 */
public class StackOp extends Instruction {

    private final int opcode;

    /**
     * Creates an instance of <code>StackOp</code> as a place holder for
     * a stack manipulation instruction.
     *
     * @param opcode  the opcode of the stack manipulation instruction
     */
    public StackOp(int opcode) {
        this.opcode = opcode;
    }

    /**
     * Gets the opcode of the stack manipulation instruction for which this
     * object is a placeholder.
     *
     * @return the opcode of the encapsulated stack manipulation instruction
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doStackOp(this);
    }
}