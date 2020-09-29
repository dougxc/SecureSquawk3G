/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import java.io.PrintStream;
import com.sun.squawk.util.Assert;
import com.sun.squawk.compiler.cgen.*;
import com.sun.squawk.util.Stack; // Version without synchronization


/**
 * Class representing a label in the compiler interface.
 *
 * @author   Nik Shaylor
 */
abstract public class XLabel implements Label {

    /**
     * The next available label ID.
     */
    private static int nextid;

    /**
     * The sentinel value for an unbound label.
     */
    private static Scope SCOPENOTSET = new Scope(null);

    /**
     * The label ID.
     */
    private int id = nextid++;

    /**
     * The scope the label is bound to
     */
    private Scope scope = SCOPENOTSET;

    /**
     * Number of elements on the evaluation stack.
     */
    private int stackSize = -1;

    /**
     * List of StackMerge objects
     */
    private Stack mergers;

    /**
     * Bind the label to the current address.
     *
     * @see #getScope
     * @param scope the scope the label is bound to
     */
    public void setScope(Scope scope) {
        this.scope = scope;
        if (stackSize == -1) {
            stackSize = 0;
        }
    }

    /**
     * Test to see if the label has been bound.
     *
     * @return true if it is
     */
    public boolean isBound() {
        return scope != SCOPENOTSET;
    }

    /**
     * Return the scope the label was bound to.
     *
     * @see #setScope
     * @return the scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Return the label ID.
     *
     * @return the ID
     */
    public int getId() {
        return id;
    }

    /**
     * Check to see if the supplied scope is in the scope of the label.
     *
     * @param scope the scope to check
     */
    public void checkScope(Scope scope) {
        Scope s = this.scope;
        while (s != null) {
            if (s == scope) {
                return;
            }
            s = s.getPrevious();
        }
        throw new RuntimeException("branch to label out of scope");
    }

    /**
     * Merge the instructions on the stack.
     *
     * @param stack a stack of instrutions
     */
    public void merge(Stack stack) {
        int size = stack.size();

//System.out.println("merge size="+size);

        if (stackSize == -1) {
            stackSize = size;
            if (size > 0) {
                mergers = new Stack();
            }
            for (int i = 0 ; i < size ; i++) {

//System.out.println("merge create "+i);

                Instruction inst = (Instruction)stack.elementAt(i);
                StackMerge  merg = new StackMerge(inst.type());
                mergers.addElement(merg);
                merg.addProducer(inst);
            }
        } else {
            Assert.that(size == stackSize, "Stack size different at label");
            for (int i = 0 ; i < size ; i++) {

//System.out.println("merge add "+i);

                Instruction inst = (Instruction)stack.elementAt(i);
                StackMerge  merg = (StackMerge)mergers.elementAt(i);
                Assert.that(inst.type() == merg.type(), "Stack type different at label");
                merg.addProducer(inst);
            }
        }
    }

    /**
     * Return the size of the evaluation stack at the label.
     *
     * @return the size
     */
    public int getStackSize() {
        if (stackSize == -1) {
            stackSize = 0;
        }
        return stackSize;
    }

    /**
     * Return a stack merge instruction.
     *
     * @param i the position in the stack
     * @return the stack merge instruction
     */
    public Instruction getStackAt(int i) {
        return (StackMerge)mergers.elementAt(i);
    }

    /**
     * Get the offset to the label in the code buffer.
     *
     * @return the offset in bytes
     */
    abstract public int getOffset();

    /**
     * Print the IR.
     *
     * @param out the output stream
     */
    abstract public void print(PrintStream out);

}
