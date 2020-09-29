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
 * An instance of <code>GetField</code> represents an instruction that pops
 * an object from the operand stack and pushes the value of a field of the
 * object to the stack.
 *
 * @author  Doug Simon
 */
public class GetField extends StackProducer implements InstanceFieldAccessor {

    /**
     * The referenced field.
     */
    private final Field field;

    /**
     * The object encapsulating the field's value.
     */
    private StackProducer object;

    /**
     * Creates a <code>GetField</code> instance representing an instruction
     * that pops an object from the operand stack and pushes the value of a
     * field of the object to the stack.
     *
     * @param field   the referenced field
     * @param object  the object encapsulating the field's value
     */
    public GetField(Field field, StackProducer object) {
        super(field.getType());
        this.field = field;
        this.object = object;
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
        visitor.doGetField(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        object = visitor.doOperand(this, object);
    }
}