package com.sun.squawk.util;

/**
 * This class provides mechanisms for manipulating a bit set.
 *
 * @author  Doug Simon
 */

public class BitSet {

    /**
     * The byte array encoding the bit set.
     */
    private byte[] bits;

    /**
     * The number of bytes in the logical size of this BitSet.
     */
    private int bytesInUse;

    /**
     * Specifies if the underlying bit array is owened by the client and therefore cannot be
     * extended.
     */
    private final boolean bitsAreExternal;

    /**
     * Creates new BitSet instance whose underlying byte array is controlled by the instance.
     * Only this type of BitSet will grow as necessary and will never throw an IndexOutOfBoundsException
     * if a given bitIndex is greater than the current physical size of the underlying byte array.
     *
     */
    public BitSet() {
        this.bits = new byte[10];
        this.bytesInUse = 0;
        this.bitsAreExternal = false;
    }

    /**
     * Creates new BitSet instance whose underlying byte array is controlled by the client of the instance.
     * This type of BitSet will throw an IndexOutOfBoundsException if a given bitIndex
     * is greater than the highest bit that can be expressed in the underlying byte array
     */
    public BitSet(byte[] bits) {
        this.bits = bits;
        this.bitsAreExternal = true;

        // calculate bytesInUse
        for (int i = bits.length - 1; i >= 0; --i) {
            if (bits[i] != 0) {
                bytesInUse = i + 1;
                break;
            }
        }

    }

    public boolean areBitsExternal() {
        return bitsAreExternal;
    }

    /**
     * Determines if a given bit index is valid.
     *
     * @param bitIndex the bit index to test
     * @throws IndexOutOfBoundsException is the given index is negative
     */
    protected void validateIndex(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
    }

    /**
     * Copies the bit set representation into a provided byte array. The number of bytes
     * copied is equal to the minimum of length of the provided byte array and the internal
     * byte array.
     *
     * @param bits  the byte array to copy into
     * @throws NullPointerException if the given buffer is null.
     */
    public void copyInto(byte[] bits) {
        int length = bits.length;
        if (length > this.bits.length) {
            length = this.bits.length;
        }
        System.arraycopy(this.bits, 0, bits, 0, length);
    }

    /**
     * Returns the "logical size" of this <code>BitSet</code>: the index of
     * the highest set bit in the <code>BitSet</code> plus one.
     *
     * @return  the logical size of this <code>BitSet</code>.
     */
    public int length() {
        if (bytesInUse == 0) {
            return 0;
        }

        int highestUnit = bits[bytesInUse - 1] & 0xFF;
        int hiBit = 0;
        while (highestUnit != 0) {
            highestUnit >>= 1;
            ++hiBit;
        }
        return 8 * (bytesInUse - 1) + hiBit;
    }


    /**
     * Returns the number of bits of space actually in use by this
     * <code>BitSet</code> to represent bit values.
     *
     * @return  the number of bits currently in this bit set.
     */
    public int size() {
        return bits.length * 8;
    }

    /**
     * Sets the bit at a given index.
     *
     * @param bitIndex  the index of the bit to set
     * @throws IndexOutOfBoundsException if the given bit index is negative or if this is
     *        an {@link #areBitsExternal() external} BitSet instance and <code>bitIndex >= this.size()</code>
     */
    public void set(int bitIndex) {
        validateIndex(bitIndex);

        // calculate the index of the relevant byte in the map
        int byteIndex = bitIndex / 8;

        // extend the byte array if necessary
        ensureCapacity(bitIndex);

        if (byteIndex >= bytesInUse) {
            bytesInUse = byteIndex + 1;
        }

        // set the relevant bit
        byte bit = (byte)(1 << (bitIndex % 8));
        bits[byteIndex] |= bit;
    }

    /**
     * Ensures that the capacity of this bit set is equal to a given minimum.
     *
     * @param bitIndex       the index of the bit being accessed in the caller
     * @throws IndexOutOfBoundsException if this is an {@link #areBitsExternal() external}
     *              BitSet instance and <code>bitIndex >= this.size()</code>
     */
    private void ensureCapacity(int bitIndex) throws IndexOutOfBoundsException {
        int bytesRequired = (bitIndex / 8) + 1;
        if (bits.length < bytesRequired) {
            // Cannot grow a bit set whose bits are external
            if (bitsAreExternal) {
                throw new IndexOutOfBoundsException("bitIndex >= this.size(): " + bitIndex);
            }

            // Allocate larger of doubled size or required size
            int request = Math.max(2 * bits.length, bytesRequired);
            byte newBits[] = new byte[request];
            System.arraycopy(bits, 0, newBits, 0, bytesInUse);
            bits = newBits;
        }
    }

    /**
     * Clears the bit at a given index.
     *
     * @param  bitIndex  the index of the bit to clear
     * @throws IndexOutOfBoundsException if the given bit index is negative
     */
    public void clear(int bitIndex) {
        validateIndex(bitIndex);

        // calculate the index of the relevant byte in the map
        int byteIndex = bitIndex / 8;
        if (byteIndex < bytesInUse) {
            // clear the relevant bit
            byte bit = (byte)(1 << (bitIndex % 8));
            bits[byteIndex] &= ~bit;
        }
    }

    /**
     * Clears all of the bits in this BitSet.
     */
    public void clear() {
        while (bytesInUse > 0) {
            bits[--bytesInUse] = 0;
        }
    }

    /**
     * Returns the value of the bit with the specified index. The value
     * is <code>true</code> if the bit with the index <code>bitIndex</code>
     * is currently set in this <code>BitSet</code>; otherwise, the result
     * is <code>false</code>.
     *
     * @param     bitIndex   the bit index.
     * @return    the value of the bit with the specified index.
     * @throws IndexOutOfBoundsException if the given bit index is negative
     */
    public boolean get(int bitIndex) {
        validateIndex(bitIndex);

        boolean result = false;
        int byteIndex = bitIndex / 8;
        if (byteIndex < bytesInUse) {
            // clear the relevant bit
            byte bit = (byte)(1 << (bitIndex % 8));
            result = (bits[byteIndex] & bit) != 0;
        }
        return result;
    }

    /**
     * Returns the number of bits set to 1 in this <code>BitSet</code>.
     *
     * @return  the number of bits set to 1 in this <code>BitSet</code>.
     */
    public int cardinality() {
        int sum = 0;
        for (int i = 0; i < bytesInUse; i++)
            sum += BitSetTable.BIT_COUNT[((int)bits[i]) & 0xFF];
        return sum;
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit at index
     * <i>n</i> in this set is 1 if and only if it was already set
     * or the bit at index <i>n</i> in <code>other</code>
     * was set. The semantics of this operation can be expressed as:
     * <p><hr><blockquote><pre>
     *      this = this | other;
     * </pre></blockquote><hr>
     *
     * @param   other    a bit set
     */
    public void or(BitSet other) {
        or(other, 0);
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit at index
     * <i>n</i> in this set is 1 if and only if it was already set
     * or the bit at index <i>n</i><code> + offset</code> in <code>other</code>
     * was set. The semantics of this operation can be expressed as:
     * <p><hr><blockquote><pre>
     *      this = this | (other << offset);
     * </pre></blockquote><hr>
     *
     * @param   other    a bit set
     * @param   offset   the offset to be applied to the index of a bit
     *                   in <code>other</code> before it is <b>OR</b>ed
     *                   with this set
     */
    public void or(BitSet other, int offset) {
        if (this == other) {
            return;
        }

        ensureCapacity(other.bytesInUse + ((offset + 7) / 8));

        for (int bitIndex = other.nextSetBit(0); bitIndex != -1; bitIndex = other.nextSetBit(bitIndex + 1)) {
            set(bitIndex + offset);
        }
    }

    /**
     * Compares this object against the specified object.
     * The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and is a <code>Bitset</code> object that has
     * exactly the same set of bits set to <code>true</code> as this bit
     * set. That is, for every nonnegative <code>int</code> index <code>k</code>,
     * <pre>((BitSet)obj).get(k) == this.get(k)</pre>
     * must be true. The current sizes of the two bit sets are not compared.
     * <p>Overrides the <code>equals</code> method of <code>Object</code>.
     *
     * @param   obj   the object to compare with.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     * @see     java.util.BitSet#size()
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof BitSet)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        BitSet set = (BitSet)obj;
        int minBytesInUse = Math.min(bytesInUse, set.bytesInUse);

        // Check bytes in use by both BitSets
        for (int i = 0; i < minBytesInUse; i++) {
            if (bits[i] != set.bits[i]) {
                return false;
            }
        }

        // Check any bytes in use by only one BitSet (must be 0 in other)
        if (bytesInUse > minBytesInUse) {
            for (int i = minBytesInUse; i < bytesInUse; i++) {
                if (bits[i] != 0) {
                    return false;
                }
            }
        } else {
            for (int i = minBytesInUse; i < set.bytesInUse; i++) {
                if (set.bits[i] != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns a hash code value for this bit set.
     *
     * @return  a hash code value for this bit set.
     */
    public int hashCode() {
        int h = 1234;
        for (int i = bits.length; --i >= 0;) {
            h ^= bits[i] * (i + 1);
        }
        return h;
    }

    /**
     * Returns the index of the first bit that is set to <code>true</code>
     * that occurs on or after the specified starting index. If no such
     * bit exists then -1 is returned.
     *
     * To iterate over the <code>true</code> bits use the following loop:
     *
     * <p><blockquote><pre>
     * for (int i = oopMap.nextSetBit(0); i >= 0; i = oopMap.nextSetBit(i + 1)) {
     *     // operate on index i here
     * }
     * </pre></blockquote>
     *
     * @param   fromIndex the index to start checking from (inclusive). This must be positive
     * @return  the index of the next set bit.
     * @throws  IndexOutOfBoundsException if the specified index is negative.
     */
    public int nextSetBit(int fromIndex) {
        validateIndex(fromIndex);

        int byteIndex = fromIndex / 8;
        if (byteIndex >= bytesInUse) {
            return -1;
        }

        int bitIndex = fromIndex % 8;
        while (byteIndex != bytesInUse) {
            byte bitSetUnit = bits[byteIndex];
            if (BitSetTable.BIT_COUNT[((int)bitSetUnit) & 0xFF] != 0) {
                while (bitIndex != 8) {
                    if ((bitSetUnit & (1 << bitIndex)) != 0) {
                        return (byteIndex * 8) + bitIndex;
                    }
                    ++bitIndex;
                }
            }
            ++byteIndex;
            bitIndex = 0;
        }

        return -1;
    }
    /**
     * Returns a string representation of this bit set. For every index
     * for which this <code>BitSet</code> contains a bit in the set
     * state, the decimal representation of that index is included in
     * the result. Such indices are listed in order from lowest to
     * highest, separated by ",&nbsp;" (a comma and a space) and
     * surrounded by braces, resulting in the usual mathematical
     * notation for a set of integers.<p>
     * Overrides the <code>toString</code> method of <code>Object</code>.
     * <p>Example:
     * <pre>
     * BitSet drPepper = new BitSet();</pre>
     * Now <code>drPepper.toString()</code> returns "<code>{}</code>".<p>
     * <pre>
     * drPepper.set(2);</pre>
     * Now <code>drPepper.toString()</code> returns "<code>{2}</code>".<p>
     * <pre>
     * drPepper.set(4);
     * drPepper.set(10);</pre>
     * Now <code>drPepper.toString()</code> returns "<code>{2, 4, 10}</code>".
     *
     * @return  a string representation of this bit set.
     */
    public String toString() {
        int numBits = bytesInUse * 8;
        StringBuffer buffer = new StringBuffer(8 * numBits + 2);
        String separator = "";
        buffer.append('{');

        for (int i = 0; i < numBits; i++) {
            if (get(i)) {
                buffer.append(separator);
                separator = ", ";
                buffer.append(i);
            }
        }

        buffer.append('}');
        return buffer.toString();
    }

/*if[J2ME.DEBUG]*/
    public static void main(String[] args) {
        BitSet bs = new BitSet();
        for (int i = 0; i != 256; ++i) {
            Assert.that(!bs.get(i));
            Assert.that(bs.cardinality() == i);
            bs.set(i);
            Assert.that(bs.length() == i + 1);
            Assert.that(bs.get(i));
            Assert.that(bs.cardinality() == i + 1);
        }

        bs = new BitSet(new byte[] { -1 });
        try {
            bs.set(8);
            Assert.shouldNotReachHere();
        } catch (IndexOutOfBoundsException e) {
        }

        bs = new BitSet(); /* 00001111 */
        bs.set(0);
        bs.set(1);
        bs.set(2);
        bs.set(3);

        BitSet other = new BitSet(new byte[] { 7 /* 00000111 */} );

        bs.or(other, 5);
        BitSet expected = new BitSet(new byte[] { (byte)239 /* 11101111 */} );
        Assert.that(bs.equals(expected), "bs = " + bs + ", expected = " + expected);
    }
/*end[J2ME.DEBUG]*/
}

/**
 * This class exists so that BitSet does not have a static initializer (which would cause
 * problems for the Squawk start up sequence).
 *
 * @author  Doug Simon
 */
class BitSetTable {
    /**
     * A table to enable fast counting of the bits set in a byte value. This table was
     * generated by capturing the output of the following code:
     *
     * <p><hr><blockquote><pre>
     *   System.out.print("    static private final byte[] BIT_COUNT = {");
     *   for (int i = 0; i != 256; ++i) {
     *       int unit = i;
     *       int count = 0;
     *       while (unit != 0) {
     *           if ((unit & 1) != 0) {
     *               ++count;
     *           }
     *           unit = unit >> 1;
     *       }
     *       if ((i % 8) == 0) {
     *           System.out.println();
     *           System.out.print("        ");
     *       }
     *       System.out.print(count);
     *       if (i != 255) {
     *           System.out.print(", ");
     *       }
     *   }
     *   System.out.println();
     *   System.out.println("    };");
     * </pre></blockquote><hr>
     */
    static final int[] BIT_COUNT = {
        0, 1, 1, 2, 1, 2, 2, 3,
        1, 2, 2, 3, 2, 3, 3, 4,
        1, 2, 2, 3, 2, 3, 3, 4,
        2, 3, 3, 4, 3, 4, 4, 5,
        1, 2, 2, 3, 2, 3, 3, 4,
        2, 3, 3, 4, 3, 4, 4, 5,
        2, 3, 3, 4, 3, 4, 4, 5,
        3, 4, 4, 5, 4, 5, 5, 6,
        1, 2, 2, 3, 2, 3, 3, 4,
        2, 3, 3, 4, 3, 4, 4, 5,
        2, 3, 3, 4, 3, 4, 4, 5,
        3, 4, 4, 5, 4, 5, 5, 6,
        2, 3, 3, 4, 3, 4, 4, 5,
        3, 4, 4, 5, 4, 5, 5, 6,
        3, 4, 4, 5, 4, 5, 5, 6,
        4, 5, 5, 6, 5, 6, 6, 7,
        1, 2, 2, 3, 2, 3, 3, 4,
        2, 3, 3, 4, 3, 4, 4, 5,
        2, 3, 3, 4, 3, 4, 4, 5,
        3, 4, 4, 5, 4, 5, 5, 6,
        2, 3, 3, 4, 3, 4, 4, 5,
        3, 4, 4, 5, 4, 5, 5, 6,
        3, 4, 4, 5, 4, 5, 5, 6,
        4, 5, 5, 6, 5, 6, 6, 7,
        2, 3, 3, 4, 3, 4, 4, 5,
        3, 4, 4, 5, 4, 5, 5, 6,
        3, 4, 4, 5, 4, 5, 5, 6,
        4, 5, 5, 6, 5, 6, 6, 7,
        3, 4, 4, 5, 4, 5, 5, 6,
        4, 5, 5, 6, 5, 6, 6, 7,
        4, 5, 5, 6, 5, 6, 6, 7,
        5, 6, 6, 7, 6, 7, 7, 8
    };

}
