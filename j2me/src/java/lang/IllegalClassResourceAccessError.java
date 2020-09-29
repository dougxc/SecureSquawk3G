//if[TRUSTED]
/* This will selectively exclude the entire file from the build */
/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: $
 */
package java.lang;


public class IllegalClassResourceAccessError extends SecurityError {

    public IllegalClassResourceAccessError() {
        super();
    }

    public IllegalClassResourceAccessError(String msg) {
        super(msg);
    }
}
