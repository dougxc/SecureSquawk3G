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
 * An instance of <code>IncDecLocal</code> represents an instruction that
 * adjusts the value of an integer typed local variable by 1 or -1.
 */
public final class IncDecLocal extends Instruction implements LocalVariable, Mutator {

    /**
     * The local containing the value that is adjusted.
     */
    private final Local local;

    /**
     * Specifies if this instruction increments or decrements the
     * local variable.
     */
    private final boolean increment;

    /**
     * Creates a <code>IncDecLocal</code> instance representing an instruction
     * that adjusts the value of an integer typed local variable by 1 or -1.
     *
     * @param local      the local variable adjusted by the instruction
     * @param increment  true if the adjustment is 1, false if it is -1
     */
    public IncDecLocal(Local local, boolean increment) {
        this.local = local;
        this.increment = increment;
    }

    /**
     * Determines if this instruction increments or decrements the
     * local variable.
     *
     * @return true if this instruction increments the local variable, false
     *         otherwise
     */
    public boolean isIncrement() {
        return increment;
    }

    /**
     * {@inheritDoc}
     */
    public Local getLocal() {
        return local;
    }

    /**
     * {@inheritDoc}
     */
    public Klass getMutationType() {
        return local.getType();
    }

    /**
     * Returns <code>true</code> to indicate that this instruction writes a
     * value to the referenced local variable.
     *
     * @return  false
     */
    public boolean writesValue() {
        return true;
    }

    /**
     * Returns <code>true</code> to indicate that this instruction reads a
     * value from the referenced local variable.
     *
     * @return true
     */
    public boolean readsValue() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doIncDecLocal(this);
    }
}
