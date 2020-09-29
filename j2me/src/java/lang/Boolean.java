/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

/**
 * The Boolean class wraps a value of the primitive type
 * <code>boolean</code> in an object. An object of type
 * <code>Boolean</code> contains a single field whose type is
 * <code>boolean</code>.
 *
 * @author  Arthur van Hoff
 * @version 1.37, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0
 */
public final
class Boolean {

    /**
     * The value of the Boolean.
     *
     * @serial
     */
    private boolean value;

    /**
     * Allocates a <code>Boolean</code> object representing the
     * <code>value</code> argument.
     *
     * @param   value   the value of the <code>Boolean</code>.
     */
    public Boolean(boolean value) {
        this.value = value;
    }

    /**
     * Returns the value of this <tt>Boolean</tt> object as a boolean
     * primitive.
     *
     * @return  the primitive <code>boolean</code> value of this object.
     */
    public boolean booleanValue() {
        return value;
    }

    /**
     * Returns a String object representing this Boolean's value.
     * If this object represents the value <code>true</code>, a string equal
     * to <code>"true"</code> is returned. Otherwise, a string equal to
     * <code>"false"</code> is returned.
     *
     * @return  a string representation of this object.
     */
    public String toString() {
      return value ? "true" : "false";
    }

    /**
     * Returns a hash code for this <tt>Boolean</tt> object.
     *
     * @return  the integer <tt>1231</tt> if this object represents
     * <tt>true</tt>; returns the integer <tt>1237</tt> if this
     * object represents <tt>false</tt>.
     */
    public int hashCode() {
        return value ? 1231 : 1237;
    }

    /**
     * Returns <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>Boolean </code>object that
     * represents the same <code>boolean</code> value as this object.
     *
     * @param   obj   the object to compare with.
     * @return  <code>true</code> if the Boolean objects represent the
     *          same value; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Boolean) {
            return value == ((Boolean)obj).booleanValue();
        }
        return false;
    }

}


