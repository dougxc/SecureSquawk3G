/*
 * @(#)RecordStoreNotFoundException.java	1.8 01/06/19
 * Copyright (c) 2000-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Copyright 2000 Motorola, Inc. All Rights Reserved.
 * This notice does not imply publication.
 */

package javax.microedition.rms; 

/** 
 * Thrown to indicate an operation could not be completed because the 
 * record store could not be found.
 * 
 * @version MIDP 1.0
 * @author Jim Van Peursem 
 */ 
public class RecordStoreNotFoundException 
    extends RecordStoreException 
{ 
    /** 
     * Constructs a new <code>RecordStoreNotFoundException</code> with the 
     * specified detail message. 
     * 
     * @param message the detail message. 
     */ 
    public RecordStoreNotFoundException(String message) {
	super(message);
    } 
    
    /** 
     * Constructs a new <code>RecordStoreNotFoundException</code> with no 
     * detail message. 
     */ 
    public RecordStoreNotFoundException() {
    } 
}
