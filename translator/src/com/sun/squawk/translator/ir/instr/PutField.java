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
 * An instance of <code>PutField</code> represents an instruction that pops
 * an object and a value from the operand stack and assigns the value to a
 * field of the object.
 *
 * @author  Doug Simon
 */
public class PutField extends Instruction implements InstanceFieldAccessor, Mutator {

    /**
     * The referenced field.
     */
    private final Field field;

    /**
     * The value stored to the field.
     */
    private StackProducer value;

    /**
     * The object encapsulating the field's value.
     */
    private StackProducer object;

    /**
     * Creates a <code>PutField</code> instance representing an instruction
     * that pops an object and a value from the operand stack and assigns
     * the value to a field of the object.
     *
     * @param field   the referenced field
     * @param object  the object encapsulating the field's value
     * @param value   the value stored to the field
     */
    public PutField(Field field, StackProducer object, StackProducer value) {
        super();
        this.field = field;
        this.object = object;
        this.value = value;
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
    public StackProducer getObject() {
        return object;
    }

    /**
     * {@inheritDoc}
     */
    public Klass getMutationType() {
        return field.getType();
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
    public boolean mayCauseGC(boolean isStatic) {
        Instruction prev = getPrevious();
        if (prev instanceof LoadLocal) {
            Local local = ((LoadLocal)prev).getLocal();
            if (!isStatic && local.isParameter() && local.getSquawkParameterIndex() == 0) {
                return false; // will become a this_getfield for a virtual function
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doPutField(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        object = visitor.doOperand(this, object);
        value = visitor.doOperand(this, value);
    }
}
