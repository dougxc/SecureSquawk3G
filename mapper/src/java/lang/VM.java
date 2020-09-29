package java.lang;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

import java.io.*;
import java.util.*;
import com.sun.squawk.vm.*;
import com.sun.squawk.util.*;
import com.sun.squawk.translator.Translator;

public class VM {

    static boolean bigEndian;
    static boolean verbose;
    static PrintStream out = System.err;
    static Isolate currentIsolate;
    static Translator translator;

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

    /**
     * Gets the flag indicating if the VM is running in verbose mode.
     *
     * @return true if the VM is running in verbose mode
     */
    public static boolean isVerbose() {
        return verbose;
    }

    /**
     * Get the endianess.
     *
     * @return true if the system is big endian
     */
    public static boolean isBigEndian() {
        return bigEndian;
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
     * Determines if the VM was built with memory access type checking enabled.
     *
     * @return true
     */
    public static boolean usingTypeMap() {
        return true;
    }

    /**
     * Gets the Klass object corresponding to the address of a class in the image.
     *
     * @param klassOop  the address of a class in the image
     * @return the Klass instance corresponding to <code>klassOop</code>
     */
    static Klass asKlass(Object klassOop) {
        Klass klass = (Klass)classCache.get(klassOop);
        if (klass == null) {
            Object nameOop = Unsafe.getObject(klassOop, (int)FieldOffsets.java_lang_Klass$name);
            String name = asString(nameOop);
            int classID = Unsafe.getInt(klassOop, (int)FieldOffsets.java_lang_Klass$classID);
            klass = translator.findClass(name, -1, false);
//            Assert.that(classID == klass.getClassID());
            translator.load(klass);
            classCache.put(klassOop, klass);
            classIDCache.put(classID, klass);
        }
        return klass;
    }

    private static HashMap classCache = new HashMap();
    private static IntHashtable classIDCache = new IntHashtable();

    /**
     * Gets the Klass object corresponding to a given class identifier.
     *
     * @param   classID  a class identifier
     * @return Klass corresponding to classID or null if that class has not yet been loaded
     */
    static Klass asKlass(int classID) {
        return (Klass)classIDCache.get(classID);
    }

    /**
     * Gets the String instance corresponding to a String in the image.
     *
     * @param stringOop    the address of a String in the image
     * @return the String instance corresponding to <code>stringOop</code>
     */
    static String asString(Object stringOop) {
        int length = GC.getArrayLengthNoCheck(stringOop);

        // get the class ID of the string
        Object something = Unsafe.getObject(stringOop, HDR.klass);
        Object stringOopKlass =  Unsafe.getObject(something, (int)FieldOffsets.java_lang_Klass$self);
        int classID = Unsafe.getInt(stringOopKlass, (int)FieldOffsets.java_lang_Klass$classID);
        Assert.that(classID == CID.STRING || classID == CID.STRING_OF_BYTES);


        // assume it is an 8-bit string
        StringBuffer buf = new StringBuffer(length);
        for (int i = 0 ; i < length; i++) {
            char ch;
            if (classID == CID.STRING) {
                ch = (char) Unsafe.getChar(stringOop, i);
            } else {
                ch = (char)(Unsafe.getByte(stringOop, i) & 0xFF);
            }
            buf.append(ch);
        }

        return buf.toString();
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
     * Stop fatally.
     */
    static void fatalVMError() {
         Assert.shouldNotReachHere("fatalVMError");
    }

    /**
     * Print an error message.
     *
     * @param msg the message
     */
    public static void println(String msg) {
        System.err.println(msg);
    }

    public static void print(String s) {
        System.err.print(s);
    }

    public static void print(int i) {
        System.err.print(i);
    }

    public static void println() {
        System.err.println();
    }

    public static void printAddress(Object val) {
        System.err.print(val);
    }

}
