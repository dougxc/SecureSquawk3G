/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir;

/**
 * This is the root of the instruction hierarchy.
 *
 * @author  Doug Simon
 */
public abstract class Instruction {

    /**
     * The value of an undefined offset.
     */
    final static int OFFSETNOTDEFINED = -99999;

    /**
     * Offset in bytes to the start of the instruction in the method. This field
     * starts life as the offset into the Java bytecodes and is used later as the
     * offset into the Squawk bytecodes.
     */
    private int offset = OFFSETNOTDEFINED;

    /**
     * Next instruction in a linked list of instructions. This field is
     * directly manipulated by an {@link IR} instance.
     */
    Instruction next;

    /**
     * Previous instruction in a linked list of instructions. This field is
     * directly manipulated by an {@link IR} instance.
     */
    Instruction previous;

    /**
     * Creates an instruction.
     */
    protected Instruction() {
    }

    /**
     * Gets the instruction immediately after this one in an
     * instruction sequence. The value <code>null</code> will be returned if
     * this is the last instruction in the sequence.
     *
     * @return  the instruction immediately after this one in an
     *          instruction sequence
     */
    public Instruction getNext() {
        return next;
    }

    /**
     * Gets the instruction immediately before this one in an
     * instruction sequence. The value <code>null</code> will be returned if
     * this is the first instruction in the sequence.
     *
     * @return  the instruction immediately before this one in an
     *          instruction sequence
     */
    public Instruction getPrevious() {
        return previous;
    }

    /**
     * Sets the offset in bytes from the start of the machine code sequence.
     *
     * @param offset the machine code offset
     */
    public void setBytecodeOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Gets the offset in bytes from the start of the mechine code sequence.
     *
     * @return  the machine code offset
     */
    public int getBytecodeOffset() {
        return offset;
    }

    /**
     * Gets the size of the machine code sequence.
     *
     * @return the machine code size
     */
    public int getSize() {
        return 0;
    }

    /**
     * Calculate the size of the machine code sequence.
     *
     * @return the machine code size
     */
    public int calculateSize() {
        return getSize();
    }

    /**
     * Get a constant object used by the instruction.
     *
     * @return the object or null if there is none
     */
    public Object getConstantObject() {
        return null;
    }

    /**
     * Determines if this instruction will (potentially) result in a call to
     * Java code or is a backward branch.
     *
     * @return  true if this instruction will (potentially) result in a call
     *          to Java code or is a backward branch.
     */
    public boolean constrainsStack() {
        return false;
    }

    /**
     * Determines if execution of this instruction may cause a garbage collection.
     * This will be true if the instruction is an invoke or any other instruction
     * that results in a call to Java code as any call may require the stack to be
     * extended which in turn requires allocation of memory.
     *
     * @param isStatic true if the instruction is in a static method
     * @return true if this instruction may cauise a garbage collection
     */
    public boolean mayCauseGC(boolean isStatic) {
        return constrainsStack();
    }

    /**
     * Entry point for an InstructionVisitor.
     *
     * @param visitor  the InstructionVisitor object
     */
    public abstract void visit(InstructionVisitor visitor);

    /**
     * Entry point for an OperandVisitor. The operands must be visited in the
     * order that they were pushed onto the stack.
     *
     * @param visitor  the OperandVisitor object
     */
    public void visit(OperandVisitor visitor) {}

}
