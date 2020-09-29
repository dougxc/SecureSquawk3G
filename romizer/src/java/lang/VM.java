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
import com.sun.squawk.util.*;
import java.util.Enumeration;

public class VM {

    static Romizer romizer;

    /*
     * Create the dummy isolate for romizing.
     */
    private static Isolate currentIsolate;

    /**
     * Flag to show if the extend bytecode can be executed.
     */
    static boolean extendsEnabled;

    /*=======================================================================*\
     *                           Romizer support                             *
    \*=======================================================================*/

    /**
     * The system-dependent path-separator character. This character is used to
     * separate filenames in a sequence of files given as a <em>path list</em>.
     * On UNIX systems, this character is <code>':'</code>; on Windows
     * systems it is <code>';'</code>.
     *
     * @return  the system-dependent path-separator character
     */
    public static char getPathSeparatorChar() {
        return File.pathSeparatorChar;
    }

    /**
     * The system-dependent default name-separator character.  This field is
     * initialized to contain the first character of the value of the system
     * property <code>file.separator</code>.  On UNIX systems the value of this
     * field is <code>'/'</code>; on Microsoft Windows systems it is <code>'\'</code>.
     *
     * @see     java.lang.System#getProperty(java.lang.String)
     */
    public static char getFileSeparatorChar() {
        return File.separatorChar;
    }

    public static boolean isVerbose() {
        return false;
    }

    /**
     * Determines if the Squawk system is being run in a hosted environment
     * such as the romizer or mapper application.
     *
     * @return true if the Squawk system is being run in a hosted environment
     */
    public static boolean isHosted() {
        return true;
    }

    /**
     * Get the endianess.
     *
     * @return true if the system is big endian
     */
    public static boolean isBigEndian() {
        return Romizer.bigEndian;
    }

    /**
     * Assert a condition.
     *
     * @param b a boolean value that must be true.
     */
    public static void assume(boolean b) {
        Assert.that(b);
    }

    /**
     * Get the isolate of the currently executing thread.
     *
     * @return the isolate
     */
    public static Isolate getCurrentIsolate() {
        return currentIsolate;
    }

    /**
     * Set the isolate of the currently executing thread.
     *
     * @param isolate the isolate
     */
    static void setCurrentIsolate(Isolate isolate) {
        currentIsolate = isolate;
    }


    /*=======================================================================*\
     *                              Native methods                           *
    \*=======================================================================*/

    /**
     * Zero a block of memory.
     *
     * @param      start        the start address of the memory area
     * @param      end          the end address of the memory area
     */
    static void zeroWords(Object start, Object end) {
        // Not needed for the romizer.
    }

    /**
     * Determines if the VM was built with memory access type checking enabled.
     *
     * @return true
     */
    public static boolean usingTypeMap() {
        return true;
    }


    /*=======================================================================*\
     *                              Symbols dumping                          *
    \*=======================================================================*/


    public static final int STREAM_STDOUT = 0;
    public static final int STREAM_STDERR = 1;
    static final int STREAM_SYMBOLS = 2;
    static final int STREAM_HEAPTRACE = 3;

    static int stream = STREAM_STDOUT;
    static final PrintStream Streams[] = new PrintStream[4];
    static {
        Streams[STREAM_STDOUT] = System.out;
        Streams[STREAM_STDERR] = System.err;
    }


    /**
     * Sets the stream for the VM.print... methods to one of the STREAM_... constants.
     *
     * @param stream  the stream to use for the print... methods
     */
    public static int setStream(int stream) {
        Assert.always(stream >= STREAM_STDOUT && stream <= STREAM_HEAPTRACE, "invalid stream specifier");
        int old = VM.stream;
        VM.stream = stream;
        return old;
    }

    /**
     * Print an error message.
     *
     * @param msg the message
     */
    public static void println(String msg) {
        PrintStream out = Streams[stream];
        out.println(msg);
        out.flush();
    }

    public static void println() {
        PrintStream out = Streams[stream];
        out.println();
        out.flush();
    }

    public static void print(String s) {
        PrintStream out = Streams[stream];
        out.print(s);
        out.flush();
    }

    public static void print(int i) {
        PrintStream out = Streams[stream];
        out.print(i);
        out.flush();
    }

    public static void printAddress(Object val) {
        PrintStream out = Streams[stream];
        out.print(val);
        out.flush();
    }

    public static void printUWord(UWord val) {
        PrintStream out = Streams[stream];
        out.print(val);
        out.flush();
    }

    public static void printOffset(Offset val) {
        PrintStream out = Streams[stream];
        out.print(val);
        out.flush();
    }

    /**
     * Stop fatally.
     */
    public static void fatalVMError() {
        throw new Error();
    }

    /*=======================================================================*\
     *                             Object graph copying                      *
    \*=======================================================================*/

    /**
     * Make a copy of the object graph rooted at a given object.
     *
     * @param object    the root of the object graph to copy
     * @return the ObjectMemorySerializer.ControlBlock instance that contains the serialized object graph and
     *                  its metadata. This will be null if there was insufficient memory
     *                  to do the serialization.
     */
    static ObjectMemorySerializer.ControlBlock copyObjectGraph(Object object) {
        assume(object instanceof Suite);
        ObjectMemorySerializer.ControlBlock cb = new ObjectMemorySerializer.ControlBlock();

        cb.root = Address.fromObject(ObjectGraphSerializer.serialize(object, romizer)).toUWord().toInt();
        cb.size = Unsafe.getMemorySize();
        cb.memory = Address.zero();
        cb.oopMap = Unsafe.getOopMap();
        return cb;
    }


    /*=======================================================================*\
     *                          Native method lookup                         *
    \*=======================================================================*/

    /**
     * Hashtable to translate names into enumerations.
     */
    private static Hashtable table = new Hashtable();

    /**
     * Hashtable of unused native methods.
     */
    private static Hashtable unused = new Hashtable();

    /**
     * Initializer.
     */
    static {
        try {
            Class clazz = Class.forName("com.sun.squawk.vm.Native");
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (int i = 0 ; i < fields.length ; i++) {
                java.lang.reflect.Field field = fields[i];
                String name = field.getName().replace('_', '.').replace('$', '.');
                int number  = field.getInt(null);
                table.put(name, new Integer(number));
                unused.put(name, name);
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    /**
     * Gets the identifier for a registered native method.
     *
     * @param name   the fully qualified name of the native method
     * @return the identifier for the method or -1 if the method has not been registered
     */
    public static int lookupNative(String name) {
        Integer id = (Integer)table.get(name);
        if (id != null) {
            unused.remove(name);
            return id.intValue();
        }
        return -1;
    }

    /**
     * Get all the symbols in a form that will go into a properties file.
     *
     * @return a string with all the definitions.
     */
    public static void printNatives(PrintStream out) {
        Enumeration names = table.keys();
        Enumeration identifiers = table.elements();
        while (names.hasMoreElements()) {
            out.println("NATIVE."+identifiers.nextElement()+".NAME="+names.nextElement());
        }


/* Uncomment to get list of apparently unused native methods */
/*
        Enumeration keys = unused.keys();
        while (keys.hasMoreElements()) {
            System.err.println("Warning: Unused native method "+keys.nextElement());
        }
*/
    }
}
