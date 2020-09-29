package svmtools;

import java.security.*;
import java.io.*;
import java.util.*;

/**
 * This class is a database of keys. Currently, the database contents are
 * initialized from a JKS KeyStore.
 */
public class KeyManager {

    /**
     * Collection of imported key pairs. This is a map from String to KeyPair
     * objects.
     */
    private Hashtable keys = new Hashtable();

    /**
     * A cache of the public aliasesin the database.
     */
    private String[] publicAliases;

    /**
     * A cache of the public aliasesin the database.
     */
    private String[] privateAliases;

    /**
     * The CSP used for encoding/decoding.
     */
    private CSP csp;

    /**
     * Return the aliases of all the keys of a certain type that are in the
     * database.
     * @param isPublic if true, return the aliases for all the
     * public keys otherwise return the aliases of all the private keys.
     */
    public String[] getKeyAliases(boolean isPublic) {
        // Build caches if they don't already exist.
        if (publicAliases == null) {
            publicAliases = new String[keys.size()];
            Vector priAliases = new Vector(keys.size());
            int i = 0;
            for (Enumeration e = keys.keys(); e.hasMoreElements();) {
                String alias = (String)e.nextElement();
                publicAliases[i++] = alias;
                KeyPair kp = (KeyPair)keys.get(alias);
                if (kp.getPrivate() != null)
                    priAliases.add(alias);
            }
            privateAliases = new String[priAliases.size()];
            priAliases.toArray(privateAliases);
        }
        // A clone is returned so that the cached arrays can't be manipulated
        // externally.
        if (isPublic)
            return (String[])publicAliases.clone();
        else
            return (String[])privateAliases.clone();
    }

    /**
     * Return the key pair corresponding to a given alias. The private key
     * component of the key pair may be null.
     * @param alias The alias used to retrieve the key pair.
     */
    public KeyPair getKeyPair(String alias) {
        return (KeyPair)keys.get(alias);
    }

    /**
     * Return the key pair (if any) whose public key matches the given
     * public key.
     * @param key The public key to match against.
     * @return the key pair (if any) whose public key matches key or null if
     * there is no match.
     */
    public KeyPair getKeyPair(PublicKey key) {
        for (Enumeration e = keys.elements(); e.hasMoreElements();) {
            KeyPair kp = (KeyPair)e.nextElement();
            if (java.util.Arrays.equals(kp.getPublic().getEncoded(),key.getEncoded()))
                return kp;
        }
        return null;
    }


    /**
     * Return the alias (if any) for a given key.
     */
    public String getAlias(byte[] encodedKey) {
        for (Enumeration e = keys.keys(); e.hasMoreElements();) {
            String alias = (String)e.nextElement();
            KeyPair kp = (KeyPair)keys.get(alias);
            PublicKey key = kp.getPublic();
            try {
                byte[] encodedPublicKey = csp.encodePublicKey(key);
                if (java.util.Arrays.equals(encodedKey,encodedPublicKey))
                    return alias;
            } catch (CSPException cspe) {
            }
        }
        return null;
    }

    /**
     * Initialize the contents of the database from a JKS keystore. Only the
     * entries that are not password protected or have the same password as the
     * keystore itself are imported.
     * @param keystore The name of the file containing a JKS keystore.
     * @param storepass The password for the keystore.
     * @param csp If not null, this CSP is used to restrict the imported keys to
     * be only those that are compatible with itself.
     * @exception KeyStoreException if the initialization fails for some reason.
     */
    public void initialize(String keystore, char[] storepass, CSP csp)
            throws KeyStoreException
    {
        this.csp = csp;
        FileInputStream ksStream = null;
        try {
            // Open the keystore
            KeyStore ks = KeyStore.getInstance("JKS");
            ksStream = new FileInputStream(keystore);
            ks.load(ksStream,storepass);
            ksStream.close();
            // Import all the entries
            Enumeration aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String)aliases.nextElement();
                // Read in a public key (if any)
                java.security.cert.Certificate cert = ks.getCertificate(alias);
                PublicKey pubKey = null;
                if (cert != null) {
                    pubKey = cert.getPublicKey();
                    // Make sure it is compatible with the CSP
                    if (csp == null || csp.isKeyTypeSupported(pubKey,true)) {
                        // Now see if there is a matching private key that can
                        // be used for package signing
                        PrivateKey priKey = null;
                        try {
                            priKey = (PrivateKey)ks.getKey(alias,storepass);
                        } catch (UnrecoverableKeyException uke) {
                            // Cannot recover private key - can still use public key
                            System.err.println("private key <"+alias +"> unrecoverable");
                        }
                        catch (NoSuchAlgorithmException nsae) {
                            // Should never happen as the public key was
                            // successfully recovered if we are here
                        }
                        keys.put(alias,new KeyPair(pubKey,priKey));
                    }
                }
            }
        }
        catch (Exception e) {
            throw new KeyStoreException("Exception while initializing KeyManager: "+
                e);
        }
    }
}
