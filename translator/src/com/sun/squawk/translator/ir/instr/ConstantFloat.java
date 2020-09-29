//if[FLOATS]
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
 * An instance of <code>ConstantFloat</code> represents an instruction that
 * pushes a float constant onto the operand stack.
 *
 * @author  Doug Simon
 */
public final class ConstantFloat extends Constant {

    /**
     * Creates a <code>ConstantFloat</code> instance representing the
     * loading of a constant float value to the operand stack.
     *
     * @param value  the float value (wrapped in a {@link Float} object)
     */
    public ConstantFloat(Float value) {
        super(Klass.FLOAT, ConstantPool.CONSTANT_Float, value);
    }

    /**
     * {@inheritDoc}
     *
     * @return  true if the constant value is 0L
     */
    public boolean isDefaultValue() {
        return ((Float)value).floatValue() == 0L;
    }
}
