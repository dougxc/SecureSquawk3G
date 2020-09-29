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
import com.sun.squawk.csp.NoSuchAlgorithmException;
import com.sun.squawk.csp.key.PublicKey;
import com.sun.squawk.csp.key.RSAPublicKeyImp;

import com.sun.squawk.csp.math.BigInteger;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;

import com.sun.squawk.util.Tracer;
import com.sun.squawk.util.Hashtable;

/**
 * This class implements a CSP that provides MD5withRSA signatures.
 * The RSA public keys are encoded as follows:
 *
 *    {
 *        int   modulusSizeInBits,    // big-endian
 *        int   exponentSizeInBits,   // big-endian
 *        byte  modulus[],            // length = (modulusSizeInBits+7)/8
 *        byte  exponent[]            // length = (exponentSizeInBits+7)/8
 *    }
 *
 * The signatures are encoded as follows:
 *
 *    {
 *        byte  signature[] // raw signature data
 *    }
 *
 *  @author Doug Simon
 *  @author Andrew Crouch
 */
public class MD5RSABasic extends CSP {

    /**
     * Used to intern keys so that pointer equality can be used.
     */
    private Hashtable keyTable;

    /**
     * Creates a CSP for the given CSP identifier.
     */
    public MD5RSABasic() throws NoSuchAlgorithmException {
        super("MD5RSABasic", Signature.getInstance("MD5withRSA"));
        keyTable = new Hashtable();
    }

    /**
     * Decode a public key. Decode a key from a trusted classfile.
     *
     * @param key the raw public key bytes to be decoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public PublicKey decodePublicKey(byte[] encodedKey) throws CSPException {

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" decoding public key");
        }

        int modulusBitLength;
        int exponentBitLength;
        RSAPublicKeyImp pk;
        try {
            ByteArrayInputStream encoding = new ByteArrayInputStream(encodedKey);
            DataInputStream in = new DataInputStream(encoding);

            modulusBitLength = in.readInt();
            exponentBitLength = in.readInt();

            if (modulusBitLength % 8 != 0) {
                throw new CSPException ("RSA modulus size is not a multiple of 8");
            }

            byte modulusBytes[] = new byte[modulusBitLength / 8 + 1];
            byte exponentBytes[] = new byte[ (exponentBitLength + 7) / 8];

            in.read(modulusBytes, 1, modulusBytes.length - 1);
            in.read(exponentBytes);

            if (in.available() != 0) {
                throw new CSPException ("badly formed RSA key");
            }

            in.close();

            BigInteger modulus = new BigInteger(modulusBytes);
            BigInteger exponent = new BigInteger(exponentBytes);

            pk = new RSAPublicKeyImp(modulus, exponent);

            /**
             * Intern the key.  If the key represented by the above values
             * already exists, return a pointer to that instance so we can perform
             * pointer equality. If one doesn't exist, add it to the collection of
             * keys available.
             */
            RSAPublicKeyImp existing = (RSAPublicKeyImp)keyTable.get(pk);
            if(existing == null) {
                keyTable.put(pk, pk);
                existing = pk;
            }

            return existing;

        } catch (Exception ioe) {
            throw new CSPException("Exception during public key decoding: " + ioe.getMessage());
        }
    }
}
