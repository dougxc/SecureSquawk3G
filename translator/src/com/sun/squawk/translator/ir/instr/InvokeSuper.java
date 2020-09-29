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
 * An instance of <code>InvokeSuper</code> represents an instruction that
 * invokes a virtual method where the method look up starts with the vtable
 * of the superclass of the receiver.
 *
 * @author  Doug Simon
 */
public final class InvokeSuper extends Invoke {

    /**
     * Creates an <code>InvokeSuper</code> representing an instruction
     * that invokes a virtual method where the method look up starts
     * with the vtable of a fixed class (as opposed to the class of the
     * receiver). This is used to implement invocation of private
     * methods and methods of the superclass of the current class.
     *
     * @param  method      the method invoked
     * @param  parameters  the parameters passed to the invocation
     */
    public InvokeSuper(Method method, StackProducer[] parameters) {
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
        visitor.doInvokeSuper(this);
    }
}
