/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;

/**
 * The Squawk interpreter generator.
 *
 * @author   Nik Shaylor
 */
public class Interpreter extends InterpreterNative {

    /**
     * Option to preload the next bytecode earlier where possible.
     */
    private final static boolean PRELOADNEXTBYTECODE = true;

    /**
     * The local that instruction parameter.
     */
    private Local iparm;

    /**
     * The local that contains the value of the bytecode to execute.
     */
    private Local nextBytecode;

    /**
     * The interpreter's instruction pointer.
     */
    private Local ip;

    /**
     * The interpreter's local variable pointer.
     */
    private Local lp;

    /**
     * The interpreter's stack start pointer.
     */
    private Local ss;

    /**
     * The interpreter's class pointer.
     */
    private Local cp;

    /**
     * The address of a common post() expansion.
     */
    private Label commonPost;

    /**
     * The address of the main bytecode dispatch table.
     */
    private Label dispatchTable;

    /**
     * The list of labels for the main bytecode dispatch table.
     */
    private Label dispatchLabels[];

    /**
     * The address of the native bytecode dispatch table.
     */
    private Label nativeTable;

    /**
     * The list of labels for the native bytecode dispatch table.
     */
    private Label nativeLabels[];

    /**
     * The Constructor.
     */
    public Interpreter() {
    }

    /*-----------------------------------------------------------------------*\
     *                          The producer main code                       *
    \*-----------------------------------------------------------------------*/

    /**
     * The Producer.
     */
    private Compiler produce(MethodMap mmap) {
        commentBox("Squawk bytecode interpreter");
        c.enter(Compiler.E_REGISTER);
        c.result(VOID);
            mp    = c.local(MP);
            ip    = c.local(IP,  Compiler.P_HIGH);
            lp    = c.local(LP,  Compiler.P_HIGH);
            ss    = c.local(SS);
            iparm = c.local(INT, Compiler.P_HIGH);
            cp    = c.local(OOP, Compiler.P_MEDIUM);

            /*
             * Define the bytecode labels.
             */
            dispatchLabels = new Label[OPC.BYTECODE_COUNT+OPC.PSEUDOCODE_COUNT];
            for (int i = 0 ; i < dispatchLabels.length ; i++) {
                dispatchLabels[i] = c.label();
            }
            dispatchTable = c.label();

            /*
             * Define the native labels.
             */
            nativeLabels = new Label[Native.ENTRY_COUNT];
            for (int i = 0 ; i < nativeLabels.length ; i++) {
                nativeLabels[i] = c.label();
            }
            nativeTable = c.label();

            commentBox("Setup the interpreter's cp local variable");
            super.getCP();
            c.store(cp);

            /*
             * Setup lp.
             */
            c.begin();
                Label largeFormat = c.label();
                Label done        = c.label();
                Local p           = c.local(REF);
                Local val         = c.local(INT);
                Local stackWords  = c.local(INT);
                Local localWords  = c.local(INT);
//              Local parmWords   = c.local(INT);

                commentBox("Decode the number of parameter and locals");

                c.load(mp);
                pointBeforeOwningClassPointer();
                c.store(p);

                c.comment("Read the first byte into val.");
                c.load(p);
                c.read(BYTE);
                c.store(val);

                c.comment("If the top bit is set then the large format is being used.");
                c.load(val).literal(0).lt().bt(largeFormat);

                c.comment("Get the next byte and or it into val<<8.");
                c.load(p);
                c.literal(1);
                c.sub();
                c.read(UBYTE);
                c.load(val);
                c.literal(8).shl();
                c.or();
                c.store(val);

                c.comment("Get number of stack words.");
                c.load(val);
                c.literal(31);
                c.and();
                c.store(stackWords);

                c.comment("Get number of locals.");
                c.load(val);
                c.literal(5);
                c.ushr();
                c.literal(31);
                c.and();
                c.store(localWords);

//                c.comment("Get number of parms.");
//                c.load(val);
//                c.literal(10);
//                c.ushr();
//                c.literal(31);
//                c.and();
//                c.store(parmWords);
                c.br(done);

            c.bind(largeFormat);
                c.comment("Decode large format. -- Get number of stack words.");
                decode(p, stackWords);
                c.comment("Get number of locals.");
                decode(p, localWords);
//                c.comment("Get number of parms.");
//                decode(p, parmWords);

            c.bind(done);
                c.comment("Perform a stack check to preallocate the required activation record");
                c.load(localWords); timesWordSize();
                c.load(stackWords); timesWordSize();
                c.stackCheck();

                c.comment("Allocate the memory needed for the local variables and setup lp");
                c.load(localWords); timesWordSize();
                c.alloca();
                c.store(lp);
            c.end();

            /*
             * Use the standard way to dispatch to the first bytecode.
             */
            commentBox("Dispatch to the first bytecode");
            pre(FLOW_CALL);
            /* nop */
            commonPost = c.label();
            c.bind(commonPost);
            post();

            /*
             * The interpreter should never end up here.
             */
            shouldNotReachHere();

            /*
             * Put the code for the interpreter here.
             */
            do_switch();

            /*
             * Put the code for the native bytecodes here.
             */
            do_nativeswitch();

            /*
             * Dispatch table.
             */
            commentBox("Bytecode dispatch table");
            c.data(dispatchTable, dispatchLabels);
            c.data(nativeTable, nativeLabels);

        c.leave(mmap);

        c.compile();
        return c;
    }

    /**
     * Generate code to decode the number of parameter and local variables.
     *
     * @param p   the pointer local to the data
     * @param res the destination local to contain the result
     */
    private void decode(Local p, Local res) {
        Label done  = c.label();

        c.load(p).literal(1).sub().store(p);
        c.load(p);
        c.read(BYTE);
        c.store(res);

        c.comment("If the top bit is set then the large format is being used.");
        c.load(res).literal(0).ge().bt(done);

        c.load(res);
        c.literal(127);
        c.and();
        c.literal(8);
        c.shl();

        c.load(p).literal(1).sub().store(p);
        c.load(p);
        c.read(BYTE);
        c.or();
        c.store(res);

    c.bind(done);
    }


    /*-----------------------------------------------------------------------*\
     *                                Binding                                *
    \*-----------------------------------------------------------------------*/

    /**
     * Bind the standard entry point for a bytecode to the current location.
     *
     * @param code the bytecode
     */
    protected void bind(int code) {
        if (code < Mnemonics.OPCODES.length) {
            commentBox("OPC."+Mnemonics.OPCODES[code].toUpperCase());
        } else {
            commentBox("OPC."+code);
        }
        c.bind(dispatchLabels[code]);
    }


    /*-----------------------------------------------------------------------*\
     *                         Native bytecode support                       *
    \*-----------------------------------------------------------------------*/

    /**
     * Bind the standard entry point for a bytecode to the current location.
     *
     * @param code the bytecode
     */
    protected void nativebind(int code) {
        commentBox("Native "+code);
        c.bind(nativeLabels[code]);
    }

    /**
     * Pop a value on the runtime stack
     *
     * @param type the type to pop
     */
    protected void nativepop(Type type) {
        c.pop(type);
    }

    /**
     * Push a value on the runtime stack
     *
     * @param type the type to pop
     */
    protected void nativepush(Type type) {
        Assert.that(c.tosType() == type, "tosType() = "+c.tosType().getTypeCode());
        c.push();
    }

    /**
     * Dispatch after a native invoke.
     */
    protected void nativedone() {
        c.br(commonPost);
    }

    /*-----------------------------------------------------------------------*\
     *                          Bytecode dispatching                         *
    \*-----------------------------------------------------------------------*/

    /**
     * Produce code that will add n to ip
     *
     * @param n the constant to add to the ip.
     */
    private void incIp(int n) {
        c.load(ip).literal(n).add().store(ip);
    }

    /**
     * Define nextBytecode and load it with the unsigned byte pointed to by ip.
     */
    private void setupNextBytecode() {
        Assert.that(nextBytecode == null);
        c.comment("setupNextBytecode()");
        nextBytecode = c.local(INT);
        fetchUByte(0, nextBytecode);
    }

    /**
     * Define nextBytecode and load it with the unsigned byte pointed to by ip.
     * Then increment ip by 1.
     */
    private void setupNextBytecodeAndIncIp() {
        setupNextBytecode();
        incIp(1);
    }

    /**
     * Define nextBytecode and load it with the unsigned byte pointed to by ip.
     * Then load iparm with specified data type from [ip+1] and add increment ip
     * by 1 plus the sixe of the data type loaded.
     */
    private void setupNextBytecodeAndIncIp(Type type) {
        setupNextBytecode();
        fetch(type, 1, false);
        incIp(type.getStructureSize()+1);
        c.store(iparm);
    }

    /**
     * Produce code that will add n to nextBytecode
     *
     * @param n the amount to add
     */
    protected void incNextBytecode(int n) {
        c.load(nextBytecode).literal(n).add().store(nextBytecode);
    }

    /**
     * Pre bytecode exexution.
     *
     * @param code prefetch code.
     */
    protected void pre(int code) {
        Assert.that(nextBytecode == null);
        c.comment("+pre");
        c.begin();
            if (PRELOADNEXTBYTECODE && code == FLOW_NEXT) {
                setupNextBytecode();
            }
            // Start of bytecode code
            c.comment("-pre");
    }

    /**
     * Post bytecode exexution.
     *
     * @param code prefetch code.
     */
    protected void post() {
            c.comment("+post");
            if (nextBytecode == null) {
                setupNextBytecode();
            }
            c.comment("Next dispatch");
            c.load(nextBytecode);
            timesWordSize();
            c.literal(dispatchTable);
            c.add();
            c.read(REF);
            c.jump();
        c.end();
        c.comment("-post");
        nextBytecode = null;
    }

    /**
     * Or the (parameter<<8) in to the value of the next bytecode and then
     * setup to dispatch using the wide table.
     */
    protected void do_wide(int n) {
        setupNextBytecodeAndIncIp(UBYTE);
        if (n != 0) {
            c.load(iparm).literal(n<<8).or().store(iparm);
        }
        incNextBytecode(OPC.WIDE_DELTA);
    }

    /**
     * Load the inlined short as the value of the next bytecode and then
     * setup to dispatch using the wide table.
     */
    protected void do_wide_short() {
        setupNextBytecodeAndIncIp(SHORT);
        incNextBytecode(OPC.WIDE_DELTA);
    }

    /**
     * Load the inlined int as the value of the next bytecode and then
     * setup to dispatch using the wide table.
     */
    protected void do_wide_int() {
        setupNextBytecodeAndIncIp(INT);
        incNextBytecode(OPC.WIDE_DELTA);
    }

    /**
     * Add 256 to the next unsigned byte and jump to that bytecode execution.
     */
    protected void do_escape() {
        setupNextBytecodeAndIncIp();
        incNextBytecode(256);
    }

    /**
     * Or the (parameter<<8) in to the value of the next bytecode and then
     * setup to dispatch using the escaped wide table.
     */
    protected void do_escape_wide(int n) {
/*if[FLOATS]*/
        setupNextBytecodeAndIncIp(UBYTE);
        if (n != 0) {
            c.load(iparm).literal(n<<8).or().store(iparm);
        }
        incNextBytecode(256 + OPC.ESCAPE_WIDE_DELTA);
/*else[FLOATS]*/
//      emitFatal("Floats not supported");
/*end[FLOATS]*/
    }

    /**
     * Load the inlined short as the value of the next bytecode and then
     * setup to dispatch using the escaped wide table.
     */
    protected void do_escape_wide_short() {
/*if[FLOATS]*/
        setupNextBytecodeAndIncIp(SHORT);
        incNextBytecode(256 + OPC.ESCAPE_WIDE_DELTA);
/*else[FLOATS]*/
//      emitFatal("Floats not supported");
/*end[FLOATS]*/
    }

    /**
     * Load the inlined int as the value of the next bytecode and then
     * setup to dispatch using the escaped wide table.
     */
    protected void do_escape_wide_int() {
/*if[FLOATS]*/
        setupNextBytecodeAndIncIp(INT);
        incNextBytecode(256 + OPC.ESCAPE_WIDE_DELTA);
/*else[FLOATS]*/
//      emitFatal("Floats not supported");
/*end[FLOATS]*/
    }


    /*-----------------------------------------------------------------------*\
     *                             Utility code                              *
    \*-----------------------------------------------------------------------*/

    /**
     * Clear the runtime stack.
     *
     * <p>
     * Java Stack:  ... -> _
     * <p>
     *
     */
    protected void clearInterpreterStack() {
        c.popAll();
    }


    /*-----------------------------------------------------------------------*\
     *                          Access to registers                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Push 'value' on the stack.
     *
     * <p>
     * Compiler Stack: ... -> VALUE
     * <p>
     *
     */
    protected void getValue() {
        c.load(iparm);
    }

    /**
     * Push local pointer.
     *
     * <p>
     * Compiler Stack: ... -> ..., lp
     * <p>
     *
     */
    private void getLP() {
        c.load(lp);
    }

    /**
     * Push class pointer.
     *
     * <p>
     * Compiler Stack: ... -> ..., cp
     * <p>
     *
     */
    protected void getCP() {
        c.load(cp);
    }


    /*-----------------------------------------------------------------------*\
     *                             Access to data                            *
    \*-----------------------------------------------------------------------*/

    /**
     * Get a local variable.
     *
     * <p>
     * Compiler Stack: ..., INDEX -> ..., VALUE
     * <p>
     *
     * @param t the type to read
     */
    private void getLocal() {
        timesWordSize();
        getLP();
        c.add();
        c.read(INT);
    }

    /**
     * Set a local variable.
     *
     * <p>
     * Compiler Stack: ..., VALUE, INDEX -> ...
     * <p>
     *
     * @param t the type to read
     */
    private void setLocal() {
        timesWordSize();
        getLP();
        c.add();
        c.write(INT);
    }

    /**
     * Get a parameter word.
     *
     * <p>
     * Compiler Stack: ..., INT -> ..., VALUE
     * <p>
     *
     */
    private void getParm() {
        c.loadParm();
    }

    /**
     * Set a parameter word.
     *
     * <p>
     * Compiler Stack: ..., VALUE, INT -> ...
     * <p>
     *
     */
    private void setParm() {
        c.storeParm();
    }


    /*-----------------------------------------------------------------------*\
     *                           Instruction decoding                        *
    \*-----------------------------------------------------------------------*/

    /**
     * Read a value realtive to the ip.
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     *
     * @param type       the data type to read
     * @param byteOffset the offset in bytes from ip
     * @param isAligned  true if the data in know to be well aligned
     */
    protected void fetch(Type type, int byteOffset, boolean isAligned) {
        if (isAligned || c.loadsMustBeAligned() == false) {
            c.load(ip);
            if (byteOffset != 0) {
                c.literal(byteOffset);
                c.add();
            }
            c.read(type);
        } else {
            c.begin();
                Local res = c.local(INT);
                if (type == USHORT) {
                    fetchUByte(byteOffset);
                    c.literal(8);
                    c.shl();
                    fetchUByte(byteOffset+1);
                    c.or();
                    c.store(res);
                } else if (type == SHORT) {
                    fetchByte(byteOffset);
                    c.literal(8);
                    c.shl();
                    fetchUByte(byteOffset+1);
                    c.or();
                    c.store(res);
                } else if (type == INT) {
                    fetchUByte(byteOffset);
                    c.literal(24);
                    c.shl();
                    c.store(res);

                    fetchUByte(byteOffset+1);
                    c.literal(16);
                    c.shl();
                    c.load(res);
                    c.or();
                    c.store(res);

                    fetchUByte(byteOffset+2);
                    c.literal(8);
                    c.shl();
                    c.load(res);
                    c.or();
                    c.store(res);

                    fetchUByte(byteOffset+3);
                    c.load(res);
                    c.or();
                    c.store(res);
                } else {
                    Assert.shouldNotReachHere();
                }
                c.load(res);
            c.end();
        }
    }

    /**
     * Fetch a byte from [ip+byteOffset].
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     *
     */
    protected void fetchByte(int byteOffset) {
        fetch(BYTE, byteOffset, true);
    }

    /**
     * Fetch a byte from [ip+byteOffset], and store the result in a local.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     *
     * @param local the destination
     */
    protected void fetchByte(int byteOffset, Local local) {
        fetchByte(byteOffset);
        c.store(local);
    }

    /**
     * Fetch an unsigned byte from [ip+byteOffset].
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     *
     */
    protected void fetchUByte(int byteOffset) {
        fetch(UBYTE, byteOffset, true);
    }

    /**
     * Fetch an unsigned byte from [ip+byteOffset], and store the result in a local.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     *
     * @param local the destination
     */
    protected void fetchUByte(int byteOffset, Local local) {
        fetchUByte(byteOffset);
        c.store(local);
    }

    /**
     * Fetch a short from [ip+byteOffset].
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     *
     */
    protected void fetchShort(int byteOffset) {
        fetch(SHORT, byteOffset, false);
    }

    /**
     * Fetch a short from [ip+byteOffset], and store the result in a local.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     *
     * @param local the destination
     */
    protected void fetchShort(int byteOffset, Local local) {
        fetchShort(byteOffset);
        c.store(local);
    }

    /**
     * Fetch an unsigned short from [ip+byteOffset].
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     *
     */
    protected void fetchUShort(int byteOffset) {
        fetch(USHORT, byteOffset, false);
    }

    /**
     * Fetch an unsigned short from [ip+byteOffset], and store the result in a local.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     *
     * @param local the destination
     */
    protected void fetchUShort(int byteOffset, Local local) {
        fetchUShort(byteOffset);
        c.store(local);
    }

    /**
     * Fetch an int from [ip+byteOffset].
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     *
     */
    protected void fetchInt(int byteOffset) {
        fetch(INT, byteOffset, false);
    }

    /**
     * Fetch an int from [ip+byteOffset], and store the result in a local.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     *
     * @param local the destination
     */
    protected void fetchInt(int byteOffset, Local local) {
        fetchInt(byteOffset);
        c.store(local);
    }

    /**
     * Update the ip to point to the next instruction.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     */
    protected void iparmNone() {
        incIp(1);
    }

    /**
     * Fetch a signed byte from [ip+1] and update the ip to point to the next instruction.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     */
    protected void iparmByte() {
        fetchByte(1, iparm);
        incIp(2);
    }

    /**
     * Fetch an unsigned byte from [ip+1] and update the ip to point to the next instruction.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     */
    protected void iparmUByte() {
        fetchUByte(1, iparm);
        incIp(2);
    }


    /*-----------------------------------------------------------------------*\
     *                               Constants                               *
    \*-----------------------------------------------------------------------*/

    /**
     * Push a constant null.
     *
     * <p>
     * Java Stack: ... -> ..., INT
     * <p>
     *
     * @param n the integer value
     */
    protected void do_const_null() {
        c.literal(0);
        c.push();
    }

    /**
     * Push a constant integer value.
     *
     * <p>
     * Java Stack: ... -> ..., INT
     * <p>
     *
     * @param n the integer value
     */
    protected void do_const(int n) {
        c.literal(n);
        c.push();
    }

    /**
     * Push a constant byte value.
     *
     * <p>
     * Java Stack: ... -> ..., INT
     * <p>
     */
    protected void do_const_byte() {
        fetchByte(0);
        c.push();
        incIp(1);
    }

    /**
     * Push a constant short value.
     *
     * <p>
     * Java Stack: ... -> ..., INT
     * <p>
     */
    protected void do_const_short() {
        fetchShort(0);
        c.push();
        incIp(2);
    }

    /**
     * Push a constant char value.
     *
     * <p>
     * Java Stack: ... -> ..., INT
     * <p>
     */
    protected void do_const_char() {
        fetchUShort(0);
        c.push();
        incIp(2);
    }

    /**
     * Push a constant int value.
     *
     * <p>
     * Java Stack: ... -> ..., INT
     * <p>
     */
    protected void do_const_int() {
        fetchInt(0);
        c.push();
        incIp(4);
    }

    /**
     * Push a constant long value.
     *
     * <p>
     * Java Stack: ... -> ..., LONG
     * <p>
     */
    protected void do_const_long() {
        fetchInt(0);
        fetchInt(0);
        if (!c.isBigEndian()) {
            c.swap();
        }
        c.push();
        c.push();
        incIp(8);
    }

    /**
     * Push a constant floa value.
     *
     * <p>
     * Java Stack: ... -> ..., FLOAT
     * <p>
     */
    protected void do_const_float() {
        do_const_int();
    }

    /**
     * Push a constant double value.
     *
     * <p>
     * Java Stack: ... -> ..., DOUBLE
     * <p>
     */
    protected void do_const_double() {
        do_const_long();
    }

    /**
     * Push a constant object value.
     *
     * <p>
     * Java Stack: ... -> ..., OOP
     * <p>
     *
     * @param n the index into the class object table
     */
    protected void do_object(int n) {
        c.literal(n);
        getObject();
        c.push();
    }

    /**
     * Push a constant object value.
     *
     * <p>
     * Java Stack: ... -> ..., OOP
     * <p>
     */
    protected void do_object() {
        getValue();
        getObject();
        c.push();
    }

    /*-----------------------------------------------------------------------*\
     *                          Access to locals                             *
    \*-----------------------------------------------------------------------*/

    /**
     * Push a single word local.
     *
     * <p>
     * Java Stack: ... -> ..., VALUE
     * <p>
     *
     * @param n the index to local
     */
    protected void do_load(int n) {
        c.literal(n);
        getLocal();
        c.push();
    }

    /**
     * Push a single word local.
     *
     * <p>
     * Java Stack: ... -> ..., VALUE
     * <p>
     */
    protected void do_load() {
        getValue();
        getLocal();
        c.push();
    }

    /**
     * Pop a single word local.
     *
     * <p>
     * Java Stack: ..., VALUE -> ...
     * <p>
     *
     * @param n the index to local
     */
    protected void do_store(int n) {
        c.pop(INT);
        c.literal(n);
        setLocal();
    }

    /**
     * Pop a single word local.
     *
     * <p>
     * Java Stack: ..., VALUE -> ...
     * <p>
     */
    protected void do_store() {
        c.pop(INT);
        getValue();
        setLocal();
    }

    /**
     * Push a double word local.
     *
     * <p>
     * Java Stack: ... -> ..., LONG
     * <p>
     */
    protected void do_load_i2() {
        getValue();
        getLocal();
        c.push();
        getValue();
        c.literal(1).add();
        getLocal();
        c.push();
    }

    /**
     * Pop a double word local.
     *
     * <p>
     * Java Stack: ..., LONG -> ...
     * <p>
     */
    protected void do_store_i2() {
        c.pop(INT);
        getValue();
        setLocal();
        c.pop(INT);
        getValue();
        c.literal(1).add();
        setLocal();
    }

    /**
     * Increment a single word local.
     *
     * <p>
     * Java Stack: ... -> ...
     * <p>
     */
    protected void do_inc() {
        getValue();
        getLocal();
        c.literal(1).add();
        getValue();
        setLocal();
    }

    /**
     * Decrement a single word local.
     *
     * <p>
     * Java Stack: ... -> ...
     * <p>
     */
    protected void do_dec() {
        getValue();
        getLocal();
        c.literal(1).sub();
        getValue();
        setLocal();
    }

    /**
     * Increment a single word parameter.
     *
     * <p>
     * Java Stack: ... -> ...
     * <p>
     */
    protected void do_incparm() {
        getValue();
        getParm();
        c.literal(1).add();
        getValue();
        setParm();
    }

    /**
     * Decrement a single word parameter.
     *
     * <p>
     * Java Stack: ... -> ...
     * <p>
     */
    protected void do_decparm() {
        getValue();
        getParm();
        c.literal(1).sub();
        getValue();
        setParm();
    }


    /*-----------------------------------------------------------------------*\
     *                         Access to parameters                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Push a single word parm.
     *
     * <p>
     * Java Stack: ... -> ..., VALUE
     * <p>
     *
     * @param n the index to local
     */
    protected void do_loadparm(int n) {
        c.literal(n);
        getParm();
        c.push();
    }

    /**
     * Push a single word parm.
     *
     * <p>
     * Java Stack: ... -> ..., VALUE
     * <p>
     */
    protected void do_loadparm() {
        getValue();
        getParm();
        c.push();
    }

    /**
     * Pop a single word parm.
     *
     * <p>
     * Java Stack: ..., VALUE -> ...
     * <p>
     */
    protected void do_storeparm() {
        c.pop(INT);
        getValue();
        setParm();
    }

    /**
     * Push a double word parm.
     *
     * <p>
     * Java Stack: ... -> ..., LONG
     * <p>
     */
    protected void do_loadparm_i2() {
        getValue();
        getParm();
        c.push();
        getValue();
        c.literal(1).add();
        getParm();
        c.push();
    }

    /**
     * Pop a double word parm.
     *
     * <p>
     * Java Stack: ..., VALUE -> ...
     * <p>
     *
     */
    protected void do_storeparm_i2() {
        c.pop(INT);
        getValue();
        setParm();
        c.pop(INT);
        getValue();
        c.literal(1).add();
        setParm();
    }


    /*-----------------------------------------------------------------------*\
     *                               Branching                               *
    \*-----------------------------------------------------------------------*/

    /**
     * Backward branch target in system.
     *
     * <p>
     * Java Stack:  _  ->  _
     * <p>
     */
    protected void do_bbtarget_sys() {
    }

    /**
     * Backward branch target in applicaton.
     *
     * <p>
     * Java Stack:  _  ->  _
     * <p>
     */
    protected void do_bbtarget_app() {
         reschedule(1);
    }

    /**
     * Unconditional branch.
     *
     * <p>
     * Java Stack: ... -> ...  (Forward branches);
     * <p>
     * Java Stack:  _  ->  _   (Backward branches);
     * <p>
     */
    protected void do_goto() {
        c.load(ip);
        c.load(iparm);
        c.add();
        c.store(ip);
        // If there is no reschedule bytecode then call do_reschedule() here;
        Assert.that(nextBytecode == null);
    }

    /**
     * Conditional branch.
     *
     * <p>
     * Java Stack: ..., VALUE, [VALUE] -> ...  (Forward branches);
     * <p>
     * Java Stack:      VALUE, [VALUE] ->  _   (Backward branches);
     * <p>
     *
     * @param operands the number of operands (1 or 2)
     * @param cc       the condition code
     * @param type     the type of the data to compare
     */
    protected void do_if(int operands, int cc, Type type) {

        /*
         * Change type OOP to INT.
         */
        if (type == OOP) {
            type = INT;
        }

        /*
         * Pop the first parameter from the runtime stack.
         */
        c.pop(type);

        /*
         * Get the second parameter.
         */
        if (operands == 2) {
            c.pop(type);
        } else {
            zero(type);
        }

        /*
         * Swap them around and make the appropriate test.
         */
        c.swap();
        switch (cc) {
            case EQ: c.eq(); break;
            case NE: c.ne(); break;
            case LT: c.lt(); break;
            case LE: c.le(); break;
            case GT: c.gt(); break;
            case GE: c.ge(); break;
            default: Assert.shouldNotReachHere();
        }

        /*
         * Increment the ip as required and fetch the next bytecode.
         */
        Label done = c.label();
        c.bf(done);
        do_goto();
    c.bind(done);
    }

    /**
     * Table switch.
     *
     * <p>
     * Java Stack: KEY ->  _
     * <p>
     */
    protected void do_tableswitch(Type type) {
        tableswitch(type.getStructureSize());
    }

    /**
     * General table switch.
     *
     * <p>
     * Java Stack: KEY ->  _
     * <p>
     */
    private void tableswitch(int size) {
        Local key    = c.local(INT);
        Local p      = c.local(REF);
        Local low    = c.local(INT);
        Local high   = c.local(INT);
        Local def    = c.local(INT);
        Label lookup = c.label();
        Label usedef = c.label();
        Label done   = c.label();

// FIXME - This needs fixing the reference point for address calcuylation is wrong,.
//         The pad calculation is also wrong.

        /*
         * Workout the address of the first int after the initial bytecode.
         */
        if (c.tableSwitchPadding()) {
            c.literal(3);
            c.load(ip);
            c.force(INT);
            c.literal(3);
            c.and();
            c.sub();
        } else {
            c.literal(1);
        }
        c.load(ip);
        c.add();
        c.store(p);

        /*
         * Read the low bound.
         */
        c.load(p);
        c.read(INT);
        c.store(low);

        /*
         * Read the high bound.
         */
        c.load(p);
        c.literal(4);
        c.add();
        c.read(INT);
        c.store(high);

        /*
         * Get the key.
         */
        c.pop(INT);
        c.store(key);

        /*
         * Test to see if key < low || key > high.
         */
        c.load(key);
        c.load(low);
        c.lt().bt(usedef);
        c.load(key);
        c.load(high);
        c.gt().bf(lookup);

        /*
         * Load value with the default offset.
         */
    c.bind(usedef);
        c.load(p);
        c.literal(4+4);
        c.add();
        c.store(p);
        c.br(done);

    c.bind(lookup);
        /*
         * Load value with the target offset.
         */
        c.load(p);
        c.literal(4+4+size);
        c.add();
        c.load(key);
        c.literal(size);
        c.mul();
        c.add();
        c.store(p);

        /*
         * Read the offset, place in value and do a goto.
         */
    c.bind(done);
        c.load(p);
        c.read(size == 2 ? SHORT : INT);
        c.store(iparm);
        do_goto();
    }


    /*-----------------------------------------------------------------------*\
     *                          Static field access                          *
    \*-----------------------------------------------------------------------*/

    /**
     * class_getstatic.
     *
     * <p>
     * Java Stack: _ -> VALUE
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_class_getstatic(Type t) {
        getCP();                        // Ref
        getStatic(t);
        c.push();                       // Value
    }

    /**
     * getstatic.
     *
     * <p>
     * Java Stack: CLASS -> VALUE
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_getstatic(Type t) {
        c.pop(OOP);                     // Ref
        getStatic(t);
        c.push();                       // Value
    }

    /**
     * class_putstatic.
     *
     * <p>
     * Java Stack: VALUE -> _
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_class_putstatic(Type t) {
        c.pop(t);                       // Value
        getCP();                        // Ref
        putStatic(t);
    }

    /**
     * putstatic.
     *
     * <p>
     * Java Stack: VALUE, CLASS -> _
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_putstatic(Type t) {
        c.pop(OOP);                     // Ref
        c.pop(t);                       // Value
        c.swap();
        putStatic(t);
    }


    /*-----------------------------------------------------------------------*\
     *                         Instance field access                         *
    \*-----------------------------------------------------------------------*/

    /**
     * this_getfield.
     *
     * <p>
     * Java Stack: ... -> ..., VALUE
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_this_getfield(Type t) {
        c.literal(0);
        getLocal();                     // Ref
        c.force(OOP);
        getValue();
        read(t, NOCHECK);
        c.push();                       // Value
    }

    /**
     * getfield.
     *
     * <p>
     * Java Stack: ..., OOP -> ..., VALUE
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_getfield(Type t) {
        c.pop(OOP);                     // Ref
        getValue();
        read(t, NULLCHECK);
        c.push();                       // Value
    }

    /**
     * this_putfield.
     *
     * <p>
     * Java Stack: ..., VALUE -> ...
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_this_putfield(Type t) {
        c.literal(0);
        getLocal();                     // Ref
        c.force(OOP);
        c.pop(t.getPrimitiveType());    // Value
        getValue();                     // Index
        write(t, NOCHECK);
    }

    /**
     * putfield.
     *
     * <p>
     * Java Stack: ..., OOP, VALUE -> ...
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_putfield(Type t) {
        c.pop(t.getPrimitiveType());    // Value
        c.pop(OOP);                     // Ref
        c.swap();
        getValue();                     // Index
        write(t, NULLCHECK);
    }


    /*-----------------------------------------------------------------------*\
     *                           Array field access                          *
    \*-----------------------------------------------------------------------*/

    /**
     * aload.
     *
     * <p>
     * Java Stack: ..., OOP, INT -> ..., VALUE
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_aload(Type t) {
        c.pop(INT);                     // Index
        c.pop(OOP);                     // Ref
        c.swap();
        read(t, BOUNDSCHECK);
        c.push();                       // Value
    }

    /**
     * astore.
     *
     * <p>
     * Java Stack: ..., OOP, INT, VALUE -> ...
     * <p>
     * Java Stack:      OOP, INT, VALUE -> _ (when type = OOP)
     * <p>
     *
     * @param t the operation data type
     */
    protected void do_astore(Type t) {
        c.pop(t.getPrimitiveType());    // Value
        c.pop(INT);                     // Index
        c.pop(OOP);                     // Ref

        c.begin();
        Local ref   = localStore(OOP);
        Local index = localStore(INT);
        Local value = localStore(t.getPrimitiveType());
        c.load(ref);
        c.load(value);
        c.load(index);
        c.end();

        write(t, (t == OOP) ? STORECHECK : BOUNDSCHECK);
    }


    /*-----------------------------------------------------------------------*\
     *                           Invoke instructions                         *
    \*-----------------------------------------------------------------------*/

    /**
     * invokestatic.
     *
     * <p>
     * Java Stack: [arg1, [arg2 ...]], CLASS -> [VALUE] (Stack grows up)
     * <p>
     * Java Stack: [[... arg2], arg1], CLASS -> [VALUE] (Stack grows down)
     * <p>
     *
     * @param t the return type
     */
    protected void do_invokestatic(Type t) {
        c.pop(OOP);         // Get the class
        invokestatic(t, Compiler.C_JVM_DYNAMIC);
        if (t != VOID) {
            c.push();
        }
    }

    /**
     * invokesuper.
     *
     * <p>
     * Java Stack: OOP, [arg1, [arg2 ...]], CLASS -> [VALUE] (Stack grows up)
     * <p>
     * Java Stack: [[... arg2], arg1], OOP, CLASS -> [VALUE] (Stack grows down)
     * <p>
     *
     * @param t the return type
     */
    protected void do_invokesuper(Type t) {
        c.pop(OOP);         // Get the class
        invokesuper(t, Compiler.C_JVM_DYNAMIC);
        if (t != VOID) {
            c.push();
        }
    }

    /**
     * invokevirtual.
     *
     * <p>
     * Java Stack: OOP, [arg1, [arg2 ...]] -> [VALUE] (Stack grows up)
     * <p>
     * Java Stack: [[... arg2], arg1], OOP -> [VALUE] (Stack grows down)
     * <p>
     *
     * @param t the return type
     */
    protected void do_invokevirtual(Type t) {
        c.peekReceiver();   // Get the receiver
        invokevirtual(t, Compiler.C_JVM_DYNAMIC);
        if (t != VOID) {
            c.push();
        }
    }

    /**
     * findslot.
     *
     * <p>
     * Java Stack: OOP, CLASS -> INT
     * <p>
     */
    protected void do_findslot() {
        c.pop(OOP);         // Get the interface class
        c.pop(OOP);         // Get the receiver
        c.swap();
        findslot();
        c.push();
    }

    /**
     * invokeslot.
     *
     * <p>
     * Java Stack: OOP, [arg1, [arg2 ...]], SLOT -> [VALUE] (Stack grows up)
     * <p>
     * Java Stack: [[... arg2], arg1], OOP, SLOT -> [VALUE] (Stack grows down)
     * <p>
     *
     * @param t the return type
     */
    protected void do_invokeslot(Type t) {
        c.pop(INT);         // Get the slot number
        c.peekReceiver();   // Get the receiver
        c.swap();
        invokeslot(t, Compiler.C_JVM_DYNAMIC);
        if (t != VOID) {
            c.push();
        }
    }

    /**
     * Return from a method.
     *
     * <p>
     * Java Stack: [VALUE] -> _
     * <p>
     *
     * @param t type of data to return
     */
    protected void do_return(Type t) {
        if (t != VOID) {
            c.pop(t);
        }
        c.ret(t);
    }

    /**
     * Pop one or two words from the Java stack.
     *
     * <p>
     * Compiler Stack: ..., INT/LONG -> ...
     * <p>
     */
    protected void do_pop(int n) {
       c.pop((n==1) ? INT : LONG);
       c.drop();
    }

    /**
     * invokenative.
     *
     * <p>
     * Compiler Stack: [[... arg2], arg1] -> [VALUE]
     * <p>
     */
    protected void do_invokenative(Type t) {
        getValueAsWordOffset();
        c.literal(nativeTable);
        c.add();
        c.read(REF);
        c.jump();
    }


    /*-----------------------------------------------------------------------*\
     *                             ALU instructions                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Pop two things from the runtime stack to the compiler stack swapping them.
     *
     * <p>
     * Java Stack:     ..., VALUE1, VALUE2 -> ...
     * <p>
     * Compiler Stack: ..., -> ..., VALUE1, VALUE2
     * <p>
     *
     * @param t the data type.
     */
    private Compiler popTwoSwap(Type t) {
        c.pop(t).pop(t).swap();
        return c;
    }

    /**
     * Add two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1+VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_add(Type t) {
        popTwoSwap(t).add().push();
    }

    /**
     * Subtract two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1-VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_sub(Type t) {
        popTwoSwap(t).sub().push();
    }

    /**
     * And two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1&VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_and(Type t) {
        popTwoSwap(t).and().push();
    }

    /**
     * Or two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1|VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_or(Type t) {
        popTwoSwap(t).or().push();
    }

    /**
     * Xor two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1^VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_xor(Type t) {
        popTwoSwap(t).xor().push();
    }

    /**
     * Signed left shift two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1<<(VALUE2&1f)
     * <p>
     *
     * @param t the data type.
     */
    protected void do_shl(Type t) {
        c.pop(INT).pop(t).swap().shl().push();
    }

    /**
     * Right shift two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1>>(VALUE2&1f)
     * <p>
     *
     * @param t the data type.
     */
    protected void do_shr(Type t) {
        c.pop(INT).pop(t).swap().shr().push();
    }

    /**
     * Unsigned right shift two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1>>>(VALUE2&1f)
     * <p>
     *
     * @param t the data type.
     */
    protected void do_ushr(Type t) {
        c.pop(INT).pop(t).swap().ushr().push();
    }

    /**
     * Multiply two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1*VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_mul(Type t) {
        popTwoSwap(t).mul().push();
    }

    /**
     * Divide two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1/VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_div(Type t) {
        popTwoSwap(t); div(t); c.push();
    }

    /**
     * Rem two things.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1%VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_rem(Type t) {
        popTwoSwap(t); rem(t); c.push();
    }

    /**
     * Negate something.
     *
     * <p>
     * Java Stack: ..., VALUE -> ..., -VALUE1
     * <p>
     *
     * @param t the data type.
     */
    protected void do_neg(Type t) {
        c.pop(t).neg().push();
    }


    /*-----------------------------------------------------------------------*\
     *                              Convertions                              *
    \*-----------------------------------------------------------------------*/

    /**
     * Convert int to byte.
     *
     * <p>
     * Java Stack: ..., INT -> ..., INT
     * <p>
     */
    protected void do_i2b() {
        c.pop(INT).convert(BYTE).push();
    }

    /**
     * Convert int to short.
     *
     * <p>
     * Java Stack: ..., INT -> ..., INT
     * <p>
     */
    protected void do_i2s() {
        c.pop(INT).convert(SHORT).push();
    }

    /**
     * Convert int to char.
     *
     * <p>
     * Java Stack: ..., INT -> ..., INT
     * <p>
     */
    protected void do_i2c() {
        c.pop(INT).convert(USHORT).push();
    }

    /**
     * Convert long to int.
     *
     * <p>
     * Java Stack: ..., LONG -> ..., INT
     * <p>
     */
    protected void do_l2i() {
        c.pop(LONG).convert(INT).push();
    }

    /**
     * Convert int to long.
     *
     * <p>
     * Java Stack: ..., INT -> ..., LONG
     * <p>
     */
    protected void do_i2l() {
        c.pop(INT).convert(LONG).push();
    }

    /**
     * Convert int to float.
     *
     * <p>
     * Java Stack: ..., INT -> ..., FLOAT
     * <p>
     */
    protected void do_i2f() {
        c.pop(INT).convert(FLOAT).push();
    }

    /**
     * Convert long to float.
     *
     * <p>
     * Java Stack: ..., LONG -> ..., FLOAT
     * <p>
     */
    protected void do_l2f() {
        c.pop(LONG).convert(FLOAT).push();
    }

    /**
     * Convert float to int.
     *
     * <p>
     * Java Stack: ..., FLOAT -> ..., INT
     * <p>
     */
    protected void do_f2i() {
        c.pop(FLOAT).convert(INT).push();
    }

    /**
     * Convert float to long.
     *
     * <p>
     * Java Stack: ..., FLOAT -> ..., LONG
     * <p>
     */
    protected void do_f2l() {
        c.pop(FLOAT).convert(LONG).push();
    }

    /**
     * Convert int to double.
     *
     * <p>
     * Java Stack: ..., INT -> ..., DOUBLE
     * <p>
     */
    protected void do_i2d() {
        c.pop(INT).convert(DOUBLE).push();
    }

    /**
     * Convert long to double.
     *
     * <p>
     * Java Stack: ..., LONG -> ..., DOUBLE
     * <p>
     */
    protected void do_l2d() {
        c.pop(LONG).convert(DOUBLE).push();
    }

    /**
     * Convert float to double.
     *
     * <p>
     * Java Stack: ..., FLOAT -> ..., DOUBLE
     * <p>
     */
    protected void do_f2d() {
        c.pop(FLOAT).convert(DOUBLE).push();
    }

    /**
     * Convert double to int.
     *
     * <p>
     * Java Stack: ..., DOUBLE -> ..., INT
     * <p>
     */
    protected void do_d2i() {
        c.pop(DOUBLE).convert(INT).push();
    }

    /**
     * Convert double to long.
     *
     * <p>
     * Java Stack: ..., DOUBLE -> ..., LONG
     * <p>
     */
    protected void do_d2l() {
        c.pop(DOUBLE).convert(LONG).push();
    }

    /**
     * Convert double to float.
     *
     * <p>
     * Java Stack: ..., DOUBLE -> ..., FLOAT
     * <p>
     */
    protected void do_d2f() {
        c.pop(DOUBLE).convert(FLOAT).push();
    }


    /*-----------------------------------------------------------------------*\
     *        Complex instrcutions implemented with external function        *
    \*-----------------------------------------------------------------------*/

    /**
     * Throw an exception.
     *
     * <p>
     * Java Stack: OOP -> _
     * <p>
     */
    protected void do_throw() {
        c.pop(OOP);
        super.do_throw();
    }

    /**
     * Start an exception handler.
     *
     * <p>
     * Java Stack: OOP -> _
     * <p>
     */
    protected void do_catch() {
        super.do_catch();
        c.push();
    }

    /**
     * Execute a monitor enter.
     *
     * <p>
     * Java Stack: OOP -> _
     * <p>
     */
    protected void do_monitorenter() {
        c.pop(OOP);
        super.do_monitorenter();
    }

    /**
     * Execute a monitor exit.
     *
     * <p>
     * Java Stack: OOP -> _
     * <p>
     */
    protected void do_monitorexit() {
        c.pop(OOP);
        super.do_monitorexit();
    }

    /**
     * Get the length of an array.
     *
     * <p>
     * Java Stack: ..., OOP -> ..., INT
     * <p>
     */
    protected void do_arraylength() {
        c.pop(OOP);
        super.do_arraylength();
        c.push();
    }

    /**
     * Allocate an object.
     *
     * <p>
     * Java Stack: ..., CLASS -> ..., OOP
     * <p>
     */
    protected void do_new() {
        c.pop(OOP);
        super.do_new();
        c.push();
    }

    /**
     * Allocate a new array.
     *
     * <p>
     * Java Stack: SIZE, CLASS -> ..., OOP
     * <p>
     */
    protected void do_newarray() {
        c.pop(OOP).pop(INT).swap();
        super.do_newarray();
        c.push();
    }

    /**
     * Allocate a new array dimension.
     *
     * <p>
     * Java Stack: OOP, SIZE -> ..., OOP
     * <p>
     */
    protected void do_newdimension() {
        c.pop(INT).pop(OOP).swap();
        super.do_newdimension();
        c.push();
    }

    /**
     * Instanceof.
     *
     * <p>
     * Java Stack: ..., OOP, CLASS -> ..., INT
     * <p>
     */
    protected void do_instanceof() {
        c.pop(OOP).pop(INT).swap();
        super.do_instanceof();
        c.push();
    }

    /**
     * Checkcast.
     *
     * <p>
     * Java Stack: ..., OOP, CLASS -> ..., OOP
     * <p>
     */
    protected void do_checkcast() {
        c.pop(OOP).pop(INT).swap();
        super.do_checkcast();
        c.push();
    }

    /**
     * Lookup.
     *
     * <p>
     * Java Stack: OOP, CLASS -> VALUE
     * <p>
     *
     * @param t the type of array to lookup
     */
    protected void do_lookup(Type t) {
        c.pop(OOP).pop(INT).swap();
        super.do_lookup(t);
        c.push();
    }

    /**
     * Reserved.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     *
     * @param n ignored parameter
     */
    protected void do_res(int n) {
        shouldNotReachHere();
    }


    public static void printMmap(MethodMap mmap, String name) {
        System.out.println("Method Map" + name);
        System.out.print("  locals = ");
        printMap(mmap.getLocalSlotCount(), mmap.getLocalOopMap());
        System.out.println();
        System.out.print("  parms  = ");
        printMap(mmap.getParameterSlotCount(), mmap.getParameterOopMap());
        System.out.println();
    }

    public static void printMap(int count, byte[] oopmap) {
        int offset = 0;
        int mask = 1;
        for (int i = 0 ; i < count ; i++) {
            if ((oopmap[offset] & mask) == 0) {
                System.out.print("0");
            } else {
                System.out.print("1");
            }
            mask <<= 1;
            if (mask == 0x100) {
                offset++;
                mask = 1;
            }
        }
    }

    /**
     * main()
     */
    public static void main(String[] args) {
        MethodMap mmap = new MethodMap();
        Compiler c = (new Interpreter()).produce(mmap);
        Linker linker = Compilation.newLinker(c);
        linker.print();
        printMmap(mmap, "interpreter");
    }


}
