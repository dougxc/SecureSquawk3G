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
 * reference a field of an object.
 *
 * @author  Doug Simon
 */
public interface InstanceFieldAccessor extends FieldAccessor {

    /**
     * Gets the instance encapsulating the field's value.
     *
     * @return the instance encapsulating the field's value
     */
    public StackProducer getObject();
}