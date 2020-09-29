/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir.instr;

/**
 * This interface is implemented by <code>Instruction</code> subclasses that
 * reference a field of an object or class.
 *
 * @author  Doug Simon
 */
public interface FieldAccessor {

    /**
     * Gets the referenced field.
     *
     * @return field referenced by this instruction
     */
    public Field getField();
}