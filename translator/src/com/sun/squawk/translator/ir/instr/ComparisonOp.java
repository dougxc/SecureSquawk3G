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
 * An instance of <code>ComparisonOp</code> represents an instruction that
 * pops two values off the operand stack, compares them and pushes the
 * result of the comparison.  This type of instruction is not directly
 * supported in Squawk and must be converted.
 *
 * @author  Doug Simon
 */
public final class ComparisonOp extends StackProducer {

    /**
     * The left operand of the operation.
     */
    private StackProducer left;

    /**
     * The left operand of the operation.
     */
    private StackProducer right;

    /**
     * The JVM opcode corresponding to this operation.
     */
    private final int opcode;

    /**
     * Creates a <code>ComparisonOp</code> instance representing a binary
     * comparison operation.
     *
     * @param   left    the left operand of the operation
     * @param   right   the right operand of the operation
     * @param   opcode  the JVM opcode corresponding to the operation
     */
    public ComparisonOp(StackProducer left, StackProducer right, int opcode) {
        super(Klass.INT);
        this.left   = left;
        this.right  = right;
        this.opcode = opcode;
    }

    /**
     * Gets the left operand of this comparison operation.
     *
     * @return the left operand of this comparison operation
     */
    public StackProducer getLeft() {
        return left;
    }

    /**
     * Gets the right operand of this comparison operation.
     *
     * @return the right operand of this comparison operation
     */
    public StackProducer getRight() {
        return right;
    }

    /**
     * Gets the JVM opcode corresponding this comparison operation.
     *
     * @return the JVM opcode corresponding this comparison operation
     */
    public int getJVMOpcode() {
        return opcode;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This could be optomized a little because most ComparisonOps are deleted immeadiatly
     * after they are created because they are followed by some kind of integer 'if'
     * instruction and this is optomized in the IRBuilder. This might be done by implementing
     * some kind of opcode lookahead.
     */
    public final boolean constrainsStack() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doComparisonOp(this);
    }
}