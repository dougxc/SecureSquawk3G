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
import com.sun.squawk.csp.key.RSAPublicKey;
import com.sun.squawk.csp.digest.MessageDigest;
import com.sun.squawk.csp.NoSuchAlgorithmException;

import com.sun.squawk.util.Assert;
import com.sun.squawk.util.Tracer;

import com.sun.squawk.csp.math.BigInteger;

/**
 * <p>The <code>RSASignature</code> class implements a limited implementation
 * of an RSA signature.  It is minimal in that it only provides the functionality
 * for verifying an RSA signature.
 *
 * <p>The <code>Signature</code> class allows for pluggable hash algorithms and as
 * such the <code>RSASignature</code> is designed to use any hash algorithm conforming
 * to the <code>MessageDigest</code> interface.
 *
 * <p><code>RSASignature</code> assumes PCKS #1 Encoding.
 *
 * @author Andrew Crouch
 * @see {@link MessageDigest}
 */
public class RSASignature extends Signature {

    public RSASignature() {
        super("RSA");
    }


    /**
     * Used to format a byte array for when tracing is used.  Returns a string
     * whos output is the hex representation of the supplied byte array with
     * <code>entriesPerLine</code> bytes per line.
     *
     * @param b                byte array to format
     * @param entriesPerLine   number of bytes to output per line
     * @return String          the formatted hex representation of the byte array
     */
    private String dumpArrayData(byte[] b, int entriesPerLine) {
        String returnString = "";
        for(int i = 0; i < b.length; i++) {
            if(i % entriesPerLine == 0) {
                returnString += "\n";
            }

            String s = Integer.toHexString(b[i]&0xFF).toUpperCase();
            String t = s;
            if(s.length() == 1) {
                t = "0" + s;
            }
            returnString += t + " ";
        }
        returnString += "\n";

        return returnString;
    }


    /**
     * Return the number of bytes required to store the magnitude byte[] of
     * this BigInteger. Do not count a 0x00 byte toByteArray() would
     * prefix for 2's complement form.
     *
     * This method is from sun.security.rsa.RSACore
     *
     * @param b      the BigInteger whos byte length is to be calculated
     * @return int   the minimal number of bytes to store the magnitude byte[] of b
     * @see sun.security.rsa.RSACore
     */
    private static int getByteLength(BigInteger b) {
        int n = b.bitLength();
        return (n + 7) >> 3;
    }

    /**
     * Return the encoding of this BigInteger that is exactly len bytes long.
     * Prefix/strip off leading 0x00 bytes if necessary.
     * Precondition: bi must fit into len bytes.
     *
     * This method is from sun.security.rsa.RSACore
     *
     * @param bi      the BigInteger that is to be converted to a byte[]
     * @param len     the number of bytes the encoded BigInteger is expected to be
     * @return byte[] the byte[] representation of the BigInteger
     * @see sun.security.rsa.RSACore
     */
    private static byte[] toByteArray(BigInteger bi, int len) {
        byte[] b = bi.toByteArray();
        int n = b.length;
        if (n == len) {
            return b;
        }
        // BigInteger prefixed a 0x00 byte for 2's complement form, remove it
        if ( (n == len + 1) && (b[0] == 0)) {
            byte[] t = new byte[len];
            System.arraycopy(b, 1, t, 0, len);
            return t;
        }
        // must be smaller
        Assert.that(n < len);
        byte[] t = new byte[len];
        System.arraycopy(b, 0, t, (len - n), n);
        return t;
    }



    /**
     * {@inheritDoc}
     *
     */
    public final boolean verify(byte[] sigBytes, byte[] expectedHash, PublicKey suppliedPublicKey) throws SignatureException {

        /* Check the key is okay */
        if(suppliedPublicKey == null) {
            throw new SignatureException("signature verify parameters must not be null");
        }

        if(!(suppliedPublicKey instanceof RSAPublicKey)){
            throw new SignatureException("public key is invalid type");
        }


        /* Check that the hash okay */
        if(expectedHash == null) {
            throw new SignatureException("Hash cannot be null");
        }

        MessageDigest md;
        try {
            md = getDigest();
        }
        catch (NoSuchAlgorithmException ex) {
            throw new SignatureException("Could not load hash algorithm: " + ex.getMessage());
        }

        if(expectedHash.length != md.getDigestLength()) {
            throw new SignatureException("Expected hash is an invalid length");
        }

        RSAPublicKey rsaPublicKey = (RSAPublicKey)suppliedPublicKey;

        if(rsaPublicKey.getModulus() == null || rsaPublicKey.getPublicExponent() == null ) {
             throw new SignatureException("Invalid public key");
        }

        /*
         * Decrypt supplied signature into hash
         */
        BigInteger encryptedHash = new BigInteger(1, sigBytes);

        /* Check that message is smaller than modulus */
        if (encryptedHash.compareTo(rsaPublicKey.getModulus()) >= 0) {
            throw new SignatureException("Message is larger than modulus");
        }

        /* Perform RSA decrypt operation */
        BigInteger encodedHash = encryptedHash.modPow(rsaPublicKey.getPublicExponent(), rsaPublicKey.getModulus());

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" Signature for verification  : " + dumpArrayData(sigBytes, 16));
        }

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" Expected hash : " + dumpArrayData(expectedHash, 16));
        }

        /* Decode the hash contained within signature */
        byte[] decodedHash = decodeHash(encodedHash, rsaPublicKey, md);

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" Hash contained in supplied signature : " + dumpArrayData(decodedHash, 16));
        }

        /* Compare the digests for equality */
        return MessageDigest.isEqual(expectedHash, decodedHash);
    }

    /**
     * Checks that the decrypted signature is in the PCKS #1 format and extracts the hash contained
     * within.  Checks that the hash obtained conforms to the supplied <code>MessageDigest</code>
     * algorithm.
     *
     * @param encodedHash   a BigInteger representing the decrypted signature
     * @param rsaPublicKey  the key associated with the signature being verified
     * @param md            an instance of the expected <code>MessageDigest</code> algorithm
     * @throws SignatureException if the signature is not encoded as expected
     * @return byte[]       the decoded hash
     */
    private byte[] decodeHash (BigInteger encodedHash, RSAPublicKey rsaPublicKey, MessageDigest md) throws SignatureException {

        /* Parameters already checked for consistency */

        /* Do some minor conversions */
        byte[] fixedEncodedHash = toByteArray(encodedHash, getByteLength(rsaPublicKey.getModulus()));

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" Decrypted Signature : " + dumpArrayData(fixedEncodedHash, 16));
        }

        /* Check format is PKCS #1 */
        int i = 0;

        if(fixedEncodedHash[0] != 0 || fixedEncodedHash[1] != 1) {
            throw new SignatureException("invalid encoding scheme (start bytes invalid)");
        }

        for(i = 2; i < rsaPublicKey.getModulus().toByteArray().length - 1; i++) {
            if(fixedEncodedHash[i] != (byte)0xFF) {
                break;
            }
        }

        if(fixedEncodedHash[i++] != 0) {
            throw new SignatureException("invalid encoding scheme");
        }

        byte[] digestAlgorithmID = md.getAlgorithmID();

        for(int j = 0; j < digestAlgorithmID.length; j++) {
            if(fixedEncodedHash[i++] != digestAlgorithmID[j]) {
                throw new SignatureException("mismatched digest info");
            }
        }

        byte[] hash = new byte[md.getDigestLength()];

        if((fixedEncodedHash.length - i) != md.getDigestLength()) {
            throw new SignatureException("invalid hash length");
        }

        System.arraycopy(fixedEncodedHash, i, hash, 0, hash.length);

        return hash;
    }
}
