/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.*;

/**
 * Class that defines a local variable that is created as a part of a method's activation record.
 *
 * @author   Nik Shaylor
 */
public class ActivationSlot implements Types {

    /**
     * Flag that says that the slots must only be resued for the same type.
     */
    static boolean strictSlotReuse = false;

    /**
     * The type of the local variable.
     */
    private Type type;

    /**
     * Physical offset to the space allocated on the activation record.
     */
    private int offset;

    /**
     * true of the slot is for a parameter.
     */
    private boolean isParm;

    /**
     * The "in use" flag.
     */
    private boolean inUse;

    /**
     * Pointer to the next local.
     */
    private ActivationSlot next;

    /**
     * Construct a slot.
     *
     * @param type the type of the slot
     * @param offset the offset in words to the slot
     * @param isParm true of the slot is for a parameter
     */
    public ActivationSlot(Type type, int offset, boolean isParm) {
        this.inUse  = true;
        this.type   = type;
        this.offset = offset;
        this.isParm = isParm;
    }

    /**
     * Get the offset to the slot.
     *
     * @return the offset in words
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Get the slot type.
     *
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * Test to see if the activation slot is for a parameter.
     *
     * @return true if it is
     */
    public boolean isParm() {
        return isParm;
    }

    /**
     * Test to see if the activation slot is for an oop
     *
     * @return true if it is
     */
    public boolean isOop() {
        return type.isOop();
    }

    /**
     * Set the next pointer.
     *
     * @see #getNext
     * @param next the next activation slot in the list
     */
    public void setNext(ActivationSlot next) {
        this.next = next;
    }

    /**
     * Get the next activation slot in the list.
     *
     * @see #setNext
     * @return the next activation slot in the list
     */
    public ActivationSlot getNext() {
        return next;
    }

    /**
     * Get the last activation slot in the list.
     *
     * @return the last activation slot in the list
     */
    public ActivationSlot getLast() {
        if (next == null) {
            return this;
        }
        return next.getLast();
    }

    /**
     * In use flag.
     *
     * @param type the type of the slot to allocate
     * @return the new slot
     */
    public ActivationSlot alloc(Type type) {
        Assert.that(!isParm);

        /*
         * If this slot is not in use, and the slot size and oopness is the same as the
         * size and oopness of the type being allocated, then reset this slot type for
         * the one being allocated and reuse this slot for the new type.
         */
        if (
             !inUse &&
             this.type.getActivationSize() == type.getActivationSize() &&
             this.type.isOop() == type.isOop()
            ) {
            if (strictSlotReuse == false || this.type == type) {
                this.type = type;
                inUse = true;
                return this;
            }
        }

        /*
         * If there are no more slots then allocate a new one and add it to the list.
         */
        if (next == null) {
            next = new ActivationSlot(type, offset + this.type.getActivationSize(), false);
            return next;
        }

        /*
         * Try the next slot in the list. (Tail recursion would be be nice!)
         */
        return next.alloc(type);
    }

    /**
     * Get the count of locals allocaed.
     *
     * @return the number allocated
     */
    public int getTotalSlots() {
        if (next == null) {
            return offset + type.getActivationSize();
        }
        return next.getTotalSlots();
    }

    /**
     * Free the local.
     */
    public void free() {
        inUse = false;
    }

}