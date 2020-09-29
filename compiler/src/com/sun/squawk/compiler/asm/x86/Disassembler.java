/*
 * @(#)Disassembler.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.x86;
import com.sun.squawk.util.Hashtable;
import com.sun.squawk.compiler.asm.*;

/**
 * Prints the generated machine code together with the corresponding assembly
 * instructions.
 *
 * @see      Assembler
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Disassembler {
    /**
     * An operand mode constant.
     */
    private static final int
            R8    =  0, R32   =  1, MEM   =  2, RM8   =  3, RM16  =  4,
            RM32  =  5, RM64  =  6, IMM8  =  7, IMM16 =  8, IMM32 =  9,
            REL8  = 10, REL32 = 11;

    /**
     * The names of the byte general-purpose registers.
     */
    private static final String[] BYTE_REG_NAMES = new String[] {
            "al", "cl", "dl", "bl", "ah", "ch", "dh", "bh"};

    /**
     * The names of the word general-purpose registers.
     */
    private static final String[] WORD_REG_NAMES = new String[] {
            "ax", "cx", "dx", "bx", "sp", "bp", "si", "di"};

    /**
     * The names of the double-word general-purpose registers.
     */
    private static final String[] DWORD_REG_NAMES = new String[] {
            "eax", "ecx", "edx", "ebx", "esp", "ebp", "esi", "edi"};

    /**
     * The hexadecimal digits.
     */
    private static final char[] HEX_DIGITS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * The condition code appendices of conditional instructions.
     */
    private static final String[] CC = new String[] {
            "o", "no", "b", "ae", "e", "ne", "be", "a",
            "s", "ns", "p", "np", "l", "ge", "le", "g"};

    /**
     * The buffer that stores the generated code.
     */
    private CodeBuffer code;

    /**
     * The start address of the code.
     */
    private int codeStart;

    /**
     * The position of the next byte to be disassembled.
     */
    private int cur;

    /**
     * The string of lock and repeat prefixes.
     */
    private String prefix;

    /**
     * A possible segment-override prefix.
     */
    private String segment;

    /**
     * Whether or not the instruction has an operand-size override prefix.
     */
    private boolean operandSizeOverride;

    /**
     * Hashtable of offsets to comments.
     */
    private Hashtable comments = new Hashtable();

    /**
     * Constructs a new disassembler for the specified code buffer.
     *
     * @param  code  the code buffer
     */
    public Disassembler(CodeBuffer code) {
        this.code      = code;
        this.codeStart = code.getCodeBegin();
        this.comments  = new Hashtable();
    }

    /**
     * Constructs a new disassembler for the specified code buffer.
     *
     * @param  code  the code buffer
     */
    public Disassembler(CodeBuffer code, Hashtable comments) {
        this.code      = code;
        this.codeStart = code.getCodeBegin();
        this.comments  = comments;
    }

    /**
     * Returns the hexadecimal representation of the specified value. The
     * resulting string consists of uppercase hexadecimal digits with no extra
     * leading zeros and ends with the character <code>'h'</code>.
     *
     * @param   value  value to be converted
     * @return  the hexadecimal representation
     */
    private static String hexString(int value) {
        char[] buf = new char[9];
        buf[8] = 'h';
        int charPos = 8;
        do {
            buf[--charPos] = HEX_DIGITS[value & 0xf];
            value >>>= 4;
        } while (value != 0);
        return new String(buf, charPos, (9 - charPos));
    }

    /**
     * Returns the hexadecimal representation of the specified address. The
     * resulting string is extended with leading zeros if necessary and does
     * not contain any appendix.
     *
     * @param   value  address to be converted
     * @return  the hexadecimal representation
     */
    private static String address(int value) {
        char[] buf = new char[8];
        for (int i = 7; i >= 0; i--) {
            buf[i] = HEX_DIGITS[value & 0xf];
            value >>>= 4;
        }
        return new String(buf);
    }

    /**
     * Returns a string representation an unknown instruction.
     *
     * @return  the bad format string
     */
    private static String shouldNotReachHere() {
        //Assert.shouldNotReachHere();
        //return null;
        return "?";
    }

    /**
     * Returns the name of the register with the specified number. The method
     * takes the current operand size into account to choose between the names
     * of word and double-word registers.
     *
     * @param   reg  number of the register
     * @return  name of the specified register
     */
    private String register(int reg) {
        if (operandSizeOverride) {
            return WORD_REG_NAMES[reg];
        } else {
            return DWORD_REG_NAMES[reg];
        }
    }

    /**
     * Tests if the specified operand mode denotes a register or memory
     * location.
     *
     * @param   mode  the operand mode
     * @return  whether or not operand is register or memory location
     */
    private static boolean isRegMem(int mode) {
        return (mode == R8) || (mode == R32) || (mode == MEM)
            || (mode == RM8) || (mode == RM16) || (mode == RM32)
            || (mode == RM64);
    }

    /**
     * Returns the byte at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the byte at the specified index
     */
    private int byteAt(int pos) {
        if (pos >= code.getCodeEnd()) {
            throw new EndException();
        }
        return code.byteAt(pos);
    }

    /**
     * Returns the 16-bit value at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the 16-bit value at the specified index
     */
    private int intAt(int pos) {
        return (byteAt(pos + 1) << 8) | byteAt(pos);
    }

    /**
     * Returns the 32-bit value at the specified position in the code buffer.
     *
     * @param   pos  index into the code buffer
     * @return  the 32-bit value at the specified index
     */
    private int longAt(int pos) {
        return (intAt(pos + 2) << 16) | intAt(pos);
    }

    /**
     * Returns the next byte and advances the current position.
     *
     * @return  the next byte
     */
    private int next() {
        return code.byteAt(cur++);
    }

    /**
     * Returns the byte at the specified offset from the current position. The
     * pointer to the next byte keeps unchanged and if the actual position is
     * outside the code, -1 will be returned without throwing any exception.
     *
     * @param   offset  offset from the current position
     * @return  the byte at the specified offset
     */
    private int peek(int offset) {
        int pos = cur + offset;
        if ((pos >= 0) && (pos < code.getCodeSize())) {
            return code.byteAt(pos);
        } else {
            return -1;
        }
    }

    /**
     * Returns a string representation of the specified SIB byte. This byte
     * encodes the scaling factor as well as the number of the index register
     * and the number of the base register. Note that this method changes the
     * pointer to the next byte, if the SIB byte specifies the existence of a
     * 32-bit displacement that is not read anywhere else.
     *
     * @param   mod  value of the mod field
     * @param   sib  value of the SIB byte
     * @return  string representation of the SIB byte
     */
    private String scaleIndexBase(int mod, int sib) {
        int scale = sib >>> 6;
        int index = (sib >>> 3) & 0x7;
        int base = sib & 0x7;
        StringBuffer str = new StringBuffer();
        if ((mod != 0) || (base != 5)) {
            str.append(DWORD_REG_NAMES[base]);
        }
        if (index != 4) {
            if ((mod != 0) || (base != 5)) {
                str.append('+');
            }
            str.append(DWORD_REG_NAMES[index]);
            if (scale != 0) {
                str.append('*');
                str.append(1 << scale);
            }
        }
        if ((mod == 0) && (base == 5)) {
            if (index != 4) {
                str.append(displacement(longAt(cur)));
            } else {
                str.append(hexString(longAt(cur)));
            }
            cur += 4;
        }
        return str.toString();
    }

    /**
     * Returns a string representation of the specified displacement. The
     * string contains the sign of the displacement, even if it is positive,
     * followed by the hexadecimal representation of its absolute value. If
     * the specified displacement equals zero, the empty string will be
     * returned.
     *
     * @param   disp  the 32-bit displacement
     * @return  string representation of the displacement
     */
    private String displacement(int disp) {
        if (disp < 0) {
            return '-' + hexString(-disp);
        } else if (disp > 0) {
            return '+' + hexString(disp);
        } else {
            return "";
        }
    }

    /**
     * Returns the string representation of the specified register or memory
     * operand.
     *
     * @param   mode  the operand mode
     * @param   mod   value of the mod field
     * @param   rm    value of the r/m field
     * @return  string representation of the address operand
     */
    private String regMem(int mode, int mod, int rm) {
        if (mod == 3) {
            if (mode == RM8) {
                return BYTE_REG_NAMES[rm];
            } else if (mode == RM32) {
                return register(rm);
            } else {
                return shouldNotReachHere();
            }
        } else {
            StringBuffer str = new StringBuffer();
            if (mode == MEM) {
                /* nothing to do */
            } else if (mode == RM8) {
                str.append("byte ptr ");
            } else if (mode == RM16) {
                str.append("word ptr ");
            } else if (mode == RM32) {
                str.append(operandSizeOverride ? "word ptr " : "dword ptr ");
            } else if (mode == RM64) {
                str.append("qword ptr ");
            } else {
                return shouldNotReachHere();
            }
            str.append(segment);
            str.append('[');
            if (rm == 4) {
                str.append(scaleIndexBase(mod, next()));
            } else if ((mod == 0) && (rm == 5)) {
                str.append(hexString(longAt(cur)));
                cur += 4;
            } else {
                str.append(DWORD_REG_NAMES[rm]);
            }
            if (mod == 1) {
                int disp8 = (next() << 24) >> 24;
                str.append(displacement(disp8));
            } else if (mod == 2) {
                int disp32 = longAt(cur);
                cur += 4;
                str.append(displacement(disp32));
            }
            str.append(']');
            return str.toString();
        }
    }

    /**
     * Returns a string representation of the operand at the current position.
     *
     * @param   mode   the operand mode
     * @param   ModRM  the addressing-form specifier
     * @return  string representation of the operand
     */
    private String operand(int mode, int ModRM) {
        switch (mode) {
        case R8:
            return BYTE_REG_NAMES[(ModRM >>> 3) & 0x7];
        case R32:
            return register((ModRM >>> 3) & 0x7);
        case MEM:
            /* falls through */
        case RM8:
            /* falls through */
        case RM16:
            /* falls through */
        case RM32:
            /* falls through */
        case RM64:
            return regMem(mode, ModRM >>> 6, ModRM & 0x7);
        case IMM8:
            return hexString(next());
        case IMM16:
            cur += 2;
            return hexString(intAt(cur - 2));
        case IMM32:
            if (operandSizeOverride) {
                cur += 2;
                return hexString(intAt(cur - 2));
            } else {
                cur += 4;
                return hexString(longAt(cur - 4));
            }
        case REL8:
            int rel8 = (next() << 24) >> 24;
            return address(code.getRelocation() + codeStart + cur + rel8);
        case REL32:
            int rel32 = longAt(cur);
            cur += 4;
            return address(code.getRelocation() + codeStart + cur + rel32);
        default:
            return shouldNotReachHere();
        }
    }

    /**
     * Returns a string representation of the current assembly instruction.
     *
     * @param   opname  name of the assembly instruction
     * @param   mode1   mode of the first operand
     * @param   mode2   mode of the second operand
     * @return  string representation of the assembly instruction
     */
    private String op(String opname, int mode1, int mode2) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(((mode1 != RM8) && (mode1 != RM32)) || ((mode2 != RM8) && (mode2 != RM32)), "only one address operand allowed");
            Assert.that((mode1 != IMM8) && (mode1 != IMM32), "immediate must be last operand");
            Assert.that((mode1 != REL8) && (mode1 != REL32) && (mode2 != REL8) && (mode2 != REL32), "relative address must be only operand");
        }
        StringBuffer str = new StringBuffer();
        str.append(op(opname));
        int ModRM = -1;
        if (isRegMem(mode1) || isRegMem(mode2)) {
            ModRM = next();
        }
        str.append(operand(mode1, ModRM));
        str.append(", ");
        str.append(operand(mode2, ModRM));
        return str.toString();
    }

    /**
     * Returns a string representation of the current assembly instruction.
     *
     * @param   opname  name of the assembly instruction
     * @param   mode    mode of the operand
     * @return  string representation of the assembly instruction
     */
    private String op(String opname, int mode) {
        StringBuffer str = new StringBuffer();
        str.append(op(opname));
        int ModRM = isRegMem(mode) ? next() : -1;
        str.append(operand(mode, ModRM));
        return str.toString();
    }

    /**
     * Returns a string representation of the current assembly instruction.
     *
     * @param   opname  name of the assembly instruction
     * @return  string representation of the assembly instruction
     */
    private String op(String opname) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(opname != null, "opname must be specified");
        }
        StringBuffer str = new StringBuffer();
        str.append(prefix);
        str.append(opname);
        while (str.length() < 14) {
            str.append(' ');
        }
        return str.toString();
    }

    /**
     * Returns a string representation of the current arithmetic instruction.
     * This method inspects the second lowest bit of the specified opcode to
     * determine if an 8-bit or 32-bit immediate value is used.
     *
     * @param   opname  name of the assembly instruction
     * @param   opcode  value of the opcode byte
     * @return  string representation of the assembly instruction
     */
    private String arith(String opname, int opcode) {
        if ((opcode & 0x02) != 0) {
            return op(opname, RM32, IMM8);
        } else {
            return op(opname, RM32, IMM32);
        }
    }

    /**
     * Returns a string representation of the specified floating-point
     * instruction.
     *
     * @param   opname  name of the assembly instruction
     * @param   i       index of the first operand register
     * @param   j       index of the second operand register
     * @return  string representation of the assembly instruction
     */
    private String farith(String opname, int i, int j) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((i >= 0) && (i < 8) && (j >= 0) && (j < 8), "invalid floating-point register");
        }
        StringBuffer str = new StringBuffer();
        str.append(farith(opname));
        str.append("st(" + i + ")");
        str.append(", ");
        str.append("st(" + j + ")");
        return str.toString();
    }

    /**
     * Returns a string representation of the specified floating-point
     * instruction.
     *
     * @param   opname  name of the assembly instruction
     * @param   i       index of the operand register
     * @return  string representation of the assembly instruction
     */
    private String farith(String opname, int i) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((i >= 0) && (i < 8), "invalid floating-point register");
        }
        StringBuffer str = new StringBuffer();
        str.append(farith(opname));
        str.append("st(" + i + ")");
        return str.toString();
    }

    /**
     * Returns a string representation of the specified floating-point
     * instruction. Since the actual operation is specified by the second
     * opcode byte, the current position is incremented by one in this method.
     *
     * @param   opname  name of the assembly instruction
     * @return  string representation of the assembly instruction
     */
    private String farith(String opname) {
        cur++;
        return op(opname);
    }

    /**
     * Returns the string representation of assembly instructions whose first
     * opcode byte is <code>'0F'</code>.
     *
     * @return  string representation of the assembly instruction
     */
    private String escape() {
        int opcode2 = next();
        switch (opcode2) {
        case 0x40: case 0x41: case 0x42: case 0x43: case 0x44: case 0x45:
        case 0x46: case 0x47: case 0x48: case 0x49: case 0x4a: case 0x4b:
        case 0x4c: case 0x4d: case 0x4e: case 0x4f:
            return op("cmov" + CC[opcode2 & 0xf], R32, RM32);
        case 0x80: case 0x81: case 0x82: case 0x83: case 0x84: case 0x85:
        case 0x86: case 0x87: case 0x88: case 0x89: case 0x8a: case 0x8b:
        case 0x8c: case 0x8d: case 0x8e: case 0x8f:
            return op("j" + CC[opcode2 & 0xf], REL32);
        case 0x90: case 0x91: case 0x92: case 0x93: case 0x94: case 0x95:
        case 0x96: case 0x97: case 0x98: case 0x99: case 0x9a: case 0x9b:
        case 0x9c: case 0x9d: case 0x9e: case 0x9f:
            return op("set" + CC[opcode2 & 0xf], RM8);
        case 0xa2:
            return op("cpuid");
        case 0xa5:
            return op("shld", RM32, R32) + ", cl";
        case 0xad:
            return op("shrd", RM32, R32) + ", cl";
        case 0xaf:
            return op("imul", R32, RM32);
        case 0xb1:
            return op("cmpxchg", RM32, R32);
        case 0xb6:
            return op("movzx", R32, RM8);
        case 0xb7:
            return op("movzx", R32, RM16);
        case 0xbe:
            return op("movsx", R32, RM8);
        case 0xbf:
            return op("movsx", R32, RM16);
        case 0xc0:
            return op("xadd", RM8, R8);
        case 0xc1:
            return op("xadd", RM32, R32);
        case 0xc8: case 0xc9: case 0xca: case 0xcb: case 0xcc: case 0xcd:
        case 0xce: case 0xcf:
            return op("bswap") + DWORD_REG_NAMES[opcode2 - 0xc8];
        default:
            return shouldNotReachHere();
        }
    }

    /**
     * Returns a string representation of the current floating-point
     * instruction.
     *
     * @param   opcode  value of the first opcode byte
     * @return  string representation of the assembly instruction
     */
    private String floatOp(int opcode) {
        int opcode2 = byteAt(cur);
        int digit = (opcode2 >>> 3) & 0x7;
        switch (opcode) {
        case 0xd8:
            if ((opcode2 >= 0xc0) && (opcode2 <= 0xc7)) {
                return farith("fadd", 0, opcode2 - 0xc0);
            } else if ((opcode2 >= 0xc8) && (opcode2 <= 0xcf)) {
                return farith("fmul", 0, opcode2 - 0xc8);
            } else if ((opcode2 >= 0xe0) && (opcode2 <= 0xe7)) {
                return farith("fsub", 0, opcode2 - 0xe0);
            } else if ((opcode2 >= 0xe8) && (opcode2 <= 0xef)) {
                return farith("fsubr", 0, opcode2 - 0xe8);
            } else if ((opcode2 >= 0xf0) && (opcode2 <= 0xf7)) {
                return farith("fdiv", 0, opcode2 - 0xf0);
            } else if ((opcode2 >= 0xf8) && (opcode2 <= 0xff)) {
                return farith("fdivr", 0, opcode2 - 0xf8);
            } else if (digit == 0) {
                return op("fadd", RM32);
            } else if (digit == 1) {
                return op("fmul", RM32);
            } else if (digit == 3) {
                return op("fcomp", RM32);
            } else if (digit == 4) {
                return op("fsub", RM32);
            } else if (digit == 5) {
                return op("fsubr", RM32);
            } else if (digit == 6) {
                return op("fdiv", RM32);
            } else if (digit == 7) {
                return op("fdivr", RM32);
            } else {
                return shouldNotReachHere();
            }
        case 0xd9:
            if ((opcode2 >= 0xc0) && (opcode2 <= 0xc7)) {
                return farith("fld", opcode2 - 0xc0);
            } else if ((opcode2 >= 0xc8) && (opcode2 <= 0xcf)) {
                return farith("fxch", opcode2 - 0xc8);
            } else if (opcode2 == 0xd0) {
                return farith("fnop");
            } else if (opcode2 == 0xe0) {
                return farith("fchs");
            } else if (opcode2 == 0xe1) {
                return farith("fabs");
            } else if (opcode2 == 0xe4) {
                return farith("ftst");
            } else if (opcode2 == 0xe8) {
                return farith("fld1");
            } else if (opcode2 == 0xee) {
                return farith("fldz");
            } else if (opcode2 == 0xf5) {
                return farith("fprem1");
            } else if (opcode2 == 0xf6) {
                return farith("fdecstp");
            } else if (opcode2 == 0xf7) {
                return farith("fincstp");
            } else if (opcode2 == 0xf8) {
                return farith("fprem");
            } else if (opcode2 == 0xfa) {
                return farith("fsqrt");
            } else if (opcode2 == 0xfe) {
                return farith("fsin");
            } else if (opcode2 == 0xff) {
                return farith("fcos");
            } else if (digit == 0) {
                return op("fld", RM32);
            } else if (digit == 2) {
                return op("fst", RM32);
            } else if (digit == 3) {
                return op("fstp", RM32);
            } else if (digit == 5) {
                return op("fldcw", RM16);
            } else {
                return shouldNotReachHere();
            }
        case 0xdb:
            if ((opcode2 >= 0xe8) && (opcode2 <= 0xef)) {
                return farith("fucomi", 0, opcode2 - 0xe8);
            } else if (digit == 0) {
                return op("fild", RM32);
            } else if (digit == 2) {
                return op("fist", RM32);
            } else if (digit == 3) {
                return op("fistp", RM32);
            } else {
                return shouldNotReachHere();
            }
        case 0xdc:
            if ((opcode2 >= 0xc0) && (opcode2 <= 0xc7)) {
                return farith("fadd", opcode2 - 0xc0, 0);
            } else if ((opcode2 >= 0xc8) && (opcode2 <= 0xcf)) {
                return farith("fmul", opcode2 - 0xc0, 0);
            } else if ((opcode2 >= 0xe0) && (opcode2 <= 0xe7)) {
                return farith("fsubr", opcode2 - 0xe0, 0);
            } else if ((opcode2 >= 0xe8) && (opcode2 <= 0xef)) {
                return farith("fsub", opcode2 - 0xe8, 0);
            } else if ((opcode2 >= 0xf0) && (opcode2 <= 0xf7)) {
                return farith("fdivr", opcode2 - 0xf0, 0);
            } else if ((opcode2 >= 0xf8) && (opcode2 <= 0xff)) {
                return farith("fdiv", opcode2 - 0xf8, 0);
            } else if (digit == 0) {
                return op("fadd", RM64);
            } else if (digit == 1) {
                return op("fmul", RM64);
            } else if (digit == 3) {
                return op("fcomp", RM64);
            } else if (digit == 4) {
                return op("fsub", RM64);
            } else if (digit == 5) {
                return op("fsubr", RM64);
            } else if (digit == 6) {
                return op("fdiv", RM64);
            } else if (digit == 7) {
                return op("fdivr", RM64);
            } else {
                return shouldNotReachHere();
            }
        case 0xdd:
            if ((opcode2 >= 0xc0) && (opcode2 <= 0xc7)) {
                return farith("ffree", opcode2 - 0xc0);
            } else if ((opcode2 >= 0xd8) && (opcode2 <= 0xdf)) {
                return farith("fstp", opcode2 - 0xd8);
            } else if ((opcode2 >= 0xe0) && (opcode2 <= 0xe7)) {
                return farith("fucom", opcode2 - 0xe0);
            } else if ((opcode2 >= 0xe8) && (opcode2 <= 0xef)) {
                return farith("fucomp", opcode2 - 0xe8);
            } else if (digit == 0) {
                return op("fld", RM64);
            } else if (digit == 2) {
                return op("fst", RM64);
            } else if (digit == 3) {
                return op("fstp", RM64);
            } else if (digit == 4) {
                return op("frstor", MEM);
            } else if (digit == 6) {
                return op("fnsave", MEM);
            } else {
                return shouldNotReachHere();
            }
        case 0xde:
            if ((opcode2 >= 0xc0) && (opcode2 <= 0xc7)) {
                return farith("faddp", opcode2 - 0xc0, 0);
            } else if ((opcode2 >= 0xc8) && (opcode2 <= 0xcf)) {
                return farith("fmulp", opcode2 - 0xc8, 0);
            } else if (opcode2 == 0xd9) {
                return farith("fcompp");
            } else if ((opcode2 >= 0xe0) && (opcode2 <= 0xe7)) {
                return farith("fsubrp", opcode2 - 0xe0, 0);
            } else if ((opcode2 >= 0xe8) && (opcode2 <= 0xef)) {
                return farith("fsubp", opcode2 - 0xe8, 0);
            } else if ((opcode2 >= 0xf0) && (opcode2 <= 0xf7)) {
                return farith("fdivrp", opcode2 - 0xf0, 0);
            } else if ((opcode2 >= 0xf8) && (opcode2 <= 0xff)) {
                return farith("fdivp", opcode2 - 0xf8, 0);
            } else {
                return shouldNotReachHere();
            }
        case 0xdf:
            if (opcode2 == 0xe0) {
                return farith("fnstsw") + "ax";
            } else if ((opcode2 >= 0xe8) && (opcode2 <= 0xef)) {
                return farith("fucomip", 0, opcode2 - 0xe8);
            } else if (digit == 5) {
                return op("fild", RM64);
            } else if (digit == 7) {
                return op("fistp", RM64);
            } else {
                return shouldNotReachHere();
            }
        default:
            return shouldNotReachHere();
        }
    }

    /**
     * Returns the digit that provides an extension to the opcode.
     *
     * @return  the extension of the opcode
     */
    private int digit() {
        return (byteAt(cur) >>> 3) & 0x7;
    }

    /**
     * Returns the assembly instruction for the next machine code.
     *
     * @return  next assembly instruction
     */
    private String decode() {
        prefix = "";
        segment = "";
        operandSizeOverride = false;
        for (;;) {
            int opcode = next();
            switch (opcode) {
            case 0x01:
                return op("add", RM32, R32);
            case 0x03:
                return op("add", R32, RM32);
            case 0x0b:
                return op("or", R32, RM32);
            case 0x0f:
                return escape();
            case 0x13:
                return op("adc", R32, RM32);
            case 0x1b:
                return op("sbb", R32, RM32);
            case 0x23:
                return op("and", R32, RM32);
            case 0x26:
                segment = "es:";
                break;
            case 0x2b:
                return op("sub", R32, RM32);
            case 0x2e:
                segment = "cs:";
                break;
            case 0x33:
                return op("xor", R32, RM32);
            case 0x36:
                segment = "ss:";
                break;
            case 0x3b:
                return op("cmp", R32, RM32);
            case 0x3e:
                segment = "ds:";
                break;
            case 0x40:
                /* falls through */
            case 0x41:
                /* falls through */
            case 0x42:
                /* falls through */
            case 0x43:
                /* falls through */
            case 0x44:
                /* falls through */
            case 0x45:
                /* falls through */
            case 0x46:
                /* falls through */
            case 0x47:
                return op("inc") + register(opcode - 0x40);
            case 0x48:
                /* falls through */
            case 0x49:
                /* falls through */
            case 0x4a:
                /* falls through */
            case 0x4b:
                /* falls through */
            case 0x4c:
                /* falls through */
            case 0x4d:
                /* falls through */
            case 0x4e:
                /* falls through */
            case 0x4f:
                return op("dec") + register(opcode - 0x48);
            case 0x50:
                /* falls through */
            case 0x51:
                /* falls through */
            case 0x52:
                /* falls through */
            case 0x53:
                /* falls through */
            case 0x54:
                /* falls through */
            case 0x55:
                /* falls through */
            case 0x56:
                /* falls through */
            case 0x57:
                return op("push") + register(opcode - 0x50);
            case 0x58:
                /* falls through */
            case 0x59:
                /* falls through */
            case 0x5a:
                /* falls through */
            case 0x5b:
                /* falls through */
            case 0x5c:
                /* falls through */
            case 0x5d:
                /* falls through */
            case 0x5e:
                /* falls through */
            case 0x5f:
                return op("pop") + register(opcode - 0x58);
            case 0x60:
                return op("pushad");
            case 0x61:
                return op("popad");
            case 0x64:
                segment = "fs:";
                break;
            case 0x65:
                segment = "gs:";
                break;
            case 0x66:
                operandSizeOverride = true;
                break;
            case 0x68:
                return op("push", IMM32);
            case 0x69:
                return op("imul", R32, RM32) + ", " + operand(IMM32, -1);
            case 0x6b:
                return op("imul", R32, RM32) + ", " + operand(IMM8, -1);
            case 0x70: case 0x71: case 0x72: case 0x73: case 0x74: case 0x75:
            case 0x76: case 0x77: case 0x78: case 0x79: case 0x7a: case 0x7b:
            case 0x7c: case 0x7d: case 0x7e: case 0x7f:
                return op("j" + CC[opcode & 0xf], REL8);
            case 0x81:
                /* falls through */
            case 0x83:
                switch (digit()) {
                case 0:
                    return arith("add", opcode);
                case 1:
                    return arith("or", opcode);
                case 2:
                    return arith("adc", opcode);
                case 3:
                    return arith("sbb", opcode);
                case 4:
                    return arith("and", opcode);
                case 5:
                    return arith("sub", opcode);
                case 6:
                    return arith("xor", opcode);
                case 7:
                    return arith("cmp", opcode);
                default:
                    return shouldNotReachHere();
                }
            case 0x85:
                return op("test", RM32, R32);
            case 0x87:
                return op("xchg", R32, RM32);
            case 0x88:
                return op("mov", RM8, R8);
            case 0x89:
                return op("mov", RM32, R32);
            case 0x8a:
                return op("mov", R8, RM8);
            case 0x8b:
                return op("mov", R32, RM32);
            case 0x8d:
                return op("lea", R32, MEM);
            case 0x8f:
                if (digit() == 0) {
                    return op("pop", RM32);
                } else {
                    return shouldNotReachHere();
                }
            case 0x90:
                return op("nop");
            case 0x9c:
                return op("pushfd");
            case 0x99:
                return op("cdq");
            case 0x9b:
                if ((peek(1) == 0xd9) && ((peek(2) & 0x38) == 0x38)) {
                    cur++;
                    return op("fstcw", RM16);
                } else if ((peek(1) == 0xdb) && (peek(2) == 0xe3)) {
                    cur += 2;
                    return op("finit");
                } else {
                    return op("fwait");
                }
            case 0x9d:
                return op("popfd");
            case 0x9e:
                return op("sahf");
            case 0xa5:
                return op("movsd");
            case 0xa9:
                return op("test") + "eax, " + operand(IMM32, -1);
            case 0xab:
                return op("stosd");
            case 0xb8:
                /* falls through */
            case 0xb9:
                /* falls through */
            case 0xba:
                /* falls through */
            case 0xbb:
                /* falls through */
            case 0xbc:
                /* falls through */
            case 0xbd:
                /* falls through */
            case 0xbe:
                /* falls through */
            case 0xbf:
                return op("mov") + register(opcode - 0xb8) + ", " + operand(IMM32, -1);
            case 0xc1:
                switch (digit()) {
                case 2:
                    return op("rcl", RM32, IMM8);
                case 3:
                    return op("rcr", RM32, IMM8);
                case 4:
                    return op("shl", RM32, IMM8);
                case 5:
                    return op("shr", RM32, IMM8);
                case 7:
                    return op("sar", RM32, IMM8);
                default:
                    return shouldNotReachHere();
                }
            case 0xc2:
                return op("ret", IMM16);
            case 0xc3:
                return op("ret");
            case 0xc6:
                if (digit() == 0) {
                    return op("mov", RM8, IMM8);
                } else {
                    return shouldNotReachHere();
                }
            case 0xc7:
                if (digit() == 0) {
                    return op("mov", RM32, IMM32);
                } else {
                    return shouldNotReachHere();
                }
            case 0xcc:
                return op("int") + '3';
            case 0xd1:
                switch (digit()) {
                case 2:
                    return op("rcl", RM32) + ", 1";
                case 3:
                    return op("rcr", RM32) + ", 1";
                case 4:
                    return op("shl", RM32) + ", 1";
                case 5:
                    return op("shr", RM32) + ", 1";
                case 7:
                    return op("sar", RM32) + ", 1";
                default:
                    return shouldNotReachHere();
                }
            case 0xd3:
                switch (digit()) {
                case 2:
                    return op("rcl", RM32) + ", CL";
                case 4:
                    return op("shl", RM32) + ", CL";
                case 5:
                    return op("shr", RM32) + ", CL";
                case 7:
                    return op("sar", RM32) + ", CL";
                default:
                    return shouldNotReachHere();
                }
            case 0xd8:
                /* falls through */
            case 0xd9:
                /* falls through */
            case 0xdb:
                /* falls through */
            case 0xdc:
                /* falls through */
            case 0xdd:
                /* falls through */
            case 0xde:
                /* falls through */
            case 0xdf:
                return floatOp(opcode);
            case 0xe8:
                return op("call", REL32);
            case 0xe9:
                return op("jmp", REL32);
            case 0xeb:
                return op("jmp", REL8);
            case 0xf0:
                prefix = prefix + "lock ";
                break;
            case 0xf3:
                prefix = prefix + "rep ";
                break;
            case 0xf6:
                if (digit() == 0) {
                    return op("test", RM8, IMM8);
                } else {
                    return shouldNotReachHere();
                }
            case 0xf7:
                switch (digit()) {
                case 0:
                    return op("test", RM32, IMM32);
                case 2:
                    return op("not", RM32);
                case 3:
                    return op("neg", RM32);
                case 4:
                    return op("mul", RM32);
                case 5:
                    return op("imul", RM32);
                case 7:
                    return op("idiv", RM32);
                default:
                    return shouldNotReachHere();
                }
            case 0xf8:
                return op("hlt");
            case 0xfe:
                if (digit() == 1) {
                    return op("dec", RM8);
                } else {
                    return shouldNotReachHere();
                }
            case 0xff:
                switch (digit()) {
                case 0:
                    return op("inc", RM32);
                case 1:
                    return op("dec", RM32);
                case 2:
                    return op("call", RM32);
                case 4:
                    return op("jmp", RM32);
                case 6:
                    return op("push", RM32);
                default:
                    return shouldNotReachHere();
                }
            default:
                return shouldNotReachHere();
            }
        }
    }

    /**
     * Prints the values of the bytes in the specified code area.
     *
     * @param  start  start of the code area
     * @param  size   number of bytes to be printed
     */
    private void printBytes(int start, int size) {
        StringBuffer line = new StringBuffer();
        line.append(address(code.getRelocation() + codeStart + start));
        line.append(' ');
        for (int i = 0; i < size; i++) {
            if (line.length() > 30) {
                System.out.println(line.toString());
                line = new StringBuffer("         ");
            }
            line.append(' ');
            int value = byteAt(start + i);
            line.append(HEX_DIGITS[value >>> 4]);
            line.append(HEX_DIGITS[value & 0xf]);
        }
        while (line.length() < 35) {
            line.append(' ');
        }
        System.out.print(line.toString());
    }

    /**
     * Prints the specified area of machine code without disassembling it.
     *
     * @param  start  start of the code area
     * @param  end    end of the code area
     */
    public void hexDump(int start, int end) {
        if (end > start) {
            StringBuffer line = new StringBuffer();
            for (int addr = start & ~0x0f; addr < end; addr++) {
                if ((addr & 0x0f) == 0) {
                    System.out.println(line.toString());
                    line = new StringBuffer();
                    line.append(address(addr));
                    line.append(' ');
                } else if ((addr & 0x07) == 0) {
                    line.append(' ');
                }
                if (addr < start) {
                    line.append("   ");
                } else {
                    line.append(' ');
                    int value = byteAt(addr - codeStart);
                    line.append(HEX_DIGITS[value >>> 4]);
                    line.append(HEX_DIGITS[value & 0xf]);
                }
            }
            System.out.println(line);
        }
    }

    /**
     * Disassembles the specified area of the machine code.
     *
     * @param  start  start of the code area
     * @param  end    end of the code area
     */
    public void disassemble(int start, int end) {
        cur = start - codeStart;
        while (codeStart + cur < end) {
            int last = cur;
            try {

                /*
                 * If there is a comment for this address then print it.
                 */
                String comment = (String)comments.get(new Integer(last));
                if (comment != null) {
                    boolean nl = true;
                    for (int i = 0 ; i < comment.length() ; i++) {
                        //if (nl) {
                        //    System.out.print("                                   ; ");
                        //    System.out.print("; ");
                        //}
                        int ch = comment.charAt(i);
                        nl = (ch == '\n');
                        System.out.print((char)ch);
                    }
                }

                /*
                 * Decode the instuction.
                 */
                String instr = decode();

                /*
                 * Dump the bytes for the instruction.
                 */
                printBytes(last, cur - last);

                /*
                 * Print the disassembled instruction.
                 */
                System.out.println(instr);
            } catch (EndException ex) {
                /*
                 * Just dump the byte for the instruction that could not be decoded.
                 */
                cur = last + 1;
                printBytes(last, 1);
            }
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(codeStart + cur <= end, "last instruction too long");
        }
    }

}

class EndException extends RuntimeException {
    EndException() {
    }
}