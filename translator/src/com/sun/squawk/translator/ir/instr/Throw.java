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
 * An instance of <code>Throw</code> represent s an instruction that
 * pops an object off the operand stack and throws it.
 *
 * @author  Doug Simon
 */
public final class Throw extends Instruction {

    /**
     * The object that is thrown.
     */
    private StackProducer throwable;

    /**
     * Creates a <code>Throw</code> instance representing an instruction that
     * pops an object off the operand stack and throws it.
     *
     * @param throwable  the object thrown
     */
    public Throw(StackProducer throwable) {
        this.throwable = throwable;
    }

    /**
     * Gets the object thrown by this instruction.
     *
     * @return the object thrown by this instruction
     */
    public StackProducer getThrowable() {
        return throwable;
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
    public void visit(InstructionVisitor visitor) {
        visitor.doThrow(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        throwable = visitor.doOperand(this, throwable);
    }

}