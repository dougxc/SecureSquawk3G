//if[TRUSTED]
/* This will selectively exclude the entire file from the build */
/*
 * @(#)DigestException.java	1.17 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.squawk.csp.digest;

/**
 * This is the generic Message Digest exception.
 *
 * @version 1.17, 03/12/19
 * @author Benjamin Renaud
 */
public class DigestException extends Exception {

    /**
     * Constructs a DigestException with no detail message.  (A
     * detail message is a String that describes this particular
     * exception.)
     */
    public DigestException() {
        super();
    }

    /**
     * Constructs a DigestException with the specified detail
     * message.  (A detail message is a String that describes this
     * particular exception.)
     *
     * @param msg the detail message.
     */
    public DigestException(String msg) {
        super(msg);
    }

}
