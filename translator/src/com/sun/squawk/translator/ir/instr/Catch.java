/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir.instr;

import com.sun.squawk.util.Assert;
import com.sun.squawk.translator.ir.*;

/**
 * An instance of <code>Catch</code> is a pseudo instruction that
 * represents the position in an IR at which an exception handler starts.
 * <p>
 *
 * @author  Doug Simon
 */
public class Catch extends StackProducer implements TargetedInstruction {

    /**
     * The targeted address represented by this object.
     */
    private final Target target;

    /**
     * Creates a <code>Catch</code> instance representing the entry point
     * to a handler that catches exceptions of a given type.
     * <p>
     * Section 4.9.5 of the JVMS says that exception handlers may not be
     * entered by any other mechanism other than an exception being thrown.
     * However it then goes on to say that the verifier does not have to
     * check for this and so we have TCK tests that contain gotos to
     * handlers that are never actually executed. This poses a problem
     * for the Squawk bytecode verifier when stack items have been spilled
     * prior to code flow into the handler. To solve this IR for Catch is
     * set always to 'constrainsStack()' so that all stack input is always
     * spilled and then the code InstructionEmitter will output a CONST_NULL
     * instruction at the start of the handler and the address of the handler
     * in the exception table is incremented by one byte to avoid this
     * instruction.
     *
     * @param type  the type of the exceptions caught by the exception handler
     */
    public Catch(Klass type, Target target) {
        super(type);
        Assert.that(getType() != null);
        this.target = target;
        target.setTargetedCatchInstruction(this);
    }

    /**
     * Gets the address of the first real instruction that is used for exception
     * flow targets to the handler.
     *
     * @return the address
     */
    public int getExceptionBytecodeOffset() {
        return getBytecodeOffset();
    }

    /**
     * Tests if the handler is also a control flow target.
     *
     * @return true if it is
     */
    public boolean isControlFlowTarget() {
        return target.isBackwardBranchTarget() || target.isForwardBranchTarget();
    }

    /**
     * {@inheritDoc}
     */
    public Target getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean constrainsStack() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doCatch(this);
    }
}
