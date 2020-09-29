/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

import com.sun.squawk.util.Assert;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.ir.instr.*;

/**
 * An instance of <code>LocalVariableTableEntry</code> represents a single entry
 * in a "LocalVariableTable" class file attribute.
 *
 * @see   <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#5956">
 *         The Java Virtual Machine Specification - Second Edition</a>
 */
public class LocalVariableTableEntry {

    /**
     * The position in the instruction sequence at which the scope of the
     * variable starts.
     */
    private final Position start;

    /**
     * The position in the instruction sequence at which the scope of the
     * variable ends.
     */
    private final Position end;

    /**
     * The name of the local variable.
     */
    private final String name;

    /**
     * The type of the local variable.
     */
    private final Klass type;

    /**
     * The index of the local variable in the local variable array for the
     * associated method.
     */
    private final int index;

    /**
     * The IR representation of the local variable.
     */
    private Local local;

    /**
     * Creates a new <code>LocalVariableTableEntry</code> instance.
     *
     * @param  start    the position in the instruction sequence at which the
     *                  scope of the variable starts
     * @param  end      the position in the instruction sequence at which the
     *                  scope of the variable ends
     * @param  name     the name of the variable
     * @param  type     the type of the variable
     * @param  index    the index of the local variable
     */
    LocalVariableTableEntry(Position start, Position end, String name, Klass type, int index) {
        this.start = start;
        this.end   = end;
        this.name  = name;
        this.type  = type;
        this.index = index;
    }

    /**
     * Gets the name of the local variable represented by this entry.
     *
     * @return the name of the local variable represented by this entry
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the type of the local variable represented by this entry.
     *
     * @return the type of the local variable represented by this entry
     */
    public Klass getType() {
        return type;
    }

    /**
     * Sets the <code>Local</code> instance corresponding to the variable
     * described by this entry.
     *
     * @param local  the <code>Local</code> instance corresponding this variable
     */
    public void setLocal(Local local) {
        if (this.local != null && this.local != local) {
            Assert.shouldNotReachHere("cannot overwrite local");
        }
        this.local = local;
    }

    /**
     * Gets the local variable index of this local variable. If the enclosing
     * bytecode array has not yet been transformed, then this will be the index
     * in terms of the original JVM bytecodes otherwise it will be the index in
     * terms of the Squawk bytecodes.
     *
     * @return  the index of the local variable
     */
    public int getIndex() {
        if (local != null) {
            return local.getJavacIndex();
        } else {
            return index;
        }
    }

    /**
     * Gets the position in the instruction sequence at which the scope of the
     * variable starts.
     *
     * @return  the starting position of this local's scope
     */
    public Position getStart() {
        return start;
    }

    /**
     * Gets the position in the instruction sequence at which the scope of the
     * variable ends.
     *
     * @return  the ending position of this local's scope
     */
    public Position getEnd() {
        return end;
    }

    /**
     * Determines if a given variable index and current instruction address
     * corresponding with this local variable.
     *
     * @param  index    a local variable index
     * @param  address  an instruction address
     * @return  true if <code>index</code> equals the index of this local
     *                  variable and <code>address</code> falls within the
     *                  range of code covered by this local variable
     */
    public boolean matches(int index, int address) {
        return this.index == index && this.start.getBytecodeOffset() <= address && address < this.end.getBytecodeOffset();
    }
}
