/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.vm.HDR;
import com.sun.squawk.vm.AddressType;
import com.sun.squawk.util.*;
import java.util.Enumeration;

/**
 * The Address class is used to abstract machine addresses. It is used instead
 * of 'int' or 'Object' for coding clarity and machine-portability (it can map to
 * 32 bit and 64 bit integral types).
 * <p>
 * This class is known specially by the translator as a {@link Modifier#SQUAWKPRIMITIVE}
 * and programming with it requires adhering to the restrictions implied by this
 * attribute.
 * <p>
 * Only the public methods of this class which do not override any of the
 * methods in java.lang.Object will be available in a {@link VM#isHosted() non-hosted}
 * environment. The translator replaces any calls to these methods to native
 * method calls.
 * <p>
 * This mechanism was largely inspired by the VM_Address class in the Jikes RVM.
 *
 * @author Doug Simon
 */
final class Address {

    /**
     * Casts a word expressed as the appropriate Java primitive type for the platform (i.e. int or long)
     * into a value of type Address.
     *
     * @param  word an address expressed as an int or long
     * @return the canonical Address instance for <code>value</code>
     */
    public static Address fromPrimitive(int/*S64*/ value) {
        Assert.that(VM.isHosted());
        return new Address(value);
    }

    /**
     * Casts an object reference to an address.
     *
     * @param object   the object reference to cast
     * @return the object reference as an address
     */
    public static Address fromObject(Object object) {
        Assert.that(VM.isHosted());
        Assert.that(object instanceof Address);
        return (Address)object;
    }

    /**
     * Gets the canonical Address representation of <code>null</code>.
     *
     * @return the canonical Address representation of <code>null</code>
     */
    public static Address zero() {
        Assert.that(VM.isHosted());
        return get(0);
    }

    /**
     * Gets the largest possible machine address.
     *
     * @return  the largest possible machine address
     */
    public static Address max() {
        Assert.that(VM.isHosted());
        return get(-1);
    }

    /**
     * Casts this address to an object reference.
     *
     * @return this address as an object reference
     */
    public Object toObject() {
        Assert.that(VM.isHosted());
        return this;
    }

    /**
     * Casts this address to a UWord.
     *
     * @return this address as a UWord
     */
    public UWord toUWord() {
        Assert.that(VM.isHosted());
        return UWord.fromPrimitive(value);
    }

    /**
     * Adds a 32 bit offset to this address and return the resulting address.
     *
     * @param offset   the offset to add
     * @return the result of adding <code>offset</code> to this address
     */
    public Address add(int offset) {
        Assert.that(VM.isHosted());
        return get(value + offset);
    }

    /**
     * Subtracts a 32 bit offset to this address and return the resulting address.
     *
     * @param offset   the offset to subract
     * @return the result of subtracting <code>offset</code> to this address
     */
    public Address sub(int offset) {
        Assert.that(VM.isHosted());
        return get(value - offset);
    }

    /**
     * Adds a 32 or 64 bit offset to this address and return the resulting address.
     *
     * @param offset   the offset to add
     * @return the result of adding <code>offset</code> to this address
     */
    public Address addOffset(Offset offset) {
        Assert.that(VM.isHosted());
        return get(value + offset.toPrimitive());
    }

    /**
     * Subtracts a 32 or 64 bit offset to this address and return the resulting address.
     *
     * @param offset   the offset to subract
     * @return the result of subtracting <code>offset</code> to this address
     */
    public Address subOffset(Offset offset) {
        Assert.that(VM.isHosted());
        return get(value - offset.toPrimitive());
    }

    /**
     * Logically OR a word with this address.
     *
     * @param word   the word to OR this address with
     * @return       the result of the OR operation
     */
    public Address or(UWord word) {
        Assert.that(VM.isHosted());
        return get(value | word.toPrimitive());
    }

    /**
     * Logically AND a word with this address.
     *
     * @param word   the word to AND this address with
     * @return       the result of the AND operation
     */
    public Address and(UWord word) {
        Assert.that(VM.isHosted());
        return get(value & word.toPrimitive());
    }

    /**
     * Calculates the offset between this address an another address.
     *
     * @param address2   the address to compare this address with
     * @return the offset that must be applied to this address to get <code>address2</code>
     */
    public Offset diff(Address address2) {
        Assert.that(VM.isHosted());
        Assert.that(value >= address2.value);
        return Offset.fromPrimitive(value - address2.value);
    }

    /**
     * Determines if this address is <code>null</code>.
     *
     * @return true if this address is <code>null</code>
     */
    public boolean isZero() {
        Assert.that(VM.isHosted());
        return this == zero();
    }

    /**
     * Determines if this address is equals to {@link #max() max}.
     *
     * @return true if this address is equals to {@link #max() max}
     */
    public boolean isMax() {
        Assert.that(VM.isHosted());
        return this == max();
    }

    /**
     * Determines if this address is equal to a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is equal to <code>address2</code>
     */
    public boolean eq(Address address2) {
        Assert.that(VM.isHosted());
        return this == address2;
    }

    /**
     * Determines if this address is not equal to a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is not equal to <code>address2</code>
     */
    public boolean ne(Address address2) {
        Assert.that(VM.isHosted());
        return this != address2;
    }

    /**
     * Determines if this address is lower than a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is lower than or equals to <code>address2</code>
     */
    public boolean lo(Address address2) {
        Assert.that(VM.isHosted());
        if (value >= 0 && address2.value >= 0) return value < address2.value;
        if (value < 0 && address2.value < 0) return value < address2.value;
        if (value < 0) return false;
        return true;
    }

    /**
     * Determines if this address is lower than or equal to a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is lower than or equal to <code>address2</code>
     */
    public boolean loeq(Address address2) {
        Assert.that(VM.isHosted());
        return (this == address2) || lo(address2);
    }

    /**
     * Determines if this address is higher than a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is higher than <code>address2</code>
     */
    public boolean hi(Address address2) {
        Assert.that(VM.isHosted());
        return address2.lo(this);
    }

    /**
     * Determines if this address is higher than or equal to a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is higher than or equal to <code>address2</code>
     */
    public boolean hieq(Address address2) {
        Assert.that(VM.isHosted());
        return address2.loeq(this);
    }

    /**
     * Rounds this address up based on a given alignment.
     *
     * @param alignment  this address is rounded up to be a multiple of this value
     * @return the new address
     */
    public Address roundUp(int alignment) {
        Assert.that(VM.isHosted());
        return get((value + (alignment-1)) & ~(alignment-1));
    }

    /**
     * Rounds this address up to a machine word boundary.
     *
     * @return the new address
     */
    public Address roundUpToWord() {
        Assert.that(VM.isHosted());
        return get((value + (HDR.BYTES_PER_WORD-1)) & ~(HDR.BYTES_PER_WORD-1));
    }

    /**
     * Rounds this address down based on a given alignment.
     *
     * @param alignment  this address is rounded down to be a multiple of this value
     * @return the new address
     */
    public Address roundDown(int alignment) {
        Assert.that(VM.isHosted());
        return get(value & ~(alignment-1));
    }

    /**
     * Rounds this address down to a machine word boundary.
     *
     * @return the new address
     */
    public Address roundDownToWord() {
        Assert.that(VM.isHosted());
        return get(value & ~(HDR.BYTES_PER_WORD-1));
    }

    /*-----------------------------------------------------------------------*\
     *                      Hosted execution support                         *
    \*-----------------------------------------------------------------------*/

    /**
     * Gets a hashcode value for this address which is just the address itself.
     *
     * @return  the value of this address
     */
    public int hashCode() {
        return (int)value;
    }

    /**
     * Gets a string representation of this address.
     *
     * @return String
     */
    public String toString() {
        return ""+value;
    }

    /**
     * The address represented.
     */
    private final int/*S64*/ value;

    /**
     * Unique instance pool.
     */
    private static /*S64*/IntHashtable pool;

    /**
     * Gets the canonical Address instance for a given address.
     *
     * @param  value   the machine address
     * @return the canonical Address instance for <code>value</code>
     */
    static Address get(int/*S64*/ value) {
        if (pool == null) {
            pool = new /*S64*/IntHashtable();
        }
        Address addr = (Address)pool.get(value);
        if (addr == null) {
            addr = new Address(value);
            try {
                pool.put(value, addr);
            } catch (OutOfMemoryError e) {
                throw new OutOfMemoryError("Failed to grow pool when adding " + value);
            }
        }
        return addr;
    }

    /**
     * Constructor.
     *
     * @param value  a machine address
     */
    private Address(int/*S64*/ value) {
        Assert.that(VM.isHosted());
        this.value = value;
    }

    /**
     * Checks that a given long value can be encoded as a 32 bit value.
     *
     * @param  value   the long value to check
     * @return 'value' converted to a 32 bit value
     */
    static int assert32(long value) {
        Assert.always((int)value == value, "address is out of 32 bit range");
        return (int)value;
    }

    /**
     * Converts the address into an integer index. This ensures that the memory being
     * modeled is 32 bit addressable which is a requirement given that the byte
     * array being used to model memory is only indexable by a 32 bit Java integer.
     *
     * @return this address as a 32 bit int
     */
    int asIndex() {
        return assert32(value);
    }
}
