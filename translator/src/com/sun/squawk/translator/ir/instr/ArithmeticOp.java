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
import com.sun.squawk.vm.OPC;

/**
 * An instance of <code>ArithmeticOp</code> represents an binary arithmetic
 * operation that pops two values off the operand stack and pushes the
 * result of the operation.
 *
 * @author  Doug Simon
 */
public final class ArithmeticOp extends StackProducer {

    /**
     * The left operand of the operation.
     */
    private StackProducer left;

    /**
     * The left operand of the operation.
     */
    private StackProducer right;

    /**
     * The Squawk opcode corresponding to this operation.
     */
    private final int opcode;

    /**
     * Creates a <code>ArithmeticOp</code> instance representing a binary
     * arithmetic operation.
     *
     * @param   left    the left operand of the operation
     * @param   right   the right operand of the operation
     * @param   opcode  the Squawk opcode corresponding to the operation
     */
    public ArithmeticOp(StackProducer left, StackProducer right, int opcode) {
        super(left.getType());
        this.left   = left;
        this.right  = right;
        this.opcode = opcode;
    }

    /**
     * Gets the left operand of this arithmetic operation.
     *
     * @return the left operand of this arithmetic operation
     */
    public StackProducer getLeft() {
        return left;
    }

    /**
     * Gets the right operand of this arithmetic operation.
     *
     * @return the right operand of this arithmetic operation
     */
    public StackProducer getRight() {
        return right;
    }

    /**
     * Gets the Squawk opcode corresponding this arithmetic operation.
     *
     * @return the Squawk opcode corresponding this arithmetic operation
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doArithmeticOp(this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean mayCauseGC(boolean isStatic) {
        return opcode == OPC.DIV_I || opcode == OPC.DIV_L || opcode == OPC.REM_I || opcode == OPC.REM_L;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        left  = visitor.doOperand(this, left);
        right = visitor.doOperand(this, right);
    }
}