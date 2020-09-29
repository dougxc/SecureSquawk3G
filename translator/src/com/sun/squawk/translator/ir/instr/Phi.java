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
 * This represents a point in a method that is the explicit target of a
 * control flow instruction. If there is more than one control flow
 * instruction in the method that targets this point or if the instruction
 * immediately preceeding this point lets control flow fall through, then
 * a <code>Phi</code> instance also represents a merge of control flow.
 *
 * @author  Doug Simon
 */
public final class Phi extends Instruction implements TargetedInstruction {

    /**
     * The targeted address represented by this object.
     */
    private final Target target;

    /**
     * Creates an instance of <code>Phi</code> to represent a point in a
     * method that is the explicit target of one or more control flow
     * instructions.
     *
     * @param target  the object representing the targeted address
     */
    public Phi(Target target) {
        this.target = target;
        target.setTargetedInstruction(this);
    }

    /**
     * {@inheritDoc}
     */
    public Target getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doPhi(this);
    }
}
