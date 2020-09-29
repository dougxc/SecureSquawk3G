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
 * An instance of <code>FindSlot</code> represents an instruction that creates
 * the virtual slot number for an invoke interface from an interface name and
 * the runtime receiver object.
 *
 * @author  Nik Shaylor
 */
public final class FindSlot extends StackProducer {

    /**
     * The interface method invoked.
     */
    private final Method method;

    /**
     * The instruction producing the receiver.
     */
    private StackProducer receiver;

    /**
     * Creates a <code>FindSlot</code> instance representing an instruction that
     * creates the virtual slot number for an invoke interface.
     *
     * @param  method   the method invoked
     * @param  receiver the instruction producing the receiver
     */
    public FindSlot(Method method, StackProducer receiver) {
        super(Klass.INT);
        this.method   = method;
        this.receiver = receiver;
    }

    /**
     * Gets the interface invoked by this instruction.
     *
     * @return the method invoked by this instruction
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Get the instruction producing the receiver.
     *
     * @return  the instruction producing the receiver
     */
    public StackProducer getReceiver() {
        return receiver;
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
        return getMethod().getDefiningClass();
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doFindSlot(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        receiver = visitor.doOperand(this, receiver);
    }
}