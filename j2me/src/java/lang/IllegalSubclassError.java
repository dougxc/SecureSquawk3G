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


public class IllegalSubclassError extends SecurityError {
    public IllegalSubclassError() {
        super();
    }

    public IllegalSubclassError(String msg) {
        super(msg);
    }
}
