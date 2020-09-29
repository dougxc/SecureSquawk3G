/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/**
 * <p>Title: Register Allocator</p>
 * <p>Description: Straight line scan register allocator for x86</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Sun Microsystems</p>
 * @author Cristina Cifuentes
 * @version 1.0
 */

package com.sun.squawk.compiler;

import java.io.PrintStream;
import com.sun.squawk.compiler.asm.x86.*;
import com.sun.squawk.compiler.asm.x86.Register;


class RegisterAllocator implements Constants {
    /**
     * List of registers in use during code generation
     */
    private boolean[] availRegs;

    /**
     * Number of registers used for register allocation, including long registers,
     * which are a combination of two general-purpose registers.
     * We currently allow the use of 6 general-purpose (physical) registers and
     * three long (abstract) registers.
     */
    private static int NUM_REGISTERS = 6;

    /**
     * Constructor
     */
    public RegisterAllocator() {
        availRegs = new boolean[NUM_REGISTERS];
        freeAllRegs();
    }

    /**
     * Finds the next available 32-bit register and returns it.
     *
     * @return a 32-bit register or NO_REG if none is available.
     */
    public Register nextAvailableRegister() {
        for (int i = 0; i < availRegs.length; i++) {
            if (availRegs[i] == true)
                return registerFor(i);
        }
        throw new RuntimeException("No register available; need to spill");
    }

    /**
     * Finds the next available 32-bit register that has a byte form.
     *
     * @return 32-bit register
     */
    private Register nextAvailablePartRegister() {
        for (int i = 0; i < availRegs.length; i++) {
            if ((availRegs[i] == true) && (registerHasShortForm(i))) {
                return registerFor(i);
            }
        }
        throw new RuntimeException("No register available; need to spill");
    }

    public Register nextAvailableByteRegister() {
        return nextAvailablePartRegister();
    }

    public Register nextAvailableShortRegister() {
        return nextAvailablePartRegister();
    }

    public Register nextAvailableLongRegister() {
        if (availRegs[indexForRegister(EDI)] && availRegs[indexForRegister(ESI)])
            return EDIESI;
        else if (availRegs[indexForRegister(EBX)] && availRegs[indexForRegister(ECX)])
            return EBXECX;
        else if (availRegs[indexForRegister(EDX)] && availRegs[indexForRegister(EAX)])
            return EDXEAX;
        else
            throw new RuntimeException("No long register available; need to use local");
    }

    private void freePhysicalReg(Register reg) {
        int index = indexForRegister(reg);
        if (index != -1) {
            availRegs[index] = true;
            //System.err.print("\tFree register: "); printReg(reg);
        }
    }

    public void freeReg(Register reg) {
        if (!reg.isLong()) {
            freePhysicalReg(reg);
        } else {
            freePhysicalReg(registerHi(reg));
            freePhysicalReg(registerLo(reg));
        }
    }

    public void freeAllRegs() {
        for (int i = 0; i < availRegs.length; i++)
            availRegs[i] = true;
    }

    private void usePhysicalReg(Register reg) {
        int index = indexForRegister(reg);
        if (index != -1) {
            availRegs[index] = false;
            //System.err.print("\tUse register: "); printReg(reg);
        }
    }

    public void useReg(Register reg) {
        if (!reg.isLong()) {
            usePhysicalReg(reg);
        } else {
            usePhysicalReg(registerHi(reg));
            usePhysicalReg(registerLo(reg));
        }
    }

    public boolean regIsFree(Register reg) {
        int index = indexForRegister(reg);
        if (index != -1 && availRegs[index] == false)
            return false;
        return true;
    }

    /**
     * The indeces given to these registers are not necessarily the same as
     * those in Constants.java, i.e., they are not the physical register numbers.
     * The numbering herein is based on heuristics, to try to avoid using int and
     * long registers that clash.
     */
    public Register registerFor(int index) {
        switch (index) {
            case 0:
                return EAX;
            case 1:
                return EDX;
            case 2:
                return ECX;
            case 3:
                return EBX;
            case 4:
                return ESI;
            case 5:
                return EDI;
            default:
                return NO_REG;
        }
    }

    public int indexForRegister(Register reg) {
        if (reg == EAX)
            return 0;
        else if (reg == EDX)
            return 1;
        else if (reg == ECX)
            return 2;
        else if (reg == EBX)
            return 3;
        else if (reg == ESI)
            return 4;
        else if (reg == EDI)
            return 5;
        else
            return -1;
    }

    public void printReg(Register reg) {
        if (reg == EAX)
            System.err.println("EAX");
        else if (reg == ECX)
            System.err.println("ECX");
        else if (reg == EDX)
            System.err.println("EDX");
        else if (reg == EBX)
            System.err.println("EBX");
        else if (reg == ESI)
            System.err.println("ESI");
        else if (reg == EDI)
            System.err.println("EDI");
        else
            System.err.println("No reg");
    }

    public Register registerHi(Register reg) {
        if (reg.isLong()) {
            if (reg == EDXEAX)
                return EDX;
            else if (reg == EBXECX)
                return EBX;
            else // EDIESI
                return EDI;
        }
        throw new RuntimeException(
            "Register hi: expecting a long register, received " + reg);
    }

    public Register registerLo(Register reg) {
        if (reg.isLong()) {
            if (reg == EDXEAX)
                return EAX;
            else if (reg == EBXECX)
                return ECX;
            else // EDIESI
                return ESI;
        }
        throw new RuntimeException(
            "Register lo: expecting a long register, received " + reg);
    }


// ** x86 SystemV ABI; move to separate interface
    public boolean ABIPreservedRegister(Register reg) {
        if ( (reg == EBX) || (reg == ESI) || (reg == EDI)) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the given register has a byte (8-bit) or short (16-bit) form.
     *
     * @param regIndex the index of the register to be checked.
     * @return true if reg has a byte or short form, false otherwise.
     */
    private boolean registerHasShortForm(int regIndex) {
        Register reg = registerFor(regIndex);
        if (reg == EAX || reg == EBX || reg == ECX || reg == EDX) {
            return true;
        }
        return false;
    }

}