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
 * Symbolic long registers to be placed on the shadow stack.
 * We keep track of the high and low registers of the long in Register fields.
 * The input long register is also a Register field but uses non-physical
 * register numbers, therefore, the need for storing the two parts in this class.
 *
 * @author  Cristina Cifuentes
 */

class SymbolicRegister64 extends SymbolicRegister implements Constants, ShadowStackConstants{

    private Register hi;   /**** I don't think the hi & low are being used any longer ***/
    private Register lo;

    public SymbolicRegister64(Register reg, Type type) {
        this(reg, type, false);
    }

    public SymbolicRegister64(Register reg, Type type, boolean spilled) {
        if (! reg.isLong())
            throw new RuntimeException("Not a long register");
        if (reg == EDXEAX) {
            hi = EDX;
            lo = EAX;
        } else if (reg == EBXECX) {
            hi = EBX;
            lo = ECX;
        } else if (reg == EDIESI) {
            hi = EDI;
            lo = ESI;
        }
        this.register = reg;
        this.type = type;
        this.spilled = spilled;
    }

    public int getRegisterSize() {
        return 64;
    }

    public boolean isTypeEquivalent(Type type) {
        if (type == Type.LONG || type == Type.REF) {  // ** check: only when REF is 64b
            return true;
        }
        return false;
    }

    public Register getRegisterHi() {
        return hi;
    }

    public Register getRegisterLo() {
        return lo;
    }

    public void setRegister(Register hi, Register lo) {
        this.hi = hi;
        this.lo = lo;
    }

    public void print() {
        System.err.print("SymbolicRegister64.  Register = ");
        register.print();
        System.err.print(", spilled = " + spilled + ", type = ");
        type.print();
    }

}

