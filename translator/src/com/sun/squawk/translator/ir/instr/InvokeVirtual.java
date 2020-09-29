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
 * An instance of <code>InvokeVirtual</code> represents an instruction that
 * invokes a virtual method.
 *
 * @author  Doug Simon
 */
public final class InvokeVirtual extends Invoke {

    /**
     * Creates an <code>InvokeVirtual</code> representing an instruction
     * that invokes a virtual method.
     *
     * @param  method      the method invoked
     * @param  parameters  the parameters passed to the invocation
     */
    public InvokeVirtual(Method method, StackProducer[] parameters) {
        super(method, parameters);
    }

    /**
     * {@inheritDoc}
     */
    boolean pushesClassOfMethod() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doInvokeVirtual(this);
    }

}
