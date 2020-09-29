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
package java.lang;

import com.sun.squawk.csp.key.PublicKey;
import com.sun.squawk.csp.*;
import com.sun.squawk.util.Hashtable;
import com.sun.squawk.util.Vector;
import java.util.Enumeration;
import com.sun.squawk.util.Tracer;

/**
 * The TrustedKlass class is an extension of the Squawk VM Klass that includes
 * class level trust attributes.
 *
 * Classes (and interfaces) in the secure VM are partitioned into trusted
 * classes (T-classes) and untrusted classes (U-classes). Membership in the
 * set of T-classes is by virtue of a set of privileges obtained by each class.
 * These privileges define what relationship(s) a class has with other
 * T-classes. For class distribution purposes, these privileges are
 * encapsulated in a Trust Certificate (TC) that is attached to the class file
 * of a T-class. A pre-processor is used to add this TC to a class file as a
 * special ClassFile attribute, the Trusted.
 *
 * @author  Andrew Crouch
 * @version 1.0
 */
public class TrustedKlass extends Klass
{

    /*---------------------------------------------------------------------------*\
     *                         Fields of TrustedKlass                            *
    \*---------------------------------------------------------------------------*/


    /**
     * The pubic keys used to verify permits obtained from this class.
     */
    private PublicKey subclassKey;
    private PublicKey classResourceAccessKey;

    /**
     * Hashtables to store the permits this class holds to access other classes.
     */
    private Hashtable subclassPermits;
    private Hashtable classResourceAccessPermits;
    private Hashtable refClassResourceAccessPermits;

    /**
     * The hash of the original class file (without the Trusted attribute) that is used
     * to verify the permits held by this class
     */
    private byte[] classfileHash;

    /**
     * The domains to which this trusted class belongs.
     */
    private PublicKey primaryDomainKey;
    private Hashtable domains;


    /**
     * Used if enforcing class resource access (not implemented).
     */
    //  public boolean default_field_accessibility;
    //  public int[] non_default_fields;
    //  public boolean default_method_accessibility;
    //  public int[] non_default_methods;


    /*---------------------------------------------------------------------------*\
     *                                Constructor                                *
    \*---------------------------------------------------------------------------*/

    /**
     * Sets up the underlying Klass structure while initialising some TrustedKlass
     * specific structures.
     *
     * {@inheritDoc}
     *
     */
    public TrustedKlass(String name, Klass componentType, int classID) {
        super(name, componentType, classID);
    }



    /*---------------------------------------------------------------------------*\
     *                               Key Operations                              *
    \*---------------------------------------------------------------------------*/


    /**
     * Sets the subclass and class resource access public keys which are used verify
     * permits granted by this class.
     *
     * @param subclass     the subclass PublicKey
     * @param cra          the class resource access PublicKey
     */
    public void setKeys(PublicKey subclass, PublicKey cra) {
        this.subclassKey = subclass;
        this.classResourceAccessKey = cra;
    }

    /**
     * Returns the public key used to verify subclass permits granted by this
     * class to other trusted classes.
     *
     * @return PublicKey     the subclass public key
     */
    public PublicKey getSubclassKey() {
        return this.subclassKey;
    }


    /**
     * Returns the public key used to verify membership in a particular domain and
     * whether this class is considered a trusted class.
     *
     * @return PublicKey     the primary domain public key, else null if not set
     */
    public PublicKey getPrimaryDomainKey() {
        return primaryDomainKey;
    }

    /**
     * Returns the public key used to verify class resource access permits granted by this
     * class to other trusted classes.
     *
     * @return PublicKey     the class resource access public key
     */
    public PublicKey getClassResourceAccessKey() {
        return this.classResourceAccessKey;
    }



    /*---------------------------------------------------------------------------*\
     *                         Permit Operations                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Sets the list of domains associated with this class. The first entry
     * is assumed to be the primary domain.
     *
     * @param domain  the list of domains to which this class belongs
     */
    public void addDomains(Domain[] domains) {

        if(domains == null) {
            return;
        }

        /* Check we have some storage */
        if(this.domains == null && domains.length > 0) {
            this.domains = new Hashtable();
        }

        /**
         * For each non-null domain in the array, add it to the global hashtable
         * keyed on the public key associated with the domain.
         */
        for(int i = 0; i < domains.length; i++) {
           Domain d = domains[i];

           /* Do not include null entries */
           if(d == null) {
               continue;
           }

           /* If key is null, invalid domain */
           if(d.getKey() == null) {
               continue;
           }

           /* This is the primary key */
           if(i == 0 && this.domains.isEmpty()) {
               this.primaryDomainKey = d.getKey();
           }

           /* Add the domain */
           this.domains.put(d.getKey(), d);
        }
    }


    /**
     * Adds a variable number of subclass permits to this <code>TrustedKlass</code>
     *
     * @param permits Permit[] the array of permits to add
     */
    public void addSubclassPermits(Permit[] permits) {
        if(permits == null) {
            return;
        }

        /**
         * Initialise the global if this is the first
         * call and we actually have some permits to add
         */
        if(subclassPermits == null && permits.length > 0) {
            subclassPermits = new Hashtable();
        }


        /* Permits are keyed on the granting klass */
        for(int i = 0; i < permits.length; i++) {
            this.subclassPermits.put(permits[i].getKlass(), permits[i]);
        }

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" Added " + permits.length + " subclass permits to " + getName());
        }

    }

    /**
     * Adds a variable number of class resource access permits to this <code>TrustedKlass</code>
     *
     * @param permits Permit[] the array of permits to add
     */
    public void addCRAPermits(Permit[] permits) {
        if(permits == null) {
            return;
        }

        /**
         * Initialise the global if this is the first
         * call and we actually have some permits to add
         */
        if(classResourceAccessPermits == null && permits.length > 0) {
            classResourceAccessPermits = new Hashtable();
        }

        /* Permits are keyed on the granting klass */
        for(int i = 0; i < permits.length; i++) {
            this.classResourceAccessPermits.put(permits[i].getKlass(), permits[i]);
        }

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" Added " + permits.length + " class resource access permits to " + getName());
        }

    }

    /**
     * Adds a variable number of reflection class resource access permits to this <code>TrustedKlass</code>
     *
     * @param permits Permit[] the array of permits to add
     */
    public void addRefCRAPermits(Permit[] permits) {
        if(permits == null) {
            return;
        }

        /**
         * Initialise the global if this is the first
         * call and we actually have some permits to add
         */
        if(refClassResourceAccessPermits == null && permits.length > 0) {
            refClassResourceAccessPermits = new Hashtable();
        }


        /* Permits are keyed on the granting klass */
        for(int i = 0; i < permits.length; i++) {
            this.refClassResourceAccessPermits.put(permits[i].getKlass(), permits[i]);
        }

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln(" Added " + permits.length + " reflection class resource access permits to " + getName());
        }

    }

    /**
     * Searches for and returns a subclass permit granted to this class by grantor.  A permit
     * is only retrieved when verification occurs.  Once the class has been verified, the permit
     * is no longer required, hence, this operation removes the particular permit from the list
     * of permits held by this class.
     *
     * @param grantor    the <code>TrustedKlass</code> who has granted the subclass permit
     * @return Permit    the <code>Permit</code> granted by grantor else null if one does not exist.
     */
    public Permit getSubclassPermitFor(TrustedKlass grantor) {
        if(this.subclassPermits == null) {
            return null;
        }

        return (Permit)subclassPermits.remove(grantor.getName());
    }

    /**
     * Searches for and returns a class resource access permit granted to this class by grantor. A permit
     * is only retrieved when verification occurs.  Once the class has been verified, the permit
     * is no longer required, hence, this operation removes the particular permit from the list
     * of permits held by this class.
     *
     * @param grantor    the <code>TrustedKlass</code> who has granted the class resource access permit
     * @return Permit    the <code>Permit</code> granted by grantor else null if one does not exist.
     */
    public Permit getCRAPermitFor(TrustedKlass grantor) {
        if(this.classResourceAccessPermits == null) {
            return null;
        }

        return (Permit)classResourceAccessPermits.remove(grantor.getName());
    }

    /**
     * Searches for and returns a reflection class resource access permit granted to this class by grantor. A permit
     * is only retrieved when verification occurs.  Once the class has been verified, the permit
     * is no longer required, hence, this operation removes the particular permit from the list
     * of permits held by this class.
     *
     * @param grantor    the <code>TrustedKlass</code> who has granted the reflection class resource access permit
     * @return Permit    the <code>Permit</code> granted by grantor else null if one does not exist.
     */
    public Permit getRefCRAPermitFor(TrustedKlass grantor) {
        if(this.refClassResourceAccessPermits == null) {
            return null;
        }

        return (Permit)refClassResourceAccessPermits.remove(grantor.getName());
    }


    /*---------------------------------------------------------------------------*\
     *                         TrustedKlass Modifiers                            *
    \*---------------------------------------------------------------------------*/

    /**
     * Determines if this class can be subclassed by an untrusted class.
     *
     * @return  true if this class can be subclassed by an untrusted class
     */
    public final boolean allowUntrustedSubclass() {
        return Modifier.allowUntrustedSubclass(getModifiers());
    }


    /**
     * Determines if this class allows untrusted class resources access.
     *
     * @return  true if this class allows untrusted class resource access.
     */
    public final boolean allowUntrustedClassResourceAccess() {
        return Modifier.allowUntrustedClassResourceAccess(getModifiers());
    }


    /**
     * Determines if this class allows package-private exceptions thrown by this
     * class to be caught with a publically accessible base class of the
     * exception (Standard java semantics).  If it is is false then only
     * classes within the same package may catch such exceptions.
     *
     * @return  true if this class allows package private exceptions thrown by this
     * class to be caught with a publically available base class of the exception.
     */
    public final boolean allowPackagePrivateException() {
        return Modifier.allowPackagePrivateException(getModifiers());
    }




    /*---------------------------------------------------------------------------*\
     *                         Verification                                      *
    \*---------------------------------------------------------------------------*/



    /**
     * Sets the hash for this class' classfile.
     *
     * @param hash    the byte[] representing the hash generated by the CSP
     */
    public void setClassfileHash(byte[] hash) {
        this.classfileHash = hash;
    }

    /**
     * Gets the hash of this class' classfile.
     *
     * @return the byte[] representing the hash obtained from the CSP
     */
    public byte[] getClassfileHash() {
        return this.classfileHash;
    }

    /*---------------------------------------------------------------------------*\
     *                         Miscellaneous                                     *
    \*---------------------------------------------------------------------------*/

    /**
     * Check whether the <code>Klass</code> requestor has the necessary privileges to
     * access the class resources of <code>Klass</code> grantor.
     *
     * @param grantor       the Klass wishing to access some resource of requestor
     * @param requestor     the Klass providing some resource
     */
    public static void verifyClassResourceAccessPrivileges (Klass grantor, Klass requestor) {

        /* Check that we have been passed data to check */
        if (grantor == null || requestor == null) {
            throw new IllegalClassResourceAccessError ("Granting or Requesting class cannot be null");
        }

        /**
         * In the SKVM, there was a check here to see if this CRA privilege had already been checked.
         */

        if (Klass.DEBUG && Tracer.isTracing ("svm")) {
            Tracer.traceln ("[Verifying class resource access for: " + requestor.getName () + " -> " + grantor.getName() +"]");
        }

        /* Allow access to trusted klass specific methods */
        TrustedKlass tGrantor = (TrustedKlass) grantor;
        TrustedKlass tRequestor = (TrustedKlass) requestor;

        /* Check if the classes share a domain */
        boolean sharedDomain = TrustedKlass.trustedKlassDomainsIntersect (grantor, requestor);

        if (Klass.DEBUG && Tracer.isTracing ("svm") && sharedDomain) {
            Tracer.traceln ("Grantor and requestor share domain");
        }


        /**
         * If they don't share a domain, and the grantor is a trusted class and it is
         * enforcing class resource access privileges, check that the requestor has a permit
         * for the grantor class.
         */
        if (!sharedDomain && TrustedKlass.isTrustedKlass (grantor)) {

            /**
             * If the grantor is enforcing class resource access but the requestor doesn't
             * have a permit for this class, it is an IllegalClassResourceAccessError.
             */
            if (!tGrantor.allowUntrustedClassResourceAccess ()) {
                if(tRequestor.classResourceAccessPermits == null ||
                    !tRequestor.classResourceAccessPermits.containsKey (grantor)) {
                    throw new IllegalClassResourceAccessError("Required privilege permit missing");
                }
            }
        }

        /**
         * Permit that may or may not be checked for class resource access.
         */
        Permit permitToVerify = null;

        /**
         * If the requestor is a trusted class and has a class resource access
         * permit for the grantor, extract this permit now so that it can
         * be released even if the grantor turns out not to require
         * trusted class resource access. The getCRAPermitFor will remove the
         * Permit from the list of permits the requestor holds. If there is no
         * permit, permitToVerify will remain null.
         */
        if (TrustedKlass.isTrustedKlass (requestor)) {
            permitToVerify = (Permit) tRequestor.getCRAPermitFor (tGrantor);

            if (Klass.DEBUG && Tracer.isTracing("svm") && permitToVerify == null) {
                Tracer.traceln(requestor.getName() + " does not hold permit for " + grantor.getName());
            }

        }

        /*
         * If the class resource access privilege (which may not even exist) cannot
         * be verified because:
         *
         *   a. the grantor is not a trusted class or does not hold a class
         *      resource verification key, or
         *   b. the grantor and requestor are in a shared domain
         *
         * then the class resource access is trivially permitted.
         */
        if (!sharedDomain && TrustedKlass.isTrustedKlass (grantor) &&
            !tGrantor.allowUntrustedClassResourceAccess ()) {

            PublicKey grantorCRAKey = tGrantor.getClassResourceAccessKey ();

            /**
             * Set above. If we enter this code block, and we don't have a permit, we have a problem.
             */
            if (permitToVerify == null) {
                throw new IllegalClassResourceAccessError ("Trusted klass doesn't hold subclass permit for " + tGrantor.getName ());
            }

            if (grantorCRAKey == null) {
                throw new IllegalClassResourceAccessError ("Grantor has no class resource access key");
            }

            try {
                /**
                 * Signature to verify with subclass key
                 */
                byte[] signature = permitToVerify.getSignature ();

                if (signature == null) {
                    throw new IllegalClassResourceAccessError ("Permit held by " + tRequestor.getName () + " to access " + tGrantor.getName () + " has no signature");
                }

                /**
                 * The expected hash within the signature
                 */
                byte[] hash = tRequestor.getClassfileHash ();

                if (hash == null) {
                    throw new IllegalClassResourceAccessError ("classfile hash of " + tRequestor.getName () + " cannot be null");
                }

                /* Load the CSP instance */
                CSP csp = CSP.getInstance ();

                /* This should never occur */
                if (csp == null) {
                    throw new IllegalClassResourceAccessError ("CSP cannot be null");
                }

                /* Verify using the super classes CSP */
                if (!csp.verifyHash (hash, signature, grantorCRAKey)) {
                    throw new IllegalClassResourceAccessError ("Permit held by " + tRequestor.getName () + " to access " + tGrantor.getName () + " has invalid signature");
                }
            } catch (CSPException e) {
                throw new IllegalClassResourceAccessError ("Permit held by " + tRequestor.getName () + " to access " + tGrantor.getName () + " has invalid signature");
            }
        }

        /**
         * We will reach here if the access privileges are verified or the classes have a domain in common.
         */
        }




    /**
     * Checks whether the supplied class has the appropriate privileges to subclass its superclass
     * and to implement any interfaces it implements.
     *
     * @param klass     the class whos privileges are to be checked
     */
    public static void verifySubclassPrivileges(Klass klass) {

        /* If we are passed a null object, cannot verify privileges */
        if(klass == null) {
            return;
        }

        Klass superKlass = klass.getSuperclass();

        /**
         * The Squawk VM sets the superclass of an interface to null, even though it is really java.lang.Object.
         * We will set this here so we can appropriately check privileges.
         */
        if(klass.isInterface()) {
            if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                Tracer.traceln("Found interface " + klass.getName() + ". Setting superclass to java.lang.Object");
            }

            /* Superclass of interface is object */
            superKlass = klass.OBJECT;
        }

        /**
         * At this stage, if the superKlass is null, this is an IllegalSubclassError, moreover, something
         * has gone funny with the class loader.
         */
        if(superKlass == null) {
            throw new IllegalSubclassError("Superclass must be defined.");
        }

        /**
         * This check will have already been performed in ConstantPool#getResolvedClass(),
         * however, the base class is not full loaded at this stage.  For a trusted class to
         * be in the same package as it's parent, they must share a primary domain key.  Since
         * both classes are now fully loaded, this check can now verify that the classes do
         * indeed share a primary domain key if package-level access is required.
         *
         * Additionally, this only needs to be done here as any interfaces this class implements only
         * defines public methods.
         */
        if(!superKlass.isAccessibleFrom(klass)) {
            throw new IllegalSubclassError("Base class " +  klass.getName() + " doesn't have package level access to parent " + superKlass.getName());
        }

        if (Klass.DEBUG && Tracer.isTracing("svm")) {
            Tracer.traceln("[Verifying subclass privileges for: " + klass.getName() + "]");
        }

        /*
         * If a class purports to be a trusted class but its direct superClass
         * is not a trusted class, then this is a IllegalSubclassError
         * as a class can only be installed as a trusted subclass by
         * subclassing a trusted superclass.
         */
        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln("Checking superclass is trusted if this klass is trusted");
        }

        if (TrustedKlass.isTrustedKlass(klass) && !TrustedKlass.isTrustedKlass(superKlass)) {
            throw new IllegalSubclassError("Superclass expected to be a trusted class");
        }

        /**
         * This structure contains a list of TrustedKlass' who's signatures must
         * be verified before this klass can assert that it has appropriate
         * subclassing privileges.
         */
        Vector permitsRequiredToBeSet = new Vector();

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln("Enumerating number of interface permits required to be checked");
        }

        /* Check the permits of each of the interfaces this class implements */
        if (klass.getInterfaces() != null && klass.getInterfaces().length > 0) {
            int numberOfInterfaces = klass.getInterfaces().length;
            Klass[] trustedInterfaces = klass.getInterfaces();

            for (int i = 0; i < numberOfInterfaces; i++) {

                TrustedKlass trustedInterface = (TrustedKlass) trustedInterfaces[i];
                /*
                 * An interface requires a privilege to be implemented if:
                 *
                 *   a. it is a trusted class itself,
                 *   b. it enforces trusted subclassing, and
                 *   c. it is not in a trusted domain shared with the
                 *      implementing class.
                 */
                if (trustedInterfaces != null && !trustedInterface.allowUntrustedSubclass() &&
                    !TrustedKlass.trustedKlassDomainsIntersect(klass, trustedInterface)) {
                    permitsRequiredToBeSet.addElement(trustedInterface);
                }
            }
        }

        /* This klass is not a trusted class */
        if (!TrustedKlass.isTrustedKlass(klass)) {

            if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                Tracer.traceln("This klass is untrusted. Checking that superclass and interfaces allow it.");
            }


            /*
             * If the direct superclass or any of the interfaces enforces trusted
             * subclassing and the given class does not have a Trusted attribute,
             * then this is an IllegalSubclassError.
             */
            if (TrustedKlass.isTrustedKlass(superKlass) && ! ( (TrustedKlass)superKlass).allowUntrustedSubclass()) {
                throw new IllegalSubclassError("Untrusted class cannot subclass trusted class");
            }

            if (permitsRequiredToBeSet.size() > 0) {
                throw new IllegalSubclassError("Untrusted class cannot have outstanding permits to be checked");
            }

            /*
             * The given class is not a trusted class and trivially
             * satisfies all subclassing requirements (i.e. there are
             * none).
             */
            return;

        }

        /**
         * At this point, the superclass is guaranteed to be a trusted class.
         * Also, the subclass is purporting to be a trusted class (because
         * it has a Trusted attribute). As
         * such, the subclass must prove to be a trusted subclass of the
         * superclass either by sharing a trusted domain or by holding an
         * explicit subclassing permit.
         */
        TrustedKlass tklass = (TrustedKlass) klass;

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln("Checking if domains intersect");
        }

        /* If classes share the same domain key, they intrinsically "trust" each other */
        if (!TrustedKlass.trustedKlassDomainsIntersect(klass, superKlass)) {
            permitsRequiredToBeSet.addElement(superKlass);
        } else {
            Tracer.traceln("Domains intersect");
        }

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln("Found " + permitsRequiredToBeSet.size() + " permits to be checked");
        }


        /**
         * The process of verifying permits is the same whether it is a subclass, or interface
         * implementation.
         */
        while(permitsRequiredToBeSet.size() > 0) {

            /**
             * Verify each of the interface implementation and subclass pemits contained in
             * the vector.
             */
            TrustedKlass trustedKlass = (TrustedKlass) permitsRequiredToBeSet.firstElement();
            permitsRequiredToBeSet.removeElement(trustedKlass);

            /**
             * Load the permit from the trusted class
             */
            Permit permitToVerify = (Permit)tklass.subclassPermits.get(trustedKlass);

            /**
             * If we can't find a permit for the trustedKlass, it is an error.
             */
            if (permitToVerify == null) {
                throw new IllegalSubclassError("Trusted klass doesn't hold subclass permit for " + trustedKlass.getName());
            }

            try {

                if (Klass.DEBUG && Tracer.isTracing("svm")) {
                    Tracer.traceln("[Verifying subclass permit for: " + klass.getName() + " => " + trustedKlass.getName() + "]");
                }

                /**
                 * Public key used to verify signature in permit.
                 */
                PublicKey subclassKey = trustedKlass.getSubclassKey();

                if (subclassKey == null) {
                    String s = (trustedKlass.isInterface()) ? "Superclass " : "Interface ";
                    s += trustedKlass.getName();
                    throw new IllegalSubclassError(s + " of " + tklass.getName() + " doesn't have subclass key");
                }


                /**
                 * Signature to verify with subclass key
                 */
                byte[] signature = permitToVerify.getSignature();

                if (signature == null) {
                    throw new IllegalSubclassError("Permit held by " + tklass.getName() + " to subclass " + trustedKlass.getName() + " has no signature");
                }

                /**
                 * The expected hash within the signature
                 */
                byte[] hash = tklass.getClassfileHash ();

                if (hash == null) {
                    throw new IllegalSubclassError ("classfile hash cannot be null");
                }

                /* Load the CSP instance */
                CSP csp = CSP.getInstance();

                /* This should never occur */
                if (csp == null) {
                    throw new IllegalSubclassError("CSP cannot be null");
                }


                /* Verify using the super classes CSP */
                if (!csp.verifyHash(hash, signature, subclassKey)) {
                    throw new IllegalSubclassError("Permit held by " + tklass.getName() + " to subclass " + trustedKlass.getName() + " has invalid signature");
                }
            }
            catch (CSPException e) {
                throw new IllegalSubclassError("Permit held by " +
                                               tklass.getName() + " to subclass " + trustedKlass.getName() + " has invalid signature");
            }
        }

        /* If all went well, the supplied klass has valid subclass privileges */
    }

    /**
     * Determines whether the supplied <code>Klass</code> is a trusted class.  A class is trusted
     * only if it is of type <code>TrustedKlass</code> and that it's primary domain key is not null.
     *
     * @param k        the <code>Klass</code> to be checked
     * @return true if the supplied <code>Klass</code> is a trusted class
     */
    public static boolean isTrustedKlass(Klass k) {

        /* If not instance of TrustedKlass, cannot be a trusted klass */
        if (!(k instanceof TrustedKlass)) {
            return false;
        }

        TrustedKlass tk = (TrustedKlass) k;

        /* Furthermore, a trusted klass must have a primary domain key */
        if (tk.getPrimaryDomainKey() == null) {
            return false;
        }

        return true;
    }


    /**
     * Checks that the trusted flags are consistent.
     *
     * @param tklass TrustedKlass
     */
    public static void verifyTrustedClassFlags(TrustedKlass tklass) {

        /* If a class is final it cannot be subclassed */
        if (tklass.isFinal() && tklass.getSubclassKey() != null) {
            throw new LinkageError("ClassFormatError: final class " + tklass.getName() + " cannot have subclass key");
        }

        if (tklass.allowUntrustedSubclass() && tklass.getSubclassKey() != null) {
            throw new LinkageError("ClassFormatError: " + tklass.getName() + " cannot have subclass key when unprivileged access enabled");
        }

        if (tklass.allowUntrustedClassResourceAccess() && tklass.getClassResourceAccessKey() != null) {
            throw new LinkageError("ClassFormatError: " + tklass.getName() + " cannot have class resource access key when unprivileged access enabled");
        }
    }


    /**
     * Determines whether the domains of the two supplied classes interset. ie. That they have a domain in common.
     *
     * @param klassA
     * @param klassB
     * @return true if both classes are of type <code>TrustedKlass</code> and that they have at least one
     * domain in common.
     */
    public static boolean trustedKlassDomainsIntersect(Klass klassA, Klass klassB) {

        if (klassA == null || klassB == null) {
            return false;
        }

        /** This will check that there is at least 1 entry in the domain table for each class and
         *  that the primary domain key is not null
         */
        if(!TrustedKlass.isTrustedKlass(klassA) || !TrustedKlass.isTrustedKlass(klassB)) {
            return false;
        }

        TrustedKlass tklassA = (TrustedKlass)klassA;
        TrustedKlass tklassB = (TrustedKlass)klassB;

        /**
         * Most times the primary domain keys are the ones that will be
         * identical. As an optimisation, we compare these first
         */
        if(tklassA.getPrimaryDomainKey().equals(tklassB.getPrimaryDomainKey())) {
            return true;
        }


        /**
         * If the primary domain keys do not much, we look through each of one klasses domains,
         * and see if they exist in the other classes hashtable.
         */
        Enumeration e = tklassA.domains.keys();

        while(e.hasMoreElements()) {
            /**
             * Domains cannot be undefined, hence this operation is safe
             */
            if(tklassB.domains.containsKey(e.nextElement())) {
                return true;
            }
        }

        return false;
    }


    /**
     * In addition to the regular checks, this method also differentiates between trusted and untrusted
     * classes.
     *
     * - Untrusted classes require that only their Java package names match.
     * - Trusted classes are in the same package when both their Java package names match as well as their
     *   primary domain keys.
     * - Trusted and untrusted classes are never in the same package.
     *
     * {@inheritDoc}
     */
    public final boolean isInSamePackageAs(Klass klass) {

        /* Check if both are in same java package */
        boolean sameJavaPackage = super.isInSamePackageAs(klass);

        /* If they are not in same java package, immediately return */
        if (!sameJavaPackage) {

            if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                Tracer.traceln("Not in same standard java package");
            }

            return false;
        }

        /*
         * When a class is loaded, its superclass is fully loaded before it.  This
         * method is called on the superclass to ensure the subordinate, (that is not
         * fully loaded) does indeed have access.  However, in the secure environment,
         * this check encompasses the domain keys.  At this stage, the subordinate class
         * will not have had it's domain keys loaded. Therefore, only the standard Java
         * semantics for same package can be used.
         */
        if(klass.getState() == Klass.State.LOADING) {
            return sameJavaPackage;
        }


        /*
         * An untrusted class is never in the same package as a trusted class
         */
        if (TrustedKlass.isTrustedKlass(this) != TrustedKlass.isTrustedKlass(klass)) {
            if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                Tracer.traceln("Mix of untrusted/trusted classes");
            }

            return false;
        }

        /**
         * The parent and child are both either trusted or untrusted.  If the supplied
         * class is untrusted, then the standard java semantics are fine. Since these have
         * already been checked, they are in the same package.
         */
        if(!TrustedKlass.isTrustedKlass(klass)) {
            return true;
        }

        /*
         * Two trusted classes must also share a domain.
         */
        boolean domainKeysMatch = TrustedKlass.trustedKlassDomainsIntersect(this, klass);

        if(!domainKeysMatch) {
            if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                Tracer.traceln("Mismatched domains keys");
            }
        }

        return domainKeysMatch;
    }

    /**
     * Check whether the accessingKlass can access the supplied member
     * @param accessingKlaass     the <code>Klass</code> attempting to access a trusted method
     * @param member              the <code>Member</code> being acessed
     * @return true if the accessingKlass has access privileges to the memberKlass.
     */
    public static boolean classHasTrustedAccessToPublicOrProtectedMember(Member member, Klass accessingKlass) {

        Klass definingClass = member.getDefiningClass();
        int memberModifiers = member.getModifiers();

        if (TrustedKlass.isTrustedKlass(definingClass)) {
            /**
             * If this member is allowing untrusted access, immediately return.
             */
            if (Modifier.allowUntrustedAccessToMember(memberModifiers)) {

                if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                    Tracer.traceln(member.getFullyQualifiedName() + " allows untrusted access");
                }

                return true;

            } else {
                /**
                 * The member doesn't allow untrusted access, so we must validate the credentials of the
                 * accessing klass
                 */

                /**
                 * Only perform check for constructor and static methods.
                 */
                if ( Modifier.isStatic(memberModifiers) || Modifier.isConstructor(memberModifiers) ) {


                    /**
                     * A trusted subclass always has access to its direct superclass since the subclassing
                     * privileges will have already been verified.
                     */
                    if (Modifier.isConstructor(memberModifiers) && accessingKlass.getSuperclass() == definingClass) {
                        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                            Tracer.traceln(member.getFullyQualifiedName() + " is being accessed by trusted subclass");
                        }
                        return true;
                    }

                    /**
                     * Since the accesingKlass is not a direct subclass, and the member is enforcing class
                     * resource access, we must verify that the accessingKlass has appropriate rights to
                     * access the member.
                     */
                    TrustedKlass.verifyClassResourceAccessPrivileges(definingClass, accessingKlass);

                    /**
                     * Since the method above with throw a security error if there is a problem, if we make it here
                     * the accessingKlass does have valid privileges.
                     */
                    if (Klass.DEBUG && Tracer.isTracing("svmload")) {
                        Tracer.traceln("Class resource access privileges held by " + accessingKlass.getName() + " have been verified");
                    }

                    return true;

                } else {
                    /**
                     * The field or method being accessed is an instance member. To have access to these, the class
                     * must be instanciated. To do so requires a constructor to be called.  Since privileges will
                     * have been verified on the constructor, there is no need to do this on instance members.
                     */
                    return true;
                    }

            }

        } else {

            /**
             * The member being accessed is itself not trusted.  Therefore it can be accessed by default.
             */
            return true;
        }

    }
}
