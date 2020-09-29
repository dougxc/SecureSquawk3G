/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.compiler.Compiler;


/**
 * Symbolic labels to be placed on the shadow stack (to be
 * fixed at relocation time)
 *
 * @author  Cristina Cifuentes
 */

class SymbolicLabel extends SymbolicValueDescriptor implements ShadowStackConstants{

    /* Symbol to be fixed up */
    private Label label;

    /**
     * Constructor
     */
    public SymbolicLabel(Label label) {
        this.label = label;
    }

    public int getSymbolicValueDescriptor() {
        return S_LABEL;
    }

    public Type getType() {
        return Type.INT;   /*** is this right? ***/
    }

    public Label getLabel() {
        return label;
    }

    /**
     * Prints information about this symbolic label.
     * This method is used for debugging purposes.
     */
    public void print() {
        System.err.print("SymbolicLabel.  Label = ");
        ((XLabel)label).print();
    }

}

