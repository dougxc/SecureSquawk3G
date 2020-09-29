/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir.instr;

import com.sun.squawk.translator.ci.ConstantPool;

/**
 * An instance of <code>ConstantObject</code> represents an instruction that
 * pushes an object constant (such as a <code>String</code> constant or
 * <code>Klass</code> reference) onto the operand stack.
 *
 * @author  Doug Simon
 */
public final class ConstantObject extends Constant {

    /**
     * Creates a <code>ConstantObject</code> instance representing the
     * loading of a constant object value to the operand stack.
     *
     * @param type   the type of the value
     * @param value  the object value
     */
    public ConstantObject(Klass type, Object value) {
        super(type, ConstantPool.CONSTANT_Object, value);
    }

    /**
     * {@inheritDoc}
     *
     * @return  true if the constant value is 0L
     */
    public boolean isDefaultValue() {
        return value == null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the object
     */
    public Object getConstantObject() {
        return value;
    }
}
