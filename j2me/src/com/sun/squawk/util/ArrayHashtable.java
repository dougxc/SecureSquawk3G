// Like Hashtable except it will do a shallow compare of object arrays.
// Also all synchronized attributes are removed

/*
 * Copyright 1995-2001 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package com.sun.squawk.util;

import java.util.Enumeration;

/**
 * This class implements a hashtable, which maps keys to values. Any
 * non-<code>null</code> object can be used as a key or as a value. However,
 * this variation of the standard {@link java.util.Hashtable hashtable}
 * specializes the use of array objects as keys. In particular, it uses the
 * <code>equals</code> and <code>hashCode</code> methods in the {@link Arrays}
 * class to do array-aware key comparison.
 *
 * @see     Arrays#equals(Object)
 * @see     Arrays#hashCode(Object)
 */
public class ArrayHashtable {

    /**
     * The hash table data.
     */
    private transient ArrayHashtableEntry table[];

    /**
     * The total number of entries in the hash table.
     */
    private transient int count;

    /**
     * Rehashes the table when count exceeds this threshold.
     */
    private int threshold;

    /**
     * The load factor for the hashtable.
     */
    private static final int loadFactorPercent = 75;

    /**
     * Constructs a new, empty hashtable with the specified initial
     * capacity.
     *
     * @param      initialCapacity   the initial capacity of the hashtable.
     * @exception  IllegalArgumentException  if the initial capacity is less
     *             than zero
     * @since      JDK1.0
     */
    public ArrayHashtable(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException();
        }
        if (initialCapacity == 0) {
            initialCapacity = 1;
        }
        table = new ArrayHashtableEntry[initialCapacity];
        threshold = (int)((initialCapacity * loadFactorPercent) / 100);
    }

    /**
     * Constructs a new, empty hashtable with a default capacity and load
     * factor.
     *
     * @since   JDK1.0
     */
    public ArrayHashtable() {
        this(11);
    }

    /**
     * Returns the number of keys in this hashtable.
     *
     * @return  the number of keys in this hashtable.
     * @since   JDK1.0
     */
    public int size() {
        return count;
    }

    /**
     * Tests if this hashtable maps no keys to values.
     *
     * @return  <code>true</code> if this hashtable maps no keys to values;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Returns an enumeration of the keys in this hashtable.
     *
     * @return  an enumeration of the keys in this hashtable.
     * @see     java.util.Enumeration
     * @see     java.util.Hashtable#elements()
     * @since   JDK1.0
     */
    public Enumeration keys() {
        return new ArrayHashtableEnumerator(table, true);
    }

    /**
     * Returns an enumeration of the values in this hashtable.
     * Use the Enumeration methods on the returned object to fetch the elements
     * sequentially.
     *
     * @return  an enumeration of the values in this hashtable.
     * @see     java.util.Enumeration
     * @see     java.util.Hashtable#keys()
     * @since   JDK1.0
     */
    public Enumeration elements() {
        return new ArrayHashtableEnumerator(table, false);
    }

    /**
     * Tests if some key maps into the specified value in this hashtable.
     * This operation is more expensive than the <code>containsKey</code>
     * method.
     *
     * @param      value   a value to search for.
     * @return     <code>true</code> if some key maps to the
     *             <code>value</code> argument in this hashtable;
     *             <code>false</code> otherwise.
     * @exception  NullPointerException  if the value is <code>null</code>.
     * @see        java.util.Hashtable#containsKey(java.lang.Object)
     * @since      JDK1.0
     */
    public boolean contains(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }

        ArrayHashtableEntry tab[] = table;
        for (int i = tab.length ; i-- > 0 ;) {
            for (ArrayHashtableEntry e = tab[i] ; e != null ; e = e.next) {
                if (e.valueEquals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests if the specified object is a key in this hashtable.
     *
     * @param   key   possible key.
     * @return  <code>true</code> if the specified object is a key in this
     *          hashtable; <code>false</code> otherwise.
     * @see     java.util.Hashtable#contains(java.lang.Object)
     * @since   JDK1.0
     */
    public boolean containsKey(Object key) {
        ArrayHashtableEntry tab[] = table;
        int hash = Arrays.hashCode(key);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (ArrayHashtableEntry e = tab[index] ; e != null ; e = e.next) {
            if ((e.hash() == hash) && e.keyEquals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value to which the specified key is mapped in this hashtable.
     *
     * @param   key   a key in the hashtable.
     * @return  the value to which the key is mapped in this hashtable;
     *          <code>null</code> if the key is not mapped to any value in
     *          this hashtable.
     * @see     java.util.Hashtable#put(java.lang.Object, java.lang.Object)
     * @since   JDK1.0
     */
    public Object get(Object key) {
        ArrayHashtableEntry tab[] = table;
        int hash = Arrays.hashCode(key);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (ArrayHashtableEntry e = tab[index] ; e != null ; e = e.next) {
            if ((e.hash() == hash) && e.keyEquals(key)) {
                return e.value;
            }
        }
        return null;
    }

    /**
     * Rehashes the contents of the hashtable into a hashtable with a
     * larger capacity. This method is called automatically when the
     * number of keys in the hashtable exceeds this hashtable's capacity
     * and load factor.
     *
     * @since   JDK1.0
     */
    protected void rehash() {
        int oldCapacity = table.length;
        ArrayHashtableEntry oldTable[] = table;

        int newCapacity = oldCapacity * 2 + 1;
        ArrayHashtableEntry newTable[] = new ArrayHashtableEntry[newCapacity];

        threshold = (int)((newCapacity * loadFactorPercent) / 100);
        table = newTable;

        for (int i = oldCapacity ; i-- > 0 ;) {
            for (ArrayHashtableEntry old = oldTable[i] ; old != null ; ) {
                ArrayHashtableEntry e = old;
                old = old.next;

                int index = (e.hash() & 0x7FFFFFFF) % newCapacity;
                e.next = newTable[index];
                newTable[index] = e;
            }
        }
    }

    /**
     * Maps the specified <code>key</code> to the specified
     * <code>value</code> in this hashtable. Neither the key nor the
     * value can be <code>null</code>.
     * <p>
     * The value can be retrieved by calling the <code>get</code> method
     * with a key that is equal to the original key.
     *
     * @param      key     the hashtable key.
     * @param      value   the value.
     * @return     the previous value of the specified key in this hashtable,
     *             or <code>null</code> if it did not have one.
     * @exception  NullPointerException  if the key or value is
     *               <code>null</code>.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.util.Hashtable#get(java.lang.Object)
     * @since   JDK1.0
     */
    public Object put(Object key, Object value) {
        // Make sure the value is not null
        if (value == null) {
            throw new NullPointerException();
        }

        // Makes sure the key is not already in the hashtable.
        ArrayHashtableEntry tab[] = table;
        int hash = Arrays.hashCode(key);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (ArrayHashtableEntry e = tab[index] ; e != null ; e = e.next) {
            if ((e.hash() == hash) && e.keyEquals(key)) {
                Object old = e.value;
                e.value = value;
                return old;
            }
        }

        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();
            return put(key, value);
        }

        // Creates the new entry.
        ArrayHashtableEntry e = new ArrayHashtableEntry();
        e.hash(hash);
        e.key = key;
        e.value = value;
        e.next = tab[index];
        tab[index] = e;
        count++;
        return null;
    }

    /**
     * Removes the key (and its corresponding value) from this
     * hashtable. This method does nothing if the key is not in the hashtable.
     *
     * @param   key   the key that needs to be removed.
     * @return  the value to which the key had been mapped in this hashtable,
     *          or <code>null</code> if the key did not have a mapping.
     * @since   JDK1.0
     */
    public Object remove(Object key) {
        ArrayHashtableEntry tab[] = table;
        int hash = Arrays.hashCode(key);
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (ArrayHashtableEntry e = tab[index], prev = null ; e != null ; prev = e, e = e.next) {
            if ((e.hash() == hash) && e.keyEquals(key)) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
                return e.value;
            }
        }
        return null;
    }

    /**
     * Clears this hashtable so that it contains no keys.
     *
     * @since   JDK1.0
     */
    public void clear() {
        ArrayHashtableEntry tab[] = table;
        for (int index = tab.length; --index >= 0; )
            tab[index] = null;
        count = 0;
    }

    /**
     * Returns a rather long string representation of this hashtable.
     *
     * @return  a string representation of this hashtable.
     * @since   JDK1.0
     */
    public String toString() {
        int max = size() - 1;
        StringBuffer buf = new StringBuffer();
        Enumeration k = keys();
        Enumeration e = elements();
        buf.append("{");

        for (int i = 0; i <= max; i++) {
            String s1 = k.nextElement().toString();
            String s2 = e.nextElement().toString();
            buf.append(s1 + "=" + s2);
            if (i < max) {
                buf.append(", ");
            }
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * A hashtable enumerator class.  This class should remain opaque
     * to the client. It will use the Enumeration interface.
     */
    class ArrayHashtableEnumerator implements Enumeration {
        boolean keys;
        int index;
        ArrayHashtableEntry table[];
        ArrayHashtableEntry entry;

        ArrayHashtableEnumerator(ArrayHashtableEntry table[], boolean keys) {
            this.table = table;
            this.keys = keys;
            this.index = table.length;
        }

        public boolean hasMoreElements() {
            if (entry != null) {
                return true;
            }
            while (index-- > 0) {
                if ((entry = table[index]) != null) {
                    return true;
                }
            }
            return false;
        }

        public Object nextElement() {
            if (entry == null) {
                while ((index-- > 0) && ((entry = table[index]) == null));
            }
            if (entry != null) {
                ArrayHashtableEntry e = entry;
                entry = e.next;
                return keys ? e.key : e.value;
            }
            throw new RuntimeException("no nextElement");
        }
    }
}

/**
 * Hashtable collision list.
 */
class ArrayHashtableEntry {
    private int hash;
    Object key;
    Object value;
    ArrayHashtableEntry next;

    void hash(int hash) {
        this.hash = hash;
    }

    int hash() {
        return hash;
    }

    boolean keyEquals(Object anObject) {
        if (key.getClass() != anObject.getClass()) {
            return false;
        }
        return Arrays.equals(key, anObject);
    }

    boolean valueEquals(Object anObject) {
        if (value.getClass() != anObject.getClass()) {
            return false;
        }
        return Arrays.equals(value, anObject);
    }
}


