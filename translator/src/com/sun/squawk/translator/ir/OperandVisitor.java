/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.ir.instr.StackProducer;

/**
 * A visitor for manipulating and transforming the operands for
 * instructions.
 *
 * @author  Doug Simon
 */
public interface OperandVisitor {

    /**
     * Visits a single given operand of a given instruction.
     *
     * @param   instruction  the instruction being visited
     * @param   operand      an operand of <code>instruction</code>
     * @return  the value of the operand which may be different from the original value
     */
    public StackProducer doOperand(Instruction instruction, StackProducer operand);

}