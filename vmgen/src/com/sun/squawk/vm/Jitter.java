///if[EXCLUDE]
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

import com.sun.squawk.compiler.*;
import com.sun.squawk.compiler.Compiler;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.Hashtable; // Version without synchronization


/**
 * The Squawk Jitter.
 *
 * @author   Nik Shaylor
 */
public class Jitter extends JitterSwitch {

    /**
     * The current method.
     */
    private byte[] method;

    /**
     * The current ip address.
     */
    private int ip;

    /**
     * The current op code.
     */
    private int opcode;

    /**
     * The method's paramaeters.
     */
    private Local[] parm;

    /**
     * The method's local variables.
     */
    private Local[] local;

    /**
     * The hashtable of branch targets
     */
    private Hashtable labels;

    /**
     * The Constructor.
     */
    public Jitter() {
    }

    /*-----------------------------------------------------------------------*\
     *                          The producer main code                       *
    \*-----------------------------------------------------------------------*/

    /**
     * The Producer.
     */
    private Compiler produce(byte[] method) {
        this.method = method;
        labels = new Hashtable();

        // TODO: Setup local and parm arrays.

        commentBox("Squawk jitted code");
        c.enter(Compiler.E_ADDRESS);
            mp = c.local(MP);

            /*
             * Iterate through the bytecodes building the native code.
             */
            ip = 0;
            while(ip < method.length) {
                bindLabel(ip);
                opcode = fetchUByte();
                do_switch(opcode);
            }
        c.leave();

        c.compile();
        return c;
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
     * Add 256 to the next unsigned byte and jump to that bytecode execution.
     */
    protected void do_escape() {
/*if[FLOATS]*/
        opcode += 256;
        do_switch(opcode);
/*else[FLOATS]*/
//      Assert.shouldNotReachHere("Floats not supported");
/*end[FLOATS]*/
    }

    /**
     * Or the (parameter<<8) into the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_wide(int n) {
        opcode = fetchUByte() + OPC.WIDE_DELTA;
        iparm  = fetchUByte() | (n<<8);
        do_switch(opcode);
    }

    /**
     * Load the inlined short as the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_wide_short() {
        opcode = fetchUByte() + OPC.WIDE_DELTA;
        iparm  = fetchShort();
        do_switch(opcode);
    }

    /**
     * Load the inlined int as the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_wide_int() {
        opcode = fetchUByte() + OPC.WIDE_DELTA;
        iparm  = fetchInt();
        do_switch(opcode);
    }

    /**
     * Or the (parameter<<8) in to the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_escape_wide(int n) {
/*if[FLOATS]*/
        opcode = fetchUByte() + 256 + OPC.ESCAPE_WIDE_DELTA;
        iparm  = fetchUByte() | (n<<8);
        do_switch(opcode);
/*else[FLOATS]*/
//      Assert.shouldNotReachHere("Floats not supported");
/*end[FLOATS]*/
    }

    /**
     * Load the inlined short as the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_escape_wide_short() {
/*if[FLOATS]*/
        opcode = fetchUByte() + 256 + OPC.ESCAPE_WIDE_DELTA;
        iparm  = fetchShort();
        do_switch(opcode);
/*else[FLOATS]*/
//      Assert.shouldNotReachHere("Floats not supported");
/*end[FLOATS]*/
}

    /**
     * Load the inlined int as the value of the next bytecode and then
     * dispatch to the wide version of the opcode.
     */
    protected void do_escape_wide_int() {
/*if[FLOATS]*/
        opcode = fetchUByte() + 256 + OPC.ESCAPE_WIDE_DELTA;
        iparm  = fetchInt();
        do_switch(opcode);
/*else[FLOATS]*/
//      Assert.shouldNotReachHere("Floats not supported");
/*end[FLOATS]*/
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
        return method[ip++];
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
        if (c.isBigEndian()) {
            int b1 = fetchByte();
            int b2 = fetchUByte();
            return (b1 << 8) | b2;
        } else {
            int b1 = fetchUByte();
            int b2 = fetchByte();
            return (b2 << 8) | b1;
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
        if (c.isBigEndian()) {
            return (b1 << 8) | b2;
        } else {
            return (b2 << 8) | b1;
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
        if (c.isBigEndian()) {
            return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
        } else {
            return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
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
        if (c.isBigEndian()) {
            return (b1 << 56) | (b2 << 48) | (b3 << 40) | (b4 << 32) | (b5 << 24) | (b6 << 16) | (b7 << 8) | b8;
        } else {
            return (b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) | (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        }
    }

    /**
     * Fetch a float from ip++.
     *
     * @return the value
     */
    protected float fetchFloat() {
/*if[FLOATS]*/
        return Float.intBitsToFloat(fetchInt());
/*else[FLOATS]*/
//      throw new Error("No floating poinnt");
/*end[FLOATS]*/
    }

    /**
     * Fetch a double from ip++.
     *
     * @return the value
     */
    protected double fetchDouble() {
/*if[FLOATS]*/
        return Double.longBitsToDouble(fetchLong());
/*else[FLOATS]*/
//      throw new Error("No floating poinnt");
/*end[FLOATS]*/
    }


    /*-----------------------------------------------------------------------*\
     *                               Constants                               *
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
        c.literal(iparm);
    }

    /**
     * Push a constant null.
     *
     * <p>
     * Compiler Stack: ... -> ..., REF
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
     * Compiler Stack: ... -> ..., INT
     * <p>
     *
     * @param n the integer value
     */
    protected void do_const(int n) {
        c.literal(n);
    }

    /**
     * Push a constant byte value.
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     */
    protected void do_const_byte() {
        c.literal(fetchByte());
    }

    /**
     * Push a constant short value.
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     */
    protected void do_const_short() {
        c.literal(fetchShort());
    }

    /**
     * Push a constant char value.
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     */
    protected void do_const_char() {
        c.literal(fetchUShort());
    }

    /**
     * Push a constant int value.
     *
     * <p>
     * Compiler Stack: ... -> ..., INT
     * <p>
     */
    protected void do_const_int() {
        c.literal(fetchInt());
    }

    /**
     * Push a constant long value.
     *
     * <p>
     * Compiler Stack: ... -> ..., LONG
     * <p>
     */
    protected void do_const_long() {
        c.literal(fetchLong());
    }

    /**
     * Push a constant float value.
     *
     * <p>
     * Compiler Stack: ... -> ..., FLOAT
     * <p>
     */
    protected void do_const_float() {
        c.literal(fetchFloat());
    }

    /**
     * Push a constant double value.
     *
     * <p>
     * Compiler Stack: ... -> ..., DOUBLE
     * <p>
     */
    protected void do_const_double() {
        c.literal(fetchDouble());
    }

    /**
     * Push a constant object value.
     *
     * <p>
     * Compiler Stack: ... -> ..., OOP
     * <p>
     *
     * @param n the index into the class object table
     */
    protected void do_object(int n) {
        c.literal(n);
        getObject();
    }

    /**
     * Push a constant object value.
     *
     * <p>
     * Compiler Stack: ... -> ..., OOP
     * <p>
     */
    protected void do_object() {
        getValue();
        getObject();
    }


    /*-----------------------------------------------------------------------*\
     *                          Access to locals                             *
    \*-----------------------------------------------------------------------*/

    /**
     * Push a single word local.
     *
     * <p>
     * Compiler Stack: ... -> ..., VALUE
     * <p>
     *
     * @param n the index to local
     */
    protected void do_load(int n) {
        c.load(local[n]);
    }

    /**
     * Push a single word local.
     *
     * <p>
     * Compiler Stack: ... -> ..., VALUE
     * <p>
     */
    protected void do_load() {
        c.load(local[iparm]);
    }

    /**
     * Pop a single word local.
     *
     * <p>
     * Compiler Stack: ..., VALUE -> ...
     * <p>
     *
     * @param n the index to local
     */
    protected void do_store(int n) {
        c.store(local[n]);
    }

    /**
     * Pop a single word local.
     *
     * <p>
     * Compiler Stack: ..., VALUE -> ...
     * <p>
     */
    protected void do_store() {
        c.store(local[iparm]);
    }

    /**
     * Push a double word local.
     *
     * <p>
     * Compiler Stack: ... -> ..., LONG
     * <p>
     */
    protected void do_load_i2() {
        do_load();
    }

    /**
     * Pop a double word local.
     *
     * <p>
     * Compiler Stack: ..., LONG -> ...
     * <p>
     */
    protected void do_store_i2() {
        do_store();
    }

    /**
     * Increment a single word local.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     */
    protected void do_inc() {
        do_load();
        c.literal(1).add();
        do_store();
    }

    /**
     * Decrement a single word local.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     */
    protected void do_dec() {
        do_load();
        c.literal(1).sub();
        do_store();
    }


    /*-----------------------------------------------------------------------*\
     *                         Access to parameters                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Push a single word parm.
     *
     * <p>
     * Compiler Stack: ... -> ..., VALUE
     * <p>
     *
     * @param n the index to local
     */
    protected void do_loadparm(int n) {
        c.load(parm[n]);
    }

    /**
     * Push a single word parm.
     *
     * <p>
     * Compiler Stack: ... -> ..., VALUE
     * <p>
     */
    protected void do_loadparm() {
        c.load(parm[iparm]);
    }

    /**
     * Pop a single word parm.
     *
     * <p>
     * Compiler Stack: ..., VALUE -> ...
     * <p>
     */
    protected void do_storeparm() {
        c.store(parm[iparm]);
    }

    /**
     * Push a double word parm.
     *
     * <p>
     * Compiler Stack: ... -> ..., LONG
     * <p>
     */
    protected void do_loadparm_i2() {
        do_loadparm();
    }

    /**
     * Pop a double word parm.
     *
     * <p>
     * Compiler Stack: ..., VALUE -> ...
     * <p>
     *
     */
    protected void do_storeparm_i2() {
        do_storeparm();
    }

    /**
     * Increment a single word parameter.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     */
    protected void do_incparm() {
        do_loadparm();
        c.literal(1).add();
        do_storeparm();
    }

    /**
     * Decrement a single word parameter.
     *
     * <p>
     * Compiler Stack: ... -> ...
     * <p>
     */
    protected void do_decparm() {
        do_loadparm();
        c.literal(1).sub();
        do_storeparm();
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
        /*
         * The label can also be the target of a forward branch, and in this
         * case it will already have been bound. A test to see if the label
         * has scope will indicate if this has occured yet.
         */
        Label label = getLabel(ip-1);
        if (!label.isBound()) {
            c.bind(label);
        }
    }

    /**
     * Backward branch target in applicaton.
     *
     * <p>
     * Java Stack:  _  ->  _
     * <p>
     */
    protected void do_bbtarget_app() {
         do_bbtarget_sys();
         reschedule(10);
    }

    /**
     * Get a label for an ip offset into the method.
     *
     * @param offset the offset into the method
     * @return the label
     */
    private Label getLabel(int offset) {
        Integer key = new Integer(offset);
        Label label = (Label)labels.get(key);
        if (label == null) {
            label = c.label();
            labels.put(key, label);
        }
        return label;
    }

    /**
     * Bind the label for the specified location if there is one.
     *
     * @param offset the offset into the method
     */
    private void bindLabel(int offset) {
        Integer key = new Integer(offset);
        Label label = (Label)labels.get(key);
        if (label != null) {
            c.bind(label);
        }
    }

    /**
     * Unconditional branch.
     *
     * <p>
     * Compiler Stack: ... -> ...  (Forward branches);
     * <p>
     * Compiler Stack:  _  ->  _   (Backward branches);
     * <p>
     */
    protected void do_goto() {
        c.br(getLabel(ip+iparm));
    }

    /**
     * Conditional branch.
     *
     * <p>
     * Compiler Stack: ..., VALUE, [VALUE] -> ...  (Forward branches);
     * <p>
     * Compiler Stack:      VALUE, [VALUE] ->  _   (Backward branches);
     * <p>
     *
     * @param operands the number of operands (1 or 2)
     * @param cc       the condition code
     * @param type     the type of the data to compare
     */
    protected void do_if(int operands, int cc, Type type) {

        /*
         * Get the second parameter.
         */
        if (operands == 1) {
            zero(type);
        }

        /*
         * Swap them around and make the appropriate test.
         */
        switch (cc) {
            case EQ: c.eq(); break;
            case NE: c.ne(); break;
            case LT: c.lt(); break;
            case LE: c.le(); break;
            case GT: c.gt(); break;
            case GE: c.ge(); break;
        }

        c.bt(getLabel(iparm));
    }


    /**
     * Table switch.
     *
     * <p>
     * Compiler Stack: KEY ->  _
     * <p>
     */
    protected void do_tableswitch(Type type) {
        tableswitch(type.getStructureSize());
    }

    /**
     * General table switch.
     *
     * <p>
     * Compiler Stack: KEY ->  _
     * <p>
     */
    private void tableswitch(int size) {
        int delta = 0;
        int defoffset;

        /*
         * Workout the address of the first int after the initial bytecode.
         */
        if (c.tableSwitchPadding()) {
            delta = (ip + 4) & 3;
            ip += delta;
        }

        c.begin();
            Local key = localStore(INT);

            /*
             * Read the low and high values.
             */
            if (size == 2) {
                defoffset = fetchInt();
            } else {
                defoffset = fetchShort();
            }
            int low       = fetchInt();
            int high      = fetchInt();
            int start     = ip;

            /*
             * Define the array of targets.
             */
            int count = high - low + 1;
            Label[] targets = new Label[count];
            Label def = getLabel(defoffset - start);

            /*
             * Load the default target and the array of targets.
             */
            if (size == 2) {
                for (int i = 0 ; i < count ; i++) {
                     targets[i] = getLabel(fetchShort() - start);
                }
            } else {
                for (int i = 0 ; i < count ; i++) {
                     targets[i] = getLabel(fetchInt() - start);
                }
            }

            /*
             * Branch to the default if the key is too low.
             */
            c.load(key);
            c.literal(low);
            c.lt().bt(def);

            /*
             * Branch to the default if the key is too high.
             */
            c.load(key);
            c.literal(high);
            c.gt().bt(def);

            /*
             * Execute a computed goto using the value if the key.
             */
            c.load(key);
            c.literal(low).sub();
            c.literal(4).mul();
            c.literal(targets).add();
            c.jump();
        c.end();

        /*
         * Skip end padding if present.
         */
        if (c.tableSwitchEndPadding()) {
            ip += 3 - delta;
        }
    }


    /*-----------------------------------------------------------------------*\
     *                          Static field access                          *
    \*-----------------------------------------------------------------------*/

    /**
     * class_getstatic.
     *
     * <p>
     * Compiler Stack: _ -> VALUE
     * <p>
     */
    protected void do_class_getstatic(Type t) {
        getCP();                        // Ref
        getStatic(t);
    }

    /**
     * getstatic.
     *
     * <p>
     * Compiler Stack: CLASS -> VALUE
     * <p>
     */
    protected void do_getstatic(Type t) {
        getStatic(t);
    }

    /**
     * class_putstatic.
     *
     * <p>
     * Compiler Stack: VALUE -> _
     * <p>
     */
    protected void do_class_putstatic(Type t) {
        getCP();                        // Ref
        putStatic(t);
    }

    /**
     * putstatic.
     *
     * <p>
     * Compiler Stack: VALUE, CLASS -> _
     * <p>
     */
    protected void do_putstatic(Type t) {
        putStatic(t);
    }


    /*-----------------------------------------------------------------------*\
     *                         Instance field access                         *
    \*-----------------------------------------------------------------------*/

    /**
     * this_getfield.
     *
     * <p>
     * Compiler Stack: ... -> ..., VALUE
     * <p>
     */
    protected void do_this_getfield(Type t) {
        c.load(local[0]);               // Ref
        getValue();
        read(t, NOCHECK);
    }

    /**
     * getfield.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., VALUE
     * <p>
     */
    protected void do_getfield(Type t) {
        getValue();
        read(t, NULLCHECK);
    }

    /**
     * this_putfield.
     *
     * <p>
     * Compiler Stack: ..., VALUE -> ...
     * <p>
     */
    protected void do_this_putfield(Type t) {
        c.load(local[0]);               // Ref
        c.swap();
        getValue();
        write(t, NOCHECK);
    }

    /**
     * putfield.
     *
     * <p>
     * Compiler Stack: ..., OOP, VALUE -> ...
     * <p>
     */
    protected void do_putfield(Type t) {
        getValue();
        write(t, NULLCHECK);
    }


    /*-----------------------------------------------------------------------*\
     *                           Array field access                          *
    \*-----------------------------------------------------------------------*/

    /**
     * aload.
     *
     * <p>
     * Compiler Stack: ..., OOP, INT -> ..., VALUE
     * <p>
     */
    protected void do_aload(Type t) {
        read(t, BOUNDSCHECK);
    }

    /**
     * astore.
     *
     * <p>
     * Compiler Stack: ..., OOP, INT, VALUE -> ...
     * <p>
     */
    protected void do_astore(Type t) {
        c.swap();
        write(t, (t == OOP) ? STORECHECK : BOUNDSCHECK);
    }


    /*-----------------------------------------------------------------------*\
     *                           Invoke instructions                         *
    \*-----------------------------------------------------------------------*/

    /**
     * invokestatic.
     *
     * <p>
     * Compiler Stack: [arg1, [arg2 ...]], CLASS -> [VALUE] (Stack grows up)
     * <p>
     * Compiler Stack: [[... arg2], arg1], CLASS -> [VALUE] (Stack grows down)
     * <p>
     */
    protected void do_invokestatic(Type t) {
        invokestatic(t, Compiler.C_JVM);
    }

    /**
     * invokesuper.
     *
     * <p>
     * Compiler Stack: OOP, [arg1, [arg2 ...]], CLASS -> [VALUE] (Stack grows up)
     * <p>
     * Compiler Stack: [[... arg2], arg1], OOP, CLASS -> [VALUE] (Stack grows down)
     * <p>
     *
     * @param t the return type
     */
    protected void do_invokesuper(Type t) {
        invokesuper(t, Compiler.C_JVM);
    }

    /**
     * invokevirtual.
     *
     * <p>
     * Compiler Stack: OOP, [arg1, [arg2 ...]] -> [VALUE] (Stack grows up)
     * <p>
     * Compiler Stack: [[... arg2], arg1], OOP -> [VALUE] (Stack grows down)
     * <p>
     */
    protected void do_invokevirtual(Type t) {
        c.dupReceiver();
        invokevirtual(t, Compiler.C_JVM);
    }

    /**
     * findslot.
     *
     * <p>
     * Compiler Stack: OOP, CLASS -> INT
     * <p>
     */
    protected void do_findslot() {
        findslot();
    }

    /**
     * invokeslot.
     *
     * <p>
     * Compiler Stack: OOP, [arg1, [arg2 ...]], SLOT -> [VALUE] (Stack grows up)
     * <p>
     * Compiler Stack: [[... arg2], arg1], OOP, SLOT -> [VALUE] (Stack grows down)
     * <p>
     *
     * @param t the return type
     */
    protected void do_invokeslot(Type t) {
        c.begin();
            Local slot = localStore(INT);
            c.dupReceiver();
            c.load(slot);
            invokeslot(t, Compiler.C_JVM);
        c.end();
    }

    /**
     * Return from a method.
     *
     * <p>
     * Compiler Stack: [VALUE] -> _
     * <p>
     *
     * @param t type of data to return
     */
    protected void do_return(Type t) {
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
       c.drop();
    }

    /**
     * invokenative.
     *
     * <p>
     * Compiler Stack: [arg1, [arg2 ...]] -> [VALUE] (Stack grows up)
     * <p>
     * Compiler Stack: [[... arg2], arg1] -> [VALUE] (Stack grows down)
     * <p>
     */
    protected void do_invokenative(Type t) {
        invokenative(iparm);
/*if[INCLUDE_EXECUTECIO_PARMS]*/
/*else[INCLUDE_EXECUTECIO_PARMS]*/
//      throw new Error("Cannot work without INCLUDE_EXECUTECIO_PARMS");
/*end[INCLUDE_EXECUTECIO_PARMS]*/
    }

    /*-----------------------------------------------------------------------*\
     *                             ALU instructions                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Add two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1+VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_add(Type t) {
        c.add();
    }

    /**
     * Subtract two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1-VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_sub(Type t) {
        c.sub();
    }

    /**
     * And two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1&VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_and(Type t) {
        c.and();
    }

    /**
     * Or two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1|VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_or(Type t) {
        c.or();
    }

    /**
     * Xor two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1^VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_xor(Type t) {
        c.xor();
    }

    /**
     * Signed left shift two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1<<(VALUE2&1f)
     * <p>
     *
     * @param t the data type.
     */
    protected void do_shl(Type t) {
        c.shl();
    }

    /**
     * Right shift two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1>>(VALUE2&1f)
     * <p>
     *
     * @param t the data type.
     */
    protected void do_shr(Type t) {
        c.shr();
    }

    /**
     * Unsigned right shift two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1>>>(VALUE2&1f)
     * <p>
     *
     * @param t the data type.
     */
    protected void do_ushr(Type t) {
        c.ushr();
    }

    /**
     * Multiply two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1*VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_mul(Type t) {
        c.mul();
    }

    /**
     * Divide two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1/VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_div(Type t) {
        div(t);
    }

    /**
     * Rem two things.
     *
     * <p>
     * Compiler Stack: ..., VALUE1, VALUE2 -> ..., VALUE1%VALUE2
     * <p>
     *
     * @param t the data type.
     */
    protected void do_rem(Type t) {
        rem(t);
    }

    /**
     * Negate something.
     *
     * <p>
     * Compiler Stack: ..., VALUE -> ..., -VALUE1
     * <p>
     *
     * @param t the data type.
     */
    protected void do_neg(Type t) {
        c.neg();
    }


    /*-----------------------------------------------------------------------*\
     *                              Convertions                              *
    \*-----------------------------------------------------------------------*/

    /**
     * Convert int to byte.
     *
     * <p>
     * Compiler Stack: ..., INT -> ..., INT
     * <p>
     */
    protected void do_i2b() {
        c.convert(BYTE);
    }

    /**
     * Convert int to short.
     *
     * <p>
     * Compiler Stack: ..., INT -> ..., INT
     * <p>
     */
    protected void do_i2s() {
        c.convert(SHORT);
    }

    /**
     * Convert int to char.
     *
     * <p>
     * Compiler Stack: ..., INT -> ..., INT
     * <p>
     */
    protected void do_i2c() {
        c.convert(USHORT);
    }

    /**
     * Convert long to int.
     *
     * <p>
     * Compiler Stack: ..., LONG -> ..., INT
     * <p>
     */
    protected void do_l2i() {
        c.convert(INT);
    }

    /**
     * Convert int to long.
     *
     * <p>
     * Compiler Stack: ..., INT -> ..., LONG
     * <p>
     */
    protected void do_i2l() {
        c.convert(LONG);
    }

    /**
     * Convert int to float.
     *
     * <p>
     * Compiler Stack: ..., INT -> ..., FLOAT
     * <p>
     */
    protected void do_i2f() {
        c.convert(FLOAT);
    }

    /**
     * Convert long to float.
     *
     * <p>
     * Compiler Stack: ..., LONG -> ..., FLOAT
     * <p>
     */
    protected void do_l2f() {
        c.convert(FLOAT);
    }

    /**
     * Convert float to int.
     *
     * <p>
     * Compiler Stack: ..., FLOAT -> ..., INT
     * <p>
     */
    protected void do_f2i() {
        c.convert(INT);
    }

    /**
     * Convert float to long.
     *
     * <p>
     * Compiler Stack: ..., FLOAT -> ..., LONG
     * <p>
     */
    protected void do_f2l() {
        c.convert(LONG);
    }

    /**
     * Convert int to double.
     *
     * <p>
     * Compiler Stack: ..., INT -> ..., DOUBLE
     * <p>
     */
    protected void do_i2d() {
        c.convert(DOUBLE);
    }

    /**
     * Convert long to double.
     *
     * <p>
     * Compiler Stack: ..., LONG -> ..., DOUBLE
     * <p>
     */
    protected void do_l2d() {
        c.convert(DOUBLE);
    }

    /**
     * Convert float to double.
     *
     * <p>
     * Compiler Stack: ..., FLOAT -> ..., DOUBLE
     * <p>
     */
    protected void do_f2d() {
        c.convert(DOUBLE);
    }

    /**
     * Convert double to int.
     *
     * <p>
     * Compiler Stack: ..., DOUBLE -> ..., INT
     * <p>
     */
    protected void do_d2i() {
        c.convert(INT);
    }

    /**
     * Convert double to long.
     *
     * <p>
     * Compiler Stack: ..., DOUBLE -> ..., LONG
     * <p>
     */
    protected void do_d2l() {
        c.convert(LONG);
    }

    /**
     * Convert double to float.
     *
     * <p>
     * Compiler Stack: ..., DOUBLE -> ..., FLOAT
     * <p>
     */
    protected void do_d2f() {
        c.convert(FLOAT);
    }


}
