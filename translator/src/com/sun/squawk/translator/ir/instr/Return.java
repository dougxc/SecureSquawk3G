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
 * An instance of <code>Return</code> represents an instruction that returns
 * the flow of control to the caller of the method enclosing the instruction.
 *
 * @author  Doug Simon
 */
public class Return extends Instruction implements Mutator {

    /**
     * The returned value or null if this is a void return.
     */
    private StackProducer value;

    /**
     * Creates a Return instance representing an instruction that returns
     * the flow of control to the caller of the method enclosing the
     * instruction.
     *
     * @param value  the returned value or <code>null</code> if this is
     *               a void return
     */
    public Return(StackProducer value) {
        this.value = value;
    }

    /**
     * Gets the returned value or <code>null</code> if this is a void return.
     *
     * @return the returned value or <code>null</code> if this is a void return
     */
    public StackProducer getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Klass getMutationType() {
        return value == null ? Klass.VOID : value.getType();
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doReturn(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        if (value != null) {
            value = visitor.doOperand(this, value);
        }
    }
}
