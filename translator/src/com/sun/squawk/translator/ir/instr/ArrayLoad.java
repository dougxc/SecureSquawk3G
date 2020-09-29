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
 * An instance of <code>ArrayLoad</code> represents an instruction that
 * loads an value from an array at given index and pushes it onto the
 * operand stack.
 *
 * @author  Doug Simon
 */
public final class ArrayLoad extends StackProducer {

    /**
     * The array reference.
     */
    private StackProducer array;

    /**
     * The array index.
     */
    private StackProducer index;

    /**
     * Creates an <code>ArrayLoad</code> instance for an instruction that loads
     * a value from an array and pushes it to the operand stack.
     *
     * @param  componentType the type of the value
     * @param  array         the array from which the value is loaded
     * @param  index         the index of the loaded value
     */
    public ArrayLoad(Klass componentType, StackProducer array, StackProducer index) {
        super(componentType);
        this.array = array;
        this.index = index;
    }

    /**
     * Gets the array reference.
     *
     * @return the array reference
     */
    public StackProducer getArray() {
        return array;
    }

    /**
     * Gets the array index.
     *
     * @return the array index
     */
    public StackProducer getIndex() {
        return index;
    }

    /**
     * {@inheritDoc}
     */
    public boolean mayCauseGC(boolean isStatic) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doArrayLoad(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        array = visitor.doOperand(this, array);
        index = visitor.doOperand(this, index);
    }
}
