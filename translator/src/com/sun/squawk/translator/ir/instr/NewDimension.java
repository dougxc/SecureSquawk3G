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
 * An instance of <code>NewDimension</code> represents an instruction that
 * pops an array reference and a length value from the operand stack and
 * adds another dimension to the array of the specified length. The array
 * reference is then pushed back to the stack.
 *
 * @author  Doug Simon
 */
public final class NewDimension extends StackProducer {

    /**
     * The array to be extended.
     */
    private StackProducer array;

    /**
     * The length of the new dimension.
     */
    private StackProducer length;

    /**
     * Creates a <code>NewDimension</code> instance representing an instruction
     * that pops an array reference and a length value from the operand stack
     * and adds another dimension to the array of the specified length.
     *
     * @param  array   the array to be extended by one dimension
     * @param  length  the length of the new dimension
     */
    public NewDimension(StackProducer array, StackProducer length) {
        super(array.getType());
        this.array = array;
        this.length = length;
    }

    /**
     * Gets the array being extended.
     *
     * @return the array being extended
     */
    public StackProducer getArray() {
        return array;
    }

    /**
     * Gets the length of the new dimension.
     *
     * @return the length of the new dimension
     */
    public StackProducer getLength() {
        return length;
    }

    /**
     * {@inheritDoc}
     */
    public boolean constrainsStack() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doNewDimension(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        array = visitor.doOperand(this, array);
        length = visitor.doOperand(this, length);
    }
}