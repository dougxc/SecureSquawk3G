/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;


/**
 * Symbolic local values (parameters and local variables) to be placed on the shadow stack
 *
 * @author  Cristina Cifuentes
 */

class SymbolicLocal extends SymbolicValueDescriptor implements ShadowStackConstants{

    private Local local;

    /**
     * Constructor
     *
     * @param loc  the local value
     */
    public SymbolicLocal(Local loc) {
        local = loc;
    }

    public int getSymbolicValueDescriptor() {
        return S_LOCAL;
    }

    /**
     * Return the local value
     *
     * @return  the local value
     */
    public Local getLocal() {
        return local;
    }

    public Type getType() {
        return ((XLocal)local).getType();
    }

    public void setType(Type newType) {
        ((XLocal)local).setType(newType);
    }

    public void print() {
        System.err.print("SymbolicLocal.  ");
        ((XLocal)local).print();
    }

}

