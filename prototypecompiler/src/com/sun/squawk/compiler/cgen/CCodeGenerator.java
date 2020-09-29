/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import java.io.*;
import java.util.Hashtable;
import javax.microedition.io.*;
import com.sun.squawk.compiler.*;
//import com.sun.squawk.compiler.Compiler;

/**
 * C code generator.
 *
 * @author   Nik Shaylor
 */
public class CCodeGenerator extends CodeGenerator implements Codes {

    /**
     * Temporary output buffer.
     */
    ByteArrayOutputStream baos;

    /**
     * The C source output stream.
     */
    PrintStream out;

    /**
     * Hashtable that correlates lables to data structures created with data() calls to the compiler.
     */
    Hashtable dataTable;

    /**
     * Initialize the code generator.
     *
     * @param comp the compiler
     */
    public void initialize(Compiler comp) {
        super.initialize(comp);
        dataTable = new Hashtable();
        ActivationSlot.strictSlotReuse = true;
    }

    /**
     * Finish the code generation.
     *
     * @param load true if the code should be loaded into malloc() memory
     * @return the relocation information array
     */
    public int[] finish() {
        return null;
    }

    /**
     * Get the offset from the frame pointer to slot used for the MP variable.
     *
     * @return the offset in bytes
     */
    public int getMPOffset() {
        throw new Error();
    }

    /**
     * Get the offset from the frame pointer to slot used for the IP variable.
     *
     * @return the offset in bytes
     */
    public int getIPOffset() {
        throw new Error();
    }

    /**
     * Get the offset from the frame pointer to slot used for the LP variable.
     *
     * @return the offset in bytes
     */
    public int getLPOffset() {
        throw new Error();
    }

    /**
     * Get the length of the compiled code.
     *
     * @return the length in bytes
     */
    public int getCodeSize() {
        throw new Error();
    }

    /**
     * Test the direction of the runtime stack.
     *
     * @return true is it goes downwards (like x86), false if it does upwards (like SPARC).
     */
    public boolean stackDescends() {
        throw new Error();
    }

    /**
     * Get the length of a jump instruction.
     *
     * @return the length in bytes
     */
    public int getJumpSize() {
        throw new Error();
    }

    /**
     * Get a single byte of a jump instruction sequence.
     *
     * @param bytecodes the address of the bytecode array
     * @param interp the address of the interpreter
     * @param offset the offset to the byte to return
     * @return the byte
     */
    public int getJumpByte(int bytecodes, int interp, int offset) {
        throw new Error();
    }

    /**
     * Get the code array buffer.
     *
     * @return the code array
     */
    public byte[] getCode() {
        throw new Error();
    }

    /**
     * Output a reference to an activation slot.
     *
     * @param local the local to reference
     */
    void local(ActivationSlot aslot) {
        if (aslot.isParm()) {
            out.print("P");
        } else {
            out.print("L");
        }
        out.print(aslot.getOffset());
    }

    /**
     * Output a reference to a local variable
     *
     * @param local the local to reference
     */
    void local(XLocal local) {
        local(local.getSlot());
    }

    /**
     * Output a reference to a local variable
     *
     * @param sl the stack local variable for the local
     */
    void local(StackLocal sl) {
        local(sl.getSlot(this));
    }

    /**
     * Create a new label.
     *
     * @return a new label
     */
    public XLabel newLabel() {
        return new CCLabel(this);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitUnOp(UnOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitBinOp(BinOp inst) {
        switch (inst.opcode & 0xF) {
            case R:
            case I:
            case U:
            case L:
            case F:
            case D: {
                local(inst.getTarget());
                out.print(" = ");
                int opcode = inst.opcode & 0xFFFFFF00;
                if (opcode == OP_SHL || opcode == OP_SHR || opcode == OP_USHR) {
                    boolean isLong = inst.getTarget().getSlot(this).getType().getActivationSize() == 2;
                    if (opcode == OP_USHR) {
                        if (isLong) {
                             out.print("((ujlong)");
                        } else {
                             out.print("((unsigned)");
                        }
                        local(inst.in1);
                        out.print(")");
                    } else {
                        local(inst.in1);
                    }
                    if (opcode == OP_SHL) {
                        out.print(" << ");
                    } else {
                        out.print(" >> ");
                    }
                    out.print("(");
                    local(inst.in2);
                    if (isLong) {
                         out.print("&63)");
                    } else {
                         out.print("&31)");
                    }
                } else {
                    local(inst.in1);
                    switch (opcode) {
                        case OP_ADD:    out.print(" + ");                   break;
                        case OP_SUB:    out.print(" - ");                   break;
                        case OP_MUL:    out.print(" * ");                   break;
                        case OP_DIV:    out.print(" / ");                   break;
                        case OP_REM:    out.print(" % ");                   break;
                        case OP_AND:    out.print(" & ");                   break;
                        case OP_OR:     out.print(" | ");                   break;
                        case OP_XOR:    out.print(" ^ ");                   break;
                        case OP_EQ:     out.print(" == ");                  break;
                        case OP_NE:     out.print(" != ");                  break;
                        case OP_LT:     out.print(" < ");                   break;
                        case OP_LE:     out.print(" <= ");                  break;
                        case OP_GT:     out.print(" > ");                   break;
                        case OP_GE:     out.print(" >= ");                  break;
                        default: shouldNotReachHere();
                    }
                    local(inst.in2);
                }
                break;
            }
            default: shouldNotReachHere();
        }
        out.println(";");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitCvtOp(CvtOp inst) {
        int totype = inst.getTarget().getSlot(this).getType().getTypeCode();
        local(inst.getTarget());
        out.print(" = ");
        if (inst.conv) {
            switch (totype) {
                case I: out.print("(int)");             break;
                case U: out.print("(unsigned)");        break;
                case L: out.print("(jlong)");           break;
                case F: out.print("(float)");           break;
                case D: out.print("(double)");          break;
                case A: out.print("(unsigned char)");   break;
                case B: out.print("(signed char)");     break;
                case C: out.print("(unsigned short)");  break;
                case S: out.print("(signed short)");    break;
                default: shouldNotReachHere();
            }
            local(inst.in);
            out.println(";");
        } else {
            int fromtype = inst.in.getSlot(this).getType().getTypeCode();
            switch (fromtype) {
                case R: out.print("r2");    break;
                case I: out.print("i2");    break;
                case U: out.print("u2");    break;
                case L: out.print("l2");    break;
                case F: out.print("f2");    break;
                case D: out.print("d2");    break;
                default: shouldNotReachHere();
            }
            switch (totype) {
                case R: out.print("r(");    break;
                case I: out.print("i(");    break;
                case U: out.print("u(");    break;
                case L: out.print("l(");    break;
                case F: out.print("f(");    break;
                case D: out.print("d(");    break;
                default: shouldNotReachHere();
            }
            local(inst.in);
            out.println(");");
        }
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDupOp(DupOp inst) {
        local(inst.getTarget());
        out.print(" = ");
        local(inst.in);
        out.println(";");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitAllocaOp(AllocaOp inst) {
        local(inst.getTarget());
        out.print(" = alloca(");
        local(inst.in);
        out.println(");");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLoadParmOp(LoadParmOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitStoreParmOp(StoreParmOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitCommentOp(CommentOp inst) {
        out.println("/* " +inst.str + " */");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLoadOp(LoadOp inst) {
        local(inst.getTarget());
        out.print(" = ");
        local(inst.local);
        out.println(";");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitStoreOp(StoreOp inst) {
        local(inst.local);
        out.print(" = ");
        local(inst.in);
        out.println(";");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitReadOp(ReadOp inst) {
        switch (inst.readType.getTypeCode()) {
            case R: {
                local(inst.getTarget());
                out.print(" = ((char **)");
                local(inst.ref);
                out.println(")[0];");
                break;
            }
            case I:
            case U: {
                local(inst.getTarget());
                out.print(" = ((int *)");
                local(inst.ref);
                out.println(")[0];");
                break;
            }
            case L: {
                local(inst.getTarget());
                out.print(" = ((jlong *)");
                local(inst.ref);
                out.println(")[0];");
                break;
            }
            case A: {
                local(inst.getTarget());
                out.print(" = ((unsigned char *)");
                local(inst.ref);
                out.println(")[0];");
                break;
            }
            case B: {
                local(inst.getTarget());
                out.print(" = ((signed char *)");
                local(inst.ref);
                out.println(")[0];");
                break;
            }
            case C: {
                local(inst.getTarget());
                out.print(" = ((unsigned short *)");
                local(inst.ref);
                out.println(")[0];");
                break;
            }
            case S: {
                local(inst.getTarget());
                out.print(" = ((signed short *)");
                local(inst.ref);
                out.println(")[0];");
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
        switch (inst.writeType.getTypeCode()) {
            case R: {
                out.print("((char **)");
                local(inst.ref);
                out.print(")[0] = ");
                local(inst.in);
                out.println(";");
                break;
            }
            case I:
            case U: {
                out.print("((int *)");
                local(inst.ref);
                out.print(")[0] = ");
                local(inst.in);
                out.println(";");
                break;
            }
            case L: {
                out.print("((jlong *)");
                local(inst.ref);
                out.print(")[0] = ");
                local(inst.in);
                out.println(";");
                break;
            }
            case A: {
                out.print("((unsigned char *)");
                local(inst.ref);
                out.print(")[0] = ");
                local(inst.in);
                out.println(";");
                break;
            }
            case B: {
                out.print("((signed char *)");
                local(inst.ref);
                out.print(")[0] = ");
                local(inst.in);
                out.println(";");
                break;
            }
            case C: {
                out.print("((unsigned short *)");
                local(inst.ref);
                out.print(")[0] = ");
                local(inst.in);
                out.println(";");
                break;
            }
            case S: {
                out.print("((signed short *)");
                local(inst.ref);
                out.print(")[0] = ");
                local(inst.in);
                out.println(";");
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
        local(inst.getTarget());
        out.println(" = "+inst.value+";");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitSymbolOp(SymbolOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLongLiteralOp(LongLiteralOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitFloatLiteralOp(FloatLiteralOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDoubleLiteralOp(DoubleLiteralOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitArrayLiteralOp(ArrayLiteralOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLabelLiteralOp(LabelLiteralOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitBranchOp(BranchOp inst) {
        CCLabel clabel = (CCLabel)inst.label;
        if (inst.in != null) {
            out.print("if(");
            local(inst.in);
            out.print(") ");
        }
        out.print("goto ");
        clabel.print(out);
        out.println(";");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitJumpOp(JumpOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitCallOp(CallOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDeadCodeOp(DeadCodeOp inst) {
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitDropOp(DropOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitStackCheckOp(StackCheckOp inst) {
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitPushOp(PushOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitPopOp(PopOp inst) {
        throw new Error();
    }

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public void emitPopAllOp(PopAllOp inst) {
        throw new Error();
    }

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public void emitPeekReceiverOp(PeekReceiverOp inst) {
        throw new Error();
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitRetOp(RetOp inst) {
        out.print("return ");
        if (inst.in != null) {
            local(inst.in);
        }
        out.println(";");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitPhiOp(PhiOp inst) {
        CCLabel clabel = (CCLabel)inst.label;
        clabel.print(out);
        out.println(":");
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitEnterOp(EnterOp inst) {
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitLeaveOp(LeaveOp inst) {
        try {
            out = new PrintStream(Connector.openDataOutputStream("file://cgen.c"));
//            out = new PrintStream(new FileOutputStream("file://cgen.c"));

            out.println("#include <malloc.h>");
            out.println("");
            out.println("#define jlong  long long");
            out.println("#define ujlong unsigned long long");
            out.println("#ifdef _MSC_VER");
            out.println("#undef  jlong");
            out.println("#define jlong  __int64");
            out.println("#undef  ujlong");
            out.println("#define ujlong unsigned __int64");
            out.println("#endif /* _MSC_VER */");
            out.println("#ifdef __GNUC__");
            out.println("#undef  jlong");
            out.println("#define jlong  int64_t");
            out.println("#undef  ujlong");
            out.println("#define ujlong u_int64_t");
            out.println("#endif /* __GNUC__ */");
            out.println("");
            out.println("union uu { int i; unsigned u; float f; jlong l; double d; char *r; };");
            out.println("");
            out.println("unsigned i2u(int i)               { union uu x; x.i = i; return x.u; }");
            out.println("float    i2f(int i)               { union uu x; x.i = i; return x.f; }");
            out.println("char *   i2r(int i)               { union uu x; x.i = i; return x.r; }");
            out.println("int      u2i(unsigned u)          { union uu x; x.u = u; return x.i; }");
            out.println("int      f2i(float f)             { union uu x; x.f = f; return x.i; }");
            out.println("int      r2i(char *r)             { union uu x; x.r = r; return x.i; }");
            out.println("double   l2d(long l)              { union uu x; x.l = l; return x.d; }");
            out.println("long     d2l(double d)            { union uu x; x.d = d; return x.l; }");
            out.println("char *   l2r(long l)              { union uu x; x.l = l; return x.r; }");
            out.println("long     r2l(char *r)             { union uu x; x.r = r; return x.l; }");
            out.println("");

            out.print("int foo(");
            emitParmDefs();
            out.println(") {");
            emitLocalDefs();
            out.println();
            out.write(baos.toByteArray());
            out.println("}");

            // ++TEMP
            out.println("void main() {");
            out.println(" char ADD   = 0;");
            out.println(" char SUB   = 1;");
            out.println(" char CON   = 2;");
            out.println(" char LOAD  = 3;");
            out.println(" char STORE = 4;");
            out.println(" char RET   = 5;");
            out.println(" char buf[] = {CON, 100, CON, 1, SUB, RET};");
            out.println(" int res = foo(buf);");
            out.println(" printf(\"res=%d\\n\", res);");
            out.println("}");
            // --TEMP

            out.close();
        } catch(IOException ex) {
            assume(false, "IOException "+ex);
        }
    }

    /**
     * Emit the parameter definitions.
     */
    private void emitParmDefs() {
        ActivationSlot slot = getParameterSlots();
        if (slot != null) {
            printSlot("P", slot);
            while (slot.getNext() != null) {
                slot = slot.getNext();
                out.print(", ");
                printSlot("P", slot);
            }
        }
    }

    /**
     * Emit the local definitions.
     */
    private void emitLocalDefs() {
        ActivationSlot slot = getLocalSlots();
        if (slot != null) {
            printSlot("L", slot);
            out.print("; ");
            while (slot.getNext() != null) {
                slot = slot.getNext();
                printSlot("L", slot);
                out.print("; ");
            }
        }
    }

    /**
     * Emit the slot definitions.
     */
    private void printSlot(String prefix, ActivationSlot slot) {
        switch (slot.getType().getTypeCode()) {
            case I: out.print("int ");      break;
            case L: out.print("jlong ");    break;
            case F: out.print("float ");    break;
            case D: out.print("double ");   break;
            case R: out.print("char *");    break;
            default: shouldNotReachHere();
        }
        out.print(prefix+slot.getOffset());
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitAllocateOp(AllocateOp inst) {
        inst.local.allocLocal(this);
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
    public void emitLiteralData(LiteralData inst) {
        // Ignore...
    }

    /**
     * Create a new data allocation instruction.
     *
     * @param label the label to the data
     * @param obj   the array of data
     * @return      the new instruction
     */
    public Instruction newData(XLabel label, Object obj) {
        dataTable.put(label, obj); // Record association as the IR is being build
        return super.newData(label, obj);
    }

    /**
     * Instruction emitter.
     *
     * @param inst the instruction type
     */
    public void emitFramePointerOp (FramePointerOp inst) {
        throw new Error();
    }

    /**
     * The start routine called before code generation.
     * <p>
     * Replcace the following IR sequences with a special switch() node.
     * <code>
     * c.literal(4);
     * c.mul();
     * c.literal(table);
     * c.add();
     * c.read(REF);
     * c.jump();
     * </code>
     * @param inst the start of the IR
     */
    public void start(Instruction inst) {
        baos = new ByteArrayOutputStream();
        out = new PrintStream(baos);
        Instruction prev = null;
        while (!(inst instanceof LeaveOp)) {
            if (needsFixup(inst)) {
                inst = inst.getNext(); // BinOp
                StackLocal in = ((BinOp)inst).in1;
                inst = inst.getNext(); // LabelLiteralOp
                XLabel label = ((LabelLiteralOp)inst).label;
                inst = inst.getNext(); // BinOp
                inst = inst.getNext(); // ReadOp
                inst = inst.getNext(); // JumpOp
                Instruction next = inst.getNext();
                XLabel[] labels = (XLabel[])dataTable.get(label);
                SwitchOp switchOp = new SwitchOp(labels, in);
                prev.setNext(switchOp);
                switchOp.setNext(next);
                prev = switchOp;
                inst = next;
            } else {
                prev = inst;
                inst = inst.getNext();
            }
        }
    }

    /**
     * Routine called by specialFixup to identify the sequence.
     *
     * @param inst node to be tested
     */
    private boolean needsFixup(Instruction inst) {
        if (!(inst instanceof IntLiteralOp)) {
            return false;
        }
        if (((IntLiteralOp)inst).value != 4) {
            return false;
        }
        inst = inst.getNext();
        if (!(inst instanceof BinOp)) {
            return false;
        }
        if ((((BinOp)inst).opcode & 0xFFFFFF00) != OP_MUL) {
            return false;
        }
        inst = inst.getNext();
        if (!(inst instanceof LabelLiteralOp)) {
            return false;
        }
        inst = inst.getNext();
        if (!(inst instanceof BinOp)) {
            return false;
        }
        if ((((BinOp)inst).opcode & 0xFFFFFF00) != OP_ADD) {
            return false;
        }
        inst = inst.getNext();
        if (!(inst instanceof ReadOp)) {
            return false;
        }
        inst = inst.getNext();
        if (!(inst instanceof JumpOp)) {
            return false;
        }
        return true;
    }
}

/**
 * A subclass of Label for the X86 assembler
 */
class CCLabel extends XLabel {

    /**
     * The next available label number
     */
    private static int next = 0;

    /**
     * The code generator.
     */
    CCodeGenerator cgen;

    /**
     * The number of the label.
     */
    int number = next++;

    /**
     * Constructor
     */
    CCLabel(CCodeGenerator cgen) {
        this.cgen = cgen;
    }

    /**
     * Get the offset to the label in the code buffer.
     *
     * @return the offset in bytes
     */
    public int getOffset() {
        throw new Error();
    }

    /**
     * Print the IR
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
       out.print(" Label" + number);
    }
}


/**
 * Special IR node to replace the computed goto sequences in the IR
 */
class SwitchOp extends Instruction {

    /**
     * The branch targets.
     */
    protected XLabel[] labels;

    /**
     * The stack local variable for the input operand.
     */
    protected StackLocal in;

    /**
     * Constructor.
     *
     * @param label the branch target
     */
    public SwitchOp(XLabel[] labels, StackLocal in) {
        super(VOID);
        this.labels = labels;
        this.in = in;
    }

    /**
     * Call the approperate function of an CodeEmitter.
     *
     * @param emitter the CodeEmitter
     */
    public void emit(CodeEmitter emitter) {
        CCodeGenerator cgen = (CCodeGenerator)emitter;
        PrintStream out = cgen.out;
        out.print("switch(");
        cgen.local(in);
        out.println(") {");
        for (int i = 0 ; i  < labels.length ; i++) {
            out.print("\tcase "+i+": goto ");
            labels[i].print(out);
            out.println(";");
        }
        out.println("}");
        cgen.freeLocal(in);
    }

    /**
     * Print the IR
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        super.print(out);
        out.print(" SwitchOp ");
        in.print(out);
        for (int i = 0 ; i  < labels.length ; i++) {
            out.print(" ");
            labels[i].print(out);
        }
    }

}