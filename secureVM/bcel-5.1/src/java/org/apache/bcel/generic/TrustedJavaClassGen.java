package org.apache.bcel.generic;

import org.apache.bcel.classfile.*;
import org.apache.bcel.*;
import java.util.*;
import java.io.*;

/**
 * This is a helper for building a SVM trusted class. That is, a class
 * which has a class level "Trusted" attribute. It makes little sense to
 * build a Trusted attribute in isolation from the class itself.
 */
public class TrustedJavaClassGen {

    /**
     * Original class file.
     */
    private final JavaClass clazz;

    /**
     * A ConstantPoolGen version of the original constant pool. This is used
     * for efficiently querying the original constant pool.
     */
    private final ConstantPoolGen originalCP;

    /**
     * Original class bytes.
     */
    private final byte[] originalClass;

    /**
     * These are the fields of a Trusted attribute.
     */
    private final int cp_extra_entry_offset;
    private final String csp_identifier;
    private int access_flags;
    private byte[] subclass_key;
    private byte[] class_resource_access_key;
    private boolean default_field_accessibility;
    private boolean default_method_accessibility;
    private Set non_default_fields;
    private Set non_default_methods;

    /**
     * Map: int -> byte[]
     * This maps the constant pool index of a class granting a subclass
     * permit to the byte array holding the signature of the permit.
     */
    private HashMap subclass_permits;

    /**
     * Map: int -> byte[]
     * This maps the constant pool index of a class granting a class resource access
     * permit to the byte array holding the signature of the permit.
     */
    private HashMap class_resource_access_permits;
    /**
     * Map: String -> byte[]
     * This maps the signature of a class granting a class resource access via
     * reflection permit to the byte array holding the signature of the permit.
     */
    private HashMap ref_class_resource_access_permits;

    static public Set intArrayToSet(int array[]) {
        Set set = new TreeSet();
        for (int i = 0; i != array.length; ++i)
            set.add(new Integer(array[i]));
        return set;
    }
    static public int[] setToIntArray(Set set) {
        int array[] = new int[set.size()];
        int pos = 0;
        for (Iterator i = set.iterator(); i.hasNext(); pos++)
            array[pos] = ((Integer)i.next()).intValue();
        return array;
    }

    /**
     * Helper method to extract the bytes of a public key entry in a constant
     * pool.
     * @param cp The constant pool to access.
     * @param index The index of the ConstantPublicKey entry.
     * @return the bytes of the specified key or null if the index was 0.
     */
    private byte[] getKeyBytes(ConstantPool cp, int index) {
        if (index == 0)
            return null;
        ConstantPublicKey key = (ConstantPublicKey)
            cp.getConstant(index,Constants.CONSTANT_PublicKey);
        return key.getKey();
    }

    /**
     * Helper method to extract the bytes of a digital signature entry in a
     * constant pool.
     * @param cp The constant pool to access.
     * @param index The index of the ConstantDigitalSignature entry.
     * @return the bytes of the specified key or null if the index was 0.
     */
    private byte[] getSigBytes(ConstantPool cp, int index) {
        if (index == 0)
            return null;
        ConstantDigitalSignature sig = (ConstantDigitalSignature)
            cp.getConstant(index,Constants.CONSTANT_DigitalSignature);
        return sig.getSignature();
    }

    /**
     * Helper method to convert a Permit[] array into a HashMap.
     * @param policies The permits in array form.
     * @param sp The constant pool of the Trusted attribute.
     * @param ref If true, then these are reflection related class resource access
     * permits
     */
    static private HashMap permitsToHashMap(Trusted.Permit[] permits,
            ConstantPool sp,
            boolean ref)
    {
        HashMap map = new HashMap(permits.length);
        for (int i = 0; i != permits.length; ++i) {
            int classIndex = permits[i].getClassIndex();
            int sigIndex = permits[i].getSigIndex();
            Object key;
            byte value[] = ((ConstantDigitalSignature)
                sp.getConstant(sigIndex,Constants.CONSTANT_DigitalSignature)).
                    getSignature();

            if (!ref)
                key = new Integer(classIndex);
            else
                key = sp.constantToString(classIndex,Constants.CONSTANT_Utf8);
            map.put(key,value);
        }
        return map;
    }

    /**
     * Helper method to convert a HashMap into a Permit[] array.
     * @param policies The permits in HashMap form.
     * @param spGen The constant pool template of the Trusted attribute.
     * @param ref If true, then these are reflection related class resource access
     * permits
     * @return
     */
    static private Trusted.Permit[] hashMapToPermits(HashMap permits,
            ConstantPoolGen sp,
            boolean ref)
    {
        Trusted.Permit arr[] = new Trusted.Permit[permits.size()];
        Set entries = permits.entrySet();
        int pos = 0;
        for (Iterator i = entries.iterator(); i.hasNext() ; ++pos) {
            Map.Entry entry = (Map.Entry)i.next();
            Object grantor = entry.getKey();
            int sigIndex = sp.addDigitalSignature((byte[])entry.getValue());
            int classIndex;
            if (!ref)
                classIndex = ((Integer)grantor).intValue();
            else
                classIndex = sp.addUtf8((String)grantor);
            arr[pos] = new Trusted.Permit(classIndex,sigIndex);
        }
        return arr;
    }

    /**
     * Construct a TrustedJavaClassGen object with the default values.
     * @param clazz The JavaClass used to establish default values.
     * @param csp The Cryptographic Service Provider's identifier. This
     * cannot be changed once the TrustedJavaClassGen has been created.
     * @param serializedClass This the serialized form of the JavaClass and is
     * usually the class file from which clazz was loaded.
     */
    public TrustedJavaClassGen(JavaClass clazz, String csp) {
        this.clazz = clazz;
        ConstantPool cp = clazz.getConstantPool();
        originalCP = new ConstantPoolGen(cp);

        /*
         * Serialize the given clazz.
         */
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            clazz.dump(new DataOutputStream(baos),false);
        } catch (IOException ioe) {
            System.out.println("IOException in TrustedJavaClassGen constructor:" +
                ioe.getMessage());
        }
        this.originalClass = baos.toByteArray();

        /*
         * Extract any existing Trusted attribute out of the JavaClass to
         * use when building this object.
         */
        Trusted trusted = clazz.getTrustedAttribute();

        if (trusted != null) {
            cp_extra_entry_offset = trusted.getCPExtraEntryOffset();
            ConstantPool sp = trusted.getTrustedConstantPool();
            csp_identifier = sp.constantToString(trusted.getCSPIdentifier(),
                Constants.CONSTANT_Utf8);
            if (!csp_identifier.equals(csp))
                throw new ClassFormatError("Given CSP identifier conflicts "+
                    "with existing CSP identifier");
            access_flags = trusted.getAccessFlags();
            subclass_key = getKeyBytes(sp,trusted.getSubclassKey());
            class_resource_access_key = getKeyBytes(sp,trusted.getClassResourceAccessKey());
            default_field_accessibility = trusted.getDefaultFieldAccessibility();
            non_default_fields =
                intArrayToSet(trusted.getFieldsAccessibility());
            default_method_accessibility = trusted.getDefaultMethodAccessibility();
            non_default_methods =
                intArrayToSet(trusted.getMethodsAccessibility());
            subclass_permits =
                permitsToHashMap(trusted.getSubclassPermits(),sp,false);
            class_resource_access_permits =
                permitsToHashMap(trusted.getClassResourceAccessPermits(),sp,false);
            ref_class_resource_access_permits =
                permitsToHashMap(trusted.getRefClassResourceAccessPermits(),sp,true);
        }
        else {
            non_default_fields = new TreeSet();
            non_default_methods = new TreeSet();
            subclass_permits = new HashMap();
            class_resource_access_permits = new HashMap();
            ref_class_resource_access_permits = new HashMap();
            csp_identifier = csp;
            /*
             * calculate the cp_extra_entry_offset value
             */
            String attrName = Constants.ATTRIBUTE_NAMES[Constants.ATTR_TRUSTED];
            int name_index = originalCP.lookupUtf8(attrName);
            if (name_index < 1) {
                CountOutputStream counter = new CountOutputStream();
                DataOutputStream os = new DataOutputStream(counter);
                try {
                    cp.dump(os);
                }  catch (IOException ioe) {
                    System.out.println("IOException in TrustedJavaClassGen constructor:" +
                        ioe.getMessage());
                }
                cp_extra_entry_offset = 8 + counter.getSize();
            }
            else
                cp_extra_entry_offset = 0;
        }
    }

    /**
     * Return the
     */

    /**
     * Set the subclassing or class resource access key for this class,
     * replacing any existing key. This method can also be used to clear the
     * key.
     * @param key The new key or null if the key is to be cleared.
     * @param type An value specifying which key is to be updated. The legal
     * values are 1 (subclassing key) or 2 (class resource access key).
     * @exception IllegalArgumentException if type is not one of the legal values.
     */
    private void setKey(byte key[], int type)
            throws IllegalArgumentException {
        switch (type) {
        case 1: subclass_key = key; return;
        case 2: class_resource_access_key = key; return;
        }
        throw new IllegalArgumentException(type + " is not a valid key type");
    }
    public void setSubclassKey(byte key[]) { setKey(key,1); }
    public void setClassResourceAccessKey(byte key[]) { setKey(key,2); }



    /**
     * Return a key.
     * @param type An value specifying which key is to be returned. The legal
     * values are 1 (subclassing key) or 2 (class resource access key).
     * @exception IllegalArgumentException if type is not one of the legal values.
     */
    private byte[] getKey(int type)
            throws IllegalArgumentException {
        switch (type) {
        case 1: return subclass_key;
        case 2: return class_resource_access_key;
        }
        throw new IllegalArgumentException(type + " is not a valid key type");
    }
    public byte[] getSubclassKey() { return getKey(1); }
    public byte[] getClassResourceAccessKey() { return getKey(2); }

    /**
     * Set the class access flags.
     * @param flags The new value of the class access flags.
     */
    public void setClassAccessFlags(int flags) {
        access_flags = flags;
    }

    /**
     * Set the default access for all public and protected fields in this class
     * that don't have an entry in the non_default_fields table.
     */
    public void setDefaultFieldAccessibility(boolean isAccessible) {
        default_field_accessibility = isAccessible;
    }

    /**
     * Set the default access for all public and protected methods in this class
     * that don't have an entry in the non_default_methods table.
     */
    public void setDefaultMethodAccessibility(boolean isAccessible) {
        default_method_accessibility = isAccessible;
    }

    /**
     * Clear all non-default members.
     * @param isField
     */
    public void clearNonDefaultMembers(boolean isField) {
        if (isField)
            non_default_fields.clear();
        else
            non_default_methods.clear();
    }

    /**
     * Remove a member from the non-default set.
     * @param member The member that is to be removed.
     * @param isField true if this is a field.
     */
    public void removeNonDefaultMember(int member, boolean isField) {
        if (isField)
            non_default_fields.remove(new Integer(member));
        else
            non_default_methods.remove(new Integer(member));
    }

    /**
     * Set the access policy for a member. This overrides any previous access
     * flags set for the member. No verification is performed during this method.
     * However, the correct value of the Trusted.SACC_IS_FIELD bit is set
     * according to the value of the isField parameter.
     * @param member The member.
     * @param flags The flags implementing the access policy for the
     * given member.
     * @param isField true if this is a field.
     * @return true if there was already a policy for the given member.
     */
    public void addNonDefaultMember(int member, boolean isField){
        if (isField)
            non_default_fields.add(new Integer(member));
        else
            non_default_methods.add(new Integer(member));
    }

    /**
     * Set a permit for this class to subclass or access a class resource of a given
     * class. This method can also be used to clear the permit.
     * @param grantor If type is 1 or 2, this must be a Integer object
     * which is the constant pool index of the class granting the permit.
     * Otherwise, it must be a String which is the signature of the
     * class granting the permit.
     * @param sig The digital signature part of the permit or null if
     * clearing the permit.
     * @param type An value specifying the type of the permit. The legal
     * values are 1 (subclass permit), 2 (class resource access permit) or
     * 3 (class resource access via reflection permit).
     * @exception IllegalArgumentException if type is not one of the legal values.
     * @return true if an existing permit was overwritten
     */
    private boolean setPermit(Object grantor, byte sig[], int type) {
        HashMap permits;
        Object key;
        switch (type) {
        case 1: permits = subclass_permits; key = (Integer)grantor; break;
        case 2: permits = class_resource_access_permits; key = (Integer)grantor; break;
        case 3: permits = ref_class_resource_access_permits; key = (String)grantor; break;
        default: throw new IllegalArgumentException(type + " is not a valid permit type");
        }
        if (sig == null)
            return (permits.remove(grantor) != null);
        else
            return (permits.put(grantor,sig) != null);
    }

    /**
     * Set a permit for this class to subclass/implement a given
     * superclass/interface. This method can also be used to clear the permit.
     * @param grantor The class name of the superclass/interface.
     * @param sig The digital signature
     * @exception IllegalArgumentException if the given class does not have an
     * entry in the original class's constant pool.
     * @return true if an existing permit was overwritten
     */
    public boolean setSubclassPermit(String grantor, byte sig[]) {
        String interfaces[] = clazz.getInterfaceNames();
        int index = -1;
        if (grantor.equals(clazz.getSuperclassName()))
            // Subclass is denoted by number of interfaces.
            index = interfaces.length;
        else {
            for (int i = 0; i != interfaces.length; ++i)
                if (interfaces[i].equals(grantor)) {
                    index = i;
                    break;
                }
        }

        if (index != -1)
            return setPermit(new Integer(index),sig,1);
        else
            throw new IllegalArgumentException(grantor + " does not exist in constant pool");
    }

    /**
     * Set a permit for this class to access a class resource of a given class.
     * This method can also be used to clear the permit.
     * @param grantor The class name of the superclass/interface.
     * @param sig The digital signature
     * @param mustExist If true, ensure that the grantor exists in the
     * original class's constant pool.
     * @exception IllegalArgumentException if mustExist is true and
     * the given class does not have an entry in the original class's
     * constant pool.
     * @return true if an existing permit was overwritten
     */
    public boolean setClassResourceAccessPermit(String grantor, byte sig[],
        boolean mustExist)
    {
        int index = originalCP.lookupClass(grantor);
        if (index != -1)
            return setPermit(new Integer(index),sig,2);
        else {
            if (mustExist)
                throw new IllegalArgumentException(grantor + " does not exist in constant pool");
            else
                // This is a class resource access-via-reflection permit
                return setPermit(grantor,sig,3);
        }
    }

    /**
     * Return the digital signature representing a permit this class has
     * to subclass or access a class resource of another class.
     * @param grantor If type is 1 or 2, this must be a Integer object
     * which is the constant pool index of the class granting the permit.
     * Otherwise, it must be a String which is the signature of the
     * class granting the permit.
     * @param type An value specifying the type of the permit. The legal
     * values are 1 (subclass permit), 2 (class resource access permit) or
     * 3 (class resource access via reflection permit).
     * @exception IllegalArgumentException if type is not one of the legal values.
     * @return the digital signature or null
     */
    private byte[] getPermit(Object grantor, int type) {
        HashMap permits;
        Object key;
        switch (type) {
        case 1: permits = subclass_permits; key = (Integer)grantor; break;
        case 2: permits = class_resource_access_permits; key = (Integer)grantor; break;
        case 3: permits = ref_class_resource_access_permits; key = (String)grantor; break;
        default: throw new IllegalArgumentException(type + " is not a valid permit type");
        }
        return (byte[])permits.get(key);
    }

    /**
     * Return the digital signature representing a permit this class has
     * to subclass/implement a given superclass/interface.
     * @param grantor The class name of a superclass/interface.
     * @return the bytes of the permit or null if no such permit exists
     */
    public byte[] getSubclassPermit(String grantor) {
        String interfaces[] = clazz.getInterfaceNames();
        int index = -1;
        if (grantor.equals(clazz.getSuperclassName()))
            // Subclass is denoted by number of interfaces.
            index = interfaces.length;
        else {
            for (int i = 0; i != interfaces.length; ++i)
                if (interfaces[i].equals(grantor)) {
                    index = i;
                    break;
                }
        }
        if (index != -1)
            return getPermit(new Integer(index),1);
        else
            return null;
    }

    /**
     * Return the digital signature representing a permit this class has
     * to access a class resource of a given class.
     * @param grantor The name of the class being accessed.
     * @return the bytes of the permit or null if no such permit exists
     */
    public byte[] getClassResourceAccessPermit(String grantor) {
        int index = originalCP.lookupClass(grantor);
        if (index != -1)
            return getPermit(new Integer(index),2);
        else
            // Could be a class resource access via reflection permit
            return getPermit(grantor,3);
    }

    /**
     * Clear all the current permits.
     */
    public void clearPermits() {
        subclass_permits.clear();
        class_resource_access_permits.clear();
        ref_class_resource_access_permits.clear();
    }

    /**
     * Helper method for serializing the contents of the Trusted attribute.
     * @param dos The stream to serialize to.
     * Trusted attribute which specify the package permit.
     */
    private void serializeTrusted(DataOutputStream dos, byte[][] domainKeys,
            byte[][] domainSignatures)
                throws IOException {
        // first create the trusted constant pool
        ConstantPoolGen sp = new ConstantPoolGen();
        int csp = sp.addUtf8(csp_identifier);
        int s_key = (subclass_key == null ? 0 : sp.addPublicKey(subclass_key));
        int cra_key = (class_resource_access_key == null ? 0 : sp.addPublicKey(class_resource_access_key));
        int fields[], methods[];
        Trusted.Permit s_permits[], cra_permits[], r_cra_permits[];
        s_permits = hashMapToPermits(subclass_permits,sp,false);
        cra_permits = hashMapToPermits(class_resource_access_permits,sp,false);
        r_cra_permits = hashMapToPermits(ref_class_resource_access_permits,sp,true);

        // now serialize
        sp.getFinalConstantPool().dump(dos);
        dos.writeInt(cp_extra_entry_offset);
        dos.writeShort(csp);
        dos.writeShort(access_flags);
        dos.writeShort(s_key);
        dos.writeShort(cra_key);
        dos.writeByte(default_field_accessibility ? 1 : 0);
        dos.writeShort(non_default_fields.size());
        for (Iterator i = non_default_fields.iterator(); i.hasNext();)
            dos.writeShort(((Integer)i.next()).intValue());
        dos.writeByte(default_method_accessibility? 1 : 0);
        dos.writeShort(non_default_methods.size());
        for (Iterator i = non_default_methods.iterator(); i.hasNext();)
            dos.writeShort(((Integer)i.next()).intValue());

        dos.writeShort(s_permits.length);
        dos.writeShort(cra_permits.length);
        dos.writeShort(r_cra_permits.length);

        for (int i = 0; i != s_permits.length; ++i)
            s_permits[i].dump(dos);

        for (int i = 0; i != cra_permits.length; ++i)
            cra_permits[i].dump(dos);

        for (int i = 0; i != r_cra_permits.length; ++i)
            r_cra_permits[i].dump(dos);

        if (domainKeys != null) {
            dos.writeShort(domainKeys.length);
            for (int i = 0; i != domainKeys.length; i++) {
                dos.writeShort(domainKeys[i].length);
                dos.write(domainKeys[i]);
                dos.writeShort(domainSignatures[i].length);
                dos.write(domainSignatures[i]);
            }
        }
        dos.flush();
    }

    /**
     * Get the class access flags.
     * @return The new value of the class access flags.
     */
    public int getClassAccessFlags() {
        return access_flags;
    }

    /**
     * Return the contents that are to be signed by a class granting an
     * class resource access privilege to this class.
     */
    public byte[] getClassResourceAccessPermitInput() {
        return originalClass;
    }

    /**
     * Return the contents that are to be signed by a class granting a
     * subclassing privilege to this class.
     */
    public byte[] getSubclassPermitInput() {
        return originalClass;
    }

    /**
     * Return the serialized contents of Trusted that are part of the
     * input to the signature representing the domain permit(s). This
     * includes all items of the Trusted attribute except for the last two
     * items (i.e. domains_count and domains).
     * After calling this method, no further changes should be made to this
     * object except to set the domain permit(s) which are based on the
     * content returned by this method.
     */
    public byte[] getDomainsSignatureInput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        /*
         * serialize the contents of the Trusted attribute except for the
         * package permit.
         */
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            serializeTrusted(dos,null,null);
        } catch (IOException ioe) {
            System.err.println("Non-expected IOException in getDomainsSignatureInput");
            ioe.printStackTrace(System.err);
        }
        return baos.toByteArray();
    }

    /**
     * Return a handle to the original JavaClass.
     * @return the JavaClass object passed to the constructor.
     */
    public JavaClass getOriginalClass() {
        return clazz;
    }

    /**
     * Return the Trusted attribute (if any) that was in the original class.
     */
    public Trusted getOriginalTrusted() {
        return clazz.getTrustedAttribute();
    }

    public void verify() throws VerifyError {
            int standardClassFlags = clazz.getAccessFlags();

            // Subclass key must be null if the class is final
            /* 22nd Jan, 2002: This is not really an error - it only adds
             * a little inefficiency to a class
            if (((standardClassFlags & Constants.ACC_FINAL) != 0) && (subclass_key != null))
                throw new VerifyError("Final class cannot have a subclass key");
             */

            // Instantiation key and flag consistency
            if (((access_flags & Trusted.TACC_CLASS_RESOURCE_ACCESS) != 0) &&
             (class_resource_access_key != null))
                throw new VerifyError("Class resource access key must be null if privileged class resource access is not enforced");

            // Ensure non_default_fields entries are valid.
            Field fields[] = clazz.getFields();
            int pos = 0;
            for (Iterator i = non_default_fields.iterator(); i.hasNext(); pos++) {
                int fIndex = ((Integer)i.next()).intValue();
                if (fIndex < 0 || fIndex >= fields.length)
                    throw new VerifyError("Entry "+pos+" in non_default_fields is out of range");
                Field field = fields[fIndex];
                if (!(field.isPublic() || field.isProtected()))
                    throw new VerifyError("Entry "+pos+" in non_default_fields does not represent a public or protected field");
            }

            // Ensure non_default_methods entries are valid.
            Method methods[] = clazz.getMethods();
            pos = 0;
            for (Iterator i = non_default_methods.iterator(); i.hasNext(); pos++) {
                int fIndex = ((Integer)i.next()).intValue();
                if (fIndex < 0 || fIndex >= methods.length)
                    throw new VerifyError("Entry "+pos+" in non_default_methods is out of range");
                Method method = methods[fIndex];
                if (!(method.isPublic() || method.isProtected()))
                    throw new VerifyError("Entry "+pos+" in non_default_methods does not represent a public or protected method");
            }

            // Ensure subclass_permits entries are valid.
            int ifaces[] = clazz.getIntInterfaces();
            pos = 0;
            boolean hasSubclassPermit = false;
            for (Iterator i = subclass_permits.keySet().iterator(); i.hasNext(); pos++) {
                int index = ((Integer)i.next()).intValue();
                if (index < 0 || index > ifaces.length)
                    throw new VerifyError("Entry "+pos+" in subclass_permits is invalid");
                hasSubclassPermit |= (index == ifaces.length);
            }

            // Ensure class_resource_access_permits entries are valid.
            pos = 0;
            for (Iterator i = class_resource_access_permits.keySet().iterator(); i.hasNext(); pos++) {
                int index = ((Integer)i.next()).intValue();
                if (originalCP.getConstant(index).getTag() != Constants.CONSTANT_Class)
                    throw new VerifyError("Entry "+pos+" in class_resource_access_permits is invalid");
            }
    }

    /**
     * Given a signature of the original class and the contents of the Trusted
     * attribute (apart from the last 2 fields), complete the attribute,
     * insert it into the class and return the resulting JavaClass object.
     * @param package_permit
     * @param verify
     */
    public JavaClass getTrustedClass(byte[][] domainKeys,
            byte[][] domainSignatures, boolean verify)
            throws VerifyError
    {
        if (verify) {
            verify();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        /*
         * serialize the contents of the Trusted attribute including the
         * given package permit.
         */
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            serializeTrusted(dos,domainKeys,domainSignatures);
        } catch (IOException ioe) {
            // should never happen
            ioe.printStackTrace();
        }
        byte[] asBytes = baos.toByteArray();

        ClassGen cg = new ClassGen(clazz);
        ConstantPoolGen cp = cg.getConstantPool();

        Trusted trusted = null;
        try {
            trusted = new Trusted(cp.addUtf8("Trusted"), asBytes.length,
                new DataInputStream(new ByteArrayInputStream(asBytes)),
                cp.getConstantPool());
        } catch (IOException ioe) {
            // should never happen
            ioe.printStackTrace();
        }

        // remove any existing Trusted attribute
        Trusted old = (Trusted)Attribute.getAttribute(cg.getAttributes(),
            Constants.ATTRIBUTE_NAMES[Constants.ATTR_TRUSTED]);
        if (old != null)
            cg.removeAttribute(old);
        cg.addAttribute(trusted);
        return cg.getJavaClass();
    }
}

/**
 * This is a helper class to simply count the bytes written to an
 * OutputStream. We use it to calculate the size of the constant pool
 * inside the Trusted attribute as well as the cp_extra_entry_offset item
 * of the Trusted attribute.
 */
class CountOutputStream extends OutputStream {
    // this the total number of bytes written
    private int size = 0;
    // overwrite the methods of OutputStream
    public void write(byte[] b) {  size += b.length; }
    public void write(byte[] b, int off, int len) { size += len; }
    public void write(int b) { ++size; }
    int getSize() { return size; }
}
