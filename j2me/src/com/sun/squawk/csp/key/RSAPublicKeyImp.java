//if[TRUSTED]
/* This will selectively exclude the entire file from the build */
/*
 * Copyright 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.csp.key;


import com.sun.squawk.csp.math.BigInteger;
import com.sun.squawk.util.Tracer;

/**
 * This class implements a public RSA key. However, it does not support
 * arbitrary encoding formats.  A manual implementation of the encoding
 * format is used.
 *
 * @author Andrew Crouch
 */
public class RSAPublicKeyImp implements RSAPublicKey {


    /**
     * The algorithm associated with this key
     */
    public static final String ALGORITHM = "RSA";

    /**
     * The public exponent of this RSA public key
     */
    private BigInteger publicExponent;

    /**
     * The modulus of this RSA public key
     */
    private BigInteger modulus;


    /**
     * Construct from given values.
     *
     * @param mod      modulus of the <code>RSAPublicKey</code>
     * @param exp      the public exponent of the <code>RSAPublicKey</code>
     */
    public RSAPublicKeyImp(BigInteger mod, BigInteger exp) {
        this.modulus = mod;
        this.publicExponent = exp;
    }

    /**
     * {@inheritDoc}
     *
     */
    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }


    /**
     * {@inheritDoc}
     *
     */
    public BigInteger getModulus() {
        return this.modulus;
    }

    /**
     * {@inheritDoc}
     *
     */
    public String getAlgorithm() {
        return ALGORITHM;
    }

    /**
     * {@inheritDoc}
     *
     */
    public String getFormat() {
        /* Not supporting arbitrary encoding */
        return null;
    }

    /**
     * {@inheritDoc}
     *
     */
    public byte[] getEncoded() {
        /* Not supporting arbitrary encoding */
        return null;
    }



    /**
     * {@inheritDoc}
     *
     */
    public String toString () {
        String s ="";

        if (this.modulus != null) {
            s = this.modulus.toString (16);
        }

        if (this.publicExponent != null) {
            s += this.publicExponent.toString (16);
        }

        return s;
    }

    /**
     * {@inheritDoc}
     *
     */
    public boolean equals(Object o) {

        /* Pointer comparison */
        if(o == this) {
            return true;
        }

        /* Will also check if o is null */
        if(!(o instanceof RSAPublicKey)) {
            return false;
        }

        RSAPublicKey rsaKey = (RSAPublicKey)o;

        /* The keys are equal if exponent and modulus are equal */
        BigInteger thisExponent   = getPublicExponent();
        BigInteger theirExponent  = rsaKey.getPublicExponent();
        BigInteger thisModulus    = getModulus();
        BigInteger theirModulus   = rsaKey.getModulus();

        /**
         * Nothing can be null. If something is null, the key is invalid, and hence an
         * invalid key can never equal a valid key.
         */
        if(thisExponent == null || theirExponent == null || thisModulus == null || theirModulus == null) {
            return false;
        }

        /**
         * Perform the comparison.
         */
        if(thisExponent.equals(theirExponent) && thisModulus.equals(theirModulus)) {
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     */
    public int hashCode() {

        /* Magic number */
        int result = 17;

        BigInteger mod = getModulus();
        BigInteger exp = getPublicExponent();

        if(mod != null) {
            result = result*37 + mod.hashCode();
        }

        if(exp != null) {
            result = result*37 + exp.hashCode();
        }

        return result;
    }
}
