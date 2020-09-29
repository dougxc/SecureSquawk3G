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
 * An instance of <code>LookupSwitch</code> represents an instruction that
 * pops a value off the operand stack and matches it against a set of
 * values in a jump table. If a match is found, then the flow of control is
 * transferred to the address associated with the matched entry. Otherwise
 * the flow of control is transferred to a default address.
 *
 * @author  Doug Simon
 */
public final class LookupSwitch extends Switch {

    /**
     * The case values.
     */
    private final int[] caseValues;

    /**
     * Creates a <code>LookupSwitch</code> representing an instruction that
     * transfers the flow of control based upon a key value, an associative
     * jump table indexed by the key and a default target for the case where
     * the key is out of the bounds of the table.
     *
     * @param  key           the value being switched upon
     * @param  casesCount    the number of cases
     * @param  defaultTarget the target for the default case
     */
    public LookupSwitch(StackProducer key, int casesCount, Target defaultTarget) {
        super(key, casesCount, defaultTarget);
        this.caseValues = new int[casesCount];
    }

    /**
     * Adds a case to the switch.
     *
     * @param index       the index of the entry in the jump table
     * @param caseValue   the match value for the case
     * @param target      the target address of the case
     */
    public void addTarget(int index, int caseValue, Target target) {
        super.addTarget(index, target);
        caseValues[index] = caseValue;
    }

    /**
     * Gets the case match constant values.
     *
     * @return the case match constant values
     */
    public int[] getCaseValues() {
        return caseValues;
    }

    /**
     * {@inheritDoc}
     *
     * This returns true as a lookup switch is implemented by a 'lookup'
     * instruction which is implemented in Java.
     */
    public boolean constrainsStack() {
        return true;
    }

    /**
     * Get a constant object used by the instruction.
     *
     * @return the object or null if there is none
     */
    public Object getConstantObject() {
        return caseValues;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doLookupSwitch(this);
    }

}