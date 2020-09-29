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
 * An instance of <code>NegationOp</code> represents an unary arithmetic
 * operation that pops a value off the operand stack and pushes its
 * negated value.
 *
 * @author  Doug Simon
 */
public final class NegationOp extends StackProducer {

    /**
     * The value being negated.
     */
    private StackProducer value;

    /**
     * The Squawk opcode corresponding to this operation.
     */
    private final int opcode;

    /**
     * Creates a <code>NegationOp</code> instance representing an instruction
     * that negates a value on the operand stack.
     *
     * @param value   the value being negated
     * @param opcode  the Squawk opcode corresponding to the operation
     */
    public NegationOp(StackProducer value, int opcode) {
        super(value.getType());
        this.value = value;
        this.opcode = opcode;
    }

    /**
     * Gets the value being negated.
     *
     * @return the value being negated
     */
    public StackProducer getValue() {
        return value;
    }

    /**
     * Gets the Squawk opcode corresponding this negation operation.
     *
     * @return the Squawk opcode corresponding this negation operation
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doNegationOp(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        value = visitor.doOperand(this, value);
    }
}