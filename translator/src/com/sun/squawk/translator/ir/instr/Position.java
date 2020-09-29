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
 * An instance of <code>Position</code> is a pseudo instruction that
 * represents a logical instruction position whose physical address may change
 * as the bytecodes are transformed from JVM bytecodes to Squawk
 * bytecodes. These positions are referenced by sub-attributes of
 * a "Code" attribute such as a "LocalVariableTable" or "LineNumberTable"
 * attribute.
 *
 * @author  Doug Simon
 */
public final class Position extends Instruction implements PseudoInstruction {

    /**
     * Creates a <code>Position</code> instance to represent
     * a logical instruction position whose physical address may change
     * as the bytecodes are transformed from JVM bytecodes to Squawk
     * bytecodes.
     *
     * @param offset the address of this position in the enclosing JVM bytecodes
     */
    public Position(int offset) {
        setBytecodeOffset(offset);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doPosition(this);
    }

}
