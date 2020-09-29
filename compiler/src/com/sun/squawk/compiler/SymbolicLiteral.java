/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;


/**
 * Symbolic literal values to be placed on the shadow stack
 *
 * @author  Cristina Cifuentes
 */

abstract class SymbolicLiteral extends SymbolicValueDescriptor implements ShadowStackConstants{

    /**
     * Holds the type of the literal.  It can be a primary or secondary type.
     *
     * For 32-bit literals, valid types are: INT, UINT, REF, OOP, BYTE, UBYTE, SHORT, USHORT
     * For 64-bit literals, valid types are: LONG, REF, OOP
     */
    Type type;

    /**
     * Constructor
     */
    public SymbolicLiteral() {}

    public int getSymbolicValueDescriptor() {
        return S_LIT;
    }

    public int getLiteralSize() {
        return 0;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type newType) {
        type = newType;
    }

    public void print() {
        System.err.println("Symbolic literal - abstract");
    }
}

