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
 * An instance of <code>IfCompare</code> represents an instruction that pops
 * two values off the operand stack, compares them against each other and
 * transfers the flow of control to a specified address if the result of the
 * comparison is true.
 *
 * @author  Doug Simon
 */
public final class IfCompare extends If {

    /**
     * The right hand value of the comparison.
     */
    private StackProducer right;

    /**
     * Creates a <code>If</code> instance representing an instruction that
     * pops two values off the operand stack, compares them against each
     * other and transfers the flow of control to a specified address if the
     * result of the comparison is true
     *
     * @param  left    the left hand value of the comparison
     * @param  right   the right hand value of the comparison
     * @param  opcode  the opcode denoting the semantics of the comparison
     * @param  target  the address to which the flow of control is transferred
     *                 if the comparison returns true
     */
    public IfCompare(StackProducer left, StackProducer right, int opcode, Target target) {
        super(left, opcode, target);
        this.right = right;
    }

    /**
     * Gets the left hand value of the comparison.
     *
     * @return the left hand value of the comparison
     */
    public StackProducer getLeft() {
        return getValue();
    }

    /**
     * Gets the right hand value of the comparison.
     *
     * @return the right hand value of the comparison
     */
    public StackProducer getRight() {
        return right;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doIfCompare(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        super.visit(visitor);
        right = visitor.doOperand(this, right);
    }
}