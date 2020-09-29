/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.asm.x86.*;
import com.sun.squawk.compiler.ShadowStack;
import com.sun.squawk.util.Stack;
import com.sun.squawk.util.Hashtable;
import com.sun.squawk.compiler.SymbolicValueDescriptor;

/**
 * Instruction class.
 *
 * The Instructions class partly implements the Compiler interface,
 * it deals with all the instructions that are being emitted to the Compiler
 * stream.
 *
 *  The Instructions data structure is a sequence of individual instructions
 *  for an abstract stack machine, some which have side effects other than
 *  pushing or popping values from the abstract stack.  Therefore, state
 *  information is stored in this data structure, as the sequence is build.
 *
 * NB: methods from the Compiler interface not included in this implementation:
 *      all the code generation options methods
 *      all the compilation methods
 *      alloca(), deadCode()
 */

class Instr implements Constants, Codes, ShadowStackConstants, Types {

    /**
     * The register allocator used by this compiler
     */
    private RegisterAllocator ralloc;

    /**
     * The instruction emitter
     */
    private Emitter emitter;

    /**
     * The special preamble code; used when MP and IP are defined
     */
    private int specialPreamble;

    /**
     * Keep track of whether MP and IP were defined in the local stack frame
     */
    private boolean definedIP = false;
    private boolean definedMP = false;

    /**
     * The address of the current method.
     *
     * This address is used by the special preamble E_ADDRESS, which stores
     * the address onto the MP slot.
     */
    private Label methodAddr;

    /**
     * Slot for stack pointer (SS) in the activation record
     */
    private Local SSslot;

    /**
     * Hashtable of symbols (String) to values (address offsets) (int);
     * information to be fixed up by the linker.
     */
    private Hashtable fixupInfo;


    /**
     * Constructor
     */
    public Instr(Compiler compiler) {
        ralloc = new RegisterAllocator();
        emitter = new Emitter(ralloc, compiler);
        SSslot = null;
        fixupInfo = new Hashtable();
    }

    /**
     * Return the offsets to unresolved symbols.
     */
    public Hashtable getFixupInfo() {
        return fixupInfo;
    }

    /**
     * Relocates the target addresses in the emitted code buffer to be
     * relative to the given address.
     *
     * @param address the relative address for relocation
     * @return an array of relocation information
     */
    public int[] relocate(int address) {
        return emitter.relocate(address);
    }

    /*-----------------------------------------------------------------------*\
     *                         Code generation options                       *
    \*-----------------------------------------------------------------------*/

    /**
     * Get the length of a jump instruction.
     */
    public int getJumpSize() {
        return emitter.getJumpSize();
    }

    public int getJumpByte(int bytecodes, int interp, int offset) {
        return emitter.getJumpByte(bytecodes, interp, offset);
    }

    /**
     * Disassembles the emitted code buffer.
     *
     */
    public void decode() {
        emitter.decode();
    }


    /*-----------------------------------------------------------------------*\
     *                            Label management                           *
    \*-----------------------------------------------------------------------*/

    /**
     * Allocate a label.
     */
    public Label label() {
        XLabel xlabel = new XLabel(emitter.getAssembler());
        return xlabel;
    }

    /**
     * Bind a label to the current code generation location.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param label the label to bind
     */
    public void bind(Label label) {
        emitter.bind(label);
    }

    /*-----------------------------------------------------------------------*\
     *          Stack manipulation (for interpreters and compilers)          *
    \*-----------------------------------------------------------------------*/

    /**
     * Allocate space on the runtime stack.
     *
     * @param numBytes the number of bytes to allocate on the stack.
     *
     * NB: This method needs an update for 64-bit compilations.
     */
    public void alloca(SymbolicValueDescriptor numBytes) {
        switch (numBytes.getSymbolicValueDescriptor()) {
            case S_LIT:
                if (((SymbolicLiteral)numBytes).getLiteralSize() == 32) {
                    int litNumBytes = ((SymbolicLiteral32)numBytes).getLiteral();
                    if (SSslot != null) {
                        emitter.allocaSaveSS(litNumBytes, addressOf(SSslot));
                    } else {
                        emitter.alloca(litNumBytes);
                    }
                } else {
                    throw new RuntimeException("alloca: expecting 32-bit size");
                }
                break;
            case S_REG:
                if (SSslot != null) {
                    emitter.allocaSaveSS(((SymbolicRegister)numBytes).getRegister(), addressOf(SSslot));
                } else {
                    emitter.alloca(((SymbolicRegister)numBytes).getRegister());
                }
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)numBytes).getLocal();
                if (SSslot != null) {
                    emitter.allocaSaveSS(addressOf(loc), addressOf(SSslot));
                } else {
                    emitter.alloca(addressOf(loc));
                }
                break;
            default:
                throw new RuntimeException("alloca: invalid symbolic value descriptor");
        }

    }


    /*-----------------------------------------------------------------------*\
     *                           Function definition                         *
    \*-----------------------------------------------------------------------*/

    /**
     * Emit a function prologue.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param label a label to be bound to the start of the function
     */
    public void enter(Label label, int preambleCode) {
        // *** this will need an interface for diff "enters" on diff machines;

        // reset MP and IP booleans, set special preamble code
        definedMP = false;
        definedIP = false;
        specialPreamble = preambleCode;

        // bind label to address of this method
        if (specialPreamble == Compiler.E_ADDRESS) {
            methodAddr = label();
            emitter.bind(methodAddr);
        }
        emitter.enter(label);
        // ** missing binding the label to anything useful, if label <> null
    }

    /**
     * Define a parameter variable.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param type the type of the parameter (Must be primary)
     */
    public Local parm(Type type, int offset) {
        XLocal param = new XLocal(type, offset, true);
        return param;
    }

    /**
     * Emit a function epilogue.
     *
     * If any space was reserved on the stack frame for locals, we need to
     * backpatch the amount of bytes reserved to that specified by localsOffset.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     */
    public void leave(int localsOffset) {
        // Check that MP and IP were defined.
        switch(specialPreamble) {
            case Compiler.E_REGISTER:
                Assert.that(definedIP, " IP not defined in compilation and " +
                            "using special preamble");
            case Compiler.E_ADDRESS:
            case Compiler.E_NULL:
                Assert.that(definedMP, " MP not defined in compilation and " +
                            "using special preamble");
        }

        // emit the procedure epilogue code and back-patch the space allocated
        // for locals, if needed
        emitter.leave(localsOffset);
    }

    /*-----------------------------------------------------------------------*\
     *                           Scope definition                            *
    \*-----------------------------------------------------------------------*/

    /**
     * Define a local variable type.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param type the type of the local variable (Must be primary, or MP, or IP)
     * *** Need to check that they are all in the right order, if MP etc exist
     */
    public Local local(Type type, int offset) {
        X86XLocal loc = new X86XLocal(type, offset, false);   /***** x86-dependent ***/
        if (type == MP) {
            definedMP = true;
            emitter.localMP(specialPreamble, methodAddr);
        } else if (type == IP) {
            definedIP = true;
        }
        if (type == Type.SS) {
            SSslot = loc;
        }
        return loc;
    }

    /**
     * Get the value of a parameter word at the given index.
     *
     * The index is of type INT, this type has been checked prior to invocation
     * of this methods.
     */
    public SymbolicValueDescriptor loadParm(SymbolicValueDescriptor index) {
        Register dstReg = ralloc.nextAvailableRegister();
        Type dstType = Type.INT;
        switch (index.getSymbolicValueDescriptor()) {
            case S_LIT:
                SymbolicLiteral sindex = (SymbolicLiteral)index;
                emitter.loadParam(((SymbolicLiteral32)sindex).getLiteral(), dstReg);
                break;
            case S_REG:
                SymbolicRegister sreg = (SymbolicRegister)index;
                emitter.loadParam(((SymbolicRegister32)sreg).getRegister(), dstReg);
                break;
            case S_LOCAL:
                Local sloc = ((SymbolicLocal)index).getLocal();
                emitter.loadParam(addressOf(sloc), dstReg);
                break;
            default:
                throw new RuntimeException("Load parameter: symbolic value descriptor not supported");
        }
        ralloc.useReg(dstReg);
        return new SymbolicRegister32(dstReg, dstType);
    }

    /**
     * Set the value of a parameter word.
     */
    public void storeParm(SymbolicValueDescriptor index, SymbolicValueDescriptor value) {
        switch (index.getSymbolicValueDescriptor()) {
            case S_LIT:
                SymbolicLiteral sindex = (SymbolicLiteral)index;
                storeParmIntIndex(((SymbolicLiteral32)sindex).getLiteral(), value);
                break;
            case S_REG:
                SymbolicRegister sreg = (SymbolicRegister)index;
                storeParmRegIndex(((SymbolicRegister32)sreg).getRegister(), value);
                break;
            case S_LOCAL:
                Local sloc = ((SymbolicLocal)index).getLocal();
                storeParmAddrIndex(addressOf(sloc), value);
                break;
            default:
                throw new RuntimeException("Load parameter: symbolic value descriptor not supported");
        }
    }

    private void storeParmIntIndex(int index, SymbolicValueDescriptor value) {
        Register indexReg = ralloc.nextAvailableRegister();
        emitter.load(index, indexReg);
        ralloc.useReg(indexReg);
        storeParmRegIndex(indexReg, value);
    }

    private void storeParmRegIndex(Register indexReg, SymbolicValueDescriptor value) {
        Address parmAddr = new Address(EBP, indexReg, 4, 8);
        switch (value.getSymbolicValueDescriptor()) {
            case S_LIT:
                storeLiteral((SymbolicLiteral)value, parmAddr);
                break;
            case S_REG:
                Register reg = ((SymbolicRegister32)value).getRegister();
                emitter.load(reg, parmAddr);
                break;
            case S_LOCAL:
                Local sloc = ((SymbolicLocal)value).getLocal();
                emitter.load(addressOf(sloc), parmAddr);
                break;
            default:
                throw new RuntimeException("Store parameter: symbolic value descriptor not supported");
        }
        ralloc.freeReg(indexReg);
    }

    private void storeParmAddrIndex(Address indexAddr, SymbolicValueDescriptor value) {
        Register indexReg = ralloc.nextAvailableRegister();
        emitter.load(indexAddr, indexReg);
        ralloc.useReg(indexReg);
        storeParmRegIndex(indexReg, value);
    }

    /**
     * Add a comment node to the IR.
     *
     * @param str the comment
     */
    public void comment(String str) {
        emitter.comment(str);
    }


    /*-----------------------------------------------------------------------*\
     *                            Load/store                                 *
    \*-----------------------------------------------------------------------*/

    /**
     * Get a local variable or parameter and push it onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param local the local variable to load
     */
    public void load(Local local) {
        // *** we only need to check that local is in scope
        // no code gets generated
    }

    private void storeLiteral(SymbolicLiteral value, Address addr) {
        if (value.getLiteralSize() == 32) {
            int val = ( (SymbolicLiteral32) value).getLiteral();
            emitter.load(val, addr);
        }
        else { // 64
            long val = ( (SymbolicLiteral64) value).getLiteral();
            emitter.load(val, addr);
        }
    }

    /**
     * Set a local variable or parameter to a value popped from the stack.
     * The type of the local and the value is either a primary type or one of
     * MP/IP/LP/SS (i.e., pointer types).
     *
     * @param dst the local variable to store into
     * @param val the value to be stored at the given local
     */
    public void store(Local dst, SymbolicValueDescriptor val) {
        Address dstAddr = addressOf(dst);
        Type dstType = ((XLocal)dst).getType();
        int typeCode = dstType.getTypeCode();
        switch (typeCode) {
            case Type.Code_I:
                storeInt(dst, val);
                break;
            case Type.Code_U:
                storeUint(dst, val);
                break;
            case Type.Code_R:
                storeRef(dst, val);
                break;
            case Type.Code_O:
                storeOop(dst, val);
                break;
            case Type.Code_L:
                storeLong(dst, val);
                break;
            case Type.Code_F:
            case Type.Code_D:
                throw new RuntimeException("Store: float and double not supported");
            default:
                throw new RuntimeException("Store: invalid local type");
        }
    }

    public void storeInt(Local dst, SymbolicValueDescriptor val) {
        Address dstAddr = addressOf(dst);
        switch (val.getSymbolicValueDescriptor()) {
            case S_LIT:
                storeLiteral((SymbolicLiteral)val, dstAddr);
                break;
            case S_LOCAL:
                Local src = ((SymbolicLocal)val).getLocal();
                if (src != dst)
                    emitter.load(addressOf(src), dstAddr);
                break;
            case S_REG:
                Register reg = ((SymbolicRegister32)val).getRegister();
                emitter.load(reg, dstAddr);
                break;
            case S_FIXUP_SYM:
                int relocationCode = relocationAbsolute(getCodeOffset(), S_LOCAL);
                emitter.load(0, dstAddr);   // to be fixed up
                fixupInfo.put(new Integer(relocationCode),
                              ( (SymbolicFixupSymbol) val).getSymbol());
                break;
            default:
                throw new RuntimeException("StoreInt: illegal symbolic value descriptor");
        }
    }

    public void storeUint(Local dst, SymbolicValueDescriptor val) {
        storeInt(dst, val);
    }

    public void storeRef(Local dst, SymbolicValueDescriptor val) {
        storeInt(dst, val);
    }

    public void storeOop(Local dst, SymbolicValueDescriptor val) {
        storeRef(dst, val);
    }

    public void storeLong(Local dst, SymbolicValueDescriptor val) {
        Address dstAddr = addressOf(dst);
        switch (val.getSymbolicValueDescriptor()) {
            case S_LIT:
                storeLiteral((SymbolicLiteral)val, dstAddr);
                break;
            case S_LOCAL:
                Local src = ((SymbolicLocal)val).getLocal();
                if (src != dst) {
                    emitter.load(addressOf(src), dstAddr);
                    emitter.load(addressOf(src, 4), dstAddr.offsetFrom(4));
                }
                break;
            case S_REG:
                Register hi = ((SymbolicRegister64)val).getRegisterHi();
                Register lo = ((SymbolicRegister64)val).getRegisterLo();
                emitter.load(hi, lo, dstAddr);
                break;
            default:
                throw new RuntimeException("StoreInt: illegal symbolic value descriptor");
        }
    }

    /**
     * Load a value from a reference.
     *
     * @param type the type of the data to load (previously checked to be primary or secondary)
     */
    public SymbolicValueDescriptor read(Type type, SymbolicValueDescriptor val) {
        if ((type == Type.INT) || (type == Type.UINT))
            return readInt(val, type);
        else if ((type == Type.REF) || (type == Type.OOP))
            return readRef(val, type);
        else if (type == Type.LONG)
            return readLong(val);
        /* if [FLOATS] */
        else if ((type == Type.FLOAT) || (type == Type.DOUBLE))
            throw new RuntimeException("Read: float and double types not supported");
        /* end [FLOATS] */
        else if ( (type == Type.BYTE) || (type == Type.UBYTE))
            return readByte(val, type);
        else if ( (type == Type.SHORT) || (type == Type.USHORT))
            return readShort(val, type);
        else
            throw new RuntimeException("Read: illegal type");
    }

    // type is either INT or UINT
    private SymbolicValueDescriptor readInt(SymbolicValueDescriptor val, Type type) {
        return readIntegral(val, type, type);  // result type is either INT or UINT
    }

    private SymbolicValueDescriptor readByte(SymbolicValueDescriptor val, Type type) {
        return readIntegral(val, type, Type.INT);  // result type is always INT
    }

    private SymbolicValueDescriptor readShort(SymbolicValueDescriptor val, Type type) {
        return readIntegral(val, type, Type.INT);  // result type is always INT
    }

    private SymbolicValueDescriptor readIntegral(SymbolicValueDescriptor val,
                                                 Type type, Type resultType) {
        Register dstReg = ralloc.nextAvailableRegister();
        ralloc.useReg(dstReg);
        readLiteral(val, dstReg, type);
        return new SymbolicRegister32(dstReg, resultType);
    }

    private SymbolicValueDescriptor readRef(SymbolicValueDescriptor val, Type type) {
        // *** need to check if REF is 64b and return LONG Register instead
        return readInt(val, type);
    }

    private SymbolicValueDescriptor readLong(SymbolicValueDescriptor val) {
        Register dstReg = ralloc.nextAvailableLongRegister();
        ralloc.useReg(dstReg);
        readLiteral(val, dstReg, Type.LONG);
        return new SymbolicRegister64(dstReg, Type.LONG);
    }

    private void readLiteral(SymbolicValueDescriptor val, Register dstReg, Type type) {
        switch (val.getSymbolicValueDescriptor()) {
            case S_LOCAL:
                Local src = ((SymbolicLocal)val).getLocal();
                emitter.deref(addressOf(src), dstReg, type);
                break;
            case S_REG:
                Register srcReg = ( (SymbolicRegister32) val).getRegister();
                ralloc.useReg(srcReg);  // ** isn't this redundant? **
                emitter.deref(srcReg, dstReg, type);
                break;
            case S_LIT:
                int srcLit = ((SymbolicLiteral32)val).getLiteral();
                emitter.deref(srcLit, dstReg, type);
                break;
            case S_FIXUP_SYM:
                String name = ((SymbolicFixupSymbol)val).getSymbol();
                int relocationCode = relocationAbsolute(getCodeOffset(), S_LIT);
                emitter.deref(0, dstReg, type);   // to be fixed up
                fixupInfo.put(new Integer(relocationCode), name);
                break;
            default:
                throw new RuntimeException("ReadLiteral: symbolic value descriptor not supported");
        }
    }

    /**
     * Store a value at a reference.
     * Note that secondary types are allowed.
     *
     * @param type the type of the data to load (previously checked to be any type other than VOID)
     */
    public void write(SymbolicValueDescriptor val, SymbolicValueDescriptor addr,
                      Type type) {
        switch (type.getTypeCode()) {
            case Type.Code_I:
            case Type.Code_U:
                writeInt(val, addr, type);
                break;
            case Type.Code_R:
            case Type.Code_O:
                writeRef(val, addr, type);
                break;
            case Type.Code_L:
                writeLong(val, addr);
                break;
            case Type.Code_F:
            case Type.Code_D:
                throw new RuntimeException("Write: float and double not supported");
            case Type.Code_B:
            case Type.Code_A:
                writeByte(val, addr, type);
                break;
            case Type.Code_S:
            case Type.Code_C:
                writeShort(val, addr, type);
                break;
            default:
                throw new RuntimeException("Write: invalid type");
        }
    }

    private void writeInt(SymbolicValueDescriptor val, SymbolicValueDescriptor addr,
                          Type type) {
        switch (addr.getSymbolicValueDescriptor()) {
            case S_LOCAL:
                Local loc = ( (SymbolicLocal) addr).getLocal();
                writeIntToLocal(val, loc, type);
                break;
            case S_REG:
                Register reg = ( (SymbolicRegister32) addr).getRegister();
                writeIntToReg(val, reg, type);
                break;
            case S_LIT:
                int lit = ((SymbolicLiteral32)addr).getLiteral();
                writeIntToLiteral(val, lit, type);
                break;
            case S_FIXUP_SYM:
                String name = ((SymbolicFixupSymbol)addr).getSymbol();
                writeIntToFixupSymbol(val, name, type);
                break;
            default:
                throw new RuntimeException("Write: illegal symbolic value descriptor");
        }
    }

    private void writeIntToReg(SymbolicValueDescriptor val, Register addr, Type type) {
        switch (val.getSymbolicValueDescriptor()) {
            case S_REG:
                Register reg = ( (SymbolicRegister32) val).getRegister();
                emitter.writeRef(reg, addr, type);
                break;
            case S_LIT:
                int lit = ((SymbolicLiteral32)val).getLiteral();
                emitter.writeRef(lit, addr, type);
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)val).getLocal();
                emitter.writeRef(addressOf(loc), addr, type);
                break;
            default:
                throw new RuntimeException("Write integral to register: illegal symbolic value descriptor");
        }
    }

    private void writeIntToLocal(SymbolicValueDescriptor val, Local addr, Type type) {
        switch (val.getSymbolicValueDescriptor()) {
            case S_REG:
                Register reg = ( (SymbolicRegister32) val).getRegister();
                emitter.writeRef(reg, addressOf(addr), type);
                break;
            case S_LIT:
                int lit = ((SymbolicLiteral32)val).getLiteral();
                emitter.writeRef(lit, addressOf(addr), type);
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)val).getLocal();
                emitter.writeRef(addressOf(loc), addressOf(addr), type);
                break;
            default:
                throw new RuntimeException("Write integral to local: illegal symbolic value descriptor");
        }
    }

    private void writeIntToLiteral(SymbolicValueDescriptor val, int addr, Type type) {
        switch (val.getSymbolicValueDescriptor()) {
            case S_REG:
                Register reg = ( (SymbolicRegister32) val).getRegister();
                emitter.writeRef(reg, addr, type);
                break;
            case S_LIT:
                int lit = ((SymbolicLiteral32)val).getLiteral();
                emitter.writeRef(lit, addr, type);
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)val).getLocal();
                emitter.writeRef(addressOf(loc), addr, type);
                break;
            default:
                throw new RuntimeException("Write integral to literal: illegal symbolic value descriptor");
        }
    }

    private void writeIntToFixupSymbol(SymbolicValueDescriptor val, String addr,
                                       Type type) {
        int relocationCode = relocationAbsolute(getCodeOffset(), S_LIT);
        writeIntToLiteral(val, 0, type);
        fixupInfo.put(new Integer(relocationCode), addr);
    }

    private void writeRef(SymbolicValueDescriptor val, SymbolicValueDescriptor addr,
                          Type type) {
        // ** need to check if Reference is 64b
        writeInt(val, addr, type);
    }

    // NB: This method needs updating for 64-bit compilation.
    private void writeLong(SymbolicValueDescriptor val, SymbolicValueDescriptor addr) {
        if (val.getType() != Type.LONG)
            throw new RuntimeException("Write long: expecting long typed value");

        switch (addr.getSymbolicValueDescriptor()) {
            case S_LOCAL:
                Local loc = ((SymbolicLocal)addr).getLocal();
                writeLongToLocal(val, loc);
                break;
            case S_REG:
                Register reg = ((SymbolicRegister32)addr).getRegister();
                writeLongToReg(val, reg);
                break;
            case S_LIT:
                int lit = ((SymbolicLiteral32)addr).getLiteral();
                writeLongToLiteral(val, lit);
                break;
            default:
                throw new RuntimeException("Write: illegal symbolic value descriptor");
        }
    }

    private void writeLongToReg(SymbolicValueDescriptor val, Register addr) {
        switch (val.getSymbolicValueDescriptor()) {
            case S_REG:
                Register reg = ( (SymbolicRegister64) val).getRegister();
                emitter.writeRef(reg, addr, Type.LONG);
                break;
            case S_LIT:
                long lit = ((SymbolicLiteral64)val).getLiteral();
                emitter.writeRef(lit, addr, Type.LONG);
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)val).getLocal();
                emitter.writeRef(addressOf(loc), addr, Type.LONG);
                break;
            default:
                throw new RuntimeException("Write long to register: illegal symbolic value descriptor");
        }
    }

    private void writeLongToLocal(SymbolicValueDescriptor val, Local addr) {
        switch (val.getSymbolicValueDescriptor()) {
            case S_REG:
                Register reg = ( (SymbolicRegister64) val).getRegister();
                emitter.writeRef(reg, addressOf(addr), Type.LONG);
                break;
            case S_LIT:
                long lit = ((SymbolicLiteral64)val).getLiteral();
                emitter.writeRef(lit, addressOf(addr), Type.LONG);
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)val).getLocal();
                emitter.writeRef(addressOf(loc), addressOf(addr), Type.LONG);
                break;
            default:
                throw new RuntimeException("Write long to local: illegal symbolic value descriptor");
        }
    }

    private void writeLongToLiteral(SymbolicValueDescriptor val, int addr) {
        switch (val.getSymbolicValueDescriptor()) {
            case S_REG:
                Register reg = ( (SymbolicRegister64) val).getRegister();
                emitter.writeRef(reg, addr, Type.LONG);
                break;
            case S_LIT:
                long lit = ((SymbolicLiteral64)val).getLiteral();
                emitter.writeRef(lit, addr, Type.LONG);
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)val).getLocal();
                emitter.writeRef(addressOf(loc), addr, Type.LONG);
                break;
            default:
                throw new RuntimeException("Write long to litearl: illegal symbolic value descriptor");
        }
    }

    private void writeByte(SymbolicValueDescriptor val, SymbolicValueDescriptor addr,
                           Type type) {
        writeInt(val, addr, type);
    }

    private void writeShort(SymbolicValueDescriptor val, SymbolicValueDescriptor addr,
                            Type type) {
        writeInt(val, addr, type);
    }

    // support routine for compiler.symbol()
    public int getCodeOffset() {
        return emitter.getCodeOffset();
    }

    /**
     * Define some data.
     */
    public void data(Label label, Object obj) {
        emitter.dataBytes(label, obj);
    }

    /**
     * Checks:
     * 1- if the value is a register, the register(s) needs to be freed up
     */
    public void drop(SymbolicValueDescriptor value) {
        if (value.getSymbolicValueDescriptor() == S_REG) {
            ralloc.freeReg(((SymbolicRegister)value).getRegister());
        }
    }

    public SymbolicValueDescriptor dup(SymbolicValueDescriptor value) {
        SymbolicValueDescriptor aCopy = null;
        Type dstType;
        switch (value.getSymbolicValueDescriptor()) {
            case S_LIT:
                aCopy = value;
                break;
            case S_REG:
                aCopy = dupReg((SymbolicRegister)value);
                break;
            case S_LOCAL:
                aCopy = dupLocal(((SymbolicLocal)value).getLocal());
                // *** if there are no registers available; we need to make a copy
                // on the stack.
                break;
            default:
                throw new RuntimeException("Dup: illegal symbolic value descriptor");
        }
        return aCopy;
    }

    private SymbolicValueDescriptor dupReg(SymbolicRegister sreg) {
        Register regCopy = emitter.dup(((SymbolicRegister)sreg).getRegister());
        Type dstType = ((SymbolicRegister)sreg).getType();
        if (regCopy.isLong())
            return new SymbolicRegister64(regCopy, dstType);
        else
            return new SymbolicRegister32(regCopy, dstType);
    }

    private SymbolicValueDescriptor dupLocal(Local loc) {
        Register srcReg, regCopy;
        Type dstType = ((XLocal)loc).getType();
        if (dstType == Type.LONG) {
            regCopy = ralloc.nextAvailableLongRegister();
            emitter.load(addressOf(loc), regCopy);
            return new SymbolicRegister64(regCopy, dstType);
        } else {  // int
            regCopy = ralloc.nextAvailableRegister();
            emitter.load(addressOf(loc), regCopy);
            return new SymbolicRegister32(regCopy, dstType);
        }
    }

    /**
     * Ensure that there is enough stack.
     *
     * NB: The real implementation of this method is missing.
     */
    public void stackCheck(SymbolicValueDescriptor extraStack, SymbolicValueDescriptor extraLocals) {
        ;

        // free up registers used, if any
        if (extraStack.getSymbolicValueDescriptor() == S_REG) {
            ralloc.freeReg(((SymbolicRegister)extraStack).getRegister());
        }
        if (extraLocals.getSymbolicValueDescriptor() == S_REG) {
            ralloc.freeReg(((SymbolicRegister)extraLocals).getRegister());
        }
    }

    /**
     * Push the data onto the runtime stack.
     *
     * <p>
     * Stack: ..., VALUE -> ...
     * <p>
     *
     */
    public void push(SymbolicValueDescriptor value) {
        // ** check that value is of type primary ***???
        switch (value.getSymbolicValueDescriptor()) {
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral) value;
                if (slit.getLiteralSize() == 32) {
                    int lit = ( (SymbolicLiteral32) slit).getLiteral();
                    emitter.push(lit);
                } else { // 64
                    long lit = ( (SymbolicLiteral64) slit).getLiteral();
                    emitter.push(lit);
                }
                break;
            case S_REG:
                Register reg = ( (SymbolicRegister) value).getRegister();
                emitter.push(reg);
                break;
            case S_LOCAL:
                Local sloc = ((SymbolicLocal)value).getLocal();
                emitter.push(addressOf(sloc));
                break;
            case S_OBJECT:
                Object obj = ( (SymbolicObject) value).getObject();
                emitter.pushObject(obj);
                break;
            default:
                throw new RuntimeException("Push: illegal symbolic value descriptor");
        }
    }

    /**
     * Pop the top element of the runtime stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param type the data type to pop (must be primary type)
     */
    public SymbolicValueDescriptor pop(Type type) {
        SymbolicValueDescriptor value;
        Register reg;
        switch (type.getStructureSize()) {
            case 4:
                reg  = ralloc.nextAvailableRegister();
                emitter.pop(reg);
                value = new SymbolicRegister32(reg, type);
                break;
            case 8:
                reg = ralloc.nextAvailableLongRegister();
                emitter.pop(reg);
                value = new SymbolicRegister64(reg, type);
                break;
            default:
                throw new RuntimeException("Pop: illegal type");
        }
        return value;
    }

    /**
     * Pop all the elements of the runtime stack.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     */
    public void popAll() {
        emitter.load(addressOf(SSslot), ESP);
    }

    /**
     * Peek the receiver in the runtime stack.
     *
     * <p>
     * Stack: ... -> ..., OOP
     * <p>
     *
     */
    public SymbolicValueDescriptor peekReceiver() {
        Register reg = emitter.peekReceiver();
        return new SymbolicRegister32(reg, Type.OOP);
    }

    /*
     * Force rules 1-3 have been checked prior to invocation of these methods (in the
     * X86Compiler).  These methods check rule 4 and return the appropriate forced
     * symbolic value.
     */

    // This method allows forcing of 32-bit integral values between the types {int, uint, ref, oop}
    private SymbolicValueDescriptor forceIntegralTo32(SymbolicValueDescriptor svalue,
        Type to) {
        switch (svalue.getSymbolicValueDescriptor()) {
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral) svalue;
                slit.setType(to);
                break;
            case S_REG:
                SymbolicRegister sreg = (SymbolicRegister) svalue;
                sreg.setType(to);
                break;
            case S_LOCAL:
                SymbolicLocal sloc = (SymbolicLocal) svalue;
                sloc.setType(to);
                break;
            default:
                throw new RuntimeException("force: symbolic value descriptor not implemented");
        }
        return svalue;
    }

    // this method is exactly the same as forceIntegralTo32(), therefore, one should
    // merge them into one, however, when floats and doubles get implemented, the
    // literal case will be different, as the variable holding the type would have
    // to be changed, e.g., from a long variable to a float variable.  For registers
    // and locals, only the type would need to be changed (as per now).
    private SymbolicValueDescriptor forceLongTo64(SymbolicValueDescriptor svalue, Type to) {
        switch (svalue.getSymbolicValueDescriptor()) {
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral) svalue;
                slit.setType(to);
                break;
            case S_REG:
                SymbolicRegister sreg = (SymbolicRegister) svalue;
                sreg.setType(to);
                break;
            case S_LOCAL:
                SymbolicLocal sloc = (SymbolicLocal) svalue;
                sloc.setType(to);
                break;
            default:
                throw new RuntimeException("force: symbolic value descriptor not implemented");
        }
        return svalue;
    }

    public SymbolicValueDescriptor forceIntTo(SymbolicValueDescriptor svalue, Type to) {
        /* if [FLOATS] */
        if (to == FLOAT) {
            throw new RuntimeException("Force of INT to FLOAT not supported yet");
        }
        /* end [FLOATS] */
        return forceIntegralTo32(svalue, to);
    }

    public SymbolicValueDescriptor forceUintTo(SymbolicValueDescriptor svalue, Type to) {
        /* if [FLOATS] */
        if (to == FLOAT) {
            throw new RuntimeException("Force of UINT to FLOAT is illegal");
        }
        /* end [FLOATS] */
        return forceIntegralTo32(svalue, to);
    }

    public SymbolicValueDescriptor forceRefTo(SymbolicValueDescriptor svalue, Type to) {
        /* if [FLOATS] */
        if (to == FLOAT) {
            throw new RuntimeException("Force of REF to FLOAT is illegal");
        }
        /* end [FLOATS] */
        return forceIntegralTo32(svalue, to);
    }

    public SymbolicValueDescriptor forceOopTo(SymbolicValueDescriptor svalue, Type to) {
        /* if [FLOATS] */
        if (to == FLOAT) {
            throw new RuntimeException("Force of OOP to FLOAT is illegal");
        }
        /* end [FLOATS] */
        return forceIntegralTo32(svalue, to);
    }

    public SymbolicValueDescriptor forceLongTo(SymbolicValueDescriptor svalue, Type to) {
        /* if [FLOATS] */
        if (to == DOUBLE) { // not done yet
            throw new RuntimeException("Force of LONG to DOUBLE not implemented yet");
        }
        /* end [FLOATS */
        return forceLongTo64(svalue, to);
    }

    public SymbolicValueDescriptor forceUlongTo(SymbolicValueDescriptor svalue, Type to) {
        /* if [FLOATS] */
        if (to == DOUBLE) {
            throw new RuntimeException("Force of ULONG to DOUBLE is illegal");
        }
        /* end [FLOATS] */
        return forceLongTo64(svalue, to);
    }

    /*
     * Rules 1 and 2 have been checked prior to these methods being invoked (in
     * the X86Compiler), as well as convertions to self.  The rest of rule 3 is
     * implemented by these methods.
     */

    public SymbolicValueDescriptor convertIntTo(SymbolicValueDescriptor svalue,
                                                Type to) {
        if (to == BYTE || to == UBYTE) {
            return convertIntToByte(svalue, to);
        }
        else if (to == SHORT || to == USHORT) {
            return convertIntToShort(svalue, to);
        }
        else if (to == LONG) {
            return convertIntToLong(svalue);
        }
        /* if [FLOATS] */
        else if (to == FLOAT || to == DOUBLE) {
            throw new RuntimeException("Float/double not supported yet");
        }
        /* end [FLOATS] */
        else {
            throw new RuntimeException("Illegal conversion from INT to " + to);
        }
    }

    private SymbolicValueDescriptor convertIntToByte(SymbolicValueDescriptor svalue,
                                                     Type type) {
        Register newvalue;
        switch (svalue.getSymbolicValueDescriptor()) {
            case S_LIT:
                int lit = ((SymbolicLiteral32)svalue).getLiteral();
                newvalue = emitter.convertIntToByte(lit, type);
                break;
            case S_REG:
                Register reg = ((SymbolicRegister)svalue).getRegister();
                newvalue = emitter.convertIntToByte(reg, type);
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)svalue).getLocal();
                newvalue = emitter.convertIntToByte(addressOf(loc), type);
                break;
            default:
                throw new RuntimeException("Convert int to (u)byte: stack value not supported");
        }
        if (type == BYTE)
            return new SymbolicRegister32(newvalue, BYTE);
        else // UBYTE
            return new SymbolicRegister32(newvalue, UBYTE);
    }

    private SymbolicValueDescriptor convertIntToShort(SymbolicValueDescriptor svalue,
                                                      Type type) {
        Register newvalue;
        switch (svalue.getSymbolicValueDescriptor()) {
            case S_LIT:
                int lit = ( (SymbolicLiteral32) svalue).getLiteral();
                newvalue = emitter.convertIntToShort(lit, type);
                break;
            case S_REG:
                Register reg = ( (SymbolicRegister) svalue).getRegister();
                newvalue = emitter.convertIntToShort(reg, type);
                break;
            case S_LOCAL:
                Local loc = ( (SymbolicLocal) svalue).getLocal();
                newvalue = emitter.convertIntToShort(addressOf(loc), type);
                break;
            default:
                throw new RuntimeException(
                    "Convert int to (u)short: stack value not supported");
        }
        if (type == SHORT)
            return new SymbolicRegister32(newvalue, SHORT);
        else // USHORT
            return new SymbolicRegister32(newvalue, USHORT);
    }

    private SymbolicValueDescriptor convertIntToLong(SymbolicValueDescriptor svalue) {
        Register newvalue;
        switch (svalue.getSymbolicValueDescriptor()) {
            case S_LIT:
                int lit = ( (SymbolicLiteral32) svalue).getLiteral();
                newvalue = emitter.convertIntToLong(lit);
                break;
            case S_REG:
                Register reg = ( (SymbolicRegister) svalue).getRegister();
                newvalue = emitter.convertIntToLong(reg);
                break;
            case S_LOCAL:
                Local loc = ( (SymbolicLocal) svalue).getLocal();
                newvalue = emitter.convertIntToLong(addressOf(loc));
                break;
            default:
                throw new RuntimeException(
                    "Convert int to long: stack value not supported");
        }
        return new SymbolicRegister64(newvalue, LONG);
    }

    public SymbolicValueDescriptor convertUintTo(SymbolicValueDescriptor svalue,
                                                 Type to) {
        if (to == ULONG) {
            return convertUintToUlong(svalue);
        }
        /* if [FLOATS] */
        else if (to == FLOAT || to == DOUBLE) {
            throw new RuntimeException("Float/double not supported yet");
        }
        /* end [FLOATS] */
        else {
            throw new RuntimeException("Illegal conversion from UINT to " + to);
        }
    }

    private SymbolicValueDescriptor convertUintToUlong(SymbolicValueDescriptor svalue) {
        Register newvalue;
        switch (svalue.getSymbolicValueDescriptor()) {
            case S_LIT:
                int lit = ( (SymbolicLiteral32) svalue).getLiteral();
                newvalue = emitter.convertUintToUlong(lit);
                break;
            case S_REG:
                Register reg = ( (SymbolicRegister) svalue).getRegister();
                newvalue = emitter.convertUintToUlong(reg);
                break;
            case S_LOCAL:
                Local loc = ( (SymbolicLocal) svalue).getLocal();
                newvalue = emitter.convertUintToUlong(addressOf(loc));
                break;
            default:
                throw new RuntimeException(
                    "Convert uint to long: stack value not supported");
        }
        return new SymbolicRegister64(newvalue, ULONG);
    }

    public SymbolicValueDescriptor convertLongTo(SymbolicValueDescriptor svalue,
                                                 Type to) {
        if (to == INT) {
            return convertLongToInt(svalue);
        }
        /* if [FLOATS] */
        else if (to == FLOAT || to == DOUBLE) {
            throw new RuntimeException("Float/double not supported yet");
        }
        /* end [FLOATS] */
        else {
            throw new RuntimeException("Illegal conversion from LONG to " + to);
        }
    }

    private SymbolicValueDescriptor convertLongToInt(SymbolicValueDescriptor svalue) {
        Register newvalue;
        switch (svalue.getSymbolicValueDescriptor()) {
            case S_LIT:
                long lit = ( (SymbolicLiteral64) svalue).getLiteral();
                newvalue = emitter.convertLongToInt(lit);
                break;
            case S_REG:
                Register reg = ( (SymbolicRegister) svalue).getRegister();
                newvalue = emitter.convertLongToInt(reg);
                break;
            case S_LOCAL:
                Local loc = ( (SymbolicLocal) svalue).getLocal();
                newvalue = emitter.convertLongToInt(addressOf(loc));
                break;
            default:
                throw new RuntimeException(
                    "Convert long to int: stack value not supported");
        }
        return new SymbolicRegister32(newvalue, INT);
    }

    public SymbolicValueDescriptor convertUlongTo(SymbolicValueDescriptor svalue,
                                                 Type to) {
        if (to == UINT) {
            return convertUlongToUint(svalue);
        }
        /* if [FLOATS] */
        else if (to == FLOAT || to == DOUBLE) {
            throw new RuntimeException("Float/double not supported yet");
        }
        /* end [FLOATS] */
        else {
            throw new RuntimeException("Illegal conversion from ULONG to " + to);
        }
    }

    public SymbolicValueDescriptor convertUlongToUint(SymbolicValueDescriptor svalue) {
        SymbolicValueDescriptor res = convertLongToInt(svalue);
        ((SymbolicRegister32)res).setType(UINT);
        return res;
    }

    /*-----------------------------------------------------------------------*\
     *                   Arithmetic and logical instructions                 *
    \*-----------------------------------------------------------------------*/

    /**
     * Binary operation between a local and a symbolic value descriptor.
     * The result of the binary operation is placed in a register (normally).
     * The type of the result is the type of the first operand, except for
     * OOP, where the type becomes REF.
     *
     * @param op1
     * @param op2
     * @param opcode
     * @param shadowStk
     * @return the result of the binary operation
     */
    private SymbolicValueDescriptor binOpLocal(SymbolicLocal op1, SymbolicValueDescriptor op2,
                           int opcode, ShadowStack shadowStk) {
        Register result = NO_REG;
        Type resType = resultType(op1.getType(), op2.getType(), opcode);
        int opsize = resType.getStructureSize();
        Local oper1 = op1.getLocal();
        Address op1Addr = addressOf(oper1);
        switch (op2.getSymbolicValueDescriptor()) {
            case (S_LIT):
                SymbolicLiteral slit = (SymbolicLiteral) op2;
                if (slit.getLiteralSize() == 32) {
                    int op2Lit = ((SymbolicLiteral32)slit).getLiteral();
                    result = emitter.arithLogical(op1Addr, op2Lit, opsize, opcode, shadowStk);
                } else { // 64
                    long op2Lit = ((SymbolicLiteral64)slit).getLiteral();
                    result = emitter.arithLogical(op1Addr, op2Lit, opsize, opcode, shadowStk);
                }
                break;
            case (S_REG):
                SymbolicRegister op2Reg = ( (SymbolicRegister) op2);
                result = emitter.arithLogical(op1Addr, op2Reg.getRegister(), opsize, opcode, shadowStk);
                break;
            case (S_LOCAL):
                Local oper2 = ( ( (SymbolicLocal) op2).getLocal());
                result = emitter.arithLogical(op1Addr, addressOf(oper2), opsize, opcode, shadowStk);
                break;
            default:
                throw new RuntimeException("ArithLocal: symbolic value descriptor not supported");
        }
        if (resType.getStructureSize() == 8)
            return new SymbolicRegister64(result, resType);
        else
            return new SymbolicRegister32(result, resType);
    }

    private SymbolicValueDescriptor binOpLiteral(SymbolicLiteral op1, SymbolicValueDescriptor op2,
                         int opcode, ShadowStack shadowStk) {
        if (op1.getLiteralSize() == 32)
            return binOpLiteral32((SymbolicLiteral32)op1, op2, opcode, shadowStk);
        else // 64
            return binOpLiteral64((SymbolicLiteral64)op1, op2, opcode, shadowStk);
    }

    private SymbolicValueDescriptor binOpFixupSymbol(SymbolicFixupSymbol op1,
        SymbolicValueDescriptor op2, int opcode, ShadowStack shadowStk) {
        int relocationCode = relocationAbsolute(getCodeOffset(), S_LIT);
        SymbolicValueDescriptor res = binOpLiteral(new SymbolicLiteral32(0, INT),
            op2, opcode, shadowStk);
        fixupInfo.put(new Integer(relocationCode), op1.getSymbol());
        return res;
    }

    /**
     * Binary operation between a 32-bit literal and a symbolic value descriptor.
     *
     * The type of the result is that of the symbolic value descriptor, except
     * when an OOP is present, in which case the result is of type REF.  I.e.,
     *     INT binop INT = INT
     *     INT binop REF = REF
     *     INT binop OOP = REF
     *     INT binop UINT (not possible, checked earlier)
     *     INT binop LONG (not possible, checked earlier)
     *
     * @param op1
     * @param op2
     * @param opcode
     * @param shadowStk
     * @return the result of the binary operation
     */
    private SymbolicValueDescriptor binOpLiteral32(SymbolicLiteral32 op1,
                           SymbolicValueDescriptor op2, int opcode, ShadowStack shadowStk) {
        Register result = NO_REG;
        Type resType = resultType(op1.getType(), op2.getType(), opcode);
        int oper1i = op1.getLiteral();
        switch (op2.getSymbolicValueDescriptor()) {
            case (S_LIT):
                SymbolicLiteral op2slit = (SymbolicLiteral)op2;
                result = emitter.arithLogical(oper1i, ((SymbolicLiteral32)op2slit).getLiteral(),
                                       opcode, shadowStk);
                break;
            case (S_REG):
                Register reg = ( (SymbolicRegister) op2).getRegister();
                result = emitter.arithLogical(oper1i, reg, opcode, shadowStk);
                break;
            case (S_LOCAL):
                Local oper2 = ( ( (SymbolicLocal) op2).getLocal());
                Address op2addr = addressOf(oper2);
                result = emitter.arithLogical(oper1i, op2addr, 4, opcode, shadowStk);
                break;
            case (S_LABEL):
                Register labReg = ralloc.nextAvailableRegister();
                Label lab = ( (SymbolicLabel) op2).getLabel();
                emitter.load(lab, labReg);
                ralloc.useReg(labReg);
                result = emitter.arithLogical(oper1i, labReg, opcode, shadowStk);
                //resType = Type.REF; // **** type for a label? ***
                resType = REF;
                break;
            default:
                throw new RuntimeException("binOpLiteral32: symbolic value descriptor not supported");
        }
        return new SymbolicRegister32(result, resType);
    }

    /**
     * Binary operation between a 64-bit literal and a symbolic value descriptor.
     *
     * The type of the result is that of the symbolic value descriptor, except
     * when an OOP is present, in which case the result is of type REF.  I.e.,
     *     LONG binop LONG = LONG
     *     LONG binop REF = REF
     *     LONG binop OOP = REF
     *     LONG binop INT (not possible, checked earlier)
     *     LONG binop UINT (not possible, checked earlier)
     *
     * @param op1
     * @param op2
     * @param opcode
     * @param shadowStk
     * @return the result of the binary operation
     */
    private SymbolicValueDescriptor binOpLiteral64(SymbolicLiteral64 op1,
        SymbolicValueDescriptor op2, int opcode, ShadowStack shadowStk) {
        Register result = NO_REG;
        Type resType = resultType(op1.getType(), op2.getType(), opcode);
        long oper1i = op1.getLiteral();
        switch (op2.getSymbolicValueDescriptor()) {
            case (S_LIT):
                SymbolicLiteral op2slit = (SymbolicLiteral) op2;
                result = emitter.arithLogical(oper1i, ((SymbolicLiteral64)op2slit).getLiteral(),
                                              opcode, shadowStk);
                break;
            case (S_REG):
                Register reg = ((SymbolicRegister) op2).getRegister();
                result = emitter.arithLogical(oper1i, reg, opcode, shadowStk);
                break;
            case (S_LOCAL):
                Local oper2 = (((SymbolicLocal) op2).getLocal());
                Address op2addr = addressOf(oper2);
                result = emitter.arithLogical(oper1i, op2addr, 8, opcode, shadowStk);
                break;
            default:
                throw new RuntimeException(
                    "binOpLiteral64: symbolic value descriptor not supported");
        }
        if (resType.getStructureSize() == 8)
            return new SymbolicRegister64(result, resType);
        else
            return new SymbolicRegister32(result, resType);
    }

    private SymbolicValueDescriptor binOpRegister(SymbolicRegister op1,
                                                  SymbolicValueDescriptor op2, int opcode,
                                                  ShadowStack shadowStk) {
        Register result = NO_REG;
        Type resType = resultType(op1.getType(), op2.getType(), opcode);
        Register op1reg = op1.getRegister();
        switch (op2.getSymbolicValueDescriptor()) {
            case (S_REG):
                Register op2reg = ( (SymbolicRegister) op2).getRegister();
                result = emitter.arithLogical(op1reg, op2reg, opcode, shadowStk);
                break;
            case (S_LOCAL):
                Local op2loc = ( ( (SymbolicLocal) op2).getLocal());
                result = emitter.arithLogical(op1reg, addressOf(op2loc), opcode, shadowStk);
                break;
            case (S_LIT):
                SymbolicLiteral op2slit = (SymbolicLiteral) op2;
                if (op2slit.getLiteralSize() == 32) {
                    int op2lit = ((SymbolicLiteral32)op2slit).getLiteral();
                    result = emitter.arithLogical(op1reg, op2lit, opcode, shadowStk);
                } else {
                    long op2lit = ( (SymbolicLiteral64) op2slit).getLiteral();
                    result = emitter.arithLogical(op1reg, op2lit, opcode, shadowStk);
                }
                break;
            case (S_OBJECT):
                Object op2obj = ( (SymbolicObject) op2).getObject();
                //if (op2obj instanceof byte[]) {
                Label objlab = emitter.getLabelFor(op2obj);
                Register op2objReg = ralloc.nextAvailableRegister();
                emitter.load(objlab, op2objReg);
                ralloc.useReg(op2objReg);
                result = emitter.arithLogical(op1reg, op2objReg, opcode, shadowStk);
                /*} else if (op2obj instanceof Label[]) {
                    System.out.println("instanceof label");
                    throw new Error();
                         } else {
                    throw new Error();
                         }*/
                resType = Type.REF;
                break;
            case (S_LABEL):
                Register labReg = ralloc.nextAvailableRegister();
                Label lab = ( (SymbolicLabel) op2).getLabel();
                emitter.load(lab, labReg);
                ralloc.useReg(labReg);
                result = emitter.arithLogical(op1reg, labReg, opcode, shadowStk);
                resType = REF;
                break;
            default:
                throw new RuntimeException("Arithmetic register: illegal symbolic value descriptor");
        }
        if (resType.getStructureSize() == 8)
            return new SymbolicRegister64(result, resType);
        else
            return new SymbolicRegister32(result, resType);
    }

    private SymbolicValueDescriptor binOpLabel(SymbolicLabel op1,
            SymbolicValueDescriptor op2, int opcode, ShadowStack shadowStk) {
        Register result = NO_REG;
        Type resType = REF;
        Label oper1lab = op1.getLabel();
        switch (op2.getSymbolicValueDescriptor()) {
            case (S_REG):
                Register labReg = ralloc.nextAvailableRegister();
                emitter.load(oper1lab, labReg);
                ralloc.useReg(labReg);
                Register oper2reg = ( (SymbolicRegister) op2).getRegister();
                result = emitter.arithLogical(labReg, oper2reg, opcode, shadowStk);
                break;
            default:
                throw new RuntimeException("ArithLabel: not implemented");
        }
        if (result.isLong())
            return new SymbolicRegister64(result, resType);
        else
            return new SymbolicRegister32(result, resType);
    }

    private SymbolicValueDescriptor binOpObject(SymbolicObject op1,
                                                SymbolicValueDescriptor op2,
                                                int opcode, ShadowStack shadowStk) {
        Register result = NO_REG;
        Type resType = Type.REF;   /****/
        Object oper1obj = op1.getObject();
        Label lab = emitter.getLabelFor(oper1obj);
        Register dstRegForObj = ralloc.nextAvailableRegister();
        switch (op2.getSymbolicValueDescriptor()) {
            case (S_REG):
                Register oper2reg = ( (SymbolicRegister) op2).getRegister();
                emitter.load(lab, dstRegForObj);
                result = emitter.arithLogical(dstRegForObj, oper2reg, opcode, shadowStk);
                break;
            default:
                throw new RuntimeException("ArithObject: not implemented");
        }
        if (result.isLong())
            return new SymbolicRegister64(result, resType);
        else
            return new SymbolicRegister32(result, resType);
    }

    public SymbolicValueDescriptor binOp(SymbolicValueDescriptor op1,
                                         SymbolicValueDescriptor op2,
                                         int opcode,
                                         ShadowStack shadowStk) {
        switch (op1.getSymbolicValueDescriptor()) {
            case (S_REG):
                return binOpRegister((SymbolicRegister)op1, op2, opcode, shadowStk);
            case (S_LIT):
                return binOpLiteral((SymbolicLiteral)op1, op2, opcode, shadowStk);
            case (S_LOCAL):
                return binOpLocal((SymbolicLocal)op1, op2, opcode, shadowStk);
            case (S_OBJECT):  // *** not tested by existing examples
                return binOpObject((SymbolicObject)op1, op2, opcode, shadowStk);
            case S_FIXUP_SYM:
                return binOpFixupSymbol((SymbolicFixupSymbol)op1, op2, opcode, shadowStk);
            case (S_LABEL):   // *** treat as literal?
                return binOpLabel((SymbolicLabel)op1, op2, opcode, shadowStk);
            default:
                throw new RuntimeException("binOp: illegal symbolic value descriptor");
        }
    }

    public SymbolicValueDescriptor unaryOp(SymbolicValueDescriptor op, int opcode) {
        Register res = NO_REG;
        Type resType = op.getType();
        switch(op.getSymbolicValueDescriptor()) {
            case S_LOCAL:
                Local loc = ((SymbolicLocal)op).getLocal();
                res = emitter.unary(addressOf(loc), resType, opcode);
                break;
            case S_REG:
                res = emitter.unary(((SymbolicRegister)op).getRegister(), opcode);
                break;
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral)op;
                if (slit.getLiteralSize() == 32) {
                    res = emitter.unary(((SymbolicLiteral32)slit).getLiteral(), opcode);
                } else { // 64
                    res = emitter.unary(((SymbolicLiteral64)slit).getLiteral(), opcode);
                }
                break;
            default:
                throw new RuntimeException("unaryOp: illegal symbolic value descriptor");
        }
        if (res.isLong())
            return new SymbolicRegister64(res, resType);
        else
            return new SymbolicRegister32(res, resType);
    }

    /*-----------------------------------------------------------------------*\
     *                       Comparison instructions                         *
    \*-----------------------------------------------------------------------*/

    public SymbolicValueDescriptor compare(SymbolicValueDescriptor op1,
                                           SymbolicValueDescriptor op2,
                                           int opcode) {
        switch (op1.getSymbolicValueDescriptor()) {
            case (S_REG):
                return compareRegister( (SymbolicRegister) op1, op2, opcode, op1.getType().isSigned());
            case (S_LIT):
                return compareLiteral( (SymbolicLiteral) op1, op2, opcode, op1.getType().isSigned());
            case (S_LOCAL):
                return compareLocal( (SymbolicLocal) op1, op2, opcode, op1.getType().isSigned());
            default:
                throw new RuntimeException("Compare: illegal symbolic value descriptor");
        }
    }

    private SymbolicValueDescriptor compareRegister(SymbolicRegister op1,
            SymbolicValueDescriptor op2, int opcode, boolean signed) {
        Register result = NO_REG;
        Register op1reg = op1.getRegister();
        switch (op2.getSymbolicValueDescriptor()) {
            case S_REG:
                Register op2reg = ( (SymbolicRegister) op2).getRegister();
                result = emitter.compare(op1reg, op2reg, opcode, signed);
                break;
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral) op2;
                if (slit.getLiteralSize() == 64) {
                    long lit = ((SymbolicLiteral64)slit).getLiteral();
                    result = emitter.compare64(op1reg, lit, opcode, signed);
                } else { // 32
                    int lit = ( (SymbolicLiteral32) slit).getLiteral();
                    result = emitter.compare(op1reg, lit, opcode, signed);
                }
                break;
            case S_LOCAL:
                Address locAddr = addressOf(((SymbolicLocal)op2).getLocal());
                result = emitter.compare(op1reg, locAddr, opcode, signed);
                break;
            default:
                throw new RuntimeException("Compare: illegal symbolic value descriptor");
        }
        return new SymbolicRegister32(result, op1.getType());
    }

    private SymbolicValueDescriptor compareLiteral(SymbolicLiteral op1,
            SymbolicValueDescriptor op2, int opcode, boolean signed) {
        if (op1.getLiteralSize() == 32)
            return compareLiteral32((SymbolicLiteral32)op1, op2, opcode, signed);
        return compareLiteral64((SymbolicLiteral64)op1, op2, opcode, signed);
    }

    private SymbolicValueDescriptor compareLiteral32(SymbolicLiteral32 op1,
            SymbolicValueDescriptor op2, int opcode, boolean signed) {
        Register result = NO_REG;
        int op1lit = op1.getLiteral();
        switch (op2.getSymbolicValueDescriptor()) {
            case S_REG:
                Register op2reg = ( (SymbolicRegister) op2).getRegister();
                result = emitter.compare(op1lit, op2reg, opcode, signed);
                break;
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral) op2;
                int lit = ((SymbolicLiteral32)slit).getLiteral();
                result = emitter.compare(op1lit, lit, opcode, signed);
                break;
            case S_LOCAL:
                Address locAddr = addressOf( ((SymbolicLocal)op2).getLocal());
                result = emitter.compare(op1lit, locAddr, opcode, signed);
                break;
            default:
                throw new RuntimeException("Compare: illegal symbolic value descriptor");
        }
        return new SymbolicRegister32(result, op1.getType());
    }

    private SymbolicValueDescriptor compareLiteral64(SymbolicLiteral64 op1,
            SymbolicValueDescriptor op2, int opcode, boolean signed) {
        Register result = NO_REG;
        long op1lit = op1.getLiteral();
        switch (op2.getSymbolicValueDescriptor()) {
            case S_REG:
                Register op2reg = ( (SymbolicRegister) op2).getRegister();
                result = emitter.compare64(op1lit, op2reg, opcode, signed);
                break;
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral) op2;
                long lit = ( (SymbolicLiteral64) slit).getLiteral();
                result = emitter.compare64(op1lit, lit, opcode, signed);
                break;
            case S_LOCAL:
                Address locAddr = addressOf( ( (SymbolicLocal) op2).getLocal());
                result = emitter.compare64(op1lit, locAddr, opcode, signed);
                break;
            default:
                throw new RuntimeException("Compare: illegal symbolic value descriptor");
        }
        return new SymbolicRegister32(result, op1.getType());
    }

    private SymbolicValueDescriptor compareLocal(SymbolicLocal op1,
            SymbolicValueDescriptor op2, int opcode, boolean signed) {
        Register result = NO_REG;
        Address op1addr = addressOf( ((SymbolicLocal)op1).getLocal());
        switch (op2.getSymbolicValueDescriptor()) {
            case S_REG:
                Register op2reg = ( (SymbolicRegister) op2).getRegister();
                result = emitter.compare(op1addr, op2reg, opcode, signed);
                break;
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral) op2;
                if (slit.getLiteralSize() == 32) {
                    int lit = ((SymbolicLiteral32)slit).getLiteral();
                    result = emitter.compare(op1addr, lit, opcode, signed);
                } else { // 64
                    long lit = ((SymbolicLiteral64)slit).getLiteral();
                    result = emitter.compare64(op1addr, lit, opcode, signed);
                }
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)op2).getLocal();
                result = emitter.compare(op1addr, addressOf(loc), opcode, op1.getType(), signed);
                break;
            default:
                throw new RuntimeException("Compare: illegal symbolic value descriptor");
        }
        return new SymbolicRegister32(result, op1.getType());
    }

    /*-----------------------------------------------------------------------*\
     *                         Branch instructions                           *
    \*-----------------------------------------------------------------------*/

    public void branch(Label label, SymbolicValueDescriptor res, boolean cond) {
        switch (res.getSymbolicValueDescriptor()) {
            case S_REG:
                emitter.branch(label, ( (SymbolicRegister) res).getRegister(), cond);
                break;
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral)res;
                emitter.branch(label, ((SymbolicLiteral32)slit).getLiteral(), cond);
                break;
            case S_LOCAL:
                emitter.branch(label, addressOf(((SymbolicLocal)res).getLocal()), cond);
                break;
            default:
                throw new RuntimeException("Branch: illegal symbolic value descriptor");
        }
    }

    public void branch(int immed, SymbolicValueDescriptor res, boolean cond) {
        switch (res.getSymbolicValueDescriptor()) {
            case S_REG:
                emitter.branch(immed, ( (SymbolicRegister) res).getRegister(), cond);
                break;
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral)res;
                emitter.branch(immed, ((SymbolicLiteral32)slit).getLiteral(), cond);
                break;
            case S_LOCAL:
                emitter.branch(immed, addressOf(((SymbolicLocal)res).getLocal()), cond);
                break;
            default:
                throw new RuntimeException("Branch: illegal symbolic value descriptor");
        }
    }

    public void branch(Label label) {
        emitter.branch(label);
    }

    public void branch(int dst) {
        emitter.branch(dst);
    }

    public void jump32(SymbolicValueDescriptor dst) {
        switch (dst.getSymbolicValueDescriptor()) {
            case S_REG:
                Register reg = ((SymbolicRegister)dst).getRegister();
                emitter.branch(reg);
                break;
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral)dst;
                branch(((SymbolicLiteral32)slit).getLiteral());
                break;
            case S_LOCAL:
                Local loc = ((SymbolicLocal)dst).getLocal();
                emitter.branch(addressOf(loc));
                break;
            case S_LABEL:
                branch(((SymbolicLabel)dst).getLabel());
                break;
            default:
                throw new RuntimeException("Jump: illegal symbolic value descriptor");
        }
    }

    /*-----------------------------------------------------------------------*\
     *                    Function call and return instructions              *
    \*-----------------------------------------------------------------------*/


    /**
     * pre-process parameters: move actual arguments out of the shadow stack
     * onto a parameter stack, and then check the remainder of the shadow stack
     * for registers that need to be preserved by the caller (through spilling
     * onto the runtime stack)
     */
    private Stack preprocessParams(int nparms, ShadowStack shadowStk) {
        Stack paramStack = new Stack();
        while (nparms-- > 0) {
            SymbolicValueDescriptor param = (SymbolicValueDescriptor) shadowStk.pop();
            paramStack.push(param);
        }
        return paramStack;
    }

    // Checks the shadow stack and spills any registers that are not preserved
    // by the ABI across a call boundary.  Tags such registers.
    private boolean spill(ShadowStack shadowStk) {
        boolean spilled = false;
        int shadowSize = shadowStk.size();
        while (shadowSize-- > 0) {
            SymbolicValueDescriptor elem = (SymbolicValueDescriptor) shadowStk.
                elementAt(shadowSize);
            if (elem.getSymbolicValueDescriptor() == S_REG) {
                SymbolicRegister symbElem = (SymbolicRegister) elem;
                Register shadowReg = symbElem.getRegister();
                if (!ralloc.ABIPreservedRegister(shadowReg)) {
                    // spill register and tag it as spilled
                    emitter.push(shadowReg);
                    symbElem.setSpilled();
                    shadowStk.setElementAt(symbElem, shadowSize);
                    spilled = true;
                }
            }
        }
        return spilled;
    }

    // Unspills any registers that were spilled by spill().
    private void unspill(ShadowStack shadowStk) {
        int shadowSize = shadowStk.size();
        while (shadowSize-- > 0) {
            SymbolicValueDescriptor elem = (SymbolicValueDescriptor)
                shadowStk.elementAt(shadowSize);
            if (elem.getSymbolicValueDescriptor() == S_REG) {
                SymbolicRegister symReg = (SymbolicRegister) elem;
                if (symReg.isSpilled()) {
                    Register spilledReg = symReg.getRegister();
                    emitter.pop(spilledReg);
                    symReg.resetSpilled();
                    shadowStk.setElementAt(symReg, shadowSize);
                }
            }
        }
    }

    // emits parameters onto the runtime stack and returns the number of bytes
    // consumed in all the push instructions
    private int emitParams(Stack paramStack) {
        int actualParamBytes = 0;
        while (!paramStack.empty()) {
            SymbolicValueDescriptor param = (SymbolicValueDescriptor)paramStack.pop();
            switch (param.getSymbolicValueDescriptor()) {
                case S_LIT:
                    SymbolicLiteral slit = (SymbolicLiteral)param;
                    if (slit.getLiteralSize() == 32)
                        emitter.push( ((SymbolicLiteral32) param).getLiteral());
                    else // 64
                        emitter.push( ((SymbolicLiteral64) param).getLiteral());
                    break;
                case S_REG:
                    Register reg = ( (SymbolicRegister) param).getRegister();
                    emitter.push(reg);
                    break;
                case S_LOCAL:
                    Local loc = ( (SymbolicLocal) param).getLocal();
                    emitter.push(addressOf(loc));
                    break;
                case S_OBJECT:
                    emitter.pushObject( ( (SymbolicObject) param).getObject());
                    break;
                case S_FIXUP_SYM:
                    throw new RuntimeException("Emit Parameters: this should never happen");
                default:
                    throw new RuntimeException("Emit Parameters: illegal symbolic value descriptor");
            }
            if (param.getType() == Type.LONG)
                actualParamBytes += 8;
            else
                actualParamBytes += 4;
        }
        return actualParamBytes;
    }

    // emits the code to do the call of the given address
    // NB: this method needs updating for 64-bit compilation.
    private void doCall(SymbolicValueDescriptor address, int convention) {
        switch (address.getSymbolicValueDescriptor()) {
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral) address;
                if (slit.getLiteralSize() != 32)
                    throw new RuntimeException("Call: expecting 32-bit address literal");
                // ** actually, we need to check against the size of a ref and emit the right error message
                int lit = ((SymbolicLiteral32)address).getLiteral();
                if (convention == Compiler.C_JVM) {
                    emitter.loadJvmReg(lit);
                }
                emitter.call(lit);
                break;
            case S_REG:
                Register reg = ( (SymbolicRegister) address).getRegister();
                if (convention == Compiler.C_JVM) {
                    emitter.loadJvmReg(reg);
                }
                emitter.call(reg);
                break;
            case S_FIXUP_SYM:
                int relocationCode;
                if (convention == Compiler.C_NORMAL) {
                    relocationCode = relocationRelative(getCodeOffset(), S_LIT);
                    emitter.call(0);         // addr to be fixed up
                } else { // C_JVM
                    relocationCode = relocationAbsolute(getCodeOffset(), S_REG);
                    emitter.loadJvmReg(0);   // addr to be fixed up (EAX is reserved)
                    emitter.call(EAX);
                }
                fixupInfo.put(new Integer(relocationCode),
                              ( (SymbolicFixupSymbol) address).getSymbol());
                break;
            case S_LABEL:
                Label label = ( (SymbolicLabel) address).getLabel();
                if (convention == Compiler.C_JVM) {
                    emitter.loadJvmReg(label);
                }
                emitter.call(label);
                break;
            case S_LOCAL:
                XLocal local = (XLocal)( (SymbolicLocal) address).getLocal();
                emitter.call(addressOf(local));
                break;
            default:
                throw new RuntimeException("Call: illegal symbolic value descriptor");
        }
    }

    /* push symbolic values onto the shadow stack based on the return type
     *
     * it's the Instruction responsibility to place the return value in the
     * ABI-specified register(s); this method assumes that is the case and
     * places those register(s) on the shadow stack.
     *
     * ** x86-dependent; need to go through the ABI interface instead **
     *
     * NB: this is only for primary values
     */
    private void pushReturnValue(ShadowStack shadowStk, Type type) {
        if (type != Type.VOID) {
            if (type == Type.INT || type == Type.UINT || type == Type.REF ||
                type == Type.OOP) {
                shadowStk.push(new SymbolicRegister32(EAX, type));
                ralloc.useReg(EAX);
            }
            else if (type == Type.LONG) {
                shadowStk.push(new SymbolicRegister64(EDXEAX, type));
                ralloc.useReg(EDXEAX);
            }
            /* if [FLOATS] */
            else if ( (type == Type.FLOAT) || (type == Type.DOUBLE)) {
                throw new RuntimeException(
                    "Push return value: float/double not supported.");
            }
            /* end [FLOATS] */
            else {
                throw new RuntimeException(
                    "Push return value: only primary types allowed to be returned.");
            }
        }
    }

    /**
     * Call a function. The top most value must be the address of the function to be called.
     *
     * Of all the call methods, this is the one that does everything, including
     * the storing of the return value onto the shadow stack.  All other call
     * methods should point to this one.
     *
     * <p>
     * Stack: ..., VALUEN to VALUE1, ADDRESS -> ..., [RESULT]
     * <p>
     *
     * @param shadowStk the operand stack
     * @param nparms the number of parameters to pop
     * @param type the return type to be pushed onto the stack
     */
    public void call(SymbolicValueDescriptor address, ShadowStack shadowStk, int nparms,
                     Type type, int convention) {
        // pre-process parameters
        Stack paramStack = preprocessParams(nparms, shadowStk);
        boolean spilled = spill(shadowStk);

        // emit parameters
        int actualParamBytes = emitParams(paramStack);

        // do the call, adding fixup information if needed
        doCall(address, convention);

        // push return value onto the shadow stack
        pushReturnValue(shadowStk, type);

        // caller epilogue: fixup the runtime stack (** x86-dependent **)
        if (actualParamBytes > 0) {
            emitter.arithLogical(actualParamBytes, ESP, OP_ADD, shadowStk);
        }

        // pop any shadow stack registers that were spilled onto the runtime stack
        // *** MISSING: check that it doesn't pop eax onto the returned eax
        if (spilled) {
            unspill(shadowStk);
        }

        // restore runtime stack pointer to location after the alloca for dynamic calling conventions
        if (convention == Compiler.C_DYNAMIC || convention == Compiler.C_JVM_DYNAMIC) {
            emitter.load(addressOf(SSslot), ESP);
        }
    }

    /**
     * Return a register value as per the ABI conventions.
     *
     * ** x86-dependent for now.
     *
     * No checks:
     * - We don't need to check if the ABI return registers (EAX and EDX in
     *   the case of x86) are in use because it's the end of the method (only
     *   one exit is generated by the compiler), therefore, these registers
     *   can be reused.
     *
     * @param reg the register value to be returned.
     */
    private void retRegister(Register reg) {
        if (reg.isLong()) {
            Register regLo = ralloc.registerLo(reg);
            Register regHi = ralloc.registerHi(reg);
            if (regLo != EAX) {
                emitter.load(regLo, EAX);
            }
            if (regHi != EDX) {
                emitter.load(regHi, EDX);
            }
        }
        else {
            if (reg != EAX) {
                emitter.load(reg, EAX);
            }
        }
    }

    private void retLiteral(SymbolicLiteral slit) {
        if (slit.getLiteralSize() == 32)
            emitter.loadReturnValue( ( (SymbolicLiteral32) slit).getLiteral());
        else // 64
            emitter.loadReturnValue( ( (SymbolicLiteral64) slit).getLiteral());
    }

    private void retLocal(Local loc) {
        if (((XLocal) loc).getType() == Type.LONG)
            emitter.load(addressOf(loc, 4), EDX);
        emitter.load(addressOf(loc), EAX);
    }

    /**
     * Return from a method by setting up the return value (if any)
     * in the correct destination and by performing a jump to the
     * leave code for this method (as there may be more than one
     * return in the method).
     *
     * <p>
     * Stack: ..., [VALUE] -> _
     * <p>
     *
     */
    public void ret(SymbolicValueDescriptor svalue) {
        switch (svalue.getSymbolicValueDescriptor()) {
            case S_REG:
                Register reg = ((SymbolicRegister)svalue).getRegister();
                retRegister(reg);
                break;
            case S_LIT:
                SymbolicLiteral slit = (SymbolicLiteral)svalue;
                retLiteral(slit);
                break;
            case S_LOCAL:
                Local loc = ( (SymbolicLocal) svalue).getLocal();
                retLocal(loc);
                break;
            default:
                throw new RuntimeException("Return: symbolic value descriptor not supported");
        }

        // free registers used for return value
        if (svalue.getType() == LONG) {
            ralloc.freeReg(EDXEAX);
        } else {
            ralloc.freeReg(EAX);
        }

        // forward jump to the leave code
        emitter.branchToLeave();
    }

    /**
     * Emitter's code buffer access functions
     */
    public int getCodeSize() {
        return emitter.getCodeSize();
    }

    public byte[] getCode() {
        return emitter.getCode();
    }

    /* ABI/hw feature */
    public boolean javaOrder() {
        return emitter.javaOrder();
    }


    /*-----------------------------------------------------------------------*\
     *                        Private helper routines                        *
    \*-----------------------------------------------------------------------*/

// *** rm?
    private Address addressOf(Local local) {
        int offset = getOffset(local);
        return new Address(EBP, offset);
    }

// *** rm?
    private Address addressOf(Local local, int delta) {
        int offset = getOffset(local) + delta;
        return new Address(EBP, offset);
    }

    private Type typeOf(Local local) {
        XLocal xlocal = (XLocal)local;
        return xlocal.getType();
    }

// *** rm?  moved to XLocal/X86XLocal **/
    /**
     * Computes the offset of the Local (parameter or local) in the
     * activation record it belongs to.
     *
     * This function is X86-dependent; it relies on the stack frame format.
     * Parameters have positive offsets from EBP, starting at location 8.
     * Locals have negative offsets from EBP, starting at location -4 (word-aligned).
     *
     */
    private int getOffset(Local local) {
        XLocal loc = (XLocal)local;
        int offset = loc.getSlotOffset();
        offset = (loc.isParam()) ? 8 /* stack frame delta */ + offset : -offset - 4;
        // ** NB: the offset for locals on the stack has not been tested **/
        return offset;
    }

    /**
     * This method computes the resultant type of applying a binary
     * operation to the two given types, which are primitive types or pseudo
     * types themselves.  For example, IP + INT is a common operation.
     *
     * The rules for binary operations ensure that operands have the same
     * type or consistent types.  These rules have been checked prior to the
     * use of this method.  In general, a binary operation can only be applied
     * to operands of the same type, except for:
     * 1- an operand of type REF or OOP, which can be combined with an INT (for
     *    32-bit compilations) or LONG (for 64-bit compilations).  The result of
     *    a REF operation is always a REF, and the result of an OOP operation is
     *    also a REF.
     * 2- an operand of type IP, LP, MP or SS, which can be combined with an INT
     *    (for 32-bit compilations) or LONG (for 64-bit compilations).  The result
     *    of the operation is always REF.
     *
     * This method ensures that the following rules apply:
     *     OOP binop <any type> = REF     <any type> binop OOP = REF
     *     REF binop <any type> = REF     <any type> binop REF = REF
     *     IP binop <any type> = REF      <any type> binop IP = REF
     *     LP binop <any type> = REF      <any type> binop IP = REF
     *     SS binop <any type> = REF      <any type> binop IP = REF
     *     MP binop <any type> = REF      <any type> binop MP = REF
     *
     * NB: This compiler has not been checked for 64-bit compilations.  Some
     *     extra type tests may be required in the below code.
     *
     * @param op1type
     * @param op2type
     * @return the result type
     */
    private Type resultType(Type op1type, Type op2type, int opcode) {
        Type resType = op1type == OOP || op1type == MP ? REF : op1type;
        if (op2type == REF || op2type == OOP || op2type == IP || op2type == LP || op2type == SS)
            resType = REF;

        if (resType == LONG && isLongComparison(opcode))
            resType = INT;

        return resType;
    }

    private boolean isLongComparison(int opcode) {
        int instrCode = opcode & 0xFFFFFF00;
        switch (instrCode) {
            case OP_LT:
            case OP_LE:
            case OP_EQ:
            case OP_NE:
            case OP_GE:
            case OP_GT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Calculates the relocation code for an absolute relocation, given the
     * starting address of the instruction to be relocated and the type of
     * operand used in the instruction.
     *
     * @param codeOffset the offset in the CodeBuffer for the instruction to be relocated
     * @param shadowStkConstant the type of operand used in the relocation
     * @return the absolute relocation code
     */
    private int relocationAbsolute(int codeOffset, int shadowStkConstant) {
        return Assembler.RELOC_ABSOLUTE_INT << 24 | codeOffset + relocation(shadowStkConstant);
    }

    private int relocationRelative(int codeOffset, int shadowStkConstant) {
        return Assembler.RELOC_RELATIVE_INT << 24 | codeOffset + relocation(shadowStkConstant);
    }

    // Returns the instruction offset, in bytes, where the address that needs
    // to be fixed up is located at.
    private int relocation(int shadowStkConstant) {
        int addrOffset = 1;       // 1 byte for the "mov" opcode
        switch(shadowStkConstant) {
            case S_LIT:           // do nothing
            case S_REG:
                break;
            case S_LOCAL:
                addrOffset += 2;  // 2 bytes for the [ebp-off]
                break;
            default:
                throw new RuntimeException("relocation: shadow stack constant not supported");
        }
        return addrOffset;
    }

}
