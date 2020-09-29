/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import java.io.PrintStream;
import com.sun.squawk.compiler.asm.x86.*;

/**
 * Class representing a label in the compiler interface.
 *
 * @author   Cristina Cifuentes
 */

public class XLabel implements Label {

    /**
     * The assembler label.
     */
    private ALabel asmLabel;

    /**
     * Constructor
     *
     * @param asm the assembler that allocates this label.
     */
    public XLabel(Assembler asm) {
        asmLabel = new ALabel(asm);
    }

    /**
     * Test to see if the label has been bound.
     *
     * @return true if it is
     */
    public boolean isBound() {
        return asmLabel.isBound();
    }

    /**
     * Get the offset to the label in the code buffer.
     *
     * @return the offset in bytes
     */
    public int getOffset() {
        return asmLabel.getPos();
    }

    public ALabel getAssemblerLabel(){
        return asmLabel;
    }

    /**
     * Prints information about this label.
     * This method is used for debugging purposes.
     */
    public void print() {
        asmLabel.print();
    }
}
