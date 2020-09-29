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
 * An instance of <code>InstanceOf</code> represents an instruction that pops
 * an object from the operand stack, tests to see if it is assignable to a
 * specified type and pushes the boolean result of the test back to the stack.
 *
 * @author  Doug Simon
 */
public final class InstanceOf extends StackProducer {

    /**
     * The object whose type is being tested.
     */
    private StackProducer object;

    /**
     * The type the object is being tested against.
     */
    private final Klass checkType;

    /**
     * Creates a <code>InstanceOf</code> instance representing an instruction
     * that pops an object from the operand stack, tests to see if it is
     * assignable to a specified type and pushes the boolean result of the
     * test back to the stack.
     *
     * @param checkType  the type the object is being tested against
     * @param object     the object whose type is being tested
     */
    public InstanceOf(Klass checkType, StackProducer object) {
        super(Klass.INT);
        this.object = object;
        this.checkType = checkType;
    }

    /**
     * Gets the object whose type is being tested.
     *
     * @return  the object whose type is being tested
     */
    public StackProducer getObject() {
        return object;
    }

    /**
     * Gets the type the object is being tested against.
     *
     * @return the type the object is being tested against
     */
    public Klass getCheckType() {
        return checkType;
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
        return checkType;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doInstanceOf(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        object = visitor.doOperand(this, object);
    }
}