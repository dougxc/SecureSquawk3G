/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;

/**
 * Collector based on the lisp 2 algorithm described in "Garbage Collection : Algorithms for Automatic Dynamic Memory Management"
 * by Richard Jones, Rafael Lins.
 *
 * For objects that move during a collection, forwarding offsets are installed in high bits
 * of the class word in the object's header. The class pointer is made relative to the space
 * in which the class lies (i.e. ROM, NVM or RAM) and this offset is stored in the lower bits.
 * the two lowest bits of the class word are used as a determine if the object has forwarded
 * ('00' if not) and if so, where is the class located ('01' in the heap, '11' in NVM and '10'
 * in ROM). The forwarding offset is relative to the start of a "slice" with the absolute
 * offset of the slice stored in a fixed size "slice offset table".
 *
 *       <-------------- (W-C-2) ---------> <------ C ------> <-2->
 *      +----------------------------------+-----------------+-----+
 *      |  forwarding offset               | class offset    | tag |
 *      +----------------------------------+-----------------+-----+
 *       <--------------------------- w -------------------------->
 *                                          <---- forwardShift --->
 *
 * @author  Doug Simon, Nik Shaylor
 */
final class Lisp2Collector extends GarbageCollector {

    /*-----------------------------------------------------------------------*\
     *                               Descripton                              *
     \*-----------------------------------------------------------------------*/

    /*
                      memoryEnd ->
                                    Slice table


                                    Bit vector


                                    Fixed marking stack

                        heapEnd ->

                                    Unused heap (except as extra marking stack)

              youngGenerationEnd ->

                                    Young Generation

            youngGenerationStart ->

                                    Old Generation

           memoryStart/heapStart ->

     */

    /**
     * The default size of the young generation as a percent of the heap size.
     */
    private final static int DEFAULT_YOUNG_GENERATION_PERCENT = 10;

    /**
     * The marking stack.
     */
    private final MarkingStack markingStack;

    /**
     * This class is a namespace to encapsulate the constants that denote
     * the bits in the class word of an object's header that denote whether
     * or not the object has been forwarded and if so, how to decode the
     * encoded class pointer.
     */
    static final class ClassWordTag {
        private ClassWordTag() {
            Assert.shouldNotReachHere();
        }

        /**
         * The number of bits used for the tag.
         */
        final static int BIT_COUNT = 2;

        /**
         * The mask applied to a class word to extract the tag.
         */
        final static int MASK = 0x3;

        /**
         * The tag value denoting a non-forwarded object. The class pointer is simply the value of the class word.
         */
        final static int POINTER = 0x00;

        /**
         * The tag value denoting that the encoded class pointer is a word offset relative to the start of the heap.
         */
        final static int HEAP = 0x01;

        /**
         * The tag value denoting that the encoded class pointer is a word offset relative to the start of NVM.
         */
        final static int NVM = 0x11;

        /**
         * The tag value denoting that the encoded class pointer is a word offset relative to the start of ROM.
         */
        final static int ROM = 0x10;

        /**
         * Determines if a given class word value is a direct pointer to a class.
         *
         * @param word   the word to test
         * @return true if <code>word</code> is a direct pointer
         */
        static boolean isPointer(UWord word) {
            return (word.and(UWord.fromPrimitive(MASK))).eq(UWord.fromPrimitive(POINTER));
        }

        /**
         * Determines if a given class word value encodes a class pointer as a heap relative word offset.
         *
         * @param word   the word to test
         * @return true if <code>word</code> encodes a class pointer as a heap relative word offset
         */
        static boolean isHeapOffset(UWord word) {
            return (word.and(UWord.fromPrimitive(MASK))).eq(UWord.fromPrimitive(HEAP));
        }

        /**
         * Determines if a given class word value encodes a class pointer as a NVM relative word offset.
         *
         * @param word   the word to test
         * @return true if <code>word</code> encodes a class pointer as a NVM relative word offset
         */
        static boolean isNVMOffset(UWord word) {
            return (word.and(UWord.fromPrimitive(MASK))).eq(UWord.fromPrimitive(NVM));
        }

        /**
         * Determines if a given class word value encodes a class pointer as a ROM relative word offset.
         *
         * @param word   the word to test
         * @return true if <code>word</code> encodes a class pointer as a ROM relative word offset
         */
        static boolean isROMOffset(UWord word) {
            return (word.and(UWord.fromPrimitive(MASK))).eq(UWord.fromPrimitive(ROM));
        }
    }

    /**
     * Guard against re-entry.
     */
    private boolean collecting;

    /**
     * If non-null, then this collector is operating in the context of a call to 'copyObjectGraph'
     * (as opposed to 'collectGarbage').
     */
    private ObjectMemorySerializer.ControlBlock copyObjectGraphCB;

    /**
     * Start address of memory allocated to the garbage collector.
     *
     */
    private /*final*/ Address memoryStart;

    /**
     * The starting address of the heap. The heap is the memory range in which objects are allocated.
     */
    private Address heapStart;

    /**
     * The size (in bytes) of the heap.
     */
    private int heapSize;

    /**
     * The end address of the heap.
     */
    private Address heapEnd;

    /**
     * End address of the memory allocated to the garbage collector.
     */
    private /*final*/ Address memoryEnd;

    /**
     * Address at which to start collecting. The range <code>[collectionStart .. collectionEnd)</code>
     * is the young generation.
     */
    private Address collectionStart;

    /**
     * Address at which to end collecting.
     */
    private Address collectionEnd;

    /**
     * The address at which the slice table starts.
     */
    private Address sliceTable;

    /**
     * The number of slices the heap is partitioned into (which is also the number of entries in the slice table).
     */
    private int sliceCount;

    /**
     * The size (in bytes) of a slice.
     */
    private int sliceSize;

    /**
     * The size (in bytes) of the slice table.
     */
    private int sliceTableSize;

    /**
     * The number of bits in an encoded class word used to express the slice-relative offset to which an object will be moved.
     */
    private int sliceOffsetBits;

    /**
     * The amount by which an encoded class word is logically right shifted to
     * extract the slice-relative offset to which the corresponding object will be moved.
     */
    private int sliceOffsetShift;

    /**
     * The number of bits in an encoded class word used to express the offset of the class in ROM, NVM or the heap.
     */
    private int classOffsetBits;

    /**
     * The mask applied to an encoded class word to extract the offset to the class.
     */
    private int classOffsetMask;

    /**
     * Counter of collections.
     */
    private int collectionCount;

    /**
     * The class of com.sun.squawk.util.Hashtable.
     */
    private final Klass HashTableKlass;

    /**
     * The class of java.lang.Thread
     */
    private final Klass ThreadKlass;

    /**
     * The class of java.lang.Isolate
     */
    private final Klass IsolateKlass;

    /**
     * The class of java.lang.ObjectMemory
     */
    private final Klass ObjectMemoryKlass;

    /**
     * The isolate object (if any) currently being copied by copyObjectGraph().
     */
    private Isolate theIsolate;

    /**
     * Specifies that the next time a collection is performed, the complete heap
     * will be collected instead of just the young generation. This will be the case
     * when the space reclaimed by the previous collection is less than the size of
     * the young generation.
     */
    private boolean nextCollectionIsFull;

    /**
     * This is the start address of the young generation.
     */
    private Address youngGenerationStart;

    /**
     * The ideal size of the young generation as a percent of the heap size.
     */
    private int idealYoungGenerationSizePercent;

    /**
     * The maximum number of times that {@link #markObject(Address)} may be called recursively.
     */
    private static final int MAX_MARKING_RECURSION = 4;

    /**
     * The number of remaining times the {@link #markObject(Address)} may be called recursively
     * before the object being marked is pushed on the marking stack instead.
     */
    private int markingRecursionLevel;

    /**
     * Creates a Lisp2Collector.
     *
     * @param ramStart       start of the RAM allocated to the VM
     * @param ramEnd         end of the RAM allocated to the VM
     * @param bootstrapSuite the bootstrap suite
     */
    Lisp2Collector(Address ramStart, Address ramEnd, Suite bootstrapSuite) {
        int ramSize = ramEnd.diff(ramStart).toInt();
        Assert.always(ramSize > 0);
        int heapSize = calculateMaxHeapSize(ramSize);
        int bitmapSize = ramSize - heapSize;
        Address bitmap = ramEnd.sub(bitmapSize);
        Address bitmapWordForRamStart = Lisp2Bitmap.getAddressOfBitmapWordFor(ramStart);
        Address bitmapBase = bitmap.subOffset(Offset.fromPrimitive(bitmapWordForRamStart.toUWord().toPrimitive()));

        // Only after this call can the VM execute any bytecode that involves updating the write barrier
        // such as 'putfield_o', 'astore_o', 'putstatic_o'... etc.
        Lisp2Bitmap.initialize(bitmap, bitmapBase, bitmapSize);

        // Get the special classes.
        HashTableKlass = bootstrapSuite.lookup("com.sun.squawk.util.Hashtable");
        ThreadKlass = bootstrapSuite.lookup("java.lang.Thread");
        IsolateKlass = bootstrapSuite.lookup("java.lang.Isolate");
        ObjectMemoryKlass = bootstrapSuite.lookup("java.lang.ObjectMemory");

        // Initialize the heap trace
        if ((HEAP_TRACE || GC.TRACING_SUPPORTED) && GC.isTracing(GC.TRACE_HEAP)) {
            traceHeapInitialize(ramStart, ramEnd);
        }

        // Create the marking stack.
        markingStack = new MarkingStack();

        // Create the visitor used to mark all the reachable objects during a collection.
        markVisitor = new MarkVisitor();

        // Create the visitor used to update all the reachable objects during a collection.
        updateVisitor = new UpdateVisitor();
    }


    /**
     * Computes the size (in bytes) of the largest memory space that may contain classes.
     *
     * @param heapSize   the size of the heap
     * @return the size (in bytes) of the largest memory space that may contain classes
     */
    private static int computeMaxClassAddressSpace(int heapSize) {
        int romSize = VM.getRomEnd().diff(VM.getRomStart()).toInt();
        int nvmSize = GC.getNvmSize();
        return Math.max(nvmSize, Math.max(heapSize, romSize));
    }

    /**
     * Computes the number of bits required to represent the range of values from
     * 0 up to but not including some limit.
     *
     * @param limit   the limit of the range of values
     * @return the number of bits required to represent values in the range <code>[0 .. limit)</code>
     */
    private static int bitsRequiredFor(int limit) {
        int bits = 0;
        while (limit != 0) {
            bits++;
            limit = limit >> 1;
        }
        return bits;
    }

    /**
     * Calculates the size (in bytes) of a bitmap that must contain a bit for every word in a memory
     * range of a given size.
     *
     * @param memorySize   the size (in bytes) of the memory range that must be covered by the bitmap
     * @return  the size (in bytes) of the bitmap that will contain a bit for every word in the memory
     */
    private static int calculateBitmapSize(int memorySize) {
        Assert.always(GC.roundDownToWord(memorySize) == memorySize);
        int alignment = HDR.BYTES_PER_WORD * HDR.BITS_PER_WORD;
        return GC.roundUpToWord((memorySize + (alignment - 1)) / alignment);
    }

    /**
     * Calculates the maximum heap size for a given memory size where the memory will also
     * contain a bitmap with a bit for every word in the heap. This calculation assumes that
     * the memory will only be used for the heap and bitmap.
     *
     * @param memorySize   the size of the memory to be partitioned into a heap and bitmap
     * @return the maximum size of heap that can be allocated in the memory while leaving
     *                     sufficient space for the bitmap
     */
    private static int calculateMaxHeapSize(int memorySize) {
        int heapSize = GC.roundDownToWord((memorySize * HDR.BITS_PER_WORD) / (HDR.BITS_PER_WORD + 1));
        Assert.always(memorySize >= heapSize + calculateBitmapSize(heapSize));
        return heapSize;
    }

    /**
     * {@inheritDoc}
     */
    void initialize(Address permanentMemoryStart, Address memoryStart, Address memoryEnd, Suite bootstrapSuite) {

        Assert.always(memoryEnd.roundUpToWord().eq(memoryEnd));
        Assert.always(memoryStart.roundUpToWord().eq(memoryStart));
        Assert.always(permanentMemoryStart.roundUpToWord().eq(permanentMemoryStart));

        this.memoryStart = memoryStart;
        this.memoryEnd = memoryEnd;

        initializeHeap(permanentMemoryStart);

        int youngGenerationSize = getYoungGenerationSize();
        Address youngGenerationEnd = youngGenerationStart.add(youngGenerationSize);

        /*
         * Set up allocation to occur within the young space.
         */
        GC.setRamAllocateParameters(youngGenerationStart, youngGenerationStart, youngGenerationEnd);

        /*
         * Output trace information.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            markingStack.setup(heapEnd, Lisp2Bitmap.getStart());
            traceVariables();
        }
    }

    /**
     * Initializes the heap, bitmap, marking stack and slice table based on the current memory
     * allocated to the collector.
     *
     * @param permanentMemoryStart the address at which the permanent objects were allocated
     */
    private void initializeHeap(Address permanentMemoryStart) {

        // This is the size used for aligning the heap size. It corresponds
        // to the number of bytes covered by a single word in the bitmap.
        int alignment = HDR.BITS_PER_WORD * HDR.BYTES_PER_WORD;

        // Determine the size of the memory that must be partitioned into the
        // object heap, bitmap, slice table and minimum marking stack.
        heapStart = memoryStart.roundUp(alignment);
        int memorySize = memoryEnd.diff(heapStart).toInt();

        // Determine the space reserved for the marking stack.
        int minimumMarkingStackSize = MarkingStack.MINIMUM_MARKING_STACK_SIZE * HDR.BYTES_PER_WORD;

        /*
         * Configuring the heap involves solving a complex set of simultaneous equations
         * involving the heap size, bitmap size and slice table size. This is achieved by
         * the iterative process in the loop further below. To limit the number of iterations
         * required, an initial value for the heap size is calculated by
         * assuming that the slice table size will be 0.
         */
        heapSize = calculateMaxHeapSize(memorySize);
        int bitmapSize = calculateBitmapSize(heapSize);

        // Align the heap size
        heapSize = GC.roundUp(heapSize, alignment);
        Assert.that(heapSize > 0, "zero heap size");

        boolean firstIteration = true;
        while (firstIteration || (heapSize + bitmapSize + sliceTableSize + minimumMarkingStackSize > memorySize)) {
            if (firstIteration) {
                firstIteration = false;
            } else {
                heapSize -= alignment;
            }

            // Determine the end of the heap
            heapEnd = heapStart.add(heapSize);

            // Determine the size of the bitmap which must cover the range [permanentMemoryStart .. heapEnd)
            bitmapSize = calculateBitmapSize(heapEnd.diff(permanentMemoryStart).toInt());

            // Determine max number of bits needed for an offset (in words) to a heap object relative to heap start
            int heapOffsetBits = bitsRequiredFor(heapSize / HDR.BYTES_PER_WORD);

            // Determine number of bits needed for an offset (in words) to a class relative to the start of any of the
            // spaces in which a class may be located (i.e. ROM, NVM or RAM).
            int maxClassSpaceSize = computeMaxClassAddressSpace(heapSize);
            classOffsetBits = bitsRequiredFor(maxClassSpaceSize / HDR.BYTES_PER_WORD);

            // Determine mask used to extract class offset bits from an encoded class word
            classOffsetMask = ((1 << classOffsetBits) - 1) << ClassWordTag.BIT_COUNT;

            // Determine the amount by which an encoded class word must be logically right shifted to
            // extra the slice-relative offset (in words) to the forwarded object
            sliceOffsetShift = classOffsetBits + ClassWordTag.BIT_COUNT;

            // Determine how many remaining bits in an encoded class word will be used for a slice-relative offset
            sliceOffsetBits = (HDR.BITS_PER_WORD - sliceOffsetShift);

            // Configure the slice table based on whether or not a slice is smaller than the heap
            if (sliceOffsetBits < heapOffsetBits) {
                sliceSize = (1 << sliceOffsetBits) * HDR.BYTES_PER_WORD;
                sliceCount = (heapSize + (sliceSize - 1)) / sliceSize;
                sliceTableSize = sliceCount * HDR.BYTES_PER_WORD;
            } else {
                sliceTableSize = HDR.BYTES_PER_WORD;
                sliceSize = heapSize;
                sliceCount = 1;
            }
        }

        Address top = memoryEnd.roundDown(alignment);
        sliceTable = top.sub(sliceTableSize);
        Address bitmap = sliceTable.sub(bitmapSize);
        Address bitmapBase = bitmap.subOffset(Offset.fromPrimitive(permanentMemoryStart.toUWord().toPrimitive() / HDR.BITS_PER_WORD));

        idealYoungGenerationSizePercent = DEFAULT_YOUNG_GENERATION_PERCENT;
        youngGenerationStart = heapStart;

        Lisp2Bitmap.initialize(bitmap, bitmapBase, bitmapSize);

        Assert.always(sliceTable.add(sliceTableSize).loeq(memoryEnd), "slice table overflows memory boundary");
        Assert.always(bitmap.add(bitmapSize).loeq(sliceTable), "bitmap collides with slice table");
        Assert.always(heapEnd.loeq(bitmap.sub(minimumMarkingStackSize)), "heap collides with marking stack");
        Assert.always((heapSize % alignment) == 0, "heap size is non-aligned");
        Assert.always((bitmapSize % HDR.BYTES_PER_WORD) == 0, "bitmap size is non-aligned");
        Assert.always((sliceTableSize % HDR.BYTES_PER_WORD) == 0, "slice table size is non-aligned");
        Assert.always(Lisp2Bitmap.getAddressOfBitmapWordFor(permanentMemoryStart).eq(Lisp2Bitmap.getStart()), "incorrect bitmap base");
    }

    /**
     * Process a given command line option that may be specific to the collector implementation.
     *
     * @param arg   the command line option to process
     * @return      true if <code>arg</code> was a collector option
     */
    boolean processCommandLineOption(String arg) {
        if (arg.startsWith("-young:")) {
            int percent = Integer.parseInt(arg.substring("-young:".length()));
            if (percent < 10 || percent > 100) {
                System.err.println("Warning: ratio specified for young generation invalid");
            } else {
                idealYoungGenerationSizePercent = percent;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Prints the usage message for the collector specific options (if any).
     *
     * @param out  the stream on which to print the message
     */
    void usage(java.io.PrintStream out) {
        out.println("    -young:<n>              young space size as % of heap (default="+DEFAULT_YOUNG_GENERATION_PERCENT+"%)");
    }

    /**
     * {@inheritDoc}
     */
    void copyObjectGraph(Address object, ObjectMemorySerializer.ControlBlock cb, boolean firstPass) {

    }

    /**
     * {@inheritDoc}
     */
    long freeMemory(Address allocationPointer) {
        return heapEnd.diff(allocationPointer).toPrimitive();
    }

    /**
     * {@inheritDoc}
     */
    long totalMemory() {
        return heapSize;
    }

    /**
     * Gets the actual size of the young generation based on the current start address
     * of the young generation, the ideal young generation size and the remaining
     * amount of free memory in the heap.
     *
     * @return the minimum of the ideal young generation size and the amount of memory
     *         between the current start of the young generation and the end of the heap
     */
    private int getYoungGenerationSize() {
        int size = getIdealYoungGenerationSize();
        int available = heapEnd.diff(youngGenerationStart).toInt();
        if (available < size) {
            return available;
        } else {
            return size;
        }
    }

    /**
     * Gets a size for the young generation based its ideal ratio to the heap size.
     *
     * @return the ideal young generation size
     */
    private int getIdealYoungGenerationSize() {
        return (heapSize * idealYoungGenerationSizePercent) / 100;
    }

    /*---------------------------------------------------------------------------*\
     *                               Collection                                  *
    \*---------------------------------------------------------------------------*/

    /**
     * {@inheritDoc}
     */
    boolean collectGarbage(Address youngGenerationEnd) {
        /*
         * Test and set the re-entry guard.
         */
        Assert.that(!collecting, "recursive call to Lisp2Collector");
        collecting = true;

        /*
         * Set up the limits of the space to be collected.
         */
        if (nextCollectionIsFull) {
            collectionStart = heapStart;
            nextCollectionIsFull = false;
        } else {
            collectionStart = youngGenerationStart;
        }
        collectionEnd = youngGenerationEnd;

        /*
         * Sets up the marking stack.
         */
        markingStack.setup(collectionEnd, Lisp2Bitmap.getStart());

        /*
         * Reset the marking recursion level.
         */
        markingRecursionLevel = MAX_MARKING_RECURSION;

        /*
         * Output trace information.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            traceVariables();
        }
        if ((HEAP_TRACE || GC.TRACING_SUPPORTED) && GC.isTracing(GC.TRACE_HEAP)) {
            traceHeap("Before collection", youngGenerationEnd);
        }

        /*
         * Clears the bitmap corresponding to the collection area.
         */
        Lisp2Bitmap.clearBitsFor(collectionStart, collectionEnd);

        // Phase1: Mark objects transitively from roots
        mark();

        // Phase2: Insert forward pointers in unused near object bits
        computeNewObjectLocations();

/*
        // Phase3: Adjust interior pointers using forward pointers from phase2
        update_object_pointers();
        // Phase4; Compact
        compact_objects();
        // Restore bci, pc, and stack pointer locks in heap
        Scheduler::gc_epilogue();
        // Update _class_list_base, etc
        Universe::update_relative_pointers();
*/


        /*
         * The next run of the collector will collect the whole heap if the space reclaimed by this collection
         * is less than the ideal size of the young generation.
         */
        nextCollectionIsFull = heapEnd.diff(youngGenerationStart).lt(Offset.fromPrimitive(getIdealYoungGenerationSize()));

        /*
         * Set the main RAM allocator to the young generation.
         */
        GC.setRamAllocateParameters(youngGenerationStart, youngGenerationStart, youngGenerationStart.add(getYoungGenerationSize()));

        /*
         * Output trace information.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            traceVariables();
        }
        if ((HEAP_TRACE || GC.TRACING_SUPPORTED) && GC.isTracing(GC.TRACE_HEAP)) {
            traceHeap("After collection", youngGenerationEnd);
        }

        /*
         * Reset the re-entry guard.
         */
        collecting = false;

        return false;
    }

    /*---------------------------------------------------------------------------*\
     *                               OopVisitor                                  *
    \*---------------------------------------------------------------------------*/

    /**
     * An OopVisitor is used by the general traversal routines to specialize the action taken for
     * each pointer within a traversed object.
     *
     */
    abstract class OopVisitor {

        /**
         * Visit an oop within an object.
         *
         * @param object  the base of address of an object
         * @param offset  the offset (in words) of the oop within the object to be visited
         * @return the value of the oop
         */
        abstract Address visitOop(Address object, int offset);
    }

    /*---------------------------------------------------------------------------*\
     *                        Shared traversal routines                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Indents a trace line according to the current object graph traversal recursion level.
     */
    void indentTrace() {
        int level = MAX_MARKING_RECURSION - markingRecursionLevel;
        for (int i = 0; i != level; ++i) {
            VM.print("  ");
        }
    }

    /**
     * Traverses all the objects in the collection space reachable from the GC roots.
     *
     * @param visitor  the visitor to apply to each pointer in the traversed objects
     */
    void traverseRoots(OopVisitor visitor) {
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("Lisp2Collector::traverseRoots --------------- Start");
        }
        for (int i = 0 ; i < VM.getGlobalOopCount() ; i++) {
            Address object = Address.fromObject(VM.getGlobalOop(i));
            if (!object.isZero()) {
                if (GC.TRACING_SUPPORTED && tracing()) {
                    VM.print("Lisp2Collector::traverseRoots - index = ");
                    VM.print(i);
                    VM.print(" object = ");
                    VM.printAddress(object);
                    VM.println();
                }
                visitor.visitOop(VM.getGlobalOopTable(), i);
            }
        }
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("Lisp2Collector::traverseRoots --------------- End");
        }
    }

    /**
     * Traverses all the objects in the collection space reachable from stack chunks of each thread.
     *
     * @param visitor  the visitor to apply to each pointer in the traversed objects
     */
    private void traverseStackChunks(OopVisitor visitor) {
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("Lisp2Collector::traverseStackChunks --------------- Start");
        }
        Object chunk = GC.getStackChunkList();
        while (chunk != null) {
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.print("Lisp2Collector::traverseStackChunks - chunk = ");
                VM.printAddress(chunk);
                VM.println();
            }
            traverseOopsInStackChunk(Address.fromObject(chunk), visitor);
            chunk = Unsafe.getObject(chunk, SC.next);
        }
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("Lisp2Collector::traverseStackChunks --------------- End");
        }
    }


    /**
     * Traverses all the oops within an object.
     *
     * @param object   the object being traversed
     * @param visitor  the visitor to apply to each pointer in <code>object</code>
     */
    void traverseOopsInObject(Address object, OopVisitor visitor) {
        if (GC.TRACING_SUPPORTED && tracing()) {
            indentTrace();
            VM.print("Lisp2Collector::traverseOopsInObject - object = ");
            VM.printAddress(object);
            VM.println();
        }
        Address associationOrKlass = visitor.visitOop(object, HDR.klass);
        Klass klass = VM.asKlass(visitor.visitOop(associationOrKlass, (int)FieldOffsets.java_lang_Klass$self));
        if (Klass.isSquawkArray(klass)) {
            switch (Klass.getClassID(klass)) {
                case CID.BOOLEAN_ARRAY:
                case CID.BYTE_ARRAY:
                case CID.CHAR_ARRAY:
                case CID.DOUBLE_ARRAY:
                case CID.FLOAT_ARRAY:
                case CID.INT_ARRAY:
                case CID.LONG_ARRAY:
                case CID.SHORT_ARRAY:
                case CID.UWORD_ARRAY:
                case CID.ADDRESS_ARRAY:
                case CID.STRING:
                case CID.STRING_OF_BYTES: {
                    break;
                }
                case CID.BYTECODE_ARRAY: {
                    visitor.visitOop(object, HDR.methodDefiningClass);
                    break;
                }
                case CID.GLOBAL_ARRAY: {
                    Klass gaklass = VM.asKlass(Unsafe.getObject(object, CS.klass));
                    if (GC.TRACING_SUPPORTED && tracing()) {
                        indentTrace();
                        VM.print("Lisp2Collector::traverseOopsInObject - globals for ");
                        VM.println(Klass.getInternalName(gaklass));
                    }
                    int end = CS.firstVariable + Klass.getRefStaticFieldsSize(gaklass);
                    for (int i = 0 ; i < end ; i++) {
                        Assert.that(i < GC.getArrayLength(object), "class state index out of bounds");
                        visitor.visitOop(object, i);
                    }
                    break;
                }
                case CID.LOCAL_ARRAY: {
                    traverseOopsInStackChunk(object, visitor);
                    break;
                }
                default: { // Pointer array
                    int length = GC.getArrayLength(object);
                    for (int i = 0; i < length; i++) {
                        visitor.visitOop(object, i);
                    }
                    break;
                }
            }
        } else { // Instance
            if (copyObjectGraphCB != null) {
                /*
                 * If the object is a hashtable and we are doing a graph copy then
                 * zero the first oop which is the transient field called entryTable.
                 */
                if(Klass.isSubtypeOf(klass, HashTableKlass)) {
                    Unsafe.setAddress(object, (int)FieldOffsets.com_sun_squawk_util_Hashtable$entryTable, Address.zero());
                }

            }

            /*
             * Update the oops
             */
            int nWords = Klass.getInstanceSize(klass);
            for (int i = 0 ; i < nWords ; i++) {
                if (Klass.isInstanceWordReference(klass, i)) {
                    visitor.visitOop(object, i);
                }
            }
        }
    }

    /**
     * Traverses the oops in an activation record.
     *
     * @param fp                     the frame pointer
     * @param isInnerMostActivation  specifies if this is the inner most activation frame on the chunk
     *                               in which case only the first local variable (i.e. the method pointer) is scanned
     * @param visitor                the visitor to apply to each traversed oop
     */
    private void traverseOopsInActivation(Address fp, boolean isInnerMostActivation, OopVisitor visitor) {
        Address mp  = Address.fromObject(Unsafe.getObject(fp, FP.method));
        Address previousFP = VM.getPreviousFP(fp);
        Address previousIP = VM.getPreviousIP(fp);

        /*
         * Trace.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            indentTrace();
            VM.print("Lisp2Collector::traverseOopsInActivation - fp = ");
            VM.printAddress(fp);
            VM.print(" mp = ");
            VM.printAddress(mp);
            VM.println();
        }

        /*
         * Adjust the previous IP and MP.
         */
        if (!previousIP.isZero()) {
            if (GC.TRACING_SUPPORTED && tracing()) {
                indentTrace();
                VM.println("Lisp2Collector::traverseOopsInActivation -- change previous IP");
            }

            /*
             * Adjust the MP
             */
            Assert.that(!previousFP.isZero(), "activation frame has null previousFP");
            Address oldPreviousMP = Address.fromObject(Unsafe.getObject(previousFP, FP.method));
//            Assert.that(!isInToSpace(oldPreviousMP), "a method was copied before activation frame of the method");
            Address newPreviousMP = visitor.visitOop(previousFP, FP.method);

            /*
             * Adjust the IP
             */
            Offset diff = newPreviousMP.diff(oldPreviousMP);
            previousIP = previousIP.addOffset(diff);
            VM.setPreviousIP(fp, previousIP);
            if (copyObjectGraphCB != null) {
                Address pointerAddress = fp.add(FP.returnIP * HDR.BYTES_PER_WORD);
//                recordPointer(pointerAddress);
            }

        } else {
            Assert.that(previousFP.isZero(), "previousFP should be null when previousIP is null");
        }

        /*
         * Get the method pointer and setup to go through the parameters and locals.
         */
        int localCount     = isInnerMostActivation ? 1 : MethodBody.decodeLocalCount(mp);
        int parameterCount = MethodBody.decodeParameterCount(mp);
        int mapOffset      = MethodBody.decodeOopmapOffset(mp);
        int bitOffset      = -1;
        int byteOffset     = 0;

        /*
         * Parameters.
         */
        int varOffset = FP.parm0;
        while (parameterCount-- > 0) {
            bitOffset++;
            if (bitOffset == 8) {
                bitOffset = 0;
                byteOffset++;
            }
            int bite = Unsafe.getByte(mp, mapOffset+byteOffset);
            boolean isOop = ((bite>>bitOffset)&1) != 0;
            if (isOop) {
                if (GC.TRACING_SUPPORTED && tracing()) {
                    indentTrace();
                    VM.print("Lisp2Collector::traverseOopsInActivation -- update parm at offset ");
                    VM.print(varOffset);
                    VM.println();
                }
                visitor.visitOop(fp, varOffset);
            }
            varOffset++; // Parameters go upwards
        }

        /*
         * Locals.
         */
        varOffset = FP.local0;
        while (localCount-- > 0) {
            bitOffset++;
            if (bitOffset == 8) {
                bitOffset = 0;
                byteOffset++;
            }
            int bite = Unsafe.getByte(mp, mapOffset + byteOffset);
            boolean isOop = ((bite >> bitOffset) & 1) != 0;
            if (isOop) {
                if (GC.TRACING_SUPPORTED && tracing()) {
                    indentTrace();
                    VM.print("Lisp2Collector::traverseOopsInActivation -- update local at offset ");
                    VM.print(varOffset);
                    VM.println();
                }
                visitor.visitOop(fp, varOffset);
            }
            varOffset--; // Locals go downwards
        }
    }

    /**
     * Traverses the oops in a stack chunk.
     *
     * @param chunk    the stack chunk to traverse
     * @param visitor  the visitor to apply to each oop in the traversed objects
     */
    void traverseOopsInStackChunk(Address chunk, OopVisitor visitor) {
        Address fp = Address.fromObject(Unsafe.getObject(chunk, SC.lastFP));

        /*
         * Trace.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println();
            indentTrace();
            VM.print("Lisp2Collector::traverseOopsInStackChunk - chunk = ");
            VM.printAddress(chunk);
            VM.println();
        }

        /*
         * Update the pointers in the header part of the stack chunk
         */
        int oopMap = SC.oopMap;
        int index = 0;
        while (oopMap != 0) {
            if ((oopMap & 1) == 1) {
                visitor.visitOop(Address.fromObject(chunk), index);
            }
            index++;
            oopMap = oopMap >> 1;
        }

        /*
         * Update the pointers in each activation frame
         */
        boolean isInnerMostActivation = true;
        while (!fp.isZero()) {
            traverseOopsInActivation(fp, isInnerMostActivation, visitor);
            fp = VM.getPreviousFP(fp);
            isInnerMostActivation = false;
        }
    }

    /**
     * Traverses all the oops that in the old generation that have their write barrier bit set.
     *
     * @param start   the start address of the old generation
     * @param end     the end address of the old generation
     * @param visitor the visitor to apply to each pointer in the traversed objects
     */
    void traverseWriteBarrierOops(Address start, Address end, OopVisitor visitor) {
        Lisp2Bitmap.Iterator.start(start, end);
        Address oopAddress;
        while (!(oopAddress = Lisp2Bitmap.Iterator.getNext()).isZero()) {
            if (GC.TRACING_SUPPORTED && tracing()) {
                indentTrace();
                VM.print("Lisp2Collector::traverseWriteBarrierOops -- update oop at ");
                VM.printAddress(oopAddress);
                VM.println();
            }
            visitor.visitOop(oopAddress, 0);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                            Marking routines                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Visitor that implements the mark phase.
     */
    private final MarkVisitor markVisitor;

    /**
     * The visitor used during marking.
     */
    final class MarkVisitor extends OopVisitor {

        /**
         * {@inheritDoc}
         */
        Address visitOop(Address object, int offset) {
            Address oop = Address.fromObject(Unsafe.getObject(object, offset));
            if (!oop.isZero()) {
                markObject(oop);
            }
            return oop;
        }
    }

    /**
     * Marks all the reachable objects in the collection area.
     */
    private void mark() {
        traverseRoots(markVisitor);

        /*
         * Only mark the oops in the old generation if this is not a full GC
         */
        if (heapStart.ne(collectionStart)) {
            traverseWriteBarrierOops(heapStart, collectionStart, markVisitor);
        }

        traverseStackChunks(markVisitor);
    }



    private static final boolean VERBOSE_MARK_OBJECT_TRACE = false;

    /**
     * Sets the mark bit for a given object if it is within the current collection
     * space. If the mark bit was not previously set for the object, then all of
     * it's pointer fields are also marked recursively with this operation.
     *
     * @param  object the object to (potentially) be marked
     */
   void markObject(Address object) {

       /*
        * Ensure that the object is not forwarded
        */
        Assert.that(!isForwarded(object));

       /*
        * If the object is in the collection space and the corresponding bit
        * is not set then set it and traverse the objects pointers
        */
        if (inCollectionSpace(object)) {
            if (!Lisp2Bitmap.testAndSetBitFor(object)) {
                if (markingRecursionLevel == 0) {
                    if (VERBOSE_MARK_OBJECT_TRACE && GC.TRACING_SUPPORTED && tracing()) {
                        indentTrace();
                        VM.print("Lisp2Collector::markObject - object = ");
                        VM.printAddress(object);
                        VM.print(" klass = ");
                        VM.print(GC.getKlass(object).getInternalName());
                        VM.println("  {pushed on marking stack}");
                    }
                    markingStack.push(object);
                    //            updateHighWaterMark();
                } else {
                    if (GC.TRACING_SUPPORTED && tracing()) {
                        indentTrace();
                        VM.print("Lisp2Collector::markObject - object = ");
                        VM.printAddress(object);
                        VM.print(" klass = ");
                        VM.print(GC.getKlass(object).getInternalName());
                        VM.println();
                    }
                    markingStack.push(object);
                    --markingRecursionLevel;
                    traverseOopsInObject(object, this.markVisitor);
                    while (!(object = markingStack.pop()).isZero()) {
                        traverseOopsInObject(object, this.markVisitor);
                    }
                    ++markingRecursionLevel;
                }
            } else {
                if (VERBOSE_MARK_OBJECT_TRACE && GC.TRACING_SUPPORTED && tracing()) {
                    indentTrace();
                    VM.print("Lisp2Collector::markObject - object = ");
                    VM.printAddress(object);
                    VM.print(" klass = ");
                    VM.print(GC.getKlass(object).getInternalName());
                    VM.println(" {already marked}");
                }
            }
        } else {
            if (VERBOSE_MARK_OBJECT_TRACE && GC.TRACING_SUPPORTED && tracing()) {
                indentTrace();
                VM.print("Lisp2Collector::markObject - object = ");
                VM.printAddress(object);
                VM.print(" klass = ");
                VM.print(GC.getKlass(object).getInternalName());
                VM.println(" {not in collection space}");
            }
        }
    }

    /*-----------------------------------------------------------------------*\
     *                         Compute Address Phase                         *
    \*-----------------------------------------------------------------------*/

    /**
     * Computes the addresses to which objects will be moved and encodes these target addresses
     * into the objects' headers.
     */
    private void computeNewObjectLocations() {
        Address free = collectionStart;
        Address object;

        /*
         * Clear the slice table
         */
        for (int i = 0; i != sliceCount; ++i) {
            Unsafe.setUWord(sliceTable, i, UWord.zero());
        }

        Lisp2Bitmap.Iterator.start(free, collectionEnd);
        while (!(object = Lisp2Bitmap.Iterator.getNext()).isZero()) {

            Assert.that(!isForwarded(object));
            Klass klass = GC.getKlass(object);
            int headerSize = object.diff(GC.oopToBlock(klass, object)).toInt();

            Address objectDestination = free.add(headerSize);
            Offset delta = object.diff(objectDestination);
            int size = GC.getBodySize(klass, object);

            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.print("Lisp2Collector::computeAddresses - object = ");
                VM.printAddress(object);
                VM.print(" klass = ");
                VM.print(GC.getKlass(object).getInternalName());
                VM.print(" delta = ");
                VM.printOffset(delta);
                VM.print(" size = ");
                VM.print(size);
                VM.println();
            }

            forwardObject(object, headerSize, objectDestination);
            free = objectDestination.add(size);
        }
    }

    /**
     * Determines if a given object is within the heap.
     *
     * @param object   the object to test
     * @return true if <code>object</code> is within heap
     */
    private boolean inHeap(Address object) {
        return object.hi(heapStart) && object.loeq(heapEnd);
    }

    /**
     * Gets the offset (in bytes) of an object in the heap from the start of the heap
     *
     * @param object  the object to test
     * @return the offset (in bytes) of <code>object</code> in the heap from the start of the heap
     */
    private Offset getOffsetInHeap(Address object) {
        Assert.that(inHeap(object));
        return object.diff(heapStart);
    }

    /**
     * Gets an object in the heap given a offset (in bytes) to the object from the start of the heap.
     *
     * @param offset   the offset (in bytes) of the object to retrieve
     * @return the object at <code>offset</code> bytes from the start of the heap
     */
    private Address getObjectInHeap(Offset offset) {
        return heapStart.addOffset(offset);
    }

    /**
     * Converts the header of an object to encode the address to which it will be forwarded
     * when the heap is compacted.
     *
     * @param object      the object that will be moved during compaction
     * @param headerSize  the size (in bytes) of <code>object</code>'s header
     * @param objectDestination the address to which the object will be moved during compaction
     */
    private void forwardObject(Address object, int headerSize, Address objectDestination) {


        /*
         * Compute the offset (in words) to the class of association relative to the
         * space it is in (ROM, NVM or the heap)
         */
        Address classOrAssociation = Address.fromObject(Unsafe.getObject(object, HDR.klass));
        Offset classOrAssociationOffset;
        int tag;
        if (inHeap(classOrAssociation)) {
            classOrAssociationOffset = getOffsetInHeap(classOrAssociation);
            tag = ClassWordTag.HEAP;
        } else if (VM.inRom(classOrAssociation)) {
            classOrAssociationOffset = VM.getOffsetInRom(classOrAssociation);
            tag = ClassWordTag.ROM;
        } else {
            Assert.that(GC.inNvm(classOrAssociation));
            classOrAssociationOffset = GC.getOffsetInNvm(classOrAssociation);
            tag = ClassWordTag.NVM;
        }
        classOrAssociationOffset = classOrAssociationOffset.bytesToWords(); // convert to word-base offset
        Assert.that(classOrAssociationOffset.ge(Offset.zero()) && classOrAssociationOffset.lt(Offset.fromPrimitive(1 << classOffsetBits)));

        /*
         * Compute the forwarding offset which is relative to a slice
         */
        int sliceIndex = getSliceIndexForObject(object);
        Address sliceDestination = Address.fromObject(Unsafe.getObject(sliceTable, sliceIndex));
        Offset offsetInSlice;
        if (sliceDestination.isZero()) {
            sliceDestination = objectDestination;
            offsetInSlice = Offset.zero();
            Unsafe.setObject(sliceTable, sliceIndex, sliceDestination);
        } else {
            offsetInSlice = objectDestination.diff(sliceDestination);
        }
        offsetInSlice = offsetInSlice.bytesToWords(); // convert to word-base offset
        Assert.that(offsetInSlice.ge(Offset.zero()) && offsetInSlice.lt(Offset.fromPrimitive(1 << sliceOffsetBits)));

        /*
         * Construct the forwarded header
         */
        UWord classWord = UWord.fromPrimitive(offsetInSlice.toPrimitive() << sliceOffsetShift);
        classWord = classWord.or(UWord.fromPrimitive(classOrAssociationOffset.toPrimitive() << ClassWordTag.BIT_COUNT));
        classWord = classWord.or(UWord.fromPrimitive(tag));
        Unsafe.setAddress(object, HDR.klass, Address.zero().or(classWord));
    }

    /**
     * Determines if a given object has been forwarded.
     *
     * @param object   the object to test
     * @return true if <code>object</code>'s header indicates that it has been forwarded
     */
    private static boolean isForwarded(Address object) {
        return !ClassWordTag.isPointer(Address.fromObject(Unsafe.getObject(object, HDR.klass)).toUWord());
    }

    /**
     * Get the forwarding address of an object.
     *
     * @param  object  an object that has been forwarded
     * @return the forwarding address of <code>object</code>
     */
    private Address getForwardedObject(Address object) {
        Assert.that(isForwarded(object));
        UWord classWord = Address.fromObject(Unsafe.getObject(object, HDR.klass)).toUWord();
        int sliceIndex = getSliceIndexForObject(object);
        Offset offsetInSlice = Offset.fromPrimitive(classWord.toPrimitive() >> sliceOffsetShift).wordsToBytes();
        Address sliceDestination = Address.fromObject(Unsafe.getObject(sliceTable, sliceIndex));
        return sliceDestination.addOffset(offsetInSlice);
    }

    /**
     * Get the forwarding pointer of an object.
     *
     * @param object   the object pointer
     * @return the forwarding pointer
     */
    private Address getPossiblyForwardedObject(Address object) {
        if (isForwarded(object)) {
            return getForwardedObject(object);
        } else {
            return object;
        }
    }

    /**
     * Get the forwarding pointer of an object.
     *
     * @param object   the object pointer
     * @return the forwarding pointer
     */
    private Address getPossiblyForwardedObject(Object object) {
        return getPossiblyForwardedObject(Address.fromObject(object));
    }

    /**
     * Gets the class of an object.
     *
     * @param object  an object
     * @return the class of <code>object</code>
     */
    private Address getKlass(Address object) {
        Address classOrAssociation;
        if (isForwarded(object)) {
            UWord classWord = Address.fromObject(Unsafe.getObject(object, HDR.klass)).toUWord();
            int tag = (int)classWord.toPrimitive() & ClassWordTag.MASK;
            Offset classOrAssociationOffset = Offset.fromPrimitive((classWord.toPrimitive() & classOffsetMask) >> ClassWordTag.BIT_COUNT).wordsToBytes();
            switch (tag) {
                case ClassWordTag.HEAP: classOrAssociation = getObjectInHeap(classOrAssociationOffset);   break;
                case ClassWordTag.ROM:  classOrAssociation = VM.getObjectInRom(classOrAssociationOffset); break;
                case ClassWordTag.NVM:  classOrAssociation = GC.getObjectInNvm(classOrAssociationOffset); break;
                default: classOrAssociation = Address.zero(); Assert.shouldNotReachHere();                break;
            }
        } else {
            classOrAssociation = Address.fromObject(Unsafe.getObject(object, HDR.klass));
        }

        return Address.fromObject(Unsafe.getObject(classOrAssociation, (int)FieldOffsets.java_lang_Klass$self));
    }


    /**
     * Computes which slice a given object falls into.
     *
     * @param object      the object to test
     * @return the slice into which <code>object</code> falls
     */
    private int getSliceIndexForObject(Address object) {
        Offset heapOffset = getOffsetInHeap(object);
        return (int)(heapOffset.toPrimitive() / sliceSize);
    }

    /*---------------------------------------------------------------------------*\
     *                            Update routines                                *
    \*---------------------------------------------------------------------------*/


    /**
     * Visitor the implements the update phase.
     */
    private final UpdateVisitor updateVisitor;

    /**
     * The visitor used during marking.
     */
    final class UpdateVisitor extends OopVisitor {

        /**
         * {@inheritDoc}
         */
        Address visitOop(Address object, int offset) {
            Address oop = Address.fromObject(Unsafe.getObject(object, offset));
            if (!oop.isZero()) {
//                markObject(oop);
            }
            return oop;
        }
    }



    private void updateInternalStackChunkPointers() {
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("Lisp2Collector::updateInternalStackChunkPointers --------------- Start");
        }
        Address chunk = Address.fromObject(GC.getStackChunkList());
        while (!chunk.isZero()) {
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.print("Lisp2Collector::updateInternalStackChunkPointers - chunk = ");
                VM.printAddress(chunk);
                VM.println();
            }
            if (isForwarded(chunk)) {
                Address chunkDestination = getForwardedObject(chunk);
                updateInternalStackChunkPointers(chunk, chunkDestination);
            }
            chunk = Address.fromObject(Unsafe.getObject(chunk, SC.next));
        }
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("Lisp2Collector::traverseStackChunks --------------- End");
        }

    }

    /**
     * Update the internal pointers within a stack chunk.
     *
     * @param chunk  the stack chunk
     */
    private void updateInternalStackChunkPointers(Address oldChunk, Address newChunk) {

        Offset offsetToPointer = Offset.fromPrimitive(SC.lastFP).wordsToBytes();
        Address oldFP = Address.fromObject(Unsafe.getObject(oldChunk.addOffset(offsetToPointer), 0));
        while (!oldFP.isZero()) {
            Address newPointerAddress = newChunk.addOffset(offsetToPointer);
            Offset delta = oldFP.diff(oldChunk);
            Address newFP = newChunk.addOffset(delta);
            Unsafe.setAddress(newPointerAddress, 0, newFP);
            if (copyObjectGraphCB != null) {
//                recordPointer(newPointerAddress);
            }

            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.print("CheneyCollector::updateInternalStackChunkPointers - offset = ");
                VM.printOffset(offsetToPointer);
                VM.print(" oldFP = ");
                VM.printAddress(oldFP);
                VM.print(" newFP = ");
                VM.printAddress(newFP);
                VM.println();
            }

            offsetToPointer = delta.add(FP.returnFP * HDR.BYTES_PER_WORD);
            oldFP = Address.fromObject(Unsafe.getObject(oldChunk.addOffset(offsetToPointer), 0));
        }
    }

    /*---------------------------------------------------------------------------*\
     *                     Object state accessors and tests                      *
    \*---------------------------------------------------------------------------*/

    /**
     * Determines if a given object is within the range of memory being collected.
     *
     * @param object  the object to test
     * @return true if <code>object</code> lies within the range of memory being collected
     */
    private boolean inCollectionSpace(Address object) {
        // The test is exclusive of the range's start and inclusive of the range's end
        // which accounts for objects always having non-zero-length headers but
        // possibly having zero length bodies
        return object.hi(collectionStart) && object.loeq(collectionEnd);
    }

    /*---------------------------------------------------------------------------*\
     *                           Marking stack                                   *
    \*---------------------------------------------------------------------------*/

    static class MarkingStack {

        /**
         * The minimum number of words to be reserved for the marking stack. The actual size of
         * the stack varies on each collection depending on the amount of the heap that is currently
         * unused (i.e. the space between heapEnd and youngGenerationEnd).
         */
        final static int MINIMUM_MARKING_STACK_SIZE = 10;

        /**
         * A compile time constant that can be set to true to test the behaviour when
         * the marking stack overflows.
         */
        private final static boolean TESTMARKSTACKOVERFLOW = Klass.DEBUG;

        /**
         * The base address of the stack.
         */
        private Address base;

        /**
         * The number of slots in the stack.
         */
        private int size;

        /**
         * The index of the slot one past the top element on the stack.
         */
        private int index;

        /**
         * Specifies if the marking stack overflowed.
         */
        private boolean overflowed;

        /**
         * Initializes the parameters of this stack.
         *
         * @param base  the address of the stack's first slot
         * @param end   the address one past the stack's last slot
         */
        void setup(Address base, Address end) {
            this.base = base;
            this.size = end.diff(base).toInt() >> HDR.LOG2_BYTES_PER_WORD;
            this.index = 0;
            this.overflowed = false;
        }

        /**
         * Pushes an object on the marking stack or sets the flag indicating that
         * the marking stack overflowed if the stack is full.
         *
         * @param object   the (non-null) object to push on the stack
         */
        void push(Address object) {
//        Assert.that(stackInUse);
            Assert.that(!object.isZero());
            if (index == size || TESTMARKSTACKOVERFLOW) {
                overflowed = true;
            } else {
                Unsafe.setAddress(base, index++, object);
            }
        }

        /**
         * Pops a value off the stack and returns it.
         *
         * @return  the value popped of the top of the stack or null if the stack was empty
         */
        Address pop() {
            if (index == 0) {
                return Address.zero();
            } else {
                return Address.fromObject(Unsafe.getObject(base, --index));
            }
        }

        /**
         * Determines if an attempt has been made to push an object to this stack when it
         * was full since the last call to {@link #setup(Address, Address)}.
         *
         * @return true if the stack has overflowed
         */
        boolean hasOverflowed() {
            return overflowed;
        }

        /**
         * Trace the this marking stack's variables.
         *
         * @param parent  the collector context of this stack
         */
        void traceVariables(GarbageCollector parent) {
            parent.traceVariable("markingStack.size", size);
            parent.traceVariable("markingStack.base", base);
            parent.traceVariable("markingStack.index", index);
            parent.traceVariable("markingStack.overflowed", overflowed ? 1 : 0);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                                 Tracing                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * Tests to see if tracing is enabled.
     *
     * @return true if it is
     */
    private boolean tracing() {
        if (GC.TRACING_SUPPORTED) {
            if (copyObjectGraphCB == null) {
                return GC.isTracing(GC.TRACE_COLLECTION) && collectionCount >= GC.getTraceThreshold();
            } else {
                return GC.isTracing(GC.TRACE_OBJECT_GRAPH_COPYING) && collectionCount >= GC.getTraceThreshold();
            }
        }
        return false;
    }

    /**
     * Trace the collector variables.
     */
    void traceVariables() {
        /*
         * Output trace information.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("Lisp2Collector variables");
            traceVariable("memoryStart", memoryStart);
            traceVariable("memoryEnd", memoryEnd);
            traceVariable("memorySize", memoryEnd.diff(memoryStart).toInt());
            traceVariable("bitmap", Lisp2Bitmap.getStart());
            traceVariable("bitmapEnd", Lisp2Bitmap.getEnd());
            traceVariable("bitmapSize", Lisp2Bitmap.getSize());
            traceVariable("bitmapBase", Lisp2Bitmap.getBase());

            traceVariable("sliceTable", sliceTable);
            traceVariable("sliceTableEnd", sliceTable.add(sliceTableSize));
            traceVariable("sliceTableSize", sliceTableSize);
            traceVariable("sliceOffsetShift", sliceOffsetShift);
            traceVariable("sliceOffsetBits", sliceOffsetBits);
            traceVariable("sliceSize", sliceSize);
            traceVariable("sliceCount", sliceCount);

            markingStack.traceVariables(this);

            traceVariable("heapStart", heapStart);
            traceVariable("heapEnd", heapEnd);
            traceVariable("heapSize", heapSize);

            int youngGenerationSize = getYoungGenerationSize();
            traceVariable("youngGenerationStart", youngGenerationStart);
            traceVariable("youngGenerationEnd", youngGenerationStart.add(youngGenerationSize));
            traceVariable("youngGenerationSize", youngGenerationSize);

            int overhead = 100 - ((heapSize * 100) / (memoryEnd.diff(memoryStart).toInt()));
            traceVariable("overhead(%)", overhead);
        }
    }


    /**
     * {@inheritDoc}
     */
    void traceHeap(String description, Address allocationPointer) {
        traceHeapStart(description, freeMemory(allocationPointer) );

        int youngGenerationSize = getYoungGenerationSize();
        Address youngGenerationEnd = youngGenerationStart.add(youngGenerationSize);

        traceHeapSegment("sliceTable", sliceTable, sliceTable.add(sliceTableSize));
        traceHeapSegment("bitmap", Lisp2Bitmap.getStart(), Lisp2Bitmap.getEnd());
        traceHeapSegment("minimumMarkingStack", heapEnd, Lisp2Bitmap.getStart());
        traceHeapSegment("heap{unused}", youngGenerationEnd, heapEnd);
        traceHeapSegment("heap{youngGenerationFree}", allocationPointer, youngGenerationEnd);
        traceHeapSegment("heap{youngGenerationAllocated}", youngGenerationStart, allocationPointer);
        traceHeapSegment("heap{oldGeneration}", heapStart,youngGenerationStart);

        traceHeapEnd();
    }
}
