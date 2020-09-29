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
 * An instance of <code>ArrayLength</code> represents an instruction that pops
 * an array reference from the operand stack and pushes its length.
 *
 * @author  Doug Simon
 */
public final class ArrayLength extends StackProducer {

    /**
     * The array reference.
     */
    private StackProducer array;

    /**
     * Creates a <code>ArrayLength</code> instance representing an instruction
     * that pops an array reference from the operand stack and pushes its
     * length.
     *
     * @param array   the array reference
     */
    public ArrayLength(StackProducer array) {
        super(Klass.INT);
        this.array = array;
    }

    /**
     * Gets the referenced array.
     *
     * @return the referenced array
     */
    public StackProducer getArray() {
        return array;
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
        visitor.doArrayLength(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        array = visitor.doOperand(this, array);
    }

}