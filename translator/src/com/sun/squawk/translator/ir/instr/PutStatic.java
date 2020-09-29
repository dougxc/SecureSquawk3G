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
 * An instance of <code>PutStatic</code> represents an instruction that pops
 * a value from the operand stack and assigns the value to a static field of
 * a class.
 *
 * @author  Doug Simon
 */
public class PutStatic extends Instruction implements FieldAccessor, Mutator {

    /**
     * The referenced field.
     */
    private final Field field;

    /**
     * The value stored to the field.
     */
    private StackProducer value;

    /**
     * Creates a <code>PutStatic</code> instance representing an instruction
     * that pops a value from the operand stack and assigns the value to a
     * static field of a class.
     *
     * @param field   the referenced field
     * @param value   the value stored to the field
     */
    public PutStatic(Field field, StackProducer value) {
        super();
        this.field = field;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public Field getField() {
        return field;
    }

    /**
     * Gets the value stored to the field.
     *
     * @return the value stored to the field
     */
    public StackProducer getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Klass getMutationType() {
        return field.getType();
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
        visitor.doPutStatic(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        value = visitor.doOperand(this, value);
    }
}
