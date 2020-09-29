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

/**
 * A permit encapsulates a class and digital signature pair.  The class associated
 * with this permit is a reference to the class that granted the privilege this
 * <code>Permit</code> represents.  The signature is a signed hash of the class file
 * (without the "Trusted" attribute) of the class that holds the permit.
 *
 * @author Doug Simon
 * @author Andrew Crouch
 */
public class Permit {

    /**
     * The klass who has granted this permit.
     */
    private Klass klass;

    /**
     * The raw signature data.  The data used to generate the signature
     * is the grantee's classfile before the "Trusted" attribute has been added.
     */
    private byte[] signature;


    /**
     * Construct from given values.
     *
     * @param k      The TrustedKlass who has granted this permit.
     * @param s      The raw signature data associated with this clas who holds
     * this permit
     */
    public Permit(Klass k, byte[] s) {
        this.klass = k;
        this.signature = s;
    }

    /**
     * Return the class object of this Permit.
     *
     * @return the class object of this Permit.
     */
    public Klass getKlass() {
        return klass;
    }

    /**
     * Return the digital signature of this Permit.
     *
     * @return the digital signature of this Permit.
     */
    public byte[] getSignature() {
        return signature;
    }
}
