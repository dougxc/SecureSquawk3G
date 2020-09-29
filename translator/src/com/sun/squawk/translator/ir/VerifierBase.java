//if[J2ME.DEBUG]
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir;

import com.sun.squawk.vm.OPC;
import com.sun.squawk.util.*;

/**
 * The Squawk internal bytecode verifier. This does not check everything, and does not need to
 * because basic verification done as a part of the Java bytecode verifier in IRBuilder.java.
 * This is just a sanity check that the output of the InstructionEmitter is basically
 * correct and is only called when the Assert.ASSERTS_ENABLED flag is set.
 *
 * @author  Nik Shaylor
 */
abstract class VerifierBase {

    /**
     * Define verifier types.
     */
    protected final static Klass OOP     = Klass.REFERENCE,
                                 INT     = Klass.INT,
                                 LONG    = Klass.LONG,
                                 LONG2   = Klass.LONG2,
                                 FLOAT   = Klass.FLOAT,
                                 DOUBLE  = Klass.DOUBLE,
                                 DOUBLE2 = Klass.DOUBLE2,
                                 BOOLEAN = Klass.BOOLEAN,
                                 BYTE    = Klass.BYTE,
                                 SHORT   = Klass.SHORT,
                                 USHORT  = Klass.CHAR,
                                 VOID    = Klass.VOID;

    /**
     * Condition codes.
     */
    protected static final int EQ = 1,
                               NE = 2,
                               LT = 3,
                               LE = 4,
                               GT = 5,
                               GE = 6;

    /**
     * Special cases in the stack depth array.
     */
    private static final Klass[] NOT_A_TARGET   = {};
    private static final Klass[] ZERO_TARGET    = {};
    private static final Klass[] HANDLER_TARGET = {};

    /**
     * The trace flag.
     */
    private boolean trace;

    /**
     * The instruction parameter.
     */
    protected int iparm;

    /**
     * The current opcode.
     */
    protected int opcode;

    /**
     * The method body.
     */
    private MethodBody body;

    /**
     * The bytecode array.
     */
    private byte[] code;

    /**
     * The current instruction pointer.
     */
    private int ip;

    /**
     * The stack.
     */
    private Klass[] stack;

    /**
     * The number of parameter words.
     */
    private int parmWords;

    /**
     * The current stack pointer.
     */
    private int sp;

    /**
     * The parametrer and local types.
     */
    private Klass[] types;

    /**
     * The exseption handler table.
     */
    private ExceptionHandler[] handlers;

    /**
     * The stack contents at forward targets.
     */
    private Klass[][] targetStack;

    /**
     * Verify a method body.
     *
     * @param body the method body to verify
     */
    void verify(MethodBody body) {
        Method method = body.getDefiningMethod();
        this.body     = body;
        this.trace    = Klass.DEBUG && Tracer.isTracing("squawkverifier", method.toString());
        code          = body.getCode();
        ip            = 0;
        sp            = 0;
        stack         = new Klass[body.getMaxStack()];
        types         = body.getTypes();
        parmWords     = body.getParametersCount();
        targetStack   = new Klass[code.length + 1][];
        handlers      = body.getExceptionTable();

        /*
         * Setup the default target values.
         */
        for (int i = 0 ; i < targetStack.length ; i++) {
            targetStack[i] = NOT_A_TARGET;
        }

        /*
         * Setup the target stack for the exception handlers.
         */
        if (handlers != null && handlers.length > 0) {
            for (int i = 0 ; i < handlers.length ; i++) {
                targetStack[handlers[i].getHandler()] = HANDLER_TARGET;
            }
        }

        /*
         * Trace.
         */
        MethodBodyTracer mbt = null;
        if (trace) {
            Tracer.traceln("");
            Tracer.traceln("++++ Squawk verifier trace for " + method + " ++++");
            //Tracer.traceln("maxstack = "+stack.length);
            mbt = new MethodBodyTracer(body);
            mbt.traceHeader();
        }

        /*
         * Iterate over the bytecodes.
         */
        try {
            while (ip < code.length) {
                Klass[] target = targetStack[ip];
                if (target == HANDLER_TARGET) { // Start of a handler
                    popAll();
                }
                checkStack(target);
                if (trace) {
                    Tracer.trace("sp=" + sp + " ");
                }
                opcode = fetchUByte();
                do_switch();
                if (trace) {
                    Tracer.traceln(mbt.traceUntil(ip));
                }
            }
        } catch (RuntimeException t) {
            String src = "(" + body.getDefiningClass().getSourceFileName() + ":" + Method.getLineNumber(body.getMetadata().getLineNumberTable(), ip) + ")";
            System.out.println("Verify error in " + method + "@" + ip + src + ": ");
            throw t;
        } catch (Error t) {
            String src = "(" + body.getDefiningClass().getSourceFileName() + ":" + Method.getLineNumber(body.getMetadata().getLineNumberTable(), ip) + ")";
            System.out.println("Verify error in " + method + "@" + ip + src + ": ");
            throw t;
        }

        /*
         * Trace.
         */
        if (trace) {
            Tracer.traceln("---- Squawk verifier trace for " + method + " ----");
        }
    }


    /*-----------------------------------------------------------------------*\
     *                          Bytecode dispatching                         *
    \*-----------------------------------------------------------------------*/


    /**
     * Prefix for bytecode with no parameter.
     */
    protected void iparmNone() {
    }

    /**
     * Prefix for bytecode with a byte parameter.
     */
    protected void iparmByte() {
        iparm = fetchByte();
    }

    /**
     * Prefix for bytecode with an unsigned byte parameter.
     */
    protected void iparmUByte() {
        iparm = fetchUByte();
    }

    /**
     * Execute the next bytecode.
     */
    abstract protected void do_switch();

    /**
     * Add 256 to the next unsigned byte and jump to that bytecode execution.
     */
    protected void do_escape() {
        opcode = fetchUByte() + 256;
        do_switch();
    }

    /**
     * Or the (parameter<<8) into the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_wide(int n) {
        opcode = fetchUByte() + OPC.WIDE_DELTA;
        iparm  = fetchUByte() | (n<<8);
        do_switch();
    }

    /**
     * Load the inlined short as the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_wide_short() {
        opcode = fetchUByte() + OPC.WIDE_DELTA;
        iparm  = fetchShort();
        do_switch();
    }

    /**
     * Load the inlined int as the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_wide_int() {
        opcode = fetchUByte() + OPC.WIDE_DELTA;
        iparm  = fetchInt();
        do_switch();
    }

    /**
     * Or the (parameter<<8) in to the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_escape_wide(int n) {
        opcode = fetchUByte() + 256 + OPC.ESCAPE_WIDE_DELTA;
        iparm  = fetchUByte() | (n<<8);
        do_switch();
    }

    /**
     * Load the inlined short as the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_escape_wide_short() {
        opcode = fetchUByte() + 256 + OPC.ESCAPE_WIDE_DELTA;
        iparm  = fetchShort();
        do_switch();
    }

    /**
     * Load the inlined int as the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_escape_wide_int() {
        opcode = fetchUByte() + 256 + OPC.ESCAPE_WIDE_DELTA;
        iparm  = fetchInt();
        do_switch();
    }


    /*-----------------------------------------------------------------------*\
     *                           Instruction decoding                        *
    \*-----------------------------------------------------------------------*/

    /**
     * Fetch a byte from ip++.
     *
     * @return the value
     */
    protected int fetchByte() {
        return code[ip++];
    }

    /**
     * Fetch an unsigned byte from from ip++.
     *
     * @return the value
     */
    protected int fetchUByte() {
        return fetchByte() & 0xFF;
    }

    /**
     * Fetch a short from ip++.
     *
     * @return the value
     */
    protected int fetchShort() {
        if (!VM.isBigEndian()) {
            int b1 = fetchUByte();
            int b2 = fetchByte();
            return (b2 << 8) | b1;
        } else {
            int b1 = fetchByte();
            int b2 = fetchUByte();
            return (b1 << 8) | b2;
        }
    }

    /**
     * Fetch a unsigned short from ip++.
     *
     * @return the value
     */
    protected int fetchUShort() {
        int b1 = fetchUByte();
        int b2 = fetchUByte();
        if (!VM.isBigEndian()) {
            return (b2 << 8) | b1;
        } else {
            return (b1 << 8) | b2;
        }
    }

    /**
     * Fetch an int from ip++.
     *
     * @return the value
     */
    protected int fetchInt() {
        int b1 = fetchUByte();
        int b2 = fetchUByte();
        int b3 = fetchUByte();
        int b4 = fetchUByte();
        if (!VM.isBigEndian()) {
            return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        } else {
            return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
        }
    }
    /**
     * Fetch a long from ip++.
     *
     * @return the value
     */
    protected long fetchLong() {
        long b1 = fetchUByte();
        long b2 = fetchUByte();
        long b3 = fetchUByte();
        long b4 = fetchUByte();
        long b5 = fetchUByte();
        long b6 = fetchUByte();
        long b7 = fetchUByte();
        long b8 = fetchUByte();
        if (!VM.isBigEndian()) {
            return (b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) | (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        } else {
            return (b1 << 56) | (b2 << 48) | (b3 << 40) | (b4 << 32) | (b5 << 24) | (b6 << 16) | (b7 << 8) | b8;
        }
    }

/*if[FLOATS]*/
    /**
     * Fetch a float from ip++.
     *
     * @return the value
     */
    protected float fetchFloat() {
        return Float.intBitsToFloat(fetchInt());
    }

    /**
     * Fetch a double from ip++.
     *
     * @return the value
     */
    protected double fetchDouble() {
        return Double.longBitsToDouble(fetchLong());
    }
/*end[FLOATS]*/



    /*-----------------------------------------------------------------------*\
     *                               Verification                            *
    \*-----------------------------------------------------------------------*/

    /**
     * Normalize a class
     *
     * @param k the class
     */
    private Klass normalize(Klass k) {
        if (k == Klass.ADDRESS || k == Klass.UWORD || k == Klass.OFFSET) {
            return Klass.SQUAWK_64 ? LONG : INT;
        }
        return Frame.getLocalTypeFor(k);
    }

    /**
     * Get a type
     *
     * @param n in index
     */
    private Klass getType(int n) {
        Klass k = types[n];
        Assert.that(k != VOID);
        Assert.that(k != LONG2);
        Assert.that(k != DOUBLE2);
        return normalize(k);
    }

    /**
     * Push a type onto the stack.
     *
     * @param k the type
     */
    private void push(Klass k) {
        stack[sp++] = normalize(k);
        if (!Klass.SQUAWK_64) {
            if (k == LONG) {
                stack[sp++] = LONG2;
            }
            if (k == DOUBLE) {
                stack[sp++] = DOUBLE2;
            }
        }
    }

    /**
     * Pop a type from the stack.
     *
     * @param k the type
     */
    private void pop(Klass k) {
        Klass x = stack[--sp];
        if (x == LONG2) {
            Assert.that(!Klass.SQUAWK_64);
            x = stack[--sp];
            Assert.that(x == LONG, "After LONG2 came "+x);
        }
        if (x == DOUBLE2) {
            Assert.that(!Klass.SQUAWK_64);
            x = stack[--sp];
            Assert.that(x == DOUBLE, "After DOUBLE2 came "+x);
        }
        Assert.that(normalize(k) == x, "Popped "+x+" expected "+normalize(k));
    }

    /**
     * Pop everything from the stack.
     */
    private void popAll() {
        sp = 0;
    }

    /**
     * Check that an bytecode address points to a valid instruction.
     *
     * @param offset the address
     */
    private void checkAddress(int offset) {
        // TODO
    }

    /**
     * Check that the current state of the stack is the same as the target.
     *
     * @param target the target stack state
     */
    void checkStack(Klass[] target) {
        if (target != NOT_A_TARGET) {
            Assert.that(sp == target.length, "sp="+sp+" target.length="+target.length);
            for (int i = 0 ; i < sp ; i++) {
                Assert.that(stack[i] == target[i], "stack[i]="+stack[i]+" target[i]="+target[i]);
            }
        }
    }


    /*-----------------------------------------------------------------------*\
     *                               Instructions                            *
    \*-----------------------------------------------------------------------*/

    protected void do_const_null() {
        push(OOP);
    }

    protected void do_const(int n) {
        push(INT);
    }

    protected void do_object(int n) {
        push(OOP);
    }

    protected void do_loadparm(int n) {
        push(getType(n));
    }

    protected void do_load(int n) {
        push(getType(n+parmWords));
    }

    protected void do_storeparm(int n) {
        pop(getType(n));
    }

    protected void do_store(int n) {
        pop(getType(n+parmWords));
    }

    protected void do_const_byte() {
        fetchByte();
        push(INT);
    }

    protected void do_const_short() {
        fetchShort();
        push(INT);
    }

    protected void do_const_char() {
        fetchUShort();
        push(INT);
    }

    protected void do_const_int() {
        fetchInt();
        push(INT);
    }

    protected void do_const_long() {
        fetchLong();
        push(LONG);
    }

    protected void do_const_float() {
/*if[FLOATS]*/
        fetchFloat();
        push(FLOAT);
/*else[FLOATS]*/
//      throw new Error("No floating point");
/*end[FLOATS]*/
    }

    protected void do_const_double() {
/*if[FLOATS]*/
        fetchDouble();
        push(DOUBLE);
/*else[FLOATS]*/
//      throw new Error("No floating point");
/*end[FLOATS]*/
    }

    protected void do_object() {
        do_object(iparm);
    }

    protected void do_load() {
        do_load(iparm);
    }

    protected void do_load_i2() {
        do_load(iparm);
    }

    protected void do_store() {
        do_store(iparm);
    }

    protected void do_store_i2() {
        do_store(iparm);
    }

    protected void do_loadparm() {
        do_loadparm(iparm);
    }

    protected void do_loadparm_i2() {
        do_loadparm(iparm);
    }

    protected void do_storeparm() {
        do_storeparm(iparm);
    }

    protected void do_storeparm_i2() {
        do_storeparm(iparm);
    }

    protected void do_inc() {
//        do_load(iparm);
//        do_const(1);
//        do_add(INT);
//        do_store(iparm);
    }

    protected void do_dec() {
//        do_load(iparm);
//        do_const(1);
//        do_sub(INT);
//        do_store(iparm);
    }

    protected void do_incparm() {
//        do_loadparm(iparm);
//        do_const(1);
//        do_add(INT);
//        do_storeparm(iparm);
    }

    protected void do_decparm() {
//        do_loadparm(iparm);
//        do_const(1);
//        do_sub(INT);
//        do_storeparm(iparm);
    }

    private void setTarget(int current, int offset) {
        int target = current + offset;
        checkAddress(target);
        if (offset >= 0) {
            if (targetStack[target] == NOT_A_TARGET) {
                if (sp == 0) {
                    targetStack[target] = ZERO_TARGET;
                } else {
                    targetStack[target] = new Klass[sp];
                    for (int i = 0 ; i < sp ; i++) {
                        targetStack[target][i] = stack[i];
                    }
                }
            } else {
                checkStack(targetStack[target]);
            }
        } else {
            Assert.that(sp == 0);
            Assert.that((code[target]&0xFF) == OPC.BBTARGET_SYS || (code[target]&0xFF) == OPC.BBTARGET_APP);
            Assert.that(targetStack[target].length == 0);
        }
    }

    private void resetSp() {
        if (targetStack[ip] != NOT_A_TARGET) {
            sp = targetStack[ip].length;
            for (int i = 0 ; i < sp ; i++) {
                stack[i] = targetStack[ip][i];
            }
        }
    }

    protected void do_goto() {
        setTarget(ip, iparm);
        resetSp();
    }

    protected void do_if(int operands, int cc, Klass t) {
        pop(t);
        if (operands == 2) {
            pop(t);
        }
        setTarget(ip, iparm);
    }

    protected void do_getstatic(Klass t) {
        pop(OOP);
        Assert.that(sp == 0);
        push(t);
    }

    protected void do_class_getstatic(Klass t) {
        push(OOP);
        do_getstatic(t);
    }

    protected void do_putstatic(Klass t) {
        pop(OOP);
        pop(t);
        Assert.that(sp == 0);
    }

    protected void do_class_putstatic(Klass t) {
        push(OOP);
        do_putstatic(t);
    }

    protected void do_getfield(Klass t) {
        pop(OOP);
        push(t);
    }

    protected void do_this_getfield(Klass t) {
        do_load(0);
        do_getfield(t);
    }

    protected void do_putfield(Klass t) {
        pop(t);
        pop(OOP);
    }

    protected void do_this_putfield(Klass t) {
        do_load(0);
        do_putfield(t);
    }

    protected void do_invokevirtual(Klass t) {
        popAll();
        if (t != VOID) {
            push(t);
        }
    }

    protected void do_invokestatic(Klass t) {
        pop(OOP);
        do_invokevirtual(t);
    }

    protected void do_invokesuper(Klass t) {
        pop(OOP);
        do_invokevirtual(t);
    }

    protected void do_invokenative(Klass t) {
        do_invokevirtual(t);
    }

    protected void do_findslot() {
        pop(OOP);
        pop(OOP);
        Assert.that(sp == 0);
        push(INT);
    }

    protected void do_invokeslot(Klass t) {
        pop(INT);
        do_invokevirtual(t);
    }

    protected void do_return(Klass t) {
        if (t != VOID) {
            pop(t);
        }
        //Assert.that(sp == 0); <-- Not a valid assumption for the TCK
        resetSp();
    }

    protected void do_tableswitch(Klass t) {
        pop(INT);
        int size = (t == Klass.SHORT) ? 2 : 4;
        while ((ip % size) != 0) {
            fetchByte();
        }
        int low  = getSwitchEntry(size);
        int high = getSwitchEntry(size);
        int loc  = getSwitchEntry(size);
        int pos  = ip;
        setTarget(pos, loc);
        for (int i = low ; i <= high ; i++) {
            loc = getSwitchEntry(size);
            setTarget(pos, loc);
        }
    }

    private int getSwitchEntry(int size) {
        if (size == 2) {
            return fetchShort();
        } else {
            return fetchInt();
        }
    }

    protected void do_extend0() {
        Assert.that(ip == 1);
        Assert.that(sp == 0);
    }

    protected void do_extend() {
        Assert.that(ip == 2);
        Assert.that(sp == 0);
    }
    protected void do_add(Klass t) {
        pop(t);
        pop(t);
        push(t);
    }

    protected void do_sub(Klass t) {
        do_add(t);
    }

    protected void do_and(Klass t) {
        do_add(t);
    }

    protected void do_or(Klass t) {
        do_add(t);
    }
    protected void do_xor(Klass t) {
        do_add(t);
    }

    protected void do_shl(Klass t) {
        pop(INT);
        pop(t);
        push(t);
    }

    protected void do_shr(Klass t) {
        do_shl(t);
    }

    protected void do_ushr(Klass t) {
        do_shl(t);
    }

    protected void do_mul(Klass t) {
        do_add(t);
    }

    protected void do_div(Klass t) {
        do_add(t);
    }

    protected void do_rem(Klass t) {
        do_add(t);
    }

    protected void do_neg(Klass t) {
        pop(t);
        push(t);
    }

    protected void do_throw() {
        pop(OOP);
        Assert.that(sp == 0);
        resetSp();
    }

    protected void do_catch() {
        Assert.that(sp == 0);
        push(OOP);
    }

    protected void do_pop(int n) {
        Assert.that(sp >= n);
        sp -= n;
        //The following is not valid because the pop2 bytecode is turned into two squawk pop bytecodes.
        //Assert.that(sp == 0); // Always the case because pop is only used to discard the result of an invoke.
    }

    protected void do_monitorenter() {
        pop(OOP);
        Assert.that(sp == 0);
    }

    protected void do_monitorexit() {
        pop(OOP);
        Assert.that(sp == 0);
    }

    protected void do_class_monitorenter() {
        Assert.that(sp == 0);
    }

    protected void do_class_monitorexit() {
        Assert.that(sp == 0);
    }

    protected void do_arraylength() {
        pop(OOP);
        push(INT);
    }

    protected void do_new() {
        pop(OOP);
        Assert.that(sp == 0);
        push(OOP);
    }

    protected void do_newarray() {
        pop(OOP);
        pop(INT);
        Assert.that(sp == 0);
        push(OOP);
    }

    protected void do_newdimension() {
        pop(INT);
        pop(OOP);
        Assert.that(sp == 0);
        push(OOP);
    }

    protected void do_class_clinit() {
        Assert.that(sp == 0);
        push(OOP);
        pop(OOP);
    }

    protected void do_bbtarget_sys() {
        checkAddress(ip-1);
        Assert.that(sp == 0);
    }

    protected void do_bbtarget_app() {
        do_bbtarget_sys();
    }

    protected void do_instanceof() {
        pop(OOP);
        pop(OOP);
        Assert.that(sp == 0);
        push(INT);
    }

    protected void do_checkcast() {
        pop(OOP);
        pop(OOP);
        Assert.that(sp == 0);
        push(OOP);
    }

    protected void do_aload(Klass t) {
        pop(INT);
        pop(OOP);
        push(t);
    }

    protected void do_astore(Klass t) {
        pop(t);
        pop(INT);
        pop(OOP);
        if (t == OOP) {
            Assert.that(sp == 0);
        }
    }

    protected void do_lookup(Klass t) {
        pop(OOP);
        pop(INT);
        Assert.that(sp == 0);
        push(INT);
    }

    protected void do_res(int n) {
    }

    protected void do_i2b() {
        pop(INT);
        push(INT);
    }

    protected void do_i2s() {
        pop(INT);
        push(INT);
    }

    protected void do_i2c() {
        pop(INT);
        push(INT);
    }

    protected void do_l2i() {
        pop(LONG);
        push(INT);
    }

    protected void do_i2l() {
        pop(INT);
        push(LONG);
    }

    protected void do_i2f() {
        pop(INT);
        push(FLOAT);
    }

    protected void do_l2f() {
        pop(LONG);
        push(FLOAT);
    }

    protected void do_f2i() {
        pop(FLOAT);
        push(INT);
    }

    protected void do_f2l() {
        pop(FLOAT);
        push(LONG);
    }

    protected void do_i2d() {
        pop(INT);
        push(DOUBLE);
    }

    protected void do_l2d() {
        pop(LONG);
        push(DOUBLE);
    }

    protected void do_f2d() {
        pop(FLOAT);
        push(DOUBLE);
    }

    protected void do_d2i() {
        pop(DOUBLE);
        push(INT);
    }

    protected void do_d2l() {
        pop(DOUBLE);
        push(LONG);
    }

    protected void do_d2f() {
        pop(DOUBLE);
        push(FLOAT);
    }


}
