/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import java.io.PrintStream;
import com.sun.squawk.vm.HDR;
import com.sun.squawk.util.Assert;

/**
 * The <code>Throwable</code> class is the superclass of all errors
 * and exceptions in the Java language. Only objects that are
 * instances of this class (or of one of its subclasses) are thrown
 * by the Java Virtual Machine or can be thrown by the Java
 * <code>throw</code> statement. Similarly, only this class or one of
 * its subclasses can be the argument type in a <code>catch</code>
 * clause.
 * <p>
 * Instances of two subclasses, {@link java.lang.Error} and
 * {@link java.lang.Exception}, are conventionally used to indicate
 * that exceptional situations have occurred. Typically, these instances
 * are freshly created in the context of the exceptional situation so
 * as to include relevant information (such as stack trace data).
 * <p>
 * By convention, class <code>Throwable</code> and its subclasses have
 * two constructors, one that takes no arguments and one that takes a
 * <code>String</code> argument that can be used to produce an error
 * message.
 * <p>
 * A <code>Throwable</code> class contains a snapshot of the
 * execution stack of its thread at the time it was created. It can
 * also contain a message string that gives more information about
 * the error.
 * <p>
 * Here is one example of catching an exception:
 * <p><blockquote><pre>
 *     try {
 *         int a[] = new int[2];
 *         a[4];
 *     } catch (ArrayIndexOutOfBoundsException e) {
 *         System.out.println("exception: " + e.getMessage());
 *         e.printStackTrace();
 *     }
 * </pre></blockquote>
 *
 * @author  unascribed
 * @version 1.43, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0
 */
public class Throwable {

    /**
     * The detailed message for the exception.
     */
    private String detailMessage;

    /**
     * The array of method pointers in the back trace.
     */
    private Object[] backtraceMP;

    /**
     * The array of relative instruction offsets in the back trace.
     */
    private int[] backtraceIP;

    /**
     * Constructs a new <code>Throwable</code> with <code>null</code> as
     * its error message string.
     */
    public Throwable() {

        /*
         * Count the number of frames and allocate the backtrace array.
         */
        int frames = 0;
        Address fp = VM.getFP();
        fp = VM.getPreviousFP(fp);
        while (!fp.isZero()) {
            frames++;
            fp = VM.getPreviousFP(fp);
        }
        backtraceMP = new Object[frames];
        backtraceIP = new int[frames];

        /*
         * Fill in the backtrace information.
         */
        int index = 0;
        fp = VM.getFP();
        while (true) {
            Address pip = VM.getPreviousIP(fp);
            fp = VM.getPreviousFP(fp);
            if (fp.isZero()) {
                break;
            }
            Object pmp = VM.getMP(fp);
            int relip = pip.diff(Address.fromObject(pmp)).toInt();
            backtraceIP[index] = relip;
            backtraceMP[index] = pmp;
            index++;
        }
        Assert.that(index == backtraceIP.length);
    }

    /**
     * Constructs a new <code>Throwable</code> with the specified error
     * message.
     *
     * @param   message   the error message. The error message is saved for
     *          later retrieval by the {@link #getMessage()} method.
     */
    public Throwable(String message) {
        this();
        detailMessage = message;
    }

    /**
     * Returns the error message string of this throwable object.
     *
     * @return  the error message string of this <code>Throwable</code>
     *          object if it was {@link #Throwable(String) created} with an
     *          error message string; or <code>null</code> if it was
     *          {@link #Throwable() created} with no error message.
     *
     */
    public String getMessage() {
        return detailMessage;
    }

    /**
     * Returns a short description of this throwable object.
     * If this <code>Throwable</code> object was
     * {@link #Throwable(String) created} with an error message string,
     * then the result is the concatenation of three strings:
     * <ul>
     * <li>The name of the actual class of this object
     * <li>": " (a colon and a space)
     * <li>The result of the {@link #getMessage} method for this object
     * </ul>
     * If this <code>Throwable</code> object was {@link #Throwable() created}
     * with no error message string, then the name of the actual class of
     * this object is returned.
     *
     * @return  a string representation of this <code>Throwable</code>.
     */
    public String toString() {
        String s = getClass().getName();
        String message = getMessage();
        return (message != null) ? (s + ": " + message) : s;
    }

    /**
     * Prints this <code>Throwable</code> and its backtrace to the
     * standard error stream. This method prints a stack trace for this
     * <code>Throwable</code> object on the error output stream that is
     * the value of the field <code>System.err</code>. The first line of
     * output contains the result of the {@link #toString()} method for
     * this object. <p>
     *
     * The format of the backtrace information depends on the implementation.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints this <code>Throwable</code> and its backtrace to the
     * standard error stream. This method prints a stack trace for this
     * <code>Throwable</code> object on the error output stream that is
     * the value of the field <code>System.err</code>. The first line of
     * output contains the result of the {@link #toString()} method for
     * this object. <p>
     *
     * The format of the backtrace information depends on the implementation.
     */
    public void printStackTrace(PrintStream stream) {
        String message = getMessage();
        stream.print(this.getClass().getName());
        if (message != null) {
            stream.print(": ");
            stream.print(message);
        }
        stream.println();
        if (!(this instanceof OutOfMemoryError)) {
            for (int i = 0 ; i < backtraceIP.length ; i++) {
                int relip = backtraceIP[i];
                Object mp = backtraceMP[i];
                Klass klass = VM.asKlass(Unsafe.getObject(mp, HDR.methodDefiningClass));
                Method method = klass.findMethod(mp);
                if (method == null) {
                    stream.print("    in a method of ");
                    stream.print(klass.getName());
                    stream.print("(ip=");
                    stream.print(relip);
                    stream.println(")");
                } else {
                    stream.print("    at ");
                    stream.print(klass.getName());
                    stream.print(".");
                    stream.print(method.getName());
                    stream.print("(");
                    String src = klass.getSourceFileName();
                    if (src == null) {
                        src = "?";
                    }
                    stream.print(src);
                    stream.print(":");
                    int[] lnt = method.getLineNumberTable();
                    if (lnt != null) {
                        int lno = Method.getLineNumber(lnt, relip);
                        stream.print(lno);
                    } else {
                        stream.print("ip=");
                        stream.print(relip);
                    }
                    stream.println(")");
                }
            }
        }
    }

}
