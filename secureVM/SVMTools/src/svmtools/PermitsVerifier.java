package svmtools;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;
import org.apache.bcel.classfile.*;
import java.io.*;
import java.util.*;
import java.security.*;

/**
 * This is a helper class that can verify the permits for a given class.
 * The verifier uses an interface (GrantorDB) to find the class that
 * represents the grantor of a given permit and to extract relevant info
 * out of it.
 */
public class PermitsVerifier {

    /**
     * The verifier uses this interface to find granting classes and extract
     * relevant info out of them that it required to verify the permit issued
     * by a granting class.
     */
    static public interface GrantorDB {
        /**
         * Return the key data for the subclass or class resource access key
         * of a given grantor class.
         * @param name The name of the grantor class.
         * @param isSubclass Specifies if the subclass key is being requested.
         * @exception NoSuchElementException if the grantor is not available or
         * is not a trusted class. The string message in the exception should
         * specify exactly what the problem was.
         */
        public byte[] getKey(String name, boolean isSubclass)
            throws NoSuchElementException;
        /**
         * Return the trusted access flags of a given grantor class.
         * @param name The name of the grantor class.
         * @exception NoSuchElementException if the grantor is not available or
         * is not a trusted class. The string message in the exception should
         * specify exactly what the problem was.
         */
        public int getClassAccessFlags(String name)
            throws NoSuchElementException;
    }

    /**
     * This is a concrete implementation of the GrantorDB interface that
     * retrieves classes from a Hashtable of type String -> JavaClass.
     */
    static public class DefaultGrantorDB implements GrantorDB {
        Hashtable classes;
        Hashtable cache;

        public DefaultGrantorDB(Hashtable classes) {
            if (classes == null)
                throw new IllegalArgumentException("classes can't be null");
            this.classes = classes;
            this.cache = new Hashtable();
        }
        public byte[] getKey(String name, boolean isSubclass)
            throws NoSuchElementException
        {
            Trusted t = lookupTrusted(name);
            if (isSubclass)
                return t.getSubclassKeyBytes();
            else
                return t.getClassResourceAccessKeyBytes();
        }
        public int getClassAccessFlags(String name)
            throws NoSuchElementException
        {
            return lookupTrusted(name).getAccessFlags();
        }

        private Trusted lookupTrusted(String name)
            throws NoSuchElementException
        {
            Trusted t = (Trusted)cache.get(name);
            if (t == null) {
                JavaClass jc = (JavaClass)classes.get(name);
                if (jc == null)
                    throw new NoSuchElementException(name + " not found");
                t = jc.getTrustedAttribute();
                if (t == null)
                    throw new NoSuchElementException(name +
                        " is not a trusted class");
                cache.put(name,t);
            }
            return t;
        }
    }

    public static void main(String[] args) {

    }

    /**
     * This is a concrete implementation of the GrantorDB interface that
     * retrieves classes from a given ClassPath.
     */
    static public class ClassPathGrantorDB implements GrantorDB {
        ClassPath cp;
        Hashtable cache;

        public ClassPathGrantorDB(ClassPath cp) {
            if (cp == null)
                throw new IllegalArgumentException("cp can't be null");
            this.cp = cp;
            this.cache = new Hashtable();
        }
        public byte[] getKey(String name, boolean isSubclass)
            throws NoSuchElementException
        {
            Trusted t = lookupTrusted(name);
            if (isSubclass)
                return t.getSubclassKeyBytes();
            else
                return t.getClassResourceAccessKeyBytes();
        }
        public int getClassAccessFlags(String name)
            throws NoSuchElementException
        {
            return lookupTrusted(name).getAccessFlags();
        }

        private Trusted lookupTrusted(String name)
            throws NoSuchElementException
        {
            Trusted t = (Trusted)cache.get(name);
            if (t == null) {
                ClassFile cf = cp.getFile(name.replace('.',File.separatorChar) + ".class");
                if (cf == null)
                    throw new NoSuchElementException(name + " not found");
                JavaClass jc = null;
                try {
                    InputStream is = cf.getInputStream();
                   ClassParser parser = new ClassParser(is,cf.getName());
                    jc = parser.parse();
                } catch (IOException ioe) {
                    throw new NoSuchElementException("IOException while loading "+
                        name+": "+ioe.getMessage());
                }
                catch (ClassFormatError cfe) {
                    throw new NoSuchElementException("Class format error while loading "+
                        name+": "+cfe.getMessage());

                }
                t = jc.getTrustedAttribute();
                if (t == null)
                    throw new NoSuchElementException(name +
                        " is not a trusted class");
                cache.put(name,t);
            }
            return t;
        }
    }

    /**
     * Verify the permits for a given class.
     * @param jc The class to verify.
     * @param csp The CSP to use for permit verification.
     * @param db The database interface to use when searching for grantor classes.
     * @return A Vector of error messages or null if verification succeeded.
     */
    public static Vector verifyPermits(JavaClass jc, CSP csp,
        GrantorDB db)
    {
        Vector msgs = new Vector();
        try {
            Trusted t = jc.getTrustedAttribute();
            if (t == null)
                return null;

            ConstantPool cp = jc.getConstantPool();
            ConstantPool sp = t.getTrustedConstantPool();
            TrustedJavaClassGen gen =
                new TrustedJavaClassGen(jc,csp.getIdentifier());
            byte[] permitInput = gen.getSubclassPermitInput();
            byte[] domainPermitInput = gen.getDomainsSignatureInput();

            // Verify subclass permits
            String[] interfaces = jc.getInterfaceNames();
            Trusted.Permit[] permits = t.getSubclassPermits();
            for (int i = 0; i != permits.length; i++) {
                Trusted.Permit p = permits[i];
                int classIndex = p.getClassIndex();
                String grantorName;
                if (classIndex == interfaces.length)
                    grantorName = jc.getSuperclassName();
                else
                    grantorName = interfaces[classIndex];

                byte[] keyData = null;
                try {
                    keyData = db.getKey(grantorName,true);
                    if (keyData == null) {
                        msgs.add("Superclass/super-interface "+ grantorName +
                            " has no subclassing key");
                        continue;
                    }
                } catch (NoSuchElementException nsee) {
                    msgs.add("Cannot verify subclass permit: " +
                        nsee.getMessage());
                    continue;
                }

                PublicKey pk = csp.decodePublicKey(keyData);
                ConstantDigitalSignature dsig = (ConstantDigitalSignature)
                    sp.getConstant(p.getSigIndex());
                // dsig will *never* be null
                byte[] sigBytes = dsig.getSignature();
                if (csp.verify(permitInput,sigBytes,pk) != true)
                    msgs.add("Verification of subclass permit granted by "+
                        grantorName + " failed");

            }

            // Verify ClassResourceAccess permits
            Trusted.Permit[][] allPermits = new Trusted.Permit[][] {
                t.getClassResourceAccessPermits(),
                t.getRefClassResourceAccessPermits()
            };
            for (int k = 0; k != allPermits.length; k++) {
                permits = allPermits[k];
                for (int i = 0; i != permits.length; i++) {
                    Trusted.Permit p = permits[i];
                    int classIndex = p.getClassIndex();
                    String grantorName = cp.constantToString(classIndex,
                        Constants.CONSTANT_Class);

                    byte[] keyData = null;
                    try {
                        int flags = db.getClassAccessFlags(grantorName);
                        if ((flags & Trusted.TACC_CLASS_RESOURCE_ACCESS) != 0) {
                            msgs.add(grantorName +
                                " does not enforce privileged class" +
                                " resource access - permit ignored");
                            continue;
                        }
                        keyData = db.getKey(grantorName,false);
                    } catch (NoSuchElementException nsee) {
                        msgs.add("Cannot verify class resource access permit: "+
                            nsee.getMessage());
                        continue;
                    }

                    if (keyData == null) {
                        msgs.add(grantorName +
                            " has no class resource access key. It" +
                            " supports only shared domain verification");
                        continue;
                    }
                    PublicKey pk = csp.decodePublicKey(keyData);
                    ConstantDigitalSignature dsig = (ConstantDigitalSignature)
                        sp.getConstant(p.getSigIndex());
                    // dsig will *never* be null
                    byte[] sigBytes = dsig.getSignature();
                    if (csp.verify(permitInput,sigBytes,pk) != true)
                        msgs.add("Verification of class resource access " +
                            "permit granted by " + grantorName + " failed");
                }
            }

            // Verify primary domain permit
            PublicKey pk = csp.decodePublicKey(t.getDomains()[0].getKey());
            byte[] input = new byte[permitInput.length +
                                    domainPermitInput.length];
            System.arraycopy(permitInput,0,input,0,permitInput.length);
            System.arraycopy(domainPermitInput,0,input,permitInput.length,
                domainPermitInput.length);
            if (csp.verify(input,t.getDomains()[0].getSignature(),pk) != true)
                msgs.add("Verification of primary domain permit failed");

        } catch (Exception ex) {
            msgs.add(ex.toString());
        }
        // Display result of verification
        if (msgs.size() == 0)
            return null;
        return msgs;
    }

}
