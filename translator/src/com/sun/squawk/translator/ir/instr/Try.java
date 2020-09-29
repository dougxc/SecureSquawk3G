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
 * An instance of <code>Try</code> is a pseudo instruction that
 * represents the position in an IR at which an exception handler becomes
 * active.
 *
 * @author  Doug Simon
 */
public class Try extends Instruction implements PseudoInstruction {

    /**
     * Creates an instance of <code>Try</code> to represent the point at
     * which an exception handler becomes active.
     */
    public Try() {
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doTry(this);
    }
}