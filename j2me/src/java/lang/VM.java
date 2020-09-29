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
import com.sun.squawk.vm.*;
import com.sun.squawk.util.Assert;

/**
 * This is a Squawk VM specific class that is used to communicate between
 * executing Java software and the low level core VM that is expressed
 * in machine code. There are two parts to this. There are a set of native
 * methods that are used to invoke very low level operations like manipulating
 * memory at a very low level or performing I/O. In the other direction there
 * are a number of methods that the low level core may call. These are used to
 * implement high level operations that are better done in Java than in machine
 * code.
 * <p>
 * A special version of this class exists for the romizer. The romizer version
 * only implements the methods used to manipulate memory.
 *
 * @author  Nik Shaylor
 * @version 1.0
 */
public class VM {

    /*
     * Note regarding methods with names starting with "do_".
     *
     * These methods must only be called from the VM interpreter or jitted code.
     * In a system where parameters are pushed onto the stack in the right-to-left
     * order (x86, ARM, etc.) the translator makes sure that the methods in this
     * class that start "do_" are changed so that the normal Java left-to-right
     * convention is used so that parameter pushed onto the Java runtime stack
     * do not need to be reordered. The net result of this is that these methods
     * must not be called from regular Java code.
     */

    /**
     * Address of the start of the object memory in ROM.
     */
    private static Address romStart;

    /**
     * Address of the first byte after the end of the object memory in ROM.
     */
    private static Address romEnd;

    /**
     * The null terminated C string holding the name of the file from which ROM was loaded.
     */
    private static Address romFileName;

    /**
     * The hash of the object memory in ROM in it's canonical (i.e. relative to
     * address 0) form.
     */
    private static int romHash;

    /**
     * The verbosity level.
     */
    private static int verboseLevel;

    /**
     * Flag to say that synchronization is enabled.
     */
    private static boolean synchronizationEnabled;

    /**
     * Flag to say that exception handling is enabled.
     */
    private static boolean exceptionsEnabled;

    /**
     * Pointer to the preallocated OutOfMemoryError object.
     */
    private static OutOfMemoryError outOfMemoryError;

    /**
     * Pointer to the preallocated a VMBufferDecoder used by the do_throw code.
     */
    private static VMBufferDecoder vmbufferDecoder;

    /*
     * Create the isolate of the currently executing thread.
     */
    private static Isolate currentIsolate;

    /**
     * The next hashcode to be allocated.
     */
    private static int nextHashcode;

    /**
     * Allow Runtime.gc() to cause a collection.
     */
    private static boolean allowUserGC;

    /**
     * Flag to show if the extend bytecode can be executed.
     */
    static boolean extendsEnabled;

    /**
     * Flags if the VM was built with memory access type checking enabled.
     */
    private static boolean usingTypeMap;

    /**
     * The C array of the null terminated C strings representing the command line
     * arguments that will be converted to a String[] and passed to the {@link JavaApplicationManager}.
     */
    private static Address argv;

    /**
     * The number of elements in the {@link #argv} array.
     */
    private static int argc;

    /*=======================================================================*\
     *                          VM callback routines                         *
    \*=======================================================================*/

    /**
     * Squawk startup routine.
     *
     * @param bootstrapSuite        the bootstrap suite
     */
    static void do_startup(Suite bootstrapSuite) {

        /*
         * Set default for allowing Runtime.gc() to work.
         */
        VM.allowUserGC = true;

        /*
         * Initialize the garbage collector, suite manager then allocate a VMBufferDecoder
         * for use by the code in do_throw() and the OutOfMemoryError.
         */
        GC.initialize(bootstrapSuite);

        vmbufferDecoder  = new VMBufferDecoder();
        outOfMemoryError = new OutOfMemoryError();

        /*
         * Create the root isolate and manually initialize java.lang.Klass.
         */
        String[] args  = new String[argc];
        currentIsolate = new Isolate("java.lang.JavaApplicationManager", args, bootstrapSuite);
        currentIsolate.initializeClassKlass();

        /*
         * Initialise threading.
         */
        Thread.initializeThreading();
        synchronizationEnabled = true;

        /*
         * Fill in the args array with the C command line arguments.
         */
        GC.copyCStringArray(argv, args);

        /*
         * Start the isolate guarded with an exception handler. Once the isolate
         * has been stacted enter the service operation loop.
         */
        try {
            exceptionsEnabled = true;
            currentIsolate.primitiveThreadStart();
            ServiceOperation.execute();
        } catch (Throwable ex) {
            fatalVMError();
        }
    }

    /**
     * This is the native method that is called by the VM for native
     * declarations that are unsatisifed by the translator.
     *
     * @param id the identifier of the unknown native method
     */
    static void do_undefinedNativeMethod(int id) {
        throw new LinkageError("Undefined native method: " + id);
    }

    /**
     * Start running the current thread.
     */
    static void do_callRun() {
        Thread.currentThread().callRun();
    }

    /**
     * Read a static reference variable.
     *
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     * @return the value
     */
    static Object do_getStaticOop(Klass klass, int offset) {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        return Unsafe.getObject(ks, offset);
    }

    /**
     * Read a static int variable.
     *
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     * @return the value
     */
    static int do_getStaticInt(Klass klass, int offset) {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        return (int)Unsafe.getUWord(ks, offset).toPrimitive();
    }

    /**
     * Read a static long variable.
     *
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     * @return the value
     */
    static long do_getStaticLong(Klass klass, int offset) {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        return Unsafe.getLongAtWord(ks, offset);
    }

    /**
     * Write a static reference variable.
     *
     * @param value  the value
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     */
    static void do_putStaticOop(Object value, Klass klass, int offset) {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        Unsafe.setObject(ks, offset, value);
    }

    /**
     * Write a static int variable.
     *
     * @param value  the value
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     */
    static void do_putStaticInt(int value, Klass klass, int offset) {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        Unsafe.setUWord(ks, offset, UWord.fromPrimitive(value));
    }

    /**
     * Write a static long variable.
     *
     * @param value  the value
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     */
    static void do_putStaticLong(long value, Klass klass, int offset) {
        Object ks = currentIsolate.getClassStateForStaticVariableAccess(klass, offset);
        Unsafe.setLongAtWord(ks, offset, value);
    }

    /**
     * Optionally cause thread rescheduling.
     */
    static void do_yield() {
        Thread.yield();
    }

    /**
     * Throw a NullPointerException.
     */
    static void do_nullPointerException() {
        throw new NullPointerException();
    }

    /**
     * Throw an ArrayIndexOutOfBoundsException.
     */
    static void do_arrayIndexOutOfBoundsException() {
        throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * Throw an ArithmeticException.
     */
    static void do_arithmeticException() {
        throw new ArithmeticException();
    }

    /**
     * Set write barrier bit for the store of a reference to an array.
     *
     * @param array the array
     * @param index the array index
     * @param value the value to be stored into the array
     */
    static void do_arrayOopStore(Object array, int index, Object value) {
        Klass arrayKlass = GC.getKlass(array);
        Assert.that(arrayKlass.isArray());
        Assert.that(value != null);
        Klass componentType = arrayKlass.getComponentType();

        /*
         * Klass.isInstance() will not work before class Klass is initialized. Use the
         * synchronizationEnabled flag to show that the system is ready for this.
         */
        if (synchronizationEnabled == false || componentType.isInstance(value)) {
            Unsafe.setObject(array, index, value);
        } else {
            throw new ArrayStoreException();
        }
    }

    /**
     * Find the virtual slot number for an object that corresponds to the slot in an interface.
     *
     * @param obj the receiver
     * @param iklass the interface class
     * @param islot the virtual slot of the interface
     * @return the virtual slot of the receiver
     */
    static int do_findSlot(Object obj, Klass iklass, int islot) {
        Klass klass = GC.getKlass(obj);
        return klass.findSlot(iklass, islot);
    }

    /**
     * Synchronize on an object.
     *
     * @param oop the object
     */
    static void do_monitorenter(Object oop) {
        if (synchronizationEnabled) {
            Thread.monitorEnter(oop);
        }
    }

    /**
     * Desynchronize on an object.
     *
     * @param oop the object
     */
    static void do_monitorexit(Object oop) {
        if (synchronizationEnabled) {
            Thread.monitorExit(oop);
        }
    }

    /**
     * Test to see if an object is an instance of a class.
     *
     * @param obj the object
     * @param klass the class
     * @return true if is can
     */
    static boolean do_instanceof(Object obj, Klass klass) {
        return obj != null && klass != null && klass.isAssignableFrom(GC.getKlass(obj));
    }

    /**
     * Check that an object can be cast to a class.
     *
     * @param obj the object
     * @param klass the class
     * @return the same object
     * @exception ClassCastException if the case is illegal
     */
    static Object do_checkcast(Object obj, Klass klass) {
        if (obj != null && !klass.isAssignableFrom(GC.getKlass(obj))) {
            throw new ClassCastException();
        }
        return obj;
    }

    /**
     * Lookup the position of a value in a sorted array of numbers.
     *
     * @param key the value to look for
     * @param array the array
     * @return the index or -1 if the lookup fails
     */
    static int do_lookup_b(int key, byte[] array) {
        int low = 0;
        int high = array.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int val = array[mid];
            if (key < val) {
                high = mid - 1;
            } else if (key > val) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    /**
     * Lookup the position of a value in a sorted array of numbers.
     *
     * @param key the value to look for
     * @param array the array
     * @return the index or -1 if the lookup fails
     */
    static int do_lookup_s(int key, short[] array) {
        int low = 0;
        int high = array.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int val = array[mid];
            if (key < val) {
                high = mid - 1;
            } else if (key > val) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    /**
     * Lookup the position of a value in a sorted array of numbers.
     *
     * @param key the value to look for
     * @param array the array
     * @return the index or -1 if the lookup fails
     */
    static int do_lookup_i(int key, int[] array) {
        int low = 0;
        int high = array.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int val = array[mid];
            if (key < val) {
                high = mid - 1;
            } else if (key > val) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    /**
     * Initialize a class.
     *
     * @param klass the klass
     */
    static void do_class_clinit(Klass klass) {
        klass.initialiseClass();
    }

    /**
     * Allocate an instance.
     *
     * @param klass the klass of the instance
     * @return the new object
     * @exception OutOfMemoryException if allocation fails
     */
    static Object do_new(Klass klass) {
        klass.initialiseClass();
        return GC.newInstance(klass);
    }

    /**
     * Allocate an array.
     *
     * @param klass the klass of the instance
     * @param size  the element count
     * @return      the new array
     * @exception OutOfMemoryException if allocation fails
     */
    static Object do_newarray(int size, Klass klass) {
        return GC.newArray(klass, size);
    }

    /**
     * Allocate and add a new dimension to an array.
     *
     * @param array  the array
     * @param length the element count
     * @return the same array as input
     * @exception OutOfMemoryException if allocation fails
     */
    static Object do_newdimension(Object[] array, int length) {
        return newdimensionPrim(array, length);
    }

    /**
     * Execute the equivalent of the JVMS lcmp instruction.
     *
     * @param value1 the value1 operand
     * @param value2 the value2 operand
     * @return 0, 1, or -1 accorting to the spec
     */
    static int do_lcmp(long value1, long value2) {
        if (value1 > value2) {
            return 1;
        }
        if (value1 == value2) {
            return 0;
        }
        return -1;
    }

/*if[FLOATS]*/

    /**
     * Execute the equivalent of the JVMS fcmpl instruction.
     *
     * @param value1 the value1 operand
     * @param value2 the value2 operand
     * @return 0, 1, or -1 accorting to the spec
     */
    static int do_fcmpl(float value1, float value2) {
        if (value1 > value2) {
            return 1;
        }
        if (value1 == value2) {
            return 0;
        }
        return -1;
    }

    /**
     * Execute the equivalent of the JVMS fcmpg instruction.
     *
     * @param value1 the value1 operand
     * @param value2 the value2 operand
     * @return 0, 1, or -1 accorting to the spec
     */
    static int do_fcmpg(float value1, float value2) {
        if (value1 < value2) {
            return -1;
        }
        if (value1 == value2) {
            return 0;
        }
        return 1;
    }

    /**
     * Execute the equivalent of the JVMS dcmpl instruction.
     *
     * @param value1 the value1 operand
     * @param value2 the value2 operand
     * @return 0, 1, or -1 accorting to the spec
     */
    static int do_dcmpl(double value1, double value2) {
        if (value1 > value2) {
            return 1;
        }
        if (value1 == value2) {
            return 0;
        }
        return -1;
    }

    /**
     * Execute the equivalent of the JVMS dcmpg instruction.
     *
     * @param value1 the value1 operand
     * @param value2 the value2 operand
     * @return 0, 1, or -1 accorting to the spec
     */
    static int do_dcmpg(double value1, double value2) {
        if (value1 < value2) {
            return -1;
        }
        if (value1 == value2) {
            return 0;
        }
        return 1;
    }

/*end[FLOATS]*/

    /**
     * Allocate and add a new dimension to an array.
     *
     * @param array   the array
     * @param length  the element count
     * @return the same array as input
     * @exception OutOfMemoryException if allocation fails
     */
    private static Object newdimensionPrim(Object[] array, int length) {
        Klass arrayClass   = GC.getKlass(array);
        Klass elementClass = arrayClass.getComponentType();
        for (int i = 0 ; i < array.length ; i++) {
            if (array[i] == null) {
                array[i] = GC.newArray(elementClass, length);
            } else {
                newdimensionPrim((Object[])array[i], length);
            }
        }
        return array;
    }

    /*-----------------------------------------------------------------------*\
     *                      Service thread operations                        *
    \*-----------------------------------------------------------------------*/


    /**
     * Throw an exception. This routine will search for the exception handler for the
     * exception being thrown, reset the return ip and fp of the activation record that
     * it was called with and then 'return' to the handler in question.
     */
    static void throwException(Throwable exception) {
        Object otherStack = Thread.getOtherThreadStack();

        /*
         * Check that exceptions are enabled and then disable them.
         */
        if (exceptionsEnabled) {
            exceptionsEnabled = false;
        } else {
            Assert.shouldNotReachHere("do_throw called recursively");
        }

        /*
         * Check that no memory allocation is done in this routine because
         * this function must be able to function in out-of-memory conditions.
         */
        boolean oldState = GC.setAllocationEnabled(false);

        /*
         * Get the class of the exception being thrown.
         */
        Klass exceptionKlass = GC.getKlass(exception);

        /*
         * Get the fp, ip, mp, and relative ip of the frame before the
         * one that is currently executing.
         */
        Address fp = Address.fromObject(Unsafe.getObject(otherStack, SC.lastFP));
        UWord relip  = Unsafe.getUWord(otherStack, SC.lastIP);
        Object mp  = getMP(fp);

        /*
         * Loop looking for an exception handler. (The VM must put a catch-all
         * handler at the base of all user thread activations.)
         */
        while(true) {

            /*
             * Setup the preallocated VMBufferDecoder to decode the header
             * of the method for the frame being tested.
             */
            int size   = MethodBody.decodeExceptionTableSize(mp);
            int offset = MethodBody.decodeExceptionTableOffset(mp);
            vmbufferDecoder.reset(mp, offset);
            int end = offset + size;

            UWord start_ip;
            UWord end_ip;
            UWord handler_ip;

            /*
             * Iterate through the handlers for this method.
             */
            while (vmbufferDecoder.getOffset() < end) {
                start_ip    = UWord.fromPrimitive(vmbufferDecoder.readUnsignedInt());
                end_ip      = UWord.fromPrimitive(vmbufferDecoder.readUnsignedInt());
                handler_ip  = UWord.fromPrimitive(vmbufferDecoder.readUnsignedInt());
                int handler_cid = vmbufferDecoder.readUnsignedInt();

                /*
                 * If the ip and exception matches then setup the activation
                 * for this routine so that the return will go back to the
                 * handler code.
                 *
                 * Note that the relip address is now passed the instruction that
                 * caused the call. Therefore the match is > start_ip && <= end_ip
                 * rather than >= start_ip && < end_ip.
                 */
                if (relip.hi(start_ip) && relip.loeq(end_ip)) {
                    boolean match;
                    if (exceptionKlass.getClassID() == handler_cid || handler_cid == CID.THROWABLE) {
                        // Fast case
                        match = true;
                    } else {
                        Klass handerKlass = SuiteManager.getKlass(handler_cid);
                        match = handerKlass.isAssignableFrom(exceptionKlass);
                    }
                    if (match) {
                        Unsafe.setAddress(otherStack, SC.lastFP, fp);
                        Unsafe.setUWord(otherStack, SC.lastIP, handler_ip);
                        GC.setAllocationEnabled(oldState);
                        exceptionsEnabled = true;
                        return;
                    }
                }
            }

            /*
             * Backup to the previous frame and loop.
             */
            Address ip = getPreviousIP(fp);
            fp         = getPreviousFP(fp);
            Assert.that(!fp.isZero());
            mp = getMP(fp);
            relip = ip.diff(Address.fromObject(mp)).toUWord();
        }
    }


    /*-----------------------------------------------------------------------*\
     *                      Floating point convertions                       *
    \*-----------------------------------------------------------------------*/

/*if[FLOATS]*/
    /**
     * Perform a math operation.
     *
     * @param code  the opcode
     * @param a     the first operand
     * @param b     the second operand
     * @return the result
     */
    native static double math(int code, double a, double b);

    /**
     * Convert a float into bits.
     *
     * @param value the input
     * @return the result
     */
    native static int floatToIntBits(float value);

    /**
     * Convert a double into bits.
     *
     * @param value the input
     * @return the result
     */
    native static long doubleToLongBits(double value);

    /**
     * Convert bits into a float.
     *
     * @param value the input
     * @return the result
     */
    native static float intBitsToFloat(int value);

    /**
     * Convert bits into a double.
     *
     * @param value the input
     * @return the result
     */
    native static double longBitsToDouble(long value);

/*end[FLOATS]*/

    /*=======================================================================*\
     *                           Romizer support                             *
    \*=======================================================================*/

    /**
     * Determines if the Squawk system is being run in a hosted environment
     * such as the romizer or mapper application.
     *
     * @return true if the Squawk system is being run in a hosted environment
     */
    public static boolean isHosted() {
        return false;
    }

    /**
     * Get the endianess.
     *
     * @return true if the system is big endian
     */
    public static native boolean isBigEndian();


    /*=======================================================================*\
     *                              Native methods                           *
    \*=======================================================================*/

    /*-----------------------------------------------------------------------*\
     *                           Raw memory interface                        *
    \*-----------------------------------------------------------------------*/

    /**
     * Get the current frame pointer.
     *
     * @return the frame pointer
     */
    native static Address getFP();

    /**
     * Get the method pointer from a frame pointer.
     *
     * @param fp the frame pointer
     * @return the method pointer
     */
    native static Object getMP(Address fp);

    /**
     * Get the previous frame pointer from a frame pointer.
     *
     * @param fp the frame pointer
     * @return the previous frame pointer
     */
    native static Address getPreviousFP(Address fp);

    /**
     * Get the previous instruction pointer from a frame pointer.
     *
     * @param fp the frame pointer
     * @return the previous instruction pointer
     */
    native static Address getPreviousIP(Address fp);

    /**
     * Set the previous frame pointer.
     *
     * @param fp the frame pointer
     * @param pfp the previous frame pointer
     */
    native static void setPreviousFP(Address fp, Address pfp);

    /**
     * Set the previous instruction pointer.
     *
     * @param fp the frame pointer
     * @param pip the previous instruction pointer
     */
    native static void setPreviousIP(Address fp, Address pip);

    /*-----------------------------------------------------------------------*\
     *                          Oop/int convertion                           *
    \*-----------------------------------------------------------------------*/

    /**
     * Casts an object to class Klass without using <i>checkcast</i>.
     *
     * @param object the object to be cast
     * @return the object cast to be a Klass
     */
    native static Klass asKlass(Object object);

    /**
     * Casts an object to class Klass without using <i>checkcast</i>.
     *
     * @param object the object to be cast
     * @return the object cast to be a Klass
     */
    native static Thread asThread(Object object);

    /**
     * Get the hash code for an object in ROM
     *
     * @param   anObject the object
     * @return  the hash code
     */
    native static int hashcode(Object anObject);

    /**
     * Allocate a segment of virtual memory to be used as a stack.
     * If sucessfull the memory returnd is such that the all but
     * the second page is accessable. The MMU is setup to disable access
     * to the second page in order to provide a guard for accidential
     * stack overflow.
     *
     * @param   size the size of the memory
     * @return  the allocated memory or zero if none was available
     */
    native static Address allocateVirtualStack(int size);

    /**
     * Add to the VM's class state cache
     *
     * @param   klass the class
     * @param   state the class state
     */
    native static void addToClassStateCache(Object klass, Object state);

    /**
     * Invalidate the class cache.
     *
     * @return true if it was already invalid.
     */
    native static boolean invalidateClassStateCache();

    /**
     * Removes the oldest object that is pending a monitor enter operation.
     *
     * @return the next object
     */
    native static Object removeVirtualMonitorObject();

    /**
     * Tests to see if an object has a virtual monitor object.
     *
     * @param object the object
     * @return true if is does
     */
    native static boolean hasVirtualMonitorObject(Object object);

    /**
     * Execute the equivalent of the lcmp instruction on page 312 of the JVMS.
     *
     * @param value1 the value1 operand
     * @param value2 the value2 operand
     * @return 0, 1, or -1 accorting to the spec
     */
    //native static int lcmp(long value1, long value2);

    /*-----------------------------------------------------------------------*\
     *                       Access to global memory                         *
    \*-----------------------------------------------------------------------*/

    /*
     * The Squawk VM supports four types of variables. These are local
     * variables, instance variables, static varibles, and global variables.
     * Static variables are those defined in a class using the static keyword
     * these are allocated dynamically by the VM when their classes are
     * initialized, and these variables are created on a per-isolate basis.
     * Global variables are allocated by the romizer and used in place of
     * the static variables in a hard-wired set of system classes. This is
     * done in cases where certain components of the system must have static
     * state before the normal system that support things like static variables
     * are running. Global variables are shared between all isolates.
     * <p>
     * The classes java.lang.VM and java.lang.GC are included in the hard-wired
     * list and the translator will resplace the normal getstatic and putstatic
     * bytecodes will invokenative instructions that one of the following.
     * Currently only 32/64 bit references and 32 bit integers are supported.
     * <p>
     * Because the transformation is done automatically there is little evidence
     * of the following routines being used in the system code. One exception to
     * this is the garbage collector which will need to treat all the reference
     * types as roots.
     */

    /**
     * Gets the number of global integer variables.
     *
     * @return  the number of global integer variables
     */
    native static int getGlobalIntCount();

    /**
     * Gets the value of an global integer variable.
     *
     * @param  index   index of the entry in the global integer table
     * @return the value of entry <code>index</code> in the global integer table
     */
    native static int getGlobalInt(int index);

    /**
     * Sets the value of an global integer variable.
     *
     * @param  value   the value to set
     * @param  index   index of the entry to update in the global integer table
     */
    native static void setGlobalInt(int value, int index);

    /**
     * Gets the number of global pointer variables.
     *
     * @return  the number of global pointer variables
     */
    native static int getGlobalAddrCount();

    /**
     * Gets the value of an global pointer variable.
     *
     * @param  index   index of the entry in the global pointer table
     * @return the value of entry <code>index</code> in the global pointer table
     */
    native static Address getGlobalAddr(int index);

    /**
     * Sets the value of an global pointer variable.
     *
     * @param  value   the value to set
     * @param  index   index of the entry to update in the global pointer table
     */
    native static void setGlobalAddr(Address value, int index);

    /**
     * Gets the number of global object pointer variables.
     *
     * @return  the number of global object pointer variables
     */
    native static int getGlobalOopCount();

    /**
     * Gets the value of an global object pointer variable.
     *
     * @param  index   index of the entry in the global object pointer table
     * @return the value of entry <code>index</code> in the global object pointer table
     */
    native static Object getGlobalOop(int index);

    /**
     * Sets the value of an global object pointer variable.
     *
     * @param  value   the value to set
     * @param  index   index of the entry to update in the global object pointer table
     */
    native static void setGlobalOop(Object value, int index);

    /**
     * Gets the address of the global object pointer table.
     *
     * @return  the address of the global object pointer table
     */
    native static Address getGlobalOopTable();


    /*-----------------------------------------------------------------------*\
     *                        Low level VM logging                           *
    \*-----------------------------------------------------------------------*/

    /**
     * The identifier denoting the standard output stream.
     */
    public static final int STREAM_STDOUT = 0;

    /**
     * The identifier denoting the standard error output stream.
     */
    public static final int STREAM_STDERR = 1;

    /**
     * The identifier denoting the stream used to capture the symbolic information
     * relating to methods in dynamically loaded classes.
     */
    static final int STREAM_SYMBOLS = 2;

    /**
     * Sets the stream for the VM.print... methods to one of the STREAM_... constants.
     *
     * @param stream  the stream to use for the print... methods
     * @return the current stream used for VM printing
     */
    public static int setStream(int stream) {
        Assert.always(stream >= STREAM_STDOUT && stream <= STREAM_SYMBOLS, "invalid stream specifier");
        return setStream0(stream);
    }

    /**
     * Sets the stream for the VM.print... methods to one of the STREAM_... constants.
     *
     * @param stream  the stream to use for the print... methods
     * @return the current stream used for VM printing
     */
    private static int setStream0(int stream) {
        return execIO(ChannelConstants.INTERNAL_SETSTREAM, stream);
    }

    /**
     * Prints a character to the VM stream.
     *
     * @param ch      the character to print
     */
    static void printChar(char ch) {
        execIO(ChannelConstants.INTERNAL_PRINTCHAR, ch);
    }

    /**
     * Prints an integer to the VM stream.
     *
     * @param val     the integer to print
     */
    static void printInt(int val) {
        execIO(ChannelConstants.INTERNAL_PRINTINT, val);
    }

    /**
     * Prints a long to the VM stream.
     *
     * @param val     the long to print
     */
    static void printLong(long val) {
        int i1 = (int)(val >>> 32);
        int i2 = (int)val;
        execIO(ChannelConstants.INTERNAL_PRINTLONG, i1, i2);
    }

    /**
     * Prints an unsigned word to the VM stream. This will be formatted as an unsigned 32 bit or 64 bit
     * value depending on the underlying platform.
     *
     * @param val     the word to print
     */
    static void printUWord(UWord val) {
        int i1 = (int)(val.toPrimitive() >> 32);
        int i2 = (int)val.toPrimitive();
        execIO(ChannelConstants.INTERNAL_PRINTUWORD, i1, i2);
    }

    /**
     * Prints an offset to the VM stream. This will be formatted as a signed 32 bit or 64 bit
     * value depending on the underlying platform.
     *
     * @param val     the offset to print
     */
    static void printOffset(Offset val) {
        int i1 = (int)(val.toPrimitive() >> 32);
        int i2 = (int)val.toPrimitive();
        execIO(ChannelConstants.INTERNAL_PRINTOFFSET, i1, i2);
    }

    /**
     * Prints a string to the VM stream.
     *
     * @param str     the string to print
     */
    static void printString(String str) {
        executeCIO(-1, ChannelConstants.INTERNAL_PRINTSTRING, -1, 0, 0, 0, 0, 0, 0, str, null);
    }

    /**
     * Prints an address to the VM stream. This will be formatted as an unsigned 32 bit or 64 bit
     * value depending on the underlying platform.
     *
     * @param val     the address to print
     */
    static void printAddress(Object val) {
        executeCIO(-1, ChannelConstants.INTERNAL_PRINTADDRESS, -1, 0, 0, 0, 0, 0, 0, val, null);
    }

    /**
     * Prints an address to the VM stream. This will be formatted as an unsigned 32 bit or 64 bit
     * value depending on the underlying platform.
     *
     * @param val     the address to print
     */
    static void printAddress(UWord val) {
        printUWord(val);
    }

    /**
     * Prints an address to the VM stream. This will be formatted as an unsigned 32 bit or 64 bit
     * value depending on the underlying platform.
     *
     * @param val     the address to print
     */
    static void printAddress(Address val) {
        printAddress(val.toUWord());
    }

    /**
     * Prints the name of a global oop to the VM stream.
     *
     * @param index   the index of the variable to print
     */
    static void printGlobalOopName(int index) {
        execIO(ChannelConstants.INTERNAL_PRINTGLOBALOOPNAME, index);
    }

    /**
     * Prints the name and current value of every global to the VM stream.
     */
    static void printGlobals() {
        execIO(ChannelConstants.INTERNAL_PRINTGLOBALS, 0);
    }

    /**
     * Prints a line detailing the build-time configuration of the VM.
     */
    static void printConfiguration() {
        execIO(ChannelConstants.INTERNAL_PRINTCONFIGURATION, 0);
    }

    /**
     * Prints the string representation of an object to the VM stream.
     *
     * @param obj   the object whose toString() result is to be printed
     */
    public static void printObject(Object obj) {
        print(String.valueOf(obj));
    }

    public static void print(char x)       { printChar(x); }
    public static void print(String x)     { printString(x); }
    public static void print(int x)        { printInt(x); }
    public static void print(long x)       { printLong(x); }
    public static void print(boolean b)    { print(b ? "true" : "false"); }

    public static void println(char x)     { printChar(x); println(); }
    public static void println(String x)   { printString(x); println(); }
    public static void println(int x)      { printInt(x); println(); }
    public static void println(boolean x)  { print(x); println(); }
    public static void println(long x)     { printLong(x); println(); }

    public static void println()           { print("\n"); }


    /*-----------------------------------------------------------------------*\
     *                        Miscellaneous functions                        *
    \*-----------------------------------------------------------------------*/

    /**
     * A call to this method is replaced by the translator when a call to an undefined
     * native method if found.
     */
    static void undefinedNativeMethod() {
        throw new LinkageError("Undefined native method");
    }

    /**
     * Sets the write barrier for pointer updates in the old generation of a generational garbage collector.
     *
     * @param bitmap       the address at which memory has been allocated for the bitmap
     * @param size         the size (in bytes) of the memory allocated
     * @param base         the logical address at which would start if it contained a bit for address 0
     */
//    static native void setWriteBarrier(Address bitmap, int size, Address base);

    /**
     * Gets the hash of the object memory in ROM in it's canonical (i.e. relative to
     * address 0) form.
     *
     * @return the hash of the ROM
     */
    static int getRomHash() {
        return romHash;
    }

    /**
     * Gets the address of the start of the object memory in ROM.
     *
     * @return the address of the start of the object memory in ROM
     */
    static Address getRomStart() {
        return romStart;
    }

    /**
     * Gets the address of the first byte after the end of the object memory in ROM.
     *
     * @return the address of the first byte after the end of the object memory in ROM
     */
    static Address getRomEnd() {
        return romEnd;
    }

    /**
     * Determines if a given object is within ROM.
     *
     * @param object   the object to test
     * @return true if <code>object</code> is within ROM
     */
    static boolean inRom(Address object) {
        /*
         * Need to account for the object's header on the low
         * end and zero sized objects on the high end
         */
        return object.hi(romStart) && object.loeq(romEnd);
    }

    /**
     * Gets the offset (in bytes) of an object in ROM from the start of ROM
     *
     * @param object  the object to test
     * @return the offset (in bytes) of <code>object</code> in ROM from the start of ROM
     */
    static Offset getOffsetInRom(Address object) {
        Assert.that(inRom(object));
        return object.diff(romStart);
    }

    /**
     * Gets an object in ROM given a offset (in bytes) to the object from the start of ROM.
     *
     * @param offset   the offset (in bytes) of the object to retrieve
     * @return the object at <code>offset</code> bytes from the start of ROM
     */
    static Address getObjectInRom(Offset offset) {
        return romStart.addOffset(offset);
    }

    /**
     * Return the address of the null terminated ASCII C string that is the name of the file
     * from which the ROM's object memory was loaded.
     *
     * @return the ROM's file name
     */
    static Address getRomFileName() {
        return romFileName;
    }

    /**
     * The system-dependent path-separator character. This character is used to
     * separate filenames in a sequence of files given as a <em>path list</em>.
     * On UNIX systems, this character is <code>':'</code>; on Windows
     * systems it is <code>';'</code>.
     *
     * @return  the system-dependent path-separator character
     */
    public static char getPathSeparatorChar() {
        return (char)execIO(ChannelConstants.INTERNAL_GETPATHSEPARATORCHAR, 0);
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
        return (char)execIO(ChannelConstants.INTERNAL_GETFILESEPARATORCHAR, 0);
    }

    /**
     * Stop the VM working because of a fatal condition.
     */
    public native static void fatalVMError();

    /**
     * Switch from executing the Thread.currentThread to Thread.otherThread. This operation
     * will cause these two variables to be swapped, and the execution to continue after
     * the threadSwitch() of the next thread. This function also sets up VM.currentIsolate
     * when Thread.otherThread is not the service thread.
     * <p>
     * If Thread.otherThread is a new thread the method VMExtension.callrun() to be entered.
     */
    native static void threadSwitch();

    /**
     * Execute a channel I/O operation.
     *
     * @param context   the I/O context
     * @param op        the opcode
     * @param channel   the channel number
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      an outgoing reference parameter
     * @param receive   an incoming reference parameter (i.e. an array of some type)
     */
    private native static void executeCIO(int context, int op, int channel, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive);

    /**
     * Execute a garbabe collection operation.
     */
    private native static void executeGC();

    /**
     * Execute a garbabe collection operation.
     *
     * @param firstPass 1 if this is the first pass else 0
     * @param object    the root of the object graph to copy
     * @param cb        the ObjectMemorySerializer.ControlBlock
     */
    private native static void executeCOG(int firstPass, Object object, ObjectMemorySerializer.ControlBlock cb);

    /**
     * Get the result of the last service operation.
     */
    private native static int serviceResult();

    /**
     * Gets the number of backward branch instructions the VM has executed.
     *
     * @return the number of backward branch instructions the VM has executed or -1 if instruction
     *         profiling is disabled
     */
    native static long getBranchCount();

    /**
     * Enables a dynamically loaded class to call this.
     *
     * @return the number of instructions the VM has executed or -1 if instruction
     *         profiling is disabled
     */
    public static long branchCount() {
        return getBranchCount();
    }

    /**
     * Gets the flag indicating if the VM is running in verbose mode.
     *
     * @return true if the VM is running in verbose mode
     */
    public static boolean isVerbose() {
        return verboseLevel > 0;
    }

    /**
     * Gets the flag indicating if the VM is running in very verbose mode.
     *
     * @return true if the VM is running in very verbose mode
     */
    public static boolean isVeryVerbose() {
        return verboseLevel > 1;
    }

    /**
     * Sets the flag indicating if the VM is running in verbose mode.
     *
     * @param level  indicates if the VM should run in verbose mode
     */
    static void setVerboseLevel(int level) {
        verboseLevel = level;
    }

    /**
     * Create a Channel I/O context.
     *
     * @param hibernatedContext the handle for a hibernated I/O session
     * @return the channel I/O context
     */
    static int createChannelContext(int hibernatedContext) {
        return execIO(ChannelConstants.GLOBAL_CREATECONTEXT, hibernatedContext);
    }

    /**
     * Delete a channel I/O context.
     *
     * @param context the channel I/O context
     */
    static void deleteChannelContext(int context) {
        execIO(context, ChannelConstants.GLOBAL_DELETECONTEXT, 0, 0);
    }

    /**
     * Hibernate a channel context.
     *
     * @param context the channel I/O handle
     * @return        the positive identifier of the hibernated context or a negative error code
     */
    static int hibernateChannelContext(int context) {
        return execIO(context, ChannelConstants.GLOBAL_HIBERNATECONTEXT, 0, 0);
    }

    /**
     * Get the current time.
     *
     * @return the time in milliseconds
     */
    static long getTime() {
        long high = execIO(ChannelConstants.INTERNAL_GETTIME_HIGH, 0); // Must get high word first.
        long low  = execIO(ChannelConstants.INTERNAL_GETTIME_LOW, 0);
        return (high << 32) | (low & 0x00000000FFFFFFFFL);
    }

    /**
     * Poll for a completed event.
     *
     * @return the event number or zero for none
     */
    static int getEvent() {
        return execIO(ChannelConstants.GLOBAL_GETEVENT, 0);
    }

    /**
     * Pause execution until an event occurs.
     *
     * @param time the maximum time to wait
     */
    static void waitForEvent(long time) {
        int low  = (int)time;
        int high = (int)(time>>32);
        execIO(ChannelConstants.GLOBAL_WAITFOREVENT, high, low);
    }

    /**
     * Switch to the special garbage collection stack and call 'GC.collectGarbage()'
     */
    static void collectGarbage() {
        executeGC();
    }

    /**
     * Make a copy of the object graph in RAM rooted at a given object.
     *
     * @param object    the root of the object graph to copy
     * @return the ObjectMemorySerializer.ControlBlock instance that contains the serialized object graph and
     *                  its metadata. This will be null if there was insufficient memory
     *                  to do the serialization.
     */
    static ObjectMemorySerializer.ControlBlock copyObjectGraph(Object object) {
        Assert.always(GC.inRam(object));

        /*
         * Free up as much memory as possible.
         */
        collectGarbage();

        try {
            ObjectMemorySerializer.ControlBlock cb = new ObjectMemorySerializer.ControlBlock();

            // Count the potential number of objects that will be copied to
            // calculate the size of the map required by the Cheney collector
            // to record the original class word of objects that were forwarded
            int forwardingRepairMapSize = GC.countObjectsInRamAllocationSpace() * 2 * HDR.BYTES_PER_WORD;
            cb.memory = GC.newArray(Klass.BYTE_ARRAY, forwardingRepairMapSize);
            cb.size = forwardingRepairMapSize;

            /*
             * A GC must not occur between the 2 phases of the object graph copying as
             * the second phase assumes that the heap is in the same state as it was
             * left in by the first phase.
             */
            boolean state = GC.setGCEnabled(false);
            try {
                // Do the first pass to determine the size of the copied graph
                executeCOG(1, object, cb);

                // Set up the rest of the control block based on the calculated size
                byte[] bits = new byte[GC.calculateOopMapSizeInBytes(cb.size)];
                cb.oopMap = new com.sun.squawk.util.BitSet(bits);

                cb.memory = GC.newArray(Klass.BYTE_ARRAY, cb.size);

                // Do the second pass to copy the graph
                executeCOG(0, object, cb);
            } finally {
                GC.setGCEnabled(state);
            }

            return cb;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * Halt the VM in the normal way.
     *
     * @param   code the exit status code.
     */
    static void stopVM(int code) {
        execIO(ChannelConstants.INTERNAL_STOPVM, code);
    }

    /**
     * Copy memory from one array to another.
     *
     * @param      src          the source array.
     * @param      srcPos       start position in the source array.
     * @param      dst          the destination array.
     * @param      dstPos       start position in the destination data.
     * @param      length       the number of array elements to be copied.
     * @param      nvmDst       the destination buffer is in NVM
     */
    static void copyBytes(Object src, int srcPos, Object dst, int dstPos, int length, boolean nvmDst) {
        Assert.that(src != null && dst != null);
        executeCIO(-1, ChannelConstants.INTERNAL_COPYBYTES, -1, length, srcPos, dstPos, nvmDst ? 1 : 0, 0, 0, src, dst);
    }

    /**
     * Allocate a chunk of zeroed memory from RAM.
     *
     * @param   size        the length in bytes of the object and its header (i.e. the total number of
     *                      bytes to be allocated).
     * @param   klass       the class of the object being allocated
     * @param   arrayLength the number of elements in the array being allocated or -1 if a non-array
     *                      object is being allocated
     * @return a pointer to a well-formed object or null if the allocation failed
     */
    native static Object allocate(int size, Object klass, int arrayLength);

    /**
     * Zero a word-aligned block of memory.
     *
     * @param      start        the start address of the memory area
     * @param      end          the end address of the memory area
     */
    native static void zeroWords(Object start, Object end);

    /**
     * Fill a block of memory with the 0xDEADBEEF pattern.
     *
     * @param      start        the start address of the memory area
     * @param      end          the end address of the memory area
     */
    native static void deadbeef(Object start, Object end);

    /**
     * Call a static method.
     *
     * @param cls  the klass of the method
     * @param slot the offset into the static vtable
     */
    native static void callStaticNoParm(Klass cls, int slot);

    /**
     * Call a static method passing a single parameter.
     *
     * @param parm the parameter
     * @param cls  the klass of the method
     * @param slot the offset into the static vtable
     */
    native static void callStaticOneParm(Klass cls, int slot, Object parm);

    /**
     * Get the sentinal OutOfMemoryException object
     *
     * @return the object
     */
    static OutOfMemoryError getOutOfMemoryError() {
        return outOfMemoryError;
    }

    /*-----------------------------------------------------------------------*\
     *                                   I/O                                 *
    \*-----------------------------------------------------------------------*/

    /**
     * Gets the exception message for  the last channel IO operation.
     *
     * @param context the channel context
     * @return the message
     */
    private static String getExceptionMessage(int context) {
        StringBuffer sb = new StringBuffer();
        for (;;) {
            char ch = (char)execIO(context, ChannelConstants.CONTEXT_GETERROR, 0, 0);
            if (ch == 0) {
                return sb.toString();
            }
            sb.append(ch);
        }
    }

    /**
     * Raises an exception that occurred in the last channel IO operation.
     *
     * @param context the channel context
     * @throws IOException
     */
    private static void raiseChannelException(int context) throws IOException {
        String name = getExceptionMessage(context);
        Object exception = null;
        try {
            Class exceptionClass = Class.forName(name);
            try {
                exception = exceptionClass.newInstance();
            } catch (IllegalAccessException ex1) {
            } catch (InstantiationException ex1) {
            }
        } catch (ClassNotFoundException ex) {
        }
        if (exception != null) {
            if (exception instanceof IOException) {
                throw (IOException)exception;
            } else if (exception instanceof RuntimeException) {
                throw (RuntimeException)exception;
            } else if (exception instanceof Error) {
                throw (Error)exception;
            }
        }
        throw new IOException(name);
    }

    /**
     * Execute an I/O operation and return the result.
     *
     * @param  op        the opcode
     * @param  i1        an integer parameter
     * @return           the result status
     */
    static int execIO(int op, int i1) {
        executeCIO(-1, op, -1, i1, 0, 0, 0, 0, 0, null, null);
        return serviceResult();
    }

    /**
     * Execute an I/O operation and return the result.
     *
     * @param  op        the opcode
     * @param  i1        an integer parameter
     * @param  i2        an integer parameter
     * @return           the result status
     */
    static int execIO(int op, int i1, int i2) {
        executeCIO(-1, op, -1, i1, i2, 0, 0, 0, 0, null, null);
        return serviceResult();
    }


    /**
     * Execute an I/O operation and return the result.
     *
     * @param  op        the opcode
     * @param  i1        an integer parameter
     * @param  i2        an integer parameter
     * @return           the result status
     */
    static int execIO(int cio, int op, int i1, int i2) {
        executeCIO(cio, op, -1, i1, i2, 0, 0, 0, 0, null, null);
        return serviceResult();
    }

    /**
     * Execute an I/O operation and return the result.
     *
     * @param op        the opcode
     * @param channel   the channel number
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      an outgoing array parameter
     * @param receive   an incoming array parameter
     * @return the event code to wait on or zero
     */
    public static int execIO(int op, int channel, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive) throws IOException {
        int context = currentIsolate.getChannelContext();
        if (context == 0) {
            throw new IOException("No native I/O peer for isolate");
        }
        for (;;) {
            executeCIO(context, op, channel, i1, i2, i3, i4, i5, i6, send, receive);
            int result = serviceResult();
            if (result == ChannelConstants.RESULT_OK) {
                return execIO(context, ChannelConstants.CONTEXT_GETRESULT, 0, 0);
            } else if (result < 0) {
                if (result == ChannelConstants.RESULT_EXCEPTION) {
                    raiseChannelException(context);
                }
                throw new IOException("Bad result from cioExecute "+ result);
            } else {
                Thread.waitForEvent(result);
                context = currentIsolate.getChannelContext(); // Must reload in case of hibernation.
            }
        }
    }

    /**
     * Execute an I/O operation and return the result.
     *
     * @param op        the opcode
     * @param channel   the channel identifier
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      a outgoing reference parameter
     * @param receive   an incoming reference parameter (i.e. an array of some type)
     * @return          the long result
     */
    public static long execIOLong(int op, int channel, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive) throws IOException {
        long low     = execIO(op, channel, i1, i2, i3, i4, i5, i6, send, receive);
        int  context = currentIsolate.getChannelContext();
        long high    = execIO(context, ChannelConstants.CONTEXT_GETRESULT_2, 0, 0);
        return (high << 32) | (low & 0x00000000FFFFFFFFL);
    }

    /**
     * Execute an I/O operation on the graphics channel and return the result.
     *
     * @param op        the opcode
     * @param i1        an integer parameter
     * @param i2        an integer parameter
     * @param i3        an integer parameter
     * @param i4        an integer parameter
     * @param i5        an integer parameter
     * @param i6        an integer parameter
     * @param send      a outgoing reference parameter
     * @param receive   an incoming reference parameter (i.e. an array of some type)
     * @return the event code to wait on or zero
     */
    public static int execGraphicsIO(int op, int i1, int i2, int i3, int i4, int i5, int i6, Object send, Object receive) {
        try {
            int chan = currentIsolate.getGuiOutputChannel();
            return execIO(op, chan, i1, i2, i3, i4, i5, i6, send, receive);
        } catch(IOException ex) {
            throw new RuntimeException("Error executing graphics channel: " + ex);
        }
    }

    /**
     * Execute an I/O operation on the GUI input channel and return the result.
     *
     * @return the GUI event value
     */
    public static long getGUIEvent() {
        try {
            int chan = currentIsolate.getGuiInputChannel();
            return VM.execIOLong(ChannelConstants.READLONG, chan, 0, 0, 0, 0, 0, 0, null, null);
        } catch(IOException ex) {
            throw new RuntimeException("Error executing event channel: " + ex);
        }
    }

    /**
     * Get a channel.
     *
     * @param type the channel type
     * @return a new channel number
     */
    public static int getChannel(int type) throws IOException {
        int context = currentIsolate.getChannelContext();
        if (context == 0) {
            throw new IOException("no native I/O peer for isolate");
        }
        return execIO(context, ChannelConstants.CONTEXT_GETCHANNEL, type, 0);
    }

    /*
     * Free a channel.
     *
     * @param channel the channel identifier to free
     */
    public static void freeChannel(int channel) throws IOException {
        int context = currentIsolate.getChannelContext();
        if (context == 0) {
            throw new IOException("no native I/O peer for isolate");
        }
        executeCIO(context, ChannelConstants.CONTEXT_FREECHANNEL, channel, 0, 0, 0, 0, 0, 0, null, null);
    }

    /*=======================================================================*\
     *                           Core VM functions                           *
    \*=======================================================================*/


    /**
     * Enable or disable Runtime.gc()
     *
     * @param value true to enable
     */
    public static void allowUserGC(boolean value) {
         allowUserGC = value;
    }

    /**
     * Tests if Runtime.gc() is allowed.
     *
     * @return true if calls to Runtime.gc() are allowed
     */
    public static boolean userGCAllowed() {
         return allowUserGC;
    }

    /**
     * Determines if the VM was built with memory access type checking enabled.
     *
     * @return true if the VM was built with memory access type checking enabled
     */
    public static boolean usingTypeMap() {
        return usingTypeMap;
    }

    /**
     * Gets the next available hashcode.
     *
     * @return the hashcode
     */
    public static int getNextHashcode() {
        do {
            nextHashcode++;
        } while (nextHashcode == 0);
        return nextHashcode;
    }

    /**
     * Gets the isolate of the currently executing thread.
     *
     * @return the isolate
     */
    public static Isolate getCurrentIsolate() {
        return currentIsolate;
    }

    /**
     * Determines if the current isolate is set and initialized.
     *
     * @return true if the current isolate is set and initialized
     */
    public static boolean isCurrentIsolateInitialized() {
        return currentIsolate != null && currentIsolate.isClassKlassInitialized();
    }

    /**
     * Determines if the threading system is initialized.
     *
     * @return true if the threading system is initialized.
     */
    public static boolean isThreadingInitialized() {
        return synchronizationEnabled;
    }

    /**
     * Sets the isolate of the currently executing thread.
     *
     * @param isolate the isolate
     */
    static void setCurrentIsolate(Isolate isolate) {
        currentIsolate = isolate;
    }

    /**
     * Eliminates a finalizer.
     *
     * @param obj the object of the finalizer
     */
    public static void eliminateFinalizer(Object obj) {
        GC.eliminateFinalizer(obj);
    }

    /*=======================================================================*\
     *                          Native method lookup                         *
    \*=======================================================================*/

    /**
     * Gets the identifier for a registered native method.
     *
     * @param name   the fully qualified name of the native method
     * @return the identifier for the method or -1 if the method has not been registered
     */
    public static int lookupNative(String name) {
        throw new LinkageError("Calls to native methods can only appear in romized code");
    }

}
