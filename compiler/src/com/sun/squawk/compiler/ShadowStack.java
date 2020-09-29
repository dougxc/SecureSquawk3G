/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler;

import com.sun.squawk.util.Stack;


/**
 * <p>Title: ShadowStack</p>
 * <p>Description: Stack representation for the compiler's shadow stack.  This
 *                 version includes debugging information, which is accessible
 *                 via the command-line option "-Dsquawk.architecture=<arch>Debug".</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: Sun Microsystems</p>
 * @author Cristina Cifuentes
 * @version 1.0
 */

class ShadowStack extends Stack {

    /**
     * Keeps track of whether to run a debugging version of the compiler or not.
     */
    boolean debug;


    /**
     * Constructor.
     *
     * @param debug the boolean that controls running the debugging version of the compiler.
     */
    public ShadowStack(boolean debug) {
        super();
        this.debug = debug;
    }

    public void push(SymbolicValueDescriptor obj) {
        super.push(obj);

        /**
         * Debugging information
         */
        if (debug) {
            System.err.print("\tPushed ");
            obj.print();
            System.err.println();
        }
    }

    public Object pop() {
        Object obj = super.pop();

        /**
         * Debugging information
         */
        if (debug) {
            System.err.print("\tPopped ");
            ( (SymbolicValueDescriptor) obj).print();
            System.err.println();
        }

        return obj;
    }

    public void removeAllElements() {
        /**
         * Debugging information
         */
        if (debug) {
            for (int i = this.size(); i > 0; i--) {
                Object obj = this.elementAt(i-1);
                System.err.print("\tPopped ");
                ((SymbolicValueDescriptor)obj).print();
                System.err.println();
            }
        }

        super.removeAllElements();
    }

    public ShadowStack copy() {
        ShadowStack stackcp = new ShadowStack(debug);
        for (int i = 0 ; i < this.size() ; i++) {
            stackcp.push(this.elementAt(i));
        }
        return stackcp;
    }

}