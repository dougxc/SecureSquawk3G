/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import java.io.PrintStream;
import com.sun.squawk.util.Stack;     // Version without synchronization
import com.sun.squawk.util.Hashtable; // Version without synchronization
import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.compiler.asm.x86.*;

/**
 * X86 code generator.
 *
 * @author   Nik Shaylor
 */
public class X86CodeGenerator extends CodeGenerator implements Constants, Codes {

    /**
     * Code buffer used to assemble instrutions.
     */
    private CodeBuffer code;

    /**
     * The X86 assembler.
     */
    private Assembler a;

    /**
     * The address of "sub esp, #n" instruction of the last enter()
     */
    private int subinst;

    /**
     * Hack to optomize a CallOp to a label.
     */
    private LabelLiteralOp lastLabelLiteral;

    /**
     * Hack to optomize a CallOp to a symbol.
     */
    private SymbolOp lastSymbol;

    /**
     * Offset that MP must always be equal to.
     */
    private static final int MPOFFSET = 0;

    /**
     * Offset that IP must always be equal to.
     */
    private static final int IPOFFSET = 1;

    /**
     * Offset that LP must always be equal to.
     */
    private static final int LPOFFSET = 2;

    /**
     * Offset that SS must always be equal to.
     */
    private static final int SSOFFSET = 3;

    /**
     * Flag to say that a c.local(SS) has been seen.
     */
    private boolean ssdefined;

    /**
     * Hashtable of offsets to comments.
     */
    private Hashtable comments = new Hashtable();

    /**
     * The number of bytes needed for a jump instruction.
     */
    private static final int JUMPSIZE = 5;

    /**
     * Initialize the code generator.
     *
     * @param comp the compiler
     */
    public void initialize(Compiler comp) {
        super.initialize(comp);
        code = new CodeBuffer();
        a = new Assembler(code);
        assume(RELOC_ABSOLUTE_INT == Assembler.RELOC_ABSOLUTE_INT);
        assume(RELOC_RELATIVE_INT == Assembler.RELOC_RELATIVE_INT);
    }

    /**
     * Get the length of the compiled code.
     *
     * @return the length in bytes
     */
    public int getCodeSize() {
        return code.getCodeSize();
    }

    /**
     * Get the code array buffer.
     *
     * @return the code array
     */
    public byte[] getCode() {
        return code.getBytes();
    }

    /**
     * Test the direction of the runtime stack.
     *
     * @return true is it goes downwards (like x86), false if it does upwards (like SPARC).
     */
    public boolean stackDescends() {
        return true;
    }

    /**
     * Start the code generation.
     *
     * @param first the first element of the IR
     */
    public void start(Instruction first) {
    }

    /**
     * Finish the code generation.
     *
     * @param load true if the code should be loaded into malloc() memory
     * @return the relocation information array
     */
    public int[] finish() {
        int lth = code.getCodeSize();
        int[] res = a.relocate(0);
        new Disassembler(code, comments).disassemble(0, lth);
        return res;
    }

    /**
     * Get the offset from the frame pointer to slot used for the MP variable.
     *
     * @return the offset in bytes
     */
    public int getMPOffset() {
        return getSlotOffset(MPOFFSET, false);
    }

    /**
     * Get the offset from the frame pointer to slot used for the IP variable.
     *
     * @return the offset in bytes
     */
    public int getIPOffset() {
        return getSlotOffset(IPOFFSET, false);
    }

    /**
     * Get the offset from the frame pointer to slot used for the IP variable.
     *
     * @return the offset in bytes
     */
    public int getLPOffset() {
        return getSlotOffset(LPOFFSET, false);
    }

    /**
     * Get the offset from the frame pointer to slot used for the SS variable.
     *
     * @return the offset in bytes
     */
    public int getSSOffset() {
        return getSlotOffset(SSOFFSET, false);
    }

    /**
     * Get the length of a jump instruction.
     *
     * @return the length in bytes
     */
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

        int res = interp-bytecodes-5;
        offset--;

        /*
         * x86 is little endian.
         */
        while (offset-- > 0) {
            res >>>= 8;
        }
        return res & 0xFF;
    }

    /**
     * Calculate the real offset for an activation slot.
     *
     * @param index the slot index
     * @param isParm true is the slot is a parameter.
     * @return the offset in bytes
     */
    private int getSlotOffset(int index, boolean isParm) {
        if (isParm) {
            return (index + 2) * 4;
        } else {
            return (0 - index - 1) * 4;
        }
    }

    /**
     * Calculate the real offset for an activation slot.
     *
     * @param aslot the ActivationSlot to access
     * @param extra 1 for access to the second word of a two word slot. Otherwise zero.
     * @return the offset in bytes
     */
    private int getRealOffset(ActivationSlot aslot, int extra) {
        return getSlotOffset(aslot.getOffset() + extra, aslot.isParm());
    }

    /**
     * Load a variable
     *
     * @param reg the register to load into
     * @param aslot the activation slot to load
     */
    private void load(Register reg, ActivationSlot aslot) {
        a.movl(reg, new Address(EBP, getRealOffset(aslot, +0)));
    }

    /**
     * Load the second word of a variable
     *
     * @param reg the register to load into
     * @param aslot the activation slot to load
     */
    private void load2(Register reg, ActivationSlot aslot) {
        a.movl(reg, new Address(EBP, getRealOffset(aslot , +1)));
    }

    /**
     * Load a variable
     *
     * @param reg the register to load into
     * @param s the stack local variable to load
     */
    private void load(Register reg, StackLocal sl) {
        load(reg, sl.getSlot(this));
    }

    /**
     * Load the second word of a variable
     *
     * @param reg the register to load into
     * @param s the stack local variable to load
     */
    private void load2(Register reg, StackLocal sl) {
        load2(reg, sl.getSlot(this));
    }

    /**
     * Store a variable
     *
     * @param reg the register to store from
     * @param aslot the activation slot to store into
     */
    private void store(Register reg, ActivationSlot aslot) {
        a.movl(new Address(EBP, getRealOffset(aslot, +0)), reg);
    }

    /**
     * Store the second word of a variable
     *
     * @param reg the register to store from
     * @param aslot the activation slot to store into
     */
    private void store2(Register reg, ActivationSlot aslot) {
        a.movl(new Address(EBP, getRealOffset(aslot, +1)), reg);
    }

    /**
     * Store a variable
     *
     * @param reg the register to store from
     * @param sl the stack local variable to store into
     */
    private void store(Register reg, StackLocal sl) {
        store(reg, sl.getSlot(this));
    }

    /**
     * Store the second word of a variable
     *
     * @param reg the register to store from
     * @param sl the stack local variable to store into
     */
    private void store2(Register reg, StackLocal sl) {
        store2(reg, sl.getSlot(this));
    }

    /**
     * Create a new label.
     *
     * @return a new label
     */
    public XLabel newLabel() {
        return new X86Label(this, a.newLabel());
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitUnOp(UnOp inst) {
        switch (inst.opcode & 0xF) {
            case I:
            case U:  {
                load(EAX, inst.in);
                switch (inst.opcode & 0xFFFFFF00) {
                    case OP_NEG:    a.negl(EAX);                break;
                    case OP_COM:    a.notl(EAX);                break;
                    default: shouldNotReachHere();
                }
                store(EAX, inst.getTarget());
                break;
            }
            case L: {
                emitComment("** UnOP L not implemented");
                store(EAX, inst.getTarget());
                break;
            }
            case F: {
                emitComment("** UnOP F not implemented");
                store(EAX, inst.getTarget());
                break;
            }
            case D: {
                emitComment("** UnOP D not implemented");
                store(EAX, inst.getTarget());
                break;
            }
            default: shouldNotReachHere();
        }
    }

    /**
     * Compare EAX with ECX
     *
     * @param cc the conditon code
     */
    private void cmpl(int cc) {
        a.cmpl(EAX, ECX);
        a.movl(EAX, 0);
        a.setb(cc, EAX);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitBinOp(BinOp inst) {
        switch (inst.opcode & 0xF) {
            case R:
            case O:
            case I:
            case U: {
                load(EAX, inst.in1);
                load(ECX, inst.in2);
                Register res = EAX;
                switch (inst.opcode & 0xFFFFFF00) {
                    case OP_ADD:    a.addl(EAX, ECX);                               break;
                    case OP_SUB:    a.subl(EAX, ECX);                               break;
                    case OP_MUL:    a.imull(EAX, ECX);                              break;
                    case OP_DIV:    a.idivl(ECX);                                   break;
                    case OP_REM:    a.idivl(ECX); res = EDX;                        break;
                    case OP_SHL:    a.shll(EAX);                                    break;
                    case OP_SHR:    a.sarl(EAX);                                    break;
                    case OP_USHR:   a.shrl(EAX);                                    break;
                    case OP_AND:    a.andl(EAX, ECX);                               break;
                    case OP_OR:     a.orl(EAX, ECX);                                break;
                    case OP_XOR:    a.xorl(EAX, ECX);                               break;
                    case OP_EQ:     cmpl(EQUAL);                                    break;
                    case OP_NE:     cmpl(NOT_EQUAL);                                break;
                    case OP_LT:     cmpl(LESS);                                     break;
                    case OP_LE:     cmpl(LESS_EQUAL);                               break;
                    case OP_GT:     cmpl(GREATER);                                  break;
                    case OP_GE:     cmpl(GREATER_EQUAL);                            break;
                    default: shouldNotReachHere();
                }
                store(res, inst.getTarget());
                break;
            }
            case L: {
                emitComment("** BinOP L not implemented");
                store(EAX, inst.getTarget());
                break;
            }
            case G: {
                emitComment("** BinOP G not implemented");
                store(EAX, inst.getTarget());
                break;
            }
            case F: {
                emitComment("** BinOP F not implemented");
                store(EAX, inst.getTarget());
                break;
            }
            case D: {
                emitComment("** BinOP D not implemented");
                store(EAX, inst.getTarget());
                break;
            }

            default: shouldNotReachHere();
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitCvtOp(CvtOp inst) {
        if (inst.conv) {
            switch (inst.opcode & 0xF) { // From code
                case R:
                case O:
                case I:
                case U:  {
                    emitComment("** CvtOP R/O/I/U not implemented");
                    store(EAX, inst.getTarget());
                    break;
                }
                case L: {
                    emitComment("** CvtOP L not implemented");
                    store(EAX, inst.getTarget());
                    break;
                }
                case F: {
                    emitComment("** CvtOP F not implemented");
                    store(EAX, inst.getTarget());
                    break;
                }
                case D: {
                    emitComment("** CvtOP D not implemented");
                    store(EAX, inst.getTarget());
                    break;
                }
                case A:
                case B:
                case C:
                case S:  {
                    emitComment("** CvtOP A/B/C/S not implemented");
                    store(EAX, inst.getTarget());
                    break;
                }
                default: shouldNotReachHere();
            }
        } else {
            switch (inst.opcode & 0xF) { // From code
                case R:
                case O:
                case F:
                case I:
                case U: {
                    load(EAX, inst.in);
                    store(EAX, inst.getTarget());
                    break;
                }
                case L:
                case G:
                case D: {
                    load(EAX, inst.in);
                    store(EAX, inst.getTarget());
                    load2(EDX, inst.in);
                    store2(EDX, inst.getTarget());
                    break;
                }
                default: shouldNotReachHere();
            }
        }
    }



    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDupOp(DupOp inst) {
        load(EAX, inst.in);
        store(EAX, inst.getTarget());
        if (inst.type().getActivationSize() == 2) {
            load2(EAX, inst.in);
            store2(EAX, inst.getTarget());
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitAllocaOp(AllocaOp inst) {
        load(EAX, inst.in);
        a.subl(ESP, EAX);
        if (ssdefined) {
            a.movl(new Address(EBP, getSSOffset()), ESP);
        }
        store(ESP, inst.getTarget());
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLoadParmOp(LoadParmOp inst) {
        load(EAX, inst.in);
        a.movl(EAX, new Address(EBP, EAX, TIMES_4, +8));
        store(EAX, inst.getTarget());
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitStoreParmOp(StoreParmOp inst) {
        load(EDX, inst.value);
        load(EAX, inst.index);
        a.movl(new Address(EBP, EAX, TIMES_4, +8), EDX);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitComment(String str) {
        Integer key = new Integer(code.getCodePos());
        String comment = (String)comments.get(key);
        if (comment == null) {
            comment = "";
        }
        comment += str + "\n"; // append to any existing comment
        comments.put(key, comment);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitCommentOp(CommentOp inst) {
        emitComment(inst.str);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLoadOp(LoadOp inst) {
        load(EAX, inst.local.getSlot());
        store(EAX, inst.getTarget());
        if (inst.local.type().getActivationSize() == 2) {
            load2(EDX, inst.local.getSlot());
            store2(EDX, inst.getTarget());
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitStoreOp(StoreOp inst) {
        load(EAX, inst.in);
        store(EAX, inst.local.getSlot());
        if (inst.local.type().getActivationSize() == 2) {
            load2(EDX, inst.in);
            store2(EDX, inst.local.getSlot());
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitReadOp(ReadOp inst) {
        load(EAX, inst.ref);
        switch (inst.readType.getTypeCode()) {
            case R:
            case O:
            case I:
            case F:
            case U: {
                a.movl(EAX, new Address(EAX));
                store(EAX, inst.getTarget());
                break;
            }
            case A: {
                a.movzxb(EAX, new Address(EAX));
                store(EAX, inst.getTarget());
                break;
            }
            case B: {
                a.movsxb(EAX, new Address(EAX));
                store(EAX, inst.getTarget());
                break;
            }
            case C: {
                a.movzxw(EAX, new Address(EAX));
                store(EAX, inst.getTarget());
                break;
            }
            case S: {
                a.movsxw(EAX, new Address(EAX));
                store(EAX, inst.getTarget());
                break;
            }
            case D:
            case L: {
                a.movl(EAX, new Address(EAX));
                store(EAX, inst.getTarget());
                a.movl(EDX, new Address(EAX, 4));
                store2(EDX, inst.getTarget());
                break;
            }
            default: shouldNotReachHere();
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitWriteOp(WriteOp inst) {
        load(EAX, inst.ref);
        load(ECX, inst.in);
        switch (inst.writeType.getTypeCode()) {
            case R:
            case O:
            case I:
            case F:
            case U: {
                a.movl(new Address(EAX), ECX);
                break;
            }
            case A:
            case B: {
                a.movb(new Address(EAX), ECX);
                break;
            }
            case C:
            case S: {
                a.movw(new Address(EAX), ECX);
                break;
            }
            case D:
            case L: {
                a.movl(new Address(EAX), ECX);
                load2(ECX, inst.in);
                a.movl(new Address(EAX, 4), ECX);
                break;
            }
            default: shouldNotReachHere();
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitIntLiteralOp(IntLiteralOp inst) {
        a.movl(EAX, inst.value);
        store(EAX, inst.getTarget());
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitSymbolOp(SymbolOp inst) {
        if (inst.getNext() instanceof CallOp) {
            lastSymbol = inst;
            inst.getTarget().getSlot(this);
        } else {
            /*
             * Insert the move of zero (for the moment) into EAX.
             */
            int pos = a.movl(EAX);
            /*
             * Record the name of the symbol against the offset and relocation type required for the call.
             */
            comp.addFixup(RELOC_ABSOLUTE_INT << 24 | pos, inst.name);
            store(EAX, inst.getTarget());
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLongLiteralOp(LongLiteralOp inst) {
        a.movl(EAX, (int)inst.value);
        store(EAX, inst.getTarget());
        a.movl(EDX, (int)(inst.value>>32));
        store2(EDX, inst.getTarget());
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitFloatLiteralOp(FloatLiteralOp inst) {
/*if[FLOATS]*/
        a.movl(EAX, Float.floatToIntBits(inst.value));
        store(EAX, inst.getTarget());
/*else[FLOATS]*/
//      throw new Error("No floating poinnt");
/*end[FLOATS]*/
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDoubleLiteralOp(DoubleLiteralOp inst) {
/*if[FLOATS]*/
        long value = Double.doubleToLongBits(inst.value);
        a.movl(EAX, (int)value);
        store(EAX, inst.getTarget());
        a.movl(EDX, (int)(value>>32));
        store2(EDX, inst.getTarget());
/*else[FLOATS]*/
//      throw new Error("No floating poinnt");
/*end[FLOATS]*/
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitArrayLiteralOp(ArrayLiteralOp inst) {
        inst.label = allocLiteral(inst.value);
        X86Label xlabel = (X86Label)inst.label;
        a.leal(EAX, new Address(xlabel.alabel));
        store(EAX, inst.getTarget());
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLabelLiteralOp(LabelLiteralOp inst) {
        if (inst.getNext() instanceof CallOp) {
            lastLabelLiteral = inst;
            inst.getTarget().getSlot(this);
        } else {
            X86Label xlabel = (X86Label)inst.label;
            a.leal(EAX, new Address(xlabel.alabel));
            store(EAX, inst.getTarget());
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitBranchOp(BranchOp inst) {
        if (inst.label != null) {
            X86Label xlabel = (X86Label)inst.label;
            if (inst.in == null) {
                a.jmp(xlabel.alabel);
            } else {
                load(EAX, inst.in);
                a.cmpl(EAX, 0);
                a.jcc(inst.cond ? NOT_ZERO : ZERO, xlabel.alabel);
            }
        } else {
            if (inst.in == null) {
                a.jmp(inst.dst);
            } else {
                load(EAX, inst.in);
                a.cmpl(EAX, 0);
                a.jcc(inst.cond ? NOT_ZERO : ZERO, inst.dst);
            }
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitJumpOp(JumpOp inst) {
        load(EAX, inst.in);
        a.jmp(EAX);
    }

    /**
     * Instruction emitter.
     * <p>
     * Note the JVM calling convention is implemented here by making EAX
     * contain the address of the callee on entry.
     *
     * @param inst the instruction type
     */
    public void emitCallOp(CallOp inst) {
        Stack inputs = inst.inputs;
        StackLocal fn = (StackLocal)inputs.peek();
        int nparms = inputs.size() - 1;
        int convention = inst.convention;
        boolean jvm     = (convention == Compiler.C_JVM     || convention == Compiler.C_JVM_DYNAMIC);
        boolean dynamic = (convention == Compiler.C_DYNAMIC || convention == Compiler.C_JVM_DYNAMIC);

        int nwords = 0;

        /*
         * Push parameters.
         */
        for (int i = 0 ; i < nparms ; i++) {
            StackLocal p = (StackLocal)inputs.elementAt(i);
            if (p.type().getActivationSize() == 2) {
                load2(EDX, p);
                a.pushl(EDX);
                nwords++;
            }
            load(EAX, p);
            a.pushl(EAX);
            nwords++;
        }

        /*
         * Hack to optomize call to a label.
         */
        if (lastLabelLiteral != null) {
            X86Label xlabel = (X86Label)lastLabelLiteral.label;
            lastLabelLiteral = null;
            if (!jvm) {
                a.call(xlabel.alabel);
            } else {
                a.movl(EAX, xlabel.alabel);
                a.call(EAX);
            }
        }

        /*
         * Hack to optomize call to symbol.
         */
        else if (lastSymbol != null) {
            int pos;
            int rel;

            /*
             * Insert the call to location zero (for the moment).
             */
            if (jvm) {
                rel = RELOC_ABSOLUTE_INT << 24;
                pos = a.movl(EAX);
                a.call(EAX);
            } else {
                rel = RELOC_RELATIVE_INT << 24;
                pos = a.call();
            }

            /*
             * Record the name of the symbol against the offset and relocation type required for the call.
             */
            comp.addFixup(rel | pos, lastSymbol.name);
            lastSymbol = null;
        }

        else {
            /*
             * The default case.
             */
            load(EAX, fn);
            a.call(EAX);
        }

        /*
         * Pop the parameters from the stack if the number is known.
         */
        if (!dynamic && (nwords) > 0) {
            a.addl(ESP, (nwords) * 4);
        } else if(dynamic) {
            assume(ssdefined, "SS not defined");
            a.movl(ESP, new Address(EBP, getSSOffset()));
        }

        /*
         * Save return value
         */
        switch (inst.type().getActivationSize()) {
            case 2:
                store2(EDX, inst.getTarget());
            case 1:
                store(EAX, inst.getTarget());
                break;
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDeadCodeOp(DeadCodeOp inst) {
        emitComment("DeadCode");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDropOp(DropOp inst) {
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitStackCheckOp(StackCheckOp inst) {
        //if (ssdefined) {
        //    a.movl(new Address(EBP, getSSOffset()), ESP);
        //}
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitPushOp(PushOp inst) {
        switch (inst.in.type().getActivationSize()) {
            case 2:
                load2(EDX, inst.in);
                a.pushl(EDX);
            case 1:
                load(EAX, inst.in);
                a.pushl(EAX);
                break;
            default:
                shouldNotReachHere();
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitPopOp(PopOp inst) {
        switch (inst.type().getActivationSize()) {
            case 2:
                a.popl(EAX);
                store(EAX, inst.getTarget());
                a.popl(EDX);
                store2(EDX, inst.getTarget());
                break;
            case 1:
                a.popl(EAX);
                store(EAX, inst.getTarget());
                break;
            default:
                shouldNotReachHere();
        }
    }

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public void emitPopAllOp(PopAllOp inst) {
        assume(ssdefined, "SS not defined");
        a.movl(ESP, new Address(EBP, getSSOffset()));
    }

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public void emitPeekReceiverOp(PeekReceiverOp inst) {
        a.movl(EAX, new Address(ESP));
        store(EAX, inst.getTarget());
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitRetOp(RetOp inst) {
        switch (inst.type().getActivationSize()) {
            case 2:
                load2(EDX, inst.in);
            case 1:
                load(EAX, inst.in);
                break;
        }
        a.movl(ESP, EBP);
        a.popl(EBP);
        a.ret(0);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitPhiOp(PhiOp inst) {
        X86Label xlabel = (X86Label)inst.label;
        a.bind(xlabel.alabel);
    }

    /**
     * Instruction emitter.
     * <p>
     * Note the JVM calling convention is implemented here by making EAX
     * contain the address of the callee on entry.
     *
     * @param inst the instruction type
     */
    public void emitEnterOp(EnterOp inst) {

        /*
         * If the special preamble code specifies that the routine address
         * should be written into slot zero then bind a label here.
         */
        ALabel alabel = null;
        if (inst.specialPreamble == Compiler.E_ADDRESS) {
            alabel = a.newLabel();
            a.bind(alabel);
        }

        //a.int3();
        a.pushl(EBP);
        a.movl(EBP, ESP);
        subinst = code.getCodePos();
        a.nop();    // Leave space for a subl with a 32 bit immeadiate.
        a.nop();
        a.nop();
        a.nop();
        a.nop();
        a.nop();

        /*
         * Add special preabble code if necessaery.
         */
        switch (inst.specialPreamble) {
            case Compiler.E_NULL: {
                a.movl(new Address(EBP, getMPOffset()), 0);
                break;
            }
            case Compiler.E_ADDRESS: {
                a.leal(EAX, new Address(alabel));
                a.movl(new Address(EBP, getMPOffset()), EAX);
                break;
            }
            case Compiler.E_REGISTER: {
                a.movl(new Address(EBP, getMPOffset()), EAX); // EAX is the fixed register
                break;
            }
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLeaveOp(LeaveOp inst) {
        int save = code.getCodePos();
        code.setCodePos(subinst);
        a.subl(ESP, getLocalSlotCount() * 4);
        code.setCodePos(save);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitAllocateOp(AllocateOp inst) {
        inst.local.allocLocal(this);

        /*
         * If the original type of the local was either MP or IP then
         * check that the slot allocator gave the correct offset. This
         * ensures that the c.local() definitions are in the right place
         * and not duplicated etc.
         */
        Type type = inst.originalType;
        if (type == MP) {
            assume(!inst.local.getSlot().isParm());
            assume(inst.local.getSlot().getOffset() == MPOFFSET);
        } else if (type == IP) {
            assume(!inst.local.getSlot().isParm());
            assume(inst.local.getSlot().getOffset() == IPOFFSET);
        } else if (type == LP) {
            assume(!inst.local.getSlot().isParm());
            assume(inst.local.getSlot().getOffset() == LPOFFSET);
        } else if (type == SS) {
            assume(!inst.local.getSlot().isParm());
            assume(inst.local.getSlot().getOffset() == SSOFFSET);
            ssdefined = true;
        }

    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDeallocateOp(DeallocateOp inst) {
        inst.local.freeLocal(this);
    }

    /**
     * Bind a label
     *
     * @param label the label to be bound
     */
    private void bind(Label label) {
        X86Label xlabel = (X86Label)label;
        a.bind(xlabel.alabel);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDataOp(DataOp inst) {
        inst.data.emit(this);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitFramePointerOp (FramePointerOp inst) {
        store(EBP, inst.getTarget());
    }

    /**
     * Instruction emitter.
     *
     * @param data the data to be emitted along with a label to bind to it
     */
    public void emitLiteralData(LiteralData data) {
        Object obj  = data.getValue();
        Label label = data.getLabel();
        if (obj instanceof byte[]) {
            byte[] array = (byte[])obj;
            a.align(1);
            bind(label);
            for (int i = 0 ; i < array.length ; i++) {
                a.emitByte(array[i]);
            }
            return;
        }

        if (obj instanceof char[]) {
            char[] array = (char[])obj;
            a.align(2);
            bind(label);
            for (int i = 0 ; i < array.length ; i++) {
                a.emitShort(array[i]);
            }
            return;
        }

        if (obj instanceof short[]) {
            short[] array = (short[])obj;
            a.align(2);
            bind(label);
            for (int i = 0 ; i < array.length ; i++) {
                a.emitShort(array[i]);
            }
            return;
        }

        if (obj instanceof int[]) {
            int[] array = (int[])obj;
            a.align(4);
            bind(label);
            for (int i = 0 ; i < array.length ; i++) {
                a.emitInt(array[i]);
            }
            return;
        }

        if (obj instanceof long[]) {
            long[] array = (long[])obj;
            a.align(4);
            bind(label);
            for (int i = 0 ; i < array.length ; i++) {
                a.emitLong(array[i]);
            }
            return;
        }

/*if[FLOATS]*/

        if (obj instanceof float[]) {
            float[] array = (float[])obj;
            a.align(4);
            bind(label);
            for (int i = 0 ; i < array.length ; i++) {
                a.emitFloat(array[i]);
            }
            return;
        }

        if (obj instanceof double[]) {
            double[] array = (double[])obj;
            a.align(4);
            bind(label);
            for (int i = 0 ; i < array.length ; i++) {
                a.emitDouble(array[i]);
            }
            return;
        }

/*end[FLOATS]*/

        if (obj instanceof Label[]) {
            Label[] array = (Label[])obj;
            a.align(4);
            bind(label);
            for (int i = 0 ; i < array.length ; i++) {
                X86Label xlabel = (X86Label)array[i];
                a.emitLabel(xlabel.alabel);
            }
            return;
        }

        shouldNotReachHere();
    }


}

/**
 * A subclass of Label for the X86 assembler
 */
class X86Label extends XLabel {

    /**
     * The code generator.
     */
    X86CodeGenerator cgen;

    /**
     * The assember label.
     */
    ALabel alabel;

    /**
     * Constructor.
     *
     * @param cgen the code generator
     * @param alabel the assembler label
     */
    X86Label(X86CodeGenerator cgen, ALabel alabel) {
        this.cgen = cgen;
        this.alabel = alabel;
    }

    /**
     * Get the offset to the label in the code buffer.
     *
     * @return the offset in bytes
     */
    public int getOffset() {
        return alabel.getPos();
    }

    /**
     * Print the IR
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        out.print(" &" + alabel.getId());
    }
}