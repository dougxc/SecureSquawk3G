/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.compiler.asm.x86.*;

/**
 * Symbolic registers to be placed on the shadow stack
 * We use 0 as a "no register" value; all other integer numbers are
 * valid virtual/symbolic registers.
 *
 * *** For now, this has been made x86-specific.  We save register
 * names as per the x86 assembler code.
 *
 * @author  Cristina Cifuentes
 */

abstract class SymbolicRegister extends SymbolicValueDescriptor implements ShadowStackConstants{

    Register register;
    Type type;
    boolean spilled = false;

    /**
     * Constructor
     */
    public SymbolicRegister() {}

    public int getSymbolicValueDescriptor() {
        return S_REG;
    }

    public int getRegisterSize() {
        return 0;
    }

    public boolean isTypeEquivalent(Type type) {
        return false;
    }

    public Register getRegister() {
        return register;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type newType) {
        type = newType;
    }

    public boolean isSpilled() {
        return spilled;
    }

    public void resetSpilled() {
        spilled = false;
    }

    public void setSpilled() {
        spilled = true;
    }

    public void print() {
        System.err.println("SymbolicRegister - abstract");
    }

}

