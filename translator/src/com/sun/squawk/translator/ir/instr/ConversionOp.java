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
 * An instance of <code>ConversionOp</code> represents an unary
 * operation that pops a value of a given type off the operand stack,
 * converts it to a value of another type and pushes the result of the
 * operation.
 *
 * @author  Doug Simon
 */
public final class ConversionOp extends StackProducer {

    /**
     * The Squawk opcode corresponding to this operation.
     */
    private final int opcode;

    /**
     * The value being converted.
     */
    private StackProducer value;

    /**
     * Creates a <code>ConversionOp</code> instance representing an
     * instruction that converts a value on the operand stack from one type to
     * another.
     *
     * @param  to     the type the value is converted to
     * @param  value  the value being converted
     * @param  opcode the Squawk opcode corresponding to the operation
     */
    public ConversionOp(Klass to, StackProducer value, int opcode) {
        super(to);
        this.value = value;
        this.opcode = opcode;
    }

    /**
     * Gets the value being converted.
     *
     * @return the value being converted
     */
    public StackProducer getValue() {
        return value;
    }

    /**
     * Gets the type the value is being converted to.
     *
     * @return the type the value is being converted tp
     */
    public Klass getTo() {
        return getType();
    }

    /**
     * Gets the type the value is being converted from.
     *
     * @return the type the value is being converted from
     */
    public Klass getFrom() {
        return value.getType();
    }

    /**
     * Gets the Squawk opcode corresponding this conversion operation.
     *
     * @return the Squawk opcode corresponding this conversion operation
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doConversionOp(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        value = visitor.doOperand(this, value);
    }
}