//if[TRUSTED]
/* This will selectively exclude the entire file from the build */
package com.sun.squawk.csp;

/**
 * The exception class used when an error occurs in a CSP implementation.
 */
public class CSPException extends Exception {

    public CSPException() {
    }

    public CSPException(String msg) {
        super(msg);
    }
}
