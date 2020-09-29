/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir;

import com.sun.squawk.vm.CID;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.Arrays;
import com.sun.squawk.util.IntHashtable;
import com.sun.squawk.util.Tracer;
import com.sun.squawk.translator.Translator;
import com.sun.squawk.translator.ci.CodeParser;
import com.sun.squawk.translator.ci.Opcode;
import com.sun.squawk.translator.ir.instr.StackMerge;
import com.sun.squawk.translator.ir.instr.StackProducer;
import java.util.Enumeration;
import com.sun.squawk.translator.ci.LocalVariableTableEntry;

/**
 * This class is used to emulate the execution frame of a method. That
 * is, it models the operand stack and local variables.
 *
 * @author  Doug Simon
 */
public final class Frame {

    /**
     * The object used to throw contextualised linkage errors.
     */
    private final CodeParser codeParser;

    /**
     * Creates a Frame instance to emulate and verify the execution of a
     * single class file method.
     *
     * @param codeParser  the parser used to parse the "Code" attribute of the method being emulated
     * @param extraLocals the number of extra local variables needed
     */
    public Frame(CodeParser codeParser, int extraLocals) {
        this.maxStack = codeParser.getMaxStack();
        this.codeParser = codeParser;

        /*
         * Initialize the operand stack
         */
        stack = new StackProducer[maxStack];

        /*
         * Initialize the types in the local variables
         */
        localTypes = new Klass[codeParser.getMaxLocals() + extraLocals];
        Method method = codeParser.getMethod();
        Klass[] parameterTypes = method.getParameterTypes();
        Local[] parameterLocals = null;
        if (Translator.REVERSE_PARAMETERS && method.isVMdoMethod()) {
            parameterLocals = new Local[parameterTypes.length];
        }
        int javacIndex = 0;

        /*
         * Initialize 'this' in non-static methods. The type of 'this' is
         * UNINITIALIZED_THIS if this method is a constructor in any class
         * except java.lang.Object otherwise it is the class in which the
         * method was defined.
         */
        if (!method.isStatic() || method.isConstructor()) {
            Assert.that(parameterLocals == null);
            Klass thisType = method.getDefiningClass();
            if (method.isConstructor() && thisType != Klass.OBJECT) {
                thisType = Klass.UNINITIALIZED_THIS;
            }
            Local thisLocal = allocateParameter(thisType, javacIndex);
            store(javacIndex, thisType, thisLocal);
            javacIndex++;
        }

        /*
         * Initialize locals for the parameters.
         */
        int parameterIndex = javacIndex;
        for (int i = 0 ; i < parameterTypes.length ; i++) {
            Klass parameterType = parameterTypes[i];
            Local parameterLocal = allocateParameter(parameterType, javacIndex);
            if (parameterLocals != null) {
                parameterLocals[i] = parameterLocal;
            }
            if (Klass.SQUAWK_64) {
                if (javacIndex != parameterIndex) {
                    Assert.that(parameterIndex < javacIndex);
                    parameterLocal.setParameterIndex(parameterIndex);
                }
                parameterIndex++;
            }
            store(javacIndex, parameterType, parameterLocal);
            javacIndex += (parameterType.isDoubleWord() ? 2 : 1);
        }
        parameterLocalsCount = javacIndex;

        /*
         * Adjust the parameter offsets for parameter order reversal.
         */
        if (parameterLocals != null) {
            parameterIndex = 0;
            for (int i = parameterTypes.length - 1 ; i >= 0 ; i--) {
                Klass parameterType = parameterTypes[i];
                parameterLocals[i].setParameterIndex(parameterIndex++);
                if (!Klass.SQUAWK_64 && parameterType.isDoubleWord()) {
                    parameterIndex++;
                }
            }
        }

        /*
         * Initialize the remaining local variables to the TOP type
         */
        while (javacIndex < localTypes.length) {
            localTypes[javacIndex++] = Klass.TOP;
        }
    }

    /**
     * Determines if the operand stack or local variable array currently
     * contains an entry representing an uninitialized object.
     *
     * @return true if the current frame state contains an entry representing
     *              an uninitialized object, false otherwise
     */
    public boolean containsUninitializedObject() {
        for (int i = 0 ; i < localTypes.length ; i++) {
            if (Klass.UNINITIALIZED.isAssignableFrom(localTypes[i])) {
                return true;
            }
        }
        for (int i = 0 ; i < sp ; i++) {
            StackProducer producer = stack[i];
            if (producer != null && Klass.UNINITIALIZED.isAssignableFrom(producer.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Changes the type of any object in a local varibale or on the operand
     * stack whose current type is UNINITIALIZED_THIS to be the type of the
     * enclosing class. This emulates the effect of a chained constructor
     * invocation (i.e. a call to another constructor in the same class or
     * a super class).
     */
    public void chainedConstructorInvoked() {
        Klass initializedType = codeParser.getMethod().getDefiningClass();
        for (int i = 0 ; i < localTypes.length ; i++) {
            if (localTypes[i] == Klass.UNINITIALIZED_THIS) {
                localTypes[i] = initializedType;
            }
        }
        for (int i = 0 ; i < sp ; i++) {
            StackProducer producer = stack[i];
            if (producer != null && producer.getType() == Klass.UNINITIALIZED_THIS) {
                producer.initializeType(initializedType);
            }
        }
    }

    /*---------------------------------------------------------------------------*\
     *                Special target for synchronized method                     *
    \*---------------------------------------------------------------------------*/

    /**
     * The target that will be the entry to the exception handler synthesized to
     * implement a synchronized method.
     */
    private Target finallyTargetForSynchronizedMethod;

    /**
     * Creates the target representing the address of the exception handler
     * that is synthesized to handle any exceptions in a synchronized method.
     *
     * @return  the target of the synthesized exception handler
     */
    public Target createFinallyTargetForSynchronizedMethod() {
        Assert.that(finallyTargetForSynchronizedMethod == null);
        Klass[] stack = new Klass[] { Klass.THROWABLE };
        Klass[] parameterLocals = new Klass[parameterLocalsCount];
        for (int i = 0 ; i < parameterLocalsCount ; i++) {
            parameterLocals[i] = getParameterTypeFor(getDerivedLocalTypeAt(i));
        }
        finallyTargetForSynchronizedMethod = new Target(999999, stack, parameterLocals);
        return finallyTargetForSynchronizedMethod;
    }


    /*---------------------------------------------------------------------------*\
     *                             Local variables                               *
    \*---------------------------------------------------------------------------*/

    /**
     * The instances of <code>Local</code> representing typed values in the
     * local variables of the method.
     */
    private IntHashtable localValues;

    /**
     * The local variable types.
     */
    private final Klass[] localTypes;

    /**
     * The number of local variable slots used to hold the incoming
     * parameters of the method.
     */
    private final int parameterLocalsCount;

    /**
     * Gets the number of local variable slots used to hold the incoming
     * parameters of the method.
     *
     * @return  the number of local variable slots used to hold the incoming
     *          parameters of the method
     */
    public int getParameterLocalsCount() {
        return parameterLocalsCount;
    }

    /**
     * Gets the number of local variable slots used by the method.
     *
     * @return  the number of local variable slots used by the method
     */
    public int getLocalsCount() {
        return localTypes.length;
    }

    /**
     * Gets the type of a local variable that is used to store a value of a
     * given class. This method partitions all classes into one of the
     * following categories:
     * <p><blockquote><pre>
     *
     *     Local Variable Type  |  Types
     *     ---------------------+-------
     *     INT                  | boolean, byte, short, int
     *     FLOAT                | float
     *     LONG                 | long
     *     DOUBLE               | double
     *     ADDRESS              | Address
     *     UWORD                | UWord
     *     OFFSET               | Offset
     *     REFERENCE            | types in java.lang.Object hierarchy
     *
     * </pre></blockquote><p>
     *
     * @param   type   the type of a value that will be stored in a local variable
     * @return  the local variable type for storing values of type <code>type</code>
     */
    public static Klass getLocalTypeFor(Klass type) {
        switch (type.getClassID()) {
            case CID.BOOLEAN:
            case CID.BYTE:
            case CID.SHORT:
            case CID.CHAR:
            case CID.INT: {
                return Klass.INT;
            }
            case CID.FLOAT:
            case CID.LONG:
            case CID.DOUBLE: {
                return type;
            }
            case CID.UWORD:
            case CID.OFFSET:
            case CID.ADDRESS: {
                return type;
            }
            default: {
                Assert.that(Klass.REFERENCE.isAssignableFrom(type));
                return Klass.REFERENCE;
            }
        }
    }

    /**
     * Gets the type of a local variable that is used to describe a 32 bit slot
     * in a parameter list. This method partitions all classes into one of the
     * following categories:
     * <p><blockquote><pre>
     *
     *     Local Variable Type  |  Types
     *     ---------------------+-------
     *     INT                  | boolean, byte, short, int
     *     FLOAT                | float
     *     LONG                 | long
     *     LONG2                | long2
     *     DOUBLE               | double
     *     DOUBLE2              | double2
     *     ADDRESS              | Address
     *     WORD                 | Word
     *     REFERENCE            | types in java.lang.Object hierarchy
     *
     * </pre></blockquote><p>
     *
     * @param   type   the type of a value that will be stored in a local variable
     * @return  the local variable type for storing values of type <code>type</code>
     */
    private Klass getParameterTypeFor(Klass type) {
        if (type == Klass.LONG2 || type == Klass.DOUBLE2) {
            return type;
        }
        return getLocalTypeFor(type);
    }

    /**
     * Get a <code>Local</code> instance to represent a value of a given type
     * that will be stored/loaded to/from a given local variable.
     *
     * @param   type     the type of the value
     * @param   index    the index of the local variable
     * @return  the variable at index <code>index</code> in which values of type <code>type</code> are stored
     */
    public Local allocateLocal(Klass type, int index) {
        return allocateLocalPrim(type, index, false);
    }

    /**
     * Get a <code>Local</code> instance to represent a value of a given type
     * that will be stored/loaded to/from a given local variable.
     *
     * @param   type     the type of the value
     * @param   index    the index of the local variable
     * @return  the variable at index <code>index</code> in which values of type <code>type</code> are stored
     */
    private Local allocateParameter(Klass type, int index) {
        return allocateLocalPrim(type, index, true);
    }

    /**
     * Creates and returns the detailed error message when a local variable is used as a Squawk
     * primitive as well as some value not of exactly the same type. The message includes
     * information derived from the LocalVariableTable attribute so that the source code
     * can be easily changed.
     *
     * @param index   the local variable index that is (mis)used
     * @return the detailed error message
     */
    private String getBadAddressLocalVariableMessage(int index) {
        StringBuffer buf = new StringBuffer("local variable " + index + " must not be used for a Squawk primitive " +
                                            "value as it is also used a value of another type");
/*if[SCOPEDLOCALVARIABLES]*/
        Enumeration e = codeParser.getLocalVariableTableEntries();
        if (e != null) {
            buf.append(" (source code usage: ");
            while (e.hasMoreElements()) {
                LocalVariableTableEntry entry = (LocalVariableTableEntry)e.nextElement();
                if (entry.getIndex() == index) {
                    int start = codeParser.getSourceLineNumber(entry.getStart().getBytecodeOffset());
                    int end = codeParser.getSourceLineNumber(entry.getEnd().getBytecodeOffset());
                    buf.append(entry.getType().getName()).append(' ').append(entry.getName()).append(" from line ").
                       append(start).append(" to line ").append(end).append(';');
                }
            }
        }
/*end[SCOPEDLOCALVARIABLES]*/
        return buf.toString();
    }

    /**
     * Get a <code>Local</code> instance to represent a value of a given type
     * that will be stored/loaded to/from a given local variable.
     *
     * @param   type        the type of the value
     * @param   index       the index of the local variable
     * @param   isParameter true if the local is a parameter
     * @return  the variable at index <code>index</code> in which values of type <code>type</code> are stored
     */
    private Local allocateLocalPrim(Klass type, int index, boolean isParameter) {
        Assert.that(localTypes.length < 0xFFFF);
        Assert.that(index >= 0 && index < localTypes.length, "index="+index+" localTypes.length="+localTypes.length);

        Klass localType = getLocalTypeFor(type);

        int key = localType.getClassID();
        if (localType.isSquawkPrimitive()) {
            key = Klass.REFERENCE.getClassID();
        }
        key = key << 16 | index;
        if (localValues == null) {
            localValues = new IntHashtable();
        }
        Local local = (Local)localValues.get(key);
        if (local == null) {
            local = new Local(localType, index, isParameter);
            localValues.put(key, local);
        }


        /*
         * Ensure that the original class file does not use the same local variable
         * for both a Squawk primitive value and any other reference value. This prevents the
         * translator from having to do a complete liveness analysis to de-multiplex
         * such a local variable slot. Such de-multiplexing is required as Squawk primitives
         * are 'magically' translated into integers (or longs on a 64 bit system).
         */
        if (localType.isSquawkPrimitive() || local.getType().isSquawkPrimitive()) {
            if (localType != local.getType()) {
                // Offsets are implemented as Words
//                if (localType.getClassID() + local.getType().getClassID() != CID.UWORD + CID.OFFSET) {
                    throw codeParser.verifyError(getBadAddressLocalVariableMessage(index));
//                }
            }
        }

        //System.out.println("allocated: "+local+" index "+index);
/*if[SCOPEDLOCALVARIABLES]*/
        codeParser.localVariableAllocated(codeParser.getCurrentIP(), local);
/*end[SCOPEDLOCALVARIABLES]*/
        return local;

    }

    /**
     * Determines if a given local variable is initialized.
     *
     * @param   index  the index of the local variable to test
     * @return  true if the local variable at <code>index</code> is initialized
     */
    public boolean isLocalInitialized(int index) {
        Assert.that(index >= 0 && index < localTypes.length);
        return localTypes[index] != Klass.TOP;
    }

    /**
     * Verify that a local variable index for a given type is not out of bounds.
     *
     * @param  type   the type of the local variable at <code>index</code> in
     *                the local variables array
     * @param  index  the index of a local variable
     */
    public void verifyLocalVariableIndex(Klass type, int index) {
        if (index < 0) {
            throw codeParser.verifyError("invalid local variable index");
        }
        if (type.isDoubleWord()) {
            index++;
        }
        if (index >= localTypes.length) {
            throw codeParser.verifyError("invalid local variable index");
        }
    }

    /**
     * Tests two given types to ensure that they are both {@link Klass#isSquawkPrimitive() Squawk primitives}
     * or both not Squawk primitives. If they are both Squawk primitives, then they must be exactly the
     * same type. This enforces the constraint that Squawk primitive values
     * cannot be assigned to or compared with any other type.
     *
     * @param type1  the first type to compare
     * @param type2  the second type to compare
     */
    public void verifyUseOfSquawkPrimitive(Klass type1, Klass type2) {
        if (type1.isSquawkPrimitive() || type2.isSquawkPrimitive()) {
            if (type1 != type2) {
                // Offsets are implemented as Words
//                if (type1.getClassID() + type2.getClassID() != CID.UWORD + CID.OFFSET) {
                    String type = type1.getName();
                    throw codeParser.verifyError(type + " values can only be written to or compared with other " +
                                                 type + "values");
//                }
            }
        }
    }

    /**
     * Gets the currently derived type of a local at a given index.
     *
     * @param   index  the index of the local variable
     * @return  the currently derived type for the local variable at index
     *          <code>index</code>
     */
    public Klass getDerivedLocalTypeAt(int index) {
        Assert.that(index >= 0 && index < localTypes.length);
        return localTypes[index];
    }

    /**
     * Emulates the storing of a value to a local variable.
     *
     * @param index  the index of the local variable being stored to
     * @param type   the type of the value
     * @param local  the variable to which the value is stored
     */
    public void store(int index, Klass type, Local local) {
        Klass localType = local.getType();
        Assert.that(localType.isAssignableFrom(type) || localType == getLocalTypeFor(type));
        verifyLocalVariableIndex(localType, index);
        localTypes[index] = type;
        if (localType.isDoubleWord()) {
            localTypes[index+1] = Klass.getSecondWordType(localType);
        } else {
            verifyUseOfSquawkPrimitive(localType, type);
        }

    }

    /**
     * Emulates loading a value of a given type from a local variable.
     *
     * @param index      the index of the local variable being loaded from
     * @param localType  the expected type of the variable from which the value is loaded
     * @return           the variable from which the value is loaded
     */
    public Local load(int index, Klass localType) {
        verifyLocalVariableIndex(localType, index);
        Klass derivedType = localTypes[index];
        if (!localType.isAssignableFrom(derivedType)) {
            throw codeParser.verifyError("incompatible type in local variable");
        }

        if (localType.isDoubleWord()) {
            Klass secondWordType = Klass.getSecondWordType(localType);
            if (!secondWordType.isAssignableFrom(localTypes[index + 1])) {
                throw codeParser.verifyError("incompatible type in local variable");
            }
        }

        if (derivedType.isSquawkPrimitive()) {
            localType = derivedType;
        }
        return allocateLocal(localType, index);
    }

    /**
     * Verifies that the current state of the local variables array matches
     * the state specified by a stack map entry.
     *
     * @param target  the target encapsulating a stack map entry specifying
     *                what the current state of the local variables array
     *                should be
     * @param replaceWithTarget if true, then the current state of the local
     *                variable array is updated to reflect the state recorded
     *                in the stack map entry
     */
    public void mergeLocals(Target target, boolean replaceWithTarget) {
        Klass[] recordedTypes = target.getLocals();
        if (recordedTypes.length > localTypes.length) {
            throw codeParser.verifyError( "size of recorded and derived local variable array differs");
        }

        /*
         * Check the locals
         */
        for (int i = 0 ; i < recordedTypes.length ; i++) {
            Klass recordedType = recordedTypes[i];
            Klass derivedType  = localTypes[i];
            if (!recordedType.isAssignableFrom(derivedType)) {
                /*
                 * For some reason, the preverifier occasionally generates
                 * stack map entries for local variables even though the
                 * local variable is dead. What's more, in these cases,
                 * it determines that the type resulting from merging an
                 * object type and an interface type is the interface
                 * type. This makes no sense to me, but the case must be
                 * allowed.
                 */
                if (!recordedType.isInterface() || derivedType.isPrimitive()) {
                    throw codeParser.verifyError("invalid type in local variable");
                }
            }
            if (replaceWithTarget) {
                localTypes[i] = recordedType;
            }
        }
    }

    /**
     * Reset the state of the local variable array based on the local variable
     * state recorded for a given target.
     *
     * @param target   the target whose recorded state will be used to
     *                 reset the state of this frame's local variable array
     */
    public void resetLocals(Target target) {
        Klass[] recordedTypes = target.getLocals();
        int i;
        for (i = 0 ; i < recordedTypes.length ; i++) {
            localTypes[i] = recordedTypes[i];
        }
        while (i < localTypes.length) {
            localTypes[i++] = Klass.TOP;
        }

        // There is at least one TCK test that has a stack map in a synchronized method
        // at the entry to some unreachable code: javasoft.sqe.tests.vm.instr.athrow.athrow013.athrow01301m1.athrow01301m1_wrapper
        // In this case, the stack map will be invalid with respect to the synthesized
        // stack map at the entry to the exception handler wrapping the whole method
        // to implement the correct semantics for method synchronization. To make such
        // code pass the translator, the reset state of the locals includes the extra state
        // derived from the synthesized stack map.
        if (finallyTargetForSynchronizedMethod != null) {
            Klass[] types = finallyTargetForSynchronizedMethod.getLocals();
            if (types.length > recordedTypes.length) {
                for (i = recordedTypes.length; i != types.length; ++i) {
                    localTypes[i] = types[i];
                }
            }
        }
    }

    /*---------------------------------------------------------------------------*\
     *                             Operand stack                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * The operand stack.
     */
    private StackProducer[] stack;

    /**
     * The current number of words on the operand stack. Values of type
     * <code>long</code> or <code>double</code> occupy two words.
     */
    private int sp;

    /**
     * The maximum number of words that may be pushed to the operand stack.
     */
    private int maxStack;

    /**
     * Grow the max stack limit if necessary. This is used to convey extra stack usage by the
     * Squawk bytecode(s) that will be emitted for the current JVM bytecode(s).
     *
     * @param   amount  the amount of stack required for the Squawk implementation of the current JVM bytecode
     */
    public void growMaxStack(int amount) {
        Assert.that(amount > 0);
        int growth = (sp + amount) - maxStack;
        if (growth > 0) {
            maxStack += growth;
            ensureStack(maxStack);
        }
    }

    /**
     * Resets the max stack limit back to the value specified in the class file for the current method.
     */
    public void resetMaxStack() {
        maxStack = codeParser.getMaxStack();
    }

    /**
     * Ensure that the operand stack is large enough to meet a given limit. The
     * operand stack is expanded if necessary.
     *
     * @param limit  the minimum size of the operand stack to be guaranteed by this method
     */
    private void ensureStack(int limit) {
        if (limit > stack.length) {
            stack = (StackProducer[])Arrays.copy(stack, 0, new StackProducer[limit], 0, stack.length);
        }
    }

    /**
     * The the maximum number of stack words used.
     *
     * @return the number of words
     */
    public int getMaxStack() {
        return stack.length;
    }

    /**
     * Pushes a value to the operand stack.
     *
     * @param producer the instruction producing the value being pushed
     */
    public void push(StackProducer producer) {
        Klass type = producer.getType();
        Assert.that(type != Klass.VOID);

        /*
         * Check for overflow and push the producer.
         */
        if (sp == maxStack) {
            throw codeParser.verifyError("operand stack overflow");
        }
        stack[sp++] = producer;

        /*
         * For long and double check for overflow and then add a null word to the stack.
         */
        if (type.isDoubleWord()) {
            if (sp == maxStack) {
                throw codeParser.verifyError("operand stack overflow");
            }
            stack[sp++] = null;
        }
    }

    /**
     * Pops a value off the operand stack.
     *
     * @param type the type that the value popped off the operand stack  must be assignable to
     * @return the instruction that produced the popped value
     */
    public StackProducer pop(Klass type) {
        StackProducer producer;
        if (type.isDoubleWord()) {
            if (sp < 2) {
                throw codeParser.verifyError("operand stack underflow");
            }
            if (!isTopDoubleWord()) {
                throw codeParser.verifyError("incompatible type on operand stack "+tosKlassName());
            }
            sp -= 2;
            producer = stack[sp];
        } else {
            if (sp < 1) {
                throw codeParser.verifyError("operand stack underflow");
            }
            if (isTopDoubleWord()) {
                throw codeParser.verifyError("incompatible type on operand stack "+tosKlassName());
            }
            producer = stack[--sp];

            /*
             * The primitive one-word, non-float types are all assignment compatible with each other
             */
            if (type.isPrimitive() && type != Klass.FLOAT) {
                type = Klass.INT;
            }
        }

        Assert.that(producer != null);

        /*
         * Interfaces are treated as java.lang.Object in the verifier.
         */
        if (type.isInterface()) {
            type = Klass.OBJECT;
        }

        /*
         * Verify that the instruction is producing the correct type.
         */
        if (!type.isAssignableFrom(producer.getType())) {
            throw codeParser.verifyError("incompatible type: '"+type+"' is not assignable from '"+producer.getType() + "'");
        }


        return producer;
    }

    /**
     * Gets the number of words currently on the operand stack. Each
     * <code>double</code> or <code>long</code> value on the stack occupies
     * two words.
     *
     * @return the number of words currently on the operand stack
     */
    public int getStackSize() {
        return sp;
    }

    /**
     * Gets the number of values on the stack. If there is at least one
     * <code>double</code> or <code>long</code> value on the stack then
     * this will not be equal to the value returned by {@link #getStackSize()}.
     *
     * @return the number of values on the stack
     */
    private int getStackValueCount() {
        int count = 0;
        for (int i = 0 ; i < sp ; i++) {
            if (stack[i] != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the stack producer that wrote the value at a given index on the
     * operand stack. The <code>index</code> must not indicate a stack slot
     * that corresponds to the second word of a double word value.
     *
     * @param   index  the operand stack index for which the corresponding stack producer is requested
     * @return  the stack producer that wrote the value at index <code>index</code> on the operand stack
     */
    public StackProducer getStackAt(int index) {
        Assert.that(index < sp, "index out of bounds");
        Assert.that(stack[index] != null, "cannot index the second word of a double word value");
        return stack[index];
    }

    /**
     * Merges the current state of the operand stack into the saved state at
     * a control flow target. This method also verifies that the current
     * state of the operand stack matches the expected state of the operand
     * stack at the target as pecified by a stack map entry.
     *
     * @param target the target encapsulting the merged state of the operand stack at a distinct address
     * @param replaceWithTarget if true, then the current state of the operand stack is updated to reflect
     *                          the state recorded in the stack map entry
     */
    public void mergeStack(Target target, boolean replaceWithTarget) {
        Klass[] recordedTypes = target.getStack();

        /*
         * Fail if the map sp is different
         */
        if (recordedTypes.length != getStackValueCount()) {
            throw codeParser.verifyError( "size of recorded and derived stack differs");
        }

        /*
         * Check the stack items
         */
        for (int r = 0, d = 0 ; r < recordedTypes.length ; r++, d++) {
            Klass recordedType = recordedTypes[r];
            Klass derivedType = stack[d].getType();
            if (!recordedType.isAssignableFrom(derivedType)) {
                throw codeParser.verifyError("invalid type on operand stack @ "+d+": expected " + recordedType + ", received " + derivedType);
            }
            if (derivedType.isDoubleWord()) {
                d++;
            }
        }

        /*
         * Merge the instructions on the stack
         */
        target.merge(this);

        if (replaceWithTarget) {
            resetStack(target, false);
        }
    }

    /**
     * Reset the state of the operand stack based on the stack state recorded
     * for a given target.
     *
     * @param target       the target whose recorded state will be used to
     *                     reset the state of this frame's operand stack
     * @param isForCatch   specifies if target corresponds to an exception
     *                     handler entry
     */
    public void resetStack(Target target, boolean isForCatch) {
        Klass[] recordedTypes = target.getStack();
        if (!isForCatch) {
            sp = 0;
            if (recordedTypes.length != 0) {
                StackProducer[] derivedStack = target.getDerivedStack();
                boolean isBackwardBranchTarget = (derivedStack[0] == null);
                if (isBackwardBranchTarget) {
                    for (int i = 0 ; i < recordedTypes.length ; i++) {
                        Klass recordedType = recordedTypes[i];
                        StackMerge merge = new StackMerge(recordedType);
                        push(merge);
                    }
                } else {
                    while (sp != derivedStack.length) {
                        StackProducer producer = derivedStack[sp];
                        stack[sp++] = producer;
                    }
                }
            }
        } else {
            //Assert.that(sp == 0);
            sp = 0;
        }
    }

    /**
     * next ID for a local that is used for spilling.
     */
    private int nextSpillLocalId;

    /**
     * Allocate local that is used for spilling.
     *
     * @param producer the instruction producing the value that needs to be spilled
     * @return a unique local to be used to hold the value
     */
    public Local allocateLocalForSpill(StackProducer producer) {
        Klass type = getLocalTypeFor(producer.getType());
        return new Local(type, --nextSpillLocalId, false);
    }

    /**
     * Spill the result of an instruction.
     *
     * @param producer the instruction producing the value
     */
    public void spill(StackProducer producer) {
        if (!producer.isSpilt()) {
            producer.spill(allocateLocalForSpill(producer));
        }
    }

    /**
     * Spill each item on the operand stack.
     */
    public void spillStack() {
//System.out.println("** spillStack ** --------------------------------------------------------------------------");
        for (int i = 0 ; i < sp ; i++) {
            StackProducer producer = stack[i];
            if (producer != null) {
                spill(producer);
            }
        }
    }

    /**
     * Returns the internal name of the toop type on the stack.
     *
     * @return  the name
     */
    private String tosKlassName() {
        StackProducer top = stack[sp - (isTopDoubleWord() ? 2 : 1)];
        if (top == null) {
            return "null";
        }
        return top.getType().getInternalName();
    }


    /*---------------------------------------------------------------------------*\
     *                           Dup/Swap emulation                              *
    \*---------------------------------------------------------------------------*/

    /**
     * Determines if the value on the top of stack is of type
     * <code>double</code> or <code>long</code>.
     *
     * @return  true if the value on the top of stack is two words in size
     */
    public boolean isTopDoubleWord() {
        return sp > 1 && stack[sp - 1] == null;
    }

    /**
     * Emulate the semantics of the <i>dup...</i> and <i>swap</i> bytecodes.
     *
     * @param  opcode  the opcode of an untyped stack manipulation instruction
     */
    public void doStackOp(int opcode) {
        switch (opcode) {
            case Opcode.opc_dup: {
                StackProducer x1 = pop(Klass.ONE_WORD);
                push(x1);
                push(x1);
                x1.setDuped(this);
                break;
            }
            case Opcode.opc_dup2: {
                if (!isTopDoubleWord()) {
                    /*
                     * Form 1:
                     */
                    StackProducer x1 = pop(Klass.ONE_WORD);
                    StackProducer x2 = pop(Klass.ONE_WORD);
                    push(x2);
                    push(x1);
                    push(x2);
                    push(x1);
                    x1.setDuped(this);
                    x2.setDuped(this);
                } else {
                    /*
                     * Form 2:
                     */
                    StackProducer x1 = pop(Klass.TWO_WORD);
                    push(x1);
                    push(x1);
                    x1.setDuped(this);
                }
                break;
            }
            case Opcode.opc_dup_x1: {
                StackProducer x1 = pop(Klass.ONE_WORD);
                StackProducer x2 = pop(Klass.ONE_WORD);
                push(x1);
                push(x2);
                push(x1);
                x1.setDuped(this);
                x2.setDuped(this);
                break;
            }
            case Opcode.opc_dup_x2: {
                StackProducer x1 = pop(Klass.ONE_WORD);
                if (!isTopDoubleWord()) {
                    /*
                     * Form 1:
                     */
                    StackProducer x2 = pop(Klass.ONE_WORD);
                    StackProducer x3 = pop(Klass.ONE_WORD);
                    push(x1);
                    push(x3);
                    push(x2);
                    push(x1);
                    x1.setDuped(this);
                    x2.setDuped(this);
                    x3.setDuped(this);
                } else {
                    /*
                     * Form 2:
                     */
                    StackProducer x2 = pop(Klass.TWO_WORD);
                    push(x1);
                    push(x2);
                    push(x1);
                    x1.setDuped(this);
                    x2.setDuped(this);
                }
                break;
            }
            case Opcode.opc_dup2_x1: {
                if (!isTopDoubleWord()) {
                    /*
                     * Form 1:
                     */
                    StackProducer x1 = pop(Klass.ONE_WORD);
                    StackProducer x2 = pop(Klass.ONE_WORD);
                    StackProducer x3 = pop(Klass.ONE_WORD);
                    push(x2);
                    push(x1);
                    push(x3);
                    push(x2);
                    push(x1);
                    x1.setDuped(this);
                    x2.setDuped(this);
                    x3.setDuped(this);
                } else {
                    /*
                     * Form 2:
                     */
                    StackProducer x1 = pop(Klass.TWO_WORD);
                    StackProducer x2 = pop(Klass.ONE_WORD);
                    push(x1);
                    push(x2);
                    push(x1);
                    x1.setDuped(this);
                    x2.setDuped(this);
                }
                break;
            }
            case Opcode.opc_dup2_x2: {
                if (!isTopDoubleWord()) {
                    StackProducer x1 = pop(Klass.ONE_WORD);
                    StackProducer x2 = pop(Klass.ONE_WORD);
                    if (!isTopDoubleWord()) {
                        /*
                         * Form 1:
                         */
                        StackProducer x3 = pop(Klass.ONE_WORD);
                        StackProducer x4 = pop(Klass.ONE_WORD);
                        push(x2);
                        push(x1);
                        push(x4);
                        push(x3);
                        push(x2);
                        push(x1);
                        x1.setDuped(this);
                        x2.setDuped(this);
                        x3.setDuped(this);
                        x4.setDuped(this);
                    } else {
                        /*
                         * Form 3:
                         */
                        StackProducer x3 = pop(Klass.TWO_WORD);
                        push(x2);
                        push(x1);
                        push(x3);
                        push(x2);
                        push(x1);
                        x1.setDuped(this);
                        x2.setDuped(this);
                        x3.setDuped(this);
                    }
                } else {
                    StackProducer x1 = pop(Klass.TWO_WORD);
                    if (!isTopDoubleWord()) {
                        /*
                         * Form 2:
                         */
                        StackProducer x2 = pop(Klass.ONE_WORD);
                        StackProducer x3 = pop(Klass.ONE_WORD);
                        push(x1);
                        push(x3);
                        push(x2);
                        push(x1);
                        x1.setDuped(this);
                        x2.setDuped(this);
                        x3.setDuped(this);
                    } else {
                        /*
                         * Form 4:
                         */
                        StackProducer x2 = pop(Klass.TWO_WORD);
                        push(x1);
                        push(x2);
                        push(x1);
                        x1.setDuped(this);
                        x2.setDuped(this);
                    }
                }
                break;
            }
            case Opcode.opc_swap: {
                StackProducer x1 = pop(Klass.ONE_WORD);
                StackProducer x2 = pop(Klass.ONE_WORD);
                push(x1);
                push(x2);
                x1.setDuped(this);
                x2.setDuped(this);
                break;
            }
            default: {
                Assert.that(false, "unknown dup/swap opcode: " + opcode);
            }
        }
    }


    /*---------------------------------------------------------------------------*\
     *                                Tracing                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Traces a type on the operand stack or in a local variable.
     *
     * @param type       the type to trace
     * @param prefix     the prefix to use if <code>isDerived</code> is true
     *                   otherwise a prefix of spaces the same length as
     *                   <code>prefix</code> is used instead
     * @param isDerived  specifies if this a type derived by the verifer or
     *                   is specified by a stack map entry
     */
    private void traceType(Klass type, String prefix, boolean isDerived) {
        if (Klass.DEBUG) {
            if (!isDerived) {
                char[] spaces = new char[prefix.length()];
                Arrays.fill(spaces, ' ');
                Tracer.trace(new String(spaces));
            } else {
                Tracer.trace(prefix);
            }
            String name = (type == null ? "-T-" : type.getInternalName());
            if (isDerived) {
                Tracer.traceln(" "+name);
            } else {
                Tracer.traceln("{"+name+"}");
            }
        }
    }

    /**
     * Traces the state of the operand stack at the current verification
     * address.
     *
     * @param target  the stack map (if any) at the current verification address
     */
    private void traceStack(Target target) {
        if (Klass.DEBUG) {
            Klass[] map = target == null ? Klass.NO_CLASSES : target.getStack();
            int r = 0;  // index into recorded stack (i.e. from stack map)
            int d = 0;  // index into derived stack
            while (r < map.length || d < sp) {
                Klass derived  = (d < sp)         ? stack[d].getType() : null;
                Klass recorded = (r < map.length) ? map[r]             : null;
                String prefix = "  stack["+r+"]: ";
                traceType(derived, prefix, true);
                if (recorded != null) {
                    traceType(recorded, prefix, false);
                }
                r++;
                d += (derived != null && derived.isDoubleWord()) ? 2 : 1;
            }

        }
    }

    /**
     * Traces the state of the local variables at the current verification address.
     *
     * @param target  the stack map (if any) at the current verification address
     */
    private void traceLocals(Target target) {
        if (Klass.DEBUG) {
            Klass[] map = (target == null) ? Klass.NO_CLASSES : target.getLocals();
            int i = 0;
            int l = 0;
            while (i < map.length || l < localTypes.length) {
                Klass derived  = (l < localTypes.length) ? localTypes[l] : null;
                Klass recorded = (i < map.length)        ? map[i]        : null;
                String prefix  = "  local["+l+"]: ";
                traceType(derived, prefix, true);
                if (recorded != null) {
                    traceType(recorded, prefix, false);
                }
                i++;
                l++;
            }
        }
    }

    /**
     * Traces the frame state at the current verification address.
     *
     * @param  opcode   the opcode of the instruction at <code>address</code>
     * @param  address  the current verification address
     */
    public void traceFrameState(int opcode, int address) {
        /*
         * Trace the recorded and derived types
         */
        if (Klass.DEBUG) {
            Target target = null;
            try {
                target = codeParser.getTarget(address);
            } catch (LinkageError e) {
                /* Just means there is no stack map at this address */
            }
            Tracer.traceln("Frame state @ "+address+" [ "+ Opcode.mnemonics[opcode]+" ]");
            traceLocals(target);
            traceStack(target);
        }
    }
}
