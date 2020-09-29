/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.util.Hashtable; // Version without synchronization
import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.compiler.Type;
import com.sun.squawk.util.Stack;
import com.sun.squawk.compiler.ShadowStack;
import com.sun.squawk.compiler.SymbolicValueDescriptor;
import com.sun.squawk.compiler.asm.x86.*;


/**
 * Implementation of the Compiler interface for the X86 architecture.
 *
 * @author Cristina Cifuentes
 */

class X86Compiler extends AbstractX86Compiler implements Compiler, Types, Codes, ShadowStackConstants, Constants {

    /**
     *  The shadow stack automaton.
     */
    private static ShadowStack shadowStk;

    /**
     * The parameters for this compilation
     */
    private static Stack parameters;

    /**
     * The locals for this compilation
     */
    private static Stack locals;

    /**
     * The count of bytes allocated in the stack frame for locals (local
     * variables and temporaries).  This count is the sum of each of the
     * individual byte counts for elements in the locals stack.
     */
    private static int localsOffset;

    /**
     * Result type for this compilation
     */
    private static Type result;

    /**
     * The current scope (0 if none)
     */
    private static int currentScope;

    /**
     * Array holding the relocation information for the compilation unit.
     */
    private int[] relocationInfo;


    /**
     * Hashtable of labels (Label) to shadow stacks (ShadowStack);
     * information to be used to overwrite the shadow stack at the bind
     * location of the label.
     */
    private Hashtable forwardBranchInfo;


  /*-----------------------------------------------------------------------*\
   *                               Constructor                             *
  \*-----------------------------------------------------------------------*/

    /**
     * Create a new compiler for a given architecture.  Debugging support is
     * available when the debug parameter is true.
     */
    public X86Compiler(String arch, boolean debug) {
        Assert.that(arch != null, "Property \"com.sun.squawk.compiler.architecture\" not defined");
        shadowStk = new ShadowStack(debug);
        parameters = new Stack();   // of XLocal
        locals = new Stack();       // of XLocal
        result = VOID;              // no type
        currentScope = 0;
        forwardBranchInfo = new Hashtable();
    }

    /**
     * Create a new compiler.
     */
    public X86Compiler(boolean debug) {
        //this(System.getProperty("com.sun.squawk.compiler.architecture"));
        this("X86", debug); // TEMP
    }

    /**
     * Create a new compiler with no debugging support.
     */
     public X86Compiler() {
        //this(System.getProperty("com.sun.squawk.compiler.architecture"));
        this("X86", false); // TEMP
    }


    /*-----------------------------------------------------------------------*\
     *                         Code generation options                       *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public int getMPOffset() {
        throw new Error();
    }

    /**
     * @see Compiler
     */
    public int getIPOffset() {
        throw new Error();
    }

    /**
     * @see Compiler
     */
    public int getLPOffset() {
        throw new Error();
    }

    /**
     * @see Compiler
     */
    public int getJumpSize() {
        return instr.getJumpSize();
    }

    /**
     * @see Compiler
     */
    public int getJumpByte(int bytecodes, int interp, int offset) {
        return instr.getJumpByte(bytecodes, interp, offset);
    }

    /**
     * @see Compiler
     *
     * @todo This method needs an update for 64-bit compilation.
     */
    public Compiler alloca() {
        SymbolicValueDescriptor numBytes = (SymbolicValueDescriptor)shadowStk.pop();
        instr.alloca(numBytes);
        shadowStk.push(new SymbolicRegister32(ESP, REF));
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler deadCode() {
        return this;
    }

    /*-----------------------------------------------------------------------*\
     *                               Compilation                             *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler compile() {
        // code has been generated on the fly w/o optimizations using an
        // on-the-fly register allocator

        // need to check that the shadow stack is empty
        if (! shadowStk.isEmpty()) {
            throw new RuntimeException("At the end of the compilation process " +
                                       "the evaluation stack is not empty");
        }

        // relocate target addresses to being relative to 0
        relocationInfo = instr.relocate(0);

        //** after linking, we should disassemble; this should be moved elsewhere
        instr.decode();

        return this;
    }

    /**
     * @see Compiler
     */
    public int getCodeSize() {
        return instr.getCodeSize();
    }

    /**
     * @see Compiler
     */
    public byte[] getCode() {
        return instr.getCode();
    }

    /**
     * @see Compiler
     */
    public int[] getRelocationInfo() {
        return relocationInfo;
    }

    /**
     * @see Compiler
     */
    public Hashtable getFixupInfo() {
        return instr.getFixupInfo();
    }

    /*-----------------------------------------------------------------------*\
     *-----------------------------------------------------------------------*
     *                           Instructions                                *
     *-----------------------------------------------------------------------*
    \*-----------------------------------------------------------------------*/

    Instr instr = new Instr(this);

    /*-----------------------------------------------------------------------*\
     *                            Label management                           *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Label label() {
        return instr.label();
    }

    /**
     * @see Compiler
     */

    /*
     * Check whether there is an entry in the forwardBranchesInfo table for
     * this label, if so, overwrite the shadow stack with the information in
     * the table as this is the target of a forward conditional branch that
     * can have something on the stack.
     */
    public Compiler bind(Label label) {
        ShadowStack stackcp = (ShadowStack)forwardBranchInfo.get(label);
        if (stackcp != null) { // the label was in the hash table
            shadowStk = stackcp;
        }

        instr.bind(label);
        return this;
    }

    /*-----------------------------------------------------------------------*\
     *                           Function definition                         *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler enter(Label label, int preambleCode) {
        // restore compiler state
        parameters.removeAllElements();
        locals.removeAllElements();
        localsOffset = 0;
        result = VOID;
        currentScope = 0;

        // emit prologue code
        instr.enter(label, preambleCode);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler enter() {
        return enter(null, Compiler.E_NONE);
    }

    /**
     * @see Compiler
     */
    public Compiler enter(Label label) {
        return enter(label, Compiler.E_NONE);
    }

    /**
     * @see Compiler
     */
    public Compiler enter(int preambleCode) {
        return enter(null, preambleCode);
    }

    /**
     * @see Compiler
     */
    public Local parm(Type type, int hint) {
        /** missing check for type of local var ***/

        int offset = numParamBytes();
        Local param = instr.parm(type, offset);
        parameters.push(param);
        return param;
    }

    /**
     * @see Compiler
     */
    public Local parm(Type type) {
        return parm(type, P_LOW);
    }

    /**
     * @see Compiler
     *
     * @todo Need to check that it's primary?
     */
    public Compiler result(Type type) {
        result = type;
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler leave(MethodMap mmap) {
        if (currentScope != 0) {
            throw new RuntimeException("Expecting end of scope");
        }
        instr.leave(localsOffset);
        if (mmap != null) { // *** this needs to be for the runtime stack ***
            mmap.setup(getNumberLocalSlots(), getLocalOopMap(),
                       getNumberParameterSlots(), getParameterOopMap());
        }
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler leave() {
        leave(null);
        return this;
    }

    /*-----------------------------------------------------------------------*\
     *                           Scope definition                            *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler begin() {
        currentScope++;
        return this;
    }

    /**
     * @see Compiler
     */

    /*
     * This method builds a list of locals, each Local stores its offset,
     * in bytes, on the stack frame.  The first Local has an offset of 0,
     * the next one will have an offset of the size of the type of the
     * first Local, and so on.
     */
    public Local local(Type type, int hint) {
        Local loc = instr.local(type, localsOffset);
        localsOffset += type.getStructureSize();
        locals.push(loc);
        return loc;
    }

    /**
     * @see Compiler
     */
    public Local local(Type type) {
        return local(type, P_LOW);
    }

    /**
     * @see Compiler
     *
     * @todo Need to clear up locals to this scope
     */
    public Compiler end() {
        currentScope--;
        return this;
    }

    /**
     * @see Compiler
     */
    public int getLocalCount() {
        return locals.size();
    }

    /**
     * @see Compiler
     */
    public Type tosType() {
        throw new RuntimeException("tosType not implemented");
    }

    /**
     * @see Compiler
     */
    public Compiler loadParm() {
        SymbolicValueDescriptor index = (SymbolicValueDescriptor) shadowStk.pop();
        if (index.getType() != INT)
            throw new RuntimeException("Load parameter: expecting integer type for index");
        SymbolicValueDescriptor val = instr.loadParm(index);
        shadowStk.push(val);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler storeParm() {
        SymbolicValueDescriptor index = (SymbolicValueDescriptor) shadowStk.pop();
        if (index.getType() != INT)
            throw new RuntimeException("Store parameter: expecting integer type for index");
        SymbolicValueDescriptor value = (SymbolicValueDescriptor) shadowStk.pop();
        instr.storeParm(index, value);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler comment(String str) {
        instr.comment(str);
        return this;
    }

    /*-----------------------------------------------------------------------*\
     *                            Load/store                                 *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler load(Local variable) {
        instr.load(variable);
        SymbolicLocal slocal = new SymbolicLocal(variable);
        shadowStk.push(slocal);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler store(Local local) {
        SymbolicValueDescriptor tos = (SymbolicValueDescriptor)shadowStk.pop();
        Assert.that(((XLocal)local).getType().isPrimary() || ((XLocal)local).getType().isPointer(),
                    "Store: local must hold a primary type or be a pointer");
        Assert.that(tos.getType().isPrimary() || tos.getType().isPointer(),
                    "Store: type of value to be stored must be primary or pointer");
        instr.store(local, tos);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler read(Type type) {
        SymbolicValueDescriptor addr = (SymbolicValueDescriptor)shadowStk.pop();
        Assert.that(addr.getType().isPointer(),
                    "Read: expecting value of type reference.");
        Assert.that(type.isPrimary() || type.isSecondary(),
                    "Read: expecting value of primary or secondary type.");
        SymbolicValueDescriptor value = instr.read(type, addr);
        shadowStk.push(value);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler write(Type type) {
        SymbolicValueDescriptor addr = (SymbolicValueDescriptor)shadowStk.pop();
        SymbolicValueDescriptor val = (SymbolicValueDescriptor)shadowStk.pop();
        Assert.that(addr.getType().getPrimitiveType() == REF,
                    "Write: top of stack value should be of type reference.");
        Assert.that(type.getPrimitiveType() == val.getType().getPrimitiveType(),
                    "Write: value to be stored has different primitive type than given type.");
        instr.write(val, addr, type);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler literal(int n) {
        SymbolicLiteral sliteral = new SymbolicLiteral32(n, INT);
        shadowStk.push(sliteral);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler literal(long n) {
        SymbolicLiteral slong = new SymbolicLiteral64(n, LONG);
        shadowStk.push(slong);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler literal(float n) {
        throw new Error();
    }

    /**
     * @see Compiler
     */
    public Compiler literal(double n) {
        throw new Error();
    }

    /**
     * @see Compiler
     */
    public Compiler literal(boolean n) {
        literal(n ? 1 :0);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler literal(Object obj) {
        SymbolicObject sobject = new SymbolicObject(obj);
        shadowStk.push(sobject);
        Label lab = label();
        data(lab, obj);  // so that label is bound to the object
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler symbol(String name) {
        SymbolicFixupSymbol sname = new SymbolicFixupSymbol(name);
        shadowStk.push(sname);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler literal(Label label) {
        SymbolicLabel slabel = new SymbolicLabel(label);
        shadowStk.push(slabel);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler data(Label label, Object obj) {
        instr.data(label, obj);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler dup() {
        SymbolicValueDescriptor tos = (SymbolicValueDescriptor)shadowStk.pop();
        SymbolicValueDescriptor dup = instr.dup(tos);
        shadowStk.push(dup);
        shadowStk.push(tos);
        return this;
    }

    /**
     * @see Compiler
     */

    /*
     * Depending on the ABI, parameters are pushed in Java order (left to right)
     * or in right to left order. The first order would have the receiver at the
     * bottom of the stack, whereas the latter order would have it at the top of
     * the stack.
     *
     * Example:
     *     class F {...}
     *     F z = new F();
     *     z.f(1);
     * A VM would invoke the above method f() as follows:
     *     z.f(z, 1);
     * therefore, z is used twice.
     *
     * Using Java order parameter passing, we have:
     *     shadowStk = {z, 1}
     * i.e., z is at the bottom of the stack.
     * Using right to left order parameter passing, we have:
     *     shadowStk = {1, z}
     * i.e., z is at the top of the stack.
     */
    public Compiler dupReceiver() {
        if (! instr.javaOrder()) {
            return dup();
        }

        /* The assumption is that the stack only contains parameters at this
         * point of time, therefore, we get the last parameter and we dup it */
        SymbolicValueDescriptor receiver = (SymbolicValueDescriptor)shadowStk.elementAt(0);
        SymbolicValueDescriptor dup = instr.dup(receiver);
        shadowStk.push(dup);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler drop() {
        SymbolicValueDescriptor tos = (SymbolicValueDescriptor)shadowStk.pop();
        instr.drop(tos);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler dumpAll() {
        shadowStk.removeAllElements();
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler stackCheck() {
        SymbolicValueDescriptor extraStack = (SymbolicValueDescriptor)shadowStk.pop();
        Assert.that(extraStack.getType() == INT,
                    "Stack check: expecting integer value for extra stack space");
        SymbolicValueDescriptor extraLocals = (SymbolicValueDescriptor)shadowStk.pop();
        Assert.that(extraLocals.getType() == INT,
                    "Stack check: expecting integer value for extra locals space");
        instr.stackCheck(extraStack, extraLocals);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler swap() {
        SymbolicValueDescriptor val1 = (SymbolicValueDescriptor)shadowStk.pop();
        SymbolicValueDescriptor val2 = (SymbolicValueDescriptor)shadowStk.pop();
        shadowStk.push(val1);
        shadowStk.push(val2);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler swapAll() {
        if (shadowStk.size() > 1) {
            Object[] data = new Object[shadowStk.size()];
            shadowStk.copyInto(data);
            shadowStk.removeAllElements();
            for (int i = data.length - 1 ; i >= 0  ; --i) {
                shadowStk.push(data[i]);
            }
        }
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler swapForABI() {
        return swapAll(); // True for x86
    }

    /**
     * @see Compiler
     */
    public Compiler push() {
        SymbolicValueDescriptor value = (SymbolicValueDescriptor)shadowStk.pop();
        instr.push(value);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler pop(Type type) {
        SymbolicValueDescriptor value = instr.pop(type);
        shadowStk.push(value);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler popAll() {
        instr.popAll();
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler peekReceiver() {
        SymbolicValueDescriptor oopValue = instr.peekReceiver();
        shadowStk.push(oopValue);
        return this;
    }

    /*-----------------------------------------------------------------------*\
     *               Type transformations: Force and Convert                 *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler force(Type to) {
        SymbolicValueDescriptor tos = (SymbolicValueDescriptor)shadowStk.pop();
        Type srcType = tos.getType();

        /**
         * Check force rules 1-3
         */

        if (! srcType.isPrimary()) {
            throw new RuntimeException("Illegal force: source type is not a primary type");
        }
        if (! to.isPrimary()) {
            throw new RuntimeException("Illegal force: destination type is not a primary type");
        }
        if (srcType.getStructureSize() != to.getStructureSize()) {
            throw new RuntimeException("Illegal force: source and destination types are of different size");
        }

        /**
         * Invoke the instruction class to check rule 4 based on the "type" of
         * symbolic value descriptor at hand.  Return the appropriate new forced value.
         */
        SymbolicValueDescriptor forcedValue;
        if (srcType == to) {
            forcedValue = tos;
        } else {
            if (srcType == REF) {
                forcedValue = instr.forceRefTo(tos, to);
            }
            else if (srcType == OOP) {
                forcedValue = instr.forceOopTo(tos, to);
            }
            else if (srcType == INT) {
                forcedValue = instr.forceIntTo(tos, to);
            }
            else if (srcType == UINT) {
                forcedValue = instr.forceUintTo(tos, to);
            }
            else if (srcType == LONG) {
                forcedValue = instr.forceLongTo(tos, to);
            }
            else if (srcType == ULONG) {
                forcedValue = instr.forceUlongTo(tos, to);
            }
            /* if [FLOATS] */
            else if ( (srcType == FLOAT) || (srcType == DOUBLE)) {
                throw new Error();
                    // "Force from float or double not implemented"
            }
            /* end [FLOATS] */
            else {
                throw new RuntimeException("Force: source type is illegal");
            }
        }
        shadowStk.push(forcedValue);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler convert(Type to) {
        SymbolicValueDescriptor tos = (SymbolicValueDescriptor) shadowStk.pop();
        Type srcType = tos.getType();

        /**
         * Check convert rules 1-2
         */

        if (!srcType.isPrimary()) {
            throw new RuntimeException(
                "Illegal convert: source type is not a primary type");
        }
        if (srcType != INT && ! to.isPrimary()) {
            throw new RuntimeException(
                "Illegal convert: destination type is not a primary type");
        }

        /**
         * Invoke the instruction class to check rule 3 based on the "type" of
         * symbolic value descriptor at hand.  Return the appropriate new converted value.
         */
        SymbolicValueDescriptor convertedValue;
        if (srcType == to) {
            convertedValue = tos;
        }
        else {
            if (srcType == INT) {
                convertedValue = instr.convertIntTo(tos, to);
            }
            else if (srcType == UINT) {
                convertedValue = instr.convertUintTo(tos, to);
            }
            else if (srcType == LONG) {
                convertedValue = instr.convertLongTo(tos, to);
            }
            else if (srcType == ULONG) {
                convertedValue = instr.convertUlongTo(tos, to);
            }
            /* if [FLOATS] */
            else if ( (srcType == FLOAT) || (srcType == DOUBLE)) {
                throw new Error();
                    // "Convert from float or double not implemented"
            }
            /* end [FLOATS] */
            else {
                throw new RuntimeException("Convert: source type is illegal");
            }
        }
        shadowStk.push(convertedValue);
        return this;
    }

    /*-----------------------------------------------------------------------*\
     *                   Arithmetic and logical instructions                 *
    \*-----------------------------------------------------------------------*/

    /**
     * Gets the top two operands from the shadow stack and applies an arithmetic
     * or logical operation based on the opcode.  Pushes the result onto the shadow stack.
     *
     * @param opcode the opcode to apply
     */
    private void arithmeticLogical(int opcode) {
        SymbolicValueDescriptor op2 = (SymbolicValueDescriptor)shadowStk.pop();
        SymbolicValueDescriptor op1 = (SymbolicValueDescriptor)shadowStk.pop();

        if (op1.getType().isPointer()) {
            Assert.that(opcode == OP_ADD || opcode == OP_SUB,
                        "Arithmetic: pointer type can only be used in add() or sub() instructions");
            Assert.that(op2.getType() == INT,
                        "Arithmetic add/sub: expecting second operand to be of integer type; first operand is a pointer");
        }
        else if (op2.getType().isPointer()) {
            Assert.that(opcode == OP_ADD || opcode == OP_SUB,
                        "Arithmetic: pointer type can only be used in add() or sub() instructions");
            Assert.that(op1.getType() == INT,
                        "Add/subtract instruction: expecting first operand type to integer; second operand is a pointer");
        }
        else if (opcode == OP_SHL || opcode == OP_SHR || opcode == OP_USHR) {
            Assert.that(op2.getType() == INT,
                        "Shift instruction: expecting second operand to be of type integer");
        }
        else {
            Assert.that(op1.getType() == op2.getType(),
                        "Arithmetic/logical: expecting operands to be of the same type");
        }

        SymbolicValueDescriptor result = instr.binOp(op1, op2, opcode, shadowStk);
        shadowStk.push(result);
    }

    /**
     * @see Compiler
     */
    public Compiler add() {
        arithmeticLogical(OP_ADD);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler sub() {
        arithmeticLogical(OP_SUB);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler mul() {
        arithmeticLogical(OP_MUL);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler div() {
        arithmeticLogical(OP_DIV);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler rem() {
        arithmeticLogical(OP_REM);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler and() {
        arithmeticLogical(OP_AND);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler or() {
        arithmeticLogical(OP_OR);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler xor() {
        arithmeticLogical(OP_XOR);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler shl() {
        arithmeticLogical(OP_SHL);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler shr() {
        arithmeticLogical(OP_SHR);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler ushr() {
        arithmeticLogical(OP_USHR);
        return this;
    }

    /**
     * Gets the top element of the shadow stack, applies the unary operation based
     * on the opcode, and stores the result back onto the shadow stack.
     *
     * @param opcode   the opcode of a unary instruction
     */
    private void unary(int opcode) {
        SymbolicValueDescriptor op = (SymbolicValueDescriptor)shadowStk.pop();
        SymbolicValueDescriptor result = instr.unaryOp(op, opcode);
        shadowStk.push(result);
    }

    /**
     * @see Compiler
     */
    public Compiler neg() {
        unary(OP_NEG);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler com() {
        unary(OP_COM);
        return this;
    }

    /*-----------------------------------------------------------------------*\
     *                       Comparison instructions                         *
    \*-----------------------------------------------------------------------*/

    /**
     * Gets the top two operands from the shadow stack, applies the comparison
     * operation to the operands based on the opcode, and stores the result of
     * the comparison on the shadow stack.
     * <p>
     * Checks:
     * <ul>
     * <li> The operands must be of primary type.
     * <li> If one of the operands is a pointer, the other operand must be of type integer
     *      and the operation must be either an equality or non-equality comparison.
     * <li> For all other operands, the types of the operands must be the same.
     * </ul>
     *
     * @param opcode the opcode to apply
     *
     * @todo  needs to be updated to 64-bits.
     */
    private void comparison(int opcode) {
        SymbolicValueDescriptor op2 = (SymbolicValueDescriptor)shadowStk.pop();
        SymbolicValueDescriptor op1 = (SymbolicValueDescriptor)shadowStk.pop();

        Type op1type = op1.getType();
        Type op2type = op2.getType();
        Assert.that(op1type.isPrimary() && op2type.isPrimary(),
                    "Compare: operands should be of primary type.");
        if ((op1type.isPointer() && op2type == INT) || (op2type.isPointer() && op1type == INT)) {
            Assert.that(opcode == OP_EQ || opcode == OP_NE,
                        "Compare: operand of pointer type can only be compared with eq() and ne() instructions");
        }
        else {
            Assert.that(op1type == op2type,
                        "Compare: expecting operands to be of the same type.  Received type codes: " +
                        op1type.getTypeCode() + " and " + op2type.getTypeCode());
        }

        SymbolicValueDescriptor result = instr.compare(op1, op2, opcode);
        shadowStk.push(result);
    }

    /**
     * @see Compiler
     */
    public Compiler eq() {
        comparison(OP_EQ);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler ne() {
        comparison(OP_NE);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler le() {
        comparison(OP_LE);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler lt() {
        comparison(OP_LT);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler ge() {
        comparison(OP_GE);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler gt() {
        comparison(OP_GT);
        return this;
    }

    /*-----------------------------------------------------------------------*\
     *                         Branch instructions                           *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     */
    public Compiler br(Label label) {
        if (label.isBound()) {
            Assert.that(shadowStk == null || shadowStk.isEmpty(),
                "Branch to a label: backward branch with non-empty evaluation stack");
        } else { // forward branch
            ShadowStack shadowStkCopy = shadowStk.copy();
            forwardBranchInfo.put(label, shadowStkCopy);
        }
        instr.branch(label);
        return this;
    }

    /**
     * Gets the result on the top of the shadow stack and tests if the condition
     * applies.  If so, jumps to the target address. For backward branches, branches
     * only if the stack is empty.
     *
     * <p>
     * Stack: ..., RESULT -> ...
     * <p>
     *
     * In order to support code for exception handling in the Interpreter, there
     * is need to support forward branches to labels with elements on the stack.
     * In such cases, the fall through case will make use of the existing shadow
     * stack, and the destination label case will need to override its shadow
     * stack with the one that was available at the point of the conditional
     * branch.  Therefore, this method stores the shadow stack in a hash table
     * of labels, for use at bind time of that label.
     *
     * <p>
     * Note that this is the only branch that allows forward branches with elements
     * on the stack.
     *
     * <p>
     * Checks
     * <ul>
     * <li> For a backward (the label is bound) branch, the stack must be empty.
     * <li> The result is 0 or 1, therefore, it's type should be integer.
     * </ul>
     *
     * @param label      the address (label) to branch to
     * @param condition  boolean that determines whether the branch is performed or not
     *
     * @todo  this method is not correct when using a shadow stack-based representation.
     *        Allowing forward branches to carry stack values is not ideal as we now
     *        need to implement merge points.  The current implementation works for
     *        exception handling paths only.
     */
    private void branch(Label label, boolean condition) {
        SymbolicValueDescriptor res = (SymbolicValueDescriptor) shadowStk.pop();

        if (label.isBound()) {
            Assert.that(shadowStk == null || shadowStk.isEmpty(),
                        "Branch to a label: backward branch with non-empty evaluation stack");
        }
        else { // forward branch
            ShadowStack shadowStkCopy = shadowStk.copy();
            forwardBranchInfo.put(label, shadowStkCopy);
        }
        Assert.that(res.getType() == INT,
                    "Branch to a label: expecting result on the evaluation stack to be of type integer");

        instr.branch(label, res, condition);
    }

    /**
     * @see Compiler
     */
    public Compiler bt(Label label) {
        branch(label, true);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler bf(Label label) {
        branch(label, false);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler br(int dst) {
        instr.branch(dst);
        return this;
    }

    /**
     * Get top element of the shadow stack, and perform the branch based on its
     * value (whether it is true or false).
     *
     * <p>
     * Stack: ..., RESULT -> ...
     * <p>
     *
     * Checks
     * <ul>
     * <li> For a backward and forward branch, the stack must be empty.
     * <li> The result is 0 or 1, therefore, it's type should be integer.
     * </ul>
     *
     * @param dst    the target address
     * @param cond   boolean that determines whether this branch is performed or not
     */
    private void branch(int dst, boolean cond) {
        SymbolicValueDescriptor result = (SymbolicValueDescriptor)shadowStk.pop();
        Assert.that(shadowStk == null || shadowStk.isEmpty(),
                    "Branch to a literal: branch with non-empty evaluation stack");
        Assert.that(result.getType() == INT,
                    "Branch to a literal: expecting result on the evaluation stack to be of type integer");
        instr.branch(dst, result, cond);
    }

    /**
     * @see Compiler
     */
    public Compiler bt(int dst) {
        branch(dst, true);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler bf(int dst) {
        branch(dst, false);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler jump() {
        SymbolicValueDescriptor dst = (SymbolicValueDescriptor)shadowStk.pop();
        Assert.that(dst.getType().isPointer(), "Jump: expecting an address");
        Assert.that(shadowStk == null || shadowStk.isEmpty(),
                    "Jump: branch with non-empty evaluation stack");
        instr.jump32(dst);
        return this;
    }

    /*-----------------------------------------------------------------------*\
     *                    Function call and return instructions              *
    \*-----------------------------------------------------------------------*/

    /**
     * @see Compiler
     *
     * @todo This method needs to be updated for 64-bit compilation.
     */

    /*
     * Note that it's the Instruction responsibility to push the return value(s),
     * if any, onto the shadow stack.
     */
    public Compiler call(int nparms, Type type, int convention) {
        Assert.that(nparms <= shadowStk.size(),
                    "Call: not enough parameters on the evaluation stack. " +
                    "Expecting " + nparms + " parameters, stack has " + shadowStk.size() +
                    " elements.");
        SymbolicValueDescriptor address = (SymbolicValueDescriptor)shadowStk.pop();
        nparms--;
        Assert.that(address.getType().getStructureSize() == 4,
                    "Call: expecting address of 32 bits.  Address type code: " +
                    address.getType().getTypeCode() + ".");
        instr.call(address, shadowStk, nparms, type, convention);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler call(int nparms, Type type) {
        call(nparms, type, C_NORMAL);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler call(Type type) {
        call(shadowStk.size(), type);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler call(Type type, int convention) {
        call(shadowStk.size(), type, convention);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler ret() {
        ret(result);
        return this;
    }

    /**
     * @see Compiler
     */
    public Compiler ret(Type returnType) {
        result = returnType;
        if (result != VOID) {
            Assert.that(!shadowStk.isEmpty(),
                        "Return: Runtime stack is empty and return value expected to be returned.");
            SymbolicValueDescriptor retValue = (SymbolicValueDescriptor)shadowStk.pop();
            instr.ret(retValue);
        }
        else {
            Assert.that(shadowStk.isEmpty(),
                        "Return: Runtime stack is not empty and no return value (i.e., void) is expected.");
        }
        return this;
    }

    /**
     * @see Compiler
     *
     * @todo remove x86-specific data
     */
    public Compiler framePointer() {
        SymbolicRegister reg = new SymbolicRegister32(EBP, REF);   // *** x86-specific ***
        shadowStk.push(reg);
        return this;
    }


    /*-----------------------------------------------------------------------*\
     *                    Private helper methods                             *
    \*-----------------------------------------------------------------------*/

    /**
     * Calculates how many bytes are used in parameters.
     *
     * @return   the number of bytes used as parameters.
     */
    private int numParamBytes() {
        int numBytes = 0;
        int numElements = parameters.size();
        for (int i = 0; i < numElements; i++) {
            Local loc = (Local)parameters.elementAt(i);
            Type type = ((XLocal)loc).getType();
            numBytes += type.getStructureSize();
        }
        return numBytes;
    }

    /**
     * Checks the number of Local slots used so far.
     *
     * @return   the number of local slots.
     */
    private int getNumberLocalSlots() {
        return locals.size();
    }

    /**
     * Gets the OOP map table for the locals.
     *
     * @return   the OOP map table for the locals.
     */
    private byte[] getLocalOopMap() {
        return getOopMap(locals);
    }

    /**
     * Gets the number of parameters used so far.
     *
     * @return   the number of parameters.
     */
    private int getNumberParameterSlots() {
        return parameters.size();
    }

    /**
     * Gets the OOP map table for the parameters.
     *
     * @return   the OOP map table for the parameters.
     */
    private byte[] getParameterOopMap() {
        return getOopMap(parameters);
    }

    /**
     * Gets the OOP map table for a given stack.
     *
     * @param setOfLocations   the stack of elements to be processed.
     * @return                 a bitmap with the OOP table for the given stack.
     */
    private byte[] getOopMap(Stack setOfLocations) {
        int size = setOfLocations.size();
        byte[] oopMap = new byte[size+7/8];
        int offset = 0;
        int index = 0;
        for (int i = 0; i < size; i++) {
            XLocal loc = (XLocal)setOfLocations.elementAt(i);
            if (loc.getType() == OOP) {
                oopMap[index] = (byte)(oopMap[index] | (1 << offset));
            } else {
                oopMap[index] = (byte)(oopMap[index] | (0 << offset));
            }
            offset++;
            if (offset == 8) {
                offset = 0;
                index++;
            }
        }
        return oopMap;
    }

}
