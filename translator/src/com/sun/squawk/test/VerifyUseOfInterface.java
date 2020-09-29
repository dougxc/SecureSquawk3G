/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.test;

/**
 * This tests that the translator correctly handles the case of
 * assignment to an interface type where the derived type does
 * not directly implement the interface type.
 */
public class VerifyUseOfInterface extends Base {

    private static Interface sIface;
    private Interface vIface;

    private void doIt() {
    }

    static void main(String[] args) {

        // call private method with null receiver
        VerifyUseOfInterface vuoi = null;
        try {
            vuoi.doIt();
            throw new RuntimeException("Expected NullPointerException");
        } catch (NullPointerException npe) {
            // ok
            vuoi = new VerifyUseOfInterface();
        }

        Interface iface = new SubClass();
        // derived type of 'iface' is now 'SubClass'

        if (args == null) {
            iface = new AnotherSubClass();
            // derived type of 'iface' on this branch is now 'AnotherSubClass'
        }

        // at this merge point, derived type of 'iface' is now Base which does
        // not directly imnplement 'Interface'

        // translator/verifier should treat derived type of 'iface' as
        // 'java.lang.Object' for the following statements
        vuoi.vIface = iface;
        sIface = iface;
        int it = iface.getIt();
        iface = null;
        useInterface(iface);
    }

    static void useInterface(Interface iface) {
    }
}

interface Interface {
    int getIt();
}

class Base {
    public Base() {
    }
}

class SubClass extends Base implements Interface {
    public int getIt() {
        return 1;
    }
}

class AnotherSubClass extends Base implements Interface {
    public int getIt() {
        return 1;
    }
}
