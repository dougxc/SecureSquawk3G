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
 */
package com.sun.squawk.csp;

import com.sun.squawk.csp.key.PublicKey;

/**
 * A Domain encapsulates a public key and a digital signature
 * (both with their contents directly inlined) which represent a class's
 * membership in a domain.
 *
 * The signature is a hash of the original class
 * file including the trusted attribute, except for domains.
 *
 * @author Doug Simon
 * @author Andrew Crouch
 */
public class Domain {

    /**
     * The <code>PublicKey</code> used to verify the supplied signature
     */
    private PublicKey key;

    /**
     * The raw signature of the plain class file, with trusted attribute, but without
     * domain entries.
     */
    private byte[] signature;


    /**
     * Construct from given values.
     *
     * @param key
     * @param signature
     */
    public Domain(PublicKey key, byte[] signature) {
        this.key = key;
        this.signature = signature;
    }

    /**
     * Get the public key used to verify this domain entry
     *
     * @return PublicKey  the <code>PublicKey</code> that is to be used
     * to verify this domain entry.
     */
    public PublicKey getKey() {
        return key;
    }

    /**
     * Get the raw signature of this domain.
     *
     * @return byte[]  the raw signature data for this domain
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Convert to a String.
     *
     * @return String    the string represenation of this domain
     */
    public String toString() {
        String s = "\tkey=";
        s += key.toString();
        s += "\n\tsignature=";
        s += signature.toString();
        return s;
    }

}
