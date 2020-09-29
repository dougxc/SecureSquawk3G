/*
 * @(#)Assembler.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.x86;

/**
 * This class contains methods for generating code into a code buffer. For each
 * possibly used machine instruction there exists a method that combines the
 * operation code and the specified parameters into one or more bytes and
 * appends them to the end of the code buffer. All instruction encodings are
 * subsets of the general instruction format:<p>
 *
 * <table align="center" border="1" style="font-family:sans-serif;font-size:10pt"><tr>
 * <td>&nbsp;Prefixes&nbsp;</td><td>&nbsp;Opcode&nbsp;</td>
 * <td>&nbsp;ModR/M&nbsp;</td><td>&nbsp;SIB&nbsp;</td>
 * <td>&nbsp;Displacement&nbsp;</td><td>&nbsp;Immediate&nbsp;</td>
 * </tr></table><p>
 *
 * The FPU instructions treat the eight FPU data registers as a register stack.
 * The register number of the current top-of-stack register ST(0) is stored in
 * the TOP field of the FPU status word. ST(i) denotes the i-th element from the
 * top of the register stack.<p>
 *
 * Some of the condition code constants have the same value. The resulting
 * instructions have the same operation code and test for the same condition.
 * The alternate mnemonics are provided only to make code more intelligible.
 *
 * @see      "IA-32 Intel Architecture Software Developer's Manual"
 * @author   Thomas Kotzmann
 * @author   Nik Shaylor (Adaptation from Javac1)
 * @version  1.00
 */
public class Assembler implements Constants {

    /**
     * Relocation type for absolute int addresses.
     */
    public static final int RELOC_ABSOLUTE_INT = 0;

    /**
     * Relocation type for relative int addresses.
     */
    public static final int RELOC_RELATIVE_INT = 1;

    /**
     * The code buffer that stores the generated instructions.
     */
    private CodeBuffer code;

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
        this.code = code;
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
     * Tests if the specified value can be represented with not more than 8
     * bits. This is true if and only if the value is inside the interval
     * [-128,128[.
     *
     * @param   x  the value to be tested
     * @return  whether or not the argument is an 8-bit value
     */
    protected static boolean is8bit(int x) {
        return (-128 <= x) && (x < 128);
    }

    /**
     * Tests if the specified value is within the range of one unsigned byte.
     * This is true if and only if the value is inside the interval [0,256[.
     *
     * @param   x  the value to be tested
     * @return  whether or not the argument is a byte
     */
    protected static boolean isByte(int x) {
        return (0 <= x) && (x < 256);
    }

    /**
     * Tests if the specified value is within the range of shift distances. This
     * is true if and only if the value is inside the interval [0,32[.
     *
     * @param   x  the value to be tested
     * @return  whether or not the argument is a valid shift distance
     */
    protected static boolean isShiftCount(int x) {
        return (0 <= x) && (x < 32);
    }

    /**
     * Tests if the CMOV instructions are supported by the processor. The
     * conditional moves and some other instructions have been introduced for
     * the Pentium Pro processor family.
     *
     * @return  whether or not CMOV is supported
     */
    protected static boolean supportsCMOV() {
        return Info.CodeForP6;
    }

    /**
     * Returns the code buffer that contains the generated instructions.
     *
     * @return  the code buffer
     */
    public CodeBuffer getCode() {
        return code;
    }

    /**
     * Returns the address of the first byte of the code buffer.
     *
     * @return  start address of the code
     */
    public int getCodeBegin() {
        return code.getCodeBegin();
    }

    /**
     * Returns the current code generation position.
     *
     * @return  current code generation position
     */
    public int getCodePos() {
        return code.getCodePos();
    }

    /**
     * Returns the current code generation offset.
     *
     * @return  current code generation offset
     */
    public int getOffset() {
        return getCodePos() - getCodeBegin();
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
        emitByte(x & 0xff);
        emitByte(x >>> 8);
    }

    /**
     * Emits the specified 32-bit integer value into the code buffer.
     *
     * @param  x  32-bit value to be emitted
     */
    public void emitInt(int x) {
        emitShort(x & 0xffff);
        emitShort(x >>> 16);
    }

    /**
     * Emits the specified 64-bit integer value into the code buffer.
     *
     * @param  x  64-bit value to be emitted
     */
    public void emitLong(long x) {
        emitInt((int)(x));
        emitInt((int)(x >>> 32));
    }

/*if[FLOATS]*/

    /**
     * Emits the specified Java float value into the code buffer.
     *
     * @param  x  Java float value to be emitted
     */
    public void emitFloat(float x) {
        emitInt(Float.floatToIntBits(x));
    }

    /**
     * Emits the specified Java double value into the code buffer.
     *
     * @param  x  Java double value to be emitted
     */
    public void emitDouble(double x) {
        emitLong(Double.doubleToLongBits(x));
    }

/*end[FLOATS]*/

    /**
     * Returns the byte at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the byte at the specified index
     */
    public int byteAt(int pos) {
        return code.byteAt(pos);
    }

    /**
     * Returns the 16-bit value at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the 16-bit value at the specified index
     */
    public int shortAt(int pos) {
        return (byteAt(pos + 1) << 8) | byteAt(pos);
    }

    /**
     * Returns the 32-bit value at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the 32-bit value at the specified index
     */
    public int intAt(int pos) {
        return (shortAt(pos + 2) << 16) | shortAt(pos);
    }

    /**
     * Sets the byte at the specified position to the new value.
     *
     * @param  pos  index of the byte to be replaced
     * @param  x    the new value
     */
    public void setByteAt(int pos, int x) {
        code.setByteAt(pos, x);
    }

    /**
     * Sets the 16-bit integer at the specified position to the new value.
     *
     * @param  pos  index of the 16-bit value to be replaced
     * @param  x    the new value
     */
    public void setShortAt(int pos, int x) {
        setByteAt(pos, x & 0xff);
        setByteAt(pos + 1, x >>> 8);
    }

    /**
     * Sets the 32-bit integer at the specified position to the new value.
     *
     * @param  pos  index of the 32-bit value to be replaced
     * @param  x    the new value
     */
    public void setIntAt(int pos, int x) {
        setShortAt(pos, x & 0xffff);
        setShortAt(pos + 2, x >>> 16);
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
     * Emits an arithmetic instruction with an 8-bit immediate operand.
     *
     * @param  op1   primary operation code
     * @param  op2   extension of the operation code
     * @param  dst   destination register
     * @param  imm8  8-bit immediate value
     */
    protected void emitArithByte(int op1, int op2, Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isByte(op1) && isByte(op2) && ((op1 & 0x01) == 0), "wrong operation code");
            Assert.that(dst.hasByteRegister(), "must have byte register");
            Assert.that(isByte(imm8), "immediate out of range");
        }
        emitByte(op1);
        emitByte(op2 | dst.getNumber());
        emitByte(imm8);
    }

    /**
     * Emits an arithmetic instruction with a 32-bit immediate operand.
     *
     * @param  op1    primary operation code
     * @param  op2    extension of the operation code
     * @param  dst    destination register
     * @param  imm32  32-bit immediate value
     */
    protected void emitArith(int op1, int op2, Register dst, int imm32) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isByte(op1) && isByte(op2) && ((op1 & 0x03) == 1), "wrong operation code");
        }
        if (is8bit(imm32)) {
            emitByte(op1 | 0x02);
            emitByte(op2 | dst.getNumber());
            emitByte(imm32 & 0xff);
        } else {
            emitByte(op1);
            emitByte(op2 | dst.getNumber());
            emitInt(imm32);
        }
    }

    /**
     * Emits an arithmetic instruction with a register operand.
     *
     * @param  op1  primary operation code
     * @param  op2  extension of the operation code
     * @param  dst  destination register
     * @param  src  source register
     */
    protected void emitArith(int op1, int op2, Register dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isByte(op1) && isByte(op2), "wrong operation code");
        }
        emitByte(op1);
        emitByte(op2 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * Emits the SIB byte of an address. The SIB byte encodes the scaling factor
     * as well as the number of the index register and the number of the base
     * register.
     *
     * @param  scale  scaling factor
     * @param  index  index register
     * @param  base   base register
     */
    private void emitSIB(int scale, Register index, Register base) {
        emitByte((scale << 6) | (index.getNumber() << 3) | base.getNumber());
    }

    /**
     * Emits the instruction part that specifies the operands. Depending on the
     * chosen addressing mode, between 1 and 6 bytes will be emitted.
     *
     * @param  reg    the register operand
     * @param  base   base register
     * @param  index  index register
     * @param  scale  scaling factor
     * @param  label  the displacement label if any
     * @param  disp   displacement if any
     */
    protected void emitOperand(Register reg, Register base, Register index, int scale, ALabel label, int disp) {
        boolean noRelocation = label == null;
        if (base.isValid()) {
            if (index.isValid()) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(scale != NO_SCALE, "inconsistent address");
                }
                if ((disp == 0) && noRelocation) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!index.equals(ESP) && !base.equals(EBP), "illegal addressing mode");
                    }
                    emitByte(0x04 | (reg.getNumber() << 3));
                    emitSIB(scale, index, base);
                } else if (is8bit(disp) && noRelocation) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!index.equals(ESP), "illegal addressing mode");
                    }
                    emitByte(0x44 | (reg.getNumber() << 3));
                    emitSIB(scale, index, base);
                    emitByte(disp & 0xff);
                } else {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!index.equals(ESP), "illegal addressing mode");
                    }
                    emitByte(0x84 | (reg.getNumber() << 3));
                    emitSIB(scale, index, base);
                    emitData(label, disp);
                }
            } else if (base.equals(ESP)) {
                if ((disp == 0) && noRelocation) {
                    emitByte(0x04 | (reg.getNumber() << 3));
                    emitByte(0x24);
                } else if (is8bit(disp) && noRelocation) {
                    emitByte(0x44 | (reg.getNumber() << 3));
                    emitByte(0x24);
                    emitByte(disp & 0xff);
                } else {
                    emitByte(0x84 | (reg.getNumber() << 3));
                    emitByte(0x24);
                    emitData(label, disp);
                }
            } else {
                if ((disp == 0) && noRelocation) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(!base.equals(EBP), "illegal addressing mode");
                    }
                    emitByte(0x00 | (reg.getNumber() << 3) | base.getNumber());
                } else if (is8bit(disp) && noRelocation) {
                    emitByte(0x40 | (reg.getNumber() << 3) | base.getNumber());
                    emitByte(disp & 0xff);
                } else {
                    emitByte(0x80 | (reg.getNumber() << 3) | base.getNumber());
                    emitData(label, disp);
                }
            }
        } else {
            if (index.isValid()) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(scale != NO_SCALE, "inconsistent address");
                    Assert.that(!index.equals(ESP), "illegal addressing mode");
                }
                emitByte(0x04 | (reg.getNumber() << 3));
                emitByte((scale << 6) | (index.getNumber() << 3) | 0x05);
                emitData(label, disp);
            } else {
                emitByte(0x05 | (reg.getNumber() << 3));
                emitData(label, disp);
            }
        }
    }

    /**
     * Emits the instruction part that specifies the operands.
     *
     * @param  reg  the register operand
     * @param  adr  the address part
     */
    protected void emitOperand(Register reg, Address adr) {
        emitOperand(reg, adr.getBase(), adr.getIndex(), adr.getScale(), adr.getLabel(), adr.getDisp());
    }

    /**
     * Emits the specified floating-point arithmetic instruction.
     *
     * @param  op1  primary operation code
     * @param  op2  extension of the operation code
     * @param  i    floating-point register stack offset
      */
    protected void emitFarith(int op1, int op2, int i) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isByte(op1) && isByte(op2), "wrong operation code");
            Assert.that((i >= 0) && (i < 8), "illegal stack offset");
        }
        emitByte(op1);
        emitByte(op2 + i);
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
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void adcl(Register dst, int imm32) {
        emitArith(0x81, 0xd0, dst, imm32);
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void adcl(Register dst, Register src) {
        emitArith(0x13, 0xc0, dst, src);
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void adcl(Register dst, Address src) {
        emitByte(0x13);
        emitOperand(dst, src);
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void addl(Register dst, int imm32) {
        emitArith(0x81, 0xc0, dst, imm32);
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst    destination operand address
     * @param  imm32  source operand 32-bit immediate
     */
    public void addl(Address dst, int imm32) {
        if (is8bit(imm32)) {
            emitByte(0x83);
            emitOperand(EAX, dst);
            emitByte(imm32 & 0xff);
        } else {
            emitByte(0x81);
            emitOperand(EAX, dst);
            emitInt(imm32);
        }
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void addl(Address dst, Register src) {
        emitByte(0x01);
        emitOperand(src, dst);
    }

    /**
     * This instruction adds the destination operand and the source operand and
     * stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void addl(Register dst, Register src) {
        emitArith(0x03, 0xc0, dst, src);
    }

    /**
     * This instruction adds the destination operand, the source operand, and
     * the carry flag and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void addl(Register dst, Address src) {
        emitByte(0x03);
        emitOperand(dst, src);
    }

    /**
     * This instruction performs a bitwise AND operation on the destination and
     * source operands and stores the result in the destination operand
     * location.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void andl(Register dst, int imm32) {
        emitArith(0x81, 0xe0, dst, imm32);
    }

    /**
     * This instruction performs a bitwise AND operation on the destination and
     * source operands and stores the result in the destination operand
     * location.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void andl(Register dst, Register src) {
        emitArith(0x23, 0xc0, dst, src);
    }

    /**
     * This instruction performs a bitwise AND operation on the destination and
     * source operands and stores the result in the destination operand
     * location.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void andl(Register dst, Address src) {
        emitByte(0x23);
        emitOperand(dst, src);
    }

    /**
     * This instruction reverses the byte order of the destination register.
     * This instruction is provided for converting little-endian values to
     * big-endian format and vice versa.
     *
     * @param  reg  destination register
     */
    public void bswap(Register reg) {
        emitByte(0x0f);
        emitByte(0xc8 | reg.getNumber());
    }

    /**
     * This instruction saves procedure linking information on the stack and
     * branches to the procedure specified with the destination operand.
     *
     * @param  label  destination operand label
     */
    public void call(ALabel label) {
        jcc(CALL, label);
    }

    /**
     * This instruction saves procedure linking information on the stack and
     * branches to the procedure, whose first instruction has the specified
     * address.
     *
     * @param  adr  destination address
     */
    public void call(Address adr) {
        emitByte(0xff);
        emitOperand(EDX, adr);
    }

    /**
     * This instruction saves procedure linking information on the stack and
     * branches to the procedure specified with the destination operand.
     *
     * @param   dst  destination operand register
     */
    public void call(Register dst) {
        emitByte(0xff);
        emitByte(0xd0 | dst.getNumber());
    }

    /**
     * This instruction saves procedure linking information on the stack and
     * branches to the procedure specified with the destination operand.
     *
     * @param   dst  destination operand address
     */
    public void call(int dst) {
        relocs = new CallRelocator(1, dst, code.getCodePos(), relocs);
        emitByte(0xe8);
        emitInt(0xDEADBEEF);
    }

    /**
     * Special version of the above where the target address is to be fixed
     * up by the linker.
     *
     * @return the offset into the code buffer where the offset to the destination must be written
     */
    public int call() {
        relocs = new CallRelocator(1, 0, code.getCodePos(), relocs);
        emitByte(0xe8);
        emitInt(0);
        return getOffset() - 4;
    }

    /**
     * This instruction doubles the size of the operand in the EAX register by
     * means of sign extension and stores the result in the EDX:EAX registers.
     */
    public void cdql() {
        emitByte(0x99);
    }

    /**
     * This instruction checks the state of one or more of the status flags and
     * performs a move operation if the flags are in a specified state.
     *
     * @param  cc   condition code
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void cmovl(int cc, Register dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(supportsCMOV(), "instruction not supported");
        }
        emitByte(0x0f);
        emitByte(0x40 | cc);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void cmpl(Register dst, int imm32) {
        emitArith(0x81, 0xf8, dst, imm32);
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst    destination operand address
     * @param  imm32  source operand 32-bit immediate
     */
    public void cmpl(Address dst, int imm32) {
        if (is8bit(imm32)) {
            emitByte(0x83);
            emitOperand(EDI, dst);
            emitByte(imm32 & 0xff);
        } else {
            emitByte(0x81);
            emitOperand(EDI, dst);
            emitInt(imm32);
        }
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void cmpl(Register dst, Register src) {
        emitArith(0x3b, 0xc0, dst, src);
    }

    /**
     * This instruction compares the destination operand with the source operand
     * and sets the status flags in the EFLAGS register according to the
     * results.
     *
     * @param  dst    destination operand register
     * @param  src    source address
     */
    public void cmpl(Register dst, Address src) {
        emitByte(0x3b);
        emitOperand(dst, src);
    }

    /**
     * This instruction compares the value in the EAX register with the
     * destination operand. If the two values are equal, the source operand is
     * loaded into the destination operand. Otherwise, the destination operand
     * is loaded into the EAX register.
     *
     * @param  reg  source operand register
     * @param  adr  destination operand address
     */
    public void cmpxchg(Register reg, Address adr) {
        emitByte(0x0f);
        emitByte(0xb1);
        emitOperand(reg, adr);
    }

    /**
     * This instruction subtracts one from the destination operand, while
     * preserving the state of the CF flag.
     *
     * @param  dst  destination operand register
     */
    public void decb(Register dst) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(dst.hasByteRegister(), "must have byte register");
        }
        emitByte(0xfe);
        emitByte(0xc8 | dst.getNumber());
    }

    /**
     * This instruction subtracts one from the destination operand, while
     * preserving the state of the CF flag.
     *
     * @param  dst  destination operand register
     */
    public void decl(Register dst) {
        emitByte(0x48 | dst.getNumber());
    }

    /**
     * This instruction subtracts one from the destination operand, while
     * preserving the state of the CF flag.
     *
     * @param  dst  destination operand address
     */
    public void decl(Address dst) {
        emitByte(0xff);
        emitOperand(ECX, dst);
    }

    /**
     * This instruction clears the sign bit of the ST(0) register to create the
     * absolute value of the operand.
     */
    public void fabs() {
        emitByte(0xd9);
        emitByte(0xe1);
    }

    /**
     * This instruction adds the single-real source operand to the ST(0)
     * register and stores the sum in ST(0).
     *
     * @param  src  source operand address
     */
    public void fadds(Address src) {
        emitByte(0xd8);
        emitOperand(EAX, src);
    }

    /**
     * This instruction adds the double-real source operand to the ST(0)
     * register and stores the sum in ST(0).
     *
     * @param  src  source operand address
     */
    public void faddd(Address src) {
        emitByte(0xdc);
        emitOperand(EAX, src);
    }

    /**
     * This instruction adds the contents of the ST(0) register to the ST(i)
     * register and stores the sum in ST(0).
     *
     * @param  i  source operand index
     */
    public void fadd(int i) {
        emitFarith(0xd8, 0xc0, i);
    }

    /**
     * This instruction adds the contents of the ST(0) register to the ST(i)
     * register, stores the sum in ST(i) and pops the register stack.
     *
     * @param  i  source operand index
     */
    public void faddp(int i) {
        emitFarith(0xde, 0xc0, i);
    }

    /**
     * This instruction complements the sign bit of the ST(0) register, that is
     * changes a positive value into a negative value of equal magnitude or vice
     * versa.
     */
    public void fchs() {
        emitByte(0xd9);
        emitByte(0xe0);
    }

    /**
     * This instruction compares the value of the ST(0) register with the
     * single-real source operand, sets the condition code flags in the FPU
     * status word according to the results and pops the register stack.
     *
     * @param  src  source operand address
     */
    public void fcomps(Address src) {
        emitByte(0xd8);
        emitOperand(EBX, src);
    }

    /**
     * This instruction compares the value of the ST(0) register with the
     * double-real source operand, sets the condition code flags in the FPU
     * status word according to the results and pops the register stack.
     *
     * @param  src  source operand address
     */
    public void fcompd(Address src) {
        emitByte(0xdc);
        emitOperand(EBX, src);
    }

    /**
     * This instruction compares the value of the ST(0) register with the value
     * of the ST(1) register, sets the condition code flags in the FPU status
     * word according to the results and pops the register stack twice.
     */
    public void fcompp() {
        emitByte(0xde);
        emitByte(0xd9);
    }

    /**
     * This instruction calculates the cosine of the source operand in the ST(0)
     * register and stores the result in ST(0).
     */
    public void fcos() {
        emitByte(0xd9);
        emitByte(0xff);
    }

    /**
     * This instruction subtracts one from the TOP field of the FPU status word,
     * that is decrements the top-of-stack pointer.
     */
    public void fdecstp() {
        emitByte(0xd9);
        emitByte(0xf6);
    }

    /**
     * This instruction divides the value of the ST(0) register by the
     * single-real source operand and stores the result in ST(0).
     *
     * @param  src  source operand address
     */
    public void fdivs(Address src) {
        emitByte(0xd8);
        emitOperand(ESI, src);
    }

    /**
     * This instruction divides the value of the ST(0) register by the
     * double-real source operand and stores the result in ST(0).
     *
     * @param  src  source operand address
     */
    public void fdivd(Address src) {
        emitByte(0xdc);
        emitOperand(ESI, src);
    }

    /**
     * This instruction divides the value of the ST(0) register by the value of
     * the ST(i) register and stores the result in ST(0).
     *
     * @param  i  source operand index
     */
    public void fdiv(int i) {
        emitFarith(0xd8, 0xf0, i);
    }

    /**
     * This instruction divides the value of the ST(0) register by the value of
     * the ST(i) register, stores the result in ST(i) and pops the register
     * stack.
     *
     * @param  i  source operand index
     */
    public void fdivp(int i) {
        emitFarith(0xde, 0xf8, i);
    }

    /**
     * This instruction divides the single-real source operand by the value of
     * the ST(0) register and stores the result in ST(0).
     *
     * @param  src  source operand address
     */
    public void fdivrs(Address src) {
        emitByte(0xd8);
        emitOperand(EDI, src);
    }

    /**
     * This instruction divides the double-real source operand by value of the
     * ST(0) register and stores the result in ST(0).
     *
     * @param  src  source operand address
     */
    public void fdivrd(Address src) {
        emitByte(0xdc);
        emitOperand(EDI, src);
    }

    /**
     * This instruction divides the value of the ST(i) register by the value of
     * the ST(0) register, stores the result in ST(i) and pops the register
     * stack.
     *
     * @param  i  source operand index
     */
    public void fdivrp(int i) {
        emitFarith(0xde, 0xf0, i);
    }

    /**
     * This instruction sets the tag in the FPU tag register associated with the
     * register ST(i) to empty. The contents of the register and the FPU
     * top-of-stack pointer are not affected.
     *
     * @param  i  index of the register
     */
    public void ffree(int i) {
        emitFarith(0xdd, 0xc0, i);
    }

    /**
     * This instruction converts the short signed integer source operand into
     * extended-real format and pushes the value onto the FPU register stack.
     *
     * @param  adr  source operand address
     */
    public void filds(Address adr) {
        emitByte(0xdb);
        emitOperand(EAX, adr);
    }

    /**
     * This instruction converts the long signed integer source operand into
     * extended-real format and pushes the value onto the FPU register stack.
     *
     * @param  adr  source operand address
     */
    public void fildd(Address adr) {
        emitByte(0xdf);
        emitOperand(EBP, adr);
    }

    /**
     * This instruction adds one to the TOP field of the FPU status word, that
     * is increments the top-of-stack pointer.
     */
    public void fincstp() {
        emitByte(0xd9);
        emitByte(0xf7);
    }

    /**
     * This instruction sets the FPU control, status, tag, instruction pointer,
     * and data pointer registers to their default states.
     */
    public void finit() {
        emitByte(0x9b);
        emitByte(0xdb);
        emitByte(0xe3);
    }

    /**
     * This instruction converts the value in the ST(0) register to a short
     * signed integer and stores the result in the destination operand.
     *
     * @param  adr  destination operand address
     */
    public void fists(Address adr) {
        emitByte(0xdb);
        emitOperand(EDX, adr);
    }

    /**
     * This instruction converts the value in the ST(0) register to a short
     * signed integer, stores the result in the destination operand and pops the
     * register stack.
     *
     * @param  adr  destination operand address
     */
    public void fistps(Address adr) {
        emitByte(0xdb);
        emitOperand(EBX, adr);
    }

    /**
     * This instruction converts the value in the ST(0) register to a long
     * signed integer, stores the result in the destination operand and pops the
     * register stack.
     *
     * @param  adr  destination operand address
     */
    public void fistpd(Address adr) {
        emitByte(0xdf);
        emitOperand(EDI, adr);
    }

    /**
     * This instruction converts the single-real source operand to the
     * extended-real format and pushes the value onto the FPU register stack.
     *
     * @param adr source operand address
     */
    public void flds(Address adr) {
        emitByte(0xd9);
        emitOperand(EAX, adr);
    }

    /**
     * This instruction converts the double-real source operand to the
     * extended-real format and pushes the value onto the FPU register stack.
     *
     * @param adr source operand address
     */
    public void fldd(Address adr) {
        emitByte(0xdd);
        emitOperand(EAX, adr);
    }

    /**
     * This instruction pushes the value in the ST(i) register onto the stack.
     *
     * @param  i  source operand index
     */
    public void flds(int i) {
        emitFarith(0xd9, 0xc0, i);
    }

    /**
     * This instruction pushes 1.0 onto the FPU register stack.
     */
    public void fld1() {
        emitByte(0xd9);
        emitByte(0xe8);
    }

    /**
     * This instruction pushes 0.0 onto the FPU register stack.
     */
    public void fldz() {
        emitByte(0xd9);
        emitByte(0xee);
    }

    /**
     * This instruction loads the 16-bit source operand into the FPU control
     * word.
     *
     * @param  src  source operand address
     */
    public void fldcw(Address src) {
        emitByte(0xd9);
        emitOperand(EBP, src);
    }

    /**
     * This instruction multiplies the value of the ST(0) register by the
     * single-real source operand and stores the product in ST(0).
     *
     * @param  src  source operand address
     */
    public void fmuls(Address src) {
        emitByte(0xd8);
        emitOperand(ECX, src);
    }

    /**
     * This instruction multiplies the value of the ST(0) register by the
     * double-real source operand and stores the product in ST(0).
     *
     * @param  src  source operand address
     */
    public void fmuld(Address src) {
        emitByte(0xdc);
        emitOperand(ECX, src);
    }

    /**
     * This instruction multiplies the value of the ST(0) register by the value
     * of the ST(i) register and stores the product in ST(0).
     *
     * @param  i  source operand index
     */
    public void fmul(int i) {
        emitFarith(0xd8, 0xc8, i);
    }

    /**
     * This instruction multiplies the value of the ST(0) register by the value
     * of the ST(i) register, stores the product in ST(i) and pops the register
     * stack.
     *
     * @param  i  source operand index
     */
    public void fmulp(int i) {
        emitFarith(0xde, 0xc8, i);
    }

    /**
     * This instruction stores the current FPU state at the specified
     * destination in memory without checking for pending unmasked
     * floating-point exceptions, and then re-initializes the FPU.
     *
     * @param  dst  destination address
     */
    public void fnsave(Address dst) {
        emitByte(0xdd);
        emitOperand(ESI, dst);
    }

    /**
     * This instruction stores the current value of the FPU control word at the
     * specified destination in memory after checking for pending unmasked
     * floating-point exceptions.
     *
     * @param  dst  destination address
     */
    public void fstcw(Address dst) {
        emitByte(0x9b);
        emitByte(0xd9);
        emitOperand(EDI, dst);
    }

    /**
     * This instruction stores the current value of the FPU status word in the
     * AX register without checking for pending unmasked floating-point
     * exceptions. It is used primarily in conditional branching, where the
     * direction of the branch depends on the state of the FPU condition code
     * flags.
     */
    public void fnstswax() {
        emitByte(0xdf);
        emitByte(0xe0);
    }

    /**
     * This instruction computes the partial remainder obtained from dividing
     * the value in the ST(0) register by the value in the ST(1) register and
     * stores the result in ST(0).
     */
    public void fprem() {
        emitByte(0xd9);
        emitByte(0xf8);
    }

    /**
     * This instruction computes the partial IEEE remainder obtained from
     * dividing the value in the ST(0) register by the value in the ST(1)
     * register and stores the result in ST(0).
     */
    public void fprem1() {
        emitByte(0xd9);
        emitByte(0xf5);
    }

    /**
     * This instruction loads the FPU state from the memory area specified by
     * the source operand.
     *
     * @param  src  source operand address
     */
    public void frstor(Address src) {
        emitByte(0xdd);
        emitOperand(ESP, src);
    }

    /**
     * This instruction calculates the sine of the source operand in the ST(0)
     * register and stores the result in ST(0).
     */
    public void fsin() {
        emitByte(0xd9);
        emitByte(0xfe);
    }

    /**
     * This instruction calculates the square root of the source value in the
     * ST(0) register and stores the result in ST(0).
     */
    public void fsqrt() {
        emitByte(0xD9);
        emitByte(0xfa);
    }

    /**
     * This instruction copies the value in the ST(0) register to the
     * destination address after converting it to single-real format.
     *
     * @param  adr  destination address
     */
    public void fsts(Address adr) {
        emitByte(0xD9);
        emitOperand(EDX, adr);
    }

    /**
     * This instruction copies the value in the ST(0) register to the
     * destination address after converting it to double-real format.
     *
     * @param  adr  destination address
     */
    public void fstd(Address adr) {
        emitByte(0xdd);
        emitOperand(EDX, adr);
    }

    /**
     * This instruction copies the value in the ST(0) register to the
     * destination address after converting it to single-real format and pops
     * the register stack.
     *
     * @param  adr  destination address
     */
    public void fstps(Address adr) {
        emitByte(0xd9);
        emitOperand(EBX, adr);
    }

    /**
     * This instruction copies the value in the ST(0) register to the
     * destination address after converting it to double-real format and pops
     * the register stack.
     *
     * @param  adr  destination address
     */
    public void fstpd(Address adr) {
        emitByte(0xdd);
        emitOperand(EBX, adr);
    }

    /**
     * This instruction copies the value in the ST(0) register to the ST(i)
     * register and pops the register stack.
     *
     * @param  i  destination operand index
     */
    public void fstpd(int i) {
        emitFarith(0xdd, 0xd8, i);
    }

    /**
     * This instruction subtracts the single-real source operand from the value
     * of the ST(0) register and stores the difference in ST(0).
     *
     * @param  src  source operand address
     */
    public void fsubs(Address src) {
        emitByte(0xd8);
        emitOperand(ESP, src);
    }

    /**
     * This instruction subtracts the double-real source operand from the value
     * of the ST(0) register and stores the difference in ST(0).
     *
     * @param  src  source operand address
     */
    public void fsubd(Address src) {
        emitByte(0xdc);
        emitOperand(ESP, src);
    }

    /**
     * This instruction subtracts the value of the ST(i) register from the value
     * of the ST(0) register and stores the difference in ST(0).
     *
     * @param  i  source operand index
     */
    public void fsub(int i) {
        emitFarith(0xd8, 0xe0, i);
    }

    /**
     * This instruction subtracts the value of the ST(i) register from the value
     * of the ST(0) register, stores the difference in ST(i) and pops the
     * register stack.
     *
     * @param  i  source operand index
     */
    public void fsubp(int i) {
        emitFarith(0xde, 0xe8, i);
    }

    /**
     * This instruction subtracts the value of the ST(0) register from the
     * single-real source operand and stores the difference in ST(0).
     *
     * @param  src  source operand address
     */
    public void fsubrs(Address src) {
        emitByte(0xd8);
        emitOperand(EBP, src);
    }

    /**
     * This instruction subtracts the value of the ST(0) register from the
     * double-real source operand and stores the difference in ST(0).
     *
     * @param  src  source operand address
     */
    public void fsubrd(Address src) {
        emitByte(0xdc);
        emitOperand(EBP, src);
    }

    /**
     * This instruction subtracts the value of the ST(0) register from the value
     * of the ST(i) register, stores the difference in ST(i) and pops the
     * register stack.
     *
     * @param  i  source operand index
     */
    public void fsubrp(int i) {
        emitFarith(0xde, 0xe0, i);
    }

    /**
     * This instruction compares the value in the ST(0) register with 0.0 and
     * sets the condition code flags in the FPU status word according to the
     * results.
     */
    public void ftst() {
        emitByte(0xd9);
        emitByte(0xe4);
    }

    /**
     * This instruction performs an unordered comparison of the contents of the
     * registers ST(0) and ST(i) and sets condition code flags according to the
     * results.
     *
     * @param  i  source operand index
     */
    public void fucomi(int i) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(supportsCMOV(), "instruction not supported");
        }
        emitFarith(0xdb, 0xe8, i);
    }

    /**
     * This instruction performs an unordered comparison of the contents of the
     * registers ST(0) and ST(i), sets condition code flags according to the
     * results and pops the register stack.
     *
     * @param  i  source operand index
     */
    public void fucomip(int i) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(supportsCMOV(), "instruction not supported");
        }
        emitFarith(0xdf, 0xe8, i);
    }

    /**
     * This instruction causes the processor to check for and handle pending,
     * unmasked, floating-point exceptions before proceeding.
     */
    public void fwait() {
        emitByte(0x9b);
    }

    /**
     * This instruction exchanges the contents of the ST(0) register and the
     * ST(i) register.
     *
     * @param  i  index of the register
     */
    public void fxch(int i) {
        emitFarith(0xd9, 0xc8, i);
    }

    /**
     * This instruction stops instruction execution and places the processor in
     * a HALT state.
     */
    public void hlt() {
        emitByte(0xf4);
    }

    /**
     * This instruction performs a signed division of the value in the EAX
     * register by the source operand and stores the quotient rounded toward
     * zero in the EAX register and the remainder in the EDX register.
     *
     * @param  src  source operand register
     */
    public void idivl(Register src) {
        emitByte(0xf7);
        emitByte(0xf8 | src.getNumber());
    }

    /**
     * This instruction performs a signed multiplication of the destination
     * operand and the source operand and stores the product in the destination
     * operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void imull(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xaf);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction multiplies the first source operand by the second source
     * operand and stores the product in the destination operand.
     *
     * @param  dst    destination operand register
     * @param  src    first source operand register
     * @param  value  second source operand value
     */
    public void imull(Register dst, Register src, int value) {
        if (is8bit(value)) {
            emitByte(0x6b);
            emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
            emitByte(value);
        } else {
            emitByte(0x69);
            emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
            emitInt(value);
        }
    }

    /**
     * This instruction adds one to the destination operand, while preserving
     * the state of the CF flag.
     *
     * @param  dst  destination operand register
     */
    public void incl(Register dst) {
        emitByte(0x40 | dst.getNumber());
    }

    /**
     * This instruction adds one to the destination operand, while preserving
     * the state of the CF flag.
     *
     * @param  dst  destination operand address
     */
    public void incl(Address dst) {
        emitByte(0xff);
        emitOperand(EAX, dst);
    }

    /**
     * This instruction explicitly calls the breakpoint exception handler.
     */
    public void int3() {
        emitByte(0xcc);
    }

    /**
     * This instruction checks the state of one or more of the status flags and
     * performs a jump to the target instruction if the flags are in a specified
     * state.
     *
     * @param  cc      condition code
     * @param  label   destination operand label
     */
    public void jcc(int cc, ALabel label) {
        if (cc == CALL) {
            if (label.isBound()) {
                final int longSize = 5;
                int offset = label.getPos() - getOffset();
                emitByte(0xe8);
                emitInt(offset - longSize);
            } else {
                label.addJcc(getOffset(), cc);
                emitByte(0xe8);
                emitInt(0xDEADBEEF);
            }
        } else if (cc == JUMP) {
            if (label.isBound()) {
                final int shortSize = 2;
                final int longSize = 5;
                int offset = label.getPos() - getOffset();
                if (offset <= 0 && is8bit(offset - shortSize)) {
                    emitByte(0xeb);
                    emitByte((offset - shortSize) & 0xff);
                } else {
                    emitByte(0xe9);
                    emitInt(offset - longSize);
                }
            } else {
                label.addJcc(getOffset(), cc);
                emitByte(0xe9);
                emitInt(0xDEADBEEF);
            }
        } else {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that((cc >= 0) && (cc < 16), "illegal condition code");
            }
            if (label.isBound()) {
                final int shortSize = 2;
                final int longSize = 6;
                int offset = label.getPos() - getOffset();
                if (offset <= 0 && is8bit(offset - shortSize)) {
                    emitByte(0x70 | cc);
                    emitByte((offset - shortSize) & 0xff);
                } else {
                    emitByte(0x0f);
                    emitByte(0x80 | cc);
                    emitInt(offset - longSize);
                }
            } else {
                label.addJcc(getOffset(), cc);
                emitByte(0x0f);
                emitByte(0x80 | cc);
                emitInt(0xDEADBEEF);
            }
        }
    }

    /**
     * This instruction checks the state of one or more of the status flags and
     * performs a jump to the target instruction if the flags are in a specified
     * state.
     *
     * @param  cc      condition code
     * @param  dat     destination operand address
     */
    public void jcc(int cc, int dst) {
        if (cc == CALL) {
            call(dst);
        } else if (cc == JUMP) {
            jmp(dst);
        } else {
            relocs = new CallRelocator(2, dst, code.getCodePos(), relocs);
            emitByte(0x0f);
            emitByte(0x80 | cc);
            emitInt(0xDEADBEEF);
        }
    }

    /**
     * This instruction transfers program control to a different point in the
     * instruction stream without recording return information.
     *
     * @param  label  destination operand label
     */
    public void jmp(ALabel label) {
        jcc(JUMP, label);
    }

    /**
     * This instruction transfers program control to a different point in the
     * instruction stream without recording return information.
     *
     * @param  adr  destination operand address
     */
    public void jmp(Address adr) {
        emitByte(0xff);
        emitOperand(ESP, adr);
    }

    /**
     * This instruction transfers program control to a different point in the
     * instruction stream without recording return information.
     *
     * @param   reg  destination operand register
     */
    public void jmp(Register reg) {
        emitByte(0xff);
        emitByte(0xe0 | reg.getNumber());
    }


    /**
     * This instruction transfers program control to a different point in the
     * instruction stream without recording return information.
     *
     * @param   dst  destination operand address
     */
    public void jmp(int dst) {
        relocs = new CallRelocator(1, dst, code.getCodePos(), relocs);
        emitByte(0xe9);
        emitInt(0xDEADBEEF);
    }


    /**
     * This instruction computes the effective address of the source operand and
     * stores it in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void leal(Register dst, Address src) {
        emitByte(0x8d);
        emitOperand(dst, src);
    }

    /**
     * The LOCK prefix can be prepended to certain instructions to turn them
     * into atomic instructions.
     */
    public void lock() {
        emitByte(0xf0);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void movb(Address dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(src.hasByteRegister(), "must have byte register");
        }
        emitByte(0x88);
        emitOperand(src, dst);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void movw(Address dst, Register src) {
        emitByte(0x66);
        emitByte(0x89);
        emitOperand(src, dst);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void movl(Address dst, Register src) {
        emitByte(0x89);
        emitOperand(src, dst);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movb(Register dst, Address src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(dst.hasByteRegister(), "must have byte register");
        }
        emitByte(0x8a);
        emitOperand(dst, src);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movw(Register dst, Address src) {
        emitByte(0x66);
        emitByte(0x8b);
        emitOperand(dst, src);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movl(Register dst, Register src) {
        emitByte(0x8b);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movl(Register dst, Address src) {
        emitByte(0x8b);
        emitOperand(dst, src);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void movl(Register dst, int imm32) {
        emitByte(0xb8 | dst.getNumber());
        emitInt(imm32);
    }

    /**
     * This is a special version of the above instruction where a linker
     * is going to fill in the source value later.
     *
     * @param  dst  destination operand register
     * @return      the offset to the int32 operant
     */
    public int movl(Register dst) {
        emitByte(0xb8 | dst.getNumber());
        emitInt(0);
        return getOffset() - 4;
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand register
     * @param  label  source operand 32-bit immediate
     */
    public void movl(Register dst, ALabel label) {
        emitByte(0xb8 | dst.getNumber());
        relocs = new Relocator(code.getCodePos(), relocs);
        label.addRelocator(relocs);
        emitInt(0xDEADBEEF);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  imm8  source operand 8-bit immediate
     */
    public void movb(Address dst, int imm8) {
        emitByte(0xc6);
        emitOperand(EAX, dst);
        emitByte(imm8);
    }

    /**
     * This instruction copies the source operand to the destination operand.
     *
     * @param  dst  destination operand address
     * @param  imm32  source operand 32-bit immediate
     */
    public void movl(Address dst, int imm32) {
        emitByte(0xc7);
        emitOperand(EAX, dst);
        emitInt(imm32);
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and sign extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movsxb(Register dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(src.hasByteRegister(), "must have byte register");
        }
        emitByte(0x0f);
        emitByte(0xbe);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and sign extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movsxb(Register dst, Address src) {
        emitByte(0x0f);
        emitByte(0xbe);
        emitOperand(dst, src);
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and sign extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movsxw(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xbf);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and sign extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movsxw(Register dst, Address src) {
        emitByte(0x0f);
        emitByte(0xbf);
        emitOperand(dst, src);
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and zero extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movzxb(Register dst, Register src) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(src.hasByteRegister(), "must have byte register");
        }
        emitByte(0x0f);
        emitByte(0xb6);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and zero extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movzxb(Register dst, Address src) {
        emitByte(0x0f);
        emitByte(0xb6);
        emitOperand(dst, src);
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and zero extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void movzxw(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xb7);
        emitByte(0xc0 | (dst.getNumber() << 3) | src.getNumber());
    }

    /**
     * This instruction copies the contents of the source operand to the
     * destination operand and zero extends the value to 32 bits.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void movzxw(Register dst, Address src) {
        emitByte(0x0f);
        emitByte(0xb7);
        emitOperand(dst, src);
    }

    /**
     * This instruction performs an unsigned multiplication of the EAX register
     * and the source operand and stores the result in the EDX:EAX registers.
     *
     * @param  src  source operand register
     */
    public void mull(Register src) {
        emitByte(0xf7);
        emitByte(0xe0 | src.getNumber());
    }

    /**
     * This instruction performs an unsigned multiplication of the EAX register
     * and the source operand and stores the result in the EDX:EAX registers.
     *
     * @param  src  source operand address
     */
    public void mull(Address src) {
        emitByte(0xf7);
        emitOperand(ESP, src);
    }

    /**
     * This instruction replaces the value of the destination operand with its
     * two's complement, that is subtracts the operand from 0.
     *
     * @param  dst  destination operand register
     */
    public void negl(Register dst) {
        emitByte(0xf7);
        emitByte(0xd8 | dst.getNumber());
    }

    /**
     * This instruction performs no operation. It is a one-byte instruction that
     * takes up space in the instruction stream but does not affect the machine
     * context, except the EIP register.
     */
    public void nop() {
        emitByte(0x90);
    }

    /**
     * This instruction performs a bitwise NOT operation on the destination
     * operand and stores the result in the destination operand location.
     *
     * @param  dst  destination operand register
     */
    public void notl(Register dst) {
        emitByte(0xf7);
        emitByte(0xd0 | dst.getNumber());
    }

    /**
     * This instruction performs a bitwise inclusive OR operation between the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst  destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void orl(Register dst, int imm32) {
        emitArith(0x81, 0xc8, dst, imm32);
    }

    /**
     * This instruction performs a bitwise inclusive OR operation between the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void orl(Register dst, Register src) {
        emitArith(0x0b, 0xc0, dst, src);
    }

    /**
     * This instruction performs a bitwise inclusive OR operation between the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void orl(Register dst, Address src) {
        emitByte(0x0b);
        emitOperand(dst, src);
    }

    /**
     * This instruction loads the value from the top of the stack to the
     * location specified with the destination operand and then increments the
     * stack pointer.
     *
     * @param  dst  destination operand register
     */
    public void popl(Register dst) {
        emitByte(0x58 | dst.getNumber());
    }

    /**
     * This instruction loads the value from the top of the stack to the
     * location specified with the destination operand and then increments the
     * stack pointer.
     *
     * @param  dst  destination operand address
     */
    public void popl(Address dst) {
        emitByte(0x8f);
        emitOperand(EAX, dst);
    }

    /**
     * This instruction pops doublewords from the stack into the general-purpose
     * registers EDI, ESI, EBP, EBX, EDX, ECX, and EAX.
     */
    public void popad() {
        emitByte(0x61);
    }

    /**
     * This instruction pops a doubleword from the top of the stack and stores
     * the value in the EFLAGS register.
     */
    public void popfd() {
        emitByte(0x9d);
    }

    /**
     * Emits the specified prefix byte.
     *
     * @param  prefix  prefix byte
     */
    public void prefix(int prefix) {
        emitByte(prefix);
    }

    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  src  source operand address
     */
    public void pushl(Address src) {
        emitByte(0xFF);
        emitOperand(ESI, src);
    }

    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  src  source operand register
     */
    public void pushl(Register src) {
        emitByte(0x50 | src.getNumber());
    }

    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  imm32  source operand 32-bit immediate
     */
    public void pushl(int imm32) {
        emitByte(0x68);
        emitInt(imm32);
    }


    /**
     * This instruction decrements the stack pointer and then stores the source
     * operand on the top of the stack.
     *
     * @param  label  source operand 32-bit immediate
     */
    public void pushl(ALabel label) {
        emitByte(0x68);
        relocs = new Relocator(code.getCodePos(), relocs);
        label.addRelocator(relocs);
        emitInt(0xDEADBEEF);
    }


    /**
     * This instruction pushes the contents of the general-purpose registers
     * EAX, ECX, EDX, EBX, EBP, ESP, EBP, ESI, and EDI onto the stack.
     */
    public void pushad() {
        emitByte(0x60);
    }

    /**
     * This instruction decrements the stack pointer by four and pushes the
     * entire contents of the EFLAGS register onto the stack.
     */
    public void pushfd() {
        emitByte(0x9c);
    }

    /**
     * This instruction rotates 33 bits of the destination operand the specified
     * number of bit positions and stores the result in the destination operand.
     *
     * @param  dst   destination operand register
     * @param  imm8  8-bit shift count
     */
    public void rcll(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isShiftCount(imm8), "illegal shift count");
        }
        if (imm8 == 1) {
            emitByte(0xd1);
            emitByte(0xd0 | dst.getNumber());
        } else {
            emitByte(0xc1);
            emitByte(0xd0 | dst.getNumber());
            emitByte(imm8);
        }
    }

    /**
     * This instruction moves doublewords from DS:ESI to ES:EDI the number of
     * times specified in the count register ECX.
     */
    public void repmovs() {
        emitByte(0xf3);
        emitByte(0xa5);
    }

    /**
     * This instruction fills doublewords at ES:EDI with the value of the EAX
     * register the number of times specified in the count register ECX.
     */
    public void repstos() {
        emitByte(0xf3);
        emitByte(0xab);
    }

    /**
     * This instruction transfers program control to a return address located on
     * the top of the stack and pops the specified number of bytes from the
     * stack.
     *
     * @param  imm16  16-bit number of bytes to pop
     */
    public void ret(int imm16) {
        if (imm16 == 0) {
            emitByte(0xc3);
        } else {
            emitByte(0xc2);
            emitShort(imm16);
        }
    }

    /**
     * This instruction loads the SF, ZF, AF, PF, and CF flags of the EFLAGS
     * register with the values from the bits 7, 6, 4, 2, and 0 in the AH
     * register.
     */
    public void sahf() {
        emitByte(0x9e);
    }

    /**
     * This instruction performs a signed division of the destination operand by
     * two multiple times and rounds toward negative infinity. That is it shifts
     * the operand to the right by the specified number of bits and sets or
     * clears the most significant bits depending on the sign of the original
     * value.
     *
     * @param  dst   destination operand register
     * @param  imm8  8-bit shift count
     */
    public void sarl(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isShiftCount(imm8), "illegal shift count");
        }
        if (imm8 == 1) {
            emitByte(0xd1);
            emitByte(0xf8 | dst.getNumber());
        } else {
            emitByte(0xc1);
            emitByte(0xf8 | dst.getNumber());
            emitByte(imm8);
        }
    }

    /**
     * This instruction performs a signed division of the destination operand by
     * two multiple times and rounds toward negative infinity. That is it shifts
     * the operand to the right by the number of bits specified in the count
     * register CL and sets or clears the most significant bits depending on the
     * sign of the original value.
     *
     * @param  dst  destination operand register
     */
    public void sarl(Register dst) {
        emitByte(0xd3);
        emitByte(0xf8 | dst.getNumber());
    }

    /**
     * This instruction performs a multiplication of the destination operand by
     * two multiple times, that is shifts the operand to the left by the
     * specified number of bits.
     *
     * @param  dst   destination operand register
     * @param  imm8  8-bit shift count
     */
    public void shll(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isShiftCount(imm8), "illegal shift count");
        }
        if (imm8 == 1) {
            emitByte(0xd1);
            emitByte(0xe0 | dst.getNumber());
        } else {
            emitByte(0xc1);
            emitByte(0xe0 | dst.getNumber());
            emitByte(imm8);
        }
    }

    /**
     * This instruction performs a multiplication of the destination operand by
     * two multiple times, that is shifts the operand to the left by the number
     * of bits specified in the count register CL.
     *
     * @param  dst  destination operand register
     */
    public void shll(Register dst) {
        emitByte(0xd3);
        emitByte(0xe0 | dst.getNumber());
    }

    /**
     * This instruction performs an unsigned division of the destination operand
     * by two multiple times, that is shifts the operand to the right by the
     * specified number of bits and clears the most significant bits.
     *
     * @param  dst   destination operand register
     * @param  imm8  8-bit shift count
     */
    public void shrl(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isShiftCount(imm8), "illegal shift count");
        }
        if (imm8 == 1) {
            emitByte(0xd1);
            emitByte(0xe8 | dst.getNumber());
        } else {
            emitByte(0xc1);
            emitByte(0xe8 | dst.getNumber());
            emitByte(imm8);
        }
    }

    /**
     * This instruction performs an unsigned division of the destination operand
     * by two multiple times, that is shifts the operand to the right by the
     * number of bits specified in the count register CL and clears the most
     * significant bits.
     *
     * @param  dst   destination operand register
     */
    public void shrl(Register dst) {
        emitByte(0xd3);
        emitByte(0xe8 | dst.getNumber());
    }

    /**
     * This instruction adds the source operand and the carry flag, subtracts
     * the result from the destination operand and stores the difference in the
     * destination operand.
     *
     * @param  dst  destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void sbbl(Register dst, int imm32) {
        emitArith(0x81, 0xd8, dst, imm32);
    }

    /**
     * This instruction adds the source operand and the carry flag, subtracts
     * the result from the destination operand and stores the difference in the
     * destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void sbbl(Register dst, Register src) {
        emitArith(0x1b, 0xc0, dst, src);
    }

    /**
     * This instruction adds the source operand and the carry flag, subtracts
     * the result from the destination operand and stores the difference in the
     * destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void sbbl(Register dst, Address src) {
        emitByte(0x1b);
        emitOperand(dst, src);
    }

    /**
     * This instruction sets the destination operand to 0 or 1 depending on the
     * settings of the status flags CF, SF, OF, ZF, and PF in the EFLAGS
     * register.
     *
     * @param  cc   condition being tested for
     * @param  dst  destination operand register
     */
     public void setb(int cc, Register dst) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((cc >= 0) && (cc < 16), "illegal condition code");
        }
        emitByte(0x0f);
        emitByte(0x90 | cc);
        emitByte(0xc0 | dst.getNumber());
    }

    /**
     * This instruction shifts the destination operand to the left the number
     * of bits specified in the count register CL. The source operand provides
     * bits to shift in from the right.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void shldl(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xa5);
        emitByte(0xc0 | (src.getNumber() << 3) | dst.getNumber());
    }

    /**
     * This instruction shifts the destination operand to the right the number
     * of bits specified in the count register CL. The source operand provides
     * bits to shift in from the left.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void shrdl(Register dst, Register src) {
        emitByte(0x0f);
        emitByte(0xad);
        emitByte(0xc0 | (src.getNumber() << 3) | dst.getNumber());
    }

    /**
     * This instruction subtracts the source operand from the destination
     * operand and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void subl(Register dst, int imm32) {
        emitArith(0x81, 0xe8, dst, imm32);
    }

    /**
     * This instruction subtracts the source operand from the destination
     * operand and stores the result in the destination operand.
     *
     * @param  dst  destination operand address
     * @param  imm32  source operand 32-bit immediate
     */
    public void subl(Address dst, int imm32) {
        if (is8bit(imm32)) {
            emitByte(0x83);
            emitOperand(EBP, dst);
            emitByte(imm32 & 0xff);
        } else {
            emitByte(0x81);
            emitOperand(EBP, dst);
            emitInt(imm32);
        }
    }

    /**
     * This instruction subtracts the source operand from the destination
     * operand and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand 32-bit register
     */
    public void subl(Register dst, Register src) {
        emitArith(0x2b, 0xc0, dst, src);
    }

    /**
     * This instruction subtracts the source operand from the destination
     * operand and stores the result in the destination operand.
     *
     * @param  dst  destination operand register
     * @param  src  source operand address
     */
    public void subl(Register dst, Address src) {
        emitByte(0x2b);
        emitOperand(dst, src);
    }

    /**
     * This instruction computes the bitwise logical AND of the destination
     * operand and the source operand and sets the SF, ZF, and PF status flags
     * according to the result.
     *
     * @param  dst   destination operand register
     * @param  imm8  source operand 8-bit immediate
     */
    public void testb(Register dst, int imm8) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(dst.hasByteRegister(), "must have byte register");
        }
        emitArithByte(0xf6, 0xc0, dst, imm8);
    }

    /**
     * This instruction computes the bitwise logical AND of the destination
     * operand and the source operand and sets the SF, ZF, and PF status flags
     * according to the result.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void testl(Register dst, int imm32) {
        if (dst.getNumber() == 0) {
            emitByte(0xa9);
        } else {
            emitByte(0xf7);
            emitByte(0xc0 | dst.getNumber());
        }
        emitInt(imm32);
    }

    /**
     * This instruction computes the bitwise logical AND of the destination
     * operand and the source operand and sets the SF, ZF, and PF status flags
     * according to the result.
     *
     * @param  dst  destination operand register
     * @param  src  source operand register
     */
    public void testl(Register dst, Register src) {
        emitArith(0x85, 0xc0, dst, src);
    }

    /**
     * This instruction exchanges the destination operand with the source
     * operand and then loads the sum of the two values into the destination
     * operand.
     *
     * @param  dst  destination operand address
     * @param  src  source operand register
     */
    public void xaddl(Address dst, Register src) {
        emitByte(0x0f);
        emitByte(0xc1);
        emitOperand(src, dst);
    }

    /**
     * This instruction exchanges the contents of the destination and source
     * operand.
     *
     * @param  reg  destination operand register
     * @param  adr  source operand address
     */
    public void xchg(Register reg, Address adr) {
        emitByte(0x87);
        emitOperand(reg, adr);
    }

    /**
     * This instruction performs a bitwise exclusive OR operation on the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst    destination operand register
     * @param  imm32  source operand 32-bit immediate
     */
    public void xorl(Register dst, int imm32) {
        emitArith(0x81, 0xf0, dst, imm32);
    }

    /**
     * This instruction performs a bitwise exclusive OR operation on the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst    destination operand register
     * @param  src    source operand register
     */
    public void xorl(Register dst, Register src) {
        emitArith(0x33, 0xc0, dst, src);
    }

    /**
     * This instruction performs a bitwise exclusive OR operation on the
     * destination and source operand and stores the result in the destination
     * operand location.
     *
     * @param  dst    destination operand register
     * @param  src    source operand address
     */
    public void xorl(Register dst, Address src) {
        emitByte(0x33);
        emitOperand(dst, src);
    }
}
