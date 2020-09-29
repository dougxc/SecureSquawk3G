package svmtools;

import java.security.Signature;
import java.security.SignatureException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.InvalidKeyException;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * This class represents the interface to the Cryptographic Service
 * Provider (CSP) for the Wobulator. The CSP provides the Wobulator the
 * functionality of a digital signature algorithm and a key encoding format.
 */
public abstract class CSP {

    /**
     * This is the identifier of the CSP.
     */
    private final String identifier;

    /**
     * The Signature object used for signing/verifying digital signatures.
     */
    private final Signature signature;

    /**
     * Creates a CSP for the given CSP identifier.
     */
    protected CSP (String identifier, Signature signature) {
        this.identifier = identifier;
        this.signature = signature;
    }

    /**
     * Generates a CSP object that implements a specific signature
     * algorithm and provides specific encodings.
     * @param identifier Identifys a specific implementation class.
     * @return an instance of the class denoted by identifier or null if no
     * such class is available.
     */
    public static CSP getInstance(String clazz) {
        try {
            Class c = Class.forName(clazz);
            CSP csp = (CSP)c.newInstance();
            return csp;
        }
        catch (ClassNotFoundException cnfe) {
            return null;
        }
        catch (InstantiationException ie) {
            return null;
        }
        catch (IllegalAccessException iae) {
            return null;
        }
    }

    /**
     * Return the identifier.
     */
    public final String getIdentifier() {
        return identifier;
    }

    /**
     * Confirm that a given type of public key is supported.
     */
    public abstract boolean isKeyTypeSupported(Key key, boolean isPublic);

    /**
     * This method decodes a signature from a DataInputStream.
     * @param in the DataInputStream holding an encoded signature
     * @return the signature's bytes
     * @exception CSPException if the reading of the input stream
     * fails or the signature is improperly encoded.
     */
    public abstract byte[] decodeSignature(DataInputStream in)
        throws CSPException;

    /**
     * Compute the signature on a given array of bytes with a given private
     * key and return the result.
     * @param data the content to be signed
     * @param key the private key to be used for signing
     * @exception CSPException if the key type does not match the key
     * type supported by this CSP for signing or
     */
    public byte[] sign(byte[] data, PrivateKey key) throws CSPException {
        if (key == null)
            throw new CSPException("signing key cannot be null");
        if (data == null)
            throw new CSPException("cannot sign null message");
        try {
            signature.initSign(key);
            signature.update(data);
            return signature.sign();
        }
        catch (InvalidKeyException ike) {
            throw new CSPException ("invalid key");
        }
        catch (SignatureException se) {
            // this should *never* happen
        }
        return null;
    }

    /**
     * Verify a passed-in signature.
     * @param data The data that was signed to produce sig.
     * @param sig The signature to be verified.
     * @param key The public key used for verification.
     * @return true if verification succeeds, false otherwise.
     * @exception CSPException if the key type does not match the key type
     * supported by this CSP for verification.
     */
    public boolean verify(byte[] data, byte[] sig, PublicKey key) throws
        CSPException
    {
        try {
            signature.initVerify(key);
            signature.update(data);
            return signature.verify(sig);
        }
        catch (InvalidKeyException ike) {
            throw new CSPException ("invalid key");
        }
        catch (SignatureException se) {
            throw new CSPException(se.getMessage());
        }
    }

    /**
     * Encode a public key. This is how the key will be encoded and placed
     * into a trusted classfile.
     * @param key the PublicKey to be encoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public abstract byte[] encodePublicKey(PublicKey key)
        throws CSPException;

    /**
     * Decode a public key. Decode a key from a trusted classfile.
     * @param key the raw public key bytes to be decoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public abstract PublicKey decodePublicKey(byte key[])
        throws CSPException;

    /**
     * Encode a private key.
     * @param key the PrivateKey to be encoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public abstract byte[] encodePrivateKey(PrivateKey key)
        throws CSPException;

    /**
     * Decode a private key.
     * @param key the raw private key bytes to be decoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public abstract PrivateKey decodePrivateKey(byte key[])
        throws CSPException;
}
