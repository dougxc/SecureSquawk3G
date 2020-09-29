/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.Hashtable; // Version without synchronization

/**
 * The <code>System</code> class contains several useful class fields
 * and methods. It cannot be instantiated.
 *
 * @author  Arthur van Hoff
 * @version 1.110, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0
 */
public final class System {

    /**
     * The table of system properties underlying the getProperty and
     * setProperty methods in System.
     */
    private static Hashtable systemProperties;

    /*
     * Threading must be initialized and class Klass initialized before System can be initialized.
     */
    static {
        Assert.that(Thread.currentThread() != null);
        Assert.that(VM.getCurrentIsolate().isClassKlassInitialized());
    }

    /**
     * The following is a required variable for J2SE conpilence. It is currently unsupported.
     */
    public final static InputStream in = null;

    /**
     * Don't let anyone instantiate this class
     */
    private System() { }

    /**
     * The "standard" output stream. This stream is already
     * open and ready to accept output data. Typically this stream
     * corresponds to display output or another output destination
     * specified by the host environment or user.
     * <p>
     * For simple stand-alone Java applications, a typical way to write
     * a line of output data is:
     * <blockquote><pre>
     *     System.out.println(data)
     * </pre></blockquote>
     * <p>
     * See the <code>println</code> methods in class <code>PrintStream</code>.
     *
     * @see     java.io.PrintStream#println()
     * @see     java.io.PrintStream#println(boolean)
     * @see     java.io.PrintStream#println(char)
     * @see     java.io.PrintStream#println(char[])
     * @see     java.io.PrintStream#println(int)
     * @see     java.io.PrintStream#println(long)
     * @see     java.io.PrintStream#println(java.lang.Object)
     * @see     java.io.PrintStream#println(java.lang.String)
     */
    public final static PrintStream out = getOutput(false);

    /**
     * The "standard" error output stream. This stream is already
     * open and ready to accept output data.
     * <p>
     * Typically this stream corresponds to display output or another
     * output destination specified by the host environment or user. By
     * convention, this output stream is used to display error messages
     * or other information that should come to the immediate attention
     * of a user even if the principal output stream, the value of the
     * variable <code>out</code>, has been redirected to a file or other
     * destination that is typically not continuously monitored.
     */
    public final static PrintStream err = getOutput(true);

    /**
     * Gets an output stream for strout or stderr.
     *
     * @param err true if the output strean required is stderr
     * @return the output stream
     */
    private static PrintStream getOutput(boolean err) {
        PrintStream res = (err ? out : new java.io.NativePrintStream());
        try {
            String url = System.getProperty(err ? "java.lang.System.err" : "java.lang.System.out");
            if (url == null) {
                url = err ? "debug:err" : "debug:";
            }
            OutputStream os = Connector.openOutputStream(url);
            res = new PrintStream(os);
        } catch(IOException x) {
            x.printStackTrace();
            throw new RuntimeException("Could not open debug stream: " + x.getMessage());
        }
        return res;
    }

    /**
     * Returns the current time in milliseconds.
     *
     * @return  the difference, measured in milliseconds, between the current
     *          time and midnight, January 1, 1970 UTC.
     */
    public static long currentTimeMillis() {
        return VM.getTime();
    }

    /**
     * Copies an array from the specified source array, beginning at the
     * specified position, to the specified position of the destination array.
     * A subsequence of array components are copied from the source
     * array referenced by <code>src</code> to the destination array
     * referenced by <code>dst</code>. The number of components copied is
     * equal to the <code>length</code> argument. The components at
     * positions <code>srcOffset</code> through
     * <code>srcOffset+length-1</code> in the source array are copied into
     * positions <code>dstOffset</code> through
     * <code>dstOffset+length-1</code>, respectively, of the destination
     * array.
     * <p>
     * If the <code>src</code> and <code>dst</code> arguments refer to the
     * same array object, then the copying is performed as if the
     * components at positions <code>srcOffset</code> through
     * <code>srcOffset+length-1</code> were first copied to a temporary
     * array with <code>length</code> components and then the contents of
     * the temporary array were copied into positions
     * <code>dstOffset</code> through <code>dstOffset+length-1</code> of the
     * destination array.
     * <p>
     * If <code>dst</code> is <code>null</code>, then a
     * <code>NullPointerException</code> is thrown.
     * <p>
     * If <code>src</code> is <code>null</code>, then a
     * <code>NullPointerException</code> is thrown and the destination
     * array is not modified.
     * <p>
     * Otherwise, if any of the following is true, an
     * <code>ArrayStoreException</code> is thrown and the destination is
     * not modified:
     * <ul>
     * <li>The <code>src</code> argument refers to an object that is not an
     *     array.
     * <li>The <code>dst</code> argument refers to an object that is not an
     *     array.
     * <li>The <code>src</code> argument and <code>dst</code> argument refer to
     *     arrays whose component types are different primitive types.
     * <li>The <code>src</code> argument refers to an array with a primitive
     *     component type and the <code>dst</code> argument refers to an array
     *     with a reference component type.
     * <li>The <code>src</code> argument refers to an array with a reference
     *     component type and the <code>dst</code> argument refers to an array
     *     with a primitive component type.
     * </ul>
     * <p>
     * Otherwise, if any of the following is true, an
     * <code>IndexOutOfBoundsException</code> is
     * thrown and the destination is not modified:
     * <ul>
     * <li>The <code>srcOffset</code> argument is negative.
     * <li>The <code>dstOffset</code> argument is negative.
     * <li>The <code>length</code> argument is negative.
     * <li><code>srcOffset+length</code> is greater than
     *     <code>src.length</code>, the length of the source array.
     * <li><code>dstOffset+length</code> is greater than
     *     <code>dst.length</code>, the length of the destination array.
     * </ul>
     * <p>
     * Otherwise, if any actual component of the source array from
     * position <code>srcOffset</code> through
     * <code>srcOffset+length-1</code> cannot be converted to the component
     * type of the destination array by assignment conversion, an
     * <code>ArrayStoreException</code> is thrown. In this case, let
     * <b><i>k</i></b> be the smallest nonnegative integer less than
     * length such that <code>src[srcOffset+</code><i>k</i><code>]</code>
     * cannot be converted to the component type of the destination
     * array; when the exception is thrown, source array components from
     * positions <code>srcOffset</code> through
     * <code>srcOffset+</code><i>k</i><code>-1</code>
     * will already have been copied to destination array positions
     * <code>dstOffset</code> through
     * <code>dstOffset+</code><i>k</I><code>-1</code> and no other
     * positions of the destination array will have been modified.
     * (Because of the restrictions already itemized, this
     * paragraph effectively applies only to the situation where both
     * arrays have component types that are reference types.)
     *
     * @param      src          the source array.
     * @param      srcPos       start position in the source array.
     * @param      dst          the destination array.
     * @param      dstPos       start position in the destination data.
     * @param      length       the number of array elements to be copied.
     * @exception  IndexOutOfBoundsException  if copying would cause
     *               access of data outside array bounds.
     * @exception  ArrayStoreException  if an element in the <code>src</code>
     *               array could not be stored into the <code>dest</code> array
     *               because of a type mismatch.
     * @exception  NullPointerException if either <code>src</code> or
     *               <code>dst</code> is <code>null</code>.
     */
    public static void arraycopy(Object src, int srcPos, Object dst, int dstPos, int length) {
        if ((src == null) || (dst == null)) {
            throw new NullPointerException();
        }

        Klass srcClass = GC.getKlass(src);
        Klass dstClass = GC.getKlass(dst);
        if (!srcClass.isArray() || !dstClass.isArray()) {
            throw new ArrayStoreException();
        }

        Klass srcElmt = srcClass.getComponentType();
        Klass dstElmt = dstClass.getComponentType();
        boolean prim  = srcElmt.isPrimitive() || dstElmt.isPrimitive();
        if (prim && srcElmt != dstElmt) {
            throw new ArrayStoreException(srcElmt.getName() + " != " + dstElmt.getName());
        }

        int srcEnd = length + srcPos;
        int dstEnd = length + dstPos;
        if (
                (length < 0) ||
                (srcPos < 0) ||
                (dstPos < 0) ||
                (length > 0 && (srcEnd < 0 || dstEnd < 0)) ||
                (srcEnd > GC.getArrayLength(src)) ||
                (dstEnd > GC.getArrayLength(dst))
           ) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (!prim) {
            if (!dstElmt.isAssignableFrom(srcElmt)) {
                Object[] srcArray = (Object[])src;
                Object[] dstArray = (Object[])dst;
                for (int i = 0 ; i < length ; i++) {
                    Object item = srcArray[srcPos + i];
                    if (item != null && !dstElmt.isAssignableFrom(GC.getKlass(item))) {
                        throw new ArrayStoreException();
                    }
                    dstArray[dstPos + i] = item;
                }
                return;
            }
        }
        if (length > 0) {
            arraycopy0(src, srcPos, dst, dstPos, length);
        }
    }

    /*
     * arraycopy0
     */
    private static void arraycopy0(Object src, int src_position, Object dst, int dst_position, int totalLength) {
        final int MAXMOVE = 4096;
        while (true) {
            int length = Math.min(totalLength, MAXMOVE);
            GC.arraycopy(src, src_position, dst, dst_position, length);
            totalLength -= length;
            if (totalLength == 0) {
                break;
            }
            src_position += length;
            dst_position += length;
            Thread.yield();
        }
    }

    /**
     * Returns the same hashcode for the given object as
     * would be returned by the default method hashCode(),
     * whether or not the given object's class overrides
     * hashCode().
     * The hashcode for the null reference is zero.
     *
     * @param x object for which the hashCode is to be calculated
     * @return  the hashCode
     * @since   JDK1.1
     */
    public static int identityHashCode(Object x) {
        return x == null ? 0 : GC.getHashCode(x);
    }

    /**
     * Sets the system property indicated by the specified key.
     * <p>
     * First, if a security manager exists, its
     * <code>SecurityManager.checkPermission</code> method
     * is called with a <code>PropertyPermission(key, "write")</code>
     * permission. This may result in a SecurityException being thrown.
     * If no exception is thrown, the specified property is set to the given
     * value.
     * <p>
     *
     * @param      key   the name of the system property.
     * @param      value the value of the system property.
     * @return     the previous value of the system property,
     *             or <code>null</code> if it did not have one.
     *
     * @exception  SecurityException  if a security manager exists and its
     *             <code>checkPermission</code> method doesn't allow
     *             setting of the specified property.
     * @exception  NullPointerException if <code>key</code> is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     * @see        #getProperty
     * @see        java.lang.System#getProperty(java.lang.String)
     * @since      1.2
     */
    public static String setProperty(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key can't be empty");
        }
        if (systemProperties == null) {
            systemProperties = new Hashtable();
        }
        return (String)systemProperties.put(key, value);
    }


    /**
     * Gets the system property indicated by the specified key.
     *
     * @param      key   the name of the system property.
     * @return     the string value of the system property,
     *             or <code>null</code> if there is no property with that key.
     *
     * @exception  NullPointerException if <code>key</code> is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     */
    public static String getProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key can't be empty");
        }

        /*
         * These are the hard-coded properties that cannot be changed.
         */
        if (key.equals("microedition.configuration"))                   return "CLDC-1.0";
        if (key.equals("microedition.encoding"))                        return "ISO8859_1";
        if (key.equals("microedition.platform"))                        return "j2me";
        if (key.equals("awtcore.classbase"))                            return "awtcore.impl.squawk";
        if (key.equals("javax.microedition.io.Connector.protocolpath")) return "com.sun.squawk.io";
        if (key.equals("file.separator")) return "" + VM.getFileSeparatorChar();
        if (key.equals("path.separator")) return "" + VM.getPathSeparatorChar();

        String value = VM.getCurrentIsolate().getProperty(key);
        if (value != null) {
            return value;
        }

        if (systemProperties != null) {
            return (String)systemProperties.get(key);
        } else {
            return null;
        }
    }

    /**
     * Terminates the currently running Java application. The
     * argument serves as a status code; by convention, a nonzero
     * status code indicates abnormal termination.
     * <p>
     * This method calls the <code>exit</code> method in class
     * <code>Runtime</code>. This method never returns normally.
     * <p>
     * The call <code>System.exit(n)</code> is effectively equivalent
     * to the call:
     * <blockquote><pre>
     * Runtime.getRuntime().exit(n)
     * </pre></blockquote>
     *
     * @param      status   exit status.
     * @see        java.lang.Runtime#exit(int)
     */
    public static void exit(int status) {
        Runtime.getRuntime().exit(status);
    }

    /**
     * Runs the garbage collector.
     * <p>
     * Calling the <code>gc</code> method suggests that the Java Virtual
     * Machine expend effort toward recycling unused objects in order to
     * make the memory they currently occupy available for quick reuse.
     * When control returns from the method call, the Java Virtual
     * Machine has made a best effort to reclaim space from all discarded
     * objects.
     * <p>
     * The call <code>System.gc()</code> is effectively equivalent to the
     * call:
     * <blockquote><pre>
     * Runtime.getRuntime().gc()
     * </pre></blockquote>
     *
     * @see     java.lang.Runtime#gc()
     */
    public static void gc() {
        Runtime.getRuntime().gc();
    }

    /**
     * Loads a native code library. This is not currently a supported function.
     *
     * @param name the name of the DLL to load
     */
    public static void loadLibrary(String name) {
        throw new RuntimeException("System.loadLibrary() called");
    }

    /**
     * Use the "systemproperties:" protocol if it exists to update the
     * system properties table from the system properties of the host
     * environment.
     *
     * The method enables an application to get access to all the system
     * properties when the VM is running on another JVM or at least
     * using an embedded JVM (via JNI) for it IO and graphics subsystems.
     *
     * Previous set system properties are not overridden.
     */
/*
    public static void updateSystemPropertiesFromHostJVM() {
        Hashtable properties = systemProperties;
        if (properties == null) {
            properties = systemProperties = new Hashtable();
        }
        try {
            DataInputStream in = Connector.openDataInputStream("systemproperties:");
            while (in.available() != 0) {
                String key = in.readUTF();
                String value = in.readUTF();
                if (!properties.containsKey(key)) {
                    properties.put(key, value);
                }
            }
        } catch (IOException ioe) {
        }
    }
*/

}


