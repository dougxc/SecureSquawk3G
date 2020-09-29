package java.lang;

import com.sun.squawk.vm.HDR;
import com.sun.squawk.vm.AddressType;
import com.sun.squawk.util.*;
import java.util.Enumeration;

/**
 * A collection of methods for performing peek and poke operations on
 * memory addresses.
 * <p>
 * Only the public methods of this class which do not override any of the
 * methods in java.lang.Object will be available in a {@link VM#isHosted() non-hosted}
 * environment. The translator replaces any calls to these methods to native
 * method calls.
 *
 * @author  Nik Shaylor, Doug Simon
 */
final class Unsafe {

    private Unsafe() {}

    /*-----------------------------------------------------------------------*\
     *                      Storing to/loading from memory                   *
    \*-----------------------------------------------------------------------*/

    /**
     * Sets an 8 bit value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in bytes) from <code>base</code> at which to write
     * @param value the value to write
     */
     public static void setByte(Object base, int offset, int value) {
         Assert.that(VM.isHosted());
         int index = ((Address)base).add(offset).asIndex();
         checkAddress(index);
         memory[index] = (byte)(value>>0);
         setType0(index, AddressType.BYTE);
     }

     /**
      * Sets a signed 16 bit value in memory.
      *
      * @param base   the base address
      * @param offset the offset (in 16 bit words) from <code>base</code> at which to write
      * @param value  the value to write
      */
    public static void setShort(Object base, int offset, int value) {
        setChar(base, offset, value);
    }

    /**
     * Sets an unsigned 16 bit value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in 16 bit words) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setChar(Object base, int offset, int value) {
        Assert.that(VM.isHosted());
        int index = ((Address)base).add(offset * 2).asIndex();
        checkAddress(index + 1);
        if (VM.isBigEndian()) {
            memory[index+0] = (byte)(value>>8);
            memory[index+1] = (byte)(value>>0);
        } else {
            memory[index+0] = (byte)(value>>0);
            memory[index+1] = (byte)(value>>8);
        }
        setType0(index, AddressType.SHORT);
    }

    /**
     * Sets a 32 bit value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in 32 bit words) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setInt(Object base, int offset, int value) {
        Assert.that(VM.isHosted());
        int index = ((Address)base).add(offset * 4).asIndex();
        checkAddress(index + 3);
        if (VM.isBigEndian()) {
            memory[index + 0] = (byte) (value >> 24);
            memory[index + 1] = (byte) (value >> 16);
            memory[index + 2] = (byte) (value >> 8);
            memory[index + 3] = (byte) (value >> 0);
        }
        else {
            memory[index + 0] = (byte) (value >> 0);
            memory[index + 1] = (byte) (value >> 8);
            memory[index + 2] = (byte) (value >> 16);
            memory[index + 3] = (byte) (value >> 24);
        }
        setType0(index, AddressType.INT);
    }

    /**
     * Sets a UWord value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setUWord(Object base, int offset, UWord value) {
        Assert.that(VM.isHosted());
        setInt/*S64*/(base, offset, value.toPrimitive());
        int index = ((Address)base).add(offset * HDR.BYTES_PER_WORD).asIndex();
        setType0(index, AddressType.UWORD);
    }

    /**
     * Sets a 64 bit value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in 64 bit words) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setLong(Object base, int offset, long value) {
        Assert.that(VM.isHosted());
        int index = ((Address)base).add(offset * 8).asIndex();
        checkAddress(index + 7);
        if (VM.isBigEndian()) {
            memory[index+0] = (byte)(value>>56);
            memory[index+1] = (byte)(value>>48);
            memory[index+2] = (byte)(value>>40);
            memory[index+3] = (byte)(value>>32);
            memory[index+4] = (byte)(value>>24);
            memory[index+5] = (byte)(value>>16);
            memory[index+6] = (byte)(value>>8);
            memory[index+7] = (byte)(value>>0);
        } else {
            memory[index+0] = (byte)(value>>0);
            memory[index+1] = (byte)(value>>8);
            memory[index+2] = (byte)(value>>16);
            memory[index+3] = (byte)(value>>24);
            memory[index+4] = (byte)(value>>32);
            memory[index+5] = (byte)(value>>40);
            memory[index+6] = (byte)(value>>48);
            memory[index+7] = (byte)(value>>56);
        }
        setType0(index, AddressType.LONG);
    }

    /**
     * Sets a 64 bit value in memory at a 32 bit word offset.
     *
     * @param base   the base address
     * @param offset the offset (in 32 bit words) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setLongAtWord(Object base, int offset, long value) {
        Assert.that(VM.isHosted());
        Address ea = ((Address)base).add(offset * HDR.BYTES_PER_WORD);
        setLong(ea, 0, value);
        setType0(ea.asIndex(), AddressType.LONG);
    }

    /**
     * Sets a pointer value in memory without updating the write barrier.
     *
     * If this method is being called in a
     * {@link VM#isHosted() hosted} environment then the corresponding bit in the
     * oop map (if any) is also set.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setAddress(Object base, int offset, Object value) {
        Assert.that(VM.isHosted());
        Address ea = ((Address)base).add(offset * HDR.BYTES_PER_WORD);
        if (value instanceof Klass) {
            unresolvedClassPointers.put(ea, value);
            setUWord(ea, 0, UWord.zero());
        } else {
            Assert.that(value instanceof Address);
            unresolvedClassPointers.remove(ea);
            setUWord(ea, 0, ((Address)value).toUWord());
        }
        oopMap.set(ea.asIndex() / HDR.BYTES_PER_WORD);
        setType0(ea.asIndex(), AddressType.REF);
    }

    /**
     * Sets a pointer value in memory and updates the write barrier.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> at which to write
     * @param value  the value to write
     */
    public static void setObject(Object base, int offset, Object value) {
        setAddress(base, offset, value);
    }

    private static void setType0(int index, byte type) {
/*if[TYPEMAP]*/
        typeMap[index] = type;
/*end[TYPEMAP]*/
    }

    /**
     * Sets the type of a value at a given address.
     *
     * This operation is a nop when {@link VM#usingTypeMap()} returns false.
     *
     * @param ea   the address of the value
     * @param type the type of the value
     * @param size the size (in bytes) of the value
     */
    public static void setType(Address ea, byte type, int size) {
        Assert.that(VM.isHosted());
/*if[TYPEMAP]*/
        setType0(ea.asIndex(), type);
/*end[TYPEMAP]*/
    }

    /**
     * Gets the type of a value at a given address.
     *
     * This operation is a nop when {@link VM#usingTypeMap()} returns false.
     *
     * @param ea   the address to query
     * @return the type of the value at <code>ea</code>
     */
    public static byte getType(Address ea) {
        Assert.that(VM.isHosted());
/*if[TYPEMAP]*/
        return typeMap[ea.asIndex()];
/*else[TYPEMAP]*/
//      throw Assert.shouldNotReachHere();
/*end[TYPEMAP]*/
    }

    /**
     * Block copies the types recorded for a range of memory to another range of memory.
     *
     * @param src    the start address of the source range
     * @param dst    the start address of the destination range
     * @param length the length (in bytes) of the range
     */
    public static void copyTypes(Address src, Address dst, int length) {
        Assert.that(VM.isHosted());
/*if[TYPEMAP]*/
        System.arraycopy(typeMap, src.asIndex(), typeMap, dst.asIndex(), length);
/*end[TYPEMAP]*/
    }

    /**
     * Gets a signed 8 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in bytes) from <code>base</code> from which to load
     * @return the value
     */
    public static int getByte(Object base, int offset) {
        Assert.that(VM.isHosted());
        int index = ((Address)base).add(offset).asIndex();
        checkAddress(index);
        return memory[index];
    }

    /**
     * Gets a signed 16 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in 16 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getShort(Object base, int offset) {
        return (short)getChar(base, offset);
    }

    /**
     * Gets an unsigned 16 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in 16 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getChar(Object base, int offset) {
        Assert.that(VM.isHosted());
        int index = ((Address)base).add(offset * 2).asIndex();
        checkAddress(index + 1);
        int b0 = memory[index] & 0xFF;
        int b1 = memory[index + 1] & 0xFF;
        if (VM.isBigEndian()) {
            return b0 << 8 | b1;
        } else {
            return b1 << 8 | b0;
        }
    }


    /**
     * Gets a signed 32 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in 32 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getInt(Object base, int offset) {
        Assert.that(VM.isHosted());
        int index = ((Address)base).add(offset * 4).asIndex();
        checkAddress(index + 3);
        int b0 = memory[index + 0] & 0xFF;
        int b1 = memory[index + 1] & 0xFF;
        int b2 = memory[index + 2] & 0xFF;
        int b3 = memory[index + 3] & 0xFF;
        if (VM.isBigEndian()) {
            return (b0<<24) | (b1<<16) | (b2<<8) | b3;
        } else {
            return (b3<<24) | (b2<<16) | (b1<<8) | b0;
        }
    }

    /**
     * Gets an unsigned 32 or 64 bit value from memory.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> from which to load
     * @return the value
     */
    public static UWord getUWord(Object base, int offset) {
        Assert.that(VM.isHosted());
        return UWord.fromPrimitive(getInt/*S64*/(base, offset));
    }

    /**
     * Gets a 64 bit value from memory using a 64 bit word offset.
     *
     * @param base   the base address
     * @param offset the offset (in 64 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static long getLong(Object base, int offset) {
        Assert.that(VM.isHosted());
        int index = ((Address)base).add(offset * 8).asIndex();
        checkAddress(index + 7);
        long b0 = memory[index + 0] & 0xFF;
        long b1 = memory[index + 1] & 0xFF;
        long b2 = memory[index + 2] & 0xFF;
        long b3 = memory[index + 3] & 0xFF;
        long b4 = memory[index + 4] & 0xFF;
        long b5 = memory[index + 5] & 0xFF;
        long b6 = memory[index + 6] & 0xFF;
        long b7 = memory[index + 7] & 0xFF;
        if (VM.isBigEndian()) {
            return (b0<<56) | (b1<<48) | (b2<<40) | (b3<<32) | (b4<<24) | (b5<<16) | (b6<<8) | b7;
        } else {
            return (b7<<56) | (b6<<48) | (b5<<40) | (b4<<32) | (b3<<24) | (b2<<16) | (b1<<8) | b0;
        }
    }

    /**
     * Gets a 64 bit value from memory using a 32 bit word offset.
     *
     * @param base   the base address
     * @param offset the offset (in 32 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static long getLongAtWord(Object base, int offset) {
        return getLong(((Address)base).add(offset * HDR.BYTES_PER_WORD), 0);
    }

    /**
     * Gets a pointer from memory.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> from which to load
     * @return the value
     */
    public static Object getObject(Object base, int offset) {
        Assert.that(VM.isHosted());
        return Address.get(getUWord(base, offset).toPrimitive());
    }

    /**
     * Gets a UWord value from memory ignoring any recorded type of the value at the designated location.
     * This operation is equivalent to {@link #getUWord(Object, int)} when {@link VM#usingTypeMap() runtime type checking}
     * is disabled.
     *
     * @param base   the base address
     * @param offset the offset (in words) from <code>base</code> from which to load
     * @return the value
     */
    public static UWord getAsUWord(Object base, int offset) {
        Assert.that(VM.isHosted());
        return getUWord(base, offset);
    }

    /**
     * Gets a signed 8 bit value from memory ignoring any recorded type of the value at the designated location.
     * This operation is equivalent to {@link #getByte(Object, int)} when {@link VM#usingTypeMap() runtime type checking}
     * is disabled.
     *
     * @param base   the base address
     * @param offset the offset (in 8 bit words) from <code>base</code> from which to load
     * @return the value
     */
    public static int getAsByte(Object base, int offset) {
        Assert.that(VM.isHosted());
        return getByte(base, offset);
    }

    /**
     * Gets character from a string.
     *
     * @param str   the string
     * @param index the index to the character
     * @return the value
     */
    public static char charAt(String str, int index) {
        return str.charAt(index);
    }

    /*-----------------------------------------------------------------------*\
     *                      Hosted execution support                         *
    \*-----------------------------------------------------------------------*/

    /**
     * A table of all the addresses that hold a pointer to a class which has
     * not yet been written to memory.
     */
    private static Hashtable unresolvedClassPointers = new Hashtable();

    /**
     * Resolve all the deferred writes of unresolved class pointers.
     *
     * @param classMap a map from JVM objects to their addresses in the image. This
     *                 is used to patch up class pointers in objects that were
     *                 written to the image before their classes were.
     */
    static void resolveClasses(ArrayHashtable classMap) {
        Enumeration keys = unresolvedClassPointers.keys();
        Enumeration values = unresolvedClassPointers.elements();
        while (keys.hasMoreElements()) {
            Address address = (Address)keys.nextElement();
            Klass unresolvedClass = (Klass)values.nextElement();
            Address klassAddress = (Address)classMap.get(unresolvedClass);
            setAddress(address, 0, klassAddress);
        }
        unresolvedClassPointers.clear();
    }

    /**
     * Clears a pointer value in memory.
     *
     * @param base   the base address
     * @param offset the offset (in UWords) from <code>base</code> of the pointer to clear
     */
    static void clearObject(Object base, int offset) {
        Assert.that(VM.isHosted());
        Address ea = ((Address)base).add(offset * HDR.BYTES_PER_WORD);
        setUWord(ea, 0, UWord.zero());
        unresolvedClassPointers.remove(ea);
        oopMap.clear(ea.asIndex() / HDR.BYTES_PER_WORD);
        setType0(ea.asIndex(), AddressType.UNDEFINED);
    }

    /*-----------------------------------------------------------------------*\
     *                      Memory model and initialization                  *
    \*-----------------------------------------------------------------------*/

    /**
     * The memory model.
     */
    private static byte[] memory = {};

/*if[TYPEMAP]*/
    /**
     * The type checking map for memory.
     */
    private static byte[] typeMap = {};
/*end[TYPEMAP]*/

    /**
     * The used amount of memory.
     */
    private static int memorySize = 0;

    /**
     * The oop map describing where the pointers in memory are.
     */
    private static final BitSet oopMap = new BitSet();

    /**
     * Verifies that a given address is within range of the currently allocated
     * memory.
     *
     * @param address  the address to check
     * @throws IndexOfOutBoundsException if the address is out of bounds
     */
    private static void checkAddress(int address) throws IndexOutOfBoundsException {
        if (address < 0 || address >= memorySize) {
            throw new IndexOutOfBoundsException("address is out of range: " + address);
        }
    }

    /**
     * Ensures that the underlying buffer representing memory is at least a given size.
     *
     * @param size  the minimum size the memory buffer will be upon returning
     */
    private static void ensureCapacity(int size) {
        size = GC.roundUpToWord(size);
        if (memory.length < size) {
//System.err.println("growing memory: " + memory.length + " -> " + size*2);
            byte[] newMemory = new byte[size * 2];
            System.arraycopy(memory, 0, newMemory, 0, memory.length);
            memory = newMemory;
/*if[TYPEMAP]*/
            byte[] newTypeMap = new byte[memory.length];
            System.arraycopy(typeMap, 0, newTypeMap, 0, typeMap.length);
            typeMap = newTypeMap;
/*end[TYPEMAP]*/
        }
    }

    /**
     * Initialize or appends to the contents of memory.
     *
     * @param buffer  a buffer containing a serialized object memory relative to 0
     * @param oopMap  an oop map specifying where the pointers in the serialized object memory are
     * @param append  specifies if the memory is being appended to
     */
    static void initialize(byte[] buffer, BitSet oopMap, boolean append) {
        Assert.that(VM.isHosted());
        if (!append) {
            setMemorySize(buffer.length);
            System.arraycopy(buffer, 0, memory, 0, buffer.length);

            // Set up the oop map
            Unsafe.oopMap.or(oopMap);
        } else {
            int canonicalStart = memorySize;
            setMemorySize(memorySize + buffer.length);
            System.arraycopy(buffer, 0, memory, canonicalStart, buffer.length);

            // OR the given oop map onto the logical end of the existing oop map
            int offset = canonicalStart / HDR.BYTES_PER_WORD;
            Unsafe.oopMap.or(oopMap, offset);
        }
    }

    /**
     * Sets the size of used/initialized memory. If the new size is less than the current size, all
     * memory locations at index <code>newSize</code> and greater are zeroed.
     *
     * @param   newSize   the new size of memory
     */
    static void setMemorySize(int newSize) {
        Assert.always(newSize >= 0);
        if (newSize > memorySize) {
            ensureCapacity(newSize);
        } else {
            for (int i = newSize ; i < memory.length ; i++) {
                memory[i] = 0;
            }
        }
        memorySize = newSize;
    }

    /**
     * Gets the amount of used/initialized memory.
     *
     * @return the amount of used/initialized memory
     */
    static int getMemorySize() {
        return memorySize;
    }

    /**
     * Determines if the word at a given address is a reference. A word is a reference if
     * the last update at the address was via {@link #setObject(Object,int,Object)}.
     *
     * @param address  the address to test
     * @return true if <code>address</code> is a reference
     */
    static boolean isReference(Address address) {
        return (address.asIndex() % HDR.BYTES_PER_WORD) == 0 && oopMap.get(address.asIndex() / HDR.BYTES_PER_WORD);
    }

    /**
     * Gets the oop map that describes where all the pointers in the memory are.
     *
     * @return the oop map that describes where all the pointers in the memory are
     */
    static BitSet getOopMap() {
        return oopMap;
    }
}
