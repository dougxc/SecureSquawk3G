/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir;

import com.sun.squawk.util.Assert;

/**
 * This class represents a slot for a local variable (which is not a parameter)
 * in a Squawk activation record.
 *
 * @author  Nik Shaylor
 */
final class Slot {

    /**
     * Flag to show the slot is free.
     */
    private boolean free;

    /**
     * Flag to show the slot is for a stack value.
     */
    private boolean isForStack;

    /**
     * Flag to show the slot must be zeroed at the start of the method.
     */
    private boolean needsClearing;

    /**
     * Type of the slot.
     */
    private final Klass type;

    /**
     * Index of the slot in the activation record.
     */
    private int squawkIndex = -1;

    /**
     * Constructor.
     *
     * @param type the type of the slot
     */
    Slot(Klass type, boolean isForStack) {
        this.type       = type;
        this.isForStack = isForStack;
    }

    /**
     * Get the slot type.
     *
     * @return the type
     */
    Klass getType() {
        return type;
    }

    /**
     * Test to see if the slot is free.
     *
     * @return true if it is
     */
    boolean isFree() {
        return free;
    }

    /**
     * Test to see if the slot is for a stack value.
     *
     * @return true if it is
     */
    boolean isForStack() {
        return isForStack;
    }

    /**
     * Set the slot free or not free.
     *
     * @param free true if free
     */
    void setFree(boolean free) {
        Assert.that(isForStack);
        this.free = free;
    }

    /**
     * Get the index in to the Squawk activation record.
     *
     * @return the slot index
     */
    int getSquawkIndex() {
        return squawkIndex;
    }

    /**
     * Set the index in to the Squawk activation record.
     *
     * @param index the slot index
     */
    void setSquawkIndex(int index) {
        Assert.that(index != 0, "slot 0 is reserved for method pointer");
        squawkIndex = index;
    }

    /**
     * Mark the slot as needing clearing at start of the method.
     *
     * @return true if the slot was set as needing clearing for the first time
     */
    boolean setNeedsClearing() {
        if (needsClearing) {
            return false;
        } else {
            needsClearing = true;
            return true;
        }
    }

    /**
     * Test to see if the slot needing clearing at start of the method.
     *
     * @return true if it does
     */
    boolean needsClearing() {
        return needsClearing;
    }

}
