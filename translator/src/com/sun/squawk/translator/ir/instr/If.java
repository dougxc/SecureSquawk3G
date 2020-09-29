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
 * An instance of <code>If</code> represents an instruction that pops a value
 * off the operand stack, compares it against 0 or <code>null</code> and
 * transfers the flow of control to a specified address if the result of the
 * comparison is true.
 *
 * @author  Doug Simon
 */
public class If extends Branch {

    /**
     * The value that is compared against 0 or <code>null</code>.
     */
    private StackProducer value;

    /**
     * The Squawk opcode corresponding to this operation.
     */
    private final int opcode;

    /**
     * Creates a <code>If</code> instance representing an instruction that
     * pops a value off the operand stack, compares it against 0 or
     * <code>null</code> and transfers the flow of control to a specified
     * address if the result of the comparison is true.
     *
     * @param  value   the value that is compared against 0 or <code>null</code>
     * @param  opcode  the opcode denoting the semantics of the comparison
     * @param  target  the address to which the flow of control is transferred
     *                 if the comparison returns true
     */
    public If(StackProducer value, int opcode, Target target) {
        super(target);
        this.opcode = opcode;
        this.value = value;
    }

    /**
     * Gets the value that is compared against 0 or <code>null</code>.
     *
     * @return the value that is compared against 0 or <code>null</code>
     */
    public StackProducer getValue() {
        return value;
    }

    /**
     * Gets the Squawk opcode corresponding this conditional branch operation.
     *
     * @return the Squawk opcode corresponding this conditional branch operation
     */
    public final int getOpcode() {
        return opcode;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doIf(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        value = visitor.doOperand(this, value);
    }
}