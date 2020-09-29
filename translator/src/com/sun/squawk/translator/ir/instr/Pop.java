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
 * An instance of <code>Pop</code> represents an instruction that pops one
 * or two words off the operand stack.
 *
 * @author  Doug Simon
 */
public class Pop extends Instruction {

    /**
     * Instruction that pushed the value to be popped.
     */
    private StackProducer value;

    /**
     * Creates an instance of <code>Pop</code> that represents an instruction
     * that pops one or two words off the operand stack
     *
     * @param value the instruction that pushed the value being popped
     */
    public Pop(StackProducer value) {
        this.value = value;
    }

    /**
     * Gets the instruction that pushed the value being popped.
     *
     * @return the instruction that pushed the value being popped
     */
    public StackProducer value() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doPop(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        value = visitor.doOperand(this, value);
    }
}