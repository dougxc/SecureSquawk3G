/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package java.lang;

import com.sun.squawk.util.Vector;
import java.io.PrintStream;

/**
 * This interface is implemented to desribe the (sub)set of tests
 * in a given Technology Conformace Kit (TCK).
 */
public interface TCK {

    /**
     * Add all the classes in the set (or subset) to a given vector.
     *
     * @param classes the vector to which the classes should be added
     * @param out     print stream on which to print the command line arguments
     *                that are passed to the Squawk VM to run all the tests in
     *                the TCK
     * @param century specifies which range of 100 tests should be added. The
     *                value -1 denotes that all tests should be added.
     */
    void addClasses(Vector classes, int century, PrintStream out);

}
