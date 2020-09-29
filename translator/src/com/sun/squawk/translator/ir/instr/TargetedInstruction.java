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
 * This interface is implemented by an <code>Instruction</code> that
 * is at an address for which there is a {@link Target stack map entry}.
 * Such an instruction is also a basic block entry point.
 *
 * @author  Doug Simon
 */
public interface TargetedInstruction {

    /**
     * Gets the target associated with this instruction.
     *
     * @return the target associated with this instruction
     */
    public Target getTarget();
}