/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;


/**
 * Symbolic values to be placed on the shadow stack
 *
 * @author	Cristina Cifuentes
 */

abstract class SymbolicValueDescriptor implements ShadowStackConstants{

    /**
     * Constructor
     */
    public SymbolicValueDescriptor() {}

    /**
     * Return symbolic value descriptor
     *
     * @return  the symbolic value descriptor constant
     */
    public int getSymbolicValueDescriptor() {
        return S_OTHER;
    }

    public Type getType() {
        return Type.VOID;
    }

    public boolean isTypeEquivalent(Type type) {
        return false;
    }

    // for debugging purposes, print to stderr
    public void print() {
        System.err.println("Symbolic value descriptor - abstract");
    }

}


