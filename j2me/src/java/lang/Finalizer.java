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
 * Class to record objects that require finalization.
 *
 * @author Nik Shaylor
 */
public class Finalizer implements Runnable {

    /**
     * The object requiring finalization.
     */
    private Object object;

    /**
     * The isolate of the thread that created the object.
     */
    private Isolate isolate;

    /**
     * Pointer to next finalizer in the garbage collector or isolate queue.
     */
    private Finalizer next;

    /**
     * Constructor.
     *
     * @param object the object that needs finalization
     */
    Finalizer(Object object) {
        this.object  = object;
        this.isolate = VM.getCurrentIsolate();
    }

    /**
     * Get the object.
     *
     * @return the object.
     */
    Object getObject() {
        return object;
    }

    /**
     * Set the next finalizer.
     *
     * @param nextFinalizer the finalizer
     */
    void setNext(Finalizer nextFinalizer) {
        next = nextFinalizer;
    }

    /**
     * Get the next finalizer.
     *
     * @return the next finalizer.
     */
    Finalizer getNext() {
        return next;
    }

    /**
     * Get the isolate.
     *
     * @return the isolate.
     */
    Isolate getIsolate() {
        return isolate;
    }

    /**
     * Queue the finalizer onto the isolate for execution.
     */
    void queueToIsolate() {
        isolate.addFinalizer(this);
    }

    /**
     * Run the finalzer.
     */
    public void run() {
        try {
            object.finalize();
        } catch(Throwable ex) {
        }
    }
}


