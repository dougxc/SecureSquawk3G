/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.vm.*;
import com.sun.squawk.util.Assert;

/**
 * This class defines the global Squawk variables that are used to communicate between
 * a normal Java thread and the Squawk system service thread.
 *
 * @author   Nik Shaylor
 */
public final class ServiceOperation {

    /**
     * The an invalid service operation code.
     */
    public final static int NONE = 0;

    /**
     * Extend the current thread.
     */
    public final static int EXTEND = 1;

    /**
     * Collect the garbage.
     */
    public final static int GARBAGE_COLLECT = 2;

    /**
     * Copy an object graph.
     */
    public final static int COPY_OBJECT_GRAPH = 3;

    /**
     * Throw an exception.
     */
    public final static int THROW = 4;

    /**
     * Execute a channel I/O operation.
     */
    public final static int CHANNELIO = 5;

    /**
     * The service operation code.
     */
    private static int code;

    /**
     * The channel context (only used for CHANNELIO).
     */
    private static int context;

    /**
     * The channel operation code (only used for CHANNELIO).
     */
    private static int op;

    /**
     * The channel identifier (only used for CHANNELIO).
     */
    private static int channel;

    /**
     * Integer parameters.
     */
    private static int i1, i2, i3, i4, i5, i6;

    /**
     * Object parameters.
     */
    private static Address o1, o2;

    /**
     * The result code.
     */
    private static int result;

    /**
     * The pending exception for the next catch bytecode.
     */
    private static Throwable pendingException;

    /**
     * This is the service thread operation loop.
     */
    static void execute() {
        for (;;) {
            VM.threadSwitch();
            switch(code) {
                case EXTEND: {
/*if[CHUNKY_STACKS]*/
                    if (Thread.extendStack()) {
                        break;
                    }
/*end[CHUNKY_STACKS]*/
                    pendingException = VM.getOutOfMemoryError(); // and drop through to THROW
                }
                case THROW: {
                    VM.throwException(pendingException);
                    break;
                }
                case GARBAGE_COLLECT: {
                    GC.collectGarbage();
                    break;
                }
                case COPY_OBJECT_GRAPH: {
                    GC.copyObjectGraph(o1, (ObjectMemorySerializer.ControlBlock)o2.toObject(), i1 == 1);
                    break;
                }
                case CHANNELIO: {
                    cioExecute();
                    break;
                }
                default: {
                    VM.fatalVMError();
                    break;
                }
            }
            o1 = null;
            o2 = null;
        }
    }

    /**
     * Execute a channel I/O operation.
     */
    private native static void cioExecute();

}






