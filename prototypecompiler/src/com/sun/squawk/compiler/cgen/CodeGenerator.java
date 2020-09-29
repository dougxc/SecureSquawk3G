/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import com.sun.squawk.util.Stack;     // Version without synchronization
import com.sun.squawk.compiler.*;
import com.sun.squawk.util.Assert;

/**
 * Class that specifies the code generator interface.
 *
 * @author   Nik Shaylor
 */
public abstract class CodeGenerator extends CodeEmitter implements Types {

    /**
     * The compiler object.
     */
    protected XCompiler comp;

    /**
     * The literal pool of data elements to be emitted at the next leave().
     */
    private LiteralData lpool;

    /**
     * List of allocated activation slots for local variables.
     */
    private ActivationSlot localSlots;

    /**
     * List of allocated activation slots for parameters.
     */
    private ActivationSlot parameterSlots;

    /**
     * Initialize the code generator.
     *
     * @param comp the compiler
     */
    public void initialize(Compiler comp) {
        this.comp = (XCompiler)comp;
    }

    /**
     * Reset the compiler.
     */
    final void reset() {
        parameterSlots = null;
        localSlots = null;
        lpool = null;
    }

    /**
     * The start routine called before code generation.
     *
     * @param first the first element of the IR
     */
    public void start(Instruction first) {
    }

    /**
     * Finish the code generation.
     *
     * @return the relocation information array
     */
    abstract public int[] finish();

    /**
     * Get the length of the compiled code.
     *
     * @return the length in bytes
     */
    abstract public int getCodeSize();

    /**
     * Get the code array buffer.
     *
     * @return the code array
     */
    abstract public byte[] getCode();

    /**
     * Test the direction of the runtime stack.
     *
     * @return true is it goes downwards (like x86), false if it does upwards (like SPARC).
     */
    abstract public boolean stackDescends();


    /*-----------------------------------------------------------------------*\
     *                                General                                *
    \*-----------------------------------------------------------------------*/

    /**
     * Test for fatal condition.
     *
     * @param b a boolean condition
     * @param msg the error message
     */
    public static void assume(boolean b, String msg) {
        Assert.that(b, ""+msg);
    }

    /**
     * Test for fatal condition.
     *
     * @param b a boolean condition
     */
    public static void assume(boolean b) {
        Assert.that(b);
    }

    /**
     * Signal a condition that should not occur.
     */
    public static void shouldNotReachHere() {
        Assert.shouldNotReachHere();
    }

    /*-----------------------------------------------------------------------*\
     *                               IR creation                             *
    \*-----------------------------------------------------------------------*/

    /**
     * Create the IR for a unary operation.
     *
     * @param op    the operation code
     * @param p1    the instruction that produces the input value
     * @return      the new instruction
     */
    public Instruction newUnOp(int op, Instruction p1) {
        return new UnOp(op, p1);
    }

    /**
     * Create the IR for a binary operation.
     *
     * @param op    the operation code
     * @param p1    the instruction that produces the first input value
     * @param p2    the instruction that produces the second input value
     * @return      the new instruction
     */
    public Instruction newBinOp(int op, Instruction p1, Instruction p2) {
        return new BinOp(op, p1, p2);
    }

    /**
     * Create the IR for a compare operation.
     *
     * @param op    the operation code
     * @param p1    the instruction that produces the first input value
     * @param p2    the instruction that produces the second input value
     * @return      the new instruction
     */
    public Instruction newCmpOp(int op, Instruction p1, Instruction p2) {
        return new BinOp(op, INT, p1, p2);
    }

    /**
     * Create the IR for a convert operation.
     *
     * @param p1    the instruction that produces the input value
     * @param to    the type to convert to
     * @param conv  true if the value should be convert, false if it should be forced
     * @return      the new instruction
     */
    public Instruction newCvtOp(Instruction p1, Type to, boolean conv) {
        return new CvtOp(p1, to, conv);
    }

    /**
     * Create a dup copier.
     *
     * @param p1    the instruction that produces the input value
     * @param first a reference to the first DupOp, or null if this one is first
     * @return      the new instruction
     */
    public DupOp newDup(Instruction p1, DupOp first) {
        return new DupOp(p1, first);
    }

    /**
     * Create the IR for an alloca.
     *
     * @param p1    the instruction that produces the input value (the size)
     * @return      the new instruction
     */
    public Instruction newAlloca(Instruction p1) {
        return new AllocaOp(p1);
    }

    /**
     * Create the IR for an LoadParm.
     *
     * @param p1    the instruction that produces the parameter index
     * @return      the new instruction
     */
    public Instruction newLoadParm(Instruction p1) {
        return new LoadParmOp(p1);
    }

    /**
     * Create the IR for an StoreParm.
     *
     * @param p1    the instruction that produces the parameter value
     * @param p2    the instruction that produces the parameter index
     * @return      the new instruction
     */
    public Instruction newStoreParm(Instruction p1, Instruction p2) {
        return new StoreParmOp(p1, p2);
    }

    /**
     * Create the IR for a comment.
     *
     * @param str the comment string
     * @return      the new instruction
     */
    public Instruction newComment(String str) {
        return new CommentOp(str);
    }

    /**
     * Create the IR for a load operation.
     *
     * @param local the variable to load
     * @return      the new instruction
     */
    public Instruction newLoad(XLocal local) {
        return new LoadOp(local);
    }

    /**
     * Create the IR for a store operation.
     *
     * @param local the variable to store into
     * @param p1 the instruction producing the input value
     * @return      the new instruction
     */
    public Instruction newStore(Instruction p1, XLocal local) {
        return new StoreOp(p1, local);
    }

    /**
     * Create the IR for a mermory load operation.
     *
     * @param addr the instruction producing the address
     * @param type the type of the load
     * @return      the new instruction
     */
    public Instruction newRead(Instruction addr, Type type) {
        return new ReadOp(addr, type);
    }

    /**
     * Create the IR for a mermory  store operation.
     *
     * @param addr the instruction producing the address
     * @param p1 the instruction producing the input value
     * @param type the type of the store
     * @return      the new instruction
     */
    public Instruction newWrite(Instruction addr, Instruction p1, Type type) {
        return new WriteOp(addr, p1, type);
    }

    /**
     * Create the IR for a literal.
     *
     * @param value the value for the literal
     * @return      the new instruction
     */
    public Instruction newLiteral(int value) {
        return new IntLiteralOp(value, null);
    }

    /**
     * Create the IR for an unresolved literal symbol.
     *
     * @param name  the symbol the literal needs to be linked to
     * @return      the new instruction
     */
    public Instruction newSymbol(String name) {
        return new SymbolOp(name);
    }

    /**
     * Create the IR for a literal.
     *
     * @param value the value for the literal
     * @return      the new instruction
     */
    public Instruction newLiteral(long value) {
        return new LongLiteralOp(value);
    }

    /**
     * Create the IR for a literal.
     *
     * @param value the value for the literal
     * @return      the new instruction
     */
    public Instruction newLiteral(float value) {
        return new FloatLiteralOp(value);
    }

    /**
     * Create the IR for a literal.
     *
     * @param value the value for the literal
     * @return      the new instruction
     */
    public Instruction newLiteral(double value) {
        return new DoubleLiteralOp(value);
    }

    /**
     * Create the IR for a literal.
     *
     * @param array the value for the literal
     * @return      the new instruction
     */
    public Instruction newLiteral(Object array) {
        return new ArrayLiteralOp(array);
    }

    /**
     * Create the IR for a literal.
     *
     * @param label the value for the literal
     * @return      the new instruction
     */
    public Instruction newLiteral(XLabel label) {
        return new LabelLiteralOp(label);
    }

    /**
     * Create the IR for an conditional branch.
     *
     * @param label the branch target
     * @param p1 the contition input value
     * @param cond the contition of the branch
     * @return      the new instruction
     */
    public Instruction newBranch(XLabel label, Instruction p1, boolean cond) {
        return new BranchOp(label, p1, cond);
    }

    /**
     * Create the IR for an conditional branch.
     *
     * @param dst  the branch target absolute destination
     * @param p1   the contition input value
     * @param cond the contition of the branch
     * @return     the new instruction
     */
    public Instruction newBranch(int dst, Instruction p1, boolean cond) {
        return new BranchOp(dst, p1, cond);
    }

    /**
     * Create the IR for an jump instruction.
     *
     * @param addr the instruction producing the address
     * @return      the new instruction
     */
    public Instruction newJump(Instruction addr) {
        return new JumpOp(addr);
    }

    /**
     * Create the IR for an call instruction.
     *
     * @param nparms the number of parameters
     * @param stack the stack from which to pop the input parameters
     * @param returnType the return type
     * @param convention the calling convention
     * @return      the new instruction
     */
    public Instruction newCall(int nparms, Stack stack, Type returnType, int convention) {
        return new CallOp(nparms, stack, returnType, convention);
    }

    /**
     * Specify an unreachable place.
     *
     * @return      the new instruction
     */
    public Instruction newDeadCode() {
        return new DeadCodeOp();
    }

    /**
     * Create the IR for an drop instruction.
     *
     * @param p1 the input value
     * @return      the new instruction
     */
    public Instruction newDrop(Instruction p1) {
        return new DropOp(p1);
    }

    /**
     * Create the IR for a stack check instruction.
     *
     * @param p1 the input value for the number of extra local varible bytes
     * @param p2 the input value for the number of extra stack bytes
     * @return      the new instruction
     */
    public Instruction newStackCheck(Instruction p1, Instruction p2) {
        return new StackCheckOp(p1, p2);
    }

    /**
     * Create the IR for an push instruction.
     *
     * @param p1 the input value
     * @return      the new instruction
     */
    public Instruction newPush(Instruction p1) {
        return new PushOp(p1);
    }

    /**
     * Create the IR for an pop instruction.
     *
     * @param type the data type to pop
     * @return      the new instruction
     */
    public Instruction newPop(Type type) {
        return new PopOp(type);
    }

    /**
     * Create the IR for an pop all instruction.
     *
     * @return      the new instruction
     */
    public Instruction newPopAll() {
        return new PopAllOp();
    }

    /**
     * Create the IR for an peek receiver instruction.
     *
     * @return      the new instruction
     */
    public Instruction newPeekReceiver() {
        return new PeekReceiverOp();
    }

    /**
     * Create the IR for an return instruction.
     *
     * @param p1 the instruction supplying the result (or null if void)
     * @return      the new instruction
     */
    public Instruction newRet(Instruction p1) {
        return new RetOp(p1);
    }

    /**
     * Create a new phi instruction.
     *
     * @param label the label being bound
     * @return      the new instruction
     */
    public Instruction newPhi(XLabel label) {
        return new PhiOp(label);
    }

    /**
     * Create a new enter instruction.
     *
     * @param specialPreamble the special premable code
     * @return      the new instruction
     */
    public Instruction newEnter(int specialPreamble) {
        return new EnterOp(specialPreamble);
    }

    /**
     * Create a new leave instruction.
     *
     * @param  mmap the method map for the function
     * @return      the new instruction
     */
    public Instruction newLeave(MethodMap mmap) {
        return new LeaveOp(mmap);
    }

    /**
     * Create a new local variable allocate instruction.
     *
     * @param local the local to allocate
     * @param originalType the original type
     * @return      the new instruction
     */
    public Instruction newAllocate(XLocal local, Type originalType) {
        return new AllocateOp(local, originalType);
    }

    /**
     * Create a new local variable deallocate instruction.
     *
     * @param local the local to deallocate
     * @return      the new instruction
     */
    public Instruction newDeallocate(XLocal local) {
        return new DeallocateOp(local);
    }

    /**
     * Create a new data allocation instruction.
     *
     * @param label the label to the data
     * @param obj   the array of data
     * @return      the new instruction
     */
    public Instruction newData(XLabel label, Object obj) {
        return new DataOp(label, obj);
    }

    /**
     * Generate code that will result in the value of the frame pointer.
     *
     * @return      the new instruction
     */
    public Instruction newFramePointer() {
        return new FramePointerOp();
    }


    /*-----------------------------------------------------------------------*\
     *                             Local management                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Create a new local.
     *
     * @param type the type of the local
     * @param scope the scope of the local
     * @return a new local
     */
    public XLocal newLocal(Type type, Scope scope) {
        return new XLocal(type, scope);
    }

    /**
     * Create a new parameter local.
     *
     * @param type the type of the local
     * @param offset the fffset in words from the frame pointer to the parameter
     * @return a new local
     */
    public XLocal newParm(Type type, int offset) {
        return new XLocal(type, offset);
    }

    /**
     * Create a new label.
     *
     * @return a new label
     */
    abstract public XLabel newLabel();

    /**
     * Free a stack variable.
     *
     * @param sl the stack local variable to free
     */
    public  void freeLocal(StackLocal sl) {
        sl.freeSlot(this);
    }

    /*-----------------------------------------------------------------------*\
     *                        ActivationSlot management                      *
    \*-----------------------------------------------------------------------*/

    /**
     * Allocate a local variable.
     *
     * @param type the type of the variable
     * @return a local variable
     */
    public ActivationSlot allocSlot(Type type) {
        if (localSlots == null) {
            localSlots = new ActivationSlot(type, 0, false);
            return localSlots;
        }
        return localSlots.alloc(type);
    }

    /**
     * Free an activation slot.
     *
     * @param aslot the slot to be freed
     */
    public void freeSlot(ActivationSlot aslot) {
        aslot.free();
    }

    /**
     * Add an activation parameter slot to the list.
     *
     * @param slot the parameter slot
     */
    public void addParameterSlot(ActivationSlot slot) {
        if (parameterSlots == null) {
            parameterSlots = slot;
        } else {
            parameterSlots.getLast().setNext(slot);
        }
    }

    /**
     * Get the list of local slots.
     *
     * @return the first local slot
     */
    public ActivationSlot getLocalSlots() {
        return localSlots;
    }

    /**
     * Get the list of parameter slots.
     *
     * @return the first parameter slot
     */
    public ActivationSlot getParameterSlots() {
        return parameterSlots;
    }

    /**
     * Get the count of locals allocaed.
     *
     * @return the number allocated
     */
    public int getLocalSlotCount() {
        if (localSlots == null) {
            return 0;
        }
        return localSlots.getTotalSlots();
    }

    /**
     * Get an oopmap for the local variables.
     *
     * @return the oopmap
     */
    public byte[] getLocalOopMap() {
        return getOopMap(localSlots);
    }

    /**
     * Get the count of parameters allocaed.
     *
     * @return the number allocated
     */
    public int getParameterSlotCount() {
        if (parameterSlots == null) {
            return 0;
        }
        return parameterSlots.getTotalSlots();
    }

    /**
     * Get an oopmap for the parameter variables.
     *
     * @return the oopmap
     */
    public byte[] getParameterOopMap() {
        return getOopMap(parameterSlots);
    }

    /**
     * Get an oopmap for the local or parameter variables.
     *
     * @return the oopmap
     */
    private byte[] getOopMap(ActivationSlot slot) {
        if (slot == null) {
            return new byte[0];
        }
        int slots = slot.getTotalSlots();
        byte[] res = new byte[(slots+7)/8];
        int count = 0;
        int offset = 0;
        while (slot != null) {
            if (slot.isOop()) {
                res[offset] = (byte)(res[offset] | (1 << count));
            }
            slot = slot.getNext();
            if (++count == 8) {
                count = 0;
                offset++;
            }
        }
        return res;
    }

    /*-----------------------------------------------------------------------*\
     *                            Literal management                         *
    \*-----------------------------------------------------------------------*/

    /**
     * Allocate a literal.
     *
     * @param obj the array to allocate
     * @return a label to the data
     */
    public XLabel allocLiteral(Object obj) {
        XLabel label = (XLabel)comp.label();
        LiteralData next = new LiteralData(label, obj);
        if (lpool == null) {
            lpool = next;
        } else {
            LiteralData prev = lpool;
            while (prev.getNext() != null) {
                prev = prev.getNext();
            }
            prev.setNext(next);
        }
        return label;
    }

    /**
     * Allocate a literal string.
     *
     * @param s the string to allocate
     * @return a label to the data
     */
    public XLabel allocLiteral(String s) {
        return allocLiteral(s.toCharArray());
    }

    /**
     * Emit the stored literals.
     */
    public void emitLiterals() {
        while (lpool != null) {
            lpool.emit(this);
            lpool = lpool.getNext();
        }
    }


    /*-----------------------------------------------------------------------*\
     *                     Special slot offset information                   *
    \*-----------------------------------------------------------------------*/

    /**
     * Get the offset from the frame pointer to slot used for the MP variable.
     *
     * @return the offset in bytes
     */
    abstract public int getMPOffset();

    /**
     * Get the offset from the frame pointer to slot used for the IP variable.
     *
     * @return the offset in bytes
     */
    abstract public int getIPOffset();

    /**
     * Get the offset from the frame pointer to slot used for the LP variable.
     *
     * @return the offset in bytes
     */
    abstract public int getLPOffset();

    /**
     * Get the length of a jump instruction.
     *
     * @return the length in bytes
     */
    abstract public int getJumpSize();

    /**
     * Get a single byte of a jump instruction sequence.
     *
     * @param bytecodes the address of the bytecode array
     * @param interp the address of the interpreter
     * @param offset the offset to the byte to return
     * @return the byte
     */
    abstract public int getJumpByte(int bytecodes, int interp, int offset);

}
