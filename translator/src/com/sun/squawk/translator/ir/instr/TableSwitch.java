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
 * An instance of <code>TableSwitch</code> represents an instruction that
 * pops a value off the operand stack and matches it against a range of
 * values in a jump table. If a match is found, then the flow of control is
 * transferred to the address associated with the matched entry. Otherwise
 * the flow of control is transferred to a default address.
 *
 * @author  Doug Simon
 */
public final class TableSwitch extends Switch {

    /**
     * The lowest match value in the jump table.
     */
    private final int low;

    /**
     * The highest match value in the jump table.
     */
    private final int high;

    /**
     * Creates a <code>TableSwitch</code> representing an instruction that
     * transfers the flow of control based upon a key value, a jump table
     * indexed by the key and a default target for the case where the
     * key is out of the bounds of the table.
     *
     * @param  key           the value being switched upon
     * @param  low           the lowest match value in the jump table
     * @param  high          the highest match value in the jump table
     * @param  defaultTarget the target for the default case
     */
    public TableSwitch(StackProducer key, int low, int high, Target defaultTarget) {
        super(key, high-low+1, defaultTarget);
        this.low = low;
        this.high = high;
    }

    /**
     * Gets the lowest match value in the jump table.
     *
     * @return the lowest match value in the jump table
     */
    public int getLow() {
        return low;
    }

    /**
     * Gets the highest match value in the jump table.
     *
     * @return the highest match value in the jump table
     */
    public int getHigh() {
        return high;
    }

    /**
     * Adds a target corresponding to a case of the switch.
     *
     * @param  caseValue the match value of the case
     * @param  target    the target
     */
    public void addTarget(int caseValue, Target target) {
        super.addTarget(caseValue - low, target);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doTableSwitch(this);
    }

}