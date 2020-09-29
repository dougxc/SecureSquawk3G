/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

import com.sun.squawk.util.Hashtable; // Version without synchronization

/**
 * Definition of all the Squawk classes that use global variables.
 * <p>
 * The Squawk VM supports four types of variables. These are local
 * variables, instance variables, static varibles, and global variables.
 * Static variables are those defined in a class using the static keyword
 * these are allocated dynamically by the VM when their classes are
 * initialized, and these variables are created on a per-isolate basis.
 * Global variables are allocated by the romizer and used in place of
 * the static variables in this hard-wired set of system classes. This is
 * done in cases where certain components of the system must have static
 * state before the normal system that support things like static variables
 * are running. Global variables are shared between all isolates.
 *
 * @author  Nik Shaylor
 */
public final class Global {

    /**
     * The tables of int, addresss, and reference globals.
     */
    private static Hashtable intGlobals  = new Hashtable(),
                             addrGlobals = new Hashtable(),
                             oopGlobals  = new Hashtable();

    /**
     * Test to see if a class requires global variables
     *
     * @param name  the name of the class to test
     * @return true if is does need global variables
     */
    public static boolean needsGlobalVariables(String name) {
        return name.equals("java.lang.VM")
            || name.equals("java.lang.GC")
            || name.equals("java.lang.Thread")
            || name.equals("java.lang.Lisp2Bitmap")
            || name.equals("java.lang.ServiceOperation")
            || name.equals("com.sun.squawk.util.Tracer")
            ;
    }

    /**
     * Fields specified here will be allocated constant offsets.
     */
    public final static long
          java_lang_VM$currentIsolate                   = Oop("java.lang.VM.currentIsolate")
        , java_lang_VM$extendsEnabled                   = Int("java.lang.VM.extendsEnabled")
        , java_lang_VM$usingTypeMap                     = Int("java.lang.VM.usingTypeMap")

        , java_lang_GC$traceFlags                       = Int("java.lang.GC.traceFlags")
        , java_lang_GC$collecting                       = Int("java.lang.GC.collecting")
        , java_lang_GC$monitorExitCount                 = Int("java.lang.GC.monitorExitCount")
        , java_lang_GC$monitorReleaseCount              = Int("java.lang.GC.monitorReleaseCount")

        , java_lang_Thread$nextThreadNumber             = Int("java.lang.Thread.nextThreadNumber")
        , java_lang_Thread$currentThread                = Oop("java.lang.Thread.currentThread")
        , java_lang_Thread$otherThread                  = Oop("java.lang.Thread.otherThread")
        , java_lang_Thread$serviceThread                = Oop("java.lang.Thread.serviceThread")

        , java_lang_ServiceOperation$pendingException   = Oop("java.lang.ServiceOperation.pendingException")
        , java_lang_ServiceOperation$code               = Int("java.lang.ServiceOperation.code")
        , java_lang_ServiceOperation$context            = Int("java.lang.ServiceOperation.context")
        , java_lang_ServiceOperation$op                 = Int("java.lang.ServiceOperation.op")
        , java_lang_ServiceOperation$channel            = Int("java.lang.ServiceOperation.channel")
        , java_lang_ServiceOperation$i1                 = Int("java.lang.ServiceOperation.i1")
        , java_lang_ServiceOperation$i2                 = Int("java.lang.ServiceOperation.i2")
        , java_lang_ServiceOperation$i3                 = Int("java.lang.ServiceOperation.i3")
        , java_lang_ServiceOperation$i4                 = Int("java.lang.ServiceOperation.i4")
        , java_lang_ServiceOperation$i5                 = Int("java.lang.ServiceOperation.i5")
        , java_lang_ServiceOperation$i6                 = Int("java.lang.ServiceOperation.i6")
        , java_lang_ServiceOperation$o1                 = Add("java.lang.ServiceOperation.o1")
        , java_lang_ServiceOperation$o2                 = Add("java.lang.ServiceOperation.o2")
        , java_lang_ServiceOperation$result             = Int("java.lang.ServiceOperation.result")

        , branchCountHigh                               = Int("branchCountHigh")
        , branchCountLow                                = Int("branchCountLow")
        , traceStartHigh                                = Int("traceStartHigh")
        , traceStartLow                                 = Int("traceStartLow")
        , traceEndHigh                                  = Int("traceEndHigh")
        , traceEndLow                                   = Int("traceEndLow")
        , tracing                                       = Int("tracing")
        , runningOnServiceThread                        = Int("runningOnServiceThread")
        , cheneyStartMemoryProtect                      = Add("cheneyStartMemoryProtect")
        , cheneyEndMemoryProtect                        = Add("cheneyEndMemoryProtect")
//        , writeBarrier                                  = Add("writeBarrier")           // The write barrier employed by the Lisp2 collector.
//        , writeBarrierSize                              = Int("writeBarrierSize")       // The size (in bytes) of the write barrier.
//        , writeBarrierBase                              = Add("writeBarrierBase")       // The logical base of the write barrier if it contained a bit for address 0.
        , newCount                                      = Int("newCount")
        , newHits                                       = Int("newHits")
        ;

    /**
     * Tags
     */
    private final static long INT     = 0x8888888800000000L,
                              OOP     = 0x9999999900000000L,
                              ADDR    = 0xAAAAAAAA00000000L,
                              TAGMASK = 0xFFFFFFFF00000000L;


    /**
     * Add a global int.
     *
     * @param name the field name
     * @return the field constant
     */
    private static long Int(String name) {
        int index = intGlobals.size();
        intGlobals.put(name, new Integer(index));
        return INT | index;
    }

    /**
     * Add a global address.
     *
     * @param name the field name
     * @return the field constant
     */
    private static long Add(String name) {
        int index = addrGlobals.size();
        addrGlobals.put(name, new Integer(index));
        return ADDR | index;
    }

    /**
     * Add a global oop reference.
     *
     * @param name the field name
     * @return the field constant
     */
    private static long Oop(String name) {
        int index = oopGlobals.size();
        oopGlobals.put(name, new Integer(index));
        return OOP | index;
    }

    /**
     * Get the hashtable of global ints.
     *
     * @return the hashtable
     */
    public static Hashtable getGlobalInts() {
        return intGlobals;
    }

    /**
     * Get the hashtable of global addresses.
     *
     * @return the hashtable
     */
    public static Hashtable getGlobalAddrs() {
        return addrGlobals;
    }

    /**
     * Get the hashtable of global oops.
     *
     * @return the hashtable
     */
    public static Hashtable getGlobalOops() {
        return oopGlobals;
    }

    /**
     * Test to see if the field constant is a global int.
     *
     * @param field the field constant
     * @return true if it is
     */
    public static boolean isGlobalInt(long field) {
        return (field & TAGMASK) == INT;
    }

    /**
     * Test to see if the field constant is a global address
     *
     * @param field the field constant
     * @return true if it is
     */
    public static boolean isGlobalAddr(long field) {
        return (field & TAGMASK) == ADDR;
    }

    /**
     * Test to see if the field constant is a global ref
     *
     * @param field the field constant
     * @return true if it is
     */
    public static boolean isGlobalOop(long field) {
        return (field & TAGMASK) == OOP;
    }

    /**
     * Get offset
     *
     * @param field the field constant
     * @return the offset
     */
    public static int getOffset(long field) {
        return (short)field;
    }

}
