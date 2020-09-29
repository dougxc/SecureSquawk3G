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
 * This interface is implemented by <code>Instruction</code> subclasses that
 * reference a local variable.
 *
 * @author  Doug Simon
 */
public interface LocalVariable {

    /**
     * Determines if this instruction writes a value to the referenced
     * local variable.
     *
     * @return true if this instruction writes a value to the referenced local variable
     */
    public boolean writesValue();

    /**
     * Determines if this instruction reads a value from the referenced
     * local variable.
     *
     * @return true if this instruction reads a value from the referenced local variable
     */
    public boolean readsValue();

    /**
     * Gets the local variable referenced by this instruction.
     *
     * @return the local variable referenced by this instruction
     */
    public Local getLocal();
}
