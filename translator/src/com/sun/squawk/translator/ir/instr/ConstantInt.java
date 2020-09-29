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
 * An instance of <code>ConstantInt</code> represents an instruction that
 * pushes an integer constant onto the operand stack.
 *
 * @author  Doug Simon
 */
public final class ConstantInt extends Constant {

    /**
     * Creates a <code>ConstantInt</code> instance representing the
     * loading of a constant integer value to the operand stack.
     *
     * @param value  the integer value (wrapped in a {@link Integer} object)
     */
    public ConstantInt(Integer value) {
        super(Klass.INT, ConstantPool.CONSTANT_Integer, value);
    }

    /**
     * {@inheritDoc}
     *
     * @return  true if the constant value is 0
     */
    public boolean isDefaultValue() {
        return ((Integer)value).intValue() == 0;
    }
}