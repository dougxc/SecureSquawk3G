/*
 * Copyright 1995-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

/**
 * Thrown to indicate that an attempt has been made to store a
 * pointer from persistent memory into non-persistent memory.
 */
public
class IllegalStoreException extends RuntimeException {
    /**
     * Constructs an <code>IllegalStoreException</code> with no detail message.
     */
    public IllegalStoreException() {
    super();
    }

    /**
     * Constructs an <code>IllegalStoreException</code> with the specified
     * detail message.
     *
     * @param   s   the detail message.
     */
    public IllegalStoreException(String s) {
    super(s);
    }
}


