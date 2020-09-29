/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

public class FinalizerTest extends FinalizerTestParent {

    public static void main(String[] args) {
        System.out.println("Running: java.lang.FinalizerTest");
        System.gc();
        createObjects();
        while (count > 0) {
            System.out.println("Calling gc()");
            System.gc();
            Thread.yield();
        }
        System.out.println("FinalizerTest done");
    }

    static void createObjects() {
        System.out.println("Creating "+new FinalizerTest());
        System.out.println("Creating "+new FinalizerTest());
        System.out.println("Creating "+new FinalizerTest());
        System.out.println("Creating "+new FinalizerTest());
        System.out.println("Creating "+new FinalizerTest());
    }
}


/*
 * Put finalizer in parent class to test that the finalize() method is inherited correctly.
 */
class FinalizerTestParent {
    static int count = 5;
    protected void finalize() {
        System.out.println("finalize() called for "+this);
        --count;
    }
}
