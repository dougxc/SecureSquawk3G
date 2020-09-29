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
 * We save register names as per the x86 assembler code.
 *
 * @author  Cristina Cifuentes
 */

class SymbolicRegister32 extends SymbolicRegister implements ShadowStackConstants{

    public SymbolicRegister32(Register reg, Type type) {
        register = reg;
        this.type = type;
    }

    public SymbolicRegister32(Register reg, Type type, boolean spilled) {
        register = reg;
        this.type = type;
        this.spilled = spilled;
    }

    public int getRegisterSize() {
        return 32;
    }

    public boolean isTypeEquivalent(Type type) {
        if (type == Type.INT || type == Type.OOP || type == Type.REF) { // ** check REF is 32b
            return true;
        }
        return false;
    }

    public void setRegister(Register reg) {
        register = reg;
    }

    public void print() {
        System.err.print("SymbolicRegister32.  Register = ");
        register.print();
        System.err.print(", spilled = " + spilled + ", type = ");
        type.print();
    }

}

