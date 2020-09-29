/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

/**
 * An instance of <code>ExceptionHandler</code> describes a single exception
 * handler in a method. It can be used to represent an exception handler in
 * a JVM or Squawk method.
 *
 * @author  Doug Simon
 */
public class ExceptionHandler {

    /**
     * The bytecode address at which the exception handler becomes active.
     */
    private final int start;

    /**
     * The bytecode address at which the exception handler becomes deactive.
     */
    private final int end;

    /**
     * The entry bytecode address of the handler.
     */
    private final int handler;

    /**
     * The <code>Throwable</code> subclass caught by this handler.
     *
     */
    private final Klass klass;

    /**
     * Create an exception handler.
     *
     * @param start   the start of code range protected by the handler
     * @param end     the end of code range protected by the handler
     * @param handler the handler's entry point
     * @param klass   the <code>Throwable</code> subclass caught by the handler.
     */
    public ExceptionHandler (int start, int end, int handler, Klass klass) {
        this.start   = start;
        this.end     = end;
        this.handler = handler;
        this.klass   = klass;
    }

    /**
     * Gets the address at which this exception handler becomes active.
     *
     * @return the address at which this exception handler becomes active
     */
    public int getStart() {
        return start;
    }

    /**
     * Gets the address at which this exception handler becomes deactive.
     *
     * @return the address at which this exception handler becomes deactive
     */
    public int getEnd() {
        return end;
    }

    /**
     * Gets the address of the entry to this exception handler.
     *
     * @return the address of the entry to this exception handler
     */
    public int getHandler() {
        return handler;
    }

    /**
     * Gets the subclass of {@link Throwable} caught by this handler.
     *
     * @return  the subclass of <code>Throwable</code> caught by this handler
     */
    public Klass getKlass() {
        return klass;
    }
}