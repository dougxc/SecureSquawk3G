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
import com.sun.squawk.vm.CID;

/**
 * This class represents a local variable that either existed in a
 * Java bytecode method or was allocated as a result or transforming
 * that method into Squawk bytecodes.
 *
 * @author  Doug Simon
 */
public final class Local {

    /**
     * The type of the local variable.
     */
    private final Klass type;

    /**
     * The index of the local variable. This will be index assigned by javac
     * for a real local variable and some abritrary negative number for
     * a local variable representing a stack slot.
     */
    private final int javacIndex;

    /**
     * The offset to the local if it is a parameter (and -1 if it is not a parameter).
     */
    private int parameterIndex;

    /**
     * The activation slot for the local.
     */
    private Slot slot;

    /**
     * The last instruction in the method to use the local.
     */
    private Instruction lastUse;

    /**
     * Flag to say the local is dead.
     */
    private boolean isDead;

    /**
     * Creates a new local variable.
     *
     * @param   type        the type of the local variable
     * @param   javacIndex  the index of the local variable. This will be negative for a local variable representing a stack slot.
     * @param   isParameter true if the local is a parameter
     */
    public Local(Klass type, int javacIndex, boolean isParameter) {
        Assert.that(Frame.getLocalTypeFor(type) == type);
        this.type        = type;
        this.javacIndex  = javacIndex;
        if (isParameter) {
            this.parameterIndex = javacIndex;
        } else {
            this.parameterIndex = -1;
        }
    }

    /**
     * Gets the type of this local variable.
     *
     * @return  the type of this local variable
     */
    public Klass getType() {
        return type;
    }

    /**
     * Test to see if this is a two word variable.
     *
     * @return  true if it is
     */
    public boolean is64Bit() {
        return type == Klass.LONG | type == Klass.DOUBLE | (Klass.SQUAWK_64 ? type.isSquawkPrimitive() : false);
    }

    /**
     * Gets the javac asssigned index of the local variable (or negative value if a stack slot).
     *
     * @return  the index of the local variable
     */
    public int getJavacIndex() {
        return javacIndex;
    }

    /**
     * Test if the local is a parameter.
     *
     * @return true if the local is a parameter
     */
    public boolean isParameter() {
        return parameterIndex >= 0;
    }

    /**
     * Sets the index into the squawk parameter array for this variable
     */
    public void setParameterIndex(int index) {
        Assert.that(isParameter());
        parameterIndex = index;
    }

    /**
     * Set the last instruction to load the local.
     *
     * @param inst the instruction
     */
    public void setLastLoad(Instruction inst) {
        this.lastUse = inst;
    }

    /**
     * Setup the activation slot for a store operation.
     *
     * @param allocator the slot allocator
     */
    public void setSlotForStore(SlotAllocator allocator) {
        if (!isParameter()) {
            if (isDead) {
                throw new RuntimeException("Using dead variable "+javacIndex);
            }
            if (slot == null) {
                slot = allocator.allocate(type, javacIndex < 0);
            }
        }
    }

    /**
     * Setup the activation slot for a load operation.
     *
     * @param allocator the slot allocator
     */
    public void setSlotForLoad(SlotAllocator allocator, Instruction inst) {
        if (!isParameter()) {
            setSlotForStore(allocator);
            if (javacIndex < 0 && lastUse == inst) {
                allocator.free(slot);
//System.out.println("%%%%%%%%%%%%%%%%% free "+javacIndex);
                isDead = true;
            }
        }
    }

    /**
     * Setup the activation slot for an inc or dec operation.
     *
     * @param allocator the slot allocator
     */
    public void setSlotForIncDec(SlotAllocator allocator) {
        Assert.that(javacIndex >= 0);
        if (isParameter() == false) {
            setSlotForStore(allocator);
        }
    }

    /**
     * Test to see of the referenced local uses the same slot as this local.
     *
     * @param aLocal the local to test
     * @return true if they use the same local
     */
    public boolean hasSameSlotAs(Local aLocal) {
        return aLocal.slot == slot;
    }

    /**
     * Get the activation slot index for a local.
     *
     * @return the slot index
     */
    public int getSquawkLocalIndex() {
        Assert.that(slot != null);
        Assert.that(!isParameter());
        return slot.getSquawkIndex();
    }

    /**
     * Get the activation slot index for a parameter.
     *
     * @return the slot index
     */
    public int getSquawkParameterIndex() {
        Assert.that(isParameter());
        Assert.that(slot == null);
        return parameterIndex;
    }

    /**
     * Sets the flag specifying that this local variable is uninitialized at some
     * point in the method where a garbage collection may occur.
     *
     * @return true if the slot used for the local was set as needing clearing for the first time
     */
    public boolean setUninitializedAtGC() {
        Assert.that(!isParameter() && type == Klass.REFERENCE && slot != null);
        return slot.setNeedsClearing();
    }

    /**
     * Gets a string representation for the local variable. The value returned
     * will have the prefix "loc" for a javac local variable and the prefix "tmp" for
     * a stack local variable. The suffix indicates the type of the local variable.
     *
     * @return a string representation of the local variable
     */
    public String toString() {
        String suffix;
        switch (type.getClassID()) {
            case CID.INT:       suffix = "_i";  break;
            case CID.FLOAT:     suffix = "_f";  break;
            case CID.LONG:      suffix = "_l";  break;
            case CID.DOUBLE:    suffix = "_d";  break;
            default:            suffix = "_o";  break;
        }
        if (isParameter()) {
            return "parm" + getSquawkParameterIndex() + suffix;
        }
        if (slot != null) {
            int ndx = getSquawkLocalIndex();
            if (ndx >= 0) {
                suffix += " (slot " + ndx + ")";
            }
        }
        if (javacIndex < 0) {
            return "tmp" + (0-javacIndex) + suffix;
        } else {
            return "loc" +    javacIndex  + suffix;
        }
    }

}
