package svmtools.csp;

import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import svmtools.CSP;
import svmtools.CSPException;

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
 */
public class MD5RSABasic extends CSP {

    /**
     * Creates a CSP for the given CSP identifier.
     */
    public MD5RSABasic() throws NoSuchAlgorithmException {
        super("MD5RSABasic", Signature.getInstance("MD5withRSA"));
    }

    /**
     * Confirm that a given type of public key is supported.
     */
    public boolean isKeyTypeSupported(Key key, boolean isPublic) {
        if (key == null)
            return false;
        if (isPublic)
            return (key instanceof RSAPublicKey);
        else
            return (key instanceof RSAPrivateKey);
    }

    /**
     * This method decodes a signature from a DataInputStream.
     * @param in the DataInputStream holding an encoded signature
     * @return the signature's bytes
     * @exception CSP if the reading of the input stream
     * fails or the signature is improperly encoded.
     */
    public byte[] decodeSignature(DataInputStream in) throws CSPException
    {
        try {
            int size = in.readInt();
            byte sig[] = new byte[size];
            in.read(sig);
            return sig;
        } catch (IOException ioe) {
            throw new CSPException("signature decoding failed: " +
                ioe.getMessage());
        }
    }

    /**
     * Encode a public key. This is how the key will be encoded and placed
     * into a trusted classfile.
     * @param key the PublicKey to be encoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public byte[] encodePublicKey(PublicKey key)
        throws CSPException {

        if (!(key instanceof RSAPublicKey))
            throw new CSPException("unsupported key type: " +
                key.getClass().getName());

        RSAPublicKey pk = (RSAPublicKey)key;
        BigInteger mod = pk.getModulus();
        BigInteger exp = pk.getPublicExponent();
        int modBitLength = mod.bitLength();
        int expBitLength = exp.bitLength();
        byte modBytes[] = mod.toByteArray();
        byte expBytes[] = exp.toByteArray();

        try {
            ByteArrayOutputStream encoding = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(encoding);
            out.writeInt(modBitLength);
            out.writeInt(expBitLength);
            // We only want the absolute form of the big int - not the extra sign bit
            if (modBitLength % 8 != 0)
                throw new CSPException("RSA modulus size is not a multiple of 8");
            int absByteLength = modBitLength / 8;
            out.write(modBytes,modBytes.length - absByteLength,absByteLength);
            out.write(expBytes);
            out.flush();
            out.close();
            return encoding.toByteArray();
        } catch (IOException ioe) {
            throw new CSPException("Exception during public key encoding: "+
                ioe.getMessage());
        }
    }

    /**
     * Decode a public key. Decode a key from a trusted classfile.
     * @param key the raw public key bytes to be decoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public PublicKey decodePublicKey(byte[] key)
        throws CSPException {

        int modBitLength;
        int expBitLength;
        try {
            ByteArrayInputStream encoding = new ByteArrayInputStream(key);
            DataInputStream in = new DataInputStream(encoding);
            modBitLength = in.readInt();
            expBitLength = in.readInt();
            if (modBitLength % 8 != 0)
                throw new CSPException("RSA modulus size is not a multiple of 8");
            byte modBytes[] = new byte[modBitLength/8 + 1];
            byte expBytes[] = new byte[(expBitLength + 7)/8];
            in.read(modBytes,1,modBytes.length - 1);
            in.read(expBytes);
            if (in.available() != 0)
                throw new CSPException("badly formed RSA key");
            in.close();

            BigInteger mod = new BigInteger(modBytes);
            BigInteger exp = new BigInteger(expBytes);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(mod,exp);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ioe) {
            throw new CSPException("Exception during public key encoding: "+
                ioe.getMessage());
        }
    }

    /**
     * Encode a private key.
     * @param key the PrivateKey to be encoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public byte[] encodePrivateKey(PrivateKey key)
        throws CSPException {

        if (!(key instanceof RSAPrivateKey))
            throw new CSPException("unsupported key type: " +
                key.getClass().getName());

        try {
            PKCS8EncodedKeySpec spec = (PKCS8EncodedKeySpec)
                KeyFactory.getInstance("RSA").getKeySpec(key,PKCS8EncodedKeySpec.class);
            return spec.getEncoded();
        } catch (NoSuchAlgorithmException nsae) {
            throw new CSPException("Exception during private key encoding: "+
                nsae.getMessage());
        } catch (InvalidKeySpecException ikse) {
            throw new CSPException("Exception during private key encoding: "+
                ikse.getMessage());
        }
    }

    /**
     * Decode a private.
     * @param key the raw private key bytes to be decoded
     * @exception CSPException if the key type does not match the
     * key type supported by this CSP.
     */
    public PrivateKey decodePrivateKey(byte[] key)
        throws CSPException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (NoSuchAlgorithmException nsae) {
            throw new CSPException("Exception during private key decoding: "+
                nsae.getMessage());
        } catch (InvalidKeySpecException ikse) {
            throw new CSPException("Exception during private key decoding: "+
                ikse.getMessage());
        }
    }
}
