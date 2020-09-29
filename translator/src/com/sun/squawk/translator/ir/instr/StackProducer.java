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
import com.sun.squawk.translator.ir.*;

/**
 * This is the base class of all the instruction that push a value to the
 * operand stack.
 *
 * @author  Doug Simon
 */
public abstract class StackProducer extends Instruction implements Mutator {

    /**
     * The type of the value pushed to the stack by this instruction.
     */
    private Klass type;

    /**
     * Counter of number of times the StackProducer is used.
     */
    private int useCount;

    /**
     * Creates an instance of an instruction that writes to the operand stack.
     *
     * @param type  the type of the value pushed to the stack by the instruction
     */
    public StackProducer(Klass type) {
        Assert.that(type != null);
        this.type = type;
    }

    /**
     * Gets the type of the value pushed to the stack by this instruction.
     *
     * @return  the type of the value pushed to the stack by the instruction
     */
    public final Klass getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public final Klass getMutationType() {
        return getType();
    }

    /**
     * Updates the type of this instruction which must currently be
     * assignable to UNINITIALIZED. That is, this instruction pushes an
     * uninitialized object to the operand stack.
     *
     * @param initializedType  the type of the uninitialized object after the
     *                         appropriate constructor has been invoked
     */
    public final void initializeType(Klass initializedType) {
        Assert.that(Klass.UNINITIALIZED.isAssignableFrom(type));
        type = initializedType;
    }


    /*---------------------------------------------------------------------------*\
     *                                 Duping                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Flags if the value pushed by this instruction was subquently manipulated
     * by one of the untyped stack manipulation instructions.
     */
    private boolean isDuped;

    /**
     * Sets the flag indicating that the value pushed by this instruction was
     * subquently manipulated by one of the untyped stack manipulation
     * instructions.
     */
    public void setDuped(Frame frame) {
        isDuped = true;
        if (!isSpilt()) {
            spill(frame.allocateLocalForSpill(this));
        }
    }

    /**
     * Unsets the flag indicating that the value pushed by this instruction was
     * subquently manipulated by one of the untyped stack manipulation
     * instructions.
     */
    public void cancelDuping() {
        Assert.that(isDuped == true);
        isDuped = false;
    }

    /**
     * Determines if the value pushed by this instruction was
     * subquently manipulated by one of the untyped stack manipulation
     * instructions.
     *
     * @return true the value pushed by this instruction was subquently
     *              manipulated by one of the untyped stack manipulation
     *              instructions
     */
    public boolean isDuped() {
        return isDuped;
    }


    /*---------------------------------------------------------------------------*\
     *                                Spilling                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * The local variable to which the value is spilt.
     */
    private Local spillLocal;

    /**
     * Determines whether or not the value pushed by this instruction
     * must be spilt.
     *
     * @return  true if this instruction spills
     */
    public boolean isSpilt() {
        return spillLocal != null;
    }

    /**
     * Determines whether or not the value pushed by this instruction
     * is really on the runtime stack or has been spilt.
     *
     * @return  true if this instruction's value is on the stack
     */
    public boolean isOnStack() {
        return !isSpilt();
    }

    /**
     * Sets the local variable to which the value is spilt.
     */
    public void spill(Local spillLocal) {
        this.spillLocal = spillLocal;
    }

    /**
     * Gets the local variable to which the value is spilt.
     *
     * @return the local variable to which the value is spilt
     */
    public Local getSpillLocal() {
        Assert.that(spillLocal != null, "producer doesn't spill");
        return spillLocal;
    }

    /**
     * Undoes the spilling.
     */
    public void cancelSpilling() {
        spillLocal = null;
    }

    /**
     * Increments the number of times the StackProducer is used.
     */
    public void incUseCount() {
        useCount++;
    }

    /**
     * Gets the the number of times the StackProducer is used.
     *
     * @return the number of times the StackProducer is used
     */
    public int getUseCount() {
        return useCount;
    }
}
