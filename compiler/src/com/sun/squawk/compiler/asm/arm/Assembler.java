/*
 * @(#)Assembler.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.arm;

import com.sun.squawk.compiler.asm.*;

/**
 * This class contains methods for generating ARM code into a code buffer. For each
 * possibly used machine instruction there exists a method that combines the
 * operation code and the specified parameters into one or more bytes and
 * appends them to the end of the code buffer.
 *
 * @see "ARM Architecture Reference Manual, Second Edition"
 * @author   David Liu
 * @version  1.00
 */
public class Assembler extends AbstractAssembler implements Constants {

    /**
     * Relocation type for absolute int addresses.
     */
    public static final int RELOC_ABSOLUTE_INT = 0;

    /**
     * Relocation type for relative int addresses.
     */
    public static final int RELOC_RELATIVE_INT = 1;

    /**
     * List of relocation records.
     */
    private Relocator relocs;

    /**
     * Count of unbound labels.
     */
    private int unboundLabelCount;

    /**
     * Constructs a new assembler generating code into the specified buffer.
     *
     * @param  code  code buffer that stores the instructions
     */
    public Assembler(CodeBuffer code) {
        super(code);
    }

    /**
     * Aligns the next instruction to the specified boundary.
     *
     * @param  modulus  modulus of alignment
     */
    public void align(int modulus) {
        while (getOffset() % modulus != 0) {
            nop();
        }
    }

    /**
     * Allocate a new label.
     *
     * @return  a new label
     */
    public ALabel newLabel() {
        return new ALabel(this);
    }

    /**
     * Adjust the number of unbound labels.
     *
     * @param  x  the amount to adjust by
     */
    public void unboundLabelCount(int x) {
        unboundLabelCount += x;
    }

    /**
     * Binds the specified label to the current code position.
     *
     * @param  label  label to be bound
     */
    public void bind(ALabel label) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!label.isBound(), "label can be bound once only");
        }
        label.bindTo(getOffset());
    }

    /**
     * Emits the specified unsigned byte value into the code buffer.
     *
     * @param  x  byte to be emitted
     */
    public void emitByte(int x) {
        code.emit(x);
    }

    /**
     * Emits the specified 16-bit integer value into the code buffer.
     *
     * @param  x  16-bit value to be emitted
     */
    public void emitShort(int x) {
        emitByte(x >>> 8);
        emitByte(x & 0xff);
    }

    /**
     * Emits the specified 32-bit integer value into the code buffer.
     *
     * @param  x  32-bit value to be emitted
     */
    public void emitInt(int x) {
        emitShort(x >>> 16);
        emitShort(x & 0xffff);
    }

    /**
     * Emits the specified 24-bit integer value into the code buffer.
     *
     * @param x 24-bit value to be emitted
     */
    public void emitInt24(int x) {
        emitByte((x >>> 16) & 0xff);
        emitByte((x >>> 8) & 0xff);
        emitByte(x & 0xff);
    }

    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @param  label   the label containing the displacement
     */
    public void emitLabel(ALabel label) {
        relocs = new Relocator(code.getCodePos(), relocs);
        label.addRelocator(relocs);
        emitInt(0xDEADBEEF);
    }

    /**
     * Emits the specified data into the code buffer and generates relocation
     * information.
     *
     * @param  label   the label containing the displacement
     * @param  disp    the displacement when the label is null
     */
    protected void emitData(ALabel label, int disp) {
        if (label == null) {
            emitInt(disp);
        } else {
            emitLabel(label);
        }
    }

    /**
     * Produces and returns a hex dump (with addresses) of the code.
     *
     * @return hex dump of the code
     */
    public String getHexDump() {
        StringBuffer s = new StringBuffer("Code buffer has " + code.getCodeSize() + " bytes:\n\n");

        for (int i = 0; i < code.getCodeSize(); i += 4) {
            String addr = Integer.toHexString (i + code.getRelocation());
            s.append("0000".substring(addr.length()) + addr + ":    ");

            for (int j = i; (j < i + 4) &&  (j < code.getCodeSize ()); j++) {
                String hex = Integer.toHexString((int) code.getBytes() [j] & 0xff);
                s.append((hex.length() < 2 ? "0" : "") + hex + " ");
            }

            s.append("\n");
        }

        return s.toString();
    }

    /**
     * Relocate the code buffer to a specific address.
     *
     * @param  address   the code buffer address.
     * @return           an array if ints that contain the relocation information
     */
    public int[] relocate(int address) {
        Assert.that(unboundLabelCount == 0, "Unbound label count = "+unboundLabelCount);
        int save = code.getCodePos();

        /*
         * Count the number of relocators and allocate an array large
         * enough to hold the rellocation info
         */
        int count = 0;
        Relocator r = relocs;
        while (r != null) {
            count++;
            r = r.getNext();
        }
        int[] relinfo = new int[count];

        /*
         * Iterate throught the relocators resolveing their addresses and
         * recording the relocation information.
         */
        while (relocs != null) {
            relinfo[--count] = relocs.emit(this, address);
            relocs = relocs.getNext();
        }

        code.setCodePos(save);
        code.setRelocation(address);
        return relinfo;
    }

    /**
     * Emits a branch instruction into the code buffer.
     *
     * @param cond condition flag under which the branch will occur
     * @param l specifies whether the link address should be saved (1) or not (0)
     * @param label destination operand label
     */
    private void emitBranch(int cond, int l, ALabel label) {
        Assert.that(l == 0 || l == 1, "invalid l value");

        final int base = getOffset() + 8;

        emitByte((cond << 4) | 0xa | l);
        int offset24 = 0xcafebb;

        if (label.isBound()) {
            final int offset = label.getPos() - base;
            Assert.that(offset >= -33554432 && offset <= 33554428, "branch offset too large");
            offset24 = (offset >>> 2) & 0xffffff;
        } else {
            label.addBranch(cond, l, base - 8);
        }

        emitByte(offset24 >>> 16);
        emitByte(offset24 >>> 8);
        emitByte(offset24);
    }

    /**
     * Emits an arithmetic or logic instruction into the code buffer.
     *
     * @param cond condition flag under which this instruction will be executed
     * @param opcode opcode of the instruction
     * @param updateFlags determines if the status flags are to be updated by this instruction
     * @param dst destination operand register
     * @param src first source operand register
     * @param op2 flexible second operand
     */
    private void emitDataProc(int cond, int opcode, boolean updateFlags,
                              Register dst, Register src, Operand2 op2) {
        switch (op2.getType())
        {
            case OPER2_IMM: emitDataProcImm(cond, opcode, updateFlags, dst, src, op2); break;
            case OPER2_REG: emitDataProcReg(cond, opcode, updateFlags, dst, src, op2); break;
            case OPER2_LSL_IMM: emitDataProcSftImm(cond, opcode, 0x0, updateFlags, dst, src, op2); break;
            case OPER2_LSL_REG: emitDataProcSftReg(cond, opcode, 0x1, updateFlags, dst, src, op2); break;
            case OPER2_LSR_IMM: emitDataProcSftImm(cond, opcode, 0x2, updateFlags, dst, src, op2); break;
            case OPER2_LSR_REG: emitDataProcSftReg(cond, opcode, 0x3, updateFlags, dst, src, op2); break;
            case OPER2_ASR_IMM: emitDataProcSftImm(cond, opcode, 0x4, updateFlags, dst, src, op2); break;
            case OPER2_ASR_REG: emitDataProcSftReg(cond, opcode, 0x5, updateFlags, dst, src, op2); break;
            case OPER2_ROR_IMM: emitDataProcSftImm(cond, opcode, 0x6, updateFlags, dst, src, op2); break;
            case OPER2_ROR_REG: emitDataProcSftReg(cond, opcode, 0x7, updateFlags, dst, src, op2); break;
            case OPER2_RRX: emitDataProcRRX(cond, opcode, updateFlags, dst, src, op2); break;
            default: Assert.that(false, "should not reach here");
        }
    }

    /**
     * Emits a data processing instruction with a constant operand.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src first operand register
     * @param op2 second operand
     */
    private void emitDataProcImm(int cond, int opcode, boolean updateFlags, Register dst,
                                 Register src, Operand2 op2) {
        emitByte((cond << 4) | 0x2 | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte(((dst.getNumber() & 0xf) << 4) | (op2.getPackedImmRot() & 0xf));
        emitByte(op2.getPackedImm8() & 0xff);
    }

    /**
     * Emits a data processing instruction with a register operand.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src first operand register
     * @param op2 the second operand
     */
    private void emitDataProcReg(int cond, int opcode, boolean updateFlags, Register dst,
                                 Register src, Operand2 op2) {
        emitByte((cond << 4) | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte((dst.getNumber() & 0xf) << 4);
        emitByte(op2.getReg().getNumber() & 0xf);
    }

    /**
     * Emits a data processing instruction with a register operand that is shifted by an immediate
     * value. The shift operation performed is specified by the oper parameter.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param oper shift operation to perform
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 the second operand
     */
    private void emitDataProcSftImm(int cond, int opcode, int oper, boolean updateFlags,
                                    Register dst, Register src, Operand2 op2) {
        Assert.that(oper == 0x0 || oper == 0x2 || oper == 0x4 || oper == 0x6, "invalid shift operation");
        emitByte((cond << 4) | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte(((dst.getNumber() & 0xf) << 4) | (op2.getImm() >>> 1));
        emitByte(((op2.getImm() << 7) & 0x80) | ((oper & 0x7) << 4) | (op2.getReg().getNumber() & 0xf));
    }

    /**
     * Emits a data processing instruction with a register operand that is shifted by a register
     * value. The shift operation performed is specified by the oper parameter.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param oper shift operation to perform
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src first operand register
     * @param op2 the second operand
     */
    private void emitDataProcSftReg(int cond, int opcode, int oper, boolean updateFlags,
                                    Register dst, Register src, Operand2 op2) {
        Assert.that(oper == 0x1 || oper == 0x3 || oper == 0x5 || oper == 0x7, "invalid shift operation");
        emitByte((cond << 4) | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte(((dst.getNumber() & 0xf) << 4) | (op2.getShift().getNumber() & 0xf));
        emitByte(((oper & 0xf) << 4) | (op2.getReg().getNumber() & 0xf));
    }

    /**
     * Emits a data processing instruction with a register operand that is rotated 33-bits right
     * using the Carry Flag as the 33rd bit.
     *
     * @param cond condition under which the instruction is executed
     * @param opcode operation of the instruction
     * @param updateFlags whether the condition codes are to be updated by the instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    private void emitDataProcRRX(int cond, int opcode, boolean updateFlags, Register dst,
                                    Register src, Operand2 op2) {
        emitByte((cond << 4) | ((opcode >>> 3) & 1));
        emitByte((opcode << 5) | (updateFlags ? 0x10 : 0) | (src.getNumber () & 0xf));
        emitByte((dst.getNumber() & 0xf) << 4);
        emitByte(0x60 | (op2.getReg().getNumber() & 0xf));
    }

    /**
     * Emits a load or store instruction.
     *
     * @param cond condition under which the instruction is executed
     * @param access specifies unsigned byte (1) and word (0) access
     * @param action distinguishes between a load (1) and store (0)
     * @param dst destination register
     * @param addr memory address
     */
    private void emitLoadStore(int cond, int access, int action, Register dst, Address2 addr) {
        Assert.that(access == 0 || access == 1, "access can only be 0 or 1");
        Assert.that(action == 0 || action == 1, "action can only be 0 or 1");

        final int bit25 = addr.getType() == ADDR_IMM ? 0 : 0x2;
        final int p = addr.getPreIndexed () ? 1 : 0;
        final int s = addr.getSign () ? 0x80 : 0;
        final int b = access << 6;
        final int w = addr.getUpdateBase () ? 0x20 : 0;
        final int l = action << 4;

        emitByte((cond << 4) | 0x4 | bit25 | p);
        emitByte(s | b | w | l | (addr.getBaseReg ().getNumber() & 0xf));

        switch (addr.getType())
        {
            case ADDR_IMM:
            {
                emitByte((dst.getNumber() << 4) | (addr.getOffset() >>> 8));
                emitByte(addr.getOffset() & 0xff);
                break;
            }

            case ADDR_REG:
            {
                emitByte(dst.getNumber () << 4);
                emitByte(addr.getIndexReg ().getNumber () & 0xf);
                break;
            }

            case ADDR_SCALE:
            {
                final int scaleMode = (addr.getScaleMode () & 0x3) << 5;

                emitByte((dst.getNumber() << 4) | ((addr.getShift() >>> 1) & 0xf));
                emitByte(((addr.getShift() & 0x1) << 7) | scaleMode | (addr.getIndexReg().getNumber() & 0xf));
                break;
            }

            default: Assert.that(false, "should not reach here");
        }
    }

    /**
     * Emits a miscellaneous load/store instruction.
     *
     * @param cond condition code
     * @param l distinguishes between load (l == 1) and store (l == 0) instructions
     * @param s distinguishes between a signed (s == 1) and unsigned (s == 0) halfword access
     * @param h distinguishes between halfword (h == 1) and a signed byte (h == 0) access
     * @param reg destination register
     * @param addr addresing mode (see ADDR_xxx constants in {@link Constants} under Addressing Mode 3)
     */
    private void emitLoadStoreMisc(int cond, int l, int s, int h, Register reg, Address3 addr) {
        final int bitU = (addr.getSign() ? 1 : 0) << 7;                // add or subtract index
        final int bit22 = (addr.getType() == ADDR_REG ? 0 : 1) << 6;   // immediate or register indexing
        final int bit21 = (addr.getUpdateBase() ? 1 : 0) << 5;         // pre or post indexing
        final int bitL = l << 4;
        final int bits8to11 = addr.getType() == ADDR_REG ? (SBZ.getNumber()) : ((addr.getOffset() >> 4) & 0xf);
        final int bitS = s << 6;
        final int bitH = h << 5;
        final int bits1to4 = addr.getType() == ADDR_REG ? (addr.getIndexReg().getNumber()) : (addr.getOffset() & 0xf);

        emitByte((cond << 4) | (addr.getPreIndexed() ? 1 : 0));
        emitByte(bitU | bit22 | bit21 | bitL | addr.getBaseReg().getNumber());
        emitByte((reg.getNumber() << 4) | bits8to11);
        emitByte(0x90 | bitS | bitH | bits1to4);
    }

    /**
     * Emits a load/store multiple instruction.
     *
     * @param cond condition code
     * @param l distinguishes between load (l == 1) and store (l == 0) instructions
     * @param addr addresing mode (see ADDR_xxx constants in {@link Constants} under Addressing Mode 4)
     * @param s specifies if the CPSR is loaded from the SPSR (for LDMs that load the PC) or
     * that user mode banked registers are transferred instead of the register of the current
     * mode (for LDMs that do not load the PC and all STMs)
     * @param w indicates if the base register is to be updated after the transfer
     * @param base base register used by the addressing mode
     * @param regs list of registers to be loaded or stored
     */
    private void emitLoadStoreMultiple(int cond, int l, int s, int w, Address4 addr) {
        emitByte((cond << 4) | 0x8 | addr.getPBit(l));
        emitByte((addr.getUBit(l) << 7) | (s << 6) | (w << 5) | (l << 4) | addr.getBaseReg().getNumber());
        emitByte(addr.getRegsBits() >>> 8);
        emitByte(addr.getRegsBits() & 0xff);
    }

    /**
     * This instruction adds the value of the src register and carry flag to the op2 operand and
     * stores the result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void adc(Register dst, Register src, Operand2 op2) {
        adc(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction adds the value of the src register and carry flag to the op2 operand,
     * storing the result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void adcs(Register dst, Register src, Operand2 op2) {
        adc(COND_AL, true, dst, src, op2);
    }

    /**
     * This instruction adds the value of the src register and the carry flag to the op2 operand
     * and stores the result in the dst register only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void adccond(int cond, Register dst, Register src, Operand2 op2) {
        adc(cond, false, dst, src, op2);
    }

    /**
     * This instruction adds the value of the src register and carry flag to the op2 operand
     * and stores the result in the dst register and updates the CPSR only when the condition is
     * met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void adcconds(int cond, Register dst, Register src, Operand2 op2) {
        adc(cond, true, dst, src, op2);
    }

    /**
     * Emits an adc instruction.
     *
     * @param cond condition under which the instruction is executed
     * @param s specifies whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    private void adc(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_ADC, s, dst, src, op2);
    }

    /**
     * This instruction adds the value of the src register and the op2 operand and stores the
     * result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void add(Register dst, Register src, Operand2 op2) {
        add(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction adds the value of the src register and the op2 operand, stores the
     * result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void adds(Register dst, Register src, Operand2 op2) {
        add(COND_AL, true, dst, src, op2);
    }

    /**
     * This instruction adds the value of the src register and the op2 operand and stores the
     * result in the dst register only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void addcond(int cond, Register dst, Register src, Operand2 op2) {
        add(cond, false, dst, src, op2);
    }

    /**
     * This instruction adds the value of the src register and the op2 operand, stores the
     * result in the dst register and updates the CPSR only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void addconds(int cond, Register dst, Register src, Operand2 op2) {
        add(cond, true, dst, src, op2);
    }

    /**
     * Emits an add instruction.
     *
     * @param cond condition under which the instruction is executed
     * @param s specifies whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    private void add(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_ADD, s, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise AND of the value in the src register with the value of
     * op2 and stores the result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void and(Register dst, Register src, Operand2 op2) {
        and(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise AND of the value in the src register with the value of
     * op2 and stores the result in the dst register when the condition is met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void andcond(int cond, Register dst, Register src, Operand2 op2) {
        and(cond, false, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise AND of the value in the src register with the value of
     * op2 and stores the result in the dst register and updates the CPSR when the condition is
     * met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void andconds(int cond, Register dst, Register src, Operand2 op2) {
        and(cond, true, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise AND of the value in the src register with the value of
     * op2 and stores the result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible source operand
     */
    public void ands(Register dst, Register src, Operand2 op2) {
        and(COND_AL, true, dst, src, op2);
    }

    /**
     * Emits an and instruction.
     *
     * @param cond condition code
     * @param s whether the CPSR should be updated by the instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible source operand
     */
    private void and(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_AND, s, dst, src, op2);
    }

    /**
     * This instruction jumps to the target address unconditionally.
     *
     * @param label destination operand label
     */
    public void b(ALabel label) {
        emitBranch(COND_AL, 0, label);
    }

    /**
     * This instruction jumps to the target address when the condition is satisfied.
     *
     * @param cond condition code
     * @param label destination operand label
     */
    public void bcond(int cond, ALabel label) {
        emitBranch(cond, 0, label);
    }

    /**
     * This instruction jumps to the target address when the condition is satisfied.
     *
     * @param cond condition code
     * @param l specifies whether the return address should be saved (1) or not (0)
     * @param label destination operand label
     */
    public void bcond(int cond, int l, ALabel label) {
        Assert.that(l == 0 || l == 1, "invalid l value");
        emitBranch(cond, l, label);
    }

    /**
     * This instruction causes a software breakpoint to occur.
     *
     * @param imm16 information about the breakpoint for use by the debugger
     */
    public void bkpt(int imm16) {
        emitByte(0xe1);
        emitByte(0x20 | (imm16 >>> 12));
        emitByte(imm16 >>> 4);
        emitByte(0x70 | imm16 & 0xf);
    }

    /**
     * This instruction performs a bitwise AND of the value in the src register with the complement
     * of the value of op2 and stores the result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void bic(Register dst, Register src, Operand2 op2) {
        bic(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise AND of the value in the src register with the complement
     * of the value of op2 and stores the result in the dst register when the condition is met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void biccond(int cond, Register dst, Register src, Operand2 op2) {
        bic(cond, false, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise AND of the value in the src register with the complement
     * of the value of op2 and stores the result in the dst register and updates the CPSR when the
     * condition is met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void bicconds(int cond, Register dst, Register src, Operand2 op2) {
        bic(cond, true, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise AND of the value in the src register with the complement
     * of the value of op2 and stores the result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible source operand
     */
    public void bics(Register dst, Register src, Operand2 op2) {
        bic(COND_AL, true, dst, src, op2);
    }

    /**
     * Emits a bic instruction.
     *
     * @param cond condition code
     * @param s whether the CPSR should be updated by the instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible source operand
     */
    private void bic(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_BIC, s, dst, src, op2);
    }

    /**
     * This instruction jumps to the target address unconditionally and saves the return address
     * in the link register.
     *
     * @param label destination operand label
     */
    public void bl(ALabel label) {
        emitBranch(COND_AL, 1, label);
    }

    /**
     * This instruction jumps to the target address and saves the return address in the link
     * register when the condition is satisfied.
     *
     * @param cond condition code
     * @param label destination operand label
     */
    public void blcond(int cond, ALabel label) {
        emitBranch(cond, 1, label);
    }

    /**
     * This instruction compares a register value with another arithmetic value by addition and
     * updates the CPSR.
     *
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void cmn(Register op1, Operand2 op2) {
        cmncond(COND_AL, op1, op2);
    }

    /**
     * This instruction compares a register value with another arithmetic value by addition and
     * updates the CPSR when the condition is met.
     *
     * @param cond condition code
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void cmncond(int cond, Register op1, Operand2 op2) {
        emitDataProc(cond, OPCODE_CMN, true, Register.SBZ, op1, op2);
    }

    /**
     * This instruction compares a register value with another arithmetic value by subtraction and
     * updates the CPSR.
     *
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void cmp(Register op1, Operand2 op2) {
        cmpcond(COND_AL, op1, op2);
    }

    /**
     * This instruction compares a register value with another arithmetic value by subtraction and
     * updates the CPSR when the condition is met.
     *
     * @param cond condition code
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void cmpcond(int cond, Register op1, Operand2 op2) {
        emitDataProc(cond, OPCODE_CMP, true, Register.SBZ, op1, op2);
    }

    /**
     * This instruction performs a bitwise XOR of the value in the src register with the value of
     * op2 and stores the result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void eor(Register dst, Register src, Operand2 op2) {
        eor(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise XOR of the value in the src register with the value of
     * op2 and stores the result in the dst register when the condition is met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void eorcond(int cond, Register dst, Register src, Operand2 op2) {
        eor(cond, false, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise XOR of the value in the src register with the value of
     * op2 and stores the result in the dst register and updates the CPSR when the condition is
     * met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void eorconds(int cond, Register dst, Register src, Operand2 op2) {
        eor(cond, true, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise XOR of the value in the src register with the value of
     * op2 and stores the result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible source operand
     */
    public void eors(Register dst, Register src, Operand2 op2) {
        eor(COND_AL, true, dst, src, op2);
    }

    /**
     * Emits an eor instruction.
     *
     * @param cond condition code
     * @param s whether the CPSR should be updated by the instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible source operand
     */
    private void eor(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_EOR, s, dst, src, op2);
    }

    /**
     * This instruction loads a non-empty subset (or all) of the general-purpose registers from
     * sequential memory locations.
     *
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void ldm(Address4 addr) {
        emitLoadStoreMultiple(COND_AL, 1, 0, 0, addr);
    }

    /**
     * This instruction loads a non-empty subset (or all) of the general-purpose registers from
     * sequential memory locations when the condition is met.
     *
     * @param cond condition code
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void ldmcond(int cond, Address4 addr) {
        emitLoadStoreMultiple(cond, 1, 0, 0, addr);
    }

    /**
     * This instruction loads a non-empty subset (or all) of the general-purpose registers from
     * sequential memory locations and updates the base register when the condition is met.
     *
     * @param cond condition code
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void ldmcondw(int cond, Address4 addr) {
        emitLoadStoreMultiple(cond, 1, 0, 1, addr);
    }

    /**
     * This instruction loads a non-empty subset (or all) of the general-purpose registers from
     * sequential memory locations and updates the base register.
     *
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void ldmw(Address4 addr) {
        emitLoadStoreMultiple(COND_AL, 1, 0, 1, addr);
    }

    /**
     * This instruction loads a word from the specified memory address and writes it to the
     * destination register.
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void ldr(Register dst, Address2 addr) {
        ldrcond(COND_AL, dst, addr);
    }

    /**
     * This instruction loads a byte from the specified memory address and writes the value zero
     * extended to 32-bits in the destination register.
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrb(Register dst, Address2 addr) {
        ldrcondb(COND_AL, dst, addr);
    }

    /**
     * This instruction loads a word from the specified memory address and writes it to the
     * destination register when the condition is satisfied.
     *
     * @param cond condition under which this instruction is executed
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrcond(int cond, Register dst, Address2 addr) {
        emitLoadStore(cond, 0, 1, dst, addr);
    }

    /**
     * This instruction loads a byte from the specified memory address and writes the value zero
     * extended to 32-bits in the destination register when the condition is satisfied.
     *
     * @param cond condition under which this instruction is executed
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrcondb(int cond, Register dst, Address2 addr) {
        emitLoadStore(cond, 1, 1, dst, addr);
    }

    /**
     * This instruction loads a doubleword from the specified memory address and writes the value
     * in two registers beginning at the destination register.
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrd(Register dst, Address3 addr) {
        Assert.that(dst.getNumber() % 2 == 0, "destination register must be even");
        Assert.that(dst != R14, "destination register cannot be " + R14);

        emitLoadStoreMisc(COND_AL, 0, 1, 0, dst, addr);
    }

    /**
     * This instruction loads a doubleword from the specified memory address and writes the value
     * in two registers beginning at the destination register when the condition is satisfied.
     *
     * @param cond condition code
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrcondd(int cond, Register dst, Address3 addr) {
        Assert.that(dst.getNumber() % 2 == 0, "destination register must be even");
        Assert.that(dst != R14, "destination register cannot be " + R14);

        emitLoadStoreMisc(cond, 0, 1, 0, dst, addr);
    }

    /**
     * This instruction loads a halfword from the specified memory address and writes the value
     * zero extended to 32-bits in the destination register.
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrh(Register dst, Address3 addr) {
        emitLoadStoreMisc(COND_AL, 1, 0, 1, dst, addr);
    }

    /**
     * This instruction loads a halfword from the specified memory address and writes the value
     * zero extended to 32-bits in the destination register when the condition is satisfied.
     *
     * @param cond condition code
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrcondh(int cond, Register dst, Address3 addr) {
        emitLoadStoreMisc(cond, 1, 0, 1, dst, addr);
    }

    /**
     * This instruction loads a signed byte from the specified memory address and writes the value
     * signed extended to 32-bits in the destination register.
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrsb(Register dst, Address3 addr) {
        emitLoadStoreMisc(COND_AL, 1, 1, 0, dst, addr);
    }

    /**
     * This instruction loads a signed byte from the specified memory address and writes the value
     * sign extended to 32-bits in the destination register when the condition is satisfied.
     *
     * @param cond condition code
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrcondsb(int cond, Register dst, Address3 addr) {
        emitLoadStoreMisc(cond, 1, 1, 0, dst, addr);
    }

    /**
     * This instruction loads a signed halfword from the specified memory address and writes the value
     * signed extended to 32-bits in the destination register.
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrsh(Register dst, Address3 addr) {
        emitLoadStoreMisc(COND_AL, 1, 1, 1, dst, addr);
    }

    /**
     * This instruction loads a signed halfword from the specified memory address and writes the value
     * sign extended to 32-bits in the destination register when the condition is satisfied.
     *
     * @param cond condition code
     * @param dst destination register
     * @param addr memory address
     */
    public void ldrcondsh(int cond, Register dst, Address3 addr) {
        emitLoadStoreMisc(cond, 1, 1, 1, dst, addr);
    }

    /**
     * This instruction multiplies the values in op1 and op2, adds op3 to the result and stores
     * the final result into dst.
     *
     * @param dst destination register
     * @param op1 source register multiplied with op2
     * @param op2 source register multiplied with op1
     * @param op3 source register added to op1 * op2
     */
    public void mla(Register dst, Register op1, Register op2, Register op3) {
        mla(COND_AL, false, dst, op1, op2, op3);
    }

    /**
     * This instruction multiplies the values in op1 and op2, adds op3 to the result and stores
     * the final result into dst when the condition is met.
     *
     * @param cond condition flag
     * @param dst destination register
     * @param op1 source register multiplied with op2
     * @param op2 source register multiplied with op1
     * @param op3 source register added to op1 * op2
     */
    public void mlacond(int cond, Register dst, Register op1, Register op2, Register op3) {
        mla(cond, false, dst, op1, op2, op3);
    }

    /**
     * This instruction multiplies the values in op1 and op2, adds op3 to the result and stores
     * the final result into dst and updates the CPSR when the condition is met.
     *
     * @param cond condition flag
     * @param dst destination register
     * @param op1 source register multiplied with op2
     * @param op2 source register multiplied with op1
     * @param op3 source register added to op1 * op2
     */
    public void mlaconds(int cond, Register dst, Register op1, Register op2, Register op3) {
        mla(cond, true, dst, op1, op2, op3);
    }

    /**
     * This instruction multiplies the values in op1 and op2, adds op3 to the result and stores
     * the final result into dst and updates the CPSR.
     *
     * @param dst destination register
     * @param op1 source register multiplied with op2
     * @param op2 source register multiplied with op1
     * @param op3 source register added to op1 * op2
     */
    public void mlas(Register dst, Register op1, Register op2, Register op3) {
        mla(COND_AL, true, dst, op1, op2, op3);
    }

    /**
     * Emits an mla instruction.
     *
     * @param cond condition code
     * @param s determines whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param op1 first source operand (multiplied with op2)
     * @param op2 second source operand (multiplied with op1)
     * @param op3 third source operand (added to op1 * op2)
     */
    private void mla(int cond, boolean s, Register dst, Register op1, Register op2, Register op3) {
        Assert.that(dst != PC && op1 != PC && op2 != PC && op3 != PC, "register " + PC + " not allowed here");

        emitByte(cond << 4);
        emitByte(0x20 | (s ? 0x10 : 0x0) | dst.getNumber());
        emitByte((op3.getNumber() << 4) | op2.getNumber());
        emitByte(0x90 | op1.getNumber());
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void mov(Register dst, Operand2 op2) {
        mov(COND_AL, false, dst, op2);
    }

    /**
     * This instruction copies the source operand to the destination operand and updates the CPSR.
     *
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void movs(Register dst, Operand2 op2) {
        mov(COND_AL, true, dst, op2);
    }

    /**
     * This instruction copies the source operand to the destination operand when the condition is
     * true.
     *
     * @param cond condition flag
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void movcond(int cond, Register dst, Operand2 op2) {
        mov(cond, false, dst, op2);
    }

    /**
     * This instruction copies the source operand to the destination operand when the condition is
     * true and updates the CPSR.
     *
     * @param cond condition flag
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void movconds(int cond, Register dst, Operand2 op2) {
        mov(cond, true, dst, op2);
    }

    /**
     * Emits a mov instruction.
     *
     * @param cond condition flag under which this instruction will be executed
     * @param s determines whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param op2 flexible source operand
     */
    private void mov(int cond, boolean s, Register dst, Operand2 op2) {
        emitDataProc(cond, OPCODE_MOV, s, dst, Register.SBZ, op2);
    }

    /**
     * This instruction multiplies the values in two registers together and stores the result in
     * another register.
     *
     * @param dst destination register
     * @param op1 first source register
     * @param op2 second source register
     */
    public void mul(Register dst, Register op1, Register op2) {
        mul(COND_AL, false, dst, op1, op2);
    }

    /**
     * This instruction multiplies the values in two registers together and stores the result in
     * another register when the condition is met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param op1 first source register
     * @param op2 second source register
     */
    public void mulcond(int cond, Register dst, Register op1, Register op2) {
        mul(cond, false, dst, op1, op2);
    }

    /**
     * This instruction multiplies the values in two registers together and stores the result in
     * another register and updates the CPSR when the condition is met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param op1 first source register
     * @param op2 second source register
     */
    public void mulconds(int cond, Register dst, Register op1, Register op2) {
        mul(cond, true, dst, op1, op2);
    }

    /**
     * This instruction multiples the values in two registers together and stores the result in
     * another register and updates the CPSR.
     *
     * @param dst destination register
     * @param op1 first source register
     * @param op2 second source register
     */
    public void muls(Register dst, Register op1, Register op2) {
        mul(COND_AL, true, dst, op1, op2);
    }

    /**
     * Emits a mul instruction.
     *
     * @param cond condition flag
     * @param s determines whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param op1 first source operand register
     * @param op2 second source operand register
     */
    private void mul(int cond, boolean s, Register dst, Register op1, Register op2) {
        Assert.that(dst != PC && op1 != PC && op2 != PC, "register " + PC + " not allowed here");

        emitByte(cond << 4);
        emitByte((s ? 0x10 : 0x0) | dst.getNumber());
        emitByte((SBZ.getNumber() << 4) | op2.getNumber());
        emitByte(0x90 | op1.getNumber());
    }

    /**
     * This instruction copies the logical one's complement of the source operand to the
     * destination register.
     *
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void mvn(Register dst, Operand2 op2) {
        mvn(COND_AL, false, dst, op2);
    }

    /**
     * This instruction copies the logical one's complement of the source operand to the
     * destination register and updates the CPSR.
     *
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void mvns(Register dst, Operand2 op2) {
        mvn(COND_AL, true, dst, op2);
    }

    /**
     * This instruction copies the logical one's complement of the source operand to the
     * destination register when the condition is met.
     *
     * @param cond condition flag
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void mvncond(int cond, Register dst, Operand2 op2) {
        mvn(cond, false, dst, op2);
    }

    /**
     * This instruction copies the logical one's complement of the source operand to the
     * destination register and updates the CPSR when the condition is met.
     *
     * @param cond condition flag
     * @param dst destination register
     * @param op2 flexible source operand
     */
    public void mvnconds(int cond, Register dst, Operand2 op2) {
        mvn(cond, true, dst, op2);
    }

    /**
     * Emits an mvn instruction.
     *
     * @param cond condition flag under which this instruction will be executed
     * @param s determines whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param op2 flexible source operand
     */
    private void mvn(int cond, boolean s, Register dst, Operand2 op2) {
        emitDataProc(cond, OPCODE_MVN, s, dst, Register.SBZ, op2);
    }

    /**
     * This instruction performs no operation. It is implemented as "mov r0, r0".
     */
    public void nop() {
        mov(Register.R0, Operand2.reg(Register.R0));
    }

    /**
     * This instruction performs a bitwise OR of the value in the src register with the value of
     * op2 and stores the result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void orr(Register dst, Register src, Operand2 op2) {
        orr(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise OR of the value in the src register with the value of
     * op2 and stores the result in the dst register when the condition is met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void orrcond(int cond, Register dst, Register src, Operand2 op2) {
        orr(cond, false, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise OR of the value in the src register with the value of
     * op2 and stores the result in the dst register and updates the CPSR when the condition is
     * met.
     *
     * @param cond condition code
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void orrconds(int cond, Register dst, Register src, Operand2 op2) {
        orr(cond, true, dst, src, op2);
    }

    /**
     * This instruction performs a bitwise OR of the value in the src register with the value of
     * op2 and stores the result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible source operand
     */
    public void orrs(Register dst, Register src, Operand2 op2) {
        orr(COND_AL, true, dst, src, op2);
    }

    /**
     * Emits an orr instruction.
     *
     * @param cond condition code
     * @param s whether the CPSR should be updated by the instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible source operand
     */
    private void orr(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_ORR, s, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the src register from the op2 operand and stores the
     * result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void rsb(Register dst, Register src, Operand2 op2) {
        rsb(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the src register from the op2 operand and stores the
     * result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void rsbs(Register dst, Register src, Operand2 op2) {
        rsb(COND_AL, true, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the src register from the op2 operand and stores the
     * result in the dst register only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void rsbcond(int cond, Register dst, Register src, Operand2 op2) {
        rsb(cond, false, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the src register from the op2 operand and stores the
     * result in the dst register and updates the CPSR only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void rsbconds(int cond, Register dst, Register src, Operand2 op2) {
        rsb(cond, true, dst, src, op2);
    }

    /**
     * Emits an rsb instruction.
     *
     * @param cond condition under which the instruction is executed
     * @param s specifies whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    private void rsb(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_RSB, s, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the src register from the op2 operand with carry
     * and stores the result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
v     * @param op2 flexible second operand
     */
    public void rsc(Register dst, Register src, Operand2 op2) {
        rsc(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the src register from the op2 operand with carry
     * and stores the result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void rscs(Register dst, Register src, Operand2 op2) {
        rsc(COND_AL, true, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the src register from the op2 operand with carry
     * and stores the result in the dst register only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void rsccond(int cond, Register dst, Register src, Operand2 op2) {
        rsc(cond, false, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the src register from the op2 operand with carry
     * and stores the result in the dst register and updates the CPSR only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void rscconds(int cond, Register dst, Register src, Operand2 op2) {
        rsc(cond, true, dst, src, op2);
    }

    /**
     * Emits an rsc instruction.
     *
     * @param cond condition under which the instruction is executed
     * @param s specifies whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    private void rsc(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_RSC, s, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the op2 operand from the src register with carry
     * and stores the result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void sbc(Register dst, Register src, Operand2 op2) {
        sbc(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the op2 operand from the src register with carry
     * and stores the result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void sbcs(Register dst, Register src, Operand2 op2) {
        sbc(COND_AL, true, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the op2 operand from the src register with carry
     * and stores the result in the dst register only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void sbccond(int cond, Register dst, Register src, Operand2 op2) {
        sbc(cond, false, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the op2 operand from the src register with carry
     * and stores the result in the dst register and updates the CPSR only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void sbcconds(int cond, Register dst, Register src, Operand2 op2) {
        sbc(cond, true, dst, src, op2);
    }

    /**
     * Emits an sbc instruction.
     *
     * @param cond condition under which the instruction is executed
     * @param s specifies whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    private void sbc(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_SBC, s, dst, src, op2);
    }

    /**
     * This instruction stores a non-empty subset (or all) of the general-purpose registers to
     * sequential memory locations.
     *
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void stm(Address4 addr) {
        emitLoadStoreMultiple(COND_AL, 0, 0, 0, addr);
    }

    /**
     * This instruction stores a non-empty subset (or all) of the general-purpose registers to
     * sequential memory locations when the condition is met.
     *
     * @param cond condition code
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void stmcond(int cond, Address4 addr) {
        emitLoadStoreMultiple(cond, 0, 0, 0, addr);
    }

    /**
     * This instruction stores a non-empty subset (or all) of the general-purpose registers to
     * sequential memory locations and updates the base register when the condition is met.
     *
     * @param cond condition code
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void stmcondw(int cond, Address4 addr) {
        emitLoadStoreMultiple(cond, 0, 0, 1, addr);
    }

    /**
     * This instruction stores a non-empty subset (or all) of the general-purpose registers to
     * sequential memory locations and updates the base register.
     *
     * @param addr address specifying where and how the registers are to be loaded
     */
    public void stmw(Address4 addr) {
        emitLoadStoreMultiple(COND_AL, 0, 0, 1, addr);
    }

    /**
     * This instruction stores a word from a register to the specified memory address.
     *
     * @param source source register
     * @param addr memory address
     */
    public void str(Register src, Address2 addr) {
        strcond(COND_AL, src, addr);
    }

    /**
     * This instruction stores a byte from the least significant byte of a register to the
     * specified memory address.
     *
     * @param src source register
     * @param addr memory address
     */
    public void strb(Register src, Address2 addr) {
        strcondb(COND_AL, src, addr);
    }

    /**
     * This instruction stores a word from a register to the specified memory address when the
     * condition is satisfied.
     *
     * @param cond condition under which this instruction is executed
     * @param src source register
     * @param addr memory address
     */
    public void strcond(int cond, Register src, Address2 addr) {
        emitLoadStore(cond, 0, 0, src, addr);
    }

    /**
     * This instruction stores a byte from the least significant byte of a register to the
     * specified memory address when the condition is satisfied.
     *
     * @param cond condition under which this instruction is executed
     * @param src source register
     * @param addr memory address
     */
    public void strcondb(int cond, Register src, Address2 addr) {
        emitLoadStore(cond, 1, 0, src, addr);
    }

    /**
     * This instruction stores a doubleword from the two registers beginning at the destination
     * register to the specified memory address.
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void strd(Register src, Address3 addr) {
        Assert.that(src.getNumber() % 2 == 0, "source register must be even");
        Assert.that(src != R14, "source register cannot be " + R14);

        emitLoadStoreMisc(COND_AL, 0, 1, 1, src, addr);
    }

    /**
     * This instruction stores a doubleword from the two registers beginning at the destination
     * register to the specified memory address when the condition is satisfied.
     *
     * @param cond condition code
     * @param dst destination register
     * @param addr memory address
     */
    public void strcondd(int cond, Register src, Address3 addr) {
        Assert.that(src.getNumber() % 2 == 0, "source register must be even");
        Assert.that(src != R14, "source register cannot be " + R14);

        emitLoadStoreMisc(cond, 0, 1, 1, src, addr);
    }

    /**
     * This instruction stores a halfword from the least significant halfword of the source
     * register to the specified memory address.
     *
     * @param dst destination register
     * @param addr memory address
     */
    public void strh(Register src, Address3 addr) {
        emitLoadStoreMisc(COND_AL, 0, 0, 1, src, addr);
    }

    /**
     * This instruction stores a halfword from the least significant halfword of the source
     * register to the specified memory address when the condition is satisfied.
     *
     * @param cond condition code
     * @param dst destination register
     * @param addr memory address
     */
    public void strcondh(int cond, Register src, Address3 addr) {
        emitLoadStoreMisc(cond, 0, 0, 1, src, addr);
    }

    /**
     * This instruction subtracts the value of the op2 operand from the src register and stores the
     * result in the dst register.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void sub(Register dst, Register src, Operand2 op2) {
        sub(COND_AL, false, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the op2 operand from the src register and stores the
     * result in the dst register and updates the CPSR.
     *
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void subs(Register dst, Register src, Operand2 op2) {
        sub(COND_AL, true, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the op2 operand from the src register and stores the
     * result in the dst register only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void subcond(int cond, Register dst, Register src, Operand2 op2) {
        sub(cond, false, dst, src, op2);
    }

    /**
     * This instruction subtracts the value of the op2 operand from the src register and stores the
     * result in the dst register and updates the CPSR only when the condition is met.
     *
     * @param cond condition under which the instruction is executed
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    public void subconds(int cond, Register dst, Register src, Operand2 op2) {
        sub(cond, true, dst, src, op2);
    }

    /**
     * Emits a sub instruction.
     *
     * @param cond condition under which the instruction is executed
     * @param s specifies whether the CPSR should be updated by this instruction
     * @param dst destination register
     * @param src source operand register
     * @param op2 flexible second operand
     */
    private void sub(int cond, boolean s, Register dst, Register src, Operand2 op2) {
        emitDataProc(cond, OPCODE_SUB, s, dst, src, op2);
    }

    /**
     * This instruction causes a Software Interrupt (SWI) exception unconditionally.
     *
     * @param imm24 condition under which the instruction is executed
     */
    public void swi(int imm24) {
        swicond(COND_AL, imm24);
    }

    /**
     * This instruction causes a Software Interrupt (SWI) exception, used as an operating system
     * call.
     *
     * @param cond condition under which the instruction is executed
     * @param imm24 specifies the required service to the operating system
     */
    public void swicond(int cond, int imm24) {
        Assert.that((imm24 & 0xfff) == imm24, "not a 24-bit integer");

        emitByte((cond << 4) | 0xf);
        emitInt24(imm24);
    }

    /**
     * This instruction swaps a word between registers and memory. A word is loaded from the memory
     * location specified by the address register and its value stored in the destination register.
     * The value in the source register is then stored at the memory location.
     *
     * @param dst register where the value at the memory address will be written to
     * @param src value that will be written to the memory address
     * @param addr address of the memory location
     */
    public void swp(Register dst, Register src, Register addr) {
        swpcond(COND_AL, dst, src, addr);
    }

    /**
     * This instruction swaps a word between registers and memory when the condition is sastified.
     * A word is loaded from the memory location specified by the address register and its value
     * stored in the destination register. The value in the source register is then stored at the
     * memory location.
     *
     * @param cond condition code
     * @param dst register where the value at the memory address will be written to
     * @param src value that will be written to the memory address
     * @param addr address of the memory location
     */
    public void swpcond(int cond, Register dst, Register src, Register addr) {
        Assert.that(dst != R15 && src != R15 && addr != R15, R15 + " cannot be used as a parameter");
        Assert.that(src != addr, "the same register cannot be used for both the source value and memory address");
        Assert.that(dst != addr, "the same register cannot be used for both the destination value and memory address");

        emitByte((cond << 4) | 0x1);
        emitByte(addr.getNumber());
        emitByte(dst.getNumber() << 4);
        emitByte(0x90 | src.getNumber());
    }

    /**
     * This instruction swaps a byte between registers and memory. A byte is loaded from the memory
     * location specified by the address register and its zero extended value stored in the
     * destination register. The least significant byte of the value in the source register is then
     * stored at the memory location.
     *
     * @param dst register where the value at the memory address will be written to
     * @param src value that will be written to the memory address
     * @param addr address of the memory location
     */
    public void swpb(Register dst, Register src, Register addr) {
        swpcondb(COND_AL, dst, src, addr);
    }

    /**
     * This instruction swaps a byte between registers and memory when the condition is sastified.
     * A byte is loaded from the memory location specified by the address register and its zero
     * extended value stored in the destination register. The least significant byte of the value
     * in the source register is then stored at the memory location.
     *
     * @param cond condition code
     * @param dst register where the value at the memory address will be written to
     * @param src value that will be written to the memory address
     * @param addr address of the memory location
     */
    public void swpcondb(int cond, Register dst, Register src, Register addr) {
        Assert.that(dst != R15 && src != R15 && addr != R15, R15 + " cannot be used as a parameter");
        Assert.that(src != addr, "the same register cannot be used for both the source value and memory address");
        Assert.that(dst != addr, "the same register cannot be used for both the destination value and memory address");

        emitByte((cond << 4) | 0x1);
        emitByte(0x40 | addr.getNumber());
        emitByte(dst.getNumber() << 4);
        emitByte(0x90 | src.getNumber());
    }

    /**
     * This instruction compares a register value OR'd with another arithmetic value and updates the
     * CPSR.
     *
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void teq(Register op1, Operand2 op2) {
        teqcond(COND_AL, op1, op2);
    }

    /**
     * This instruction compares a register value OR'd with another arithmetic value and updates the
     * CPSR when the condition is met.
     *
     * @param cond condition code
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void teqcond(int cond, Register op1, Operand2 op2) {
        emitDataProc(cond, OPCODE_TEQ, true, Register.SBZ, op1, op2);
    }

    /**
     * This instruction compares a register value AND'd with another arithmetic value and updates the
     * CPSR.
     *
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void tst(Register op1, Operand2 op2) {
        tstcond(COND_AL, op1, op2);
    }

    /**
     * This instruction compares a register value AND'd with another arithmetic value and updates the
     * CPSR when the condition is met.
     *
     * @param cond condition code
     * @param op1 first operand register
     * @param op2 flexible second operand
     */
    public void tstcond(int cond, Register op1, Operand2 op2) {
        emitDataProc(cond, OPCODE_TST, true, Register.SBZ, op1, op2);
    }
}
