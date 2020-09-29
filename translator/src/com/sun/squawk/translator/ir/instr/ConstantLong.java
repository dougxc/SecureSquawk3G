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
 * An instance of <code>ConstantLong</code> represents an instruction that
 * pushes a long constant onto the operand stack.
 *
 * @author  Doug Simon
 */
public final class ConstantLong extends Constant {

    /**
     * Creates a <code>ConstantLong</code> instance representing the
     * loading of a constant long value to the operand stack.
     *
     * @param value  the long value (wrapped in a {@link Long} object)
     */
    public ConstantLong(Long value) {
        super(Klass.LONG, ConstantPool.CONSTANT_Long, value);
    }

    /**
     * {@inheritDoc}
     *
     * @return  true if the constant value is 0L
     */
    public boolean isDefaultValue() {
        return ((Long)value).longValue() == 0L;
    }
}
