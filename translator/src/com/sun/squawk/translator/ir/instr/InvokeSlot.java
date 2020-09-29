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
 * An instance of <code>InvokeSlot</code> represents an instruction that
 * invokes a method that is specified by a method offset popped from the
 * operand stack. This kind of invoke is used to implement invocation of
 * interface methods by providing support for a minimal reflection capability.
 *
 * @author  Doug Simon
 */
public final class InvokeSlot extends Invoke {

    /**
     * Creates an <code>InvokeSlot</code> representing an instruction
     * that invokes a method that is specified by a method offset popped
     * from the operand stack.
     *
     * @param  method      the method invoked
     * @param  parameters  the parameters passed to the invocation
     */
    public InvokeSlot(Method method, StackProducer[] parameters) {
        super(method, parameters);
    }

    /**
     * {@inheritDoc}
     */
    boolean pushesClassOfMethod() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doInvokeSlot(this);
    }
}
