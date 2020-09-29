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
 * This interface is implemented by <code>Instruction</code> subclasses whose
 * runtime semantics involves mutating a memory location such as writing to a
 * local variable, pushing to the operand stack or writing to a field.
 *
 * @author  Doug Simon
 */
public interface Mutator {

    /**
     * Gets the type of the value written to a memory location by this instruction.
     *
     * @return  the type of the value written to a memory location by this instruction
     */
    public Klass getMutationType();
}
