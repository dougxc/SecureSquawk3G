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
import com.sun.squawk.util.BitSet;
import com.sun.squawk.util.Assert;

/**
 * The classic two-space Cheney garbage collector.
 *
 * @author Bernd Mathiske, Nik Shaylor, Doug Simon
 */
final class CheneyCollector extends GarbageCollector {

    /**
     * Guard against re-entry.
     */
    private boolean collecting;

    /**
     * Guard against incorrect sequencing during object graph copying.
     */
    private boolean expectingCopyObjectGraphSecondPass;

    /**
     * If non-null, then this collector is operating in the context of a call to 'copyObjectGraph'
     * (as opposed to 'collectGarbage').
     */
    private ObjectMemorySerializer.ControlBlock copyObjectGraphCB;

    /**
     * Start address of 'from' space.
     */
    private Address fromSpaceStartPointer;

    /**
     * End address of 'from' space.
     */
    private Address fromSpaceEndPointer;

    /**
     * Start address of 'to' space.
     */
    private Address toSpaceStartPointer;

    /**
     * End address of 'to' space.
     */
    private Address toSpaceEndPointer;

    /**
     * Address of current allocation point in 'to' space.
     */
    private Address toSpaceAllocationPointer;

    /**
     * The class of com.sun.squawk.util.Hashtable.
     */
    private final Klass HashTableKlass;

    /**
     * The of java.lang.Thread
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
     * The decoder used to decode the type table of a method.
     */
    private final VMBufferDecoder decoder;

    /**
     * Creates a CheneyCollector.
     *
     * @param ramStart       start of the RAM allocated to the VM
     * @param ramEnd         end of the RAM allocated to the VM
     * @param bootstrapSuite the bootstrap suite
     */
    CheneyCollector(Address ramStart, Address ramEnd, Suite bootstrapSuite) {
        /*
         * Get the special classes.
         */
        HashTableKlass = bootstrapSuite.lookup("com.sun.squawk.util.Hashtable");
        ThreadKlass = bootstrapSuite.lookup("java.lang.Thread");
        IsolateKlass = bootstrapSuite.lookup("java.lang.Isolate");
        ObjectMemoryKlass = bootstrapSuite.lookup("java.lang.ObjectMemory");

        /*
         * Initialize the heap trace
         */
        if ((HEAP_TRACE || GC.TRACING_SUPPORTED) && GC.isTracing(GC.TRACE_HEAP)) {
            traceHeapInitialize(ramStart, ramEnd);
        }

        decoder = Klass.DEBUG ? new VMBufferDecoder() : null;
    }

    /**
     * {@inheritDoc}
     */
    void initialize(Address ignored, Address memoryStart, Address memoryEnd, Suite bootstrapSuite) {

        /*
         * Set the start of 'from' space.
         */
        fromSpaceStartPointer = memoryStart.roundUpToWord();

        /*
         * Calculate the semi space size.
         */
        int heapSize = memoryEnd.diff(fromSpaceStartPointer).toInt();
        int semispaceSize = GC.roundDownToWord(heapSize / 2);
        Assert.always(heapSize > 0);

        /*
         * Set the end of 'from' space.
         */
        fromSpaceEndPointer = fromSpaceStartPointer.add(semispaceSize);

        /*
         * Set up the 'to' space pointers.
         */
        toSpaceStartPointer = fromSpaceEndPointer;
        toSpaceEndPointer   = toSpaceStartPointer.add(semispaceSize);

        /*
         * Set up the current allocation point.
         */
        toSpaceAllocationPointer = toSpaceStartPointer;

        /*
         * Check that both semispaces have the same size.
         */
        Assert.that(fromSpaceEndPointer.diff(fromSpaceStartPointer).eq(toSpaceEndPointer.diff(toSpaceStartPointer)), "semi-spaces are different sizes");

        /*
         * Set the main RAM allocator to the 'to' space.
         */
        GC.setRamAllocateParameters(toSpaceStartPointer, toSpaceAllocationPointer, toSpaceEndPointer);

        /*
         * Output trace information.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            traceVariables();
        }
    }

    /**
     * Returns the amount of free memory in the system. Calling the <code>gc</code>
     * method may result in increasing the value returned by <code>freeMemory.</code>
     *
     * @param  allocationPointer  the current allocationPointer
     * @return an approximation to the total amount of memory currently
     *         available for future allocated objects, measured in bytes.
     */
    long freeMemory(Address allocationPointer) {
        return toSpaceEndPointer.diff(allocationPointer).toPrimitive();
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
    long totalMemory() {
        return toSpaceEndPointer.diff(toSpaceStartPointer).toPrimitive();
    }

    /**
     * Sets a region of memory that must not be written to. This is only used
     * when debugging problems relating to this specific garbage collector.
     *
     * @param start   the start of the memory region to be protected
     * @param end     the endof the meory region to be protected
     */
    private static native void memoryProtect(Address start, Address end);

    /**
     * Switch over 'from' space and 'to' space.
     */
    private void toggleSpaces() {
        Address newStartPointer  = fromSpaceStartPointer;
        Address newEndPointer    = fromSpaceEndPointer;

        fromSpaceStartPointer    = toSpaceStartPointer;
        fromSpaceEndPointer      = toSpaceEndPointer;

        toSpaceStartPointer      = newStartPointer;
        toSpaceEndPointer        = newEndPointer;

        toSpaceAllocationPointer = toSpaceStartPointer;

        /*
         * Output trace information.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("CheneyCollector::toggleSpaces");
            traceVariables();
        }

    }

    /**
     * Gets the class of an object. This method takes into account that any of the
     * following may have been moved: the object itself, its class or its association.
     *
     * @param object the object address
     * @return the klass
     */
    private Klass getKlass(Address object) {
        object = getPossiblyForwardedObject(object);
        Object classWord = Unsafe.getObject(object, HDR.klass);
        Address something = getPossiblyForwardedObject(classWord);
        Address klass = Address.fromObject(Unsafe.getObject(something, (int)FieldOffsets.java_lang_Klass$self));
        return VM.asKlass(getPossiblyForwardedObject(klass));
    }

    /**
     * Set the forwarding pointer of an object.
     *
     * @param object         the object pointer
     * @param forwardPointer the object forwarding pointer
     */
    private void setForwardPointer(Address object, Address forwardPointer) {
        memoryProtect(Address.zero(), Address.zero());
        if (copyObjectGraphCB != null) {
            ObjectMemorySerializer.ControlBlock cb = copyObjectGraphCB;

            // If this fails, then there is an error in the calculated required minimum
            // size of the forwarding repair map
            Assert.that(cb.size >= 2, "forwarding repair map is too small");

            UWord classWord = Address.fromObject(Unsafe.getObject(object, HDR.klass)).toUWord();
            Unsafe.setAddress(cb.memory, --cb.size, object);
            Unsafe.setUWord(cb.memory, --cb.size, classWord);
        }
        Unsafe.setAddress(object, HDR.klass, forwardPointer.or(UWord.fromPrimitive(HDR.forwardPointerBit)));
        memoryProtect(fromSpaceStartPointer, fromSpaceEndPointer);
    }

    /**
     * Get the forwarded copy of an object.
     *
     * @param  object  the pointer to an object that has been forwarded
     * @return the forwarding pointer
     */
    private static Address getForwardedObject(Address object) {
        Address classWord = Address.fromObject(Unsafe.getObject(object, HDR.klass));
        Assert.that(classWord.and(UWord.fromPrimitive(HDR.forwardPointerBit)).ne(Address.zero()), "object is not forwarded");
        return classWord.and(UWord.fromPrimitive(~HDR.headerTagMask));
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
     * Test to see if an object is in 'from' space.
     *
     * @param pointer the object pointer
     * @return true if it is
     */
    private boolean isInFromSpace(Address pointer) {
        return fromSpaceStartPointer.loeq(pointer) && pointer.lo(fromSpaceEndPointer);
    }

    /**
     * Test to see if an object is in 'to' space.
     *
     * @param pointer the object pointer
     * @return true if it is
     */
    private boolean isInToSpace(Address pointer) {
        return toSpaceStartPointer.loeq(pointer) && pointer.lo(toSpaceEndPointer);
    }

    /**
     * Tests the class pointer word of an object to see if it is a forwarding pointer.
     *
     * @param object   the object to test
     * @return boolean true if object has been forwarded
     */
    private boolean isForwarded(Address object) {
        UWord classWord = Address.fromObject(Unsafe.getObject(object, HDR.klass)).toUWord();
        return classWord.and(UWord.fromPrimitive(HDR.forwardPointerBit)).ne(UWord.zero());
    }

    /**
     * Copy an object from the 'from' space to the 'to' space. If the object
     * reference supplied is not in the 'from' space the object is not copied
     * and the address of the supplied object is returned. If the object reference
     * points to an object that has already been copied to the 'to' space then
     * the address of the already copied object is returned.
     *
     * @param object the object to copy
     * @return the address of the copied object
     */
    private Address copyObject(final Address object) {

        /*
         * Check that the pointer is word aligned.
         */
        Assert.that(object.roundUpToWord().eq(object), "cannot copy unaligned object");

        /*
         * Trace.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.print("CheneyCollector::copyObject - object = ");
            VM.printAddress(object);
        }

        /*
         * Deal with null pointers.
         */
        if (object.isZero()) {
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.println(" is null");
            }
            return null;
        }

        /*
         * Check to see if the pointer is in the space we are collecting.
         */
        if (!isInFromSpace(object)) {
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.print(" klass = ");
                VM.print(getKlass(object).getInternalName());
                VM.println(" Not in from space ");
            }
            return object;
        }

        /*
         * Certain objects must never be copied.
         */
        Assert.that(object.toObject() != this, "cannot copy the CheneyCollector object");

        /*
         * Check to see if the pointer is in the space we are collecting.
         */
        if (isForwarded(object)) {
            Address forwardPointer = getForwardedObject(object);
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.print(" klass = ");
                VM.print(getKlass(forwardPointer).getInternalName());
                VM.print(" already forwarded to ");
                VM.printAddress(forwardPointer);
                VM.println();
            }
            Assert.that(forwardPointer.roundUpToWord().eq(forwardPointer), "unaligned forward pointer");
            return forwardPointer;
        }

        /*
         * Copy the object.
         */
        Address block  = GC.oopToBlock(getKlass(object), object);
        int headerSize = object.diff(block).toInt();
        int blockSize  = headerSize + GC.getBodySize(getKlass(object), object);
        Assert.that(GC.blockToOop(block).eq(object), "mis-sized header for copied object");
        VM.copyBytes(block, 0, toSpaceAllocationPointer, 0, blockSize, false);
/*if[TYPEMAP]*/
        if (VM.usingTypeMap()) {
            Unsafe.copyTypes(block, toSpaceAllocationPointer, blockSize);
        }
/*end[TYPEMAP]*/
        Address copiedObject = toSpaceAllocationPointer.add(headerSize);
        Assert.that(copiedObject.roundUpToWord().eq(copiedObject), "unaligned copied object");
        toSpaceAllocationPointer = toSpaceAllocationPointer.add(blockSize);
        setForwardPointer(object, copiedObject);

        /*
         * Trace.
         */
        Klass klass = getKlass(copiedObject);
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.print(" blockSize = ");
            VM.print(blockSize);
            VM.print(" klass = ");
            VM.print(Klass.getInternalName(klass));
            VM.print(" copied to ");
            VM.printAddress(copiedObject);
            VM.println();
        }

        /*
         * If the object is a stack chunk then update all the frame pointers.
         */
        if (Klass.getClassID(klass) == CID.LOCAL_ARRAY) {
            updateStackChunkOops(object, copiedObject);
        }

        /*
         * Run some assertions that will hopefully ensure isolation
         */
        if (Assert.ASSERTS_ENABLED && copyObjectGraphCB != null) {
            assertIsolation(object, klass);
        }


        if (VM.isVerbose() && Klass.getClassID(klass) == CID.BYTECODE_ARRAY) {
            int old = VM.setStream(VM.STREAM_SYMBOLS);
            if (copyObjectGraphCB == null) {
                VM.print("METHOD.");
                VM.printAddress(copiedObject);
                VM.print(".MOVED_FROM=");
                VM.printAddress(object);
                VM.println();
            } else {
                if (expectingCopyObjectGraphSecondPass) {
                    VM.print("SAVED_METHOD.");
                    VM.printOffset(copiedObject.diff(toSpaceStartPointer));
                    VM.print(".COPIED_FROM=");
                    VM.printAddress(object);
                    VM.println();
                }
            }
            VM.setStream(old);
        }

        /*
         * Return the new object pointer.
         */
        return copiedObject;
    }

    /**
     * Determines if a given object address does not break isolation. This is not
     * a comprehensive test but should catch most errors.
     *
     * @param object   the object address to test
     * @param klass    the class of the object
     */
    private void assertIsolation(Address object, Klass klass) {

        // Needs to be at outmost scope for at least one version of javac
        // not to multiplex it with the 'thread' variable further below
        Address global;

        // Ensure that the object does not coincides with the value of a global reference
        for (int i = 0 ; i < VM.getGlobalOopCount() ; i++) {
            global = Address.fromObject(VM.getGlobalOop(i));
            if (global.eq(object)) {
                VM.print("cannot copy value shared with global '");
                VM.printGlobalOopName(i);
                VM.println("' -- most likely caused by a local variable in a method in java.lang.Thread pointing to a global");
                Assert.shouldNotReachHere();
            }
        }

        Assert.that(klass != ObjectMemoryKlass, "cannot copy an ObjectMemory instance");
        Assert.that(object.toObject() != copyObjectGraphCB, "cannot copy the graph copying control block");

        if (theIsolate != null) {
            if (Klass.isSubtypeOf(klass, ThreadKlass)) {
                Assert.that(Unsafe.getObject(object, (int)FieldOffsets.java_lang_Thread$isolate) == theIsolate,
                            "cannot copy thread from another isolate");
            } else if (Klass.isSubtypeOf(klass, IsolateKlass)) {
                Assert.that(object.toObject() == theIsolate, "cannot copy another isolate");
            } else if (klass == Klass.LOCAL_ARRAY) {
                Object thread = Unsafe.getObject(object, SC.owner);
                Assert.that(thread == null || Unsafe.getObject(thread, (int)FieldOffsets.java_lang_Thread$isolate) == theIsolate,
                            "cannot copy stack chunk from another isolate");
            }
        }
    }

    /**
     * Update an object reference in an object copying the object referred to
     * from 'from' space to 'to' space if necessary.
     *
     * @param base the base address of the object holding the reference
     * @param offset the offset (in words) from 'base' of the reference
     */
    private Address updateReference(Address base, int offset) {
        Assert.that(base.roundUpToWord().eq(base), "unaligned base address");
        Address oldObject = Address.fromObject(Unsafe.getObject(base, offset));
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.print("CheneyCollector::updateReference - [");
            VM.printAddress(base);
            VM.print("%");
            VM.print(offset);
            VM.print("] = ");
            VM.printAddress(oldObject);
            VM.println();
        }
        Address newObject = copyObject(oldObject);
        if (newObject.ne(oldObject)) {
            Unsafe.setAddress(base, offset, newObject);
        }

        if (copyObjectGraphCB != null) {
            // Update the oop map
            Address pointerAddress = base.add(offset * HDR.BYTES_PER_WORD);
            recordPointer(pointerAddress);
        }

        return newObject;
    }

    /**
     * Update the frame pointers within a stack chunk.
     *
     * @param oldChunk the old stack chunk.
     * @param newChunk the new stack chunk.
     */
    private void updateStackChunkOops(Address oldChunk, Address newChunk) {
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("CheneyCollector::updateStackChunkOops");
        }

        /*
         * Update the frame pointers.
         */
        Offset offsetToPointer = Offset.fromPrimitive(SC.lastFP).wordsToBytes();
        Address oldFP = Address.fromObject(Unsafe.getObject(oldChunk.addOffset(offsetToPointer), 0));
        while (!oldFP.isZero()) {
            Address newPointerAddress = newChunk.addOffset(offsetToPointer);
            Offset delta = oldFP.diff(oldChunk);
            Address newFP = newChunk.addOffset(delta);
            Unsafe.setAddress(newPointerAddress, 0, newFP);
            if (copyObjectGraphCB != null) {
                recordPointer(newPointerAddress);
            }

            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.print("CheneyCollector::updateStackChunkOops offset = ");
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


    /**
     * Update an activation record.
     *
     * @param fp                     the frame pointer
     * @param isInnerMostActivation  specifies if this is the inner most activation frame on the chunk
     *                               in which case only the first local variable (i.e. the method pointer) is scanned
     */
    private void updateActivation(Address fp, boolean isInnerMostActivation) {
        Address mp  = Address.fromObject(Unsafe.getObject(fp, FP.method));
        Address previousFP = VM.getPreviousFP(fp);
        Address previousIP = VM.getPreviousIP(fp);

        /*
         * Trace.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.print("CheneyCollector::updateActivation fp = ");
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
                VM.println("CheneyCollector::updateActivation -- change previous MP");
            }

            /*
             * Adjust the MP
             */
            Assert.that(!previousFP.isZero(), "activation frame has null previousFP");
            Address oldPreviousMP = Address.fromObject(Unsafe.getObject(previousFP, FP.method));
            Assert.that(!isInToSpace(oldPreviousMP), "a method was copied before activation frame of the method");
            Address newPreviousMP = updateReference(previousFP, FP.method);

            /*
             * Adjust the IP
             */
            Offset delta = newPreviousMP.diff(oldPreviousMP);
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.println("CheneyCollector::updateActivation -- change previous IP");
                VM.print("        oldPreviousMP = ");
                VM.printAddress(oldPreviousMP);
                VM.print(" oldPreviousIP = ");
                VM.printAddress(previousIP);
                VM.print(" newPreviousMP = ");
                VM.printAddress(newPreviousMP);
                VM.print(" delta = ");
                VM.printOffset(delta);
                VM.print(" newPreviousIP = ");
                VM.printAddress(previousIP.addOffset(delta));
                VM.println("");
            }
            previousIP = previousIP.addOffset(delta);
            VM.setPreviousIP(fp, previousIP);

            /*
             * Record the pointer if graph copying.
             */
            if (copyObjectGraphCB != null) {
                Address pointerAddress = fp.add(FP.returnIP * HDR.BYTES_PER_WORD);
                recordPointer(pointerAddress);
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
                    VM.print("CheneyCollector::updateActivation -- update parm at offset ");
                    VM.print(varOffset);
                    VM.println();
                }
                updateReference(fp, varOffset);
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
                    VM.print("CheneyCollector::updateActivation -- update local at offset ");
                    VM.print(varOffset);
                    VM.println();
                }
                updateReference(fp, varOffset);
            }
            varOffset--; // Locals go downwards
        }
    }

    /**
     * Determines if the value of any Address-typed slots in an activation frame indicate
     * an address within the heap.
     *
     * @param fp                     the frame pointer
     * @param isInnerMostActivation  specifies if this is the inner most activation frame on the chunk
     *                               in which case no local variables are scanned
     */
    private void checkActivationForAddresses(Address fp, boolean isInnerMostActivation) {
        Address mp  = Address.fromObject(Unsafe.getObject(fp, FP.method));

        /*
         * Trace.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.print("CheneyCollector::checkActivationForAddresses fp = ");
            VM.printAddress(fp);
            VM.print(" mp = ");
            VM.printAddress(mp);
            VM.println();
        }

        /*
         * Get the method pointer and setup to go through the parameters and locals.
         */
        int localCount      = isInnerMostActivation ? 0 : MethodBody.decodeLocalCount(mp);
        int parameterCount  = MethodBody.decodeParameterCount(mp);
        int typeTableSize   = MethodBody.decodeTypeTableSize(mp);
        int typeTableOffset = MethodBody.decodeTypeTableOffset(mp);

        if (typeTableSize > 0) {
            decoder.reset(mp, typeTableOffset);
            int typeTableEndOffset = typeTableOffset + typeTableSize;
            while (decoder.getOffset() < typeTableEndOffset) {
                int cid  = decoder.readUnsignedInt();
                int slot = decoder.readUnsignedInt();
                if (cid == CID.ADDRESS) {
                    if (slot < parameterCount) {
                        int varOffset = FP.parm0 + slot; // parameters go upward
                        Address value = Address.fromObject(Unsafe.getObject(fp, varOffset));
                        if (value.hieq(fromSpaceStartPointer) && value.loeq(fromSpaceEndPointer)) {
                            VM.print("**WARNING**: parameter ");
                            VM.print(slot);
                            VM.print(" of type Address points into the heap: mp = ");
                            VM.printAddress(mp);
                            VM.println();
                        }
                    } else {
                        int local = slot - parameterCount;
                        if (local < localCount) {
                            int varOffset = FP.local0 - local; // locals go downward
                            Address value = Address.fromObject(Unsafe.getObject(fp, varOffset));
                            if (value.hieq(fromSpaceStartPointer) && value.loeq(fromSpaceEndPointer)) {
                                VM.print("**WARNING**: local ");
                                VM.print(local);
                                VM.print(" of type Address points into the heap: mp = ");
                                VM.printAddress(mp);
                                VM.println();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the references in a stack chunk.
     *
     * @param chunk the stack chunk.
     */
    private void updateStackChunk(Object chunk) {
        Address fp = Address.fromObject(Unsafe.getObject(chunk, SC.lastFP));

        /*
         * Trace.
         */
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println();
            VM.print("CheneyCollector::updateStackChunk chunk = ");
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
                updateReference(Address.fromObject(chunk), index);
            }
            index++;
            oopMap = oopMap >> 1;
        }

        /*
         * Update the pointers in each activation frame
         */
        boolean isInnerMostActivation = true;
        while (!fp.isZero()) {
            updateActivation(fp, isInnerMostActivation);
            if (Assert.ASSERTS_ENABLED) {
                checkActivationForAddresses(fp, isInnerMostActivation);
            }
            fp = VM.getPreviousFP(fp);
            isInnerMostActivation = false;
        }
    }

    /**
     * Update the object references in the root objects copying the objects referred to
     * from 'from' space to 'to' space as necessary.
     */
    private void copyRootObjects() {
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("CheneyCollector::copyRootObjects --------------- Start");
        }
        for (int i = 0 ; i < VM.getGlobalOopCount() ; i++) {
            Address oldObject = Address.fromObject(VM.getGlobalOop(i));
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.print("CheneyCollector::copyRootObjects index = ");
                VM.print(i);
                VM.print(" object = ");
                VM.printAddress(oldObject);
                VM.println();
            }
            Address newObject = copyObject(oldObject);
            if (newObject.ne(oldObject)) {
                VM.setGlobalOop(newObject, i);
            }
        }
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("CheneyCollector::copyRootObjects --------------- End");
        }
    }

    /**
     * On systems that do not have stack chunks in the heap this
     * routine will update the object references.
     */
    private void updateVirtualStackChunks() {
/*if[CHUNKY_STACKS]*/
/*else[CHUNKY_STACKS]*/
//      Object chunk = GC.getStackChunkList();
//      while (chunk != null) {
//          updateStackChunk(chunk);
//          chunk = Unsafe.getObject(chunk, SC.next);
//      }
/*end[CHUNKY_STACKS]*/
    }


    /**
     * Update the object references in an object copying the objects referred to
     * from 'from' space to 'to' space as necessary.
     *
     * @param object the object in 'to' space
     */
    private void updateOops(Address object) {
        Address associationOrKlass = updateReference(object, HDR.klass);
        Klass klass = VM.asKlass(updateReference(associationOrKlass, (int)FieldOffsets.java_lang_Klass$self));
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.print("CheneyCollector::updateOops object = ");
            VM.printAddress(object);
            VM.print(" klass = ");
            VM.println(Klass.getInternalName(klass));
        }
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
                    updateReference(object, HDR.methodDefiningClass);
                    break;
                }
                case CID.GLOBAL_ARRAY: {
                    Klass gaklass = VM.asKlass(Unsafe.getObject(object, CS.klass));
                    if (gaklass == null) { // This can occur if a GC occurs when a class state is being allocated
                        if (GC.TRACING_SUPPORTED && tracing()) {
                            VM.println("CheneyCollector::updateOops GLOBAL_ARRAY with null CS.klass not scanned");
                        }
                    } else {
                        if (GC.TRACING_SUPPORTED && tracing()) {
                            VM.print("CheneyCollector::updateOops globals for ");
                            VM.println(Klass.getInternalName(gaklass));
                        }
                        int end = CS.firstVariable + Klass.getRefStaticFieldsSize(gaklass);
                        for (int i = 0 ; i < end ; i++) {
                            Assert.that(i < GC.getArrayLength(object), "class state index out of bounds");
                            updateReference(object, i);
                        }
                    }
                    break;
                }
                case CID.LOCAL_ARRAY: {
                    updateStackChunk(object);
                    break;
                }
                default: { // Pointer array
                    int length = GC.getArrayLength(object);
                    for (int i = 0; i < length; i++) {
                        updateReference(object, i);
                    }
                    break;
                }
            }

        } else { // Instance

            /*
             * If the object is a hashtable and we are doing a graph copy for hibernation then
             * zero the first oop which is the transient field called entryTable.
             */
            if (copyObjectGraphCB != null && Klass.isSubtypeOf(klass, HashTableKlass)) {
                Unsafe.setAddress(object, (int)FieldOffsets.com_sun_squawk_util_Hashtable$entryTable, Address.zero());
            }

            /*
             * Update the oops
             */
            int nWords = Klass.getInstanceSize(klass);
            for (int i = 0 ; i < nWords ; i++) {
                if (Klass.isInstanceWordReference(klass, i)) {
                    updateReference(object, i);
                }
            }
        }
    }

    /**
     * Update the object references in all the objects currently in the 'to' space
     * copying the objects referred to from 'from' space to 'to' space as necessary.
     *
     * @param   toSpaceUpdatePointer  the address in 'to' space at which to start processing
     * @return  the address in 'to' space at which the next call to this method should start processing
     */
    private Address copyNonRootObjects(Address toSpaceUpdatePointer) {
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("CheneyCollector::copyNonRootObjects --------------- Start");
        }
        while (toSpaceUpdatePointer.lo(toSpaceAllocationPointer)) {
            Address object = GC.blockToOop(toSpaceUpdatePointer);
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.println();
                VM.print("CheneyCollector::copyNonRootObjects block = ");
                VM.printAddress(toSpaceUpdatePointer);
                VM.print(" object = ");
                VM.printAddress(object);
                VM.print(" klass = ");
                VM.println(Klass.getInternalName(getKlass(object)));
            }
            Klass klass = getKlass(object);
            Assert.that(GC.oopToBlock(klass, object).eq(toSpaceUpdatePointer), "bad size for copied object");
            updateOops(object);
            toSpaceUpdatePointer = object.add(GC.getBodySize(klass, object));
        }
        if (GC.TRACING_SUPPORTED && tracing()) {
            VM.println("CheneyCollector::copyNonRootObjects --------------- End");
        }
        return toSpaceUpdatePointer;
    }

    /**
     * Process the finalizer queue.
     */
    private void processFinalizerQueue(Address toSpaceUpdatePointer) {
        Finalizer entry = getFinalizers();
        while (entry != null) {
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.println("CheneyCollector::processFinalizerQueue -- processing finalizer ");
            }
            Assert.that(!isInToSpace(getPossiblyForwardedObject(Address.fromObject(entry))), "finalizer for object copied prematurely");
            Finalizer next = entry.getNext();
            Address object = Address.fromObject(entry.getObject());
            boolean referenced = isInToSpace(getPossiblyForwardedObject(object));
            entry = (Finalizer)copyObject(Address.fromObject(entry)).toObject();
            entry.setNext(null);
            toSpaceUpdatePointer = copyNonRootObjects(toSpaceUpdatePointer);
            if (referenced) {
                if (GC.TRACING_SUPPORTED && tracing()) {
                    VM.print("CheneyCollector::processFinalizerQueue -- requeue ");
                    VM.printAddress(entry);
                    VM.print(" klass ");
                    VM.print(getKlass(Address.fromObject(entry.getObject())).getInternalName());
                    VM.println();
                }
                addFinalizer(entry);        // Requeue the finalizer back on the collecor.
            } else {
                if (GC.TRACING_SUPPORTED && tracing()) {
                    VM.print("CheneyCollector::processFinalizerQueue -- release ");
                    VM.printAddress(entry);
                    VM.print(" klass ");
                    VM.print(getKlass(Address.fromObject(entry.getObject())).getInternalName());
                    VM.print(" queued to isolate for class ");
                    VM.print(entry.getIsolate().getMainClassName());
                    VM.println();
                }
                entry.queueToIsolate();     // Queue for execution when the isolate is next preempted.
            }
            entry = next;
        }
    }

    /**
     * Debuging aid. Fill from space with the 0xDEADBEEF pattern.
     */
    private void clearFromSpace() {
        VM.deadbeef(fromSpaceStartPointer, fromSpaceEndPointer);
    }

    /**
     * Garbage collection entry point. Called by 'VM.collectGarbage()'.
     * Executed on special garbage collection stack of fixed size.
     *
     * @param allocationPointer the current allocation pointer
     */
    boolean collectGarbage(Address allocationPointer) {

        Assert.that(!expectingCopyObjectGraphSecondPass);

        /*
         * Test and set the re-entry guard.
         */
        Assert.that(!collecting, "recursive call to collector");
        collecting = true;

        /*
         * Output HTML heap trace
         */
        if ((HEAP_TRACE || GC.TRACING_SUPPORTED) && GC.isTracing(GC.TRACE_HEAP)) {
            traceHeap("Before collection", allocationPointer);
        }

        /*
         * Switch semi-spaces
         */
        toggleSpaces();

        /*
         * Set the from space to be read-only
         */
        memoryProtect(fromSpaceStartPointer, fromSpaceEndPointer);

        /*
         * Copy all the reachable objects.
         */
        copyRootObjects();
        updateVirtualStackChunks();
        Address toSpaceUpdatePointer = copyNonRootObjects(toSpaceStartPointer);

        /*
         * Process the finalizer queue
         */
        processFinalizerQueue(toSpaceUpdatePointer);

        /*
         * Set the from space to be read-write
         */
        memoryProtect(Address.zero(), Address.zero());

        /*
         * Fill from space with the 0xDEADBEEF pattern.
         */
        clearFromSpace();

        /*
         * Set the main RAM allocator to the 'to' space.
         */
        GC.setRamAllocateParameters(toSpaceStartPointer, toSpaceAllocationPointer, toSpaceEndPointer);

        /*
         * Output HTML heap trace
         */
        if ((HEAP_TRACE || GC.TRACING_SUPPORTED) && GC.isTracing(GC.TRACE_HEAP)) {
            traceHeap("After collection", toSpaceAllocationPointer);
        }

        /*
         * Reset the re-entry guard.
         */
        collecting = false;

        /*
         * The Cheney collector always collects the full heap
         */
        return true;
    }

    /**
     * Copies an object graph. This method is called twice to copy a graph. The first call is used
     * by the caller to discover the size of the buffers required for the second call. On the first
     * call (i.e. when <code>firstPass</code> is true), the elements of <code>cb</code> have the following semantics:
     *
     *   cb.memory: the address of a buffer whose size must be no less 'objectCount * 2 * HDR.BYTES_PER_WORD'
     *              where 'objectCount' is the result of GC.countObjectsInRamAllocationSpace()
     *   cb.size:   the size (in bytes) of the buffer pointed to by cb.memory. Upon returning, this
     *              field will hold the minimum required size of cb.memory on the subsequent call
     *
     * On the second call (i.e. when <code>firstPass</code> is false), the semantics are:
     *
     *   cb.memory: the address of a buffer whose size must be as specified by the value of cb.size
     *              after the first call. Upon returning, this buffer holds the copied graph
     *   cb.size:   the size (in bytes) of the buffer pointed to by cb.memory. Upon returning, this
     *              field will hold the size of the graph that was copied into cb.memory
     *   cb.root:   Upon returning, this holds the offset (in bytes) from cb.memory to the
     *              root of the copied graph.
     *
     * @param  object  the root of the object graph to copy
     * @param  cb      the in and out parameters of this call (described above)
     * @param firstPass specifies if this is the first or second pass of object graph copying
     */
    void copyObjectGraph(Address object, ObjectMemorySerializer.ControlBlock cb, boolean firstPass) {

        if (expectingCopyObjectGraphSecondPass == firstPass) {
            Assert.shouldNotReachHere();
        }

        // Test and set the re-entry guard.
        Assert.that(!collecting, "recursive call to collector");
        collecting = true;
        copyObjectGraphCB = cb;

        if (GC.getKlass(object.toObject()) == IsolateKlass) {
            theIsolate = (Isolate)object.toObject();
        }

        int cbSize = cb.size;
        int forwardingRepairMapLength = cbSize / HDR.BYTES_PER_WORD;
        cb.size = forwardingRepairMapLength;

        // Switch semi-spaces
        toggleSpaces();

        // Set the from space to be read-only
        memoryProtect(fromSpaceStartPointer, fromSpaceEndPointer);

        // Copy all the reachable objects.
        object = copyObject(object);
        copyNonRootObjects(toSpaceStartPointer);

        // Set the from space to be read-write
        memoryProtect(Address.zero(), Address.zero());

        // Get the start and end of the serialized graph
        Address graph = toSpaceStartPointer;
        Address graphEnd = toSpaceAllocationPointer;

        // Toggle the spaces bacIk
        toggleSpaces();

        // Repair the class word of the forwarded objects
        int copiedObjectCount = repairForwardedObjects(Address.fromObject(cb.memory), cb.size, forwardingRepairMapLength);

        if (!firstPass) {
            // Relocate all the pointers in the copy of the graph.
            relocate(graph, graphEnd, copyObjectGraphCB.oopMap);

            // Copy the serialized graph into the provided buffer.
            int graphSize = graphEnd.diff(graph).toInt();
            Assert.always(graphSize > 0);
            Assert.that(cbSize >= graphSize, "buffer provided for copied graph is too small");
            VM.copyBytes(graph, 0, cb.memory, 0, graphSize, false);
/*if[TYPEMAP]*/
            if (VM.usingTypeMap()) {
                Unsafe.copyTypes(graph, Address.fromObject(cb.memory), graphSize);
            }
/*end[TYPEMAP]*/
            cb.root = object.diff(graph).toInt();
            cb.size = graphSize;

            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.println();
                VM.print("CheneyCollector::copyObjectGraph [second pass] - graph = ");
                VM.printAddress(graph);
                VM.print(" graphEnd = ");
                VM.printAddress(graphEnd);
                VM.print(" cb.size ");
                VM.print(cb.size);
                VM.println();
            }
            expectingCopyObjectGraphSecondPass = false;
        } else {
            int nextForwardingRepairMapSize = copiedObjectCount * HDR.BYTES_PER_WORD * 2;
            cb.size = graphEnd.diff(graph).toInt();
            if (cb.size < nextForwardingRepairMapSize) {
                cb.size = nextForwardingRepairMapSize;
            }

            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.println();
                VM.print("CheneyCollector::copyObjectGraph [first pass] - graph = ");
                VM.printAddress(graph);
                VM.print(" graphEnd = ");
                VM.printAddress(graphEnd);
                VM.print(" cb.size ");
                VM.print(cb.size);
                VM.println();
            }
            expectingCopyObjectGraphSecondPass = true;
        }

        // Reset the re-entry guard.
        collecting = false;
        copyObjectGraphCB = null;
        theIsolate = null;
    }

    /**
     *
     * @param map Address
     * @param start int
     * @param end int
     */
    private static int repairForwardedObjects(Address map, int start, int end) {
        for (int offset = start; offset != end; ) {
            UWord classWord = Unsafe.getUWord(map, offset++);
            Object forwardedObject = Unsafe.getObject(map, offset++);
            Unsafe.setAddress(forwardedObject, HDR.klass, Address.zero().or(classWord));
        }
        return (end - start) / 2;
    }

    /**
     * Relocate all the pointers in the copied graph to be relative to the canonical addresses
     * of the memory regions that are pointed to.
     *
     * @param graph     the start of the copied graph
     * @param graphEnd  the address one byte past the end of the copied graph
     * @param map       the oop map describing where all the pointers are in the graph
     */
    private void relocate(Address graph, Address graphEnd, BitSet map) {
        // If a Suite is being copied as opposed to an Isolate, then get
        // a handle to the current isolate
        Isolate isolate = theIsolate == null ? Thread.getOtherThread().getIsolate() : theIsolate;
        Suite openSuite = isolate.getOpenSuite();
        ObjectMemory parent;
        if (openSuite == null) {
            parent = GC.lookupObjectMemoryByRoot(isolate.getBootstrapSuite());
        } else {
            parent = GC.lookupObjectMemoryByRoot(openSuite.getParent());
        }
        Assert.that(parent != null, "null object memory parent for copied graph");

        Address graphCanonicalStart = parent.getCanonicalEnd();
        ObjectMemoryLoader.relocateParents("RAM", null, graph, map, parent, true);
        ObjectMemoryLoader.relocate("RAM", null, graph, map, graph, graphCanonicalStart, graphEnd.diff(graph).toInt(), "RAM", true);
    }

    /**
     * Records the address of a pointer in the graph being copied.
     *
     * @param pointerAddress the address of a pointer
     */
    private void recordPointer(Address pointerAddress) {
        if (copyObjectGraphCB.oopMap == null) {
            // This is the call to copyObjectGraph that is calculating the size of the graph
            return;
        }
        // Update the oop map
        if (isInToSpace(pointerAddress)) {
            int word = (pointerAddress.diff(toSpaceStartPointer).toInt() / HDR.BYTES_PER_WORD);
            memoryProtect(Address.zero(), Address.zero());
            try {
                copyObjectGraphCB.oopMap.set(word);
            } catch (IndexOutOfBoundsException e) {
                Assert.shouldNotReachHere();
            }
            memoryProtect(fromSpaceStartPointer, fromSpaceEndPointer);
            if (GC.TRACING_SUPPORTED && tracing()) {
                VM.print("CheneyCollector::recordPointer - set bit in oop map for canonical pointer at ");
                VM.printAddress(pointerAddress);
                VM.println();
            }
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
                return GC.isTracing(GC.TRACE_COLLECTION);
            } else {
                return GC.isTracing(GC.TRACE_OBJECT_GRAPH_COPYING);
            }
        }
        return false;
    }

    /**
     * Trace the collector variables.
     */
    private void traceVariables() {
        /*
         * Output trace information.
         */
        VM.println("CheneyCollector variables");
        traceVariable("fromSpaceStartPointer", fromSpaceStartPointer);
        traceVariable("fromSpaceEndPointer", fromSpaceEndPointer);
        traceVariable("fromSpaceSize", fromSpaceEndPointer.diff(fromSpaceStartPointer).toInt());
        traceVariable("toSpaceStartPointer", toSpaceStartPointer);
        traceVariable("toSpaceEndPointer", toSpaceEndPointer);
        traceVariable("toSpaceSize", toSpaceEndPointer.diff(toSpaceStartPointer).toInt());
        traceVariable("toSpaceAllocationPointer", toSpaceAllocationPointer);
    }

    /**
     * {@inheritDoc}
     */
    void traceHeap(String description, Address allocationPointer) {
        traceHeapStart(description, freeMemory(allocationPointer) );
        traceHeapSegment("fromSpace", fromSpaceStartPointer, fromSpaceEndPointer);
        traceHeapSegment("toSpace{used}", toSpaceStartPointer, allocationPointer);
        traceHeapSegment("toSpace{free}", allocationPointer, toSpaceEndPointer);

        for (Address block = toSpaceStartPointer ; block.lo(allocationPointer); ) {
            Address object = GC.blockToOop(block);
            Klass klass = GC.getKlass(object);
            int size = GC.getBodySize(klass, object);
            traceHeapObject(block, object, klass, size / HDR.BYTES_PER_WORD);
            block = object.add(size);
        }
        traceHeapEnd();
    }

}
