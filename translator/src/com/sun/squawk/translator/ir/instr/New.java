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
import com.sun.squawk.translator.ci.UninitializedObjectClass;

/**
 * An instance of <code>New</code> represents an instruction that creates
 * a new instance of a specified class and pushes it to the operand stack.
 *
 * @author  Doug Simon
 */
public final class New extends StackProducer {

    /**
     * Creates a <code>New</code> instance representing an instruction that
     * creates a new instance of a specified class and pushes it to the
     * operand stack
     *
     * @param type  the class of the new instance
     */
    public New(UninitializedObjectClass type) {
        super(type);
    }

    /**
     * Gets the absolute runtine type for the instruction. In all sane code the
     * 'new' instruction is paired with an invokespecial which causes the type of
     * the 'new' instruction to change from an UninitializedObjectClass to a real
     * runtime type. However there are TCK tests that do not do this and for these
     * cases the declaired type from the class file constqnt pool is returned.
     *
     * @return the runtime type.
     */
    public Klass getRuntimeType() {
        Klass klass = getType();
        if (klass instanceof UninitializedObjectClass) {
            klass = ((UninitializedObjectClass)klass).getInitializedType();
        }
        return klass;
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
        return getRuntimeType();
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doNew(this);
    }

}