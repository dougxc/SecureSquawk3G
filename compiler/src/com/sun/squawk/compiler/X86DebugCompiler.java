/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.util.Hashtable; // Version without synchronization
import com.sun.squawk.compiler.X86Compiler;

/**
 * Debugging support for the Compiler interface
 *
 * @author: Cristina Cifuentes
 */

class X86DebugCompiler extends X86Compiler {

    /**
     * Count on the next instruction to be processed by the compiler.
     */
    int instrCount = 1;

  /*-----------------------------------------------------------------------*\
   *                               Constructor                             *
  \*-----------------------------------------------------------------------*/

    /**
     * Create a new compiler.
     */
    public X86DebugCompiler() {
        super(true);
    }

    /*-----------------------------------------------------------------------*\
     *                         Code generation options                       *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public int getMPOffset() {
        System.err.println("[" + instrCount++ + "]\t" + "GetMPoffset");
        return super.getMPOffset();
    }

    /**
     * @see Compiler
     */
    public int getIPOffset() {
        System.err.println("[" + instrCount++ + "]\t" + "GetIPOffset");
        return super.getIPOffset();
    }

    /**
     * @see Compiler
     */
    public int getLPOffset() {
        System.err.println("[" + instrCount++ + "]\t" + "GetLPOffset");
        return super.getLPOffset();
    }

    /**
     * @see Compiler
     */
    public int getJumpSize() {
        System.err.println("[" + instrCount++ + "]\t" + "GetJumpSize");
        return super.getJumpSize();
    }

    /**
     * @see Compiler
     */
    public int getJumpByte(int bytecodes, int interp, int offset) {
        System.err.println("[" + instrCount++ + "]\t" + "GetJumpByte(" + bytecodes +
                           ", " + interp + "," + offset + ")");
        return super.getJumpByte(bytecodes, interp, offset);
    }

    /**
     * @see Compiler
     */
    public Compiler alloca() {
        System.err.println("[" + instrCount++ + "]\t" + "Alloca");
        return super.alloca();
    }

    /**
     * @see Compiler
     */
    public Compiler deadCode() {
        System.err.println("[" + instrCount++ + "]\t" + "DeadCode");
        return super.deadCode();
    }

    /*-----------------------------------------------------------------------*\
     *                               Compilation                             *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler compile() {
        System.err.println("[" + instrCount++ + "]\t" + "Compile");
        return super.compile();
    }

    /**
     * @see Compiler
     */
    public int getCodeSize() {
        System.err.println("[" + instrCount++ + "]\t" + "GetCodeSize");
        return super.getCodeSize();
    }

    /**
     * @see Compiler
     */
    public byte[] getCode() {
        System.err.println("[" + instrCount++ + "]\t" + "GetCode");
        return super.getCode();
    }

    /**
     * @see Compiler
     */
    public int[] getRelocationInfo() {
        System.err.println("[" + instrCount++ + "]\t" + "GetRelocationInfo");
        return super.getRelocationInfo();
    }

    /**
     * @see Compiler
     */
    public Hashtable getFixupInfo() {
        System.err.println("[" + instrCount++ + "]\t" + "GetFixupInfo");
        return super.getFixupInfo();
    }

    /*-----------------------------------------------------------------------*\
     *-----------------------------------------------------------------------*
     *                           Instructions                                *
     *-----------------------------------------------------------------------*
    \*-----------------------------------------------------------------------*/

    /*-----------------------------------------------------------------------*\
     *                            Label management                           *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Label label() {
        System.err.println("[" + instrCount++ + "]\t" + "Label");
        return super.label();
    }

    /**
     * @see Compiler
     */
    public Compiler bind(Label label) {
        System.err.print("[" + instrCount++ + "]\t" + "Bind(");
        ((XLabel)label).print();
        System.err.println(")");
        return super.bind(label);
    }

    /*-----------------------------------------------------------------------*\
     *                           Function definition                         *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler enter(Label label, int preambleCode) {
        System.err.print("[" + instrCount++ + "]\t" + "Enter(");
        ((XLabel)label).print();
        System.err.println(", " + preambleCode + ")");
        return super.enter(label, preambleCode);
    }

    /**
     * @see Compiler
     */
    public Compiler enter() {
        System.err.println("[" + instrCount++ + "]\t" + "Enter");
        return super.enter(null, Compiler.E_NONE);
    }

    /**
     * @see Compiler
     */
    public Compiler enter(Label label) {
        System.err.print("[" + instrCount++ + "]\t" + "Enter(");
        ((XLabel)label).print();
        System.err.println(")");
        return super.enter(label, Compiler.E_NONE);
    }

    /**
     * @see Compiler
     */
    public Compiler enter(int preambleCode) {
        System.err.println("[" + instrCount++ + "]\t" + "Enter(" + preambleCode + ")");
        return super.enter(null, preambleCode);
    }

    /**
     * @see Compiler
     */
    public Local parm(Type type, int hint) {
        System.err.print("[" + instrCount++ + "]\t" + "Parm(");
        type.print();
        System.err.println(", " + hint + ")");
        return super.parm(type, hint);
    }

    /**
     * @see Compiler
     */
    public Local parm(Type type) {
        System.err.print("[" + instrCount++ + "]\t" + "Parm(");
        type.print();
        System.err.println(")");
        return super.parm(type, P_LOW);
    }

    /**
     * @see Compiler
     */
    public Compiler result(Type type) {
        System.err.print("[" + instrCount++ + "]\t" + "Result(");
        type.print();
        System.err.println(")");
        return super.result(type);
    }

    /**
     * @see Compiler
     */
    public Compiler leave(MethodMap mmap) {
        System.err.print("[" + instrCount++ +"]\t" + "Leave(");
        if (mmap == null)
            System.err.println("null)");
        else
            System.err.println("mmap)");
        return super.leave(mmap);
    }

    /**
     * @see Compiler
     */
    public Compiler leave() {
        System.err.println("[" + instrCount++ + "]\t" + "Leave");
        return super.leave();
    }

    /*-----------------------------------------------------------------------*\
     *                           Scope definition                            *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler begin() {
        System.err.println("[" + instrCount++ + "]\t" + "Begin");
        return super.begin();
    }

    /**
     * @see Compiler
     */
    public Local local(Type type, int hint) {
        System.err.print("[" + instrCount++ + "]\t" + "Local(");
        type.print();
        System.err.println(", " + hint + ")");
        return super.local(type, hint);
    }

    /**
     * @see Compiler
     */
    public Local local(Type type) {
        return local(type, P_LOW);
    }

    /**
     * @see Compiler
     */
    public Compiler end() {
        System.err.println("[" + instrCount++ + "]\t" + "End");
        return super.end();
    }

    /**
     * @see Compiler
     */
    public int getLocalCount() {
        System.err.println("[" + instrCount++ + "]\t" + "GetLocalCount");
        return super.getLocalCount();
    }

    /**
     * @see Compiler
     */
    public Type tosType() {
        System.err.println("[" + instrCount++ + "]\t" + "TosType");
        return super.tosType();
    }

    /**
     * @see Compiler
     */
    public Compiler loadParm() {
        System.err.println("[" + instrCount++ + "]\t" + "LoadParm");
        return super.loadParm();
    }

    /**
     * @see Compiler
     */
    public Compiler storeParm() {
        System.err.println("[" + instrCount++ + "]\t" + "StoreParm");
        return super.storeParm();
    }

    /**
     * @see Compiler
     */
    public Compiler comment(String str) {
        System.err.println("[" + instrCount++ + "]\t" + "Comment(" + str + ")");
        return super.comment(str);
    }

    /*-----------------------------------------------------------------------*\
     *                            Load/store                                 *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler load(Local variable) {
        System.err.print("[" + instrCount++ + "]\t" + "Load(");
        ((XLocal)variable).print();
        System.err.println(")");
        return super.load(variable);
    }

    /**
     * @see Compiler
     */
    public Compiler store(Local local) {
        System.err.print("[" + instrCount++ + "]\t" + "Store(");
        ((XLocal)local).print();
        System.err.println(")");
        return super.store(local);
    }

    /**
     * @see Compiler
     */
    public Compiler read(Type type) {
        System.err.print("[" + instrCount++ + "]\t" + "Read(");
        type.print();
        System.err.println(")");
        return super.read(type);
    }

    /**
     * @see Compiler
     */
    public Compiler write(Type type) {
        System.err.print("[" + instrCount++ + "]\t" + "Write(");
        type.print();
        System.err.println(")");
        return super.write(type);
    }

    /**
     * @see Compiler
     */
    public Compiler literal(int n) {
        System.err.println("[" + instrCount++ + "]\t" + "Literal(" + n + ")");
        return super.literal(n);
    }

    /**
     * @see Compiler
     */
    public Compiler literal(long n) {
        System.err.println("[" + instrCount++ + "]\t" + "Literal(" + n + ")");
        return super.literal(n);
    }

    /**
     * @see Compiler
     */
    public Compiler literal(float n) {
        System.err.println("[" + instrCount++ + "]\t" + "Literal(float)");
        return super.literal(n);
    }

    /**
     * @see Compiler
     */
    public Compiler literal(double n) {
        System.err.println("[" + instrCount++ + "]\t" + "Literal(double)");
        return super.literal(n);
    }

    /**
     * @see Compiler
     */
    public Compiler literal(boolean n) {
        System.err.println("[" + instrCount++ + "]\t" + "Literal(" + n + ")");
        return super.literal(n);
    }

    /**
     * @see Compiler
     */
    public Compiler literal(Object obj) {
        System.err.println("[" + instrCount++ + "]\t" + "Literal(obj)" );
        return super.literal(obj);
    }

    /**
     * @see Compiler
     */
    public Compiler symbol(String name) {
        System.err.println("[" + instrCount++ + "]\t" + "Symbol(" + name + ")");
        return super.symbol(name);
    }

    /**
     * @see Compiler
     */
    public Compiler literal(Label label) {
        System.err.print("[" + instrCount++ + "]\t" + "Literal(");
        ((XLabel)label).print();
        System.err.println(")");
        return super.literal(label);
    }

    /**
     * @see Compiler
     */
    public Compiler data(Label label, Object obj) {
        System.err.print("[" + instrCount++ + "]\t" + "Data(");
        ((XLabel)label).print();
        System.err.println(", obj)");
        return super.data(label, obj);
    }

    /**
     * @see Compiler
     */
    public Compiler dup() {
        System.err.println("[" + instrCount++ + "]\t" + "Dup");
        return super.dup();
    }

    /**
     * @see Compiler
     */
    public Compiler dupReceiver() {
        System.err.println("[" + instrCount++ + "]\t" + "DupReceiver");
        return super.dupReceiver();
    }

    /**
     * @see Compiler
     */
    public Compiler drop() {
        System.err.println("[" + instrCount++ + "]\t" + "Drop");
        return super.drop();
    }

    /**
     * @see Compiler
     */
    public Compiler dumpAll() {
        System.err.println("[" + instrCount++ + "]\t" + "DumpAll");
        return super.dumpAll();
    }

    /**
     * @see Compiler
     */
    public Compiler stackCheck() {
        System.err.println("[" + instrCount++ + "]\t" + "StackCheck");
        return super.stackCheck();
    }

    /**
     * @see Compiler
     */
    public Compiler swap() {
        System.err.println("[" + instrCount++ + "]\t" + "Swap");
        return super.swap();
    }

    /**
     * @see Compiler
     */
    public Compiler swapAll() {
        System.err.println("[" + instrCount++ + "]\t" + "SwapAll");
        return super.swapAll();
    }

    /**
     * @see Compiler
     */
    public Compiler swapForABI() {
        System.err.println("[" + instrCount++ + "]\t" + "SwapForABI");
        return super.swapForABI();
    }

    /**
     * @see Compiler
     */
    public Compiler push() {
        System.err.println("[" + instrCount++ + "]\t" + "Push");
        return super.push();
    }

    /**
     * @see Compiler
     */
    public Compiler pop(Type type) {
        System.err.print("[" + instrCount++ + "]\t" + "Pop(");
        type.print();
        System.err.println(")");
        return super.pop(type);
    }

    /**
     * @see Compiler
     */
    public Compiler popAll() {
        System.err.println("[" + instrCount++ + "]\t" + "PopAll");
        return super.popAll();
    }

    /**
     * @see Compiler
     */
    public Compiler peekReceiver() {
        System.err.println("[" + instrCount++ + "]\t" + "PeekReceiver");
        return super.peekReceiver();
    }

    /*-----------------------------------------------------------------------*\
     *               Type transformations: Force and Convert                 *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler force(Type to) {
        System.err.print("[" + instrCount++ + "]\t" + "Force(");
        to.print();
        System.err.println(")");
        return super.force(to);
    }

    /**
     * @see Compiler
     */
    public Compiler convert(Type to) {
        System.err.print("[" + instrCount++ + "]\t" + "Convert(");
        to.print();
        System.err.println(")");
        return super.convert(to);
    }

    /*-----------------------------------------------------------------------*\
     *                   Arithmetic and logical instructions                 *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler add() {
        System.err.println("[" + instrCount++ + "]\t" + "Add");
        return super.add();
    }

    /**
     * @see Compiler
     */
    public Compiler sub() {
        System.err.println("[" + instrCount++ + "]\t" + "Sub");
        return super.sub();
    }

    /**
     * @see Compiler
     */
    public Compiler mul() {
        System.err.println("[" + instrCount++ + "]\t" + "Mul");
        return super.mul();
    }

    /**
     * @see Compiler
     */
    public Compiler div() {
        System.err.println("[" + instrCount++ + "]\t" + "Div");
        return super.div();
    }

    /**
     * @see Compiler
     */
    public Compiler rem() {
        System.err.println("[" + instrCount++ + "]\t" + "Rem");
        return super.rem();
    }

    /**
     * @see Compiler
     */
    public Compiler and() {
        System.err.println("[" + instrCount++ + "]\t" + "And");
        return super.and();
    }

    /**
     * @see Compiler
     */
    public Compiler or() {
        System.err.println("[" + instrCount++ + "]\t" + "Or");
        return super.or();
    }

    /**
     * @see Compiler
     */
    public Compiler xor() {
        System.err.println("[" + instrCount++ + "]\t" + "Xor");
        return super.xor();
    }

    /**
     * @see Compiler
     */
    public Compiler shl() {
        System.err.println("[" + instrCount++ + "]\t" + "Shl");
        return super.shl();
    }

    /**
     * @see Compiler
     */
    public Compiler shr() {
        System.err.println("[" + instrCount++ + "]\t" + "Shr");
        return super.shr();
    }

    /**
     * @see Compiler
     */
    public Compiler ushr() {
        System.err.println("[" + instrCount++ + "]\t" + "Ushr");
        return super.ushr();
    }

    /**
     * @see Compiler
     */
    public Compiler neg() {
        System.err.println("[" + instrCount++ + "]\t" + "Neg");
        return super.neg();
    }

    /**
     * @see Compiler
     */
    public Compiler com() {
        System.err.println("[" + instrCount++ + "]\t" + "Com");
        return super.com();
    }

    /*-----------------------------------------------------------------------*\
     *                       Comparison instructions                         *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler eq() {
        System.err.println("[" + instrCount++ + "]\t" + "Eq");
        return super.eq();
    }

    /**
     * @see Compiler
     */
    public Compiler ne() {
        System.err.println("[" + instrCount++ + "]\t" + "Ne");
        return super.ne();
    }

    /**
     * @see Compiler
     */
    public Compiler le() {
        System.err.println("[" + instrCount++ + "]\t" + "Le");
        return super.le();
    }

    /**
     * @see Compiler
     */
    public Compiler lt() {
        System.err.println("[" + instrCount++ + "]\t" + "Lt");
        return super.lt();
    }

    /**
     * @see Compiler
     */
    public Compiler ge() {
        System.err.println("[" + instrCount++ + "]\t" + "Ge");
        return super.ge();
    }

    /**
     * @see Compiler
     */
    public Compiler gt() {
        System.err.println("[" + instrCount++ + "]\t" + "Gt");
        return super.gt();
    }

    /*-----------------------------------------------------------------------*\
     *                         Branch instructions                           *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler br(Label label) {
        System.err.print("[" + instrCount++ + "]\t" + "Br(");
        ((XLabel)label).print();
        System.err.println(")");
        return super.br(label);
    }

    /**
     * @see Compiler
     */
    public Compiler bt(Label label) {
        System.err.print("[" + instrCount++ + "]\t" + "Bt(");
        ((XLabel)label).print();
        System.err.println(")");
        return super.bt(label);
    }

    /**
     * @see Compiler
     */
    public Compiler bf(Label label) {
        System.err.print("[" + instrCount++ + "]\t" + "Bf(");
        ((XLabel)label).print();
        System.err.println(")");
        return super.bf(label);
    }

    /**
     * @see Compiler
     */
    public Compiler br(int dst) {
        System.err.println("[" + instrCount++ + "]\t" + "Br(" + dst + ")");
        return super.br(dst);
    }

    /**
     * @see Compiler
     */
    public Compiler bt(int dst) {
        System.err.println("[" + instrCount++ + "]\t" + "Bt(" + dst + ")");
        return super.bt(dst);
    }

    /**
     * @see Compiler
     */
    public Compiler bf(int dst) {
        System.err.println("[" + instrCount++ + "]\t" + "Bf(" + dst + ")");
        return super.bf(dst);
    }

    /**
     * @see Compiler
     */
    public Compiler jump() {
        System.err.println("[" + instrCount++ + "]\t" + "Jump");
        return super.jump();
    }

    /*-----------------------------------------------------------------------*\
     *                    Function call and return instructions              *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler call(int nparms, Type type, int convention) {
        System.err.print("[" + instrCount++ + "]\t" + "Call(" + nparms + ", ");
        type.print();
        System.err.println(", " + convention + ")");
        return super.call(nparms, type, convention);
    }

    /**
     * @see Compiler
     */
    public Compiler call(int nparms, Type type) {
        System.err.print("[" + instrCount++ + "]\t" + "Call(" + nparms + ", ");
        type.print();
        System.err.println(")");
        return super.call(nparms, type);
    }

    /**
     * @see Compiler
     */
    public Compiler call(Type type) {
        System.err.print("[" + instrCount++ + "]\t" + "Call(");
        type.print();
        System.err.println(")");
        return super.call(type);
    }

    /**
     * @see Compiler
     */
    public Compiler call(Type type, int convention) {
        System.err.print("[" + instrCount++ + "]\t" + "Call(");
        type.print();
        System.err.println(", " + convention + ")");
        return super.call(type, convention);
    }

    /**
     * @see Compiler
     */
    public Compiler ret() {
        System.err.println("[" + instrCount++ + "]\t" + "Ret");
        return super.ret();
    }

    /**
     * @see Compiler
     */
    public Compiler ret(Type returnType) {
        System.err.print("[" + instrCount++ + "]\t" + "Ret(");
        returnType.print();
        System.err.println(")");
        return super.ret(returnType);
    }

    /**
     * @see Compiler
     */
    public Compiler framePointer() {
        System.err.println("[" + instrCount++ + "]\t" + "FramePointer");
        return super.framePointer();
    }

    /*-----------------------------------------------------------------------*\
     *                Machine-specific information instructions              *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public int getFramePointerByteOffset(int fp_value) {
        System.err.println("[" + instrCount++ + "]\t" + "GetFramePointerByteOffset(" +
                           fp_value + ")");
        return super.getFramePointerByteOffset(fp_value);
    }

    /**
     * @see Compiler
     */
    public boolean loadsMustBeAligned() {
        System.err.println("[" + instrCount++ + "]\t" + "LoadsMustBeAligned");
        return super.loadsMustBeAligned();
    }

    /**
     * @see Compiler
     */
    public boolean isBigEndian() {
        System.err.println("[" + instrCount++ + "]\t" + "IsBigEndian");
        return super.isBigEndian();
    }

    /**
     * @see Compiler
     */
    public boolean tableSwitchPadding() {
        System.err.println("[" + instrCount++ + "]\t" + "TableSwitchPadding");
        return super.tableSwitchPadding();
    }

    /**
     * @see Compiler
     */
    public boolean tableSwitchEndPadding() {
        System.err.println("[" + instrCount++ + "]\t" + "TableSwitchEndPadding");
        return tableSwitchEndPadding();
    }

}

