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

import com.sun.squawk.csp.signature.Signature;
import com.sun.squawk.csp.signature.SignatureException;
import com.sun.squawk.csp.key.PublicKey;
import com.sun.squawk.csp.digest.MessageDigest;

import com.sun.squawk.util.Tracer;


/**
 * This class represents the interface to the Cryptographic Service
 * Provider (CSP) for the secure version of the Squawk JVM.
 *
 * The CSP is used primarily to verify digital signatures associated with Trusted
 * class files. As such the CSP provides mechanisms to obtain the
 * <code>MessageDigest</code>'s used to construct the signature as well as
 * the <code>PublicKey</code>'s to verify them.
 *
 * @author Doug Simon
 * @author Andrew Crouch
 */
public abstract class CSP {

    /**
     * This is the identifier of the CSP.
     */
    private final String identifier;

    /**
     * The Signature object used for verifying digital signatures.
     */
    private final Signature signature;

    /**
     * Creates a CSP for the given CSP identifier.
     */
    protected CSP(String identifier, Signature signature) {
        this.identifier = identifier;
        this.signature = signature;
    }

    /**
     * The global CSP for the secure Squawk JVM
     */
    private final static CSP instance;

    /**
     * Initialise the global CSP instance
     */
    static {
        CSP csp = null;
        String className = System.getProperty ("csp.provider.class");

        if (className == null) {
            className = "com.sun.squawk.csp.MD5RSABasic";
        }

        if (Klass.DEBUG && Tracer.isTracing ("svmload")) {
            Tracer.traceln ("  Attempting to load the CSP : " + className);
        }

        try {

            Class c = Class.forName (className);
            csp = (CSP) c.newInstance ();

        } catch (Exception cnfe) {
            throw new RuntimeException("CSP provider not found: " + cnfe);
        }

        instance = csp;
    }

    /**
     * Generates a CSP object that implements a specific signature
     * algorithm and provides specific encodings.
     *
     * @param identifier Identifys a specific implementation class.
     * @return an instance of the class denoted by identifier or null if no
     * such class is available.
     */
    public static CSP getInstance () {
        return instance;
    }

    /**
     * Return the identifier.
     */
    public final String getIdentifier() {
        return identifier;
    }


    /**
     * Verify a passed-in signature.
     * @param hash        The hash of the data that was signed to produce sig.
     * @param sig         The signature to be verified.
     * @param key         The public key used for verification.
     * @return true if verification succeeds, false otherwise.
     * @exception CSPException if the key type does not match the key type
     * supported by this CSP for verification.
     */
    public boolean verifyHash(byte[] hash, byte[] sig, PublicKey key) throws CSPException {

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" Verifying supplied hash, with supplied signature and key");
        }

        if (signature == null) {
            throw new CSPException(" CSP signature object cannot be null");
        }

        if (hash == null || sig == null || key == null) {
            throw new CSPException(" Invalid parameter(s) to verifyHash()");
        }

        try {
            return signature.verify(sig, hash, key);
        }
        catch (SignatureException se) {
            throw new CSPException(se.getMessage());
        }
    }

    /**
     * Obtain the <code>MessageDigest</code> associated with the <code>Signature</code> algorithm used
     * in this CSP.
     *
     * @throws CSPException   if the <code>Signature</code> associated with this CSP is invalid,
     * or if the <code>MessageDigest</code> is not available in the environment.
     * @return MessageDigest  a new instance of the <code>MessageDigest</code> used in the current
     * <code>Signature</code> scheme
     */
    public MessageDigest getDigest() throws CSPException {

        if( signature == null ) {
            throw new CSPException(" CSP Signature object cannot be null");
        }

        try {
            return signature.getDigest();
        }
        catch (NoSuchAlgorithmException ex) {
            throw new CSPException(ex.getMessage());
        }
    }

    /**
     * Decode a public key. Decode a key from a trusted classfile.
     *
     * @param key the raw public key bytes to be decoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public abstract PublicKey decodePublicKey(byte key[]) throws CSPException;
}
