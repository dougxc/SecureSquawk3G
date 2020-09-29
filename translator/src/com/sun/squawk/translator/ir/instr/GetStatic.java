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
 * An instance of <code>GetStatic</code> represents an instruction that pushes
 * the value of a static field of a class to the object to the operand stack.
 *
 * @author  Doug Simon
 */
public class GetStatic extends StackProducer implements FieldAccessor {

    /**
     * The referenced field.
     */
    private final Field field;

    /**
     * Creates a <code>GetStatic</code> instance representing an instruction
     * that pushes the value of a static field of a class to the object to
     * the operand stack.
     *
     * @param field   the referenced field
     */
    public GetStatic(Field field) {
        super(field.getType());
        this.field = field;
    }

    /**
     * {@inheritDoc}
     */
    public Field getField() {
        return field;
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
        return field.getDefiningClass();
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doGetStatic(this);
    }
}