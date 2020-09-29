package svmtools;

import java.io.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.Constants;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPair;
import svmtools.CSP;

/**
 * This class represents a class that has been loaded into the Wobulator.
 * The information encapsulated is the source from which the class was loaded
 * as well as a model of the class that can be retrieved and manipulated to
 * perform the wobulation.
 */
public class LoadedClass {

    /**
     * The class being wobulated.
     */
    private TrustedJavaClassGen model;

    /**
     * The source of the classfile.
     */
    private File source;

    /**
     * This is the key that will be used to generate the primary domain permit.
     */
    private KeyPair primaryDomainKeyPair;

    /**
     * The constructor initialises a new class-under-wobulation.
     * @param jc A JavaClass loaded from a class file.
     * @param source The file the classfile was loaded from.
     * @param csp The CSP used to wobulate the class.
     */
    public LoadedClass(JavaClass javaClass, File source, CSP csp) {
        model = new TrustedJavaClassGen(javaClass,csp.getIdentifier());
        this.source = source;
    }

    /**
     * Return the file from which the class was loaded.
     * @return the file from which the class was loaded.
     */
    public File getSourceFile() {
        return source;
    }

    /**
     * Set the primary domain permit signing key.
     * @param key The package permit key.
     */
    public void setPrimaryDomainKeyPair(KeyPair kp) {
        primaryDomainKeyPair = kp;
    }

    /**
     * Get the package permit signing key.
     * @return the package permit key.
     */
    public KeyPair getPrimaryDomainKeyPair() {
        return primaryDomainKeyPair;
    }

    /**
     * Return the model of the loaded class.
     * @return the model of the loaded class.
     */
    public TrustedJavaClassGen getModel() {
        return model;
    }

    /**
     * Create the generated class and return it.
     * @param csp The CSP that will sign the package permit.
     * @exception IOException if the class could not be saved for any reason.
     * The message in the exception will contain the reason.
     */
    public JavaClass createJavaClass(CSP csp) throws IOException {
        if (primaryDomainKeyPair == null)
            throw new IOException("A primary domain key must be selected before the class can be saved");

        // Generate package permit so that we can generate the complete
        // trusted class.
        byte part1[] = model.getSubclassPermitInput();
        byte part2[] = model.getDomainsSignatureInput();
        byte whole[] = new byte[part1.length + part2.length];
        System.arraycopy(part1,0,whole,0,part1.length);
        System.arraycopy(part2,0,whole,part1.length,part2.length);
        try {
            byte[] sig = csp.sign(whole,primaryDomainKeyPair.getPrivate());
            byte[] pubKey = csp.encodePublicKey(primaryDomainKeyPair.getPublic());
            JavaClass jc = model.getTrustedClass(new byte[][] { pubKey },
                new byte[][] { sig },true);
            // Renew the model so that it is in sync with the generated class.
            model = new TrustedJavaClassGen(jc,csp.getIdentifier());
            return jc;
        } catch (CSPException e) {
            throw new IOException(e.getMessage());
        }
        catch (VerifyError ve) {
            throw new IOException(ve.getMessage());
        }
    }

}
