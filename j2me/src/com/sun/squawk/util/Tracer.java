/*
 *  Copyright (c) 2001 Sun Microsystems, Inc., 901 San Antonio Road,
 *  Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 *  Sun Microsystems, Inc. has intellectual property rights relating
 *  to the technology embodied in this software.  In particular, and
 *  without limitation, these intellectual property rights may include
 *  one or more U.S. patents, foreign patents, or pending
 *  applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 *  and all Sun-based and Java-based marks are trademarks or
 *  registered trademarks of Sun Microsystems, Inc.  in the United
 *  States and other countries.
 *
 *  This software is distributed under licenses restricting its use,
 *  copying, distribution, and decompilation.  No part of this
 *  software may be reproduced in any form by any means without prior
 *  written authorization of Sun and its licensors, if any.
 *
 *  FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 *  Subject to Standard License Terms and Conditions
 */

package com.sun.squawk.util;

import java.io.PrintStream;
import com.sun.squawk.util.Hashtable; // Version without synchronization


/**
 * The Tracer class encapsulates a set of static methods that are used to
 * emit execution traces to a print stream.<p>
 *
 * It is intended for usage of this class to be statically conditional. That is,
 * it need not exist in a production deployment. As such, all calls/references
 * to this class should be wrapped in a conditional test of the final static
 * boolean variable {@link Klass#DEBUG}. For example:<p>
 *
 * <p><hr><blockquote><pre>
 *     if (Klass.DEBUG) && Tracer.isTracing("loading", klass.getName()) {
 *         Tracer.traceln("Loading "+klass.getName());
 *     }
 * </pre></blockquote><hr><p>
 *
 * If the value of <code>Klass.DEBUG</code> was statically
 * determined to be false by javac, then this whole block of code would be
 * omitted from the compiled class.
 */
public final class Tracer {

    /**
     * The features that are to be traced.
     */
    private static Hashtable _features;
    private static Hashtable features() {
        if (_features == null) {
            _features = new Hashtable();
        }
        return _features;
    }

    /**
     * The print stream to which trace output it written. The
     * default is {@link System#out}.
     */
    private static PrintStream _out;
    private static PrintStream out() {
        if (_out == null) {
            _out = System.out;
        }
        return _out;
    }

    /**
     * A string that is used to determine whether or not a current block
     * of trace code should be executed.
     */
    private static String filter;

    /**
     * Sets the print stream to which traces will be written. This must be
     * done before a call to {@link #trace(String)} or {@link #traceln(String)}.
     *
     * @param  out     the print stream to which trace output is written
     */
    public static void setPrintStream(PrintStream out) {
        Assert.that(out != null, "print stream for tracer cannot be null");
        Tracer._out = out;
    }

    /**
     * Gets the print stream to which traces will be written.
     *
     * @return the print stream to which trace output is written
     */
    public static PrintStream getPrintStream() {
        return Tracer.out();
    }

    /**
     * Sets the string used to enable/disable the tracer. If <code>filter</code>
     * is null, then filtering is turned off.
     *
     * @param  filter  the string used to enable/disable the tracer
     */
    public static void setFilter(String filter) {
        Tracer.filter = filter;
    }

    /**
     * Enable a given feature to be traced.
     *
     * @param feature
     */
    public static void enableFeature(String feature) {
        Assert.that(feature != null);
        features().put(feature, feature);
    }

    /**
     * Disable all the enabled features and clears the filter (if any).
     */
    public static void reset() {
        features().clear();
        filter = null;
    }

    /**
     * Determines whether or not a given feature is being traced for a given
     * component.
     *
     * @param  feature    the feature to be traced
     * @param  component  the component to be traced
     */
    public static boolean isTracing(String feature, String component) {
        return (features().contains(feature) &&
                (filter == null ||
                 component == null ||
                 component.indexOf(filter) != -1));
    }

    /**
     * Determines whether or not a given feature is being traced.
     *
     * @param  feature    the feature to be traced
     */
    public static boolean isTracing(String feature) {
        return (features().contains(feature));
    }

    /**
     * Prevent construction.
     */
    private Tracer() {
    }

    /**
     * Output a string to the tracer's print stream.
     *
     * @param message the message to write
     */
    public static void trace(String message) {
        out().print(message);
    }

    /**
     * Output a string to the tracer's print stream followed by a newline.
     *
     * @param message the message to write
     */
    public static void traceln(String message) {
        out().println(message);
    }
}
