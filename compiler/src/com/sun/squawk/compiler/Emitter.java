/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.util.Hashtable;
import com.sun.squawk.util.Stack;
import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.x86.*;

/**
 * <p>Title: Emitter</p>
 * <p>Description: x86 assembly code emitter</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Sun Microsystems</p>
 * @author Cristina Cifuentes
 * @version 1.0
 *
 * This class is a temporary code emitter for the x86.  It will have to be
 * generalized to make use of an interface to support different machines and
 * different calling conventions, etc.
 *
 */
class Emitter implements Constants, Codes, ShadowStackConstants, Types {

    /**
     * The register allocator used by the compiler
     */
    private RegisterAllocator ralloc;

    /**
     * The compiler for which this emitter is generating code for
     */
    private Compiler comp;

    /**
     * The assembler to use for code emission
     */
    private Assembler asm;

    /**
     * The hashtable of comments (code address offsets -> strings)
     */
    private Hashtable comments;

    /**
     * The code buffer to use for the instructions being emitted
     */
    private CodeBuffer codeBuffer;

    /**
     * List of jumps to leave instruction
     */
    private UnboundJmp jmpsToLeave;

    /**
     * List of arrays of bytes (data) to be emitted after the leave instruction
     */
    private UnboundDataBytes arraysOfData;

    /**
     * The number of bytes needed for a jump instruction.
     */
    private static final int JUMPSIZE = 5; // **** platform specific ****

    /**
     * The address of the instruction that allocates space on the stack frame
     * for locals.
     */
    private int allocLocalBytesAddr;


    /**
     * Constructor
     */
    public Emitter(RegisterAllocator regAlloc, Compiler compiler) {
        ralloc = regAlloc;
        comp = compiler;
        codeBuffer = new CodeBuffer();   // default is 4K
        asm = new Assembler(codeBuffer);
        comments = new Hashtable();
        jmpsToLeave = null;
        arraysOfData = null;
    }

    public int[] relocate(int address) {
        return asm.relocate(address);
    }

    public void decode() {
        Disassembler dis = new Disassembler(codeBuffer, comments);
        dis.disassemble(0, codeBuffer.getCodeSize());
    }

    /**
     * CodeBuffer access functions
     */
    public int getCodeSize() {
        return codeBuffer.getCodeSize();
    }

    public byte[] getCode() {
        return codeBuffer.getBytes();
    }

    public Assembler getAssembler() {
        return asm;
    }

    /**
     * Creates a new assembler label
     *
     * @return a new assembler label
     */
    public ALabel newLabel(){
        return asm.newLabel();
    }

    /**
     * Gets the current code generation position.
     *
     * @return the code generation position.
     */
    public int getCodeOffset() {
        return codeBuffer.getCodePos() - codeBuffer.getCodeBegin();
    }

    public void bind(Label label) {
        XLabel xlabel = (XLabel)label;
        asm.bind(xlabel.getAssemblerLabel());
    }

    /**
     * Instructions
     */

    public void alloca(int numBytes) {
        asm.subl(ESP, numBytes);
    }

    public void alloca(Register numBytes) {
        asm.subl(ESP, numBytes);
        ralloc.freeReg(numBytes);
    }

    public void alloca(Address numBytes) {
        asm.subl(ESP, numBytes);
    }

    // this code makes use of knowledge of the location of the stack segment (SS)
    // slot in the activation record.  The location will always be pre-defined
    // by the compiler.  For x86, it's [ebp-10h].
    public void allocaSaveSS(int numBytes, Address addr) {
        alloca(numBytes);
        asm.movl(addr, ESP);
    }

    public void allocaSaveSS(Register numBytes, Address addr) {
        alloca(numBytes);
        asm.movl(addr, ESP);
    }

    public void allocaSaveSS(Address numBytes, Address addr) {
        alloca(numBytes);
        asm.movl(addr, ESP);
    }

    public void comment(String str) {
        Integer location = new Integer(codeBuffer.getCodePos());
        String comment = (String)comments.get(location);
        if (comment == null)
            comment = "";
        comment = comment + str + '\n';
        comments.put(location, comment);
    }

    public void enter(Label lab) {
        ralloc.freeAllRegs();
        if (lab != null) {
            bind(lab);
        }
        // emit enter code
        asm.pushl(EBP);
        asm.movl(EBP, ESP);

        /**
         * We may or may not have to reserve space for local variables, we
         * therefore allocate enough space to emit an instruction that can
         * allocate up to 2^16-1 bytes in the stack frame.
         * In x86, the encoding of such instruction, the subl instruction, differs
         * depending on the size of the operand: for small numbers, 3 bytes are
         * required, for large numbers, 6 bytes are required.
         * Since we do not know ahead of time which encoding will be used, we
         * emit a subl instruction that will take 6 bytes, each of the last 3 bytes
         * being 90, which is equivalent to the NOP instruction on x86.
         * The leave() method will do the backpatching at the end of the method.
         */
        allocLocalBytesAddr = codeBuffer.getCodePos();
        asm.subl(ESP, 0x909090FF);   // This subl instruction takes 6 bytes

        // We may want to preserve registers ebp, esi or edi (via saving to locals,
        // we can't use pushl)
    }

    public void leave(int localsOffset) {
        // emit binding of jumps to the leave instruction
        while (jmpsToLeave != null) {
            UnboundJmp jmp = jmpsToLeave;
            jmpsToLeave = jmpsToLeave.next;
            bind(jmp.label);
        }

        // emit leave code
        asm.movl(ESP, EBP);
        asm.popl(EBP);
        asm.ret(0);

        // emit back-patching code for locals space allocated in the stack frame
        int save = codeBuffer.getCodePos();
        codeBuffer.setCodePos(allocLocalBytesAddr);
        asm.subl(ESP, localsOffset); // This instruction may take 3 or 6 bytes
        codeBuffer.setCodePos(save);

        // emit binding of labels to data, followed by the data (arrays of bytes)
        while (arraysOfData != null) {
            UnboundDataBytes data = arraysOfData;
            arraysOfData = arraysOfData.next;

            if (data.object instanceof byte[]) {
                asm.align(1);
                bind(data.label);
                byte[] byteArray = (byte[])data.object;
                for (int i = 0; i < byteArray.length; i++) {
                    asm.emitByte(byteArray[i]);
                }
            }
            else if (data.object instanceof int[]) {
                asm.align(4);
                bind(data.label);
                int[] intArray = (int[])data.object;
                for (int i = 0; i < intArray.length; i++) {
                    asm.emitInt(intArray[i]);
                }
            }
            else if (data.object instanceof Label[]) {
                asm.align(4);
                bind(data.label);
                Label[] labelArray = (Label[])data.object;
                for (int i = 0; i < labelArray.length; i++) {
                    asm.emitLabel(((XLabel)labelArray[i]).getAssemblerLabel());
                }
            }
            else {
                System.out.println("Object not implemented in leave() code emission");
                throw new Error();
            }
        }
    }

    // allocates local MP, which is always pre-allocated at slot 0 in the
    // activation record
    // NB: for the E_REGISTER case, register eax holds the address of the
    // function to be stored at this slot.
    // For the E_ADDRESS case, the address of the method is stored at this
    // slot, even when there was no label assigned to the method.
    public void localMP(int specialPreambleCode, Label methodAddr) {
        switch (specialPreambleCode) {
            case Compiler.E_NONE:
                throw new Error();  // this should never happen; emit error msg
            case Compiler.E_NULL:
                asm.movl(new Address(EBP, -4), 0);
                break;
            case Compiler.E_REGISTER:
                asm.movl(new Address(EBP, -4), EAX);
                break;
            case Compiler.E_ADDRESS:
                Register reg = ralloc.nextAvailableRegister();
                asm.leal(reg, new Address(((XLabel)methodAddr).getAssemblerLabel()));
                asm.movl(new Address(EBP, -4), reg);
                break;
        }
    }

    // we use the convention of src and dst for all loads and ariths
    public void load(Address src, Register dst) {
        if (dst.isLong()) {
            asm.movl(ralloc.registerLo(dst), src);
            asm.movl(ralloc.registerHi(dst), src.offsetFrom(4));
        } else {
            asm.movl(dst, src);
        }
        ralloc.useReg(dst);
    }

    public void load(Register src, Address dst) {
        asm.movl(dst, src);
        ralloc.freeReg(src);
    }

    // loads a long register
    // *** should remove and merge into the previous method ***
    public void load(Register hi, Register lo, Address dst) {
        asm.movl(dst, lo);
        asm.movl(dst.offsetFrom(4), hi);
        ralloc.freeReg(hi);
        ralloc.freeReg(lo);
    }

    public void load(int literal, Address dst) {
        asm.movl(dst, literal);
    }

    // in x86 long values are stored in little endian mode, and the stack
    // grows downward, this means that the low part of a long is stored at
    // the given address and the high part is stored at the address + 4
    public void load(long literal, Address dst) {
        asm.movl(dst, (int)literal);
        asm.movl(dst.offsetFrom(4), (int)(literal>>32));
    }

    public void load(long literal, Register reg) {
        if (!reg.isLong())
            throw new RuntimeException("Load: expecting long register index");
        asm.movl(ralloc.registerLo(reg), (int)literal);
        asm.movl(ralloc.registerHi(reg), (int)(literal>>32));
    }

    public void load(Address addr, Address dst) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, addr);
        asm.movl(dst, reg);
    }

    public void load(Register src, Register dst) {
        if (dst.isLong() && src.isLong()) {
            asm.movl(ralloc.registerLo(dst), ralloc.registerLo(src));
            asm.movl(ralloc.registerHi(dst), ralloc.registerHi(src));
        }
        else
            asm.movl(dst, src);
        ralloc.freeReg(src);
        ralloc.useReg(dst);
    }

    public void load(int literal, Register dst) {
        asm.movl(dst, literal);
        ralloc.useReg(dst);
    }

    public void load(Label src, Register dst) {
        XLabel lab = (XLabel)src;
        ALabel alabel = lab.getAssemblerLabel();
        asm.movl(dst, alabel);   // relocation will be done by the Assembler
        ralloc.useReg(dst);
    }

    // ** this assumes a predefined location in the stack frame for parameters
    // ** for x86, it is ebp+8
    public void loadParam(int idx, Register dst) {
        Register indexReg = ralloc.nextAvailableRegister();
        load(idx, indexReg);
        load(new Address(EBP, indexReg, 4 /*word size*/, 8), dst);
    }

    public void loadParam(Register idxReg, Register dst) {
        load(new Address(EBP, idxReg, 4, 8), dst);
        ralloc.freeReg(idxReg);
    }

    public void loadParam(Address addrIdx, Register dst) {
        Register indexReg = ralloc.nextAvailableRegister();
        load(addrIdx, indexReg);
        load(new Address(EBP, indexReg, 4 /*word size*/, 8), dst);
    }

    public void loadReturnValue(int val) {
        asm.movl(EAX, val);
    }

    public void loadReturnValue(long val) {
        asm.movl(EAX, (int)val);
        asm.movl(EDX, (int)(val>>32));
    }

    // for x86, the JVM register to be used is EAX on calls (C_JVM calling convention)
    public void loadJvmReg(int literal) {
        if (ralloc.regIsFree(EAX)) {
            asm.movl(EAX, literal);
            ralloc.useReg(EAX);
        } else { // spill EAX to use it for this call
            Register spillReg = ralloc.nextAvailableRegister();
            asm.movl(spillReg, EAX);    // *** does the shadow stk need updating? ***
            asm.movl(EAX, literal);
            ralloc.useReg(spillReg);
        }
    }

    // for x86, the JVM register to be used is EAX on calls (C_JVM calling convention)
    public void loadJvmReg(Register reg) {
        if (reg != EAX) {
            if (ralloc.regIsFree(EAX)) {
                asm.movl(EAX, reg);
                ralloc.useReg(EAX);
            }
            else { // spill EAX to use it for this call
                Register spillReg = ralloc.nextAvailableRegister();
                asm.movl(spillReg, EAX);
                asm.movl(EAX, reg);
                ralloc.useReg(spillReg);
            }
        }
    }

    // for x86, the JVM register to be used is EAX on calls (C_JVM calling convention)
    public void loadJvmReg(Label label) {
        if (ralloc.regIsFree(EAX)) {
            asm.movl(EAX, ( (XLabel) label).getAssemblerLabel());
            ralloc.useReg(EAX);
        } else  { // spill EAX to use it for this call
            Register spillReg = ralloc.nextAvailableRegister();
            asm.movl(spillReg, EAX);
            asm.movl(EAX, ( (XLabel) label).getAssemblerLabel());
            ralloc.useReg(spillReg);
        }
    }

    public void deref(Register src, Register dst, Type type) {
        int typeCode = type.getTypeCode();
        switch (typeCode) {
            case Type.Code_I:
            case Type.Code_U:
            case Type.Code_R:
            case Type.Code_O:
                asm.movl(dst, new Address(src));
                break;
            case Type.Code_B: // byte
                asm.movsxb(dst, new Address(src));
                break;
            case Type.Code_A: // ubyte
                asm.movzxb(dst, new Address(src));
                break;
            case Type.Code_S: // short
                asm.movsxw(dst, new Address(src));
                break;
            case Type.Code_C: // ushort
                asm.movzxw(dst, new Address(src));
                break;
            case Type.Code_L: // long
                asm.movl(ralloc.registerLo(dst), new Address(src));
                asm.movl(ralloc.registerHi(dst), new Address(src, 4));
                break;
            case Type.Code_F: // float
            case Type.Code_D: // double
                throw new RuntimeException("deref: float and double not supported");
        }
        ralloc.freeReg(src);
        ralloc.useReg(dst);
    }

    public void deref(Address src, Register dst, Type type) {
        asm.movl(dst, src);
        deref(dst, dst, type);
    }

    public void deref(int src, Register dst, Type type) {
        asm.movl(dst, src);
        deref(dst, dst, type);
    }

    // dst is a reference; i.e., the write is to the address contained in dst
    public void writeRef(Register src, Address dst, Type type) {
        Register dstReg = moveAddressToRegister(dst, type);
        switch (type.getStructureSize()) {
            case 1:
                asm.movb(new Address(dstReg), src);  // didn't check if src is byte form
                break;
            case 2:
                asm.movw(new Address(dstReg), src);
                break;
            case 4:
                asm.movl(new Address(dstReg), src);
                break;
            case 8:
                throw new RuntimeException("Write reference: 64-bit reference not supported");
            default:
                throw new RuntimeException("Write reference: illegal type");
        }
        ralloc.freeReg(src);
        ralloc.freeReg(dstReg);
    }

    public void writeRef(int src, Address dst, Type type) {
        Register dstReg = moveAddressToRegister(dst, type);
        switch (type.getStructureSize()) {
            case 1:
                asm.movb(new Address(dstReg), src);  // didn't check if src is byte form
                break;
            case 2:
                Register reg = ralloc.nextAvailableRegister();
                asm.movl(reg, src);
                asm.movw(new Address(dstReg), reg);
                break;
            case 4:
                asm.movl(new Address(dstReg), src);
                break;
            default:
                throw new RuntimeException("Write reference: illegal type");
        }
        ralloc.freeReg(dstReg);
    }

    public void writeRef(long src, Address dst, Type type) {
        Register dstReg = moveAddressToRegister(dst, type);
        if (type.getActivationSize() == 8) {
            throw new RuntimeException("Write reference: 64-bit addresses not supported");
        } else
            throw new RuntimeException("Write reference: illegal type, expecting long");
        //ralloc.freeReg(dstReg);
    }

    public void writeRef(Address src, Address dst, Type type) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, dst);
        ralloc.useReg(reg);
        writeRef(src, reg, type);
    }

    // dst is a reference; i.e., write to the address pointed to by the register address
    public void writeRef(Register src, Register dst, Type type) {
        switch (type.getStructureSize()) {
            case 1:
                asm.movb(new Address(dst), src);  // didn't check if src is byte form
                break;
            case 2:
                asm.movw(new Address(dst), src);
                break;
            case 4:
                asm.movl(new Address(dst), src);
                break;
            case 8:
                asm.movl(new Address(dst), ralloc.registerLo(src));
                asm.movl(new Address(dst, 4), ralloc.registerHi(src));
                break;
            default:
                throw new RuntimeException("Write reference: illegal type");
        }
        ralloc.freeReg(src);
        ralloc.freeReg(dst);
    }

    public void writeRef(int src, Register dst, Type type) {
        switch (type.getStructureSize()) {
            case 1:
                asm.movb(new Address(dst), src);  // didn't check if src is byte form
                break;
            case 2:
                asm.movl(new Address(dst), src);  // ** is there no movw??? **
                break;
            case 4:
                asm.movl(new Address(dst), src);
                break;
            default:
                throw new RuntimeException("Write reference: illegal type");
        }
        ralloc.freeReg(dst);
    }

    public void writeRef(long src, Register dst, Type type) {
        if (type.getStructureSize() == 8) {
            asm.movl(new Address(dst), (int)src);
            asm.movl(new Address(dst, 4), (int)(src>>32));
        } else
            throw new RuntimeException("Write reference: illegal type");
        ralloc.freeReg(dst);
    }

    public void writeRef(Address src, Register dst, Type type) {
        /*Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, src);
        ralloc.useReg(reg);
        writeRef(reg, dst, type);*/
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, src);
        ralloc.useReg(reg);
        switch (type.getStructureSize()) {
            case 1:
                asm.movb(new Address(dst), reg);
                break;
            case 2:
                asm.movw(new Address(dst), reg);
                break;
            case 4:
                asm.movl(new Address(dst), reg);
                break;
            case 8:
                asm.movl(new Address(dst), reg);
                asm.movl(reg, src.offsetFrom(4));
                asm.movl(new Address(dst,4), reg);
                break;
            default:
                throw new RuntimeException("Write reference: illegal type");
        }
        ralloc.freeReg(reg);
        ralloc.freeReg(dst);
    }

    public void writeRef(int src, int dst, Type type) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, dst);
        ralloc.useReg(reg);
        writeRef(src, reg, type);
    }

    public void writeRef(long src, int dst, Type type) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, dst);
        ralloc.useReg(reg);
        writeRef(src, reg, type);
    }

    public void writeRef(Register src, int dst, Type type) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, dst);
        ralloc.useReg(reg);
        writeRef(src, reg, type);
    }

    public void writeRef(Address src, int dst, Type type) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, dst);
        ralloc.useReg(reg);
        writeRef(src, reg, type);
    }

    /*
     * Arithmetics in x86 - default registers
     * The basic equation is: dst = dst op src
     * however, some operations require predefined registers to be used:
     *       dst = dst + src
     *       dst = dst - src
     *   edx:eax = eax * src
     *       eax = edx:eax / src
     *       edx = edx:eax % src
     *       dst = dst and src
     *       dst = dst or src
     *       dst = dst xor src
     * The following methods make sure predefined registers are used for these
     * computations.
     */

    // free up regToFree and update the symbolic register entry in the shadow stack
    // **** need 64 bit version of this ***
    //
    // NB: there is an error in this method when the register that is being
    //     used to spill onto (reg) is not on the shadow stack or is regToFree.
    //     This can happen with binops, where you pop both values off the
    //     stack, one of them is regToFree and the other may be reg.  ***** Needs fixing
    //                                                                ***** may happen in division
    private void freeUpRegister(Register regToFree, Stack shadowStk) {
        if (!ralloc.regIsFree(regToFree)) {
            // spill regToFree onto another reg
            Register reg = ralloc.nextAvailableRegister();
            asm.movl(reg, regToFree);
            ralloc.useReg(reg);

            // update the regToFree reference in the shadow stack
            int i = shadowStk.size();
            boolean found = false;
            while ( (!found) && (i > 0)) {
                SymbolicValueDescriptor symb = (SymbolicValueDescriptor)
                    shadowStk.elementAt(i);
                if (symb.getSymbolicValueDescriptor() == S_REG) {
                    SymbolicRegister32 symbReg = (SymbolicRegister32) symb;
                    if (symbReg.getRegister() == regToFree) {
                        found = true;
                        symbReg.setRegister(reg);
                        shadowStk.setElementAt(symbReg, i);
                    }
                }
                i--;
            }
        }
    }

    private void setRegisterInEAX(Register reg, Stack shadowStk) {
        if (reg != EAX) {
            freeUpRegister(EAX, shadowStk);
            asm.movl(EAX, reg);
            ralloc.freeReg(reg);
        }
    }

    private void setDividendRegs(Register divReg, Stack shadowStk) {
        // EAX needs to be set to the dividend register divReg
        setRegisterInEAX(divReg, shadowStk);

        // EDX needs to be set to 0
        freeUpRegister(EDX, shadowStk);
        asm.movl(EDX, 0);
        ralloc.useReg(EDX);
    }

    private Register setDivisorReg(Address src) {
        return moveAddressToRegister(src);
    }

    private Register setDivisorReg(int src) {
        return moveImmediateToRegister(src);
    }

    // The result of this method is EDX:EAX = 0:divReg and the return reg = reg for src
    // Returns register for divisor
    private Register setDivisionRegisters(Register op1, Address op2, Stack shadowStk) {
        setDividendRegs(op1, shadowStk);
        return setDivisorReg(op2);
    }

    private Register setDivisionRegisters(Register op1, int op2, Stack shadowStk) {
        setDividendRegs(op1, shadowStk);
        return setDivisorReg(op2);
    }

    private Register setDivisionRegisters(Register op1, Register op2, Stack shadowStk) {
        setDividendRegs(op1, shadowStk);
        return op2;
    }

    private void setAddressToCL(Address op2, Stack shadowStk) {
        freeUpRegister(ECX, shadowStk);
        asm.movl(ECX, op2);
    }

    private void setRegisterToCL(Register op2, Stack shadowStk) {
        if (op2 != ECX) {
            freeUpRegister(ECX, shadowStk);
            asm.movl(ECX, op2);
            ralloc.freeReg(op2);
        }
    }

    // arithLogical() was factored so that op1 is placed in a register and op2 can be
    // any of register, address, or immediate.  This is because in x86, op1 is
    // considered the destination, and therefore, it is overwritten:
    //     dst = dst op src
    //     op1 = op1 op op2
    //
    public Register arithLogical(Address op1, Register op2, int opsize, int opcode, Stack shadowStk) {
        // optimize add, do not place op1 on a register, use commutative rule
        if ((opcode & 0xFFFFFF00) == OP_ADD) {
            return arithLogical(op2, op1, opcode, shadowStk);
        }
        Register op1reg = (opsize == 8) ? moveAddressToLongRegister(op1) : moveAddressToRegister(op1);
        return arithLogical(op1reg, op2, opcode, shadowStk);
    }

    /**
     * Checks:
     * 1- Long multiplication has to be done based on addresses as there are not
     *    enough registers to hold the long operands and the intermediate results.
     * 2- For other long operations, the first source operand (op1) is also the
     *    destination operand.  This operand should be placed in a register.
     */
    public Register arithLogical(Address op1, Address op2, int opsize, int opcode, Stack shadowStk) {
        if (opsize == 8) {
            switch ((opcode & 0xFFFFFF00)) {
                case OP_MUL:
                    mul64(op1, op2);
                    return moveAddressToLongRegister(op1);
                case OP_DIV:
                    comp.symbol("div64").call(0, VOID);  // need to spill edx:eax before call, if needed
                    return EDXEAX;
                case OP_REM:
                    comp.symbol("rem64").call(0, VOID);  // need to spill edx:eax before call, if needed
                    return EDXEAX;
            }
        }
        Register op1reg = (opsize == 8) ? moveAddressToLongRegister(op1) :
                                          moveAddressToRegister(op1);
        return arithLogical(op1reg, op2, opcode, shadowStk);
    }

    public Register arithLogical(Address op1, int op2, int opsize, int opcode, Stack shadowStk) {
        Register op1reg = (opsize == 8) ? moveAddressToLongRegister(op1) :
                                          moveAddressToRegister(op1);
        return arithLogical(op1reg, op2, opcode, shadowStk);
    }

    public Register arithLogical(Address op1, long op2, int opsize, int opcode, Stack shadowStk) {
        Register reg = moveAddressToLongRegister(op1);
        return arithLogical(reg, op2, opcode, shadowStk);
    }

    public Register arithLogical(Register op1, Register op2, int opcode, Stack shadowStk) {
        if (op1.isLong())
            return arithLogical64(op1, op2, opcode, shadowStk);
        return arithLogical32(op1, op2, opcode, shadowStk);
    }

    // result gets placed in op1 unless otherwise noted
    public Register arithLogical32(Register op1, Register op2, int opcode, Stack shadowStk) {
        Register result = op1;
        switch (opcode & 0xFFFFFF00) {
            case OP_ADD:
                asm.addl(op1, op2);
                ralloc.freeReg(op2);
                break;
            case OP_SUB:
                asm.subl(op1, op2);
                ralloc.freeReg(op2);
                break;
            case OP_MUL:
                asm.imull(op1, op2);
                ralloc.freeReg(op2);
                break;
            case OP_DIV:
            case OP_REM:
                Register divisorReg = setDivisionRegisters(op1, op2, shadowStk);
                asm.idivl(divisorReg); // quotient placed in eax, remainder in edx
                ralloc.freeReg(divisorReg);
                if ((opcode & 0XFFFFFF00) == OP_DIV) {
                    ralloc.freeReg(EDX);
                    result = EAX;
                } else {   // OP_REM
                    ralloc.freeReg(EAX);
                    result = EDX;
                }
                break;
            case OP_AND:
                asm.andl(op1, op2);
                ralloc.freeReg(op2);
                break;
            case OP_OR:
                asm.orl(op1, op2);
                ralloc.freeReg(op2);
                break;
            case OP_XOR:
                asm.xorl(op1, op2);
                ralloc.freeReg(op2);
                break;
            case OP_SHL:
                setRegisterToCL(op2, shadowStk);
                asm.shll(op1); // CL is the implicit second operand
                break;
            case OP_SHR:
                setRegisterToCL(op2, shadowStk);
                asm.sarl(op1);
                break;
            case OP_USHR:
                setRegisterToCL(op2, shadowStk);
                asm.shrl(op1);
                break;
            default:
                Assert.shouldNotReachHere("Invalid Squawk opcode");
        }
        return result;
    }

    // registers op1 and op2 are of type long
    // result is in op1
    private void add64(Register op1, Register op2) {
        asm.addl(ralloc.registerLo(op1), ralloc.registerLo(op2));
        asm.adcl(ralloc.registerHi(op1), ralloc.registerHi(op2));
    }

    // Registers op1 and op2 are of type long
    // result is in op1
    private void sub64(Register op1, Register op2) {
        asm.subl(ralloc.registerLo(op1), ralloc.registerLo(op2));
        asm.sbbl(ralloc.registerHi(op1), ralloc.registerHi(op2));
    }

    // Registers op1 and op2 are of type long
    // result is in op1
    private void and64(Register op1, Register op2) {
        asm.andl(ralloc.registerLo(op1), ralloc.registerLo(op2));
        asm.andl(ralloc.registerHi(op1), ralloc.registerHi(op2));
    }

    // Registers op1 and op2 are of type long
    // result is in op1
    private void or64(Register op1, Register op2) {
        asm.orl(ralloc.registerLo(op1), ralloc.registerLo(op2));
        asm.orl(ralloc.registerHi(op1), ralloc.registerHi(op2));
    }

    // Registers op1 and op2 are of type long
    // result is in op1
    private void xor64(Register op1, Register op2) {
        asm.xorl(ralloc.registerLo(op1), ralloc.registerLo(op2));
        asm.xorl(ralloc.registerHi(op1), ralloc.registerHi(op2));
    }

    // register op1 is of type long
    // result is in op1
    /**
     * Shift left long register op1 the number of bits specified in
     * the op2 count.
     *
     * The algorithm is as follows: for a value "ab" to be shifted left n
     * times, we shift 'b' once, the most-significant bit dropping into the
     * carry flag.  The carry flag then needs to be shifted into the 'a'
     * value and the 'a' value needs to be shifted left by one.  This process
     * is repeated n times.  The result of the operation being in "ab".
     *
     * @param op1  64-bit register to be shifted left
     * @param op2  32-bit shift count
     */
    private void shl64(Register op1, Register op2) {
        ALabel labStart = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        labStart.bindTo(asm.getCodePos());
        asm.cmpl(op2, 0);
        asm.jcc(EQUAL, labEnd);
        asm.shll(ralloc.registerLo(op1), 1);
        asm.rcll(ralloc.registerHi(op1), 1);
        asm.decl(op2);
        asm.jmp(labStart);

        labEnd.bindTo(asm.getCodePos());
    }

    // register op1 is of type long
    // result is in op1
    private void shl64(Register op1, int op2) {
        Register count = ralloc.nextAvailableRegister();
        asm.movl(count, op2);
        shl64(op1, count);
    }

    // register op1 is of type long
    // result is in op1
    private void shl64(Register op1, Address op2) {
        Register count = ralloc.nextAvailableRegister();
        asm.movl(count, op2);
        shl64(op1, count);
    }

    // register op1 is of type long
    // result is in op1
    /**
     * Shift right long register op1 the number of bits specified in
     * the op2 count.
     *
     * The algorithm is as follows: for a value "ab" to be shifted right 'n'
     * times, we shift right 'a' once, the least-significant bit dropping into the
     * carry flag.  The carry flag then needs to be shifted into the 'b'
     * value and the 'b' value needs to be shifted left by one.  This process
     * is repeated 'n' times.  The result of the operation being in "ab".
     *
     * @param op1  64-bit register to be shifted right
     * @param op2  32-bit shift count
     */
    private void shr64(Register op1, Register op2) {
        ALabel labStart = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        labStart.bindTo(asm.getCodePos());
        asm.cmpl(op2, 0);
        asm.jcc(EQUAL, labEnd);
        asm.sarl(ralloc.registerHi(op1), 1);
        asm.rcrl(ralloc.registerLo(op1), 1);
        asm.decl(op2);
        asm.jmp(labStart);

        labEnd.bindTo(asm.getCodePos());
    }

    // register op1 is of type long
    // result is in op1
    private void shr64(Register op1, int op2) {
        Register count = ralloc.nextAvailableRegister();
        asm.movl(count, op2);
        shr64(op1, count);
    }

    // register op1 is of type long
    // result is in op1
    private void shr64(Register op1, Address op2) {
        Register count = ralloc.nextAvailableRegister();
        asm.movl(count, op2);
        shr64(op1, count);
    }

    // register op1 is of type long
    // result is in op1
    private void ushr64(Register op1, Register op2) {
        ALabel labStart = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        labStart.bindTo(asm.getCodePos());
        asm.cmpl(op2, 0);
        asm.jcc(EQUAL, labEnd);
        asm.shrl(ralloc.registerHi(op1), 1);
        asm.rcrl(ralloc.registerLo(op1), 1);
        asm.decl(op2);
        asm.jmp(labStart);

        labEnd.bindTo(asm.getCodePos());
    }

    // register op1 is of type long
    // result is in op1
    private void ushr64(Register op1, int op2) {
        Register count = ralloc.nextAvailableRegister();
        asm.movl(count, op2);
        ushr64(op1, count);
    }

    // register op1 is of type long
    // result is in op1
    private void ushr64(Register op1, Address op2) {
        Register count = ralloc.nextAvailableRegister();
        asm.movl(count, op2);
        ushr64(op1, count);
    }

    // result in op1
    /**
     * Multiply two long operands and place the result in the first operand (op1).
     *
     * Multiplication is done using the following observation: if you were to
     * multiply "ab" by "cd", you would do as follows:
     *
     *           ab
     *         x cd
     *         -----
     *           bd
     *         ad
     *         cb
     *     + ac
     *     ----------
     *       ac0000 + cb00 + ad00 +  bd
     *
     * If "ab" and "cd" are 8-byte quantities, then we have the following result:
     *       ac<<64 + cb<<32 + ad<<32 + bd
     * Since the result is an 8-byte quantity, ac<<64 can be disregarded, as it is
     * overflow.  The result of the multiplication is therefore:
     *       (ad+cb)<<32 + bd
     *
     * NB: We need to use 4 registers to do this computation.  We chose eax, edx,
     *     esi and edi for x86.  Note however that these registers cannot be
     *     spilled onto the stack as the VM is using chunky stacks and therefore
     *     cannot grow dynamically the pre-allocated frame of memory that it has.
     *     This means that we need to spill onto locations pre-allocated in the
     *     stack frame (and we do this by back-patching the amount of space that
     *     needs to be allocated for locals and temporaries in the stack frame).
     *
     * @param op1 the destination and source long operand
     * @param op2 the second source operand
     *
     * Note that the result is placed in op1.
     */
    private void mul64(Address op1, Address op2) {
        Address hi1 = op1.offsetFrom(4);
        Address hi2 = op2.offsetFrom(4);

        // spill the registers that are used for intermediate computation onto
        // pre-allocated stack frame locations.
        Local edxSpill = comp.local(INT);
        Local eaxSpill = comp.local(INT);
        Local ediSpill = comp.local(INT);
        Local esiSpill = comp.local(INT);
        asm.movl(((X86XLocal)edxSpill).addressOf(), EDX);
        asm.movl(((X86XLocal)eaxSpill).addressOf(), EAX);
        asm.movl(((X86XLocal)ediSpill).addressOf(), EDI);
        asm.movl(((X86XLocal)esiSpill).addressOf(), ESI);

        // multiply low parts by high parts and keep the result in registers
        asm.movl(EDX, hi1);
        asm.movl(EAX, op2);
        asm.imull(EDX);       // result in EDX:EAX
        asm.movl(EDI, EDX);
        asm.movl(ESI, EAX);
        asm.movl(EDX, hi2);
        asm.movl(EAX, op1);
        asm.imull(EDX);       // result in EDX:EAX

        // add result of multiplication of lows and highs, and shift left by 32
        add64(EDIESI, EDXEAX);
        shl64(EDIESI, 32);    // result in EDI:ESI

        // multiply low parts together and add to result so far
        asm.movl(EDX, op1);
        asm.movl(EAX, op2);
        asm.imull(EDX);        // result in EDX:EAX
        add64(EDXEAX, EDIESI); // result in EDX:EAX

        // ignore overflow (multiplication of high parts)

        // place result in op1
        asm.movl(op1, EAX);
        asm.movl(op1.offsetFrom(4), EDX);

        // restore spilled registers
        asm.movl(EDX, ((X86XLocal)edxSpill).addressOf());
        asm.movl(EAX, ((X86XLocal)eaxSpill).addressOf());
        asm.movl(EDI, ((X86XLocal)ediSpill).addressOf());
        asm.movl(ESI, ((X86XLocal)esiSpill).addressOf());
    }

    public Register arithLogical64(Register op1, Register op2, int opcode, Stack shadowStk) {
        Register result = op1;
        switch (opcode & 0xFFFFFF00) {
            case OP_ADD:
                add64(op1, op2);
                break;
            case OP_SUB:
                sub64(op1, op2);
                break;
            case OP_MUL:
                Local op1loc = comp.local(LONG);
                Local op2loc = comp.local(LONG);
                asm.movl(((X86XLocal)op1loc).addressOf(), ralloc.registerLo(op1));
                asm.movl(((X86XLocal)op1loc).addressOf(4), ralloc.registerHi(op1));
                asm.movl(((X86XLocal)op2loc).addressOf(), ralloc.registerLo(op2));
                asm.movl(((X86XLocal)op2loc).addressOf(4), ralloc.registerHi(op2));
                mul64(((X86XLocal)op1loc).addressOf(), ((X86XLocal)op2loc).addressOf());  // result in op1loc
                asm.movl(ralloc.registerLo(op1), ((X86XLocal)op1loc).addressOf());
                asm.movl(ralloc.registerHi(op1), ((X86XLocal)op1loc).addressOf(4));
                break;
            case OP_DIV:
                comp.symbol("div64").call(0, VOID);  // need to spill edx:eax before call, if needed
                return EDXEAX;
            case OP_REM:
                comp.symbol("rem64").call(0, VOID);  // need to spill edx:eax before call, if needed
                return EDXEAX;
            case OP_AND:
                and64(op1, op2);
                break;
            case OP_OR:
                or64(op1, op2);
                break;
            case OP_XOR:
                xor64(op1, op2);
                break;
            case OP_SHL:
                shl64(op1, op2);
                break;
            case OP_SHR:
                shr64(op1, op2);
                break;
            case OP_USHR:
                ushr64(op1, op2);
                break;
            default:
                Assert.shouldNotReachHere("Invalid Squawk opcode");
        }
        ralloc.freeReg(op2);
        return result;
    }

    public Register arithLogical(Register op1, Address op2, int opcode, Stack shadowStk) {
        if (op1.isLong())
            return arithLogical64(op1, op2, opcode, shadowStk);
        return arithLogical32(op1, op2, opcode, shadowStk);
    }

    public Register arithLogical32(Register op1, Address op2, int opcode, Stack shadowStk) {
        Register result = op1;
        switch (opcode & 0xFFFFFF00) {
            case OP_ADD:
                asm.addl(op1, op2);
                break;
            case OP_SUB:
                asm.subl(op1, op2);
                break;
            case OP_MUL:
                Register op2reg = moveAddressToRegister(op2);
                asm.imull(op1, op2reg);
                ralloc.freeReg(op2reg);
                break;
            case OP_DIV:
            case OP_REM:
                Register divisorReg = setDivisionRegisters(op1, op2, shadowStk);
                asm.idivl(divisorReg); // quotient placed in eax, remainder in edx
                ralloc.freeReg(divisorReg);
                if ((opcode & 0xFFFFFF00) == OP_DIV) {
                    ralloc.freeReg(EDX);
                    result = EAX;
                } else {   // OP_REM
                    ralloc.freeReg(EAX);
                    result = EDX;
                }
                break;
            case OP_AND:
                asm.andl(op1, op2);
                break;
            case OP_OR:
                asm.orl(op1, op2);
                break;
            case OP_XOR:
                asm.xorl(op1, op2);
                break;
            case OP_SHL:
                setAddressToCL(op2, shadowStk);
                asm.shll(op1);   // CL is the implicit second operand
                break;
            case OP_SHR:
                setAddressToCL(op2, shadowStk);
                asm.sarl(op1);
                break;
            case OP_USHR:
                setAddressToCL(op2, shadowStk);
                asm.shrl(op1);
                break;
            default:
                Assert.shouldNotReachHere("Invalid Squawk opcode");
        }
        return result;
    }

    public Register arithLogical64(Register op1, Address op2, int opcode, Stack shadowStk) {
        Register result = op1;
        Register hi = ralloc.registerHi(op1);
        Register lo = ralloc.registerLo(op1);
        switch (opcode & 0xFFFFFF00) {
            case OP_ADD:
                asm.addl(lo, op2);
                asm.adcl(hi, op2.offsetFrom(4));
                break;
            case OP_SUB:
                asm.subl(lo, op2);
                asm.sbbl(hi, op2.offsetFrom(4));
                break;
            case OP_MUL:
                Local op1loc = comp.local(LONG);
                asm.movl(((X86XLocal)op1loc).addressOf(), ralloc.registerLo(op1));
                asm.movl(((X86XLocal)op1loc).addressOf(4), ralloc.registerHi(op1));
                mul64(((X86XLocal)op1loc).addressOf(), op2);  // result in op1loc
                asm.movl(ralloc.registerLo(op1), ((X86XLocal)op1loc).addressOf());
                asm.movl(ralloc.registerHi(op1), ((X86XLocal)op1loc).addressOf(4));
                break;
            case OP_DIV:
                comp.symbol("div64").call(0, VOID);  // need to spill edx:eax before call, if needed
                return EDXEAX;
            case OP_REM:
                comp.symbol("rem64").call(0, VOID);  // need to spill edx:eax before call, if needed
                return EDXEAX;
            case OP_AND:
                asm.andl(lo, op2);
                asm.andl(hi, op2.offsetFrom(4));
                break;
            case OP_OR:
                asm.orl(lo, op2);
                asm.orl(hi, op2.offsetFrom(4));
                break;
            case OP_XOR:
                asm.xorl(lo, op2);
                asm.xorl(hi, op2.offsetFrom(4));
                break;
            case OP_SHL:
                shl64(op1, op2);
                break;
            case OP_SHR:
                shr64(op1, op2);
                break;
            case OP_USHR:
                ushr64(op1, op2);
                break;
            default:
                Assert.shouldNotReachHere("Invalid Squawk opcode");
        }
        return result;
    }

    public Register arithLogical(Register op1, int op2, int opcode, Stack shadowStk) {
        if (op1.isLong())
            return arithLogical64(op1, op2, opcode, shadowStk);
        return arithLogical32(op1, op2, opcode, shadowStk);
    }

    // result goes into op1, unless otherwise noted
    public Register arithLogical32(Register op1, int op2, int opcode, Stack shadowStk) {
        Register result = op1;
        switch (opcode & 0xFFFFFF00) {
            case OP_ADD:
                if (op2 == 1)
                    asm.incl(op1);
                else
                    asm.addl(op1, op2);
                break;
            case OP_SUB:
                if (op2 == 1)
                    asm.decl(op1);
                else
                    asm.subl(op1, op2);
                break;
            case OP_MUL:
                asm.imull(op1, op1, op2);   // result is in op1 in this form of imull
                break;
            case OP_DIV:
            case OP_REM:
                Register divisorReg = setDivisionRegisters(op1, op2, shadowStk);
                asm.idivl(divisorReg); // quotient placed in eax, remainder in edx
                ralloc.freeReg(divisorReg);
                if ((opcode & 0xFFFFFF00) == OP_DIV) {
                    ralloc.freeReg(EDX);
                    result = EAX;
                } else {   // OP_REM
                    ralloc.freeReg(EAX);
                    result = EDX;
                }
                break;
            case OP_AND:
                asm.andl(op1, op2);
                break;
            case OP_OR:
                asm.orl(op1, op2);
                break;
            case OP_XOR:
                asm.xorl(op1, op2);
                break;
            case OP_SHL:
                asm.shll(op1, op2);
                break;
            case OP_SHR:
                asm.sarl(op1, op2);
                break;
            case OP_USHR:
                asm.shrl(op1, op2);
                break;
            default:
                throw new RuntimeException("Invalid Squawk opcode for arithmetic or logical operation");
        }
        return result;
    }

    /*
     * For long operations, the second argument can be an integer only for the
     * shift operations (shl, shr, ushr).
     */
    public Register arithLogical64(Register op1, int op2, int opcode, Stack shadowStk) {
        Register result = op1;
        Register hi = ralloc.registerHi(op1);
        Register lo = ralloc.registerLo(op1);
        switch (opcode & 0xFFFFFF00) {
            case OP_SHL:
                shl64(op1, op2);
                break;
            case OP_SHR:
                shr64(op1, op2);
                break;
            case OP_USHR:
                ushr64(op1, op2);
                break;
            default:
                throw new RuntimeException("Long arithmetic/logical operation: expecting long operand as second argument");
        }
        return result;
    }

    public Register arithLogical(Register op1, long op2, int opcode, Stack shadowStk) {
        Register result = op1;
        Register hi = ralloc.registerHi(op1);
        Register lo = ralloc.registerLo(op1);
        switch (opcode & 0xFFFFFF00) {
            case OP_ADD:
                asm.addl(lo, (int)op2);
                asm.adcl(hi, (int)(op2>>32));
                break;
            case OP_SUB:
                asm.subl(lo, (int)op2);
                asm.sbbl(hi, (int)(op2>>32));
                break;
            case OP_MUL:
                Local op1loc = comp.local(LONG);
                Local op2loc = comp.local(LONG);
                asm.movl(((X86XLocal)op1loc).addressOf(), ralloc.registerLo(op1));
                asm.movl(((X86XLocal)op1loc).addressOf(4), ralloc.registerHi(op1));
                asm.movl(((X86XLocal)op2loc).addressOf(), (int)(op2));
                asm.movl(((X86XLocal)op2loc).addressOf(4), (int)(op2>>32));
                mul64(((X86XLocal)op1loc).addressOf(), ((X86XLocal)op2loc).addressOf());  // result in op1loc
                asm.movl(ralloc.registerLo(op1), ((X86XLocal)op1loc).addressOf());
                asm.movl(ralloc.registerHi(op1), ((X86XLocal)op1loc).addressOf(4));
                break;
            case OP_DIV:
                comp.symbol("div64").call(0, VOID);  // need to spill edx:eax before call, if needed
                return EDXEAX;
            case OP_REM:
                comp.symbol("rem64").call(0, VOID);  // need to spill edx:eax before call, if needed
                return EDXEAX;
            case OP_AND:
                asm.andl(lo, (int)op2);
                asm.andl(hi, (int)(op2>>32));
                break;
            case OP_OR:
                asm.orl(lo, (int)op2);
                asm.orl(hi, (int)(op2>>32));
                break;
            case OP_XOR:
                asm.xorl(lo, (int)op2);
                asm.xorl(hi, (int)(op2>>32));
                break;
            case OP_SHL:
            case OP_SHR:
            case OP_USHR:
                throw new RuntimeException("Invalid shift operand, expecting integer value, received long");
            default:
                throw new RuntimeException("Invalid Squawk opcode for arithmetic or logical operation");
        }
        return result;
    }

    public Register arithLogical(int op1, Register op2, int opcode, Stack shadowStk) {
        // optimize commutative operators, do not place op1 in a register; reverse operands
        int code = opcode & 0xFFFFFF00;
        if ((code == OP_ADD) || (code == OP_MUL) || (code == OP_AND) ||
            (code == OP_OR) || (code == OP_XOR)) {
            return arithLogical(op2, op1, opcode, shadowStk);
        }

        Register op1reg = moveImmediateToRegister(op1);
        return arithLogical(op1reg, op2, opcode, shadowStk);
    }

    public Register arithLogical(int op1, Address op2, int opsize, int opcode, Stack shadowStk) {
        // optimize commutative operators, do not place op1 in a register; reverse operands
        int code = opcode & 0xFFFFFF00;
        if ((code == OP_ADD) || (code == OP_MUL) || (code == OP_AND) ||
            (code == OP_OR) || (code == OP_XOR)) {
            return arithLogical(op2, op1, opsize, opcode, shadowStk);
        }

        Register op1reg = moveImmediateToRegister(op1);
        return arithLogical(op1reg, op2, opcode, shadowStk);
    }

    public Register arithLogical(int op1, int op2, int opcode, Stack shadowStk) {
        Register op1reg = moveImmediateToRegister(op1);
        return arithLogical(op1reg, op2, opcode, shadowStk);
    }

    public Register arithLogical(long op1, Register op2, int opcode, Stack shadowStk) {
        // optimize commutative operators, do not place op1 in a register; reverse operands
        int code = opcode & 0xFFFFFF00;
        if ((code == OP_ADD) || (code == OP_MUL) || (code == OP_AND) ||
            (code == OP_OR) || (code == OP_XOR)) {
            return arithLogical(op2, op1, opcode, shadowStk);
        }

        Register op1reg = moveImmediateToLongRegister(op1);
        return arithLogical(op1reg, op2, opcode, shadowStk);
    }

    public Register arithLogical(long op1, long op2, int opcode, Stack shadowStk) {
        Register op1reg = moveImmediateToLongRegister(op1);
        return arithLogical(op1reg, op2, opcode, shadowStk);
    }

    public Register arithLogical(long op1, Address op2, int opsize, int opcode, Stack shadowStk) {
        // optimize commutative operators, do not place op1 in a register; reverse operands
        int code = opcode & 0xFFFFFF00;
        if ((code == OP_ADD) || (code == OP_MUL) || (code == OP_AND) ||
            (code == OP_OR) || (code == OP_XOR)) {
            return arithLogical(op2, op1, opsize, opcode, shadowStk);
        }

        Register op1reg = moveImmediateToLongRegister(op1);
        return arithLogical(op1reg, op2, opcode, shadowStk);
    }

    public Register unary(Register reg, int opcode) {
        if (reg.isLong())
            return unary64(reg, opcode);
        return unary32(reg, opcode);
    }

    private Register unary32(Register reg, int opcode) {
        switch (opcode & 0xFFFFFF00) {
            case OP_COM:
                asm.notl(reg);
                break;
            case OP_NEG:
                asm.negl(reg);
                break;
            default:
                throw new RuntimeException("Illegal unary bytecode");
        }
        return reg;
    }

    private Register unary64(Register reg, int opcode) {
        Register hi = ralloc.registerHi(reg);
        Register lo = ralloc.registerLo(reg);
        switch (opcode & 0xFFFFFF00) {
            case OP_COM:
                asm.notl(lo);
                asm.notl(hi);
                break;
            case OP_NEG:
                asm.notl(lo);
                asm.notl(hi);
                asm.addl(lo, 1);
                asm.adcl(hi, 0);
                break;
            default:
                throw new RuntimeException("Invalid unary bytecode");
        }
        return reg;
    }

    public Register unary(int immed, int opcode) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, immed);
        ralloc.useReg(reg);
        return unary(reg, opcode);
    }

    public Register unary(long immed, int opcode) {
        Register reg = ralloc.nextAvailableLongRegister();
        asm.movl(ralloc.registerLo(reg), (int)immed);
        asm.movl(ralloc.registerHi(reg), (int)(immed>>32));
        ralloc.useReg(reg);
        return unary(reg, opcode);
    }

    public Register unary(Address addr, Type type, int opcode) {
        Register reg;
        if (type.getStructureSize() == 8) {
            reg = ralloc.nextAvailableLongRegister();
            asm.movl(ralloc.registerLo(reg), addr);
            asm.movl(ralloc.registerHi(reg), addr.offsetFrom(4));
        } else {
            reg = ralloc.nextAvailableRegister();
            asm.movl(reg, addr);
        }
        ralloc.useReg(reg);
        return unary(reg, opcode);
    }

    public void updateESP(int value) {
        asm.addl(ESP, value);
    }

    private Register doComparison(int opcode, boolean signed) {
        Register res = ralloc.nextAvailableRegister();
        asm.movl(res, 0);
        switch (opcode & 0xFFFFFF00) {
            case OP_EQ:
                asm.setb(EQUAL, res);
                break;
            case OP_NE:
                asm.setb(NOT_EQUAL, res);
                break;
            case OP_LE:
                if (signed)
                    asm.setb(LESS_EQUAL, res);
                else
                    asm.setb(BELOW_EQUAL, res);
                break;
            case OP_LT:
                if (signed)
                    asm.setb(LESS, res);
                else
                    asm.setb(BELOW, res);
                break;
            case OP_GE:
                if (signed)
                    asm.setb(GREATER_EQUAL, res);
                else
                    asm.setb(ABOVE_EQUAL, res);
                break;
            case OP_GT:
                if (signed)
                    asm.setb(GREATER, res);
                else
                    asm.setb(ABOVE, res);
                break;
            default:
                throw new RuntimeException("Invalid comparison code");
        }
        ralloc.useReg(res);
        return res;
    }

    /**
     * Compares two registers and returns the result in a register
     */
    public Register compare(Register op1, Register op2, int opcode, boolean signed) {
        if (op1.isLong()) {
            return compare64(op1, op2, opcode, signed);
        } else {
            asm.cmpl(op1, op1); // sets EFLAGS
            ralloc.freeReg(op2);
            ralloc.freeReg(op1);
            return doComparison(opcode, signed);
        }
    }

    public Register compare(Register op1, int op2, int opcode, boolean signed) {
        asm.cmpl(op1, op2);
        ralloc.freeReg(op1);
        return doComparison(opcode, signed);
    }

    public Register compare(Register op1, Address op2, int opcode, boolean signed) {
        if (op1.isLong()) {
            return compare64(op1, op2, opcode, signed);
        } else {
            asm.cmpl(op1, op2);
            ralloc.freeReg(op1);
            return doComparison(opcode, signed);
        }
    }

    // long comparison
    public Register compare64(Register op1, Address op2, int opcode, boolean signed) {
        Register res;
        int compareCode = opcode & 0xFFFFFF00;
        switch (compareCode) {
            case OP_EQ:
            case OP_NE:
                res = compareEqualityLong(op1, op2, compareCode);
                break;
            case OP_LE:
            case OP_LT:
            case OP_GE:
            case OP_GT:
                res = compareGreaterLessLong(op1, op2, compareCode, signed);
                break;
            default:
                throw new RuntimeException("Invalid comparison code");
        }
        return res;
    }

    // long comparison
    public Register compare64(Register op1, long op2, int opcode, boolean signed) {
        Register res;
        int compareCode = opcode & 0xFFFFFF00;
        switch (compareCode) {
            case OP_EQ:
            case OP_NE:
                res = compareEqualityLong(op1, op2, compareCode);
                break;
            case OP_LE:
            case OP_LT:
            case OP_GE:
            case OP_GT:
                res = compareGreaterLessLong(op1, op2, compareCode, signed);
                break;
            default:
                throw new RuntimeException("Invalid comparison code");
        }
        return res;
    }

    // long comparison
    public Register compare64(Register op1, Register op2, int opcode, boolean signed) {
        Register res;
        int compareCode = opcode & 0xFFFFFF00;
        switch (compareCode) {
            case OP_EQ:
            case OP_NE:
                res = compareEqualityLong(op1, op2, compareCode);
                break;
            case OP_LE:
            case OP_LT:
            case OP_GE:
            case OP_GT:
                res = compareGreaterLessLong(op1, op2, compareCode, signed);
                break;
            default:
                throw new RuntimeException("Invalid comparison code");
        }
        return res;
    }

    public Register compare(Address op1, int op2, int opcode, boolean signed) {
        asm.cmpl(op1, op2);
        return doComparison(opcode, signed);
    }

    public Register compare64(Address op1, long op2, int opcode, boolean signed) {
        Register res;
        int compareCode = opcode & 0xFFFFFF00;
        switch (compareCode) {
            case OP_EQ:
            case OP_NE:
                res = compareEqualityLong(op1, op2, compareCode);
                break;
            case OP_LE:
            case OP_LT:
            case OP_GE:
            case OP_GT:
                res = compareGreaterLessLong(op1, op2, compareCode, signed);
                break;
            default:
                throw new RuntimeException("Invalid comparison code");
        }
        return res;
    }

    public Register compare64(Address op1, Register op2, int opcode, boolean signed) {
        Register op1reg = ralloc.nextAvailableLongRegister();
        asm.movl(ralloc.registerLo(op1reg), op1);
        asm.movl(ralloc.registerHi(op1reg), op1.offsetFrom(4));
        return compare64(op1reg, op2, opcode, signed);
    }

    public Register compare64(Address op1, Address op2, int opcode, boolean signed) {
        Register op1reg = ralloc.nextAvailableLongRegister();
        asm.movl(ralloc.registerLo(op1reg), op1);
        asm.movl(ralloc.registerHi(op1reg), op1.offsetFrom(4));
        return compare64(op1reg, op2, opcode, signed);
    }

    /**
     * Compares greater than, greater than or equal to, less than, and
     * less than or equal to long values.
     *
     * The comparison is done in the following way (assume each symbol to
     * represent a 4-byte quantity):
     *
     *         ab > cd?
     * if a > c then the result is true
     * else if a < c then the result is false
     * otherwise, if b > d the result is true else false
     *
     */
    private Register compareGreaterLessLong(Address op1, long op2, int comparison,
                                            boolean signed) {
        Address op1hi = op1.offsetFrom(4);
        ALabel labIs = new ALabel(asm);
        ALabel labIsNot = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        asm.cmpl(op1hi, (int)(op2>>32));
        emitHighWordCompareInstructions(comparison, signed, labIs, labIsNot);

        asm.cmpl(op1, (int)op2);
        emitLowWordCompareInstruction(comparison, labIs);

        return emitResultCompareInstructions(labIsNot, labIs, labEnd);
    }

    private Register compareGreaterLessLong(Register op1, Address op2,
                                            int comparison, boolean signed) {
        Register op1hi = ralloc.registerHi(op1);
        Register op1lo = ralloc.registerLo(op1);
        Address op2hi = op2.offsetFrom(4);
        ALabel labIs = new ALabel(asm);
        ALabel labIsNot = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        asm.cmpl(op1hi, op2hi);
        emitHighWordCompareInstructions(comparison, signed, labIs, labIsNot);

        asm.cmpl(op1lo, op2);
        emitLowWordCompareInstruction(comparison, labIs);
        ralloc.freeReg(op1);

        return emitResultCompareInstructions(labIsNot, labIs, labEnd);
    }

    private Register compareGreaterLessLong(Register op1, Register op2,
                                            int comparison, boolean signed) {
        Register op1hi = ralloc.registerHi(op1);
        Register op1lo = ralloc.registerLo(op1);
        Register op2hi = ralloc.registerHi(op2);
        Register op2lo = ralloc.registerLo(op2);
        ALabel labIs = new ALabel(asm);
        ALabel labIsNot = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        asm.cmpl(op1hi, op2hi);
        emitHighWordCompareInstructions(comparison, signed, labIs, labIsNot);

        asm.cmpl(op1lo, op2lo);
        emitLowWordCompareInstruction(comparison, labIs);
        ralloc.freeReg(op1);
        ralloc.freeReg(op2);

        return emitResultCompareInstructions(labIsNot, labIs, labEnd);
    }

    private Register compareGreaterLessLong(Register op1, long op2,
                                            int comparison, boolean signed) {
        Register op1hi = ralloc.registerHi(op1);
        Register op1lo = ralloc.registerLo(op1);
        ALabel labIs = new ALabel(asm);
        ALabel labIsNot = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        asm.cmpl(op1hi, (int)(op2>>32));
        emitHighWordCompareInstructions(comparison, signed, labIs, labIsNot);

        asm.cmpl(op1lo, (int)op2);
        emitLowWordCompareInstruction(comparison, labIs);
        ralloc.freeReg(op1);

        return emitResultCompareInstructions(labIsNot, labIs, labEnd);
    }

    private void emitHighWordCompareInstructions(int comparisonCode, boolean signed,
                                         ALabel labIs, ALabel labIsNot) {
        if (comparisonCode == OP_GT || comparisonCode == OP_GE) {
            if (signed) { // long
                asm.jcc(GREATER, labIs);
                asm.jcc(LESS, labIsNot);
            }
            else { // ulong
                asm.jcc(ABOVE, labIs);
                asm.jcc(BELOW, labIsNot);
            }
        }
        else if (comparisonCode == OP_LT || comparisonCode == OP_LE) {
            if (signed) { // long
                asm.jcc(GREATER, labIsNot);
                asm.jcc(LESS, labIs);
            }
            else { // ulong
                asm.jcc(ABOVE, labIsNot);
                asm.jcc(BELOW, labIs);
            }
        }
        else {
            throw new RuntimeException(
                "Long comparison: invalid comparison code");
        }
    }

    private void emitLowWordCompareInstruction(int comparisonCode, ALabel labIs) {
        switch (comparisonCode) {
            case OP_GT:
                asm.jcc(ABOVE, labIs);
                break;
            case OP_GE:
                asm.jcc(ABOVE_EQUAL, labIs);
                break;
            case OP_LT:
                asm.jcc(BELOW, labIs);
                break;
            case OP_LE:
                asm.jcc(BELOW_EQUAL, labIs);
                break;
            default:
                throw new RuntimeException(
                    "Compare long: Invalid comparison type");
        }
    }

    private Register emitResultCompareInstructions(ALabel labIsNot, ALabel labIs,
                                                   ALabel labEnd) {
        Register res = ralloc.nextAvailableRegister();

        labIsNot.bindTo(asm.getCodePos());
        asm.movl(res, 0);
        asm.jmp(labEnd);

        labIs.bindTo(asm.getCodePos());
        asm.movl(res, 1);

        // bind assembler label to this location; res has the result
        labEnd.bindTo(asm.getCodePos());
        ralloc.useReg(res);
        return res;
    }

    /**
     * Compare if two long quantities are equal or not equal (signed or unsigned).
     *
     * The comparison is done in the following way (assume each character is a
     * word quantity in the below example):
     *
     *         ab == cd ?
     * if a is not equal to c then the result is 0 (false).
     * else if b is equal to d, then the result is 1 (true) else 0 (false).
     *
     */

    /**
     * Compare if two long (signed or unsigned) quantities are equal or not.
     *
     * @param op1 the first long operand
     * @param op2 the second long operand
     * @return a register containing either 0 (false) or 1 (true).
     */
    private Register compareEqualityLong(Address op1, long op2, int compareCode) {
        Address op1hi = op1.offsetFrom(4);
        ALabel labNotEqual = new ALabel(asm);
        ALabel labIsEqual = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        asm.cmpl(op1hi, (int)(op2>>32));
        asm.jcc(NOT_EQUAL, labNotEqual);

        asm.cmpl(op1, (int)op2);    // compare low words if high words were the same
        return emitResultEqualityInstructions(compareCode, labIsEqual, labNotEqual, labEnd);
    }

    private Register compareEqualityLong(Register op1, long op2, int compareCode) {
        Register op1hi = ralloc.registerHi(op1);
        Register op1lo = ralloc.registerLo(op1);
        ALabel labNotEqual = new ALabel(asm);
        ALabel labIsEqual = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        asm.cmpl(op1hi, (int)(op2>>32));
        asm.jcc(NOT_EQUAL, labNotEqual);
        ralloc.freeReg(op1);

        asm.cmpl(op1lo, (int)op2);    // compare low words if high words were the same
        return emitResultEqualityInstructions(compareCode, labIsEqual, labNotEqual, labEnd);
    }

    private Register compareEqualityLong(Register op1, Address op2, int compareCode) {
        Register op1hi = ralloc.registerHi(op1);
        Register op1lo = ralloc.registerLo(op1);
        Address op2hi = op2.offsetFrom(4);
        ALabel labNotEqual = new ALabel(asm);
        ALabel labIsEqual = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        asm.cmpl(op1hi, op2hi);
        asm.jcc(NOT_EQUAL, labNotEqual);
        ralloc.freeReg(op1);

        asm.cmpl(op1lo, op2);    // compare low words if high words were the same
        return emitResultEqualityInstructions(compareCode, labIsEqual, labNotEqual, labEnd);
    }

    private Register compareEqualityLong(Register op1, Register op2, int compareCode) {
        Register op1hi = ralloc.registerHi(op1);
        Register op1lo = ralloc.registerLo(op1);
        Register op2hi = ralloc.registerHi(op2);
        Register op2lo = ralloc.registerLo(op2);
        ALabel labNotEqual = new ALabel(asm);
        ALabel labIsEqual = new ALabel(asm);
        ALabel labEnd = new ALabel(asm);

        asm.cmpl(op1hi, op2hi);
        asm.jcc(NOT_EQUAL, labNotEqual);
        ralloc.freeReg(op1);

        asm.cmpl(op1lo, op2lo);    // compare low words if high words were the same
        ralloc.freeReg(op2);
        return emitResultEqualityInstructions(compareCode, labIsEqual, labNotEqual, labEnd);
    }

    private Register emitResultEqualityInstructions(int compareCode, ALabel labIsEqual,
                                                    ALabel labNotEqual, ALabel labEnd) {
        Register res = ralloc.nextAvailableRegister();

        if (compareCode == OP_EQ) {
            asm.jcc(EQUAL, labIsEqual);

            labNotEqual.bindTo(asm.getCodePos());
            asm.movl(res, 0); // they are not equal
            asm.jmp(labEnd);

            labIsEqual.bindTo(asm.getCodePos());
            asm.movl(res, 1); // they are equal
        }
        else if (compareCode == OP_NE) {
            asm.jcc(NOT_EQUAL, labNotEqual);

            asm.movl(res, 0); // they are equal
            asm.jmp(labEnd);

            labNotEqual.bindTo(asm.getCodePos());
            asm.movl(res, 1); // they are not equal
        }
        else {
            throw new RuntimeException(
                "Compare equality of long operands: invalid compare code");
        }

        labEnd.bindTo(asm.getCodePos());
        ralloc.useReg(res);
        return res;
    }

    public Register compare(Address op1, Register op2, int opcode, boolean signed) {
        if (op2.isLong()) {
            return compare64(op1, op2, opcode, signed);
        } else {
            Register op1reg = moveAddressToRegister(op1);
            return compare(op1reg, op2, opcode, signed);
        }
    }

    public Register compare(Address op1, Address op2, int opcode, Type type, boolean signed) {
        if (type == LONG || type == ULONG) {
            return compare64(op1, op2, opcode, signed);
        } else {
            Register op1reg = moveAddressToRegister(op1);
            return compare(op1reg, op2, opcode, signed);
        }
    }

    public Register compare(int op1, Register op2, int opcode, boolean signed) {
        Register op1reg = moveImmediateToRegister(op1);
        return compare(op1reg, op2, opcode, signed);
    }

    public Register compare(int op1, Address op2, int opcode, boolean signed) {
        Register op1reg = moveImmediateToRegister(op1);
        return compare(op1reg, op2, opcode, signed);
    }

    public Register compare(int op1, int op2, int opcode, boolean signed) {
        Register op1reg = moveImmediateToRegister(op1);
        return compare(op1reg, op2, opcode, signed);
    }

    public Register compare64(long op1, Register op2, int opcode, boolean signed) {
        Register op1reg = moveImmediateToLongRegister(op1);
        return compare64(op1reg, op2, opcode, signed);
    }

    public Register compare64(long op1, Address op2, int opcode, boolean signed) {
        Register op1reg = moveImmediateToLongRegister(op1);
        return compare(op1reg, op2, opcode, signed);
    }

    public Register compare64(long op1, long op2, int opcode, boolean signed) {
        Register op1reg = moveImmediateToLongRegister(op1);
        return compare64(op1reg, op2, opcode, signed);
    }

    /**
     * Conditional branch to the target label when the condition is satisfied.
     * NB: no check for 64b register is needed as the register should be of type INT
     *     (previously checked in XCompiler).
     *
     * @param label the target label
     * @param reg the register that holds the result of the previous comparison
     * @param cond the condition for the branch
     */
    public void branch(Label label, Register reg, boolean cond) {
        if (cond == true) {
            asm.cmpl(reg, 1);
        }
        else {  // false
            asm.cmpl(reg, 0);
        }
        asm.jcc(EQUAL, ( (XLabel) label).getAssemblerLabel());
        ralloc.freeReg(reg);
    }

    public void branch(Label label, int res, boolean cond) {
        Register reg = moveImmediateToRegister(res);
        branch(label, reg, cond);
    }

    // address if of type INT
    public void branch(Label label, Address addr, boolean cond) {
        if (cond == true) {
            asm.cmpl(addr, 1);
        }
        else {  // false
            asm.cmpl(addr, 0);
        }
        asm.jcc(EQUAL, ( (XLabel) label).getAssemblerLabel());
    }

    public void branch(int immed, Register reg, boolean cond) {
        if (cond == true) {
            asm.cmpl(reg, 1);
        }
        else {  // false
            asm.cmpl(reg, 0);
        }
        asm.jcc(EQUAL, immed);
        ralloc.freeReg(reg);
    }

    public void branch(int immed, int res, boolean cond) {
        Register reg = moveImmediateToRegister(res);
        branch(immed, reg, cond);
    }

    public void branch(int immed, Address addr, boolean cond) {
        if (cond == true) {
            asm.cmpl(addr, 1);
        }
        else {  // false
            asm.cmpl(addr, 0);
        }
        asm.jcc(EQUAL, immed);
    }

    public void branch(Register dst) {
        asm.jmp(dst);
        ralloc.freeReg(dst);
    }

    public void branch(int dst) {
        asm.jmp(dst);
    }

    public void branch(Address dst) {
        asm.jmp(dst);
    }

    public void branch(Label dst) {
        asm.jmp(((XLabel)dst).getAssemblerLabel());
    }

    public void branchToLeave() {
        XLabel xlabel = new XLabel(asm);
        asm.jmp(xlabel.getAssemblerLabel());
        jmpsToLeave = new UnboundJmp(xlabel, jmpsToLeave);
    }

    public void pop(Register reg) {
        if (reg.isLong()) {
            asm.popl(ralloc.registerLo(reg));
            asm.popl(ralloc.registerHi(reg));
        } else
            asm.popl(reg);
        ralloc.useReg(reg);
    }

    public void call(int addr) {
        asm.call(addr);
    }

    public void call(Register addr) {
        asm.call(addr);
        ralloc.freeReg(addr);
    }

    public void call(Label lab) {
        asm.call(((XLabel)lab).getAssemblerLabel());
    }

    public void call(Address addr) {
        asm.call(addr);
    }

    public void push(int literal) {
        asm.pushl(literal);
    }

    public void push(Register reg) {
        if (reg.isLong()) {
            asm.pushl(ralloc.registerHi(reg));
            asm.pushl(ralloc.registerLo(reg));
        } else {
            asm.pushl(reg);
        }
        ralloc.freeReg(reg);
    }

    public void push(Address addr) {
        asm.pushl(addr);
    }

    public void push(long longLit) {
        // push hi and low in that order, as the stack grows downwards
        asm.pushl((int)(longLit >> 32));
        asm.pushl((int)longLit);
    }

    public void pushObject(Object obj) {
        XLabel lab = new XLabel(asm);
        Register reg = ralloc.nextAvailableRegister();
        asm.leal(reg, new Address(lab.getAssemblerLabel()));
        asm.pushl(reg);
        arraysOfData = new UnboundDataBytes(lab, obj, arraysOfData);
    }

    public void dataBytes(Label lab, Object obj) {
        arraysOfData = new UnboundDataBytes(lab, obj, arraysOfData);
    }

    public Label getLabelFor(Object obj) {
        UnboundDataBytes data = arraysOfData;
        Label lab = null;
        while (data != null) {
            if (data.object == obj) {
                lab = data.label;
                data = null;
            }
            else
                data = data.next;
        }
        return lab;
    }

    public Register dup(Register reg) {
        Register regCopy;
        if (reg.isLong()) {
            regCopy = ralloc.nextAvailableLongRegister();
            load(reg, regCopy);
            ralloc.useReg(reg);  // load instruction frees reg
        } else {
            regCopy = ralloc.nextAvailableRegister();
            load(reg, regCopy);
            ralloc.useReg(reg);  // load instruction frees reg
        }
        return regCopy;
    }

    public int getJumpSize() {
        return JUMPSIZE;
    }

    /**
     * Get a single byte of a jump instruction sequence.
     *
     * @param bytecodes the address of the bytecode array
     * @param interp the address of the interpreter (the target routine)
     * @param offset the offset to the byte to return
     * @return the byte
     */
    public int getJumpByte(int bytecodes, int interp, int offset) {
        if (offset == 0) {
            return 0xe9;
        }

        int res = interp - bytecodes - 5;
        offset--;

        /*
         * x86 is little endian.
         */while (offset-- > 0) {
            res >>>= 8;
        }
        return res & 0xFF;
    }

    /**
     * Peek the receiver in the runtime stack.
     *
     */
    public Register peekReceiver() {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, new Address(ESP));
        ralloc.useReg(reg);
        return reg;
    }

    public Register convertIntToByte(int value, Type type) {
        Register reg = ralloc.nextAvailableByteRegister();
        asm.movl(reg, value);
        ralloc.useReg(reg);
        return convertIntToByte(reg, type);
    }

    public Register convertIntToByte(Register reg, Type type) {
        asm.andl(reg, 0xFF);
        if (type == BYTE)
            asm.movsxb(reg, reg);
        else // UBYTE
            asm.movzxb(reg, reg);
        return reg;
    }

    public Register convertIntToByte(Address value, Type type) {
        Register reg = ralloc.nextAvailableByteRegister();
        asm.movl(reg, value);
        ralloc.useReg(reg);
        return convertIntToByte(reg, type);
    }

    public Register convertIntToShort(int value, Type type) {
        Register reg = ralloc.nextAvailableShortRegister();
        asm.movl(reg, value);
        ralloc.useReg(reg);
        return convertIntToShort(reg, type);
    }

    public Register convertIntToShort(Register reg, Type type) {
        asm.andl(reg, 0xFF);
        if (type == SHORT)
            asm.movsxw(reg, reg);
        else // USHORT
            asm.movzxw(reg, reg);
        return reg;
    }

    public Register convertIntToShort(Address value, Type type) {
        Register reg = ralloc.nextAvailableByteRegister();
        asm.movl(reg, value);
        ralloc.useReg(reg);
        return convertIntToShort(reg, type);
    }

    public Register convertIntToLong(int value) {
        Register reg = ralloc.nextAvailableLongRegister();
        asm.movl(ralloc.registerLo(reg), value);

        // sign extend the top bit
        if ((value & 0x80000000) == 0x80000000) {
            asm.movl(ralloc.registerHi(reg), 0xFFFFFFFF);
        } else {
            asm.movl(ralloc.registerHi(reg), 0);
        }

        ralloc.useReg(reg);
        return reg;
    }

    public Register convertIntToLong(Register value) {
        Register reg = ralloc.nextAvailableLongRegister();
        asm.movl(ralloc.registerLo(reg), value);
        ralloc.useReg(reg);

        // sign extend the top bit
        ALabel labSignExtend = new ALabel(asm);
        ALabel labDone = new ALabel(asm);

        asm.movl(value, value);
        asm.andl(value, 0x80000000);
        asm.cmpl(value, 0x80000000);
        asm.jcc(EQUAL, labSignExtend);

        asm.movl(ralloc.registerHi(reg), 0);
        asm.jmp(labDone);

        labSignExtend.bindTo(asm.getCodePos());
        asm.movl(ralloc.registerHi(reg), 0xFFFFFFFF);

        labDone.bindTo(asm.getCodePos());
        ralloc.freeReg(value);
        return reg;
    }

    public Register convertIntToLong(Address value) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, value);
        ralloc.useReg(reg);
        return convertIntToLong(reg);
    }

    public Register convertUintToUlong(int value) {
        Register reg = ralloc.nextAvailableLongRegister();
        asm.movl(ralloc.registerLo(reg), value);
        asm.movl(ralloc.registerHi(reg), 0);  // zero extend
        ralloc.useReg(reg);
        return reg;
    }

    public Register convertUintToUlong(Register value) {
        Register reg = ralloc.nextAvailableLongRegister();
        asm.movl(ralloc.registerLo(reg), value);
        asm.movl(ralloc.registerHi(reg), 0);  // zero extend
        ralloc.freeReg(value);
        return reg;
    }

    public Register convertUintToUlong(Address value) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, value);
        ralloc.useReg(reg);
        return convertUintToUlong(reg);
    }

    public Register convertLongToInt(long value) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, (int)(value));
        ralloc.useReg(reg);
        return reg;
    }

    public Register convertLongToInt(Register value) {
        ralloc.freeReg(ralloc.registerHi(value));
        return ralloc.registerLo(value);
    }

    public Register convertLongToInt(Address value) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, value);
        ralloc.useReg(reg);
        return reg;
    }

    /*
     * Support methods
     */

    private Register moveAddressToRegister(Address addr) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, addr);
        ralloc.useReg(reg);
        return reg;
    }

    private Register moveImmediateToRegister(int immed) {
        Register reg = ralloc.nextAvailableRegister();
        asm.movl(reg, immed);
        ralloc.useReg(reg);
        return reg;
    }

    private Register moveAddressToLongRegister(Address addr) {
        Register reg = ralloc.nextAvailableLongRegister();
        asm.movl(ralloc.registerLo(reg), addr);
        asm.movl(ralloc.registerHi(reg), addr.offsetFrom(4));
        ralloc.useReg(reg);
        return reg;
    }

    private Register moveAddressToRegister(Address addr, Type type) {
        Register dstReg;
        if (type.getStructureSize() == 8) {
            dstReg = ralloc.nextAvailableLongRegister();
            asm.movl(ralloc.registerLo(dstReg), addr);
            asm.movl(ralloc.registerHi(dstReg), addr.offsetFrom(4));
        } else {
            dstReg = ralloc.nextAvailableRegister();
            asm.movl(dstReg, addr);
        }
        ralloc.useReg(dstReg);
        return dstReg;
    }

    private Register moveImmediateToLongRegister(long immed) {
        Register reg = ralloc.nextAvailableLongRegister();
        asm.movl(ralloc.registerLo(reg), (int)immed);
        asm.movl(ralloc.registerHi(reg), (int)(immed>>32));
        ralloc.useReg(reg);
        return reg;
    }

    /**
     * Test the order of parameter passing.
     * *** This is an ABI method ***
     *
     * @return true if parameters are pushed in reverse postorder (like x86), \
     *         false if they are pushed in Java order (like SPARC).
     */
    public boolean javaOrder() {
        return false;
    }

}

/**
 * Private class to hold the information for unbound jmp instructions.
 */
class UnboundJmp {
    Label label;
    UnboundJmp next;

    UnboundJmp(Label label, UnboundJmp next) {
        this.label = label;
        this.next = next;
    }
}

/**
 * Private class to hold objects of data to be code-generated after the leave instruction.
 */
class UnboundDataBytes {
    Label label;
    Object object;
    UnboundDataBytes next;

    UnboundDataBytes(Label label, Object object, UnboundDataBytes next) {
        this.label = label;
        this.object = object;
        this.next = next;
    }
}
