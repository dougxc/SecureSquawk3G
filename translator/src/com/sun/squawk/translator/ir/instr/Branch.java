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
 * An instance of <code>Branch</code> represents an instruction that transfers
 * the flow of control to a specified address.<p>
 *
 * This class is also the base of all the classes that represent a transfer of
 * flow control to one or more targets.
 *
 * @author  Doug Simon
 */
public class Branch extends Instruction {

    /**
     * The address to which the flow of control is transferred.
     */
    private final Target target;

    /**
     * True if the branch is forward.
     */
    private final boolean isForward;

    /**
     * Creates a <code>Branch</code> instance representing an instruction that
     * transfers the flow of control to a specified address.
     *
     * @param target the address to which the flow of control is transferred
     */
    public Branch(Target target) {
        this.target = target;
        isForward = target.getTargetedInstruction() == null;
    }

    /**
     * Gets the address to which the flow of control is transferred.
     *
     * @return the address to which the flow of control is transferred
     */
    public Target getTarget() {
        return target;
    }

    /**
     * Test to see if the branch goes forward.
     *
     * @return true if it is
     */
    public boolean isForward() {
        return isForward;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean constrainsStack() {
        //return !isForward;
        return true; // For Cristina's compiler
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doBranch(this);
    }
}
