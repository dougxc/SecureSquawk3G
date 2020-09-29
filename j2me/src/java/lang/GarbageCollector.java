/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.util.Assert;
import com.sun.squawk.vm.HDR;

/**
 * Base class for all garbage collectors.
 *
 * @author Nik Shaylor
 */
abstract class GarbageCollector  {

    /**
     * Queue of pending finalizers.
     */
    private Finalizer finalizers;

    /**
     * Creates and initializes the garbage collector. The exact type of the
     * garbage collector will be determined by a system property that is
     * potentially defined on the command line.
     *
     * @param permanentMemoryStart start of the memory region in which the permanent objects have already been allocated
     *                             (including the collector itself)
     * @param memoryStart          start of memory region available to the collector for the collectible object
     *                             memory as well as any auxilary data structures it requires. This value also
     *                             denotes the end of the permanent object space
     * @param memoryEnd            end of garbage collector's allocate memory region
     * @param bootstrapSuite       the bootstrap suite
     */
    abstract void initialize(Address permanentMemoryStart, Address memoryStart, Address memoryEnd, Suite bootstrapSuite);

    /**
     * Collect the garbage
     *
     * @param allocationPointer the current allocation pointer
     * @return true if the collector performed a collection of the full heap, false otherwise
     */
    abstract boolean collectGarbage(Address allocationPointer);

    /**
     * Copies an object graph.
     *
     * @param  object  the root of the object graph to copy
     * @param  cb      the in and out parameters of this call (described above)
     * @param firstPass specifies if this is the first or second pass of object graph copying
     */
    abstract void copyObjectGraph(Address object, ObjectMemorySerializer.ControlBlock cb, boolean firstPass);

    /**
     * Returns the amount of free memory in the system. Calling the <code>gc</code>
     * method may result in increasing the value returned by <code>freeMemory.</code>
     *
     * @param  allocationPointer  the current allocationPointer
     * @return an approximation to the total amount of memory currently
     *         available for future allocated objects, measured in bytes.
     */
    abstract long freeMemory(Address allocationPointer);

    /**
     * Returns the total amount of memory in the Squawk Virtual Machine. The
     * value returned by this method may vary over time, depending on the host
     * environment.
     * <p>
     * Note that the amount of memory required to hold an object of any given
     * type may be implementation-dependent.
     *
     * @return the total amount of memory currently available for current and
     *         future objects, measured in bytes.
     */
    abstract long totalMemory();

    /**
     * Process a given command line option that may be specific to the collector implementation.
     *
     * @param arg   the command line option to process
     * @return      true if <code>arg</code> was a collector option
     */
    boolean processCommandLineOption(String arg) {
        return false;
    }

    /**
     * Prints the usage message for the collector specific options (if any).
     *
     * @param out  the stream on which to print the message
     */
    void usage(java.io.PrintStream out) {}

    /**
     * Add a finalizer.
     *
     * @param finalizer the finalizer
     */
    void addFinalizer(Finalizer finalizer) {
        finalizer.setNext(finalizers);
        finalizers = finalizer;
    }

    /**
     * Eliminate a finalizer.
     *
     * @param obj the object of the finalizer
     */
    void eliminateFinalizer(Object obj) {
        Finalizer prev = null;
        Finalizer finalizer = finalizers;
        while (finalizer != null) {
            if (finalizer.getObject() == obj) {
                Finalizer next = finalizer.getNext();
                if (prev == null) {
                    finalizers = next;
                } else {
                    prev.setNext(next);
                }
                return;
            }
            prev = finalizer;
            finalizer = finalizer.getNext();
        }
    }

    /**
     * Get the queue of finalizers and clear the queue.
     *
     * @return the list of finalizers or null if there are none.
     */
    protected Finalizer getFinalizers() {
        Finalizer finalizers = this.finalizers;
        this.finalizers = null;
        return finalizers;
    }

    /*---------------------------------------------------------------------------*\
     *                                 Tracing                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * Prints a line with the name and value of an Address GC variable to the VM stream.
     *
     * @param name   the name of the variable
     * @param value  the value of the variable
     */
    final void traceVariable(String name, Address value) {
        VM.print("    ");
        VM.print(name);
        VM.print(" = ");
        VM.printAddress(value);
        VM.println();
    }

    /**
     * Prints a line with the name and value of an integer GC variable to the VM stream.
     *
     * @param name   the name of the variable
     * @param value  the value of the variable
     */
    final void traceVariable(String name, int value) {
        VM.print("    ");
        VM.print(name);
        VM.print(" = ");
        VM.print(value);
        VM.println();
    }

    /**
     * Traces the heap's state.
     *
     * @param description         a description of the temporal state of the VM
     * @param  allocationPointer  the current allocationPointer
     */
    abstract void traceHeap(String description, Address allocationPointer);

    /**
     * Conditional compilation of usage of the heap tracing facility.
     * This is controlled by an additional (optional) property in build.properties
     * so that it can be enabled even if general debugging isn't.
     */
    final static boolean HEAP_TRACE = /*VAL*/false/*J2ME.HEAP_TRACE*/ || Klass.DEBUG;


    /**
     * Writes the initial heap trace line describing the range of memory containing the heap.
     *
     * @param start    the start address of the memory being modelled
     * @param end      the end address of the memory being modelled
     */
    void traceHeapInitialize(Address start, Address end) {
        VM.print("*HEAP*:initialize:");
        VM.printAddress(start);
        VM.print(":");
        VM.printAddress(end);
        VM.println();
    }

    /**
     * Writes a heap trace line denoting the start of a heap trace.
     *
     * @param description  a description of the temporal state of the heap
     * @param freeMemory   the amount of free memory
     */
    void traceHeapStart(String description, long freeMemory) {
        VM.print("*HEAP*:start:");
        VM.print(VM.branchCount());
        VM.print(":");
        VM.print(freeMemory);
        VM.print(":");
        VM.print(totalMemory());
        VM.print(":");
        VM.println(description);
    }

    /**
     * Writes a heap trace line denoting the end of a heap trace.
     */
    void traceHeapEnd() {
        VM.println("*HEAP*:end");
    }

    /**
     * Writes a heap trace line for a segment of the memory.
     *
     * @param label   a descriptor for the segment or null if the segment denotes space never used
     * @param start   the starting address of the segment
     * @param size    the size (in bytes) of the segment
     */
    void traceHeapSegment(String label, Address start, int size) {
        if (size > 0) {
            VM.print("*HEAP*:segment:");
            VM.printAddress(start);
            VM.print(":");
            VM.printAddress(start.add(size));
            VM.print(":");
            VM.println(label);
        }
    }

    /**
     * Writes a heap trace line for a segment of the memory.
     *
     * @param label   a descriptor for the segment or null if the segment denotes space never used
     * @param start   the starting address of the segment
     * @param end     the address one byte past the end of the segment
     */
    void traceHeapSegment(String label, Address start, Address end) {
        traceHeapSegment(label, start, end.diff(start).toInt());
    }

    /**
     * Writes a heap trace line for an object. The last has the pattern:
     *
     *   trace  ::= "*HEAP*:" word ":" word* ":oop:" (word ":" | repeat ":" )* class
     *   word   ::= <a 32 or 64 bit unsigned value>
     *   repeat ::= "*" int
     *
     * where the first word is the value of <code>start</code>, the words up to ":oop:"
     * compose the object's header and the remaining words compose the object's body.
     * A 'repeat' states that the previous word is repeated 'n' more times.
     *
     * @param start    the address of the object's header
     * @param oop      the address of the object's body
     * @param klass    the klass of the object
     * @param size     the size (in words) of the object's body
     */
    void traceHeapObject(Address start, Address oop, Klass klass, int size) {
        VM.print("*HEAP*:");
        VM.printAddress(start);
        VM.print(":");
        while (start.lo(oop)) {
            VM.printUWord(Unsafe.getUWord(start, 0));
            VM.print(":");
            start = start.add(HDR.BYTES_PER_WORD);
        }
        VM.print("oop:");
        if (size != 0) {
            UWord last = Unsafe.getUWord(oop, 0);
            VM.printUWord(last);
            VM.print(":");
            int repeats = 0;
            for (int i = 1; i != size; ++i) {
                UWord word = Unsafe.getUWord(oop, i);
                if (word.eq(last)) {
                    ++repeats;
                } else {
                    if (repeats != 0) {
                        VM.print("*");
                        VM.print(repeats);
                        VM.print(":");
                        repeats = 0;
                    }
                    VM.printUWord(word);
                    VM.print(":");
                    last = word;
                }
            }
            if (repeats != 0) {
                VM.print("*");
                VM.print(repeats);
                VM.print(":");
            }
        }
        VM.println(Klass.getInternalName(klass));
    }
}


