/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.util;

/**
 * Provides support for assertions that can be removed on demand in order for
 * building a release version.
 *
 * @author   Thomas Kotzmann
 * @author   Doug Simon (modified for Squawk)
 * @version  1.00
 */
public class Assert {

    /**
     * Whether assertions are included in the bytecodes or not.
     */
    public static final boolean ASSERTS_ENABLED = /*VAL*/true/*J2ME.DEBUG*/;

    /**
     * Flag to always enable shouldNotReachHere().
     */
    public static final boolean SHOULD_NOT_REACH_HERE_ALWAYS_ENABLED = true;

    /**
     * Don't let anyone instantiate this class.
     */
    private Assert() {}

    /**
     * Asserts that the specified condition is true. If the condition is false,
     * a RuntimeException is thrown with the specified message.
     *
     * @param   cond  condition to be tested
     * @param   msg   message that explains the failure
     */
    public static void that(boolean cond, String msg) {
        if (!cond) {
            System.err.flush();
            System.out.flush();
            throw new RuntimeException("Assertion failed: " + msg);
        }
    }

    /**
     * Asserts that the specified condition is true. If the condition is false,
     * a RuntimeException is thrown.
     *
     * @param   cond  condition to be tested
     */
    public static void that(boolean cond) {
        if (!cond) {
            throw new RuntimeException("Assertion failed");
        }
    }

    /**
     * Asserts that the compiler should never reach this point.
     *
     * @param   msg   message that explains the failure
     * @return a null RuntimeException so that constructions such
     *         as <code>throw Assert.shouldNotReachHere()</code> will
     *         be legal and thus avoid the need to return meaningless
     *         values from functions that have failed.
     */
    public static RuntimeException shouldNotReachHere(String msg) {
        throw new RuntimeException("Assertion failed: should not reach here: " + msg);
    }

    /**
     * Asserts that the compiler should never reach this point.
     *
     * @return a null RuntimeException so that constructions such
     *         as <code>throw Assert.shouldNotReachHere()</code> will
     *         be legal and thus avoid the need to return meaningless
     *         values from functions that have failed.
     */
    public static RuntimeException shouldNotReachHere() {
        throw new RuntimeException("Assertion failed: should not reach here");
    }


    /*---------------------------------------------------------------------------*\
     *                      Fatal versions of the above methods                  *
    \*---------------------------------------------------------------------------*/

    /**
     * Asserts that the specified condition is true. If the condition is false
     * the specified message is displayed and the VM is halted.
     *
     * @param   cond  condition to be tested
     * @param   msg   message that explains the failure
     */
    public static void thatFatal(boolean cond, String msg) {
        if (!cond) {
            VM.print("Assertion failed: ");
            VM.println(msg);
            VM.fatalVMError();
        }
    }

    /**
     * Asserts that the specified condition is true. If the condition is false
     * the VM is halted.
     *
     * @param   cond  condition to be tested
     */
    public static void thatFatal(boolean cond) {
        if (!cond) {
            VM.println("Assertion failed");
            VM.fatalVMError();
        }
    }

    /**
     * Asserts that the compiler should never reach this point.
     *
     * @param   msg   message that explains the failure
     * @return a null RuntimeException so that constructions such
     *         as <code>throw Assert.shouldNotReachHere()</code> will
     *         be legal and thus avoid the need to return meaningless
     *         values from functions that have failed.
     */
    public static RuntimeException shouldNotReachHereFatal(String msg) {
        VM.print("Assertion failed: should not reach here: ");
        VM.println(msg);
        VM.fatalVMError();
        return null;
    }

    /**
     * Asserts that the compiler should never reach this point.
     *
     * @return a null RuntimeException so that constructions such
     *         as <code>throw Assert.shouldNotReachHere()</code> will
     *         be legal and thus avoid the need to return meaningless
     *         values from functions that have failed.
     */
    public static RuntimeException shouldNotReachHereFatal() {
        VM.println("Assertion failed: should not reach here");
        VM.fatalVMError();
        return null;
    }

    /*---------------------------------------------------------------------------*\
     *        Fatal VM assertions that won't be removed by the pre-processor     *
    \*---------------------------------------------------------------------------*/

    /**
     * Asserts that the specified condition is true. If the condition is false
     * the specified message is displayed and the VM is halted.
     *
     * Calls to this method are never removed by the Squawk pre-processor and as
     * such should only be placed in frequent execution paths absolutely necessary.
     *
     * @param   cond  condition to be tested
     * @param   msg   message that explains the failure
     */
    public static void always(boolean cond, String msg) {
        if (!cond) {
            VM.print("Assertion failed: ");
            VM.println(msg);
            VM.fatalVMError();
        }
    }

    /**
     * Asserts that the specified condition is true. If the condition is false
     * the VM is halted.
     *
     * Calls to this method are never removed by the Squawk pre-processor and as
     * such should only be placed in frequent execution paths absolutely necessary.
     *
     * @param   cond  condition to be tested
     */
    public static void always(boolean cond) {
        if (!cond) {
            VM.println("Assertion failed");
            VM.fatalVMError();
        }
    }
}

