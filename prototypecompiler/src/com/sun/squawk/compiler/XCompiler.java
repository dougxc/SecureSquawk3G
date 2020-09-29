/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.cgen.*;
import com.sun.squawk.util.Stack;     // Version without synchronization
import com.sun.squawk.util.Hashtable; // Version without synchronization


/**
 * Class representing the compiler interface.
 *
 * @author   Nik Shaylor
 */
public class XCompiler extends AbstractX86Compiler implements Compiler, Types, Codes {

    /**
     * The compiler states.
     */
    private static final int S_READY    = 0,
                             S_ENTER    = 1,
                             S_BODY     = 2,
                             S_COMPILED = 3;

    /**
     * The state of the compiler.
     */
    private int state = S_READY;

    /**
     * The first instruction in the IR.
     */
    private Instruction first;

    /**
     * The last instruction in the IR.
     */
    private Instruction last;

    /**
     * The list of parameter types.
     */
    private Stack parms;

    /**
     * The current code scope (null if not in a scope).
     */
    private Scope currentScope;

    /**
     * The list of local types.
     */
    private Stack locals;

    /**
     * The evaluation stack of instructions.
     */
    private Stack stack;

    /**
     * Offset in words from the frame pointer to the next parameter.
     */
    private int parmOffset;

    /**
     * The return type.
     */
    private Type returnType;

    /**
     * The code generator.
     */
    private CodeGenerator cgen;

    /**
     * Array holding the relocation information for the compilation unit.
     */
    private int[] relocationInfo;

    /**
     * Hashtable holding offsets to unresolved symbols.
     */
    private Hashtable fixupInfo = new Hashtable();

    /**
     * The special preamble code.
     */
    private int specialPreamble = E_NONE;

    /**
     * Flag to say a c.local(MP) was defined.
     */
    private boolean definedMP = false;

    /**
     * Flag to say a c.local(IP) was defined.
     */
    private boolean definedIP = false;

    /**
     * Flag to say a c.local(LP) was defined.
     */
    private boolean definedLP = false;

    /*-----------------------------------------------------------------------*\
     *                                General                                *
    \*-----------------------------------------------------------------------*/

    /**
     * Signal a fatal condition.
     *
     * @param msg the error message
     */
    public static void fatal(String msg) {
        Assert.shouldNotReachHere(""+msg);
    }

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

    /**
     * Test that the evaluation stack is empty.
     */
    public void assumeStackEmpty() {

// temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp
        if (stack != null) {
            if (!stack.isEmpty()) {
                System.err.println("Stack not empty");
                while (!stack.isEmpty()) {
                    Instruction i = spop();
                    System.err.println("    "+i.type().getTypeCode());
                }
                throw new Error();
            }
        }
// temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp temp

        Assert.that(stack == null || stack.isEmpty(), "Evaluation stack is not empty");
    }

    /**
     * Test that complier is in the body of a function.
     */
    public void assumeInBody() {
        Assert.that(state == S_BODY || state == S_ENTER, "Compiler not in body");
    }


    /*-----------------------------------------------------------------------*\
     *                              Construction                             *
    \*-----------------------------------------------------------------------*/

    /**
     * Create a new compiler.
     *
     * @param arch the name of the target architecture to bind to
     */
    public XCompiler(String arch) {
        assume(arch != null, "Property \"com.sun.squawk.compiler.architecture\" not defined");
        try {
            Class clazz = Class.forName("com.sun.squawk.compiler.cgen."+arch+"CodeGenerator");
            cgen = (CodeGenerator)clazz.newInstance();
            cgen.initialize(this);
            return;
        } catch (ClassNotFoundException ex) { fatal(ex.getMessage());
        } catch (InstantiationException ex) { fatal(ex.getMessage());
        } catch (IllegalAccessException ex) { fatal(ex.getMessage());
        } catch (ClassCastException ex) {     fatal(ex.getMessage());
        }
        shouldNotReachHere();
    }

    /**
     * Create a new compiler.
     */
    public XCompiler() {
        //this(System.getProperty("com.sun.squawk.compiler.architecture"));
        this("X86"); // TEMP
    }

    /*-----------------------------------------------------------------------*\
     *                            Label management                           *
    \*-----------------------------------------------------------------------*/

    /**
     * Allocate a label.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @return the label
     */
    public Label label() {
        return cgen.newLabel();
    }

    /**
     * Bind a label to the current location.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param label the label to bind
     * @return the compiler object
     */
    public Compiler bind(Label aLabel) {
        XLabel clabel = (XLabel)aLabel;
        int size = clabel.getStackSize();
        clabel.setScope(currentScope);

        /*
         * Merge the stack state if the previous instruction can fall through.
         */
        if(last != null && last.canFallThrough()) {
            assume(size == stack.size(), "stack size = "+stack.size() + " label size = " + size);
            clabel.merge(stack);
        }

        /*
         * Replace the contents of the stack with the StackMerge objects in the label.
         */
        stack.removeAllElements();
        for (int i = 0 ; i < size ; i++) {
            stack.addElement(clabel.getStackAt(i));
        }
        append(cgen.newPhi(clabel));
        return this;
    }


    /*-----------------------------------------------------------------------*\
     *                         Code generation options                       *
    \*-----------------------------------------------------------------------*/

    /**
     * Get the offset from the frame pointer to slot used for the MP variable.
     *
     * @return the offset in bytes
     */
    public int getMPOffset() {
        return cgen.getMPOffset();
    }

    /**
     * Get the offset from the frame pointer to slot used for the IP variable.
     *
     * @return the offset in bytes
     */
    public int getIPOffset() {
        return cgen.getIPOffset();
    }

    /**
     * Get the offset from the frame pointer to slot used for the LP variable.
     *
     * @return the offset in bytes
     */
    public int getLPOffset() {
        return cgen.getLPOffset();
    }

    /**
     * Get the length of a jump instruction.
     *
     * @return the length in bytes
     */
    public int getJumpSize() {
        return cgen.getJumpSize();
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
        return cgen.getJumpByte(bytecodes, interp, offset);
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
     * @param preambleCode the special preamble code for the function
     * @return the compiler object
     */
    public Compiler enter(Label label, int preambleCode) {
        assume(state == S_READY);
        state = S_ENTER;
        stack  = new Stack();
        parms  = new Stack();
        locals = new Stack();
        currentScope = null;
        specialPreamble = preambleCode;
        definedMP = false;
        definedIP = false;
        returnType = VOID;
        parmOffset = 0;        // First parm is at address 0, second 1, etc.
        if (label != null) {
            bind(label);
        }
        append(cgen.newEnter(specialPreamble));
        return this;
    }

    /**
     * Emit a function prologue.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param label a label to be bound to the start of the function
     * @return the compiler object
     */
    public Compiler enter(Label label) {
        return enter(label, Compiler.E_NONE);
    }

    /**
     * Emit a function prologue.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param preambleCode the special preamble code for the function
     * @return the compiler object
     */
    public Compiler enter(int preambleCode) {
        return enter(null, preambleCode);
    }

    /**
     * Emit a function prologue.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @return the compiler object
     */
    public Compiler enter() {
        return enter(null);
    }

    /**
     * Define a parameter variable.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param type the type of the parameter (Must be primary)
     * @param hint the P_XXXX priority hint.
     * @return the compiler object
     */
    public Local parm(Type type, int hint) {
        assume(state == S_ENTER);
        assumeStackEmpty();
        XLocal local = cgen.newParm(type, parmOffset);
        parmOffset += type.getActivationSize();
        parms.push(local);
        append(cgen.newAllocate(local, type));
        return local;
    }

    /**
     * Define a parameter variable.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param type the type of the parameter (Must be primary)
     * @return the compiler object
     */
    public Local parm(Type type) {
        return parm(type, P_LOW);
    }

    /**
     * Define the function result.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param type the type of the parameter (Must be primary)
     * @return the compiler object
     */
    public Compiler result(Type type) {
        assume(state == S_ENTER);
        state = S_BODY;
        assumeStackEmpty();
        returnType = type;
        begin();
        return this;
    }

    /**
     * Emit a function epilogue.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @return the compiler object
     */
    public Compiler leave(MethodMap mmap) {
        end();
        while (parms.size() > 0) {
            XLocal parm = (XLocal)parms.pop();
            append(cgen.newDeallocate(parm));
        }
        assume(currentScope == null);

        /*
         * Check that MP was defined.
         */
        assume(specialPreamble == E_NONE || definedMP, " MP not defined in compilation");

        /*
         * Add the IR.
         */
        append(cgen.newLeave(mmap));
        state = S_READY;
        return this;
    }

    /**
     * Emit a function epilogue.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @return the compiler object
     */
    public Compiler leave() {
        return leave(null);
    }

    /*-----------------------------------------------------------------------*\
     *                           Scope definition                            *
    \*-----------------------------------------------------------------------*/

    /**
     * Begin a code scope.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @return the compiler object
     */
    public Compiler begin() {
        assumeInBody();
        currentScope = new Scope(currentScope);
        return this;
    }

    /**
     * Define a local variable type.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param type the type of the local variable (Must be primary, or MP, or IP)
     * @param hint the P_XXXX priority hint.
     * @return the compiler object
     */
    public Local local(Type type, int hint) {
        assume(state == S_BODY);
        Type t = type;
        if (type == MP) {
            t = OOP;
            assume(definedMP == false);
            definedMP = true;
        } else if (type == IP) {
            t = REF;
            assume(definedIP == false);
            definedIP = true;
        } else if (type == LP) {
            t = REF;
            assume(definedLP == false);
            definedLP = true;
        }
        XLocal local = cgen.newLocal(t, currentScope);
        locals.push(local);
        append(cgen.newAllocate(local, type));
        return local;
    }

    /**
     * Define a local variable type.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param type the type of the local variable (Must be primary, or MP, or IP)
     * @return the compiler object
     */
    public Local local(Type type) {
        return local(type, P_LOW);
    }

    /**
     * End a code scope.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @return the compiler object
     */
    public Compiler end() {
        assumeInBody();
        while (locals.size() > 0) {
            XLocal local = (XLocal)locals.peek();
            if (local.getScope() != currentScope) {
                break;
            }
            local.kill();
            locals.pop();
            append(cgen.newDeallocate(local));
        }
        currentScope = currentScope.getPrevious();
        return this;
    }

    /**
     * Return the number of locals currently defined.
     *
     * @return the number
     */
    public int getLocalCount() {
        return locals.size();
    }


    /*-----------------------------------------------------------------------*\
     *                               IR functions                            *
    \*-----------------------------------------------------------------------*/

    /**
     * Append a new instruction.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param inst the instriction to be appended
     */
    private void append(Instruction inst) {
        if (!(inst instanceof CommentOp)) {
            assumeInBody();
        }
        if (first == null) {
            first = inst;
        } else {
            last.setNext(inst);
        }
        last = inst;
        if (inst.type() != VOID) {
            stack.push(inst);
        }
    }

    /**
     * Pop an instruction from the compiler's operand stack.
     *
     * <p>
     * Stack: ..., VALUE -> ...
     * <p>
     *
     * @return the top most instrucrtion
     */
    private Instruction spop() {
        return (Instruction)stack.pop();
    }

    /**
     * Return the type of the instruction on the top of the stack.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @return the type
     */
    public Type tosType() {
        Instruction in = (Instruction)stack.peek();
        return in.type();
    }

    /**
     * Add a unary operator.
     *
     * <p>
     * Stack: ..., VALUE -> ..., RESULT
     * <p>
     *
     * @param op the operator type
     */
    private void unop(int op) {
        Instruction p1 = spop();
        append(cgen.newUnOp(op, p1));
    }

    /**
     * Add an arithmetic operator.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @param op the operator type
     */
    private void arithop(int op) {
        Instruction r = spop();
        Instruction l = spop();
        if (r.type().isPointer() && l.type() == INT && (op == OP_ADD || op == OP_SUB)) {
            Instruction temp = r;
            r = l;
            l = temp;
        } else {
            assume(!r.type().isPointer(), "Bad arithmetic types");
            if (l.type().isPointer()) {
                assume(op == OP_ADD || op == OP_SUB, "Bad arithmetic types");
                assume(r.type() == INT, "Bad arithmetic types");
            } else {
                assume(r.type() == l.type(), "Arithmetic types not same");
            }
        }
        append(cgen.newBinOp(op, l, r));
    }

    /**
     * Add a shift operator.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @param op the operator type
     */
    private void shiftop(int op) {
        Instruction r = spop();
        Instruction l = spop();
        assume (r.type() == INT, "Invalid shift operand");
        append(cgen.newBinOp(op, l, r));
    }

    /**
     * Add a logic operator.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @param op the operator type
     */
    private void logicop(int op) {
        Instruction r = spop();
        Instruction l = spop();
        assume (l.type() == r.type(), "Invalid logic operands");
        append(cgen.newBinOp(op, l, r));
    }

    /**
     * Add a compare operator.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @param op the operator type
     */
    private void cmpop(int op) {
        Instruction r = spop();
        Instruction l = spop();
        if (l.type().isPointer() && r.type() == INT) {
            assume (op == OP_EQ || op == OP_NE, "Invalid cmp operands");
        } else {
            assume (l.type() == r.type(), "Invalid cmp operands "+l.type().getTypeCode()+" != "+r.type().getTypeCode());
        }
        append(cgen.newCmpOp(op, l, r));
    }

    /**
     * Add a stack allocation node to the IR.
     *
     * <p>
     * Stack: ..., SIZE -> ...
     * <p>
     *
     * @return the compiler object
     */
    public Compiler alloca() {
        Instruction p1 = spop();
        assume(p1.type() == INT, "alloca a needs an int");
        append(cgen.newAlloca(p1));
        return this;
    }

    /**
     * Get the value of a parameter word.
     *
     * <p>
     * Stack: ..., INDEX, -> ..., INT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler loadParm() {
        Instruction p1 = spop();
        assume(p1.type() == INT, "loadParm a needs an int");
        append(cgen.newLoadParm(p1));
        return this;
    }

    /**
     * Set the value of a parameter word.
     *
     * <p>
     * Stack: ..., VALUE, INDEX -> ...
     * <p>
     *
     * @return the compiler object
     */
    public Compiler storeParm() {
        Instruction p1 = spop();
        Instruction p2 = spop();
        assume(p1.type() == INT, "storeParm a needs an int");
        assume(p2.type() == INT, "storeParm a needs an int");
        append(cgen.newStoreParm(p2, p1));
        return this;
    }

    /**
     * Add a comment node to the IR.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param str the comment
     * @return the compiler object
     */
    public Compiler comment(String str) {
        append(cgen.newComment(str));
        return this;
    }


    /**
     * Get a local variable or parameter and push it onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param local the local variable to load
     * @return the compiler object
     */
    public Compiler load(Local local) {
        XLocal xlocal = (XLocal)local;
        xlocal.checkAlive();
        append(cgen.newLoad(xlocal));
        return this;
    }

    /**
     * Set a local variable or parameter to a value popped from the stack.
     *
     * <p>
     * Stack: ..., VALUE -> ...
     * <p>
     *
     * @param local the local variable to store into
     * @return the compiler object
     */
    public Compiler store(Local local) {
        XLocal xlocal = (XLocal)local;
        Instruction p1 = spop();
        xlocal.checkAlive();
        assume(p1.type() == xlocal.type(), "Cannot store "+p1.type()+" into "+xlocal.type());
        append(cgen.newStore(p1, xlocal));
        return this;
    }

    /**
     * Load a value from a reference. The reference is popped from the stack
     * and the type specified is loaded from that adderss. Secondry types
     * (BYTE, UBYTE, SHORT, USHORT) are all widened to INT by this operation.
     *
     * <p>
     * Stack: ..., REF -> ..., VALUE
     * <p>
     *
     * @param type the type of the data to load
     * @return the compiler object
     */
    public Compiler read(Type type) {
        Instruction ref = spop();
        append(cgen.newRead(ref, type));
        return this;
    }

    /**
     * Store a value at a reference. The value and reference are popped from the stack
     * and the value is written to the referenced address according to the specified type.
     * The type parameter is used to check primary types and to narrow secondry types.
     *
     * <p>
     * Stack: ..., VALUE, REF -> ...
     * <p>
     *
     * @param type the type of the data to load
     * @return the compiler object
     */
    public Compiler write(Type type) {
        Instruction ref = spop();
        Instruction p1  = spop();
        assume(p1.type() == type.getPrimitiveType(), "Bad write type is "+type+" p1="+ p1.type());
        append(cgen.newWrite(ref, p1, type));
        return this;
    }

    /**
     * Push an integer constant onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param n the value of the constant
     * @return the compiler object
     */
    public Compiler literal(int n) {
        append(cgen.newLiteral(n));
        return this;
    }

    /**
     * Push a long constant onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param n the value of the constant
     * @return the compiler object
     */
    public Compiler literal(long n) {
        append(cgen.newLiteral(n));
        return this;
    }

    /**
     * Push a float constant onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param n the value of the constant
     * @return the compiler object
     */
    public Compiler literal(float n) {
        append(cgen.newLiteral(n));
        return this;
    }

    /**
     * Push a double constant onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param n the value of the constant
     * @return the compiler object
     */
    public Compiler literal(double n) {
        append(cgen.newLiteral(n));
        return this;
    }

    /**
     * Push a boolean constant onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param n the value of the constant
     * @return the compiler object
     */
    public Compiler literal(boolean n) {
        return literal(n?1:0);
    }

    /**
     * Push an address an array onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param array the array
     * @return the compiler object
     */
    public Compiler literal(Object array) {
        Assert.that(!(array instanceof String));
        append(cgen.newLiteral(array));
        return this;
    }


    /**
     * Push an address an unresolved literal integer onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param array the array
     * @return the compiler object
     */
    public Compiler symbol(String name) {
        append(cgen.newSymbol(name));
        return this;
    }

    /**
     * Push the address of label onto the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param label the label
     * @return the compiler object
     */
    public Compiler literal(Label label) {
        append(cgen.newLiteral((XLabel)label));
        return this;
    }

    /**
     * Define some data.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param label the label to the data
     * @param obj the array
     * @return the compiler object
     */
    public Compiler data(Label label, Object obj) {
        append(cgen.newData((XLabel)label, obj));
        return this;
    }

    /**
     * Dup the top element of the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @return the compiler object
     */
    public Compiler dup() {
        Instruction p1 = spop();
        DupOp first = cgen.newDup(p1, null);
        append(first);
        append(cgen.newDup(p1, first));
        return this;
    }

    /**
     * Dup the receiver element of the stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @return the compiler object
     */
    public Compiler dupReceiver() {
        if (cgen.stackDescends()) {
            return dup();
        } else {
            begin();
                XLocal receiver = (XLocal)local(OOP);
                Stack oldStack = stack;
                stack = new Stack();
                stack.push(oldStack.elementAt(0));
                store(receiver);
                load(receiver);
                for (int i = 1 ; i < oldStack.size() ; i++) {
                    stack.push(oldStack.elementAt(i));
                }
                load(receiver);
            end();
            return this;
        }
    }

    /**
     * Pop the top element of the compiler stack.
     *
     * <p>
     * Stack: ..., VALUE -> ...
     * <p>
     *
     * @return the compiler object
     */
    public Compiler drop() {
        Instruction p1 = spop();
        append(cgen.newDrop(p1));
        return this;
    }

    /**
     * Pop all the element of the compiler stack.
     *
     * <p>
     * Stack: ... -> _
     * <p>
     *
     * @return the compiler object
     */
    public Compiler dumpAll() {
        while (stack.size() > 0) {
            spop();  //drop();
        }
        return this;
    }

    /**
     * Ensure that there is enough stack (the values are in  bytes).
     *
     * <p>
     * Stack: EXTRA_LOCALS, EXTRA_STACK -> _
     * <p>
     *
     * @return the compiler object
     */
    public Compiler stackCheck() {
        Instruction p1 = spop();
        Instruction p2 = spop();
        assume(p1.type() == INT, "stackCheck a needs an int");
        assume(p2.type() == INT, "stackCheck a needs an int");
        append(cgen.newStackCheck(p1, p2));
        assumeStackEmpty();
        return this;
    }

    /**
     * Swap the contence of the stack.
     *
     * <p>
     * Stack: [VALUE0, ..., VALUEN] -> [VALUEN, ... , VALUE0]
     * <p>
     *
     * @return the compiler object
     */
    public Compiler swap() {
        Instruction p1 = spop();
        Instruction p2 = spop();
        stack.push(p1);
        stack.push(p2);
        return this;
    }

    /**
     * Swap the contence of the stack if the target ABI push parameters right-to-left.
     *
     * <p>
     * Stack: [VALUE0, ..., VALUEN] -> [VALUEN, ... , VALUE0] // Only on right-to-left systems
     * <p>
     *
     * @return the compiler object
     */
    public Compiler swapAll() {
        if (stack.size() > 1) {
            Object[] data = new Object[stack.size()];
            stack.copyInto(data);
            stack.removeAllElements();
            for (int i = data.length - 1 ; i >= 0  ; --i) {
                stack.push(data[i]);
            }
        }
        return this;
    }

    /**
     * Swap the contence of the stack if the target ABI push parameters right-to-left.
     *
     * @return the compiler object
     */
    public Compiler swapForABI() {
        return swapAll(); // True for x86
    }

    /**
     * Push the data onto the runtime stack.
     *
     * <p>
     * Stack: ..., VALUE -> ...
     * <p>
     *
     * @return the compiler object
     */
    public Compiler push() {
        Instruction p1 = spop();
        append(cgen.newPush(p1));
        return this;
    }

    /**
     * Pop the top element of the runtime stack.
     *
     * <p>
     * Stack: ... -> ..., VALUE
     * <p>
     *
     * @param type the data type to pop
     * @return the compiler object
     */
    public Compiler pop(Type type) {
        append(cgen.newPop(type));
        return this;
    }

    /**
     * Pop all the elements of the runtime stack.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @return the compiler object
     */
    public Compiler popAll() {
        append(cgen.newPopAll());
        return this;
    }

    /**
     * Peek the receiver in the runtime stack.
     *
     * <p>
     * Stack: ... -> ..., OOP
     * <p>
     *
     * @return the compiler object
     */
    public Compiler peekReceiver() {
        append(cgen.newPeekReceiver());
        return this;
    }

    /**
     * Pop the top most element, force it to be the new type, and push the result.
     *
     * <p>
     * Stack: ... OLDVALUE -> ..., NEWVALUE
     * <p>
     *
     * @param to the type to convert to (Must be primary)
     * @return the compiler object
     */
    public Compiler force(Type to) {
        Type tt = tosType();
        assume(tt.getActivationSize() == to.getActivationSize(), "Cannot force convertion of different sizes");
        if (tt != to) {
            if      (tt == REF)    assumeCanForceRefTo(to);
            else if (tt == OOP)    assumeCanForceOopTo(to);
            else if (tt == INT)    assumeCanForceIntTo(to);
            else if (tt == UINT)   assumeCanForceUIntTo(to);
            else if (tt == LONG)   assumeCanForceLongTo(to);
            else if (tt == ULONG)  assumeCanForceULongTo(to);
            else if (tt == FLOAT)  assumeCanForceFloatTo(to);
            else if (tt == DOUBLE) assumeCanForceDoubleTo(to);
            else shouldNotReachHere();
            Instruction p1 = spop();
            append(cgen.newCvtOp(p1, to, false));
        }
        return this;
    }

    /**
     * Pop the top most element, convert it to a new type, and push the result.
     *
     * <p>
     * Stack: ... OLDVALUE -> ..., NEWVALUE
     * <p>
     *
     * @param to the type to convert to (Must be primary)
     * @return the compiler object
     */
    public Compiler convert(Type to) {
        Type tt = tosType();
        if (tt != to) {
            if      (tt == REF)    assumeCanConvertRefTo(to);
            else if (tt == OOP)    assumeCanConvertOopTo(to);
            else if (tt == INT)    assumeCanConvertIntTo(to);
            else if (tt == UINT)   assumeCanConvertUIntTo(to);
            else if (tt == LONG)   assumeCanConvertLongTo(to);
            else if (tt == FLOAT)  assumeCanConvertFloatTo(to);
            else if (tt == DOUBLE) assumeCanConvertDoubleTo(to);
            else shouldNotReachHere();
            Instruction p1 = spop();
            append(cgen.newCvtOp(p1, to, true));
        }
        return this;
    }

    /**
     * Add the top two elements on the stack. They must be of the same type, or be
     * a pointer and an integer.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler add() {
        arithop(OP_ADD);
        return this;
    }

    /**
     * Subtract the top two elements on the stack. They must be of the same type, or be
     * a pointer and an integer.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler sub() {
        arithop(OP_SUB);
        return this;
    }

    /**
     * Multiply the top two elements on the stack. They must be of the same type.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler mul() {
        arithop(OP_MUL);
        return this;
    }

    /**
     * Divide the top two elements on the stack. They must be of the same type.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler div() {
        arithop(OP_DIV);
        return this;
    }

    /**
     * Remainder the top two elements on the stack. They must be of the same type.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler rem() {
        arithop(OP_REM);
        return this;
    }

    /**
     * And the top two elements on the stack. They must be of the same type.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler and() {
        logicop(OP_AND);
        return this;
    }

    /**
     * Or the top two elements on the stack. They must be of the same type.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler or() {
        logicop(OP_OR);
        return this;
    }

    /**
     * Xor the top two elements on the stack. They must be of the same type.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler xor() {
        logicop(OP_XOR);
        return this;
    }

    /**
     * Shift the second element on the stack left by the number of bits specified by the top element.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler shl() {
        shiftop(OP_SHL);
        return this;
    }

    /**
     * Shift the second element on the stack right by the number of bits specified by the top element.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler shr() {
        shiftop(OP_SHR);
        return this;
    }

    /**
     * Shift the second element on the stack right by the number of bits specified by the top element.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler ushr() {
        shiftop(OP_USHR);
        return this;
    }

    /**
     * Negate (2's complement) the top element.
     *
     * <p>
     * Stack: ..., VALUE -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler neg() {
        unop(OP_NEG);
        return this;
    }

    /**
     * Produce the 1's complement of the top element.
     *
     * <p>
     * Stack: ..., VALUE -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler com() {
        unop(OP_COM);
        return this;
    }

    /**
     * Test the top two elements. Push true if s[0] == s[1] else false.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler eq() {
        cmpop(OP_EQ);
        return this;
    }

    /**
     * Test the top two elements. Push true if s[0] != s[1] else false.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler ne() {
        cmpop(OP_NE);
        return this;
    }

    /**
     * Test the top two elements. Push true if s[0] <= s[1] else false.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler le() {
        cmpop(OP_LE);
        return this;
    }

    /**
     * Test the top two elements. Push true if s[0] < s[1] else false.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler lt() {
        cmpop(OP_LT);
        return this;
    }

    /**
     * Test the top two elements. Push true if s[0] >= s[1] else false.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler ge() {
        cmpop(OP_GE);
        return this;
    }

    /**
     * Test the top two elements. Push true if s[0] > s[1] else false.
     *
     * <p>
     * Stack: ..., VALUE1, VALUE2 -> ..., RESULT
     * <p>
     *
     * @return the compiler object
     */
    public Compiler gt() {
        cmpop(OP_GT);
        return this;
    }

    /**
     * Branch to the label.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param label the label to branch to
     * @param in    the instruction producing the condition (or null)
     * @param cond  the condition (valid only when in is non-null)
     * @return the compiler object
     */
    private Compiler branch(XLabel label, Instruction in, boolean cond) {
        if (label.isBound()) {           // If the branch is backwards then the stack must be empty
            assumeStackEmpty();
            label.checkScope(currentScope);
        }
        label.merge(stack);
        append(cgen.newBranch(label, in, cond));
        return this;
    }

    /**
     * Unconditionally branch to the label.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param label the label to branch to
     * @return the compiler object
     */
    public Compiler br(Label label) {
        assumeStackEmpty(); // TEMP???
        return branch((XLabel)label, null, false);
    }

    /**
     * Branch to the label if the popped value is true.
     *
     * <p>
     * Stack: ..., VALUE -> ...
     * <p>
     *
     * @param label the label to branch to
     * @return the compiler object
     */
    public Compiler bt(Label label) {
        return branch((XLabel)label, spop(), true);
    }

    /**
     * Branch to the label if the popped value is false.
     *
     * <p>
     * Stack: ..., VALUE -> ...
     * <p>
     *
     * @param label the label to branch to
     * @return the compiler object
     */
    public Compiler bf(Label label) {
        return branch((XLabel)label, spop(), false);
    }

    /**
     * Branch to an absolute destination.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param dst   the absolute destination
     * @param in    the instruction producing the condition (or null)
     * @param cond  the condition (valid only when in is non-null)
     * @return      the compiler object
     */
    private Compiler branch(int dst, Instruction in, boolean cond) {
        append(cgen.newBranch(dst, in, cond));
        assumeStackEmpty();
        return this;
    }

    /**
     * Unconditionally branch to the label.
     *
     * <p>
     * Stack: _ -> ._
     * <p>
     *
     * @param dst   the absolute destination
     * @return the compiler object
     */
    public Compiler br(int dst) {
        return branch(dst, null, false);
    }

    /**
     * Branch to the label if the popped value is true.
     *
     * <p>
     * Stack: VALUE -> _
     * <p>
     *
     * @param dst   the absolute destination
     * @return the compiler object
     */
    public Compiler bt(int dst) {
        return branch(dst, spop(), true);
    }

    /**
     * Branch to the label if the popped value is false.
     *
     * <p>
     * Stack: VALUE -> _
     * <p>
     *
     * @param dst   the absolute destination
     * @return the compiler object
     */
    public Compiler bf(int dst) {
        return branch(dst, spop(), false);
    }

    /**
     * Jump somewhere. The top most value must be the address of the function to be called.
     *
     * <p>
     * Stack: ADDRESS -> _
     * <p>
     *
     * @return the compiler object
     */
    public Compiler jump() {
        Instruction addr = spop();
        append(cgen.newJump(addr));
        assumeStackEmpty();
        return this;
    }

    /**
     * Call a function. The top most value must be the address of the function to be called.
     *
     * <p>
     * Stack: ..., VALUEN to VALUE1, ADDRESS -> ..., [RESULT]
     * <p>
     *
     * @param nparms the number of parameters to pop
     * @param type the return type to be pushed onto the stack
     * @param convention the calling convebtion
     * @return the compiler object
     */
    public Compiler call(int nparms, Type type, int convention) {
        append(cgen.newCall(nparms, stack, type, convention));
        return this;
    }

    /**
     * Call a function. The top most value must be the address of the function to be called.
     *
     * <p>
     * Stack: ..., VALUEN to VALUE1, ADDRESS -> ..., [RESULT]
     * <p>
     *
     * @param nparms the number of parameters to pop
     * @param type the return type to be pushed onto the stack
     * @return the compiler object
     */
    public Compiler call(int nparms, Type type) {
        return call(nparms, type, C_NORMAL);
    }

    /**
     * Call a function. The top most value must be the address of the function to be called.
     * and all the other parameters on the stack are taken as parameters.
     *
     * <p>
     * Stack: ..., VALUEN to VALUE1, ADDRESS -> ..., [RESULT]
     * <p>
     *
     * @param type the return type to be pushed onto the stack
     * @param convention the calling convebtion
     * @return the compiler object
     */
    public Compiler call(Type type) {
        return call(stack.size(), type, C_NORMAL);
    }

    /**
     * Call a function. The top most value must be the address of the function to be called.
     * and all the other parameters on the stack are taken as parameters.
     *
     * <p>
     * Stack: ..., VALUEN to VALUE1, ADDRESS -> ..., [RESULT]
     * <p>
     *
     * @param type the return type to be pushed onto the stack
     * @param convention the calling convebtion
     * @return the compiler object
     */
    public Compiler call(Type type, int convention) {
        return call(stack.size(), type, convention);
    }

    /**
     * Return from a method.
     *
     * <p>
     * Stack: ..., [VALUE] -> _
     * <p>
     *
     * @param type the return type
     * @return the compiler object
     */
    public Compiler ret(Type type) {
        if (type != VOID) {
            Instruction inst = spop();
            append(cgen.newRet(inst));
            spop();
        } else {
            append(cgen.newRet(null));
        }
        assumeStackEmpty();
        return this;
    }

    /**
     * Return from a method.
     *
     * <p>
     * Stack: ..., [VALUE] -> _
     * <p>
     *
     * @return the compiler object
     */
    public Compiler ret() {
        ret(returnType);
        return this;
    }

    /**
     * Specify an unreachable place.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @return the compiler object
     */
    public Compiler deadCode() {
        append(cgen.newDeadCode());
        return this;
    }

    /**
     * Generate code that will result in the value of the frame pointer.
     *
     * <p>
     * Stack: ... -> ..., REF
     * <p>
     *
     * @return the compiler object
     */
    public Compiler framePointer() {
        append(cgen.newFramePointer());
        return this;
    }


    /*-----------------------------------------------------------------------*\
     *                               Compilation                             *
    \*-----------------------------------------------------------------------*/

    /**
     * Compile the IR.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @param load true if the code should be loaded into malloc() memory
     * @return the compiler object
     */
    public Compiler compile() {
        assumeStackEmpty();
        assume(state == S_READY);
        state = S_COMPILED;
        Instruction inst = first;
        cgen.start(first);
        while (inst != null) {
            inst.emit(cgen);
            inst.print(System.out);
            System.out.println();
            inst = inst.getNext();
        }
        relocationInfo = cgen.finish();
        return this;
    }

    /**
     * Get the length of the compiled code.
     *
     * @return the length in bytes
     */
    public int getCodeSize() {
        return cgen.getCodeSize();
    }

    /**
     * Get the code array buffer.
     *
     * @return the code array
     */
    public byte[] getCode() {
        return cgen.getCode();
    }

    /**
     * Return the relocation information. This is an array of ints where
     * where each entry discribes one offset into the code where relocation
     * is required. The format of each entry is such that the top 8 bits is
     * a code indicating the type of relocation required and the low 24 bits
     * is the offset.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @return the relocation information.
     */
    public int[] getRelocationInfo() {
        return relocationInfo;
    }

    /**
     * Return the offsets to unresolved symbols.
     *
     * <p>
     * Stack: ... -> ...
     * <p>
     *
     * @param relocationCode the reslocation type and offset
     * @param symbol the symbol to bind to the offset
     */
    public void addFixup(int relocationCode, String symbol) {
        fixupInfo.put(new Integer(relocationCode), symbol);
    }

    /**
     * Return the offsets to unresolved symbols.
     *
     * <p>
     * Stack: _ -> _
     * <p>
     *
     * @return the table of offset to symbol associations
     */
    public Hashtable getFixupInfo() {
        return fixupInfo;
    }

    /*-----------------------------------------------------------------------*\
     *                           Convertion tests                            *
    \*-----------------------------------------------------------------------*/

    /*
     * There are two kinds of type transformations. Arithmetic convertions, and
     * type casts that do not change the data. The former is called a "convertion"
     * the latter a "force". An example of a convertion is where float of 3.14
     * becomes an int of 3. An example of a force is where a float becomes an int
     * preserving the bit representation of the data like Float.floatToIntBits(),
     * for example, does.
     *
     * The legal forces are:
     *
     *     int    -> float
     *     float  -> int
     *     int    -> uint
     *     uint   -> int
     *     int    -> ref  (when refs are 32 bits)
     *     ref    -> int  (when refs are 32 bits)
     *     ref    -> oop
     *     long   -> double
     *     double -> long
     *     long   -> ref  (when refs are 64 bits)
     *     ref    -> long (when refs are 64 bits)
     *
     * The legal convertions are:
     *
     *     int    -> float
     *     float  -> int
     *     int    -> double
     *     double -> int
     *     int    -> long
     *     long   -> int
     *     uint   -> float
     *     float  -> uint
     *     uint   -> double
     *     double -> uint
     *     uint   -> long
     *     long   -> uint
     *
     * In addition the following convertions are also legal but the result is always an int
     *
     *     int    -> ubyte
     *     int    -> byte
     *     int    -> ushort
     *     int    -> short
     *
     */

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanForceRefTo(Type to) {
        assume(to == WORD || to == OOP, "Illegal force");  // CC: why WORD?
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanForceOopTo(Type to) {
        assume(to == REF, "Illegal force");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanForceIntTo(Type to) {
        if (REF.getStructureSize() == INT.getStructureSize()) {
            if (to.isPointer()) {
                return;
            }
        }
        assume(to == UINT || to == FLOAT, "Illegal force");  // CC: what about DOUBLE?
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanForceUIntTo(Type to) {
        assume(to == INT, "Illegal force");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanForceULongTo(Type to) {
        assume(to == LONG, "Illegal force");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanForceLongTo(Type to) {
        if (REF.getStructureSize() == LONG.getStructureSize()) {
            if (to.isPointer()) {
                return;
            }
        }
        assume(to == DOUBLE, "Illegal force");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanForceFloatTo(Type to) {
        assume(to == INT, "Illegal force");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanForceDoubleTo(Type to) {
        assume(to == LONG, "Illegal force");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanConvertRefTo(Type to) {
        fatal("Illegal convert");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanConvertOopTo(Type to) {
        fatal("Illegal convert");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanConvertIntTo(Type to) {
        assume(to == LONG || to == FLOAT || to == DOUBLE ||
               to == BYTE || to == UBYTE || to == SHORT || to == USHORT , "Illegal convert");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanConvertUIntTo(Type to) {
        assume(to == ULONG , "Illegal convert");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanConvertLongTo(Type to) {
        assume(to == INT || to == UINT || to == FLOAT || to == DOUBLE, "Illegal convert");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanConvertFloatTo(Type to) {
        assume(to == INT || to == UINT || to == LONG || to == DOUBLE, "Illegal convert");
    }

    /**
     * Test for valid convertion.
     *
     * @param to type to convert to
     */
    void assumeCanConvertDoubleTo(Type to) {
        assume(to == INT || to == UINT || to == FLOAT || to == LONG, "Illegal convert");
    }

}
