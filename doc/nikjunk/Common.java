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

abstract public class Common extends BytecodeRoutines implements Types {

//TEMP
    protected final static int BRANCHQUOTA = 1;
    protected final static int CLASS_ISARRAY = 123;
    protected final static int OFFSET_TO_STATIC_TABLE = 123;
    protected final static int OFFSET_TO_VIRTUAL_TABLE = 123;
    protected final static int OFFSET_TO_OBJECT_TABLE = 123;

    protected final static int java_lang_VMExtension_do_yield                   = 1;
    protected final static int java_lang_VMExtension_do_storeCheck              = 2;
    protected final static int java_lang_VMExtension_do_throw                   = 3;
    protected final static int java_lang_VMExtension_do_monitorEnter            = 4;
    protected final static int java_lang_VMExtension_do_monitorExit             = 5;
    protected final static int java_lang_VMExtension_do_newobject               = 6;
    protected final static int java_lang_VMExtension_do_newarray                = 7;
    protected final static int java_lang_VMExtension_do_newdimension            = 8;
    protected final static int java_lang_VMExtension_do_clinit                  = 9;
    protected final static int java_lang_VMExtension_do_instanceof              = 10;
    protected final static int java_lang_VMExtension_do_checkcast               = 11;
    protected final static int java_lang_VMExtension_do_lookupByte              = 12;
    protected final static int java_lang_VMExtension_do_lookupShort             = 13;
    protected final static int java_lang_VMExtension_do_lookupInt               = 14;
    protected final static int java_lang_VMExtension_do_nullPointerException    = 15;
    protected final static int java_lang_VMExtension_do_arithmeticException     = 16;
    protected final static int java_lang_VMExtension_do_arrayBoundsException    = 17;
    protected final static int java_lang_VMExtension_do_illegalStoreException   = 18;
    protected final static int java_lang_VMExtension_do_findslot                = 19;
    protected final static int java_lang_VMExtension_do_findClassState          = 20;

    protected int heapBase;
    protected int lastClass;
    protected int lastClassState;

    void getVMExtensionStaticVtable() {
         c.literal(0x12345678);
    }


//TEMP


    /*-----------------------------------------------------------------------*\
     *                                  Data                                 *
    \*-----------------------------------------------------------------------*/

    /**
     * The dynamic compiler.
     */
    protected Compiler c;

    /**
     * The pointer to the currently executing method.
     */
    protected Local mp;

    /**
     * The offset from an oop to the standard header word.
     */
    protected final static int OBJ_CLASS_WORD = -4;

    /**
     * The offset from an oop to length word of a non-compact array.
     */
    protected final static int OBJ_LENGTH_WORD = -8;

    /**
     * The size of a standard object header.
     */
    protected final static int OBJ_HEADER_SIZE = 4;

    /**
     * The size of a large array header.
     */
    protected final static int LARGE_ARRAY_HEADER_SIZE = 8;

    /**
     * Get/putfield check codes.
     */
    protected final static int NOCHECK     = 0,
                               NULLCHECK   = 1,
                               BOUNDSCHECK = 2,
                               STORECHECK  = 3;

    /**
     * Condition codes.
     */
    protected final static int EQ = 0,
                               NE = 1,
                               LT = 2,
                               LE = 3,
                               GT = 4,
                               GE = 5;

    /*-----------------------------------------------------------------------*\
     *                             General things                            *
    \*-----------------------------------------------------------------------*/

    /**
     * Make a nicely formatted comment.
     *
     * @param text the text of the comment
     */
    protected void commentBox(String text) {
        while (text.length() < 75) {
            text = " "+text;
            if (text.length() < 75) {
                text = text+" ";
            }
        }
        String msg = "/*---------------------------------------------------------------------------*\\\n"
                   + " *"+text+"*\n"
                   +"\\*---------------------------------------------------------------------------*/";

        c.comment(msg);
    }

    /**
     * Cause a fatal runtime error.
     *
     * @param msg the error message
     */
    protected void fatal(String msg) {
        c.comment("fatal -- "+msg);
        c.literal((msg+"\u0000").getBytes());
        c.symbol("printf");
        c.call(VOID);
        c.literal(-1);
        c.symbol("exit");
        c.call(VOID);
        c.deadCode();
    }

    /**
     * Cause a fatal runtime error.
     */
    protected void shouldNotReachHere() {
        fatal("shouldNotReachHere");
    }

    /**
     * Dynamically check that an oop is an array.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., INT
     * <p>
     */
    protected void isArray() {
        getKlass();
        c.literal(CLASS_ISARRAY);
        c.add();
        c.read(BYTE);
    }

    /**
     * Dynamically assume that an oop is an array.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., OOP
     * <p>
     */
    protected void assumeIsArray() {
        if (Config.assuming) {
            c.begin();
            Label ok = c.label();
            Local oop = c.localStore(OOP);
            c.load(oop);
            isArray();
            c.literal(0);
            c.ne().bt(ok);
            fatal("OOP is not an array");
        c.bind(ok);
            c.load(oop);
            c.end();
        }
    }

    /**
     * Get the class word for an object.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., CLASS
     * <p>
     */
    protected void getClassWord() {
        c.literal(OBJ_CLASS_WORD);
        c.add();
        c.read(INT);
    }

    /**
     * Get the compact length field in the class word of an object.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., INT
     * <p>
     */
    protected void getClassWordCompactLength() {
        getClassWord();
        c.literal(Config.classPointerBitCount);
        c.ushr();
    }

    /**
     * Get the array length for an object.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., LENGTH
     * <p>
     *
     */
    protected void getArrayLengthWord() {
        c.literal(OBJ_LENGTH_WORD);
        c.add();
        c.read(INT);
        c.literal(2);
        c.ushr();
    }

    /**
     * Get the class pointer for an object.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., CLASS
     * <p>
     *
     */
    protected void getKlass() {
        getClassWord();
        if (Config.hasCompactArrays) {
            c.literal(Config.classPointerMask);
            c.and();
        }
        if (Config.classPointerIsIndexIntoHeap) {
            c.literal(heapBase);
            c.add();
        }
    }

    /**
     * Get the length of an array.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., INT
     * <p>
     *
     */
    protected void getArrayLength() {
        assumeIsArray();
        if (Config.hasCompactArrays) {
            c.begin();
                Label ok  = c.label();
                Local oop = c.localStore(OOP);
                c.load(oop);
                getClassWordCompactLength();
                Local lth = c.localStore(INT);
                c.load(lth);
                c.literal(Config.compactArrayMaxSize);
                c.lt().bt(ok);
                c.load(oop);
                getArrayLengthWord();
                c.store(lth);
            c.bind(ok);
                c.load(lth);
            c.end();
        } else {
            getArrayLengthWord();
        }
    }

    /**
     * Get the stable from a class.
     *
     * <p>
     * Compiler Stack: ..., class -> ..., stable
     * <p>
     *
     */
    protected void getStable() {
        c.literal(OFFSET_TO_STATIC_TABLE);
        c.add();
        c.read(OOP);
    }

    /**
     * Get the vtable from a class.
     *
     * <p>
     * Compiler Stack: ..., class -> ..., vtable
     * <p>
     *
     */
    protected void getVtable() {
        c.literal(OFFSET_TO_VIRTUAL_TABLE);
        c.add();
        c.read(OOP);
    }

    /**
     * Back up a pointer to before the standard header of an array.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., REF
     * <p>
     *
     */
    protected void pointBeforeStandardArrayHeader() {
        assumeIsArray();
        if (Config.hasCompactArrays) {
            c.begin();
            Label ok   = c.label();
            Label done = c.label();
            Local oop  = c.localStore(OOP);
            c.load(oop);
            getClassWordCompactLength();
            c.literal(Config.compactArrayMaxSize);
            c.lt().bt(ok);
            c.literal(OBJ_HEADER_SIZE);
            c.br(done);
        c.bind(ok);
            c.literal(LARGE_ARRAY_HEADER_SIZE);
        c.bind(done);
            c.load(oop);
            c.sub();
            c.end();
        } else {
            c.literal(LARGE_ARRAY_HEADER_SIZE);
            c.sub();
        }
    }

    /**
     * Back up a method pointer to before the pointer to the owning class.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., REF
     * <p>
     *
     */
    protected void pointBeforeOwningClassPointer() {
        pointBeforeStandardArrayHeader();
        c.literal(4);
        c.sub();
    }

    /**
     * Push the pointer to the owning class of the current method on the stack.
     *
     * <p>
     * Compiler Stack: ... -> ..., cp
     * <p>
     *
     */
    protected void getCP() {
        c.load(mp);
        pointBeforeOwningClassPointer();
        c.read(OOP);
    }

    /**
     * Get a constant object.
     *
     * <p>
     * Compiler Stack: ..., INT -> ..., OOP
     * <p>
     */
    protected void getObject() {
        getCP();
        c.literal(OFFSET_TO_OBJECT_TABLE);
        c.add();
        c.read(OOP);
        c.add();
        c.read(OOP);
    }

    /**
     * Push value.
     */
    abstract protected void getValue();


    /*-----------------------------------------------------------------------*\
     *                        Exception generating code                      *
    \*-----------------------------------------------------------------------*/

    /**
     * Function that is called when an exception is to be thrown and the
     * interpreter's stack should have all its elements removed.
     *
     * <p>
     * Java Stack:  ... -> _
     * <p>
     *
     */
    protected void clearInterpreterStack() {
    }

    /**
     * Call a static method in VMExtension.
     *
     * <p>
     * Compiler Stack:  PARMS -> _
     * <p>
     *
     * @param slot the static slot in VMExtension to call.
     * @param parms the number of parameters on the compiler stack.
     */
    protected void callVMExtension(int slot, Type type) {
        c.comment("callVMExtension() slot = "+slot);
        getVMExtensionStaticVtable();
        c.literal(slot*4);
        c.add();
        c.read(OOP);
        c.call(type, Compiler.C_JVM);
    }

    /**
     * Call a static method in VMExtension to throw an exception.
     *
     * <p>
     * Compiler Stack:  PARMS -> _
     * <p>
     *
     * @param slot the static slot in VMExtension to call.
     * @param parms the number of parameters on the compiler stack.
     */
    protected void callVMExtensionToThrow(int slot, Type type) {
        callVMExtension(slot, type);
    }

    /**
     * Call VMExtension.nullPointerException() if the top element of the compiler
     * stack is true.
     *
     * <p>
     * Compiler Stack:  BOOLEAN -> _
     * <p>
     *
     * @param slot the static slot in VMExtension to call.
     */
    protected void callVMExtensionToThrowIfTrue(int slot) {
        c.comment("callVMExtensionToThrowIfTrue() slot = "+slot);

        Label ok = c.label();
        c.bf(ok);

        c.dropAll();
        clearInterpreterStack();
        callVMExtensionToThrow(slot, VOID);
        c.deadCode();

    c.bind(ok);
    }

    /**
     * Call VMExtension.nullPointerException() if the top element of the compiler
     * stack is true.
     *
     * <p>
     * Compiler Stack:  BOOLEAN -> _
     * <p>
     */
    protected void callNullPointerExceptionIfTrue() {
        callVMExtensionToThrowIfTrue(java_lang_VMExtension_do_nullPointerException);
    }

    /**
     * Call VMExtension.nullPointerException() if the top element of the compiler
     * stack is true.
     *
     * <p>
     * Compiler Stack:  BOOLEAN -> _
     * <p>
     */
    protected void callArithmeticExceptionIfTrue() {
        callVMExtensionToThrowIfTrue(java_lang_VMExtension_do_arithmeticException);
    }

    /**
     * Call VMExtension.arrayBoundsException() if the top element of the compiler
     * stack is true.
     *
     * <p>
     * Compiler Stack:  BOOLEAN -> _
     * <p>
     *
     */
    protected void callArrayBoundsExceptionIfTrue() {
        callVMExtensionToThrowIfTrue(java_lang_VMExtension_do_arrayBoundsException);
    }

    /**
     * Call VMExtension.illegalStoreException() if the top element of the compiler
     * stack is true.
     *
     * <p>
     * Compiler Stack:  BOOLEAN -> _
     * <p>
     *
     */
    protected void callIllegalStoreExceptionIfTrue() {
        callVMExtensionToThrowIfTrue(java_lang_VMExtension_do_illegalStoreException);
    }

    /*-----------------------------------------------------------------------*\
     *                           Other utility code                          *
    \*-----------------------------------------------------------------------*/



    /**
     * Push a zero of the specified type onto the compiler's stack.
     *
     * <p>
     * Compiler Stack:  ... -> ..., VALUE
     * <p>
     *
     * @return the compiler
     */
    protected Compiler zero(Type type) {
        switch (type.getTypeCode()) {
            case Type.Code_O:
            case Type.Code_I: c.literal(0);               break;
            case Type.Code_L: c.literal((long)0);         break;
            case Type.Code_F: c.literal((float)0);        break;
            case Type.Code_D: c.literal((double)0);       break;
            default: Assert.shouldNotReachHere();
        }
        return c;
    }


    /*-----------------------------------------------------------------------*\
     *                           Thread preemption                           *
    \*-----------------------------------------------------------------------*/

    /**
     * On systems where a machine register should be used as a rescheduling
     * counter, this variable should be set up.
     */
    Local branchCountLocal;

    /**
     * On systems where a global variable should be used as a rescheduling
     * counter, this variable should be set up.
     */
    int branchCountGlobal = 0xDEADBEEF;

    /**
     * On systems where decrementing of the counter should be done by the
     * interpreter/jitted code then this should be set true.
     */
    boolean threadDecrementsCounter;

    /**
     * Test for thread preemption.
     *
     * @param delta the amount to decrement the counter by
     */
    protected void reschedule(int delta) {
        Label ok = c.label();
        if (branchCountLocal != null) {
            c.load(branchCountLocal);
            if (threadDecrementsCounter) {
                c.literal(delta);
                c.sub();
                c.dup();
                c.store(branchCountLocal);
            }
        } else {
            Assert.that(branchCountGlobal != 0);
            c.literal(branchCountGlobal);
            c.read(INT);
            if (threadDecrementsCounter) {
                c.literal(delta);
                c.sub();
                c.dup();
                c.literal(branchCountGlobal);
                c.write(INT);
            }
        }
        c.literal(0).gt().bt(ok);
        c.literal(BRANCHQUOTA);
        if (branchCountLocal != null) {
            c.store(branchCountLocal);
        } else {
            c.literal(branchCountGlobal);
            c.write(INT);
        }
        callVMExtension(java_lang_VMExtension_do_yield, VOID);
    c.bind(ok);
    }


    /*-----------------------------------------------------------------------*\
     *                    The main code generation function                  *
    \*-----------------------------------------------------------------------*/

    /**
     * Do an idiv or irem operation.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1/VALUE2 (idiv)
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1%VALUE2 (irem)
     * <p>
     *
     * @param idiv set true for an idiv operation
     */
    private void idivrem(boolean idiv) {
        Label divide = c.label();
        Label done   = c.label();
        Local r = c.localStore(INT);
        Local l = c.localStore(INT);

        /*
         * If the divisor is zero then throw an arithmetic exception
         */
        c.load(r).literal(0).eq();
        callArithmeticExceptionIfTrue();

        /*
         * If l == 0x80000000 and r == -1 then the result is 0x80000000
         */
        c.load(l).literal(0x80000000).ne().bt(divide);
        c.load(r).literal(-1).ne().bt(divide);
        c.literal(idiv ? 0x80000000/-1 : 0x80000000%-1);
        c.br(done);

        /*
         * Otherwise simply do the division
         */
    c.bind(divide);
        c.load(l).load(r);
        if (idiv) {
            c.div();
        } else {
            c.rem();
        }
    c.bind(done);
    }

    /**
     * Do an div operation.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1/VALUE2
     * <p>
     *
     * @param t data type of operation
     */
    protected void div(Type t) {
        if (t == INT) {
            idivrem(true);
        } else if (t == LONG) {
            c.dup().literal((long)0).eq();
            callArithmeticExceptionIfTrue();
            c.div();
        } else {
            c.div();
        }
    }

    /**
     * Do an rem operation.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1%VALUE2
     * <p>
     *
     * @param t data type of operation
     */
    protected void rem(Type t) {
        if (t == INT) {
            idivrem(false);
        } else if (t == LONG) {
            c.dup().literal((long)0).eq();
            callArithmeticExceptionIfTrue();
            c.rem();
        } else {
            c.rem();
        }
    }

    /**
     * Get a class state record.
     *
     * <p>
     * Compiler Stack: CLASS -> CLASSSTATE
     * <p>
     *
     * @param cls the class of the required class state
     */
    private void getClassState(Local cls) {
        Label ok = c.label();

        /*
         * Test to see if the cached class state is the needed one.
         */
        c.literal(lastClass);
        c.read(OOP);
        c.load(cls);
        c.eq().bt(ok);

        /*
         * Call function to get the required class state.
         */
        c.load(cls);
        callVMExtension(java_lang_VMExtension_do_findClassState, OOP);
        c.literal(lastClassState);
        c.write(OOP);

    c.bind(ok);
        c.literal(lastClassState);
        c.read(OOP);
    }

    /**
     * Read a static field.
     *
     * <p>
     * Compiler Stack: INDEX, CLASS -> VALUE
     * <p>
     *
     * @param type the field type
     */
    protected void getStatic(Type type) {
        Local cls   = c.localStore(OOP);
        Local index = c.localStore(INT);
        getClassState(cls);
        c.load(index).literal(4).mul();
        c.add();
        c.read(type);
    }

    /**
     * Write a static field.
     *
     * <p>
     * Compiler Stack: VALUE, INDEX, CLASS -> _
     * <p>
     *
     * @param type the field type
     */
    protected void putStatic(Type type) {
        Local cls   = c.localStore(OOP);
        Local index = c.localStore(INT);
        Local value = c.localStore(type.getPrimitiveType());
        getClassState(cls);
        c.load(index).literal(4).mul();
        c.add();
        c.load(value);
        c.swap();
        c.write(type);
    }

    /**
     * Read a field from an object or an array
     *
     * <p>
     * Compiler Stack: ..., INDEX, REFERENCE -> ..., VALUE
     * <p>
     *
     * @param type the field type
     * @param check the type of checking needed
     */
    protected void getField(Type type, int check) {
        Local oop   = c.localStore(OOP);
        Local index = c.localStore(INT);

        /*
         * Do any checking that is needed.
         */
        switch(check) {
            case NULLCHECK:   nullCheck(oop);          break;
            case BOUNDSCHECK: boundsCheck(oop, index); break;
        }

        /*
         * Read the value at ref + (index * sizeof(type))
         */
        c.load(index).literal(type.getStructureSize()).mul();
        c.load(oop).add().read(type);
    }

    /**
     * Write a field to an object or an array
     *
     * <p>
     * Compiler Stack: ..., VALUE, INDEX, REFERENCE -> ...
     * <p>
     *
     * @param type the field type
     * @param check the type of checking needed
     */
    protected void putField(Type type, int check) {
        Local oop   = c.localStore(OOP);
        Local index = c.localStore(INT);
        Local val   = c.localStore(type.getPrimitiveType());

        /*
         * Do any checking that is needed.
         */
        switch(check) {
            case NULLCHECK:   nullCheck(oop);              break;
            case BOUNDSCHECK: boundsCheck(oop, index);     break;
            case STORECHECK:  storeCheck(oop, index, val); break;
        }

        /*
         * Write the value at oop + (index * sizeof(type)).
         */
        c.load(val);
        c.load(index).literal(type.getStructureSize()).mul();
        c.load(oop).add();

        /*
         * If the field type is OOP then the write barrier needs to be set.
         */
        if (type == OOP) {
            c.dup();
            writeBarrier();
        }

        /*
         * Write the value.
         */
        c.write(type);
    }

    /**
     * Insert write barrier code
     *
     * <p>
     * Compiler Stack: ..., OOP -> ...
     * <p>
     */
    private void writeBarrier() {
        c.drop(); // TEMP
    }

    /**
     * Insert a null pointer check
     *
     * @param oop the object to be checked
     */
    private void nullCheck(Local oop) {
        c.load(oop);
        nullCheck();
    }

    /**
     * Insert a null pointer check
     *
     * <p>
     * Compiler Stack: ..., OOP -> ...
     * <p>
     */
    private void nullCheck() {
        c.comment("nullCheck()");
        c.literal(0).eq();
        callNullPointerExceptionIfTrue();
    }

    /**
     * Insert a bounds check
     *
     * <p>
     * Compiler Stack: _ -> _
     * <p>
     *
     * @param oop the array to be checked
     * @param index the offset into the arrat to check
     */
    private void boundsCheck(Local oop, Local index) {

        /*
         * Start off with a null check.
         */
        nullCheck(oop);

        /*
         * Get the element count.
         */
        c.comment("boundsCheck()");
        c.load(oop);
        getArrayLength();
        c.force(UINT);

        /*
         * Compare with index.
         */
        c.load(index);
        c.force(UINT);
        c.ge();

        /*
         * Call VMExtension.arrayBoundsException() if the index exceeds the count.
         */
        callArrayBoundsExceptionIfTrue();
    }

    /**
     * Insert a store check
     *
     * @param ref   the array to be checked
     * @param index the index to be checked
     * @param val   the value to be stored
     */
    private void storeCheck(Local ref, Local index, Local val) {
        boundsCheck(ref, index);
        c.comment("storeCheck()");
        c.load(val);
        c.load(ref);
        callVMExtension(java_lang_VMExtension_do_storeCheck, VOID);
    }



    /*-----------------------------------------------------------------------*\
     *                           Invoke instructions                         *
    \*-----------------------------------------------------------------------*/

    /**
     * invokestatic.
     *
     * <p>
     * <p>
     * When called from Jitter:
     * <p>
     * Compiler Stack: [arg1, [arg2 ...]], CLASS -> [VALUE] (Stack grows up)
     * <p>
     * Compiler Stack: [[... arg2], arg1], CLASS -> [VALUE] (Stack grows down)
     * <p>
     * <p>
     * When called from interpreter generator:
     * <p>
     * Compiler Stack: CLASS -> [VALUE]
     * <p>
     *
     * @param t the return type
     * @param conv the calling convention
     */
    protected void invokestatic(Type t, int conv) {
        getStable();
        getValue();
        c.add();
        c.read(OOP);
        c.call(t, conv);
    }

    /**
     * invokesuper.
     *
     * <p>
     * <p>
     * When called from Jitter:
     * <p>
     * Compiler Stack: OOP, [arg1, [arg2 ...]], CLASS -> [VALUE] (Stack grows up)
     * <p>
     * Compiler Stack: [[... arg2], arg1], OOP, CLASS -> [VALUE] (Stack grows down)
     * <p>
     * <p>
     * When called from interpreter generator:
     * <p>
     * Compiler Stack: CLASS -> [VALUE]
     * <p>
     *
     * @param t the return type
     * @param conv the calling convention
     */
    protected void invokesuper(Type t, int conv) {
        getVtable();
        getValue();
        c.add();
        c.read(OOP);
        c.call(t, conv);
    }

    /**
     * invokevirtual.
     *
     * <p>
     * <p>
     * When called from Jitter:
     * <p>
     * Compiler Stack: OOP, [arg1, [arg2 ...]], OOP -> [VALUE] (Stack grows up)
     * <p>
     * Compiler Stack: [[... arg2], arg1], OOP, OOP -> [VALUE] (Stack grows down)
     * <p>
     * <p>
     * When called from interpreter generator:
     * <p>
     * Compiler Stack: OOP -> [VALUE]
     * <p>
     *
     * @param t the return type
     * @param conv the calling convention
     */
    protected void invokevirtual(Type t, int conv) {
        c.dup();
        nullCheck();
        getKlass();
        getVtable();
        getValue();
        c.add();
        c.read(OOP);
        c.call(t, conv);
    }

    /**
     * findslot.
     *
     * <p>
     * Compiler Stack: OOP, CLASS, SLOT -> INT
     * <p>
     */
    protected void findslot() {
        callVMExtension(java_lang_VMExtension_do_findslot, INT);
    }

    /**
     * invokeslot.
     *
     * <p>
     * <p>
     * When called from Jitter:
     * <p>
     * Compiler Stack: OOP, [arg1, [arg2 ...]], OOP, SLOT -> [VALUE] (Stack grows up)
     * <p>
     * Compiler Stack: [[... arg2], arg1], OOP, OOP, SLOT -> [VALUE] (Stack grows down)
     * <p>
     * <p>
     * When called from interpreter generator:
     * <p>
     * Compiler Stack: OOP, SLOT -> [VALUE]
     * <p>
     *
     * @param t the return type
     * @param conv the calling convention
     */
    protected void invokeslot(Type t, int conv) {
        c.begin();
            Local slot = c.localStore(INT);
            Local oop  = c.localStore(OOP);
            nullCheck(oop);
            c.load(oop);
            getKlass();
            getVtable();
            c.load(slot);
            c.add();
            c.read(OOP);
            c.call(t, conv);
        c.end();
    }


    /*-----------------------------------------------------------------------*\
     *        Complex instrcutions implemented with external function        *
    \*-----------------------------------------------------------------------*/

    /**
     * Extend the activation record.
     *
     * <p>
     * Compiler Stack: _ -> _
     * <p>
     */
    protected void do_extend0() {
        // Ignore -- This is only used in the C interpreter.
    }

    /**
     * Extend the activation record and clear certain slots.
     *
     * <p>
     * Compiler Stack: _ -> _
     * <p>
     */
    protected void do_extend() {
        // Ignore -- This is only used in the C interpreter.
    }

    /**
     * Throw an exception.
     *
     * <p>
     * Compiler Stack: OOP -> _
     * <p>
     */
    protected void do_throw() {
        callVMExtension(java_lang_VMExtension_do_throw, VOID);
    }

    /**
     * Execute a monitor enter.
     *
     * <p>
     * Compiler Stack: OOP -> _
     * <p>
     */
    protected void do_monitorenter() {
        callVMExtension(java_lang_VMExtension_do_monitorEnter, VOID);
    }

    /**
     * Execute a monitor exit.
     *
     * <p>
     * Compiler Stack: OOP -> _
     * <p>
     */
    protected void do_monitorexit() {
        callVMExtension(java_lang_VMExtension_do_monitorExit, VOID);
    }

    /**
     * Execute a class monitor enter.
     *
     * <p>
     * Compiler Stack: _ -> _
     * <p>
     */
    protected void do_class_monitorenter() {
        getCP();
        do_monitorenter();
    }

    /**
     * Execute a class monitor exit.
     *
     * <p>
     * Compiler Stack: _ -> _
     * <p>
     */
    protected void do_class_monitorexit() {
        getCP();
        do_monitorexit();
    }

    /**
     * Get the length of an array.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., INT
     * <p>
     */
    protected void do_arraylength() {
        Local oop = c.localStore(OOP);
        nullCheck(oop);
        c.load(oop);
        getArrayLength();
    }

    /**
     * Allocate an object.
     *
     * <p>
     * Compiler Stack: ..., CLASS -> ..., OOP
     * <p>
     */
    protected void do_new() {
        callVMExtension(java_lang_VMExtension_do_newobject, OOP);
    }

    /**
     * Allocate a new array.
     *
     * <p>
     * Compiler Stack: SIZE, CLASS -> ..., OOP
     * <p>
     */
    protected void do_newarray() {
        callVMExtension(java_lang_VMExtension_do_newarray, OOP);
    }

    /**
     * Allocate a new array dimension.
     *
     * <p>
     * Compiler Stack: OOP, SIZE -> ..., OOP
     * <p>
     */
    protected void do_newdimension() {
        callVMExtension(java_lang_VMExtension_do_newdimension, OOP);
    }

    /**
     * Initialize the current class.
     *
     * <p>
     * Compiler Stack: _ -> _
     * <p>
     */
    protected void do_class_clinit() {
        getCP();
        callVMExtension(java_lang_VMExtension_do_clinit, VOID);
    }

    /**
     * Instanceof.
     *
     * <p>
     * Compiler Stack: ..., OOP, CLASS -> ..., INT
     * <p>
     */
    protected void do_instanceof() {
        callVMExtension(java_lang_VMExtension_do_instanceof, INT);
    }

    /**
     * Checkcast.
     *
     * <p>
     * Compiler Stack: ..., OOP, CLASS -> ..., OOP
     * <p>
     */
    protected void do_checkcast() {
        callVMExtension(java_lang_VMExtension_do_checkcast, OOP);
    }

    /**
     * Lookup.
     *
     * <p>
     * Compiler Stack: OOP, CLASS -> VALUE
     * <p>
     *
     * @param t the type of array to lookup
     */
    protected void do_lookup(Type t) {
        int slot = -1;
             if (t == BYTE)  slot = java_lang_VMExtension_do_lookupByte;
        else if (t == SHORT) slot = java_lang_VMExtension_do_lookupShort;
        else if (t == INT)   slot = java_lang_VMExtension_do_lookupInt;
        else Assert.shouldNotReachHere();
        callVMExtension(slot,  INT);
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


}
