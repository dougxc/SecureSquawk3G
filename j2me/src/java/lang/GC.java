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
import com.sun.squawk.util.Hashtable; // Version without synchronization
import java.io.PrintStream;
import javax.microedition.io.Connector;
import java.io.*;


class GC {

    /**
     * The plug-in garbage collection algorithm. It is critical that this object
     * is not allocated in the heap that is managed by the collector.
     */
    private static GarbageCollector collector;

    /**
     * Counter for the number of full-heap collections.
     */
    private static int fullCollectionCount;

    /**
     * Counter for the number of partial-heap collections.
     */
    private static int partialCollectionCount;

    /**
     * Flags whether or not the VM is in the collector.
     */
    private static boolean collecting;

/*if[CHUNKY_STACKS]*/
/*else[CHUNKY_STACKS]*/
//  private final static int VIRTUAL_STACK_SIZE = 64*1024;
/*end[CHUNKY_STACKS]*/

    /**
     * Gets a reference to the installed collector.
     *
     * @return a reference to the installed collector
     */
    static GarbageCollector getCollector() {
        return collector;
    }

    /**
     * Excessive GC flag.
     */
    private static boolean excessiveGC;

    /**
     * Counter for the number of monitor exit operations.
     */
    private static int monitorExitCount;

    /**
     * Counter for the number of monitors released at exit time.
     */
    private static int monitorReleaseCount;

    /**
     * Sets the state of the excessive GC flag.
     *
     * @param value true of excessive GC should be is enabled
     */
    static void setExcessiveGC(boolean value) {
        excessiveGC = value;
    }

    /**
     * Gets the state of the excessive GC flag.
     *
     * @return true of excessive GC is enabled
     */
    static boolean getExcessiveGC() {
        return excessiveGC;
    }

    /**
     * Rounds up a 32 bit value to the next word boundry.
     *
     * @param value  the value to round up
     * @return the result
     */
    static int roundUpToWord(int value) {
        return (value + (HDR.BYTES_PER_WORD-1)) & ~(HDR.BYTES_PER_WORD-1);
    }

    /**
     * Rounds up a 32 bit value based on a given alignment.
     *
     * @param value      the value to be rounded up
     * @param alignment  <code>value</value> is rounded up to be a multiple of this value
     * @return the aligned value
     */
    static int roundUp(int value, int alignment) {
        return (value + (alignment-1)) & ~(alignment-1);
    }

    /**
     * Rounds down a 32 bit value to the next word boundry.
     *
     * @param value  the value to round down
     * @return the result
     */
    static int roundDownToWord(int value) {
        return value & ~(HDR.BYTES_PER_WORD-1);
    }

    /**
     * Rounds down a 32 bit value based on a given alignment.
     *
     * @param value      the value to be rounded down
     * @param alignment  <code>value</value> is rounded down to be a multiple of this value
     * @return the aligned value
     */
    static int roundDown(int value, int alignment) {
        return value & ~(alignment-1);
    }

    /*---------------------------------------------------------------------------*\
     *                        Global stack chunk list                            *
    \*---------------------------------------------------------------------------*/

    /**
     * The global list of allocated stack chunks.
     */
    private static Object stackChunks;

/*if[CHUNKY_STACKS]*/
/*else[CHUNKY_STACKS]*/
//  /**
//   * The global list of allocated stack chunks.
//   */
//  private static Object freeChunks;
/*end[CHUNKY_STACKS]*/

    /**
     * Adds a stack chunk to the global list of stack chunks.
     *
     * @param chunk  the stack chunk to be added
     */
    private static void addStackChunk(Object chunk) {
        Assert.that(!findStackChunk(stackChunks, chunk), "stack chunk already exists in list of stack chunks");
        Unsafe.setObject(chunk, SC.next, stackChunks);
        stackChunks = chunk;
//VM.print("GC::addStackChunk - added chunk "); VM.printAddress(chunk); VM.println();
    }

    /**
     * Determines whether or not a given stack chunk is present in a given list of stack chunks.
     *
     * @param haystack the head of a stack chunk list
     * @param needle   the stack chunk to find
     * @return true if <code>needle</code> is present in the list headed by <code>haystack</code>
     */
    private static boolean findStackChunk(Object haystack, Object needle) {
        while (haystack != null) {
            if (haystack == needle) {
                return true;
            }
            haystack = Unsafe.getObject(haystack, SC.next);
        }
        return false;
    }

    /**
     * Counts the number of global stack chunks.
     *
     * @return the number of global stack chunks
     */
    static int countStackChunks() {
        return countStackChunks(stackChunks);
    }

    /**
     * Counts the number of stack chunks in a given list of stack chunks.
     *
     * @param sc  the first element in a list of stack chunks
     * @return the number of stack chunks in the list headed by <code>sc</code>
     */
    static int countStackChunks(Object sc) {
        int count = 0;
        while (sc != null) {
            sc = Unsafe.getObject(sc, SC.next);
            ++count;
        }
        return count;
    }

    /**
     * Pruning predicates.
     */
    static final int PRUNE_ORPHAN = 1, PRUNE_OWNED_BY_ISOLATE = 2;

    /**
     * Prunes the global list of stack chunks according to a given predicate. If the
     * predicate is {@link #PRUNE_ORPHAN} then all the stack chunks whose owning thread pointer
     * is null are pruned. Otherwise all the stack chunks whose owning isolate matches
     * the given isolate are pruned.
     *
     * @param trace      specifies if tracing should be performed
     * @param predicate  must be {@link #PRUNE_ORPHAN} or  {@link #PRUNE_OWNED_BY_ISOLATE}
     * @param isolate    must be non-null if <code>predicate == PRUNE_OWNED_BY_ISOLATE</code>
     * @return the list of pruned stack chunks. This list is now disjoint from the
     *                   global list of stack chunks
     */
/*if[CHUNKY_STACKS]*/
    static Object pruneStackChunks(boolean trace, int predicate, Isolate isolate) {
        Assert.that(predicate == PRUNE_ORPHAN || predicate == PRUNE_OWNED_BY_ISOLATE, "invalid prune predicate");
/*else[CHUNKY_STACKS]*/
//  static void   pruneStackChunks(boolean trace, int predicate, Isolate isolate) {
//      Assert.that(predicate == PRUNE_ORPHAN , "invalid prune predicate");
/*end[CHUNKY_STACKS]*/

        Object sc = stackChunks;
        stackChunks = null;
        Object kept = null;
/*if[CHUNKY_STACKS]*/
        Object discarded = null;
/*else[CHUNKY_STACKS]*/
//      Object discarded = freeChunks;
//      freeChunks = null;
/*end[CHUNKY_STACKS]*/

        while (sc != null) {

            boolean prune;
            Thread owner = VM.asThread(Unsafe.getObject(sc, SC.owner));

            if (predicate == PRUNE_ORPHAN) {
/*if[CHUNKY_STACKS]*/
/*else[CHUNKY_STACKS]*/
//              if (owner != null) {
//                  if (owner.getIsolate().isExited()) {
//                      owner.zeroStack();
//                      owner = null;
//                  }
//              }
/*end[CHUNKY_STACKS]*/

                prune = (owner == null);
            } else {
                Assert.always(owner != null, "orphan chunks must be pruned before pruning chunks based on an isolate");
//                prune = (Unsafe.getObject(owner, (int)FieldOffsets.java_lang_Thread$isolate) == isolate);
                prune = (owner.getIsolate() == isolate);
            }

//VM.print("GC::pruneStackChunks - owner of stack chunk "); VM.printAddress(sc); VM.print(" = "); VM.printAddress(Unsafe.getObject(sc, SC.owner)); VM.println();
            Object next = Unsafe.getObject(sc, SC.next);
            if (!prune) {
                if (TRACING_SUPPORTED && trace) {
                    VM.print("GC::pruneStackChunks - keep ");
                    VM.printAddress(sc);
                    VM.println();
                }
                if (kept == null) {
                    kept = sc;
                    Unsafe.setAddress(sc, SC.next, null);
                } else {
                    Unsafe.setAddress(sc, SC.next, kept);
                    kept = sc;
                }

                Assert.always(owner.getStack() == sc, "stack and thread disagree on ownership");
            } else {
                if (TRACING_SUPPORTED && trace) {
                    VM.print("GC::pruneStackChunks - discard ");
                    VM.printAddress(sc);
                    VM.println();
                }
                if (discarded == null) {
                    discarded = sc;
                    Unsafe.setAddress(sc, SC.next, null);
                } else {
                    Unsafe.setAddress(sc, SC.next, discarded);
                    discarded = sc;
                }
            }

            sc = next;
        }

        stackChunks = kept;

/*if[CHUNKY_STACKS]*/
        return discarded;
/*else[CHUNKY_STACKS]*/
//      freeChunks = discarded;
/*end[CHUNKY_STACKS]*/
    }

    /**
     * Determines if two given stack chunk lists are disjoint.
     *
     * @param sc1  the head of the first list
     * @param sc2  the head of the second list
     * @return true if <code>sc1</code> and <code>sc2</code> are disjoint
     */
    private static boolean disjoint(Object sc1, Object sc2) {
        while (sc1 != null) {
            if (findStackChunk(sc2, sc1)) {
                return false;
            }
            sc1 = Unsafe.getObject(sc1, SC.next);
        }
        return true;
    }

    /**
     * Appends a given list of stack chunks to the end of the global stack chunk list.
     *
     * @param sc the list of stack chunks to append
     */
    static void appendStackChunks(Object sc) {
        Object tail = stackChunks;
        Assert.that(tail != null);
        Assert.that(disjoint(sc, tail));
        while (Unsafe.getObject(tail, SC.next) != null) {
            tail = Unsafe.getObject(tail, SC.next);
        }
        Unsafe.setObject(tail, SC.next, sc);
        if (Assert.ASSERTS_ENABLED) {
            // Ensure that none of the appended stack chunks has a null owner
            while (sc != null) {
//VM.print("appending: "); VM.printAddress(sc); VM.println();
                Assert.that(Unsafe.getObject(sc, SC.owner) != null);
                sc = Unsafe.getObject(sc, SC.next);
            }

        }
    }

    /**
     * Gets the head of the global stack chunk list. This caller of this method should
     * only use the list in a read only manner.
     *
     * @return the head of the global stack chunk list
     */
    static Object getStackChunkList() {
        return stackChunks;
    }

    /*---------------------------------------------------------------------------*\
     *                                  Tracing                                  *
    \*---------------------------------------------------------------------------*/
    /**
     * Used for conditional compiling trace code.
     */
    final static boolean TRACING_SUPPORTED = Klass.DEBUG;

    /**
     * GC tracing flag specifying basic tracing.
     */
    static final int TRACE_BASIC = 1;

    /**
     * GC tracing flag specifying tracing of allocation.
     */
    static final int TRACE_ALLOCATION = 2;

    /**
     * GC tracing flag specifying detailed tracing of garbage collection.
     */
    static final int TRACE_COLLECTION = 4;

    /**
     * GC tracing flag specifying detailed tracing of object graph copying.
     */
    static final int TRACE_OBJECT_GRAPH_COPYING = 8;

    /**
     * GC tracing flag specifying heap tracing.
     */
    static final int TRACE_HEAP = 16;

    /**
     * The mask of GC trace flags.
     */
    private static int traceFlags;

    /**
     * The number of collections that must occur before garbage collector
     * tracing is enabled.
     */
    private static int traceThreshold;

    /**
     * Determines if a specified GC tracing option is enabled.
     *
     * @param flag  the GC tracing option to test (one of the <code>GC.TRACE_...</code> values)
     * @return true if the option is enabled and the number of collection count threashold for
     *              tracing has been met
     */
    static boolean isTracing(int flag) {
        return (traceFlags & flag) != 0 && (collector == null || fullCollectionCount >= traceThreshold);
    }

    /**
     * Sets the number of collections that must occur before garbage collector
     * tracing is enabled.
     *
     * @param threshold the number of collections that must occur before garbage collector
     * tracing is enabled
     */
    static void setTraceThreshold(int threshold) {
        traceThreshold = threshold;
    }

    /**
     * Gets the number of collections that must occur before garbage collector
     * tracing is enabled.
     *
     * @return the number of collections that must occur before garbage collector
     * tracing is enabled
     */
    static int getTraceThreshold() {
        return traceThreshold;
    }

    /*---------------------------------------------------------------------------*\
     *                       Non-volatile memory management                      *
    \*---------------------------------------------------------------------------*/

    /**
     * The set of object memories that have been loaded into the VM. This array is not
     * owned by any isolate and there is a test in the object graph copier that
     * an ObjectMemory instance is never part of a copied object graph.
     */
    private static ObjectMemory[] readOnlyObjectMemories;

    /**
     * Searches for an ObjectMemory that corresponds to a given URL.
     *
     * @param url   the URL to search with
     * @return the ObjectMemory that corresponds to <code>url</code> or null if there is no such ObjectMemory
     */
    static ObjectMemory lookupObjectMemoryBySourceURL(String url) {
        if (readOnlyObjectMemories != null) {
            for (int i = 0; i != readOnlyObjectMemories.length; ++i) {
                ObjectMemory om = readOnlyObjectMemories[i];
                if (om.getURL().equals(url)) {
                    return om;
                }
            }
        }
        return null;
    }

    /**
     * Searches for an ObjectMemory that corresponds to a given root object.
     *
     * @param root   the root object to search with
     * @return the ObjectMemory that corresponds to <code>root</code> or null if there is no such ObjectMemory
     */
    static ObjectMemory lookupObjectMemoryByRoot(Object root) {
        if (readOnlyObjectMemories != null) {
            for (int i = 0; i != readOnlyObjectMemories.length; ++i) {
                ObjectMemory om = readOnlyObjectMemories[i];
                if (om.getRoot() == root) {
                    return om;
                }
            }
        }
        return null;
    }

    /**
     * Registers a loaded and relocated ObjectMemory that has been put into NVM. It is a
     * fatar error if the given object memory is not in NVM or is already registered.
     *
     * @param om  the object memory to register
     */
    static void addObjectMemory(ObjectMemory om) {
        Assert.that(!GC.inRam(om.getStart()));
        Assert.that(lookupObjectMemoryByRoot(om.getRoot()) == null);
        ObjectMemory[] current = readOnlyObjectMemories;
        ObjectMemory[] arr = new ObjectMemory[current.length + 1];
        System.arraycopy(current, 0, arr, 0, current.length);
        arr[current.length] = om;
        readOnlyObjectMemories = arr;

        if (VM.isVeryVerbose()) {
            PrintStream out = null;
            try {
                out = new PrintStream(Connector.openOutputStream("file://squawk.reloc"));
                for (int i = 0; i != readOnlyObjectMemories.length; ++i) {
                    om = readOnlyObjectMemories[i];
                    out.println(om.getURL() + "=" + om.getStart().toUWord().toPrimitive());
                }
                VM.println("[wrote/updated relocation info for read-only object memories to 'squawk.reloc']");
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }

    }

    /*---------------------------------------------------------------------------*\
     *                       Non-volatile memory management                      *
    \*---------------------------------------------------------------------------*/

    /**
     * The start of non-volatile memory.
     */
    private static Address nvmStart;

    /**
     * The end of non-volatile memory.
     */
    private static Address nvmEnd;

    /**
     * The current allocation point in non-volatile memory.
     */
    private static Address nvmAllocationPointer;

    /**
     * Allocate a buffer in NVM. The return buffer has not had its contents zeroed.
     *
     * @param   size        the length in bytes to be allocated (must be a mutiple of HDR.BYTES_PER_WORD)
     * @return  the address of the allocated buffer
     * @exception OutOfMemoryError if the allocation fails
     */
    static Address allocateNvmBuffer(int size) {
        Assert.that(size == roundUpToWord(size));
        Address block  = nvmAllocationPointer;
        Address next = nvmAllocationPointer.add(size);
        if (next.hi(nvmEnd)) {
            throw VM.getOutOfMemoryError();
        }
        nvmAllocationPointer = next;
        return block;
    }

    /**
     * Gets the size (in bytes) of the NVM.
     *
     * @return the size (in bytes) of the NVM
     */
    static int getNvmSize() {
        return nvmEnd.diff(nvmStart).toInt();
    }

    /**
     * Determines if a given object is within NVM.
     *
     * @param object   the object to test
     * @return true if <code>object</code> is within NVM
     */
    static boolean inNvm(Address object) {
        /*
         * Need to account for the object's header on the low
         * end and zero sized objects on the high end
         */
        return object.hi(nvmStart) && object.loeq(nvmEnd);
    }

    /**
     * Gets the offset (in bytes) of an object in NVM from the start of NVM
     *
     * @param object  the object to test
     * @return the offset (in bytes) of <code>object</code> in NVM from the start of NVM
     */
    static Offset getOffsetInNvm(Address object) {
        Assert.that(inNvm(object));
        return object.diff(nvmStart);
    }

    /**
     * Gets an object in NVM given a offset (in bytes) to the object from the start of NVM.
     *
     * @param offset   the offset (in bytes) of the object to retrieve
     * @return the object at <code>offset</code> bytes from the start of NVM
     */
    static Address getObjectInNvm(Offset offset) {
        return nvmStart.addOffset(offset);
    }

    /*---------------------------------------------------------------------------*\
     *                            RAM memory management                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Flag to show that memory allocation is enabled.
     */
    private static boolean allocationEnabled;

    /**
     * Flag to show that garbage collection is enabled.
     */
    private static boolean gcEnabled;

    /**
     * Start of RAM.
     */
    private static Address ramStart;

    /**
     * End of RAM.
     */
    private static Address ramEnd;

    /**
     * Address of the start of the allocation space.
     */
    private static Address ramAllocationStartPointer;

    /**
     * Address of current allocation point in the allocation space.
     */
    private static Address ramAllocationPointer;

    /**
     * End address of the allocation space.
     */
    private static Address ramAllocationEndPointer;

    /**
     * Initialize the memory system.
     *
     * @param bootstrapSuite the bootstrap suite
     */
    static void initialize(Suite bootstrapSuite) {

        if (VM.isHosted()) {
            ramStart = Address.zero();
            ramEnd = Address.zero();
        }

        /*
         * Get the pointers rounded correctly.
         */
        Assert.that(ramStart.eq(ramStart.roundUpToWord()));
        Assert.that(ramEnd.eq(ramEnd.roundDownToWord()));

        /*
         * Temporarily set the main allocation point to the start-end addresses
         * supplied. This allows permanent objects to be allocated outside the
         * garbage collected heap.
         */
        setRamAllocateParameters(ramStart, ramStart, ramEnd);
        setAllocationEnabled(true);

        if (!VM.isHosted()) {

            /*
             * Allocate the collector object outside the scope of the memory that
             * will be managed by the collector.
             */
            GarbageCollector collector = new/*VAL*/java.lang.CheneyCollector/*J2ME.COLLECTOR*/(ramStart, ramEnd, bootstrapSuite);

            /*
             * Initialize the collector.
             */
            collector.initialize(ramStart, ramAllocationPointer, ramEnd, bootstrapSuite);

            /*
             * Initialize the record of loaded/resident object memories
             */
            readOnlyObjectMemories = new ObjectMemory[] { ObjectMemory.createBootstrapObjectMemory(bootstrapSuite)};

            /*
             * Enable GC.
             */
            GC.collector = collector;
            gcEnabled = true;
        }
    }

    /**
     * Set the RAM parameters.
     *
     * @param start          the start of RAM memory
     * @param allocateStart  the address at which to start allocating
     * @param end            the end of RAM memory
     */
    static void setRamAllocateParameters(Address start, Address allocateStart, Address end) {
        ramAllocationStartPointer = start;
        ramAllocationPointer = allocateStart;
        ramAllocationEndPointer = end;
    }

    /**
     * Gets the number of objects currently allocated.
     *
     * @return the number of objects currently allocated
     */
    static int countObjectsInRamAllocationSpace() {
        Address end = ramAllocationPointer;
        int count = 0;
        for (Address block = ramAllocationStartPointer; block.lo(end); ) {
            ++count;
            Address object = GC.blockToOop(block);
            block = object.add(GC.getBodySize(GC.getKlass(object), object));
        }
        return count;
    }

    /**
     * Gets the offset (in bytes) of an object in RAM from the start of RAM
     *
     * @param object  the object to test
     * @return the offset (in bytes) of <code>object</code> in RAM from the start of RAM
     */
    static Offset getOffsetInRam(Address object) {
        Assert.that(inRam(object));
        return object.diff(ramStart);
    }


    /**
     * Determines if a given object is in RAM.
     *
     * @param   object  the object to test
     * @return  true if <code>object</code> is an instance in RAM
     */
    static boolean inRam(Object object) {
        if (VM.isHosted()) {
            return !(object instanceof Address);
        } else if (Address.fromObject(object).loeq(ramStart)) {
            return false;
        } else if (Address.fromObject(object).hi(ramEnd)) {
            return false;
        }
        return true;
    }

    /**
     * Enable or disable memory allocation.
     *
     * @param newState the state that allocation should be set to
     * @return the allocation state before this call
     */
    static boolean setAllocationEnabled(boolean newState) {
        boolean oldState  = allocationEnabled;
        allocationEnabled = newState;
        return oldState;
    }

    /**
     * Enable or disable the garbage collector.
     *
     * @param newState the new abled/disabled state of the garbage collector
     * @return the garbage collector's state before this call
     */
    static boolean setGCEnabled(boolean newState) {
        boolean oldState = gcEnabled;
        gcEnabled = newState;
        return oldState;
    }

    /**
     * Test to see if this is a safe time to switch threads.
     *
     * @return true if it is
     */
    static boolean isSafeToSwitchThreads() {
        return allocationEnabled;
    }

    /**
     * Allocate a chunk of zeroed memory from RAM with hosted.
     *
     * @param   size        the length in bytes of the object and its header (i.e. the total number of
     *                      bytes to be allocated). This size is rounded up by this function to be word-aligned.
     * @param   klass       the class of the object being allocated
     * @param   arrayLength the number of elements in the array being allocated or -1 if a non-array
     *                      object is being allocated
     * @return a pointer to a well-formed object or null if the allocation failed
     */
    private static Object allocatePrimHosted(int size, Object klass, int arrayLength) {
        boolean isArray  = arrayLength != -1;
        int headerSize   = isArray ? HDR.arrayHeaderSize : HDR.basicHeaderSize;
        UWord encodedArrayLength = encodeLengthWord(arrayLength);
        Object res;

        VM.extendsEnabled = false; //----------------------------------------------------------------------

        Address block = ramAllocationPointer;
        Offset remainder = ramAllocationEndPointer.diff(block);
        if (size < 0 || remainder.lt(Offset.fromPrimitive(size))) {
            res = null;
        } else {
            Address oop = block.add(headerSize);
            Unsafe.setAddress(oop, HDR.klass, klass);
            if (isArray) {
                Unsafe.setUWord(oop, HDR.length, encodedArrayLength);
            }
            ramAllocationPointer = block.add(size);
            VM.zeroWords(oop, ramAllocationPointer);
            res = oop.toObject();
        }

        VM.extendsEnabled = true; //----------------------------------------------------------------------

        return res;
    }

    /**
     * Allocate a chunk of zeroed memory from RAM. Only this method contains
     * a sequence of code that exposes some allocated some memory before it is been
     * registered with the garbage collector (i.e. assigning it to a reference).
     *
     * @param   size        the length in bytes of the object and its header (i.e. the total number of
     *                      bytes to be allocated). This size is rounded up by this function to be word-aligned.
     * @param   klass       the class of the object being allocated
     * @param   arrayLength the number of elements in the array being allocated or -1 if a non-array
     *                      object is being allocated
     * @return a pointer to a well-formed object or null if the allocation failed
     */
    private static Object allocatePrim(int size, Object klass, int arrayLength) {
        Assert.that(allocationEnabled);
        Assert.that(size == roundUpToWord(size));

        /*
         * When romizing, allocation always bumps the ram end pointer.
         */
        if (VM.isHosted()) {
            ramAllocationEndPointer = ramAllocationPointer.add(size);
            Unsafe.setMemorySize(ramAllocationPointer.toUWord().toOffset().add(size).toInt());
            return allocatePrimHosted(size, klass, arrayLength);
        }

        Address block = ramAllocationPointer;
        Offset remainder = ramAllocationEndPointer.diff(block);
        UWord blockAsWord = block.toUWord();
        block = Address.zero(); // Zero live address
        Object oop = VM.allocate(size, klass, arrayLength);
        //Object oop = allocatePrimHosted(size, klass, arrayLength);

        /*
         * Trace.
         */
        if (oop != null) {
            if (isTracing(TRACE_ALLOCATION)) {
                VM.print("[Allocated object: block = ");
                VM.printAddress(blockAsWord);
                VM.print(" oop = ");
                VM.printAddress(oop);
                VM.print(" size = ");
                VM.print(size);
                VM.print(" klass = ");
                VM.printAddress(klass);
                VM.print(" ");
                VM.print(GC.getKlass(oop).getInternalName());
                VM.println("]");
            }
        } else {
            if (isTracing(TRACE_BASIC)) {
                VM.print("[Failed allocation of ");
                VM.print(size);
                VM.print(" bytes (");
                VM.printOffset(remainder);
                VM.println(" bytes free)]");
            }
        }

        return oop;
    }

    /**
     * Allocate memory for an object from RAM.
     *
     * @param   size        the length in bytes of the object and its header (i.e. the total number of
     *                      bytes to be allocated). This size is rounded up by this function to be word-aligned.
     * @param   klass       the class of the object being allocated
     * @param   arrayLength the number of elements in the array being allocated or -1 if a non-array
     *                      object is being allocated
     * @return  a pointer to a well-formed object
     * @exception OutOfMemoryError if the allocation fails
     */
    private static Object allocate(int size, Object klass, int arrayLength) {
        Object oop = excessiveGC ? null : allocatePrim(size, klass, arrayLength);
        if (oop == null) {
            Assert.always(VM.isThreadingInitialized(), "insufficient memory to start VM");
            if (gcEnabled) {
                VM.collectGarbage();
                oop = allocatePrim(size, klass, arrayLength);
            }
            if (oop == null) {
                throw VM.getOutOfMemoryError();
            }
        }
        return oop;
    }

    /**
     * Removes the stack chunks not attached to an owning thread.
     */
    private static void removeDeadStackChunks() {
        int total = 0;
        final boolean trace = isTracing(TRACE_COLLECTION | TRACE_OBJECT_GRAPH_COPYING);
        if (TRACING_SUPPORTED && trace) {
            total = GC.countStackChunks();
        }

        GC.pruneStackChunks(TRACING_SUPPORTED && trace, GC.PRUNE_ORPHAN, null);

        if (TRACING_SUPPORTED && trace) {
            int live = GC.countStackChunks();
            VM.print("GC::removeDeadStackChunks - live count = ");
            VM.print(live);
            VM.print(" dead count = ");
            VM.print(total - live);
            VM.println();
        }
    }

    /**
     * Collect the garbage.
     * <p>
     * This routing must be called on the GC stack.
     */
    static void collectGarbage() {

        /*
         * Set the collector re-entry guard.
         */
        Assert.always(!collecting);
        collecting = true;

        /*
         * Disable allocation and check that it was enabled.
         */
        boolean oldState = setAllocationEnabled(false);
        if (oldState == false) {
            VM.fatalVMError();
        }

        /*
         * Trace.
         */
        long free = freeMemory(true);
        if (isTracing(TRACE_BASIC) && fullCollectionCount >= traceThreshold) {
            VM.print("** Collecting garbage ** (collection count: ");
            VM.print(fullCollectionCount);
            VM.print(", backward branch count:");
            VM.print(VM.getBranchCount());
            VM.print(", free memory:");
            VM.print(free);
            VM.println(" bytes)");
        }

        /*
         * Remove the dead stack chunks
         */
        removeDeadStackChunks();

        /*
         * Clear the class state cache.
         */
        VM.invalidateClassStateCache();

        /*
         * Call the collector with the current allocation pointer.
         */
        boolean fullCollection = collector.collectGarbage(ramAllocationPointer);
        if (fullCollection) {
            fullCollectionCount++;
        } else {
            partialCollectionCount++;
        }

        /*
         * Unset the collector re-entry guard.
         */
        collecting = false;

        /*
         * Enable allocation again.
         */
        setAllocationEnabled(true);

        if (isTracing(TRACE_BASIC) && fullCollectionCount >= traceThreshold) {
            long afterFree = freeMemory(true);
            VM.print("** Collection finished ** (free memory:");
            VM.print(afterFree);
            VM.print(" bytes, reclaimed ");
            VM.print(afterFree - free);
            VM.println(" bytes)");
        }

        /*
         * Check that the class state cache is still clear.
         */
        Assert.always(VM.invalidateClassStateCache());
    }

    /**
     * Calculates the size (in bytes) of an oop map that will have a bit for
     * every word in a memory of a given size.
     *
     * @param size   the size (in bytes) of the memory that the oop map will describe
     * @return the size (in bytes) of an oop map that will have a bit for every word in
     *               a memory region of size <code>size</code> bytes
     */
    static int calculateOopMapSizeInBytes(int size) {
        return ((size / HDR.BYTES_PER_WORD) + 7) / 8;
    }

    /**
     * @see VM#copyObjectGraph(Object)
     */
    static void copyObjectGraph(Address object, ObjectMemorySerializer.ControlBlock cb, boolean firstPass) {
        /*
         * Trace.
         */
        if (isTracing(TRACE_BASIC) && fullCollectionCount >= traceThreshold) {
            VM.print("** Copying object graph rooted by ");
            VM.print(object.toObject().getClass().getName());
            VM.print(" instance @ ");
            VM.printAddress(object);
            VM.print(" ** (full collection count: ");
            VM.print(fullCollectionCount);
            VM.print(", backward branch count:");
            VM.print(VM.getBranchCount());
            VM.println(")");
        }

        collector.copyObjectGraph(object, cb, firstPass);
    }

    /**
     * Encode an array length word.
     *
     * @param length the length to encode
     * @return the encoded length word
     */
    private static UWord encodeLengthWord(int length) {
        // Can only support arrays whose length can encoded in 30 bits. Throwing
        // an out of memory error is the cleanest way to handle this situtation
        // in the rare case that there was enough memory to allocate the array
        if (length > 0x3FFFFFF) {
            throw VM.getOutOfMemoryError();
        }
        return UWord.fromPrimitive((length << HDR.headerTagBits) | HDR.arrayHeaderTag);
    }

    /**
     * Decode an array length word.
     *
     * @param word encoded length word
     * @return the decoded length
     */
    static int decodeLengthWord(UWord word) {
        return (int)word.toPrimitive() >>> HDR.headerTagBits;
    }

    /**
     * Setup the class pointer field of a header.
     *
     * @param oop object pointer
     * @param klass the address of the object's classs
     */
    /*private*/ static void setHeaderClass(Address oop, Object klass) {
        Unsafe.setAddress(oop, HDR.klass, klass);
    }

    /**
     * Setup the length word of a header.
     *
     * @param oop object pointer
     * @param length the length in elements of the array
     */
    /*private*/ static void setHeaderLength(Address oop, int length) {
        Unsafe.setUWord(oop, HDR.length, encodeLengthWord(length));
    }

    /**
     * Get the class of an object.
     *
     * @param object the object
     * @return its class
     */
    static Klass getKlass(Object object) {
        Assert.that(object != null);
        Object something = Unsafe.getObject(object, HDR.klass);
        Object cls =  Unsafe.getObject(something, (int)FieldOffsets.java_lang_Klass$self);
        return VM.asKlass(cls);
    }

    /**
     * Get the class of an object.
     *
     * @param object the object
     * @return its class
     */
    static Class getClass(Object object) {
        Klass klass = getKlass(object);
        if (klass == Klass.STRING_OF_BYTES) {
            klass = Klass.STRING;
        }
        return (Class) (Object) klass;
    }

    /**
     * Get the length of an array.
     *
     * @param array the array
     * @return the length in elements of the array
     */
    static int getArrayLengthNoCheck(Object array) {
        return (int)decodeLengthWord(Unsafe.getUWord(array, HDR.length));
    }

    /**
     * Get the length of an array.
     *
     * @param array the array
     * @return the length in elements of the array
     */
    static int getArrayLength(Object array) {
        Assert.that(Klass.isSquawkArray(getKlass(array)));
        return getArrayLengthNoCheck(array);
    }

    /**
     * Create a new object instance in RAM.
     *
     * @param   klass  the object's class
     * @return  a pointer to the object
     * @exception OutOfMemoryError if allocation fails
     */
    static Object newInstance(Klass klass) {
        Object oop = allocate((klass.getInstanceSize() * HDR.BYTES_PER_WORD) + HDR.basicHeaderSize, klass, -1);

        /*
         * If the object requires finalization and it is in RAM then allocate
         * a Finalizer for it. (Objects in ROM or NVM cannot be finalized.)
         */
        if (!VM.isHosted() && klass.hasFinalizer() && GC.inRam(oop)) {
            collector.addFinalizer(new Finalizer(oop));
        }
        return oop;
    }

    /**
     * Eliminate a finalizer.
     *
     * @param obj the object of the finalizer
     */
    static void eliminateFinalizer(Object obj) {
        collector.eliminateFinalizer(obj);
    }

    /**
     * Create a new array instance.
     *
     * @param  klass         a pointer to the array's class
     * @param  length        the number of elements in the array
     * @param  dataSize      the size in bytes for an element of the array
     * @return a pointer to the allocated array
     * @exception OutOfMemoryError if allocation fails
     */
    private static Object newArray(Object klass, int length, int dataSize) {

       /*
        * Need to handle integer arithmetic wrapping. If byteLength is very large
        * then "length * dataSize" can go negative.
        */
        int bodySize = length * dataSize;
        if (bodySize < 0) {
            throw VM.getOutOfMemoryError();
        }

        /*
         * Allocate and return the array.
         */
        int size = roundUpToWord(HDR.arrayHeaderSize + bodySize);
        return allocate(size, klass, length);
    }

    /**
     * Create a new array instance in RAM.
     *
     * @param  klass   the array's class
     * @param  length  the number of elements in the array
     * @return a pointer to the array
     * @exception OutOfMemoryError if allocation fails
     * @exception NegativeArraySizeException if length is negative
     */
    static Object newArray(Klass klass, int length) {
        if (length < 0) {
            throw new NegativeArraySizeException();
        }
        return newArray(klass, length, klass.getComponentType().getDataSize());
    }

    /**
     * Get a new stack. This method is guaranteed not to call the garbage collector.
     *
     * @param   length  the number of words that the new stack should contain.
     * @return a pointer to the new stack or null if the allocation fails
     */
    static Object newStack(int length) {
/*if[CHUNKY_STACKS]*/
        int size = roundUpToWord(HDR.arrayHeaderSize + length * Klass.LOCAL.getDataSize());
        Object stack = allocatePrim(size, Klass.LOCAL_ARRAY, length);
/*else[CHUNKY_STACKS]*/
//      int actualLength = (VIRTUAL_STACK_SIZE - HDR.arrayHeaderSize) / Klass.LOCAL.getDataSize();
//      Assert.always(length < actualLength);
//      Object stack = freeChunks;
//      if (stack != null) {
//          freeChunks = Unsafe.getObject(stack, SC.next);
//          Unsafe.setObject(stack, SC.next, null);
//          Unsafe.setObject(stack, SC.lastFP, null);
//      } else {
//          Address vstack = VM.allocateVirtualStack(VIRTUAL_STACK_SIZE);
//          if (!vstack.isZero()) {
//              vstack = vstack.add(HDR.arrayHeaderSize);
//              setHeaderClass(vstack, Klass.LOCAL_ARRAY);
//              setHeaderLength(vstack, actualLength);
//              stack = vstack.toObject();
//          }
//      }
/*end[CHUNKY_STACKS]*/
        if (stack != null) {
            addStackChunk(stack);
        }
        return stack;
    }

/*if[CHUNKY_STACKS]*/

    /**
     * Copy the contents of a stack to a new stack.
     *
     * @param oldStack the old stack
     * @param newStack the new stack
     * @param failedfp the offset in words from the bottom of the stack to the fp that failed
     * @exception OutOfMemoryError if allocation fails
     */
     static void stackCopy(Object oldStack, Object newStack, int failedfp) {
        int oldBodySize = getArrayLength(oldStack) - failedfp;
        int newBodySize = getArrayLength(newStack) - failedfp;
        int extra       = newBodySize - oldBodySize;

//VM.print("GC::stackCopy - owner of oldStack "); VM.printAddress(oldStack); VM.print(" = "); VM.printAddress(Unsafe.getObject(oldStack, SC.owner)); VM.println();
//VM.print("GC::stackCopy - owner of newStack "); VM.printAddress(newStack); VM.print(" = "); VM.printAddress(Unsafe.getObject(newStack, SC.owner)); VM.println();

        /*
         * Copy the meta info in the stack chunk (i.e. next, owner and lastFP) and then copy
         * the body (i.e. the space used for activation frames):
         *
         *             <- SC.limit ->    <----  oldBodySize  ---->
         *            +--------------+----------------------------+
         *  oldStack  |     meta     |           body             |
         *            +--------------+----------------------------+
         *             <--- failedfp --->
         *
         *
         *             <- SC.limit -> <- extra ->
         *            +--------------+-----------+----------------------------+
         *  newStack  |     meta     |           |           body             |
         *            +--------------+-----------+----------------------------+
         *             <--- failedfp ---> <- extra -> <----  oldBodySize  ---->
         *
         * Note that the 'next' pointer of the new stack needs to be preserved
         */
        Object next = Unsafe.getObject(newStack, SC.next);
        arraycopy(oldStack, 0, newStack, 0, SC.limit);
        Unsafe.setObject(newStack, SC.next, next);
        arraycopy(oldStack, failedfp, newStack, extra + failedfp, oldBodySize);

        /*
         * Disconnect the old stack chunk from its owner.
         */
        Unsafe.setObject(oldStack, SC.owner, null);

//VM.print("GC::stackCopy - owner of oldStack "); VM.printAddress(oldStack); VM.print(" = "); VM.printAddress(Unsafe.getObject(oldStack, SC.owner)); VM.println();
//VM.print("GC::stackCopy - owner of newStack "); VM.printAddress(newStack); VM.print(" = "); VM.printAddress(Unsafe.getObject(newStack, SC.owner)); VM.println();

        /*
         * Set the correct frame pointers in the new stack.
         */
        int delta = failedfp;
        Address oldfp = Address.fromObject(Unsafe.getObject(oldStack, delta+1));
        while (!oldfp.isZero()) {
            int diff = oldfp.diff(Address.fromObject(oldStack)).toInt() / HDR.BYTES_PER_WORD;
            Address addr = Address.fromObject(newStack).add((diff + extra) * HDR.BYTES_PER_WORD);
            Unsafe.setObject(newStack, delta+extra+1, addr);
            delta = diff;
            oldfp = Address.fromObject(Unsafe.getObject(oldStack, delta+1));
        }
    }

/*end[CHUNKY_STACKS]*/

    /**
     * Create a new method.
     *
     * @param   definingClass     the class in which the method is defined
     * @param   body              the method body to encode
     * @return a pointer to the method
     * @exception OutOfMemoryError if allocation fails
     */
    static Object newMethod(Object definingClass, MethodBody body) {
        boolean isHosted = VM.isHosted();

        /*
         * Get a ByteBufferEncoder and write the method header into it.
         */
        ByteBufferEncoder enc = new ByteBufferEncoder();
        body.encodeHeader(enc);
        int roundup = roundUpToWord(enc.getSize());
        int padding = roundup - enc.getSize();

        /*
         * Calculate the total size of the object and allocate it.
         *
         * +word for header length word, +word for defining class pointer.
         */
        int hsize = enc.getSize() + padding + HDR.arrayHeaderSize + HDR.BYTES_PER_WORD + HDR.BYTES_PER_WORD;
        Assert.that(GC.roundUpToWord(hsize) == hsize);
        int hsizeInWords = hsize / HDR.BYTES_PER_WORD;
        int bsize = body.getCodeSize();
        UWord bsizeEncoded = encodeLengthWord(bsize);
        int totalSize = roundUpToWord(hsize + bsize);
        Assert.that((hsize & HDR.headerTagMask) == 0);

        /*
         * The method is intially allocated as a byte array so that 'oop' points to
         * a well formed object. The header is fixed up later.
         */
        Object oop = allocate(totalSize, Klass.BYTE_ARRAY, totalSize - HDR.arrayHeaderSize);

        /*
         * Disable extends and get dirty with real pointers.
         */
        VM.extendsEnabled = false; //----------------------------------------------------------------------
        Address block = Address.fromObject(oop).sub(HDR.arrayHeaderSize);
        Address methodOopAsAddress = block.add(hsize);

        /*
         * Set up the class pointer and array length header for the object inside the byte array
         * that will eventually become the bytecode array object.
         */
        Unsafe.setAddress(methodOopAsAddress, HDR.klass, Klass.BYTECODE_ARRAY);
        Unsafe.setUWord(methodOopAsAddress, HDR.length, bsizeEncoded);

        /*
         * Write the header length word and update the local variable 'oop'. These two operations
         * convert the allocated object from a byte array to a bytecode array. This is completely
         * safe as the only pointer in this object that the garbage collector will try to update (i.e.
         * the class in which the method was defined) is still null. Also, there will not
         * yet be any activation records described by the as yet incomplete header.
         */
        UWord headerWord = UWord.fromPrimitive((hsizeInWords << HDR.headerTagBits) | HDR.methodHeaderTag);
        Unsafe.setUWord(block, 0, headerWord); // Tag the header size word to indicate that this is a method
        if (isHosted) {
            Unsafe.clearObject(block, 1); // Clear the pointer to Klass.BYTE_ARRAY
        }
        oop = methodOopAsAddress.toObject();

        /*
         * Clear the Address pointers before the next real invoke.
         */
        block = methodOopAsAddress = Address.zero();
        VM.extendsEnabled = true; //----------------------------------------------------------------------

        /*
         * Plug in the defining class.
         */
        Unsafe.setAddress(oop, HDR.methodDefiningClass, definingClass);

        /*
         * Copy the header and the bytecodes to the object.
         */
        enc.writeToVMMemory(oop, 0 - hsize + HDR.BYTES_PER_WORD + padding);  // Copy the header
        body.writeToVMMemory(oop);                          // Copy the bytecodes
/*if[TYPEMAP]*/
        if (VM.usingTypeMap()) {
            body.writeTypeMapToVMMemory(oop);
        }
/*end[TYPEMAP]*/

        /*
         * Verify the method.
         */
        if (Assert.ASSERTS_ENABLED) {
            body.verifyMethod(oop);
        }

        /*
         * Write the symbol table entries.
         */
        if (isHosted || VM.isVerbose() || VM.getCurrentIsolate().getMainClassName().equals("java.lang.SuiteCreator")) {
            Method method = body.getDefiningMethod();
            String name = method.toString();
            String file = body.getDefiningClass().getSourceFilePath();
            String lnt = Method.lineNumberTableAsString(method.getLineNumberTable());

            int old = VM.setStream(VM.STREAM_SYMBOLS);

            VM.print("METHOD.");
            VM.printAddress(oop);
            VM.print(".NAME=");
            VM.println(name);

            VM.print("METHOD.");
            VM.printAddress(oop);
            VM.print(".FILE=");
            VM.println(file);

            VM.print("METHOD.");
            VM.printAddress(oop);
            VM.print(".LINETABLE=");
            VM.println(lnt);

            VM.setStream(old);
        }

        /*
         * Return the method object.
         */
        return oop;
    }

    /**
     * Copy data from one array to another.
     *
     * @param src    the source array
     * @param srcPos the start position in the source array
     * @param dst    the destination array
     * @param dstPos the start position in the destination array
     * @param lth    number of elements to copy
     */
    static void arraycopy(Object src, int srcPos, Object dst, int dstPos, int lth) {
        Assert.that(GC.getKlass(src).isArray());
        Assert.that(GC.getKlass(dst).isArray());
        int itemLength = GC.getKlass(src).getComponentType().getDataSize();
        Assert.that(GC.getKlass(dst).getComponentType().getDataSize() == itemLength);
        VM.copyBytes(src, srcPos * itemLength, dst, dstPos * itemLength, lth * itemLength, false);
    }

    /**
     * Get the size of the elements in a string.
     *
     * @param string the string
     * @return the element size
     */
    private static int getStringOperandSize(Object string) {
        switch (GC.getKlass(string).getClassID()) {
            case CID.STRING:
            case CID.CHAR_ARRAY: {
                return 2;
            }
            case CID.STRING_OF_BYTES:
            case CID.BYTE_ARRAY: {
                return 1;
            }
            default: {
                VM.fatalVMError();
                return 0;
            }
        }
    }

    /**
     * Copy data from one string to another.
     *
     * @param src the source string
     * @param srcPos the start position in the source string
     * @param dst the destination string
     * @param dstPos the start position in the destination string
     * @param lth number of characters to copy
     */
    static void stringcopy(Object src, int srcPos, Object dst, int dstPos, int lth) {
        int srcsize = getStringOperandSize(src);
        int dstsize = getStringOperandSize(dst);
        if (srcsize == dstsize) {
            VM.copyBytes(src, srcPos * dstsize, dst, dstPos * dstsize, lth * dstsize, false);
        } else if (srcsize == 1 && dstsize == 2) {
            for (int i = 0 ; i < lth ; i++) {
                int ch = Unsafe.getByte(src, srcPos++) & 0xFF;
                Unsafe.setChar(dst, dstPos++, ch);
            }
       } else if (srcsize == 2 && dstsize == 1) {
            for (int i = 0 ; i < lth ; i++) {
                int ch = Unsafe.getChar(src, srcPos++) & 0xFF;
                Unsafe.setByte(dst, dstPos++, ch);
            }
        } else {
            VM.fatalVMError();
        }
    }

    /**
     * Change the type of the given object to java.lang.StringOfBytes.
     *
     * @param oop the object
     * @return the converted object
     */
    static String makeEightBitString(Object oop) {
        Unsafe.setAddress(oop, HDR.klass, Klass.STRING_OF_BYTES);
        return (String) oop;
    }

    /**
     * Change the type of the given object to java.lang.String.
     *
     * @param oop the object
     * @return the converted object
     */
    static String makeSixteenBitString(Object oop) {
        Unsafe.setAddress(oop, HDR.klass, Klass.STRING);
        return (String) oop;
    }

    /**
     * Create a String given the address of a null terminated ASCII C string.
     *
     * @param cstring   the address of a null terminated ASCII C string
     * @return the String instance corresponding to
     */
    static String convertCString(Address cstring) {
        int size = 0;
        for (int i = 0 ;; i++) {
            int ch = Unsafe.getByte(cstring, i) & 0xFF;
            if (ch == 0) {
                break;
            }
            size++;
        }
        char[] chars = new char[size];
        for (int i = 0 ; i != size; i++) {
            int ch = Unsafe.getByte(cstring, i) & 0xFF;
            chars[i] = (char)ch;
        }
        return new String(chars);
    }

    /**
     * Create an array of String given the address and length of an array of null terminated ASCII C strings.
     *
     * @param cstringArray   the address of an array of null terminated ASCII C strings
     * @param strings        the String[] instance into which the elements of cstringArray should be copied
     */
    static void copyCStringArray(Address cstringArray, String[] strings) {
        for (int i = 0 ; i < strings.length ; i++) {
            strings[i] = convertCString(Address.fromObject(Unsafe.getObject(cstringArray, i)));
        }
    }

    /**
     * Get the hashcode for an object.
     *
     * @param object the object the hashcode is needed for.
     * @return the hashcode
     */
    static int getHashCode(Object object) {
        if (GC.inRam(object)) {
            return getObjectAssociation(object).getHashCode();
        } else {
            return VM.hashcode(object);
        }
    }

    /**
     * Get or allocate the Monitor for an object.
     *
     * @param object the object the monitor is needed for.
     * @return the monitor
     */
    static Monitor getMonitor(Object object) {
        if (GC.inRam(object)) {
            /*
             * Objects in RAM have their monitors attached to ObjectAssociation
             * that sits between the object and its class.
             */
            ObjectAssociation assn = getObjectAssociation(object);
            Monitor monitor = assn.getMonitor();
            if (monitor == null) {
                monitor = new Monitor();
                assn.setMonitor(monitor);
            }
            return monitor;
        } else {
            /*
             * Objects in ROM or NVM have their monitors in a hashtable that is
             * maintained by the isolate.
             */
            Hashtable monitorTable = VM.getCurrentIsolate().getMonitorHashtable();
            Monitor monitor = (Monitor)monitorTable.get(object);
            if (monitor == null) {
                monitor = new Monitor();
                monitorTable.put(object, monitor);
            }
            return monitor;
        }
    }

/*if[SMARTMONITORS]*/
    /**
     * Remove the monitor (and ObjectAssociation) if possible.
     *
     * @param object the object
     */
    static void removeMonitor(Object object, boolean cond) {
        monitorExitCount++;
        if (cond) {
            if (GC.inRam(object)) {
                ObjectAssociation assn = getObjectAssociation(object);
                if (!assn.hashCodeInUse()) {
                    Unsafe.setAddress(object, HDR.klass, getKlass(object));
                    monitorReleaseCount++;
                }
            } else {
                Hashtable monitorTable = VM.getCurrentIsolate().getMonitorHashtable();
                monitorTable.remove(object);
                monitorReleaseCount++;
            }
        }
    }

    /**
     * Tests to see if an object has a real monitor object.
     *
     * @param object the object
     * @return true if is does
     */
    static boolean hasRealMonitor(Object object) {
        Monitor monitor = null;
        if (GC.inRam(object)) {
            Object something = Unsafe.getObject(object, HDR.klass);
            Klass klass = getKlass(object);
            if (something == klass) {
                return false;
            } else {
                monitor = ((ObjectAssociation)something).getMonitor();
            }
        } else {
            Hashtable monitorTable = VM.getCurrentIsolate().getMonitorHashtable();
            monitor = (Monitor)monitorTable.get(object);
        }
        return monitor != null;
    }
/*end[SMARTMONITORS]*/

    /**
     * Get or allocate the ObjectAssociation for an object.
     *
     * @param object the object the ObjectAssociation is needed for.
     * @return the ObjectAssociation
     */
    private static ObjectAssociation getObjectAssociation(Object object) {
        Assert.that(GC.inRam(object));
        Object something = Unsafe.getObject(object, HDR.klass);
        Klass klass = getKlass(object);
        if (something != klass) {
            return (ObjectAssociation)something;
        }
        ObjectAssociation assn = new ObjectAssociation(klass);
        Unsafe.setAddress(object, HDR.klass, assn);
        return assn;
    }

    /**
     * Returns the amount of free memory in the system. Calling the <code>gc</code>
     * method may result in increasing the value returned by <code>freeMemory.</code>
     *
     * @return an approximation to the total amount of memory currently
     *         available for future allocated objects, measured in bytes.
     */
    static long freeMemory(boolean ram) {
        if (!ram) {
            VM.fatalVMError();
        }
        return collector.freeMemory(ramAllocationPointer);
    }

    /**
     * Returns the total amount of RAM memory in the Squawk Virtual Machine. The
     * value returned by this method may vary over time, depending on the host
     * environment.
     * <p>
     * Note that the amount of memory required to hold an object of any given
     * type may be implementation-dependent.
     *
     * @return the total amount of memory currently available for current and
     *         future objects, measured in bytes.
     */
    static long totalMemory(boolean ram) {
        if (!ram) {
            VM.fatalVMError();
        }
        return collector.totalMemory();
    }

    /**
     * Returns the number of partial-heap collections.
     *
     * @return the count of partial-heap collections.
     */
    static int getPartialCollectionCount() {
        return partialCollectionCount;
    }

    /**
     * Returns the number of full-heap collections.
     *
     * @return the count of full-heap collections.
     */
    static int getFullCollectionCount() {
        return fullCollectionCount;
    }

    /**
     * Create class state object. This method is used for boot strapping
     * java.lang.Klass.
     *
     * @param klass             the class for which the state is needed
     * @param klassGlobalArray  the class for class state records
     * @return a pointer to the class state
     * @exception OutOfMemoryError if allocation fails
     */
    static Object newClassState(Klass klass, Klass klassGlobalArray) {
        Object res = newArray(klassGlobalArray, CS.firstVariable + klass.getStaticFieldsSize(), HDR.BYTES_PER_WORD);
        Unsafe.setObject(res, CS.klass, klass);

//VM.print(VM.getCurrentIsolate().getMainClassName());
//VM.print(": created class state for ");
//VM.print(Klass.getInternalName(klass));
//VM.print(" -> ");
//VM.printAddress(res);
//VM.print("    icount=");
//VM.println(VM.branchCount());

        return res;
    }

    /**
     * Create class state object.
     *
     * @param  klass the class for which the state is needed
     * @return a pointer to the class state
     * @exception OutOfMemoryError if allocation fails
     */
    static Object newClassState(Klass klass) {
        return newClassState(klass, Klass.GLOBAL_ARRAY);
    }

    /*---------------------------------------------------------------------------*\
     *                        Object layout and traversal                        *
    \*---------------------------------------------------------------------------*/

    /**
     * Get the address at which the body of an object starts given the address of the
     * block of memory allocated for the object.<p>
     *
     * In conjunction with {@link #oopToBlock(Klass, Address) oopToBlock} and {@link #getBodySize(Klass, Address) getBodySize},
     * this method can be used to traverse a range of contiguous objects between <code>start</code> and <code>end</code> as follows:
     * <p><hr><blockquote><pre>
     *      for (Address block = start; block.LT(end); ) {
     *          Address object = GC.blockToOop(block);
     *          // do something with 'object'...
     *          block = object.add(GC.getBodySize(GC.getKlass(object), object));
     *      }
     * </pre></blockquote><hr>
     *
     * @param block   the address of the block of memory allocated for the object (i.e. the
     *                address at which the object's header starts)
     * @return the address at which the body of the object contained in <code>block</code> starts
     */
    static Address blockToOop(Address block) {
        UWord taggedWord = Unsafe.getAsUWord(block, 0);
        switch (taggedWord.and(UWord.fromPrimitive(HDR.headerTagMask)).toInt()) {
            case HDR.basicHeaderTag:  return block.add(HDR.basicHeaderSize);                 // Instance
            case HDR.arrayHeaderTag:  return block.add(HDR.arrayHeaderSize);                 // Array
            case HDR.methodHeaderTag: return block.add((int)GC.decodeLengthWord(taggedWord) * HDR.BYTES_PER_WORD); // Method
            default: VM.fatalVMError();
        }
        return null;
    }

    /**
     * Get the address of the block of memory allocated for an object. The returned
     * address is where the header of the object starts.<p>
     *
     * See {@link #blockToOop(Address) blockToOop} to see how this method can be used to traverse a range of objects.
     *
     * @param klass    the class of <code>object</code>
     * @param object   the address of an object
     * @return the address of the block of memory allocated for <code>object</code>
     */
    static Address oopToBlock(Klass klass, Address object) {
        if (Klass.getClassID(klass) == CID.BYTECODE_ARRAY) {
            return MethodBody.oopToBlock(object);   // Method
        } else if (Klass.isSquawkArray(klass)) {
            return object.sub(HDR.arrayHeaderSize); // Array
        } else {
            return object.sub(HDR.basicHeaderSize); // Instance
        }
    }

    /**
     * Get the size (in bytes) of the body of an object. That is, the size of
     * block of memory allocated for the object minus the size of the object's
     * header.<p>
     *
     * See {@link #blockToOop(Address) blockToOop} to see how this method can be used to traverse a range of objects.
     *
     * @param klass   the class of <code>object</code>
     * @param object  the address of the object to measure
     * @return the size (in bytes) of <code>object</code>'s body
     */
    static int getBodySize(Klass klass, Address object) {
        if (Klass.isSquawkArray(klass)) {
            int length = GC.getArrayLengthNoCheck(object);
            int elementSize = Klass.getSquawkArrayComponentDataSize(klass);
            return GC.roundUpToWord(length * elementSize);
        } else {
            return Klass.getInstanceSize(klass) * HDR.BYTES_PER_WORD;
        }
    }

    /**
     * Traverse a range of an object memory containing contiguous objects.
     *
     * @param start  the address of the first object block in the range
     * @param end    the address of the first byte after the range
     * @param trace  specifies if the objects are to be traced as they are traversed
     */
    static void traverseMemory(Address start, Address end, boolean trace) {
        if (trace) {
            VM.print("Trace of object memory range [");
            VM.printAddress(start);
            VM.print(" .. ");
            VM.printAddress(end);
            VM.println(")");
        }
        for (Address block = start; block.lo(end); ) {
            Address object = GC.blockToOop(block);
            Klass klass = GC.getKlass(object);

            if (trace) {
                VM.print("  object = ");
                VM.printAddress(object);
                VM.print(", block = ");
                VM.printAddress(block);
                VM.print(" is instance of ");
                VM.print(klass.getInternalName());

                VM.print(" [toString=\"");
                VM.print(object.toObject().toString());
                VM.println("\"]");
            }
            block = object.add(GC.getBodySize(GC.getKlass(object), object));
        }
    }


    /*---------------------------------------------------------------------------*\
     *                        String intern table initialization                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Finds all the instances of String in the read only memory that the
     * current isolate is bound against and adds them to a given hash table.
     *
     * @param   strings  the table to which the strings are to be added
     */
    static void getStrings(Hashtable strings) {

        Isolate isolate = VM.getCurrentIsolate();
        Suite openSuite = isolate.getOpenSuite();
        ObjectMemory parent;
        if (openSuite == null) {
            parent = readOnlyObjectMemories[0];
        } else {
            parent = GC.lookupObjectMemoryByRoot(openSuite.getParent());
        }
        Assert.that(parent != null);

        int percent = 0;
        if (VM.isVeryVerbose()) {
            VM.print("Building String intern table from read-only memory");
        }
        while (parent != null) {
            Address end = parent.getStart().add(parent.getSize());
            for (Address block = parent.getStart(); block.lo(end); ) {
                Address object = GC.blockToOop(block);
                if (object.toObject() instanceof String) {
                    strings.put(object.toObject(), object.toObject());
                }
                if (VM.isVeryVerbose()) {
                    Address start = parent.getStart();
                    Offset size = end.diff(start);
                    int percentNow = (int)((block.diff(start).toPrimitive() * 100) / size.toPrimitive());
                    if (percentNow != percent) {
                        VM.print('.');
                        percent = percentNow;
                    }
                }
                block = object.add(GC.getBodySize(GC.getKlass(object), object));
            }
            parent = parent.getParent();
        }
        if (VM.isVeryVerbose()) {
            VM.println(" done");
        }
    }
}
