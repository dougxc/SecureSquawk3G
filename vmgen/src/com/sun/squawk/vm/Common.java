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

    protected final static int BRANCHQUOTA = 1;


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
     *                              Constructors                             *
    \*-----------------------------------------------------------------------*/

    /**
     * Constructor.
     */
    protected Common() {
        c = Compilation.newCompiler();
    }

    /**
     * Constructor.
     *
     * @param arch the architecture name
     */
    protected Common(String arch) {
        c = Compilation.newCompiler(arch);
    }


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
        String msg = "\n/*---------------------------------------------------------------------------*\\\n"
                   +   " *"+text+"*\n"
                   +  "\\*---------------------------------------------------------------------------*/";

        c.comment(msg);
    }

    /**
     * Cause a fatal runtime error.
     *
     * @param msg the error message
     */
    protected void emitFatal(String msg) {
        c.dumpAll();
        clearInterpreterStack();
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
        emitFatal("shouldNotReachHere");
    }

    /**
     * Get the size in bytes of a WORD
     *
     * @return the size
     */
    protected int wordSize() {
        return WORD.getStructureSize();
    }

    /**
     * Emit a multiplication of the top most element on the compiler stack by the size in bytes of a WORD
     * <p>
     * Compiler Stack: ..., INT -> ..., INT
     * <p>
     */
    protected void timesWordSize() {
        c.literal(wordSize()).mul();
    }

    /**
     * Get the type of a field.
     *
     * @param field the field
     * @return the type
     */
    private Type getFieldType(long field) {
        switch (FieldOffsets.getClassID(field)) {
            case CID.OBJECT:  return OOP;
            case CID.BYTE:    return BYTE;
            case CID.CHAR:    return USHORT;
            case CID.SHORT:   return SHORT;
            case CID.INT:     return INT;
            case CID.LONG:    return LONG;
            case CID.FLOAT:   return FLOAT;
            case CID.DOUBLE:  return DOUBLE;
            default:          throw Assert.shouldNotReachHere();
        }
    }

    /**
     * Read a field defined in FieldOffsets.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., TYPE
     * <p>
     *
     * @param field the field
     */
    private void readField(long field) {
        int offset = FieldOffsets.getOffset(field);
        Type type = getFieldType(field);
        c.literal(offset * wordSize());
        c.add();
        c.read(type);
    }

    /**
     * Get the type of a global.
     *
     * @param field the field
     * @return the type
     */
    private Type getGlobalType(long field) {
        if (Global.isGlobalInt(field))  return INT;
        if (Global.isGlobalOop(field))  return OOP;
        if (Global.isGlobalAddr(field)) return REF;
        throw Assert.shouldNotReachHere();
    }

    /**
     * Push the symbol for the relevant global table.
     *
     * @param type the type
     */
    private void getGlobalTable(Type type) {
        if (type == INT) {
            c.symbol("Ints");
        } else if (type == OOP) {
            c.symbol("Oops");
        } else if (type == REF) {
            c.symbol("Addrs");
        } else {
            throw Assert.shouldNotReachHere();
        }
    }

    /**
     * Read a global.
     *
     * <p>
     * Compiler Stack: ... -> ..., TYPE
     * <p>
     *
     * @param field the field
     */
    private void readGlobal(long field) {
        Type type  = getGlobalType(field);
        int offset = Global.getOffset(field);
        readGlobal(type, offset);
    }

    /**
     * Read a global.
     *
     * <p>
     * Compiler Stack: ... -> ..., TYPE
     * <p>
     *
     * @param type the field type
     * @param offset the offset in words
     */
    private void readGlobal(Type type, int offset) {
        getGlobalTable(type);
        c.literal(offset * type.getStructureSize());
        c.add().read(type);
    }

    /**
     * Read a global.
     *
     * <p>
     * Compiler Stack: ... -> ..., TYPE
     * <p>
     *
     * @param type the field type
     * @param offset the offset in words
     */
    private void readGlobal(Type type, Local offset) {
        getGlobalTable(type);
        c.load(offset);
        timesWordSize();
        c.add().read(type);
    }

    /**
     * Write a global
     *
     * <p>
     * Compiler Stack: ... VALUE -> ...
     * <p>
     *
     * @param field the field
     */
    private void writeGlobal(long field) {
        Type type  = getGlobalType(field);
        int offset = Global.getOffset(field);
        writeGlobal(type, offset);
    }

    /**
     * Write a global
     *
     * <p>
     * Compiler Stack: ... VALUE -> ...
     * <p>
     *
     * @param type the field type
     * @param offset the offset in words
     */
    private void writeGlobal(Type type, int offset) {
        getGlobalTable(type);
        c.literal(offset * type.getStructureSize());
        c.add().write(type);
    }

    /**
     * Write a global
     *
     * <p>
     * Compiler Stack: ... VALUE -> ...
     * <p>
     *
     * @param type the field type
     * @param offset the offset in words
     */
    private void writeGlobal(Type type, Local offset) {
        getGlobalTable(type);
        c.load(offset);
        timesWordSize();
        c.add().write(type);
    }

    /**
     * Switch from the current thread to the other thread.
     *
     * @param code the service operation code
     */
    private void threadSwitch(int code) {

        c.comment("  --- threadSwitch ---");
        c.begin();

        /*
         * Write the service code.
         */
        c.literal(code);
        writeGlobal(Global.java_lang_ServiceOperation$code);

        /*
         * Read the current and other threads.
         */
        readGlobal(Global.java_lang_Thread$currentThread);
        Local currentThread = localStore(OOP);
        readGlobal(Global.java_lang_Thread$otherThread);
        Local otherThread = localStore(OOP);

        /*
         * Swap them around.
         */
        c.load(currentThread);
        writeGlobal(Global.java_lang_Thread$otherThread);
        c.load(otherThread);
        writeGlobal(Global.java_lang_Thread$currentThread);

        /*
         * If the other thread is not the service thread then change the current isolate.
         */
        Label isService = c.label();
        readGlobal(Global.java_lang_Thread$serviceThread);
        c.load(otherThread);
        c.eq().bt(isService);
        c.load(otherThread);
        readField(FieldOffsets.java_lang_Thread$isolate);
        writeGlobal(Global.java_lang_VM$currentIsolate);
        c.bind(isService);

        /*
         * Call the switchStack routine.
         */
        c.load(currentThread);
        readField(FieldOffsets.java_lang_Thread$stack);
        c.load(otherThread);
        readField(FieldOffsets.java_lang_Thread$stack);
        c.symbol("switchStack");
        c.call(VOID);

        c.end();

    }

    /**
     * Switch to the service thread to throw an exception.
     *
     * @param code the service operation code
     */
    void threadSwitchFor(int code) {
        c.comment("  --- threadSwitchFor "+code+" ---");
        readGlobal(Global.java_lang_Thread$serviceThread);
        writeGlobal(Global.java_lang_Thread$otherThread);
        threadSwitch(code);
    }


    /**
     * Get the modifiers field of a class.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., INT
     * <p>
     */
    private void getModifiers() {
        readField(FieldOffsets.java_lang_Klass$modifiers);
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
        getModifiers();
        c.literal(Modifier.ARRAY);
        c.and();
        c.literal(0);
        c.ne();
    }

    /**
     * Dynamically assume that an oop is an array.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., OOP
     * <p>
     */
    protected void assumeIsArray() {
        if (Assert.ASSERTS_ENABLED) {
            c.begin();
            Label ok = c.label();
            Local oop = localStore(OOP);
            c.load(oop);
            isArray();
            c.bt(ok);
            emitFatal("OOP is not an array");
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
    protected void getClassOrAssociationPointer() {
        c.literal(HDR.klass * HDR.BYTES_PER_WORD);
        c.add();
        c.read(OOP);
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
        c.literal(HDR.length * HDR.BYTES_PER_WORD);
        c.add();
        c.read(INT);
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
        getClassOrAssociationPointer();
        readField(FieldOffsets.java_lang_Klass$self);
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
        getArrayLengthWord();
        c.literal(HDR.headerTagBits);
        c.ushr();
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
        readField(FieldOffsets.java_lang_Klass$staticMethods);
    }

    /**
     * Get the vtable from a class or an association.
     *
     * <p>
     * Compiler Stack: ..., class/assn -> ..., vtable
     * <p>
     *
     */
    protected void getVtable() {
        readField(FieldOffsets.java_lang_Klass$virtualMethods);
    }

    /**
     * Get the vtable pointer from a class or object association.
     *
     * <p>
     * Compiler Stack: ..., OOP -> ..., VTABLE
     * <p>
     *
     */
    protected void getVtableFromOop() {
        getClassOrAssociationPointer();
        getVtable();
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
        assumeIsArray();
        c.literal(HDR.arrayHeaderSize+HDR.BYTES_PER_WORD);
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
        c.literal(WORD.getStructureSize()).mul();
        getCP();
        readField(FieldOffsets.java_lang_Klass$objects);
        c.add();
        c.read(OOP);
    }

    /**
     * Push the iparm value.
     */
    abstract protected void getValue();

    /**
     * Push the iparm value * 4 or 8.
     */
    protected void getValueAsWordOffset() {
        getValue();
        c.literal(WORD.getStructureSize());
        c.mul();
    }


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
     * @param stableOffset the pointer offset into the static method in java.lang.VM method.
     * @param type the return type of the called method
     */
    protected void callVMExtension(int stableOffset, Type type) {
        c.comment("+callVMExtension() for VM stable offset "+stableOffset);
        c.symbol("VM$"+stableOffset);
        c.call(type, Compiler.C_JVM);
        c.comment("-callVMExtension() for VM stable offset "+stableOffset);
    }

    /**
     * Call VMExtension.nullPointerException() if the top element of the compiler
     * stack is true.
     *
     * <p>
     * Compiler Stack:  BOOLEAN -> _
     * <p>
     *
     * @param stableOffset the pointer offset into the static method in java.lang.VM method.
     */
    protected void callVMExtensionToThrowIfTrue(int stableOffset) {
        c.comment("+callVMExtensionToThrowIfTrue() for VM stable offset "+stableOffset);
        Label ok = c.label();
        c.bf(ok);
        c.dumpAll();
        clearInterpreterStack();
        callVMExtension(stableOffset, VOID);
        c.deadCode();
    c.bind(ok);
        c.comment("-callVMExtensionToThrowIfTrue() for VM stable offset "+stableOffset);
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
        callVMExtensionToThrowIfTrue(MethodOffsets.java_lang_VM$do_nullPointerException);
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
        callVMExtensionToThrowIfTrue(MethodOffsets.java_lang_VM$do_arithmeticException);
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
        callVMExtensionToThrowIfTrue(MethodOffsets.java_lang_VM$do_arrayIndexOutOfBoundsException);
    }

    /*-----------------------------------------------------------------------*\
     *                           Other utility code                          *
    \*-----------------------------------------------------------------------*/

    /**
     * Test the word length
     * @return true if it is 32 bits
     */
    private boolean is32bit() {
        return WORD.getStructureSize() == INT.getStructureSize();
    }

    /**
     * Push an 32 or 64 bit integer
     *
     * <p>
     * Compiler Stack:  ... -> ..., VALUE
     * <p>
     *
     * @return the compiler
     */
    private Compiler literalWord(int x) {
        if (is32bit()) {
            c.literal(x);
        } else {
            c.literal((long)x);
        }
        return c;
    }



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
            case Type.Code_O: literalWord(0); c.force(OOP); break;
            case Type.Code_R: literalWord(0); c.force(REF); break;
            case Type.Code_I: c.literal(0);                 break;
            case Type.Code_L: c.literal((long)0);           break;
            case Type.Code_F: c.literal((float)0);          break;
            case Type.Code_D: c.literal((double)0);         break;
            default: Assert.shouldNotReachHere();
        }
        return c;
    }

    /**
     * Define a local variable type and then store a pushed value into it.
     *
     * <p>
     * Stack: ..., VALUE -> ...
     * <p>
     *
     * @param type the type of the local variable (Must be primary)
     * @return the compiler object
     */
    public Local localStore(Type type) {
        Local res = c.local(type);
        c.store(res);
        return res;
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
        c.begin();
        Label ok = c.label();
        if (branchCountLocal != null) {
            c.load(branchCountLocal);
            if (threadDecrementsCounter) {
                c.literal(delta);
                c.sub();
                c.store(branchCountLocal);
                c.load(branchCountLocal);
            }
        } else {
            Assert.that(branchCountGlobal != 0);
            c.symbol("bc");
            c.read(INT);
            if (threadDecrementsCounter) {
                c.literal(delta);
                c.sub();
                c.symbol("bc");
                c.write(INT);
                c.symbol("bc");
                c.read(INT);
            }
        }
        c.literal(0).gt().bt(ok);

        /* Reset the counter and call the yield finction */
        if (branchCountLocal != null) {
            c.literal(BRANCHQUOTA);
            c.store(branchCountLocal);
        } else {
            c.literal(BRANCHQUOTA);
            c.symbol("bc");
            c.write(INT);
        }
        callVMExtension(MethodOffsets.java_lang_VM$do_yield, VOID);

    c.bind(ok);
        c.end();
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
        c.begin();
        Label divide = c.label();
        Label done   = c.label();
        Local r = localStore(INT);
        Local l = localStore(INT);

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
        c.store(r); // Use r to carry result over branch
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
        c.store(r); // Use r to carry result over branch
    c.bind(done);
        c.load(r);
        c.end();
    }

    /**
     * Do an ldiv or lrem operation.
     *
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1/VALUE2 (ldiv)
     * <p>
     * Java Stack: ..., VALUE1, VALUE2 -> ..., VALUE1%VALUE2 (lrem)
     * <p>
     *
     * @param idiv set true for an idiv operation
     */
    private void ldivrem(boolean idiv) {
        c.begin();
        Local r = localStore(LONG);
        Local l = localStore(LONG);

        /*
         * If the divisor is zero then throw an arithmetic exception
         */
        c.load(r).literal((long)0).eq();
        callArithmeticExceptionIfTrue();

        /*
         * Perform the divide
         */
        c.load(l).load(r);
        if (idiv) {
            c.div();
        } else {
            c.rem();
        }
        c.end();
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
            ldivrem(true);
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
            ldivrem(false);
        } else {
            c.rem();
        }
    }

    /**
     * Read a static field.
     *
     * <p>
     * Compiler Stack: CLASS -> VALUE
     * <p>
     *
     * @param type the field type
     */
    protected void getStatic(Type type) {
        getValue();
        if (type == OOP) {
            callVMExtension(MethodOffsets.java_lang_VM$do_getStaticOop, OOP);
        } else if(type == INT || type == FLOAT) {
            callVMExtension(MethodOffsets.java_lang_VM$do_getStaticInt, INT);
        } else if(type == LONG) {
            callVMExtension(MethodOffsets.java_lang_VM$do_getStaticLong, LONG);
        } else {
            shouldNotReachHere();
        }
    }

    /**
     * Write a static field.
     *
     * <p>
     * Compiler Stack: VALUE, CLASS -> _
     * <p>
     *
     * @param type the field type
     */
    protected void putStatic(Type type) {
        getValue();
        if (type == OOP) {
            callVMExtension(MethodOffsets.java_lang_VM$do_putStaticOop, VOID);
        } else if(type == INT || type == FLOAT) {
            callVMExtension(MethodOffsets.java_lang_VM$do_putStaticInt, VOID);
        } else if(type == LONG) {
            callVMExtension(MethodOffsets.java_lang_VM$do_putStaticLong, VOID);
        } else {
            shouldNotReachHere();
        }
    }

    /**
     * Read a field from an object or an array
     *
     * <p>
     * Compiler Stack: ..., OOP, INT -> ..., VALUE
     * <p>
     *
     * @param type the field type
     * @param check the type of checking needed
     */
    protected void read(Type type, int check) {
        c.begin();
        Local index = localStore(INT);
        Local oop   = localStore(OOP);

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
        c.end();
    }

    /**
     * Write a field to an object or an array.
     *
     * <p>
     * Compiler Stack: ..., OOP, VALUE, INT -> ...
     * <p>
     *
     * @param type the field type
     * @param check the type of checking needed
     */
    protected void write(Type type, int check) {
        if (check == STORECHECK) {
            c.swap();
            callVMExtension(MethodOffsets.java_lang_VM$do_arrayOopStore, VOID);
        } else {
            c.begin();
            Local index = localStore(INT);
            Local val   = localStore(type.getPrimitiveType());
            Local oop   = localStore(OOP);

            /*
             * Do any checking that is needed.
             */
            switch(check) {
                case NULLCHECK:   nullCheck(oop);              break;
                case BOUNDSCHECK: boundsCheck(oop, index);     break;
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
                writeBarrier();
            }

            /*
             * Write the value.
             */
            c.write(type);
            c.end();
        }
    }


    /**
     * Insert write barrier code
     *
     * <p>
     * Compiler Stack: ..., REF -> ..., REF
     * <p>
     */
    private void writeBarrier() {
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
        c.comment("+nullCheck()");
        c.literal(0).eq();
        callNullPointerExceptionIfTrue();
        c.comment("-nullCheck()");
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

        c.comment("+boundsCheck()");

        /*
         * Start off with a null check.
         */
        nullCheck(oop);

        /*
         * Get the element count.
         */
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

        c.comment("-boundsCheck()");
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
        getValueAsWordOffset();
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
        getValueAsWordOffset();
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
        getVtableFromOop();
        getValueAsWordOffset();
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
        callVMExtension(MethodOffsets.java_lang_VM$do_findSlot, INT);
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
            Local slot = localStore(INT);
            Local oop  = localStore(OOP);
            nullCheck(oop);
            c.load(oop);
            getVtableFromOop();
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
        c.begin();
        Local oop = localStore(OOP);
        nullCheck(oop);
        c.load(oop);
        c.end();
        writeGlobal(Global.java_lang_ServiceOperation$pendingException);
        threadSwitchFor(ServiceOperation.THROW);
    }

    /**
     * Start an exception handler.
     *
     * <p>
     * Compiler Stack: _ -> OOP
     * <p>
     */
    protected void do_catch() {
        readGlobal(Global.java_lang_ServiceOperation$pendingException);
    }

    /**
     * Execute a monitor enter.
     *
     * <p>
     * Compiler Stack: OOP -> _
     * <p>
     */
    protected void do_monitorenter() {
        callVMExtension(MethodOffsets.java_lang_VM$do_monitorenter, VOID);
    }

    /**
     * Execute a monitor exit.
     *
     * <p>
     * Compiler Stack: OOP -> _
     * <p>
     */
    protected void do_monitorexit() {
        callVMExtension(MethodOffsets.java_lang_VM$do_monitorexit, VOID);
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
        c.begin();
        Local oop = localStore(OOP);
        nullCheck(oop);
        c.load(oop);
        c.end();
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
        callVMExtension(MethodOffsets.java_lang_VM$do_new, OOP);
    }

    /**
     * Allocate a new array.
     *
     * <p>
     * Compiler Stack: SIZE, CLASS -> ..., OOP
     * <p>
     */
    protected void do_newarray() {
        callVMExtension(MethodOffsets.java_lang_VM$do_newarray, OOP);
    }

    /**
     * Allocate a new array dimension.
     *
     * <p>
     * Compiler Stack: OOP, SIZE -> ..., OOP
     * <p>
     */
    protected void do_newdimension() {
        callVMExtension(MethodOffsets.java_lang_VM$do_newdimension, OOP);
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
        callVMExtension(MethodOffsets.java_lang_VM$do_class_clinit, VOID);
    }

    /**
     * Instanceof.
     *
     * <p>
     * Compiler Stack: ..., OOP, CLASS -> ..., INT
     * <p>
     */
    protected void do_instanceof() {
        callVMExtension(MethodOffsets.java_lang_VM$do_instanceof, INT);
    }

    /**
     * Checkcast.
     *
     * <p>
     * Compiler Stack: ..., OOP, CLASS -> ..., OOP
     * <p>
     */
    protected void do_checkcast() {
        callVMExtension(MethodOffsets.java_lang_VM$do_checkcast, OOP);
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
        int method = 0;
             if (t == BYTE)  method = MethodOffsets.java_lang_VM$do_lookup_b;
        else if (t == SHORT) method = MethodOffsets.java_lang_VM$do_lookup_s;
        else if (t == INT)   method = MethodOffsets.java_lang_VM$do_lookup_i;
        else Assert.shouldNotReachHere();
        callVMExtension(method, INT);
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


    /*-----------------------------------------------------------------------*\
     *                            Native Functions                           *
    \*-----------------------------------------------------------------------*/

    /**
     * invokenative swapping the compiler stack.
     *
     * <p>
     * Compiler Stack: [[... arg2], arg1] -> [VALUE]
     * <p>
     */
    final void invokenativeswapping(int ftn) {
        c.swapAll();
        invokenative(ftn);
    }

    /**
     * Read an address of a ceratin type.
     * <p>
     * Compiler Stack: ..., OOP, OFFSET -> ..., VALUE
     * <p>
     * @param type the type
     */
    private void badNative(Type type) {
        c.literal(type.getStructureSize()).mul();
        c.add();
        c.read(type);
    }

    /**
     * Read an address of a ceratin type.
     * <p>
     * Compiler Stack: ..., OOP, OFFSET -> ..., VALUE
     * <p>
     * @param type the type
     */
    private void get(Type type) {
        get(type, type);
    }

    /**
     * Read an address of a ceratin type.
     * <p>
     * Compiler Stack: ..., OOP, OFFSET -> ..., VALUE
     * <p>
     * @param type the type to read
     * @param 1type the type to index by
     */
    private void get(Type type, Type itype) {
        c.literal(itype.getStructureSize()).mul();
        c.add();
        c.read(type);
    }

    /**
     * Write an address of a ceratin type.
     * <p>
     * Compiler Stack: ..., OOP, OFFSET, VALUE -> ...
     * <p>
     * @param type the type
     */
    private void set(Type type) {
        set(type, type);
    }

    /**
     * Write an address of a ceratin type.
     * <p>
     * Compiler Stack: ..., OOP, OFFSET, VALUE -> ...
     * <p>
     * @param type the type to write
     * @param 1type the type to index by
     */
    private void set(Type type, Type itype) {
        c.swapAll();            // value, offset, oop
        c.swap();               // value oop, offset
        c.literal(itype.getStructureSize()).mul();
        c.add();                // value, ref
        c.write(type);
    }

    /**
     * invokenative.
     *
     * <p>
     * Compiler Stack: [[... arg2], arg1] -> [VALUE]
     * <p>
     */
    final void invokenative(int ftn) {
        c.comment("invokenative("+ftn+")");
        switch (ftn) {

            case Native.java_lang_UWord$fromPrimitive: {
                c.force(WORD);
                break;
            }

            case Native.java_lang_UWord$toPrimitive: {
                c.force(is32bit() ? INT : LONG);
                break;
            }

            case Native.java_lang_UWord$toInt: {
                c.force(INT);
                break;
            }

            case Native.java_lang_UWord$max: {
                literalWord( -1);
                break;
            }

            case Native.java_lang_Offset$zero:
            case Native.java_lang_UWord$zero: {
                zero(WORD);
                break;
            }

            case Native.java_lang_UWord$and: {
                c.and();
                break;
            }

            case Native.java_lang_UWord$or: {
                c.and();
                break;
            }

            case Native.java_lang_Offset$isZero:
            case Native.java_lang_UWord$isZero: {
                zero(WORD);
                c.eq();
                break;
            }

            case Native.java_lang_UWord$isMax: {
                literalWord( -1);
                c.eq();
                break;
            }

            case Native.java_lang_Unsafe$getObject: { // ..., OOP, OFFSET -> ..., VALUE
                get(OOP);
                break;
            }

            case Native.java_lang_Unsafe$getByte: {
                get(BYTE);
                break;
            }

            case Native.java_lang_Unsafe$getAsByte: {
                get(BYTE);
                break;
            }

            case Native.java_lang_Unsafe$getShort: {
                get(SHORT);
                break;
            }

            case Native.java_lang_Unsafe$getChar: {
                get(USHORT);
                break;
            }

            case Native.java_lang_Unsafe$getInt: {
                get(INT);
                break;
            }

            case Native.java_lang_Unsafe$getLong: {
                get(LONG);
                break;
            }

            case Native.java_lang_Unsafe$getUWord: {
                get(WORD);
                break;
            }

            case Native.java_lang_Unsafe$getAsUWord: {
                get(WORD);
                break;
            }

            case Native.java_lang_Unsafe$getType: {
                c.drop(); // TEMP
                c.literal(999999); // TEMP
                break;
            }

            case Native.java_lang_Unsafe$setType: {
                c.drop(); // TEMP
                c.drop(); // TEMP
                c.drop(); // TEMP
                break;
            }

            case Native.java_lang_Unsafe$getLongAtWord: {
                get(LONG, WORD);
                break;
            }

            case Native.java_lang_Unsafe$charAt: { // ..., STR, OFFSET -> ..., VALUE
                c.begin();
                Label sob  = c.label();
                Label done = c.label();
                Local off  = localStore(INT);
                Local str  = localStore(OOP);
                c.load(str);
                getKlass();
                readField(FieldOffsets.java_lang_Klass$classID);
                c.literal(CID.STRING_OF_BYTES);
                c.eq();
                c.bt(sob);
                c.load(str);
                c.load(off);
                get(USHORT);
                c.store(off); // Use off to carry result over branch
                c.br(done);
            c.bind(sob);
                c.load(str);
                c.load(off);
                get(UBYTE);
                c.store(off); // Use off to carry result over branch
            c.bind(done);
                c.load(off);
                c.end();
                break;
            }

            case Native.java_lang_Unsafe$setByte: { // ..., OOP, OFFSET, VALUE -> ...
                set(BYTE);
                break;
            }

            case Native.java_lang_Unsafe$setChar: {
                set(USHORT);
                break;
            }

            case Native.java_lang_Unsafe$setInt: {
                set(INT);
                break;
            }

            case Native.java_lang_Unsafe$setLong: {
                set(LONG);
                break;
            }

            case Native.java_lang_Unsafe$setShort: {
                set(SHORT);
                break;
            }

            case Native.java_lang_Unsafe$setObject: {
                set(OOP);
                break;
            }

            case Native.java_lang_Unsafe$setAddress: {
                c.force(REF); // Although its defined as an Object it is really just a REF
                set(REF);
                break;
            }

            case Native.java_lang_Unsafe$setUWord: {
                set(WORD);
                break;
            }

            case Native.java_lang_Unsafe$setLongAtWord: {
                set(LONG, WORD);
                break;
            }

            case Native.java_lang_Unsafe$copyTypes: {
                c.drop(); // TEMP // int
                c.drop(); // TEMP // java.lang.Address
                c.drop(); // TEMP // java.lang.Address
                break;
            }

            case Native.java_lang_Address$toObject: {
                c.force(OOP);
                break;
            }

            case Native.java_lang_Address$toUWord: {
                c.force(UWORD);
                break;
            }

            case Native.java_lang_Address$fromObject: {
                c.force(REF);
                break;
            }

            case Native.java_lang_Address$zero: {
                zero(REF);
                break;
            }

            case Native.java_lang_Address$isZero: {
                zero(REF);
                c.eq();
                break;
            }

            case Native.java_lang_Address$max: {
                literalWord(-1);
                c.force(REF);
                break;
            }

            case Native.java_lang_Address$isMax: {
                literalWord(-1);
                c.force(REF);
                c.eq();
                break;
            }

            case Native.java_lang_Offset$add:
            case Native.java_lang_Address$addOffset:
            case Native.java_lang_Address$add: {
                c.add();
                break;
            }

            case Native.java_lang_Offset$sub:
            case Native.java_lang_Address$subOffset:
            case Native.java_lang_Address$sub: {    // ADDR, INT -> ADDR
                c.sub();
                break;
            }

            case Native.java_lang_Address$and: {    // REF, WORD -> REF
                c.swap(); // word, ref
                c.force(WORD);
                c.and();
                c.force(REF);
                break;
            }

            case Native.java_lang_Address$or: {
                c.swap(); // word, ref
                c.force(WORD);
                c.or();
                c.force(REF);
                break;
            }

            case Native.java_lang_Address$diff: {    // ADDR, ADDR2 -> WORD
                c.swap(); // addr2, addr
                c.force(WORD);
                c.swap(); // addr, addr2
                c.force(WORD);
                c.sub();
                break;
            }

            case Native.java_lang_Address$eq:
            case Native.java_lang_Offset$eq:
            case Native.java_lang_UWord$eq: {
                c.eq();
                break;
            }

            case Native.java_lang_Address$ne:
            case Native.java_lang_Offset$ne:
            case Native.java_lang_UWord$ne: {
                c.ne();
                break;
            }

            case Native.java_lang_Offset$lt:
            case Native.java_lang_UWord$lo:
            case Native.java_lang_Address$lo: {
                c.lt();
                break;
            }

            case Native.java_lang_Offset$le:
            case Native.java_lang_UWord$loeq:
            case Native.java_lang_Address$loeq: {
                c.le();
                break;
            }

            case Native.java_lang_Offset$gt:
            case Native.java_lang_UWord$hi:
            case Native.java_lang_Address$hi: {
                c.gt();
                break;
            }

            case Native.java_lang_Offset$ge:
            case Native.java_lang_UWord$hieq:
            case Native.java_lang_Address$hieq: {
                c.ge();
                break;
            }

            case Native.java_lang_Offset$toPrimitive:
            case Native.java_lang_Offset$toInt: {
                c.force(INT);
                break;
            }

            case Native.java_lang_Offset$toUWord: {
                c.force(WORD);
                break;
            }

            case Native.java_lang_Offset$fromPrimitive: {
                c.force(WORD);
                break;
            }

            case Native.java_lang_Address$roundUp: {
                c.begin();
                c.literal(1);
                c.sub();
                Local alignment = localStore(INT);
                Local ref       = localStore(REF);
                c.load(ref);
                c.force(WORD);
                c.load(alignment).add();
                c.load(alignment).neg().and();
                c.force(REF);
                c.end();
                break;
            }

            case Native.java_lang_Address$roundUpToWord: {
                int wsm1 = WORD.getStructureSize() - 1;
                c.force(WORD);
                literalWord(wsm1);
                c.add();
                literalWord(~wsm1);
                c.and();
                c.force(REF);
                break;
            }

            case Native.java_lang_Address$roundDownToWord: {
                int wsm1 = WORD.getStructureSize() - 1;
                c.force(WORD);
                literalWord(~wsm1);
                c.and();
                c.force(REF);
                break;
            }

            case Native.java_lang_Address$roundDown: {
                c.begin();
                c.literal(1);
                c.sub();
                Local alignment = localStore(INT);
                Local ref       = localStore(REF);
                c.load(ref);
                c.force(WORD);
                c.load(alignment).neg().and();
                c.force(REF);
                c.end();
                break;
            }

            case Native.java_lang_VM$hashcode: {
                c.force(REF);
                c.force(WORD);
                break;
            }

            case Native.java_lang_VM$isBigEndian: {
                c.literal(c.isBigEndian());
                break;
            }

            case Native.java_lang_VM$getFP: {
                c.framePointer();
                break;
            }

            case Native.java_lang_VM$getMP: {
                c.literal(c.getFramePointerByteOffset(FP.method));
                c.add();
                c.read(OOP);
                break;
            }

            case Native.java_lang_VM$getPreviousFP: {
                c.literal(c.getFramePointerByteOffset(FP.returnFP));
                c.add();
                c.read(REF);
                break;
            }

            case Native.java_lang_VM$getPreviousIP: {
                c.literal(c.getFramePointerByteOffset(FP.returnIP));
                c.add();
                c.read(REF);
                break;
            }

            case Native.java_lang_VM$setPreviousFP: { // REF, VALUE -> _
                c.swap();                             // VALUE, REF -> _
                c.literal(c.getFramePointerByteOffset(FP.returnFP));
                c.add();
                c.write(REF);
                break;
            }

            case Native.java_lang_VM$setPreviousIP: { // REF, VALUE -> _
                c.swap();                             // VALUE, REF -> _
                c.literal(c.getFramePointerByteOffset(FP.returnIP));
                c.add();
                c.write(REF);
                break;
            }

            case Native.java_lang_UWord$toOffset:
            case Native.java_lang_VM$asThread:
            case Native.java_lang_VM$asKlass: {
                break;
            }

            case Native.java_lang_VM$getGlobalIntCount: {
                c.symbol("IntCount");
                c.read(INT);
                break;
            }

            case Native.java_lang_VM$getGlobalInt: {
                c.begin();
                Local offset = localStore(INT);
                readGlobal(INT, offset);
                c.end();
                break;
            }

            case Native.java_lang_VM$setGlobalInt: {
                c.begin();
                Local offset = localStore(INT);
                writeGlobal(INT, offset);
                c.end();
                break;
            }

            case Native.java_lang_VM$getGlobalAddrCount: {
                c.symbol("AddrCount");
                c.read(INT);
                break;
            }

            case Native.java_lang_VM$getGlobalAddr: {
                c.begin();
                Local offset = localStore(INT);
                readGlobal(WORD, offset);
                c.end();
                break;
            }

            case Native.java_lang_VM$setGlobalAddr: {
                c.begin();
                Local offset = localStore(INT);
                writeGlobal(WORD, offset);
                c.end();
                break;
            }

            case Native.java_lang_VM$getGlobalOopCount: {
                c.symbol("OopCount");
                c.read(INT);
                break;
            }

            case Native.java_lang_VM$getGlobalOop: {
                c.begin();
                Local offset = localStore(INT);
                readGlobal(OOP, offset);
                c.end();
                break;
            }

            case Native.java_lang_VM$setGlobalOop: {
                c.begin();
                Local offset = localStore(INT);
                writeGlobal(OOP, offset);
                c.end();
                break;
            }

            case Native.java_lang_VM$getGlobalOopTable: {
                getGlobalTable(OOP);
c.begin();
Local xxx = localStore(REF);
c.load(xxx);
c.end();
System.err.println("Hack still in case Native.java_lang_VM$getGlobalOopTable");
                break;
            }

            case Native.java_lang_VM$getBranchCount: {
                readGlobal(Global.branchCountHigh);
                c.force(UINT);
                c.convert(ULONG);
                c.literal(32);
                c.shl();
                readGlobal(Global.branchCountLow);
                c.force(UINT);
                c.convert(ULONG);
                c.or();
                c.force(LONG);
                break;
            }

            case Native.java_lang_ServiceOperation$cioExecute: {
                c.symbol("cioExecute");
                c.call(VOID);
                break;
            }

            case Native.java_lang_VM$fatalVMError: {
                emitFatal("fatalVMError");
                break;
            }

            case Native.java_lang_VM$threadSwitch: {
                threadSwitch(ServiceOperation.NONE);
                break;
            }

            case Native.java_lang_VM$executeCIO: {
                Label swit = c.label();
                Label done = c.label();

xpop(OOP);
                c.force(REF);
                writeGlobal(Global.java_lang_ServiceOperation$o2);
xpop(OOP);
                c.force(REF);
                writeGlobal(Global.java_lang_ServiceOperation$o1);
xpop(INT);
                writeGlobal(Global.java_lang_ServiceOperation$i6);
xpop(INT);
                writeGlobal(Global.java_lang_ServiceOperation$i5);
xpop(INT);
                writeGlobal(Global.java_lang_ServiceOperation$i4);
xpop(INT);
                writeGlobal(Global.java_lang_ServiceOperation$i3);
xpop(INT);
                writeGlobal(Global.java_lang_ServiceOperation$i2);
xpop(INT);
                writeGlobal(Global.java_lang_ServiceOperation$i1);
xpop(INT);
                writeGlobal(Global.java_lang_ServiceOperation$channel);
xpop(INT);
                writeGlobal(Global.java_lang_ServiceOperation$op);
xpop(INT);
                writeGlobal(Global.java_lang_ServiceOperation$context);

                readGlobal(Global.runningOnServiceThread);
                c.literal(0).eq().bt(swit);
                c.symbol("cioExecute");
                c.call(VOID);
                c.br(done);
            c.bind(swit);
                threadSwitch(ServiceOperation.CHANNELIO);
            c.bind(done);
                break;
            }

            case Native.java_lang_VM$executeGC: {
                threadSwitch(ServiceOperation.GARBAGE_COLLECT);
                break;
            }

            case Native.java_lang_VM$executeCOG: {
                c.force(REF);
                writeGlobal(Global.java_lang_ServiceOperation$o2);
                c.force(REF);
                writeGlobal(Global.java_lang_ServiceOperation$o1);
                writeGlobal(Global.java_lang_ServiceOperation$i1);
                threadSwitch(ServiceOperation.COPY_OBJECT_GRAPH);
                break;
            }

            case Native.java_lang_VM$serviceResult: {
                readGlobal(Global.java_lang_ServiceOperation$result);
                break;
            }

            case Native.java_lang_VM$deadbeef:
            case Native.java_lang_VM$zeroWords: {
                c.begin();
                Label loop = c.label();
                Label done = c.label();
                c.force(REF);
                Local end   = localStore(REF);
                c.force(REF);
                Local start = localStore(REF);
            c.bind(loop);
                c.load(start).load(end).eq().bt(done);
                c.literal((ftn == Native.java_lang_VM$deadbeef) ? 0xDEADBEEF : 0);
                c.load(start);
                c.write(INT);
                c.load(start).literal(4).add().store(start);
                c.br(loop);
            c.bind(done);
                c.end();
                break;
            }

            case Native.java_lang_VM$callStaticNoParm:
            case Native.java_lang_VM$callStaticOneParm: {
                Local parm = null;
                c.begin();
                if (ftn == Native.java_lang_VM$callStaticOneParm) {
                    parm = localStore(OOP);
                }
                Local slot = localStore(INT);
                Local cls  = localStore(OOP);
                if (ftn == Native.java_lang_VM$callStaticOneParm) {
                    c.load(parm);
                }
                c.load(cls);
                getStable();
                c.load(slot).literal(WORD.getStructureSize()).mul();
                c.add();
                c.read(OOP);
                c.call(VOID, Compiler.C_JVM_DYNAMIC);
                c.end();
                break;
            }

            case Native.java_lang_CheneyCollector$memoryProtect: {
                writeGlobal(Global.cheneyEndMemoryProtect);
                writeGlobal(Global.cheneyStartMemoryProtect);
                break;
            }

            case Native.java_lang_VM$lcmp: {
                callVMExtension(MethodOffsets.java_lang_VM$do_lcmp, INT);
                break;
            }

            case Native.java_lang_VM$allocateVirtualStack: {
                c.drop();  // TEMP
/*if[CHUNKY_STACKS]*/
                emitFatal("CHUNKY_STACKS are enabled");
/*end[CHUNKY_STACKS]*/
                zero(REF); // TEMP
                break;
            }


            case Native.java_lang_VM$addToClassStateCache: {
                c.drop();  // TEMP
                c.drop();  // TEMP
                break;
            }

            case Native.java_lang_VM$allocate: {
                c.drop();  // TEMP
                c.drop();  // TEMP
                c.drop();  // TEMP
                zero(OOP);
                break;
            }

            case Native.java_lang_VM$hasVirtualMonitorObject: {
                c.drop();  // TEMP
                zero(INT);
                break;
            }

            case Native.java_lang_VM$invalidateClassStateCache: {
                zero(INT);
                break;
            }

            case Native.java_lang_VM$removeVirtualMonitorObject: {
                zero(OOP);
                break;
            }


/*if[FLOATS]*/
            case Native.java_lang_VM$fcmpl: {
                callVMExtension(MethodOffsets.java_lang_VM$do_fcmpl, INT);
                break;
            }

            case Native.java_lang_VM$fcmpg: {
                callVMExtension(MethodOffsets.java_lang_VM$do_fcmpg, INT);
                break;
            }

            case Native.java_lang_VM$dcmpl: {
                callVMExtension(MethodOffsets.java_lang_VM$do_dcmpl, INT);
                break;
            }

            case Native.java_lang_VM$dcmpg: {
                callVMExtension(MethodOffsets.java_lang_VM$do_dcmpg, INT);
                break;
            }

            case Native.java_lang_VM$math: {
                throw new Error();
                break;
            }

            case Native.java_lang_VM$floatToIntBits: {
                c.force(INT);
                break;
            }

            case Native.java_lang_VM$doubleToLongBits: {
                c.force(LONG);
                break;
            }

            case Native.java_lang_VM$intBitsToFloat: {
                c.force(FLOAT);
                break;
            }

            case Native.java_lang_VM$longBitsToDouble: {
                c.force(DOUBLE);
                break;
            }
/*end[FLOATS]*/

            /*
             * Undefined functions.
             */
            default: {
                throw new Error("Undefined native "+ftn);
            }
        }
    }


    private void xpop(Type type) {
/*if[INCLUDE_EXECUTECIO_PARMS]*/
/*else[INCLUDE_EXECUTECIO_PARMS]*/
//      c.pop(type);
/*end[INCLUDE_EXECUTECIO_PARMS]*/
    }
}
