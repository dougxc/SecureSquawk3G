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

package java.util;

/**
 * The <code>Vector</code> class implements a growable array of
 * objects. Like an array, it contains components that can be
 * accessed using an integer index. However, the size of a
 * <code>Vector</code> can grow or shrink as needed to accommodate
 * adding and removing items after the <code>Vector</code> has been created.
 * <p>
 * Each vector tries to optimize storage management by maintaining a
 * <code>capacity</code> and a <code>capacityIncrement</code>. The
 * <code>capacity</code> is always at least as large as the vector
 * size; it is usually larger because as components are added to the
 * vector, the vector's storage increases in chunks the size of
 * <code>capacityIncrement</code>. An application can increase the
 * capacity of a vector before inserting a large number of
 * components; this reduces the amount of incremental reallocation.
 * <p>
 * Note: To conserve space, the CLDC implementation
 * is based on JDK 1.1.8, not JDK 1.3.
 *
 * @author  Lee Boynton
 * @author  Jonathan Payne
 * @version 1.39, 07/01/98 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0
 */
public class Vector extends com.sun.squawk.util.Vector {

    /**
     * Constructs an empty vector with the specified initial capacity and
     * capacity increment.
     *
     * @param   initialCapacity     the initial capacity of the vector.
     * @param   capacityIncrement   the amount by which the capacity is
     *                              increased when the vector overflows.
     * @exception IllegalArgumentException if the specified initial capacity
     *            is negative
     */
    public Vector(int initialCapacity, int capacityIncrement) {
        super(initialCapacity, capacityIncrement);
    }

    /**
     * Constructs an empty vector with the specified initial capacity.
     *
     * @param   initialCapacity   the initial capacity of the vector.
     * @since   JDK1.0
     */
    public Vector(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs an empty vector.
     *
     * @since   JDK1.0
     */
    public Vector() {
        super();
    }

    /**
     * Copies the components of this vector into the specified array.
     * The array must be big enough to hold all the objects in this  vector.
     *
     * @param   anArray   the array into which the components get copied.
     * @since   JDK1.0
     */
    public synchronized void copyInto(Object anArray[]) {
        super.copyInto(anArray);
    }

    /**
     * Trims the capacity of this vector to be the vector's current
     * size. An application can use this operation to minimize the
     * storage of a vector.
     *
     * @since   JDK1.0
     */
    public synchronized void trimToSize() {
        super.trimToSize();
    }

    /**
     * Increases the capacity of this vector, if necessary, to ensure
     * that it can hold at least the number of components specified by
     * the minimum capacity argument.
     *
     * @param   minCapacity   the desired minimum capacity.
     * @since   JDK1.0
     */
    public synchronized void ensureCapacity(int minCapacity) {
        super.ensureCapacity(minCapacity);
    }

    /**
     * Sets the size of this vector. If the new size is greater than the
     * current size, new <code>null</code> items are added to the end of
     * the vector. If the new size is less than the current size, all
     * components at index <code>newSize</code> and greater are discarded.
     *
     * @param   newSize   the new size of this vector.
     * @since   JDK1.0
     */
    public synchronized void setSize(int newSize) {
        super.setSize(newSize);
    }

    /**
     * Returns an enumeration of the components of this vector.
     *
     * @return  an enumeration of the components of this vector.
     * @see     java.util.Enumeration
     * @since   JDK1.0
     */
    public synchronized Enumeration elements() {
        return super.elements();
    }

    /**
     * Searches for the first occurence of the given argument, beginning
     * the search at <code>index</code>, and testing for equality using
     * the <code>equals</code> method.
     *
     * @param   elem    an object.
     * @param   index   the index to start searching from.
     * @return  the index of the first occurrence of the object argument in
     *          this vector at position <code>index</code> or later in the
     *          vector; returns <code>-1</code> if the object is not found.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @since   JDK1.0
     */
    public synchronized int indexOf(Object elem, int index) {
        return super.indexOf(elem, index);
    }

    /**
     * Searches backwards for the specified object, starting from the
     * specified index, and returns an index to it.
     *
     * @param   elem    the desired component.
     * @param   index   the index to start searching from.
     * @return  the index of the last occurrence of the specified object in this
     *          vector at position less than <code>index</code> in the vector;
     *          <code>-1</code> if the object is not found.
     * @since   JDK1.0
     */
    public synchronized int lastIndexOf(Object elem, int index) {
        return super.lastIndexOf(elem, index);
    }

    /**
     * Returns the component at the specified index.
     *
     * @param      index   an index into this vector.
     * @return     the component at the specified index.
     * @exception  ArrayIndexOutOfBoundsException  if an invalid index was
     *             given.
     * @since      JDK1.0
     */
    public synchronized Object elementAt(int index) {
        return super.elementAt(index);
    }

    /**
     * Returns the first component of this vector.
     *
     * @return     the first component of this vector.
     * @exception  NoSuchElementException  if this vector has no components.
     * @since      JDK1.0
     */
    public synchronized Object firstElement() {
        return super.firstElement();

    }

    /**
     * Returns the last component of the vector.
     *
     * @return  the last component of the vector, i.e., the component at index
     *          <code>size()&nbsp;-&nbsp;1</code>.
     * @exception  NoSuchElementException  if this vector is empty.
     * @since   JDK1.0
     */
    public synchronized Object lastElement() {
        return super.lastElement();
    }

    /**
     * Sets the component at the specified <code>index</code> of this
     * vector to be the specified object. The previous component at that
     * position is discarded.
     * <p>
     * The index must be a value greater than or equal to <code>0</code>
     * and less than the current size of the vector.
     *
     * @param      obj     what the component is to be set to.
     * @param      index   the specified index.
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid.
     * @see        java.util.Vector#size()
     * @since      JDK1.0
     */
    public synchronized void setElementAt(Object obj, int index) {
        super.setElementAt(obj, index);
    }

    /**
     * Deletes the component at the specified index. Each component in
     * this vector with an index greater or equal to the specified
     * <code>index</code> is shifted downward to have an index one
     * smaller than the value it had previously.
     * <p>
     * The index must be a value greater than or equal to <code>0</code>
     * and less than the current size of the vector.
     *
     * @param      index   the index of the object to remove.
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid.
     * @see        java.util.Vector#size()
     * @since      JDK1.0
     */
    public synchronized void removeElementAt(int index) {
        super.removeElementAt(index);
    }

    /**
     * Inserts the specified object as a component in this vector at the
     * specified <code>index</code>. Each component in this vector with
     * an index greater or equal to the specified <code>index</code> is
     * shifted upward to have an index one greater than the value it had
     * previously.
     * <p>
     * The index must be a value greater than or equal to <code>0</code>
     * and less than or equal to the current size of the vector.
     *
     * @param      obj     the component to insert.
     * @param      index   where to insert the new component.
     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid.
     * @see        java.util.Vector#size()
     * @since      JDK1.0
     */
    public synchronized void insertElementAt(Object obj, int index) {
        super.insertElementAt(obj, index);
    }

    /**
     * Adds the specified component to the end of this vector,
     * increasing its size by one. The capacity of this vector is
     * increased if its size becomes greater than its capacity.
     *
     * @param   obj   the component to be added.
     * @since   JDK1.0
     */
    public synchronized void addElement(Object obj) {
        super.addElement(obj);
    }

    /**
     * Removes the first occurrence of the argument from this vector. If
     * the object is found in this vector, each component in the vector
     * with an index greater or equal to the object's index is shifted
     * downward to have an index one smaller than the value it had previously.
     *
     * @param   obj   the component to be removed.
     * @return  <code>true</code> if the argument was a component of this
     *          vector; <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public synchronized boolean removeElement(Object obj) {
        return super.removeElement(obj);
    }

    /**
     * Removes all components from this vector and sets its size to zero.
     *
     * @since   JDK1.0
     */
    public synchronized void removeAllElements() {
        super.removeAllElements();
    }

    /**
     * Returns a string representation of this vector.
     *
     * @return  a string representation of this vector.
     * @since   JDK1.0
     */
    public synchronized String toString() {
        return super.toString();
    }
}

