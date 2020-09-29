package svmtools.csp;

import java.security.*;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAParams;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import svmtools.CSP;
import svmtools.CSPException;

/**
 * This class implements a CSP that provides SHAwithDSA signatures.
 * The DSA public keys are encoded as follows:
 *
 *    {
 *        int   baseSizeInBits,      // big-endian
 *        int   primeSizeInBits,     // big-endian
 *        int   subprimeSizeInBits,  // big-endian
 *        int   publickeySizeInBits, // big-endian
 *        byte  base[],              // length = (baseSizeInBits+7)/8
 *        byte  prime[]              // length = (primeSizeInBits+7)/8
 *        byte  subprime[]           // length = (subprimeSizeInBits+7)/8
 *        byte  publickey[]          // length = (publickeySizeInBits+7)/8
 *    }
 *
 * The signatures are encoded as follows:
 *
 *    {
 *        byte  signature[] // raw signature data
 *    }
 */
public class SHADSABasic extends CSP {

    // Order in which DSA key components are serialized.
    private static final int G = 0;
    private static final int P = 1;
    private static final int Q = 2;
    private static final int Y = 3;

    /**
     * Creates a CSP for the given CSP identifier.
     */
    public SHADSABasic() throws NoSuchAlgorithmException {
        super("SHADSABasic", Signature.getInstance("SHAwithDSA"));
    }

    /**
     * Confirm that a given type of public key is supported.
     */
    public boolean isKeyTypeSupported(Key key, boolean isPublic) {
        if (key == null)
            return false;
        if (isPublic)
            return (key instanceof DSAPublicKey);
        else
            return (key instanceof DSAPrivateKey);
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

        if (!(key instanceof DSAPublicKey))
            throw new CSPException("unsupported key type: " +
                key.getClass().getName());

        DSAPublicKey pk = (DSAPublicKey)key;
        DSAParams params = pk.getParams();
        BigInteger components[] = new BigInteger[4];
        components[G] = params.getG();
        components[P] = params.getP();
        components[Q] = params.getQ();
        components[Y] = pk.getY();
        try {
            ByteArrayOutputStream encoding = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(encoding);
            for (int i = 0; i != components.length; ++i)
                out.writeInt(components[i].bitLength());
            /*
             * TBD: need to see whether or not the sign bit should be discarded
             * as it is in the MD5RSABasic CSP.
             */
            for (int i = 0; i != components.length; ++i)
                out.write(components[i].toByteArray());
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

        try {
            BigInteger components[] = new BigInteger[4];
            int bitLengths[] = new int[4];
            ByteArrayInputStream encoding = new ByteArrayInputStream(key);
            DataInputStream in = new DataInputStream(encoding);
            for (int i = 0; i != bitLengths.length; ++i)
                bitLengths[i] = in.readInt();
            for (int i = 0; i != bitLengths.length; ++i) {
                /*
                 * TBD: need to see whether or not the sign bit should be added
                 * as it is in the MD5RSABasic CSP.
                 */
                byte bytes[] = new byte[bitLengths[i] / 8];
                in.read(bytes);
                components[i] = new BigInteger(bytes);
            }
            if (in.available() != 0)
                throw new CSPException("badly formed DSA key");
            in.close();

            DSAPublicKeySpec spec = new DSAPublicKeySpec(
                components[Y],
                components[P],
                components[Q],
                components[G]);
            return KeyFactory.getInstance("DSA").generatePublic(spec);
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

        if (!(key instanceof DSAPrivateKey))
            throw new CSPException("unsupported key type: " +
                key.getClass().getName());

        try {
            PKCS8EncodedKeySpec spec = (PKCS8EncodedKeySpec)
                KeyFactory.getInstance("DSA").getKeySpec(key,PKCS8EncodedKeySpec.class);
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
            return KeyFactory.getInstance("DSA").generatePrivate(spec);
        } catch (NoSuchAlgorithmException nsae) {
            throw new CSPException("Exception during private key decoding: "+
                nsae.getMessage());
        } catch (InvalidKeySpecException ikse) {
            throw new CSPException("Exception during private key decoding: "+
                ikse.getMessage());
        }
    }

}
