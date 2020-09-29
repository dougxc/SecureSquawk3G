/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package java.util;

/**
 * Thrown by methods in the <code>Stack</code> class to indicate 
 * that the stack is empty. 
 *
 * @author  Jonathan Payne
 * @version 1.16, 12/04/99 (CLDC 1.0, Spring 2000)
 * @see     java.util.Stack
 * @since   JDK1.0
 */
public
class EmptyStackException extends RuntimeException {
    /**
     * Constructs a new <code>EmptyStackException</code> with <tt>null</tt> 
     * as its error message string.
     */
    public EmptyStackException() {
    }
}


