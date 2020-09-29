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
 * An instance of <code>ArrayStore</code> represents an instruction that
 * pops a value off the operand stack and stores it into an array at a
 * given index.
 *
 * @author  Doug Simon
 */
public final class ArrayStore extends Instruction implements Mutator {

    /**
     * The array component type.
     */
    private Klass componentType;

    /**
     * The array reference.
     */
    private StackProducer array;

    /**
     * The array index.
     */
    private StackProducer index;

    /**
     * The value being stored in the array.
     */
    private StackProducer value;

    /**
     * Creates an <code>ArrayStore</code> instance for an instruction that pops
     * a value off the operand stack and stores it into an array at a given
     * index.
     *
     * @param  componentType  the array component type
     * @param  array          the array to which the value is stored
     * @param  index          the index of the stored value
     * @param  value          the value being stored
     */
    public ArrayStore(Klass componentType, StackProducer array, StackProducer index, StackProducer value) {
        this.componentType = componentType;
        this.array         = array;
        this.index         = index;
        this.value         = value;
    }

    /**
     * Gets the array component type.
     *
     * @return the array component type
     */
    public Klass getComponentType() {
        return componentType;
    }

    /**
     * {@inheritDoc}
     */
    public Klass getMutationType() {
        return getComponentType();
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
     * Gets the value being stored in the array.
     *
     * @return the value being stored in the array
     */
    public StackProducer getValue() {
        return value;
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
    public boolean constrainsStack() {
        return !value.getType().isPrimitive();
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doArrayStore(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        array = visitor.doOperand(this, array);
        index = visitor.doOperand(this, index);
        value = visitor.doOperand(this, value);
    }
}
