/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir.instr;

import com.sun.squawk.util.Assert;
import com.sun.squawk.translator.ci.ConstantPool;
import com.sun.squawk.translator.ir.*;


/**
 * This is the base class for the IR instructions that push a constant
 * value onto the stack.
 *
 * @author  Doug Simon
 */
public abstract class Constant extends StackProducer {

    /**
     * One of the <code>CONSTANT_...</code> values defined in
     * {@link ConstantPool}.
     */
    private final int tag;

    /**
     * The value loaded to the operand stack by this instruction.
     */
    protected final Object value;

    /**
     * Creates an instance of a subclass of <code>Constant</code> based on
     * a given constant value.
     *
     * @param   value  the constant value loaded to the operand stack
     * @return  an instance of a subclass of <code>Constant</code> that loads
     *                 values of the type of <code>value</code>
     */
    public static Constant create(Object value) {
        if (value == null) {
            return new ConstantObject(Klass.NULL, null);
        }
        if (value instanceof String) {
            return new ConstantObject(Klass.STRING, value);
        }
        if (value instanceof Integer) {
            return new ConstantInt((Integer)value);
        }
        if (value instanceof Long) {
            return new ConstantLong((Long)value);
        }
/*if[FLOATS]*/
        if (value instanceof Double) {
            return new ConstantDouble((Double)value);
        }
        if (value instanceof Float) {
            return new ConstantFloat((Float)value);
        }
/*end[FLOATS]*/


        throw Assert.shouldNotReachHere();

//        Klass type = Translator.getClass(value.getClass().getName(), false);
//        return new ConstantObject(type, value);
    }

    Constant(Klass type, int tag, Object value) {
        super(type);
        this.tag = tag;
        this.value = value;
    }

    /**
     * Gets the tag denoting which subclass of <code>Constant</code> this
     * object is an instance of.
     *
     * @return  one of the <code>CONSTANT_...</code> values defined in
     *          {@link ConstantPool}
     */
    public int getTag() {
        return tag;
    }

    /**
     * Gets the constant value loaded by this instruction.
     *
     * @return the constant value loaded by this instruction
     */
    public Object getValue() {
        return value;
    }

    /**
     * Determines if the constant value is the default value for its type.
     *
     * @return  true if the encapsulated constant value is the default value
     *          for its type
     */
    public abstract boolean isDefaultValue();

    /**
     * {@inheritDoc}
     */
    public final void visit(InstructionVisitor visitor) {
        visitor.doConstant(this);
    }

}
