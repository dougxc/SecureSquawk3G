package java.lang;

import com.sun.squawk.util.*;

/**
 * The word type is used by the runtime system and collector to denote machine
 * word-sized quantities. It is used instead of 'int' or 'Object' for coding
 * clarity, machine-portability (it can map to 32 bit and 64 bit integral types)
 * and access to unsigned operations (Java does not have unsigned int types).
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
 * This mechanism was largely inspired by the VM_Word class in the Jikes RVM.
 *
 * @author Doug Simon
 */
final class UWord {

    /**
     * Casts a word expressed as the appropriate Java primitive type for the platform (i.e. int or long)
     * into a value of type UWord.
     *
     * @param  value   a word expressed as an int or long
     * @return the canonical UWord instance for <code>value</code>
     */
    public static UWord fromPrimitive(int/*S64*/ value) {
        Assert.that(VM.isHosted());
        return get(value);
    }

    /**
     * Casts a value of type UWord into the appropriate Java primitive type for the platform (i.e. int or long).
     * This will cause a fatal error if this cast cannot occur without changing this uword's sign or
     * truncating its magnitude.
     *
     * @return this UWord value as an int or long
     */
    public int/*S64*/ toPrimitive() {
        return value;
    }

    /**
     * Casts a value of type UWord into an int. This will cause a fatal error if this UWord
     * value cannot be expressed as a signed 32 bit Java int without changing its sign or
     * truncating its magnitude.
     *
     * @return this UWord value as an int
     */
    public int toInt() {
        Assert.that((int)value == value);
        return (int)value;
    }

    /**
     * Casts a value of type UWord into an Offset. This may cause a change in sign if this word value cannot be expressed
     * as a signed quantity.
     *
     * @return this UWord value as an Offset
     */
    public Offset toOffset() {
        return Offset.fromPrimitive(value);
    }

    /**
     * Gets the canonical UWord representation of <code>null</code>.
     *
     * @return the canonical UWord representation of <code>null</code>
     */
    public static UWord zero() {
        Assert.that(VM.isHosted());
        return get(0);
    }

    /**
     * Gets the largest possible machine word.
     *
     * @return  the largest possible machine word
     */
    public static UWord max() {
        Assert.that(VM.isHosted());
        return get(-1);
    }

    /**
     * Logically OR a word with this word.
     *
     * @param word   the word to OR this word with
     * @return       the result of the OR operation
     */
    public UWord or(UWord word) {
        Assert.that(VM.isHosted());
        return get(this.value | word.value);
    }

    /**
     * Logically AND a word with this word.
     *
     * @param word   the word to AND this word with
     * @return       the result of the AND operation
     */
    public UWord and(UWord word) {
        Assert.that(VM.isHosted());
        return get(this.value & word.value);
    }

    /**
     * Determines if this word is 0.
     *
     * @return true if this word is 0.
     */
    public boolean isZero() {
        Assert.that(VM.isHosted());
        return this == zero();
    }

    /**
     * Determines if this word is equals to {@link #max() max}.
     *
     * @return true if this word is equals to {@link #max() max}
     */
    public boolean isMax() {
        Assert.that(VM.isHosted());
        return this == max();
    }

    /**
     * Determines if this word is equal to a given word.
     *
     * @param word2   the word to compare this word against
     * @return true if this word is equal to <code>word2</code>
     */
    public boolean eq(UWord word2) {
        Assert.that(VM.isHosted());
        return this == word2;
    }

    /**
     * Determines if this word is not equal to a given word.
     *
     * @param word2   the word to compare this word against
     * @return true if this word is not equal to <code>word2</code>
     */
    public boolean ne(UWord word2) {
        Assert.that(VM.isHosted());
        return this != word2;
    }

    /**
     * Determines if this word is lower than a given word.
     *
     * @param word2   the word to compare this word against
     * @return true if this word is lower than or equals to <code>word2</code>
     */
    public boolean lo(UWord word2) {
        Assert.that(VM.isHosted());
        if (value >= 0 && word2.value >= 0) return value < word2.value;
        if (value < 0 && word2.value < 0) return value < word2.value;
        if (value < 0) return false;
        return true;
    }

    /**
     * Determines if this word is lower than or equal to a given word.
     *
     * @param word2   the word to compare this word against
     * @return true if this word is lower than or equal to <code>word2</code>
     */
    public boolean loeq(UWord word2) {
        Assert.that(VM.isHosted());
        return (this == word2) || lo(word2);
    }

    /**
     * Determines if this word is higher than a given word.
     *
     * @param word2   the word to compare this word against
     * @return true if this word is higher than <code>word2</code>
     */
    public boolean hi(UWord word2) {
        Assert.that(VM.isHosted());
        return word2.lo(this);
    }

    /**
     * Determines if this word is higher than or equal to a given word.
     *
     * @param word2   the word to compare this word against
     * @return true if this word is higher than or equal to <code>word2</code>
     */
    public boolean hieq(UWord word2) {
        Assert.that(VM.isHosted());
        return word2.loeq(this);
    }

    /*-----------------------------------------------------------------------*\
     *                      Hosted execution support                         *
    \*-----------------------------------------------------------------------*/

    /**
     * Gets a hashcode value for this word which is just the value itself.
     *
     * @return  the value of this word
     */
    public int hashCode() {
        return (int)value;
    }

    /**
     * Gets a string representation of this word.
     *
     * @return String
     */
    public String toString() {
        return ""+value;
    }

    /**
     * The word value.
     */
    private final int/*S64*/ value;

    /**
     * Unique instance pool.
     */
    private static /*S64*/IntHashtable pool;

    /**
     * Gets the canonical UWord instance for a given word.
     *
     * @param  value   the machine word
     * @return the canonical UWord instance for <code>value</code>
     */
    private static UWord get(int/*S64*/ value) {
        if (pool == null) {
            pool = new /*S64*/IntHashtable();
        }
        UWord instance = (UWord)pool.get(value);
        if (instance == null) {
            instance = new UWord(value);
            try {
                pool.put(value, instance);
            } catch (OutOfMemoryError e) {
                throw new OutOfMemoryError("Failed to grow pool when adding " + value);
            }
        }
        return instance;
    }

    /**
     * Constructor.
     *
     * @param value  a machine word
     */
    private UWord(int/*S64*/ value) {
        Assert.that(VM.isHosted());
        this.value = value;
    }
}
