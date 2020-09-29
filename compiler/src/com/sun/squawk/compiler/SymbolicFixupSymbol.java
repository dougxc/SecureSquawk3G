/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;


/**
 * Symbolic fixup literal values to be placed on the shadow stack (to be
 * fixed by the linker)
 *
 * @author  Cristina Cifuentes
 */

class SymbolicFixupSymbol extends SymbolicValueDescriptor implements ShadowStackConstants{

    /* Symbol to be fixed */
    private String name;

    /**
     * Constructor
     */
    public SymbolicFixupSymbol(String name) {
        this.name = name;
    }

    public int getSymbolicValueDescriptor() {
        return S_FIXUP_SYM;
    }

    public Type getType() {
        return Type.REF;
    }

    public String getSymbol() {
        return name;
    }

    /**
     * Prints information about this symbolic fixup symbol.
     * This method is used for debugging purposes.
     */
    public void print() {
        System.err.print("SymbolicFixupSymbol.  Symbol name = " + name + ", type = REF");
    }

}

