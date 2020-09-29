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
 * An instance of <code>ConstantDouble</code> represents an instruction that
 * pushes a double constant onto the operand stack.
 *
 * @author  Doug Simon
 */
public final class ConstantDouble extends Constant {

    /**
     * Creates a <code>ConstantDouble</code> instance representing the
     * loading of a constant double value to the operand stack.
     *
     * @param value  the double value (wrapped in a {@link Double} object)
     */
    public ConstantDouble(Double value) {
        super(Klass.DOUBLE, ConstantPool.CONSTANT_Double, value);
    }

    /**
     * {@inheritDoc}
     *
     * @return  true if the constant value is 0L
     */
    public boolean isDefaultValue() {
        return ((Double)value).doubleValue() == 0D;
    }
}
