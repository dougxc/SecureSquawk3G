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
 * An instance of <code>NewArray</code> represents an instruction that pops
 * a value from the operand stack and uses it to create an new array of a
 * specified type whose length is determined by the popped value.
 *
 * @author  Doug Simon
 */
public final class NewArray extends StackProducer {

    /**
     * The length of the array.
     */
    private StackProducer length;

    /**
     * Creates a <code>NewArray</code> instance representing an instruction
     * that pops a value from the operand stack and uses it to create an new
     * array of a specified type whose length is determined by the popped value.
     *
     * @param type    the type of the array to create
     * @param length  the length of the array
     */
    public NewArray(Klass type, StackProducer length) {
        super(type);
        this.length = length;
    }

    /**
     * Gets the length of the array.
     *
     * @return the length of the array
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
    public Object getConstantObject() {
        return getType();
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doNewArray(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        length = visitor.doOperand(this, length);
    }
}