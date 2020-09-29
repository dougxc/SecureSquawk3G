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
 * An instance of <code>StoreLocal</code> represents an instruction that pops
 * a value off the operand stack and stores it to a local variable.
 *
 * @author  Doug Simon
 */
public final class StoreLocal extends Instruction implements LocalVariable, Mutator {

    /**
     * The value being stored.
     */
    private StackProducer value;

    /**
     * The local containing the value that is stored to.
     */
    private final Local local;

    /**
     * Flags that the referenced local is holding the value of <code>this</code>
     * in a virtual method.
     */
    private final boolean isThis;

    /**
     * Creates an instance of <code>StoreLocal</code> that pops a value from
     * the operand stack and stores it to a given local.
     *
     * @param local  the local to which the value is stored
     * @param value  the value stored to the local variable
     * @param isThis specifies whether or not <code>local</code> is holding
     *               the value of <code>this</code> in a virtual method
     */
    public StoreLocal(Local local, StackProducer value, boolean isThis) {
        this.local = local;
        this.value = value;
        this.isThis = isThis;
    }

    /**
     * {@inheritDoc}
     */
    public Local getLocal() {
        return local;
    }

    /**
     * Gets the value stored to the local variable.
     *
     * @return the value stored to the local variable
     */
    public StackProducer getValue() {
        return value;
    }

    /**
     * Determines if the local being loaded is holding the value of
     * <code>this</code> in a virtual method.
     *
     * @return  true if the referenced local holding the value of
     *          <code>this</code> in a virtual method
     */
    public boolean isThis() {
        return isThis;
    }

    /**
     * Returns <code>true</code> to indicate that a store writes a value
     * to the referenced local variable.
     *
     * @return  true
     */
    public boolean writesValue() {
        return true;
    }

    /**
     * Returns <code>false</code> to indicate that a store does not read a value
     * from the referenced local variable.
     *
     * @return  false
     */
    public boolean readsValue() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Klass getMutationType() {
        return local.getType();
    }

    ///**
    // * {@inheritDoc}
    // */
    //public boolean constrainsStack() {
    //    return true;
    //}

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doStoreLocal(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        value = visitor.doOperand(this, value);
    }
}
