//if[J2ME.DEBUG]
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.util;

import java.util.Enumeration;
import java.io.PrintStream;
import com.sun.squawk.util.Stack;     // Version without synchronization
import com.sun.squawk.util.Hashtable; // Version without synchronization


/**
 * This is a singleton class that provides support for timing computations,
 * including nested computations.<p>
 *
 * It is intended for usage of this class to be statically conditional. That is,
 * it need not exist in a production deployment. As such, all calls/references
 * to this class should be wrapped in a conditional test of the final static
 * boolean variable {@link Klass#DEBUG}. For example:<p>
 *
 * <p><hr><blockquote><pre>
 *     if (Klass.DEBUG) {
 *         ComputationTimer.time("loading", new ComputationTimer.Computation() {
 *             public Object run() {
 *                 load();
 *             }
 *         });
 *     } else {
 *         load();
 *     }
 * </pre></blockquote><hr><p>
 *
 * If the value of <code>Klass.DEBUG</code> was statically
 * determined to be false by javac, then this whole block of code would be
 * omitted from the compiled class.
 */
public class ComputationTimer {

    /**
     * A computation to be timed that does not throw a checked exception.
     * The computation is performed by invoking
     * Timer.time on the ComputationTimer.Computation object.
     */
    public static interface Computation {
        /**
         * Performs the computation that will be timed.
         *
         * @return   a context dependent value that may represent the result of
         *           the computation.
         */
        public Object run();
    }

    /**
     * A computation to be timed that throws a checked exception.
     * The computation is performed by invoking
     * Timer.time on the ComputationTimer.Computation object.
     */
    public static interface ComputationException {
        /**
         * Performs the computation that will be timed.
         *
         * @return   a context dependent value that may represent the result of
         *           the computation.
         */
        public Object run() throws Exception;
    }

    /**
     * An instance of <code>Execution</code> encapsulates the state of a
     * computation including its duration, result and any exception thrown.
     */
    static class Execution {
        /**
         * The duration of the computation inclusing any nested compuatutions.
         */
        long nestedTimes;

        /**
         * The result of the computation.
         */
        Object result;

        /**
         * The exception (if any) thrown by the computation.
         */
        Exception exception;
    }

    /**
     * A global flag that can be used to determine whether or not
     * tasks should be timed.
     */
    public static boolean enabled;

    /**
     * The collected flat times.
     */
    private static Hashtable flatTimes = new Hashtable();

    /**
     * The collected nested times.
     */
    private static Hashtable totalTimes = new Hashtable();

    /**
     * A stack to model the nesting of computations.
     */
    private static Stack executions = new Stack();

    /**
     * The time of system start up.
     */
    private static final long startUp = System.currentTimeMillis();

    /**
     * Execution a computation.
     *
     * @param   id  the identifier of the computation
     * @param   c   the <code>Computation</code> or
     *              <code>ComputationException</code> instance representing
     *              the computation to be executed
     * @return  the dynamic state of the computation's execution
     */
    private static Execution execute(String id, Object c) {
        long start = System.currentTimeMillis();
        Execution e = new Execution();
        executions.push(e);
        Long currentTotal = (Long)totalTimes.get(id);
        try {
            if (c instanceof Computation) {
                e.result = ((Computation)c).run();
            } else {
                e.result = ((ComputationException)c).run();
            }
        } catch (Exception ex) {
            e.exception = ex;
        } finally {
            executions.pop();
            long time = System.currentTimeMillis() - start;
            if (!executions.isEmpty()) {
                ((Execution)executions.peek()).nestedTimes += time;
            }
            totalTimes.put(id, new Long(time+(currentTotal == null ? 0L : currentTotal.longValue())));

            Long flatTime = (Long)flatTimes.get(id);
            if (flatTime == null) {
                flatTimes.put(id, new Long(time - e.nestedTimes));
            } else {
                flatTimes.put(id, new Long(flatTime.longValue() + (time - e.nestedTimes)));
            }
        }
        return e;
    }

    /**
     * Time a specified computation denoted by a specified identifier. The
     * time taken to perform the computation is added to the accumulative
     * time to perform all computations with the same identifier.
     *
     * @param   id           the identifier for the computation
     * @param   computation  the computation to be performed and timed
     * @return  the result of the computation
     */
    public static Object time(String id, Computation computation) {
        Execution e = execute(id, computation);
        if (e.exception != null) {
            Assert.that(e.exception instanceof RuntimeException);
            throw (RuntimeException)e.exception;
        }
        return e.result;
    }

    /**
     * Time a specified computation denoted by a specified identifier. The
     * time taken to perform the computation is added to the accumulative
     * time to perform all computations with the same identifier.
     *
     * @param   id           the identifier for the computation
     * @param   computation  the computation to be performed and timed
     * @return  the result of the computation.
     */
    public static Object time(String id, ComputationException computation) throws Exception {
        Execution e = execute(id, computation);
        if (e.exception != null) {
            throw e.exception;
        }
        return e.result;
    }

    /**
     * Gets an enumeration over the identifiers of computations for which
     * times were collected.
     *
     * @return  an enumeration over the identifiers of computations for which
     *          times were collected
     */
    public static Enumeration getComputations() {
        return flatTimes.keys();
    }

    /**
     * Gets an enumeration over the collected flat times.
     *
     * @return  an enumeration over the collected flat times
     */
    public static Enumeration getFlatTimes() {
        return flatTimes.elements();
    }

    /**
     * Gets an enumeration over the collected accumulative times.
     *
     * @return  an enumeration over the collected accumulative times
     */
    public static Enumeration getTotalTimes() {
        return totalTimes.elements();
    }

    /**
     * Returns a string representation of the times accumulated by the timer
     * in the form of a set of entries, enclosed in braces and separated
     * by the ASCII characters ", " (comma and space). Each entry is rendered
     * as the computation identifier, a colon sign ':', the total time
     * associated with the computation, a colon sign ':' and the flat time
     * associated with the computation.
     *
     * @return a string representation of the collected times
     */
    public static String timesAsString() {
        StringBuffer buf = new StringBuffer("{ ");
        Enumeration keys = flatTimes.keys();
        Enumeration ftimes = flatTimes.elements();
        Enumeration ttimes = totalTimes.elements();
        while (keys.hasMoreElements()) {
            String id = (String)keys.nextElement();
            Long ftime = (Long)ftimes.nextElement();
            Long ttime = (Long)ttimes.nextElement();
            buf.append(id).append(":").append(ttime.toString()).append(":").append(ftime.toString());
            if (keys.hasMoreElements()) {
                buf.append(", ");
            }
        }
        return buf.append(" }").toString();
    }

    /**
     * Print a summary of the times.
     *
     * @param out PrintStream
     */
    public static void dump(PrintStream out) {
        out.println("Times: flat | total | computation");
        Enumeration keys = flatTimes.keys();
        Enumeration ftimes = flatTimes.elements();
        Enumeration ttimes = totalTimes.elements();
        while (keys.hasMoreElements()) {
            String id = (String)keys.nextElement();
            Long ftime = (Long)ftimes.nextElement();
            Long ttime = (Long)ttimes.nextElement();
            out.println(ftime.toString() + '\t' + ttime + '\t' + id);
        }
        out.println("Total: " + (System.currentTimeMillis() - startUp));
    }
}
