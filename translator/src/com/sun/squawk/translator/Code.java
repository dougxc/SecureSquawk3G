/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator;

import com.sun.squawk.translator.ci.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.util.*;
import java.io.*;

/**
 * An instance of <code>Code</code> represents the "Code"  attribute of a
 * method in a class file.
 *
 * @author  Doug Simon
 */
public final class Code {

    /**
     * A zero length array of <code>Code</code>.
     */
    public final static Code[] NO_CODE = {};


    /*---------------------------------------------------------------------------*\
     *                           Fields and constructor                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Creates a new <code>Code</code> representing the code of a method.
     *
     * @param code  the data in the "Code" attribute
     */
    public Code(byte[] code) {
        this.code = code;
    }

    /*---------------------------------------------------------------------------*\
     *                           Constructor synthesis                           *
    \*---------------------------------------------------------------------------*/

    /**
     * A sentinel bytecode array to denote a synthesized default constructor.
     */
    public static final byte[] SYNTHESIZED_DEFAULT_CONSTRUCTOR_CODE = {};

    /**
     * Gets a constant pool instance for processing the "Code" attribute for
     * synthesized constructor.
     *
     * @param  declaringClass  the class enclosing this method
     * @return  the constant pool that whose 6th entry is a 'CONSTANT_Methodref'
     *          entry referring to the default constructor of the super class
     */
    private ConstantPool getConstantPoolForSynthesizedConstructor(Klass declaringClass) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeShort(7);  // constant_pool_count

            /*
             * 1 - CONSTANT_Utf8: "<init>"
             */
            dos.writeByte(ConstantPool.CONSTANT_Utf8);
            dos.writeUTF("<init>");

            /*
             * 2 - CONSTANT_Utf8: "()V"
             */
            dos.writeByte(ConstantPool.CONSTANT_Utf8);
            dos.writeUTF("()V");

            /*
             * 3 - CONSTANT_Utf8: <super class>
             */
            Assert.that(!declaringClass.isArray() && !declaringClass.isPrimitive());
            String superName = declaringClass.getSuperclass().getName().replace('.','/');
            dos.writeByte(ConstantPool.CONSTANT_Utf8);
            dos.writeUTF(superName);

            /*
             * 4 - CONSTANT_NameAndType: name_index = 1, signature_index = 2
             */
            dos.writeByte(ConstantPool.CONSTANT_NameAndType);
            dos.writeShort(1);
            dos.writeShort(2);

            /*
             * 5 - CONSTANT_Class: name_index = 3
             */
            dos.writeByte(ConstantPool.CONSTANT_Class);
            dos.writeShort(3);

            /*
             * 6 - CONSTANT_Methodref: class_index = 5, name_and_type_index = 4
             */
            dos.writeByte(ConstantPool.CONSTANT_Methodref);
            dos.writeShort(5);
            dos.writeShort(4);

            dos.close();
            byte[] data = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ClassFileReader cfr = new ClassFileReader(bais, ClassFileLoader.getClassFilePath(declaringClass));
            return new ConstantPool(cfr, declaringClass);
        } catch (IOException e) {
            Assert.shouldNotReachHere();
            return null;
        }
    }

    /**
     * Gets the "Code" attribute for this synthesized constructor which
     * contains the bytecode implementing a call to the default constructor
     * of the super class.
     *
     * @return the data in the "Code" attribute for this synthesized constructor
     *         excluding the first 6 bytes
     */
    private byte[] getCodeForSynthesizedConstructor() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            /*
             * Now build the "Code" attribute
             */
            dos.writeShort(1);                          // max_stack
            dos.writeShort(1);                          // max_locals
            dos.writeInt(5);                            // code_length
            dos.writeByte(Opcode.opc_aload_0);          // code[0]
            dos.writeByte(Opcode.opc_invokespecial);    // code[1]
            dos.writeShort(6);                          // code[2-3]
            dos.writeByte(Opcode.opc_return);           // code[4]
            dos.writeShort(0);                          // exception_table_length
            dos.writeShort(0);                          // attributes_count
            dos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            Assert.shouldNotReachHere();
            return null;
        }
    }


    /*---------------------------------------------------------------------------*\
     *                      Code attribute and conversion                        *
    \*---------------------------------------------------------------------------*/

    /**
     * The bytecode for the method. This will initially be the data in the
     * "Code" class file attribute for this method.
     */
    private byte[] code;

    /**
     * CodeParser held between phase 1 and 2.
     */
    private CodeParser codeParser;

    /**
     * IRBuilder held between phase 1 and 2.
     */
    private IRBuilder irBuilder;

    /**
     * Convert the code of this method from its Java bytecode form to its
     * Squawk bytecode form. This must only be called once and cannot be called
     * for an abstract or native <code>Method</code>.
     *
     * @param  method  the method owning this code
     * @param  index   the index of this method in the symbols table of the enclosing class
     */
    private void convertPhase1(Method method, int index) {
        try {
            Assert.that(code != null, "code is null for " + method);
            Klass declaringClass = method.getDefiningClass();
            ClassFile cf = Translator.getClassFile(declaringClass);

            /*
             * Write trace message.
             */
            if (Klass.DEBUG && Tracer.isTracing("converting", method.toString())) {
                Tracer.traceln("[converting method " + method + "]");
            }

            /*
             * Get or create the constant pool.
             */
            ConstantPool constantPool;
            if (code == SYNTHESIZED_DEFAULT_CONSTRUCTOR_CODE) {
                constantPool = getConstantPoolForSynthesizedConstructor(method.getDefiningClass());
                code = getCodeForSynthesizedConstructor();
            } else {
                constantPool = cf.getConstantPool();
            }

            /*
             * Ensure the parameter and return types are loaded.
             */
            Translator.instance().load(method.getReturnType());
            Klass[] parameterTypes = method.getParameterTypes();
            for (int i = 0 ; i < parameterTypes.length ; i++) {
                Translator.instance().load(parameterTypes[i]);
            }

            /*
             * Get the code parser and build the IR.
             */
            codeParser = new CodeParser(method, code, constantPool);
            irBuilder = new IRBuilder(codeParser);
            IR ir = irBuilder.getIR();

            /*
             * Add the object references into the table of constants.
             */
            for (Instruction instruction = ir.getHead() ; instruction != null ; instruction = instruction.getNext()) {
                Object object = instruction.getConstantObject();
                if (object != null) {
                    cf.addConstantObject(object);
                }
            }
        } finally {
            code = null; // Allow the code to be garbage collected
        }
    }

    /**
     * Second phase of the convertion.
     *
     * @param  method  the method owning this code
     * @param  index   the index of this method in the symbols table of the enclosing class
     */
    private void convertPhase2(Method method, int index) {
        try {
            Klass definingClass = method.getDefiningClass();
            ClassFile cf = Translator.getClassFile(definingClass);
            IR ir = irBuilder.getIR();
            Frame frame = irBuilder.getFrame();

/*if[J2ME.DEBUG]*/
            /*
             * Write trace message.
             */
            if (Klass.DEBUG && Tracer.isTracing("ir0", method.toString())) {
                Tracer.traceln("");
                Tracer.traceln("++++ IR0 for " + method + " ++++");
                new InstructionTracer(ir).traceAll();
                Tracer.traceln("---- IR0 for " + method + " ----");
            }
/*end[J2ME.DEBUG]*/

            /*
             * Transform the IR.
             */
            IRTransformer transformer = new IRTransformer(ir, method, frame);
            transformer.transform();

            /*
             * Do the local variable slot allocation.
             */
            SlotAllocator allocator = new SlotAllocator(ir, method);
            Klass[] localTypes = allocator.transform();

/*if[J2ME.DEBUG]*/
            /*
             * Trace the instructions again now that they have been transformed.
             */
            if (Klass.DEBUG && Tracer.isTracing("ir1", method.toString())) {
                Tracer.traceln("");
                Tracer.traceln("++++ IR1 for " + method + " ++++");
                new InstructionTracer(ir).traceAll();
                Tracer.traceln("---- IR1 for " + method + " ----");
            }
/*end[J2ME.DEBUG]*/

            /*
             * Create the method body.
             */
            MethodBody body = ir.getMethodBody(
                                                method,
                                                index,
                                                codeParser,
                                                localTypes,
                                                allocator.getClearedSlotCount(),
                                                frame.getMaxStack(),
                                                cf
                                              );
/*if[J2ME.DEBUG]*/
            /*
             * Trace the instructions again now that they have their Squawk
             * bytecode offsets.
             */
            if (Klass.DEBUG && Tracer.isTracing("ir2", method.toString())) {
                Tracer.traceln("");
                Tracer.traceln("++++ IR2 for " + method + " ++++");
                new InstructionTracer(ir).traceAll();
                Tracer.traceln("---- IR2 for " + method + " ----");
            }

            /*
             * Trace the emitted Squawk bytecode.
             */
            if (Klass.DEBUG && Tracer.isTracing("methods", method.toString())) {
                Tracer.traceln("");
                Tracer.traceln("++++ Method for " + method + " ++++");
                //new MethodBodyTracer(body).traceAll();
                MethodBodyTracer mbt = new MethodBodyTracer(body);
                mbt.traceHeader();
                new InstructionTracer(ir).traceWithBody(mbt);
                Tracer.traceln("---- Method for " + method + " ----");
            }
/*end[J2ME.DEBUG]*/

            /*
             * Install the transformed method into the class.
             */
            definingClass.installMethodBody(body, method.isStatic());

            /*
             * Write trace message.
             */
            if (Klass.DEBUG && Tracer.isTracing("converting", method.toString())) {
                Tracer.traceln("[converted method " + method + "]");
            }
        } finally {
            irBuilder  = null; // Allow GC
            codeParser = null; // Allow GC
        }
    }

    /**
     * Convert the code of this method from it Java bytecode form to its
     * Squawk bytecode form. This must only be called once and cannot be called
     * for an abstract or native <code>Method</code>.
     *
     * @param  method  the method owning this code
     * @param  index   the index of this method in the symbols table of the enclosing class
     * @param  phase   the compilation phase (1 or 2)
     */
    private void convert0(Method method, int index, int phase) {
        try {
            if (phase == 1) {
                convertPhase1(method, index);
            } else {
                convertPhase2(method, index);
            }
        } catch (LinkageError e) {
            /*
             * Write trace message and re-throw exception.
             */
            if (Klass.DEBUG && Tracer.isTracing("converting", method.toString())) {
                Tracer.traceln("[error converting method " + method + ": " + e + "]");
            }
            code = null;
            irBuilder = null;
            codeParser = null;
            throw e;
        }
    }

    /**
     * Convert the code of this method from it Java bytecode form to its
     * Squawk bytecode form. This must only be called once and cannot be called
     * for an abstract or native <code>Method</code>.<p>
     *
     * This method is a wrapper for the real conversion functionality that
     * either times the translation or not depending on whether or not a
     * timer is being used.
     *
     * @param  method  the method owning this code
     * @param  index   the index of this method in the symbols table of the enclosing class
     * @param  phase   the compilation phase (1 or 2)
     */
    void convert(final Method method, final int index, final int phase) {
/*if[J2ME.DEBUG]*/
        if (ComputationTimer.enabled) {
            ComputationTimer.time("converting phase "+phase, new ComputationTimer.Computation() {
                public Object run() {
                    convert0(method, index, phase);
                    return null;
                }
            });
            return;
        }
/*end[J2ME.DEBUG]*/
        convert0(method, index, phase);
    }

}
