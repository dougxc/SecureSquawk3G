/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;


/**
 * Symbolic literal values of physical size 32-bits to be placed on the shadow stack
 *
 * @author  Cristina Cifuentes
 */

class SymbolicLiteral32 extends SymbolicLiteral implements ShadowStackConstants{

    /**
     * Integral literal values are of size <= 32-bits (based on their type).
     * They are represented by a field of size 32 bits.
     */
    private int literal;

    /**
     * Constructor
     *
     * @param lit  the literal value
     */
    public SymbolicLiteral32(int lit, Type type) {
        literal = lit;
        this.type = type;
    }

    public boolean isTypeEquivalent(Type type) {
        if ((type == Type.INT) || (type == Type.REF) || (type == Type.OOP) ||
           (type == Type.BYTE) || (type == Type.UBYTE) || (type == Type.SHORT) ||
           (type == Type.USHORT)) {
            return true;
        }
        return false;
    }

    /**
     * Return the literal value
     *
     * @return  the literal value
     */
    public int getLiteral() {
        return literal;
    }

    public int getLiteralSize() {
        return 32;
    }

    public void print() {
        System.err.print("SymbolicLiteral32.  Value = " + literal + ", type = ");
        type.print();
    }

}

