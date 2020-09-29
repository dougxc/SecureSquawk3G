/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

/**
 * This interface is implemented to provide context to LinkageError messages.
 *
 * @author  Doug Simon
 */
public interface Context {

    /**
     * Adds a class file context prefix to a given string.
     *
     * @param    msg  the string to be prefixed with a context message
     * @return   the prefixed message
     */
    public String prefix(String msg);
}