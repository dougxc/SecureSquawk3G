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

package com.sun.squawk.csp.signature;

import com.sun.squawk.csp.key.PublicKey;
import com.sun.squawk.csp.NoSuchAlgorithmException;
import com.sun.squawk.csp.digest.MessageDigest;

import java.util.StringTokenizer;
import com.sun.squawk.util.Tracer;

/**
 * <p>This <code>Signature</code> class is used to provide the secure squawk class
 * loader the ability to verify digital signatures associated with trusted classes.
 *
 * <p>These digital signatures are used to validate the integrity of the supplied class file as
 * well as validate permissions granted to the class.
 *
 * <p>Similar to the Java Security architecture, <code>Signature</code> provides implementation-independent
 * algorithms, whereby a caller requests a particular signature algorithm and is handed back a properly
 * initialized <code>Signature</code> object.
 *
 * <p>The format of the supplied signature name follows the form {HASH}with{CIPHER}. So, for example, to verify
 * an MD5 hash with a signature using RSA encryption, the scheme would be MD5withRSA.
 *
 * <p>This implementation assumes that the application is responsible for calculating or supplying a valid hash
 * to verify the signatre with.
 *
 * <pre>
 * Usage :
 *               byte[] myData;          // data for which the signature has been written for
 *
 *               byte[] signatureData;   // raw signature data to be verified
 *               PublicKey myKey;        // public key associated with signatureData
 *
 *               Signature s = Signature.getInstance("MD5withRSA");
 *
 *               MessageDigest d = s.getDigest();
 *               byte[] expectedHash = d.digest(myData);    // calculate hash over data
 *
 *               s.verify(expectedHash,  signatureData, myKey);  // true if signature verified
 * </pre>
 *
 *
 * @author Andrew Crouch
 * @see {@link ClassFileLoader}
 */
public abstract class Signature {

    /**
     * The name of the cipher used in this signature.
     */
    private String cipher;

    /**
     * The name of the hashing algorithm used in this signature.
     */
    private String hash;


    /**
     * Creates a Signature object using the specified cipher.
     * @param cipher String  the name of the cipher used in this signature.
     */
    protected Signature(String cipher) {
        this.cipher = cipher;
    }

    /**
     * Get a new instance of the digest algorithm used in this signature.
     *
     * @throws NoSuchAlgorithmException if the message digest algorithm is unavailable.
     * @return MessageDigest a new instance of the message digest used in this signature.
     */
    public final MessageDigest getDigest() throws NoSuchAlgorithmException{
        return MessageDigest.getInstance(this.hash);
    }

    /**
     * Generates a Signature object that implements the specified digest
     * algorithm.
     *
     * @return the new Signature object.
     *
     * @exception NoSuchAlgorithmException if the algorithm is not available in the environment.
     */
    public static Signature getInstance(String algorithm) throws NoSuchAlgorithmException {

        /* Of the form {HASH}with{CIPHER} */
        StringTokenizer st = new StringTokenizer(algorithm, "with");

        if (st.countTokens() != 2) {
            throw new NoSuchAlgorithmException("format of algorithm unknown");
        }

        String hash = st.nextToken();
        String signer = st.nextToken();

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" Using signature algorithm => hash: " + hash + ", cipher: " + signer);
        }


        try {
            /* Looking for say, RSA, will be in RSASignature class */
            Class c = Class.forName("com.sun.squawk.csp.signature." + signer.toUpperCase() + "Signature");
            Object o = c.newInstance();

            if (o instanceof Signature) {
                if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                    Tracer.traceln(" Setting " + signer + "Signature to use " + hash);
                }

                /* Attempt to load the hash. If this fails, the signature type is invalid */
                MessageDigest.getInstance(hash);

                /* Set hash algorithm */
                ((Signature)o).hash = hash;

                /* Return the completed signature */
                return (Signature)o;
            }

            throw new NoSuchAlgorithmException("Algorithm not available");

        } catch (ClassNotFoundException cnfe) {
            throw new NoSuchAlgorithmException("Algorithm not available - " + cnfe.getMessage());
        } catch (InstantiationException ie) {
            throw new NoSuchAlgorithmException("Algorithm not available - " + ie.getMessage());
        } catch (IllegalAccessException iae) {
            throw new NoSuchAlgorithmException("Algorithm not available - " + iae.getMessage());
        }
    }


   /**
     * Verifies the passed-in signature using the supplied key against the expected hash.
     *
     * @param signature byte[]
     * @param expectedHash byte[]
     * @param publicKey PublicKey
     * @throws SignatureException
     * @return true if the signature was verified, false if not.
     */
    public abstract boolean verify(byte[] signature, byte[] expectedHash, PublicKey publicKey) throws SignatureException;


    /**
     * Returns the name of the algorithm for this signature object.
     *
     * @return the name of the algorithm for this signature object.
     */
    public final String getAlgorithm() {
        return this.hash + "with" + this.cipher;
    }

    /**
     * Returns a string representation of this signature object,
     * providing information that includes the state of the object
     * and the name of the algorithm used.
     *
     * @return a string representation of this signature object.
     */
    public String toString() {
        return "Signature object: " + getAlgorithm();
    }

}
