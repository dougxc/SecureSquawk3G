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
 * An instance of <code>CheckCast</code> represents an instruction that pops
 * an object from the operand stack, casts it to specified type and pushes it
 * back to the stack.
 *
 * @author  Doug Simon
 */
public final class CheckCast extends StackProducer {

    /**
     * The object whose type is cast.
     */
    private StackProducer object;

    /**
     * Creates a CheckCast instance representing an instruction that pops
     * an object from the operand stack, casts it to specified type and pushes
     * it back to the stack.
     *
     * @param  type    the type the object is cast to
     * @param  object  the object whose type is cast
     */
    public CheckCast(Klass type, StackProducer object) {
        super(type);
        this.object = object;
    }

    /**
     * Gets the object whose type is cast.
     *
     * @return the object whose type is cast
     */
    public StackProducer getObject() {
        return object;
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
        visitor.doCheckCast(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        object = visitor.doOperand(this, object);
    }
}