/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;


/**
 * Symbolic object values to be placed on the shadow stack
 *
 * @author  Cristina Cifuentes
 */

class SymbolicObject extends SymbolicValueDescriptor implements ShadowStackConstants{

    /* Object being stored */
    private Object obj;

    /**
     * Constructor
     *
     * @param object the object being stored
     */
    public SymbolicObject(Object object) {
        obj = object;
    }

    public int getSymbolicValueDescriptor() {
        return S_OBJECT;
    }

    public Type getType() {
        return Type.INT;   /*** is this right? ***/
    }

    public Object getObject() {
        return obj;
    }

    public void print() {
        System.err.print("SymbolicObject.");
    }

}
