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
 * This is the base class for the IR instructions representing the Java bytecode
 * <i>lookupswitch</i> and <i>tableswitch</i> instructions.
 *
 * @author  Doug Simon
 */
public abstract class Switch extends Instruction {

    /**
     * The value being switched upon.
     */
    private StackProducer key;

    /**
     * The targets corresponding to each non-default case.
     */
    private final Target[] targets;

    /**
     * The target for the default case.
     */
    private final Target defaultTarget;

    /**
     * The padding calculated for the instruction during relocation.
     */
    private int padding;

    /**
     * Creates a <code>Switch</code> instance.
     *
     * @param  key           the value being switched upon
     * @param  targetsCount  the number of non-default cases
     * @param  defaultTarget the target for the default case
     */
    public Switch(StackProducer key, int targetsCount, Target defaultTarget) {
        this.key = key;
        this.targets = new Target[targetsCount];
        this.defaultTarget = defaultTarget;
    }

    /**
     * Adds a target corresponding to a case of the switch.
     *
     * @param  index   the index in the targets table
     * @param  target  the target
     */
    protected void addTarget(int index, Target target) {
        targets[index] = target;
    }

    /**
     * Gets the value being switched upon.
     *
     * @return the value being switched upon
     */
    public StackProducer getKey() {
        return key;
    }

    /**
     * Gets the targets corresponding to each non-default case.
     *
     * @return the targets corresponding to each non-default case
     */
    public Target[] getTargets() {
        return targets;
    }

    /**
     * Gets the target for the default case.
     *
     * @return the target for the default case
     */
    public Target getDefaultTarget() {
        return defaultTarget;
    }

    /**
     * Sets the padding calculated for this switch during relocation.
     *
     * @param  padding  the padding calculated for this switch
     */
    public void setPadding(int padding) {
        this.padding = padding;
    }

    /**
     * Gets the padding calculated for this switch during relocation.
     *
     * @return  the padding calculated for this switch
     */
    public int getPadding() {
        return padding;
    }

    /**
     * {@inheritDoc}
     */
    public boolean constrainsStack() {
        if (defaultTarget.isBackwardBranchTarget()) {
            return true;
        }
        for (int i = 0; i != targets.length; ++i) {
            if (targets[i].isBackwardBranchTarget()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        key = visitor.doOperand(this, key);
    }
}
