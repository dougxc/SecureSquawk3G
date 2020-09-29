package java.lang;

import com.sun.squawk.util.Assert;
import com.sun.squawk.vm.HDR;
import com.sun.squawk.vm.AddressType;

/**
 * This class provides the interface to the bitmap created and used by the {@link Lisp2Collector}
 * as a write barrier and as mark bits for the young generation.
 *
 * @author  Doug Simon
 */
final class Lisp2Bitmap {

    /**
     * The address at which the bitmap starts. This bitmap is used as a
     * write barrier for the old generation as well as a marking bitvector
     * for the region being collected during a collection. As such, it has
     * a bit for every word in the heap.
     */
    private static Address start;

    /**
     * The logical starting address of the bitmap. This is the address at which
     * the bitmap would start if it had a bits for addresses starting from 0. The mutator
     * uses this base when updating the write barrier which removes the need for it
     * to convert the effective address of a pointer to be relative to
     * the start of the heap.
     */
    private static Address base;

    /**
     * This is the size (in bytes) of the bitmap.
     */
    private static int size;

    /**
     * Gets the real start of the bitmap. That is, the part of the bitmap
     * for which real memory has been allocated and whose bits correspond to addresses in the heap.
     *
     * @return the start address of the valid part of the bitmap
     */
    static Address getStart() {
        return start;
    }

    /**
     * Gets the logical start of the bitmap. That is, the address at which the bit for address 0 would be located.
     *
     * @return the logical start of the bitmap
     */
    static Address getBase() {
        return base;
    }

    /**
     * Gets the address of the word one past the end of the bitmap.
     *
     * @return  the address of the word one past the end of the bitmap
     */
    static Address getEnd() {
        return start.add(size);
    }

    /**
     * Gets the size (in bytes) of the bitmap.
     *
     * @return the size (in bytes) of the bitmap
     */
    static int getSize() {
        return size;
    }

    /**
     * Initializes or re-initializes the bitmap.
     *
     * @param start  see {@link #getStart}
     * @param base   see {@link #getBase}
     * @param size   the size (in bytes) of the bitmap
     */
    static void initialize(Address start, Address base, int size) {
        Lisp2Bitmap.start = start;
        Lisp2Bitmap.base = base;
        Lisp2Bitmap.size = size;
/*if[TYPEMAP]*/
        Address p = start;
        size = size / HDR.BYTES_PER_WORD;
        while (size != 0) {
            Unsafe.setType(p, AddressType.UWORD, HDR.BYTES_PER_WORD);
            p = p.add(HDR.BYTES_PER_WORD);
            --size;
        }
/*end[TYPEMAP]*/
    }

    /*---------------------------------------------------------------------------*\
     *                   Address based bitmap methods                            *
    \*---------------------------------------------------------------------------*/

    /**
     * Clears the bits in the bitmap corresponding to the range of memory <code>[start .. end)</code>
     *
     * @param start   the start of the memory range for which the bits are to be cleared
     * @param end     the end of the memory range for which the bits are to be cleared
     */
    static native void clearBitsFor(Address start, Address end);

    /**
     * Gets the address of the word in the bitmap that contains the bit for a given address.
     *
     * @param ea   the address for which the corresponding bitmap word is required
     * @return     the address of the bitmap word that contains the bit for <code>ea</code>
     */
    static native Address getAddressOfBitmapWordFor(Address ea);

    /**
     * Gets the address that corresponds to the first bit in the bitmap word at a given address.
     *
     * @param bitmapWordAddress  the address of a word in the bitmap
     * @return the address corresponding to the first bit in the word at <code>bitmapWordAddress</code>
     */
    static native Address getAddressForBitmapWord(Address bitmapWordAddress);

    /**
     * Sets the appropriate bit in the bitmap for a given address.
     *
     * @param ea      the effective address for which the corresponding bit is to be set
     */
    static native void setBitFor(Address ea);

    /**
     * Clears the appropriate bit in the bitmap for a given address.
     *
     * @param ea      the effective address for which the corresponding bit is to be set
     */
    static native void clearBitFor(Address ea);

    /**
     * Determines if the bit in the bitmap for a given address is set.
     *
     * @param ea      the effective address for which the corresponding bit to be tested
     * @return true if the bit for <code>ea</code> is set
     */
    static native boolean testBitFor(Address ea);

    /**
     * Determines if the bit in the bitmap for a given address is set and sets it if it isn't.
     *
     * @param ea      the effective address for which the corresponding bit to be tested
     * @return true if the bit for <code>ea</code> was set before this call
     */
    static native boolean testAndSetBitFor(Address ea);

    /*---------------------------------------------------------------------------*\
     *                                Iterators                                  *
    \*---------------------------------------------------------------------------*/

    /**
     * Guards use of the iterator to be serialized.
     */
    static private boolean Iterator_inUse;

    /**
     * The limit of the current iteration.
     */
    static private Address Iterator_end;

    /**
     * The iterator.
     */
    static private Address Iterator_next;

    /**
     * Operations for iterating over words in an address range whose corresponding bits in the bitmap are set.
     */
    static class Iterator {

        /**
         * Starts an iteration over all the words in the address range <code>[start .. end)</code>
         * whose corresponding bits in the bitmap are set. The iteration is complete when
         * {@link #getNext()} returns {@link Address#zero() null}. A subsequent iteration cannot be performed until
         * the current iteration is finished.
         *
         * @param start   the address at which to start iterating
         * @param end     the address one past the word at which to stop iterating
         */
        static void start(Address start, Address end) {
            Assert.always(!Iterator_inUse);
            Iterator_next = start;
            Iterator_end = end;
        }

        /**
         * Gets the address of the next word whose bit in the bitmap is set.
         *
         * @return  the address of the next word or {@link Address#zero() null} if the iteration is complete
         */
        static Address getNext() {
            Assert.that(Iterator_inUse);
            return iterate();
        }

    }

    /**
     * Gets the next value in the iteration. This operation will update the values of {@link #Iterator_next},
     * {@link #Iterator_end} and {@link #Iterator_inUse} so that the iteration progresses or completes.
     *
     * @return   the next value in the iteration or {@link Address#zero()} if the iteration is complete
     */
    static native Address iterate();

}


// Java based implementation of the bitmap
//
//
//    /*---------------------------------------------------------------------------*\
//     *                   Bit index based bitmap methods                          *
//    \*---------------------------------------------------------------------------*/
//
//    /**
//     * Determines if a bit index corresponds with word in the memory area reserved for the bitmap.
//     *
//     * @param n  the bit index to test
//     * @return true if bit <code>n</code> corresponds with word in the memory area reserved for the bitmap
//     */
//    private boolean isNthBitInRange(Offset n) {
//        Offset offset = Offset.fromPrimitive((n.toPrimitive() >> HDR.LOG2_BITS_PER_WORD) * HDR.BYTES_PER_WORD);
//        return bitmapBase.addOffset(offset).hieq(bitmap) &&
//               bitmapBase.addOffset(offset).lo(bitmap.add(bitmapSize));
//    }
//
//    /**
//     * Gets the address of the word in the bitmap that contains a given bit
//     *
//     * @param n   the bit index for which the corresponding bitmap word is required
//     * @return    the address of the bitmap word that contains the <code>n</code>th bit
//     */
//    private Address getAddressOfBitmapWordForNthBit(Offset n) {
//        Offset offset = Offset.fromPrimitive((n.toPrimitive() >> HDR.LOG2_BITS_PER_WORD) << HDR.LOG2_BYTES_PER_WORD);
//        return bitmapBase.addOffset(offset);
//    }
//
//    /**
//     * Sets a specified bit in the bitmap.
//     *
//     * @param n       the index of the bit to be set
//     */
//    private void setNthBit(Offset n) {
//        Assert.that(isNthBitInRange(n), "bit index out of range");
//        int offset = Offset.fromPrimitive(n.toPrimitive() >> HDR.LOG2_BITS_PER_WORD).toInt();
//        int bit = (int)(n.toPrimitive() & ~(HDR.BITS_PER_WORD - 1));
//        UWord mask = UWord.fromPrimitive(1 << bit);
//        Unsafe.setUWord(bitmapBase, offset, Unsafe.getUWord(bitmapBase, offset).or(mask));
//    }
//
//    /**
//     * Clears a specified bit in the bitmap.
//     *
//     * @param n       the index of the bit to be set
//     */
//    private void clearNthBit(Offset n) {
//        Assert.that(isNthBitInRange(n), "bit index out of range");
//        int offset = Offset.fromPrimitive(n.toPrimitive() >> HDR.LOG2_BITS_PER_WORD).toInt();
//        int bit = (int)(n.toPrimitive() & ~(HDR.BITS_PER_WORD - 1));
//        UWord mask = UWord.fromPrimitive(~(1 << bit));
//        Unsafe.setUWord(bitmapBase, offset, Unsafe.getUWord(bitmapBase, offset).and(mask));
//    }
//
//
//    /**
//     * Determines if a specified bit in the bitmap is set.
//     *
//     * @param n       the index of the bit to be tested
//     * @return true if bit <code>n</code> is set
//     */
//    private boolean testNthBit(Offset n) {
//        Assert.that(isNthBitInRange(n), "bit index out of range");
//        int offset = Offset.fromPrimitive(n.toPrimitive() >> HDR.LOG2_BITS_PER_WORD).toInt();
//        int bit = (int)(n.toPrimitive() & ~(HDR.BITS_PER_WORD - 1));
//        UWord mask = UWord.fromPrimitive(1 << bit);
//        return Unsafe.getUWord(bitmapBase, offset).and(mask).ne(UWord.zero());
//    }
//
//    /**
//     * Determines if a specified bit in the bitmap is set and sets it if it isn't.
//     *
//     * @param n       the index of the bit to be tested
//     * @return true if bit <code>n</code> was set before this call
//     */
//    private boolean testAndSetNthBit(Offset n) {
//        Assert.that(isNthBitInRange(n), "bit index out of range");
//        int offset = Offset.fromPrimitive(n.toPrimitive() >> HDR.LOG2_BITS_PER_WORD).toInt();
//        int bit = (int)(n.toPrimitive() & ~(HDR.BITS_PER_WORD - 1));
//        UWord mask = UWord.fromPrimitive(1 << bit);
//        boolean result = (Unsafe.getUWord(bitmapBase, offset).and(mask)).ne(UWord.zero());
//        Unsafe.setUWord(bitmapBase, offset, Unsafe.getUWord(bitmapBase, offset).or(mask));
//        return result;
//    }
//
//    /*---------------------------------------------------------------------------*\
//     *                   Address based bitmap methods                            *
//    \*---------------------------------------------------------------------------*/
//
//    /**
//     * Clears the bits in the bitmap corresponding to the range of memory <code>[start .. end)</code>
//     *
//     * @param start   the start of the memory range for which the bits are to be cleared
//     * @param end     the end of the memory range for which the bits are to be cleared
//     */
//    private void clearBitsFor(Address start, Address end) {
//        final int alignment = HDR.BITS_PER_WORD * HDR.BYTES_PER_WORD;
//        Address  alignedStart = start.roundUp(alignment);
//
//        while (start.lo(alignedStart)) {
//            clearBitFor(start);
//            start = start.add(HDR.BYTES_PER_WORD);
//        }
//
//        // It is always safe to clear past end, so align up
//        Address alignedEnd = end.roundUp(alignment);
//
//        Address s = getAddressOfBitmapWordFor(alignedStart);
//        Address e = getAddressOfBitmapWordFor(alignedEnd);
//        VM.zeroWords(s, e); // was -> VM.zeroWords(s, e.diff(s)).toInt();
//    }
//
//    /**
//     * Gets the address of the word in the bitmap that contains the bit for a given address.
//     *
//     * @param ea   the address for which the corresponding bitmap word is required
//     * @return     the address of the bitmap word that contains the bit for <code>ea</code>
//     */
//    private Address getAddressOfBitmapWordFor(Address ea) {
//        Offset offset = Offset.fromPrimitive(ea.diff(Address.zero()).toPrimitive() >> HDR.LOG2_BYTES_PER_WORD);
//        return getAddressOfBitmapWordForNthBit(offset);
//    }
//
//    /**
//     * Gets the address that corresponds to the first bit in the bitmap word at a given address.
//     *
//     * @param bitmapWordAddress  the address of a word in the bitmap
//     * @return the address corresponding to the first bit in the word at <code>bitmapWordAddress</code>
//     */
//    private Address getAddressForBitmapWordAddress(Address bitmapWordAddress) {
//        return bitmapWordAddress.diff(bitmapBase).toPrimitive() << HDR.LOG2_BITS_PER_BYTE;
//    }
//
//    /**
//     * Sets the appropriate bit in the bitmap for a given address.
//     *
//     * @param ea      the effective address for which the corresponding bit is to be set
//     */
//    private void setBitFor(Address ea) {
//        Offset offset = Offset.fromPrimitive(ea.diff(Address.zero()).toPrimitive() >> HDR.LOG2_BYTES_PER_WORD);
//        setNthBit(offset);
//    }
//
//    /**
//     * Clears the appropriate bit in the bitmap for a given address.
//     *
//     * @param ea      the effective address for which the corresponding bit is to be set
//     */
//    private void clearBitFor(Address ea) {
//        Offset offset = Offset.fromPrimitive(ea.diff(Address.zero()).toPrimitive() >> HDR.LOG2_BYTES_PER_WORD);
//        clearNthBit(offset);
//    }
//
//    /**
//     * Determines if the bit in the bitmap for a given address is set.
//     *
//     * @param ea      the effective address for which the corresponding bit to be tested
//     * @return true if the bit for <code>ea</code> is set
//     */
//    private boolean testBitFor(Address ea) {
//        Offset offset = Offset.fromPrimitive(ea.diff(Address.zero()).toPrimitive() >> HDR.LOG2_BYTES_PER_WORD);
//        return testNthBit(offset);
//    }
//
//    /**
//     * Determines if the bit in the bitmap for a given address is set and sets it if it isn't.
//     *
//     * @param ea      the effective address for which the corresponding bit to be tested
//     * @return true if the bit for <code>ea</code> was set before this call
//     */
//    private boolean testAndSetBitFor(Address ea) {
//        Offset offset = Offset.fromPrimitive(ea.diff(Address.zero()).toPrimitive() >> HDR.LOG2_BYTES_PER_WORD);
//        return testAndSetNthBit(offset);
//    }
//
