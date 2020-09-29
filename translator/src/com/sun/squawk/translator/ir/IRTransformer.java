/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir;

import com.sun.squawk.util.Assert;
import com.sun.squawk.translator.Translator;
import com.sun.squawk.translator.ir.instr.*;

/**
 * An instance of this transforms an <code>IR</code> from its JVM form to
 * its Squawk form. A number of these transformations are done in part by
 * the {@link IRBuilder} that built the IR as well as the {@link InstructionEmitter} that
 * emits the final Squawk bytecode. The transformations performed are:<p>
 *
 * <b>a. Typing the variables: </b>
 *
 *    Local variables are reallocated such that they have a fixed type for the
 *    lifetime of a method. They are partitioned into the following types:
 *    int, long, float, double, or reference.<p>
 *
 * <b>b. Long and floating point comparisions</b>:
 *
 *    The <i>lcmp</i>, <i>dcmpl</i>, <i>dcmpg</i>, <i>fcmpl</i>, <i>fcmpg</i>
 *    instructions are all replaced with <i>ifxx</i>, and <i>if_cmpxx</i>
 *    instructions.<p>
 *
 * <b>c. Operand stack spilling at invokes</b>:
 *
 *    The operand stack is spilt at all invoke instructions and any other
 *    instructions that may be implemented internally by a call back into
 *    Java code. See {@link Instruction#constrainsStack()}.<p>
 *
 * <b>d. Operand stack spilling at backward branches</b>:
 *
 *    The operand stack is spilt at all backward branches.
 *    See {@link Instruction#constrainsStack()}.<p>
 *
 * <b>e. Default initialization for local variables</b>:  *** TO BE COMPLETED ***
 *
 *    All local variables found to be uninitialized at an invoke instruction
 *    have instructions added to the start of a method that initialize them to
 *    their default values. This will guarantee that there are no uninitialized
 *    local variables during a garbage collection.
 *
 *    <p> -- OR ADD A 'DEAD OOP' MAP
 *    <p> -- For now we just make the 'extend' bytecode to zero all the locals.
 *
 * <b>f. Use special instructions for accessing receiver</b>:  *** TO BE COMPLETED ***
 *
 *    Special Squawk instructions are used for accessing fields of the
 *    receiver in virtual methods and for accessing static fields of the
 *    enclosing class in static methods. This removes the explicit load of
 *    the receiver for the former and the class initialization test for the
 *    latter.<p>
 *
 * <b>g. Removal of stack manipulation instructions</b>:
 *
 *    All {@link StackOp stack manipulation instructions} are replaced with
 *    the use of extra locals. For example, the code for
 *    {@link Vector#addElement} contains the following statement:
 *    <p><blockquote><pre>
 *        elementData[elementCount++] = obj;
 *    </pre></blockquote></p>
 *    This is expressed as follows in JVM bytecode:
 *    <p><blockquote><pre>
 *        aload_0
 *        getfield "elementData"
 *        aload_0
 *        dup
 *        getfield "elementCount"
 *        dup_x1
 *        iconst_1
 *        iadd
 *        putfield "elementCount"
 *        aload_1
 *        aastore
 *    </pre></blockquote></p>
 *    This is transformed into:
 *    <p><blockquote><pre>
 *        this_getfield "elementCount"
 *        store_2    // new temporary variable
 *        load_2
 *        const_1
 *        add_i
 *        this_putfield "elementCount"
 *        this_getfield "elementData"
 *        load_2
 *        load_1
 *        astore_o
 *    </pre></blockquote></p>
 *
 * <b>h. Endianess normalization</b>:  *** TO BE COMPLETED ***
 *
 *    The endianess of the bytecodes is normalized to the target platform.
 *    That is, a bytecode operand is written such that a single load
 *    instruction can be used when decoding it. Of course, this will only be
 *    really useful for the interpreter (or jitter) when its running on a
 *    platform that can do non-aligned loads (such as the x86).<p>
 *
 * <b>i. Parameter passing convention</b>:
 *
 *    The order in which parameters are passed is reordered if necessary to
 *    conform with the calling convention dictated by the Application Binary
 *    Interface (ABI) of the underlying platform. However, the parameter
 *    evaluation order must remain consistent with the JVM specification
 *   (i.e. left-to-right).<p>
 *
 * <b>j. Object construction</b>:
 *
 *    The sequence for creating a new instance of class <code>Foo</code> is
 *    transformed to be:
 *    <p><blockquote><pre>
 *        object [index of Foo class in object pool]
 *        new  // performs clinit test
 *        store_2    // new temporary variable
 *        [evaluation of other constructor parameters}
 *  i)    load_2
 *  ii)   [load other parameters]
 *        invokestatic  "Foo(...)"  // call constructor
 *    </pre></blockquote></p>
 *    The code at i) and ii) will be swapped if necessary by the <b>i</b>
 *    transformation.
 *
 * <b>k. Native calls</b>:
 *
 *    Squawk native methods are defined in com.sun.squawk.vm.Native and they
 *    must all be static methods. All calls to such methods are implemened using
 *    an <i>invokenative</i> bytecode.<p>
 *
 * <b>l. More accurate max-stack calculation.</b>
 *
 *    The maxstack value should be re-calculated by InstructionEmitter.java<p> *** TO BE COMPLETED ***
 *
 * @author  Doug Simon
 */
public final class IRTransformer implements OperandVisitor {

    /**
     * Flag to say that the parameters to native methods should not be
     * reversed even though normal parameter reversing is being done.
     */
    private static final boolean DO_NOT_REVERSE_NATIVE_CALLS = true;

    /**
     * The method being transformed.
     */
    private final Method method;

    /**
     * The IR of the method being transformed.
     */
    private final IR ir;

    /**
     * The frame used to allocate locals for spilling.
     */
    private final Frame frame;

    /**
     * Flags whether or not one of the operands of the current instruction
     * being visited is spilt.
     */
    private boolean oneOrMoreOperandsSpilt;

    /**
     * Flags if the garbage collector may have been called in the
     * current path of execution.
     */

    /**
     * The IR use counter.
     */
    private static IRUseCounter useCounter = new IRUseCounter();

    /**
     * Creates an IRTransformer.
     *
     * @param ir       the IR to be transformed
     * @param method   the method encapsulating the IR
     * @param frame    the method's frame
     */
    public IRTransformer(IR ir, Method method, Frame frame) {
        this.ir     = ir;
        this.method = method;
        this.frame  = frame;
    }

    /**
     * Do the transformations to the IR.
     */
    public void transform() {

        /*
         * Pass 0 - Count the uses of every stack producer.
         */
        useCounter.count(ir);

        /*
         * Pass 1 - Iterate over the instructions to insert loads for stack values that must be spilt.
         */
        Instruction instruction = ir.getHead();
        while (instruction != null) {
            oneOrMoreOperandsSpilt = false;
            instruction.visit(this);

            /*
             * The filling for reversing parameters is done here instead of doOperand.
             */
            if (instructionNeedParametersReversed(instruction)) {
                Invoke invoke = (Invoke)instruction;
                StackProducer[] parameters = invoke.getParameters();
                for (int i = parameters.length - 1; i >= 0; --i) {
                    fill(invoke, parameters[i]);
                }
            }

            instruction = instruction.getNext();
        }

        /*
         * Pass 2 - Iterate over all the instructions to spill stack
         * values for the loads which were inserted in the previous pass.
         */
        for (instruction = ir.getHead() ; instruction != null ; instruction = instruction.getNext()) {

            /*
             * Spill the instruction's result if necessary.
             */
            if (instruction instanceof StackProducer) {
                StackProducer producer = (StackProducer)instruction;
                if (producer.isSpilt()) {
                    Local local = producer.getSpillLocal();
                    Assert.that(local != null);

                    /*
                     * If the current instruction is the fill corresponding to the pending
                     * spill and the spill was not due to a dup or a phi merge, then
                     * cancel the spill and remove the fill
                     */
                    if (!producer.isDuped() && !(producer instanceof StackMerge) && producer.getNext() instanceof LoadLocal) {
                        LoadLocal load = (LoadLocal)instruction.getNext();
                        if (load.getLocal() == local) {
                            ir.remove(load);
                            continue;
                        }
                    }

                    Instruction store = new StoreLocal(local, producer, false);
                    ir.insertAfter(store, instruction);
                    instruction = store;
                }
            }
        }

    }

    /**
     * Check to see if the parameters of an instruction should be reversed.
     *
     * @param   instruction  the instruction to be tested
     * @return  true if the parameters should be reversed
     */
    boolean instructionNeedParametersReversed(Instruction instruction) {
         if (Translator.REVERSE_PARAMETERS && instruction instanceof Invoke) {
              Invoke invoke = (Invoke)instruction;
              if (DO_NOT_REVERSE_NATIVE_CALLS && invoke.getMethod().isNative()) {
                   return false;
              }
              return true;
         }
         return false;
    }

    /**
     * Processes a single operand of an instruction. This method marks the
     * given operand for spilling if it needs it and inserts a load of the
     * spilt value.
     *
     * @param   instruction  the instruction to which the operand belongs
     * @param   operand      the operand to process
     * @return  the value of the operand which may be different from
     *                       the original value
     */
    public StackProducer doOperand(Instruction instruction, StackProducer operand) {

        /*
         * Filling of reversed parameters is done in transform()
         */
        if (instructionNeedParametersReversed(instruction)) {
            return operand;
        }

        Assert.that(operand != null);

        if (oneOrMoreOperandsSpilt || operand.isSpilt()) {
            /*
             * Flags whether or not to spill the result of this operand to a local variable
             */
            boolean spill = true;
            oneOrMoreOperandsSpilt = true;
            if (!operand.isDuped() && operand.getUseCount() < 2) {
                if (operand instanceof LoadLocal) {

                    /*
                     * Loads of local variables may be hoisted if there is no store
                     * to the local variable between the load and its use
                     */
                    LoadLocal load = (LoadLocal) operand;
                    Local local = load.getLocal();
                    if (connectsWithoutStoringTo(operand, instruction, local)) {

                        /*
                         * Hoist the load up to the current insertion point.
                         */
                        operand.cancelSpilling();
                        ir.remove(operand);
                        ir.insertBefore(operand, instruction);

                        /*
                         * There's no need to spill the result of this parameter any more
                         */
                        spill = false;
                    }
                } else if (operand instanceof Constant) {

                    /*
                     * Loads of constants may always be hoisted
                     */
                    operand.cancelSpilling();
                    ir.remove(operand);
                    ir.insertBefore(operand, instruction);

                    /*
                     * There's no need to spill the result of this parameter any more
                     */
                    spill = false;
                }
            }
            if (spill) {

                /*
                 * Flag the operand for spilling
                 */
                if (!operand.isSpilt()) {
                    frame.spill(operand);
                }

                /*
                 * Load the spilt result
                 */
                StackProducer load = fill(instruction, operand);

                /*
                 * The load becomes the replacement operand
                 */
                operand = load;
            }
        }
        Assert.that(operand != null);
        return operand;
    }

    /**
     * Inserts the load instruction that implements the fill half of a
     * spill/fill.
     *
     * @param   instruction  the consumer
     * @param   operand      the producer
     * @return  the inserted load
     */
    private StackProducer fill(Instruction instruction, StackProducer operand) {
        Local spillLocal = operand.getSpillLocal();
        StackProducer load = new LoadLocal(operand.getType(), spillLocal, false);
        ir.insertBefore(load, instruction);
        return load;
    }

    /**
     * Determines if a given local variable is written to within a sequence of code.
     *
     * @param  start   the first instruction in the sequence of code
     * @param  end     the last instruction in the sequence of code
     * @param  local  the local variable to test
     * @return true only if <code>local</code> is not written to between
     *                <code>start</code> and <code>end</code>
     */
    private boolean connectsWithoutStoringTo(Instruction start, Instruction end, Local local) {
        Instruction instruction = start;
        while (instruction != end) {
            if (instruction instanceof LocalVariable) {
                LocalVariable lvi = (LocalVariable) instruction;
                if (lvi.getLocal() == local && lvi.writesValue()) {
                    return false;
                }
            }
            Assert.that(instruction.getNext() != null);
            instruction = instruction.getNext();
        }
        return true;
    }
}


/**
 * Private class to count the number of times an operand is used.
 */
class IRUseCounter implements OperandVisitor {

    /**
     * Iterate over the IR counting the operatd uses.
     */
    void count(IR ir) {
        Instruction instruction = ir.getHead();
        while (instruction != null) {
            instruction.visit(this);
            instruction = instruction.getNext();
        }
    }

    /**
     * Processes a single operand of an instruction. This method marks the
     * given operand for spilling if it needs it and inserts a load of the
     * spilt value.
     *
     * @param   instruction  the instruction to which the operand belongs
     * @param   operand      the operand to process
     * @return  the value of the operand which may be different from
     *                       the original value
     */
    public StackProducer doOperand(Instruction instruction, StackProducer operand) {
        operand.incUseCount();
        return operand;
    }

}
