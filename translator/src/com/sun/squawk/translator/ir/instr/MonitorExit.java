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
 * An instance of <code>MonitorExit</code> represents an instruction that
 * pops a referenced typed value off the operand stack and releases a
 * lock on its monitor.
 *
 * @author  Doug Simon
 */
public final class MonitorExit extends Instruction {

    /**
     * The object whose monitor is released. This will be null if this
     * instruction is locking the monitor of the enclosing class to implement
     * static method synchronization.

     */
    private StackProducer object;

    /**
     * Creates a <code>MonitorExit</code> instance representing an instruction
     * that pops a referenced typed value off the operand stack and releases a
     * lock on its monitor.
     *
     * @param object  the object whose monitor is released or null when this
     *                instruction is implementing static method synchronization
     */
    public MonitorExit(StackProducer object) {
        this.object = object;
    }

    /**
     * Gets the object whose monitor is released.  This will be null if this
     * instruction is locking the monitor of the enclosing class to implement
     * static method synchronization.

     *
     * @return the object whose monitor is released or <code>null</code>
     */
    public StackProducer getObject() {
        return object;
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
        visitor.doMonitorExit(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        if (object != null) {
            object = visitor.doOperand(this, object);
        }
    }
}
