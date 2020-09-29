package org.apache.bcel.classfile;

import  org.apache.bcel.Constants;
import  java.io.*;
import  java.util.Vector;

/**
 * This class is derived from <em>Attribute</em> and denotes that this class
 * is a trusted class as well as containing all the info representing the
 * Trust Certificate that enables it as a trusted class.
 * It is instantiated from the <em>Attribute.readAttribute()</em> method.
 *
 * @author  Doug Simon
 * @see     Attribute
 */
public final class Trusted extends Attribute {

  /*
   * Access flags for a trusted class.
   */
  public static final byte TACC_SUBCLASS              = 0x01;
  public static final byte TACC_CLASS_RESOURCE_ACCESS = 0x02;
  public static final byte TACC_EXCEPTION             = 0x04;

  /*
   * Names of above flags.
   */
  public static final String TACC_SUBCLASS_NAME = "TACC_SUBCLASS";
  public static final String TACC_CLASS_RESOURCE_ACCESS_NAME = "TACC_CLASS_RESOURCE_ACCESS";
  public static final String TACC_EXCEPTION_NAME = "TACC_EXCEPTION";

  public static final String[] flagsToStringArray(int flag) {
    if (flag == 0)
        return null;
    Vector v = new Vector(3);
    if ((flag & TACC_SUBCLASS) != 0)
        v.add(TACC_SUBCLASS_NAME);
    if ((flag & TACC_CLASS_RESOURCE_ACCESS) != 0)
        v.add(TACC_CLASS_RESOURCE_ACCESS_NAME);
    if ((flag & TACC_EXCEPTION) != 0)
        v.add(TACC_EXCEPTION_NAME);
    String[] result = new String[v.size()];
    return (String[])v.toArray(result);
  }

  public static final int stringToFlag(String name) {
    if (name.equals(TACC_SUBCLASS_NAME))
        return TACC_SUBCLASS;
    else if (name.equals(TACC_CLASS_RESOURCE_ACCESS_NAME))
        return TACC_CLASS_RESOURCE_ACCESS;
    else if (name.equals(TACC_EXCEPTION_NAME))
        return TACC_EXCEPTION;
    else
        throw new IllegalArgumentException();
  }

  //=========================== Permit class ==============================

  /**
   * A permit encapsulates a class and digital signature pair. The class is
   * represented as an index to a ConstantClass entry in a ConstantPool and
   * the digital signature is represented as an index to a
   * ConstantDigitalSignature entry in a constant pool.
   *
   * Note that even though this class implements Cloneable, it doesn't
   * override the default implementation of Object.clone as the semantics
   * of that implementation are sufficient for this class.
   */
  public static class Permit implements Cloneable {

    private int class_index;
    private int sig_index;

    /**
     * Construct from given values.
     * @param class_index The index of a ConstantClass entry in a
     * ConstantPool.
     * @param sig_index The index of a ConstantDigitalSignature entry in a
     * ConstantPool.
     */
    public Permit(int class_index, int sig_index) {
      this.class_index = class_index;
      this.sig_index = sig_index;
    }

    /**
     * Construct from a classfile.
     */
    public Permit(DataInputStream file) throws IOException {
      class_index = file.readUnsignedShort();
      sig_index = file.readUnsignedShort();
    }

    /**
     * Utility method to deep-clone an array of Permits.
     * @param permits The array to clone.
     */
    public static Permit[] clone(Permit permits[]) {
        Permit result[] = new Permit[permits.length];
        for (int i = 0; i != permits.length; ++i)
            try {
                result[i] = (Permit)permits[i].clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

        return result;
    }

    /**
     * Utility method for searching an array of Permits for a class
     * corresponding to a given digital signature.
     * @param permits The array of Permits to search.
     * @param sigIndex The digital signature index to search on.
     * @return the index of the corresponding class entry or -1
     * if no such index was found.
     */
    static public int lookupClassForSig(Permit[] permits, int sigIndex) {
        for(int i = 0; i != permits.length; ++i) {
            if (permits[i].sig_index == sigIndex)
                return permits[i].class_index;
        }
        return -1;
    }

    /**
     * Utility method for searching an array of Permits for a digital
     * signature corresponding to a given class.
     * @param permits The array of Permits to search.
     * @param classIndex The class index to search on.
     * @return the index of the corresponding digital signature entry or -1
     * if no such index was found.
     */
    static public int lookupSigForClass(Permit[] permits, int classIndex) {
        for(int i = 0; i != permits.length; ++i) {
            if (permits[i].class_index == classIndex)
                return permits[i].sig_index;
        }
        return -1;
    }

    /**
     * Write out to a classfile.
     * @param file The output stream to which the permit should be dumped.
     */
    public void dump(DataOutputStream file) throws IOException {
        file.writeShort(class_index);
        file.writeShort(sig_index);
    }

    /**
     * Convert subclass permit to a String.
     * @param interfaceNames
     */
    public String toString(String interfaceNames[],String superClassName) {
        if (interfaceNames != null) {
            String name = (class_index == interfaceNames.length ?
                            superClassName + "(superclass)" : interfaceNames[class_index]);
            return name+", sig_index="+sig_index;
        }
        else
            return "class_index="+class_index+", sig_index="+sig_index;
    }

    /**
     * Convert to a String.
     * @param cp If non-null, this ConstantPool is used to access the class
     * name when converting this permit to a user readable string.
     */
    public String toString(ConstantPool cp) {
        if (cp != null) {
            return cp.constantToString(cp.getConstant(class_index))+
                ", sig_index="+sig_index;
        }
        return "class_index="+class_index+", sig_index="+sig_index;
    }

    /**
     * Return the class index of this Permit.
     * @return the class index of this Permit.
     */
    public int getClassIndex() { return class_index; }

    /**
     * Return the digital signature index of this Permit.
     * @return the digital signature index of this Permit.
     */
    public int getSigIndex() { return sig_index; }
  }

  //============================== Domain ==================================

  /**
   * A Domain encapsulates a public key and a digital signature
   * (both with their contents directly inlined) which represent a class's
   * membership in a domain.
   */
  public static class Domain {

    private byte[] key;
    private byte[] signature;

    /**
     * Construct from given values.
     * @param key
     * @param signature
     */
    public Domain(byte[] key, byte[] signature) {
        this.key = key;
        this.signature = signature;
    }

    /**
     * Construct from a classfile.
     */
    public Domain(DataInputStream file) throws IOException {
      int length = file.readUnsignedShort();
      key = new byte[length];
      file.read(key);
      length = file.readUnsignedShort();
      signature = new byte[length];
      file.read(signature);
    }

    public byte[] getKey() { return key; }
    public byte[] getSignature() { return signature; }

    /**
     * Write out to a classfile.
     * @param file The output stream to which the domain should be dumped.
     */
    public void dump(DataOutputStream file) throws IOException {
        file.writeShort(key.length);
        file.write(key);
        file.writeShort(signature.length);
        file.write(signature);
    }
    /**
     * Convert to a String.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("\tkey=");
        for (int i = 0; i!= key.length; i++)
          buf.append(Integer.toHexString(key[i]&0xFF)+" ");
        buf.append("\n\tsignature=");
        for (int i = 0; i!= signature.length; i++)
          buf.append(Integer.toHexString(signature[i]&0xFF)+" ");
        return buf.toString();
    }
  }

  //=========================== Trusted class ==============================

  /**
   * These are the fields of the Trusted class.
   */
  public int cp_extra_entry_offset;
  public int csp_identifier;
  public int access_flags;
  public ConstantPool secure_pool;
  public int subclass_key;
  public int class_resource_access_key;
  public boolean default_field_accessibility;
  public int[] non_default_fields;
  public boolean default_method_accessibility;
  public int[] non_default_methods;
  public Permit[] subclass_permits;
  public Permit[] class_resource_access_permits;
  public Permit[] ref_class_resource_access_permits;
  public Domain[] domains;

  /**
   * Construct a Trusted attribute.
   */
  public Trusted(int name_index, int length,
          ConstantPool secure_pool,
          int cp_extra_entry_offset,
          int csp_identifier,
          int access_flags,
          int subclass_key,
          int class_resource_access_key,
          boolean default_field_accessibility,
          boolean default_method_accessibility,
          int[] non_default_fields,
          int[] non_default_methods,
          Permit[] subclass_permits,
          Permit[] class_resource_access_permits,
          Permit[] ref_class_resource_access_permits,
          Domain[] domains,
          ConstantPool constant_pool)
  {
    super(Constants.ATTR_TRUSTED, name_index, length, constant_pool);
    this.cp_extra_entry_offset = cp_extra_entry_offset;
    this.csp_identifier = csp_identifier;
    this.access_flags = access_flags;
    this.secure_pool = secure_pool;
    this.subclass_key = subclass_key;
    this.class_resource_access_key = class_resource_access_key;
    this.default_field_accessibility = default_field_accessibility;
    if (non_default_fields == null)
      this.non_default_fields = new int[0];
    else
      this.non_default_fields = non_default_fields;
    this.default_method_accessibility = default_method_accessibility;
    if (non_default_methods == null)
      this.non_default_methods = new int[0];
    else
      this.non_default_methods = non_default_methods;
    if (subclass_permits == null)
      this.subclass_permits = new Permit[0];
    else
      this.subclass_permits = subclass_permits;
    if (class_resource_access_permits == null)
      this.class_resource_access_permits = new Permit[0];
    else
      this.class_resource_access_permits = class_resource_access_permits;
    if (ref_class_resource_access_permits == null)
      this.ref_class_resource_access_permits = new Permit[0];
    else
      this.ref_class_resource_access_permits = ref_class_resource_access_permits;
    if (domains == null)
      this.domains = new Domain[0];
    else
      this.domains = domains;
  }

  /**
   * Construct object from file stream.
   *
   * @param name_index Index in constant pool to CONSTANT_Utf8
   * @param length Content length in bytes
   * @param file Input stream
   * @param constant_pool Array of constants
   * @throw IOException
   */
  public Trusted(int name_index, int length, DataInputStream file,
               ConstantPool constant_pool) throws IOException
  {
    super(Constants.ATTR_TRUSTED, name_index, length, constant_pool);
    int start = file.available();
    secure_pool = new ConstantPool(file);
    cp_extra_entry_offset = file.readInt();
    csp_identifier = file.readUnsignedShort();
    access_flags = file.readUnsignedShort();
    subclass_key = file.readUnsignedShort();
    class_resource_access_key = file.readUnsignedShort();

    // Fields accessibility.
    default_field_accessibility = (file.readByte() != 0);
    int non_default_fields_count = file.readUnsignedShort();
    non_default_fields =
        new int[non_default_fields_count];
    for (int i = 0; i != non_default_fields_count; i++)
      non_default_fields[i] = file.readUnsignedShort();

    // Methods accessibility.
    default_method_accessibility = (file.readByte() != 0);
    int non_default_methods_count = file.readUnsignedShort();
    non_default_methods =
        new int[non_default_methods_count];
    for (int i = 0; i != non_default_methods_count; i++)
      non_default_methods[i] = file.readUnsignedShort();

    int subclass_permits_count = file.readUnsignedShort();
    int class_resource_access_permits_count = file.readUnsignedShort();
    int ref_class_resource_access_permits_count = file.readUnsignedShort();
    subclass_permits = new Permit[subclass_permits_count];
    for (int i = 0; i != subclass_permits_count; i++)
      subclass_permits[i] = new Permit(file);

    class_resource_access_permits = new Permit[class_resource_access_permits_count];
    for (int i = 0; i != class_resource_access_permits_count; i++)
      class_resource_access_permits[i] = new Permit(file);

    ref_class_resource_access_permits = new Permit[ref_class_resource_access_permits_count];
    for (int i = 0; i != ref_class_resource_access_permits_count; i++)
      ref_class_resource_access_permits[i] = new Permit(file);

    int domains_count = file.readUnsignedShort();
    domains = new Domain[domains_count];
    for (int i = 0; i != domains_count; i++)
      domains[i] = new Domain(file);

    if (start - file.available() != length)
        throw new ClassFormatError("Given Trusted attribute length is incorrect: param="+
            length + ", actual="+(start-file.available()));
  }

  /**
   * Return the secure constant pool
   */
  public ConstantPool getTrustedConstantPool() { return secure_pool; }

  /**
   * Return the index of the CSP entry in the secure constant pool.
   */
  public int getCSPIdentifier()    { return csp_identifier; }

  /**
   * Return the secure access flags for the class.
   */
  public int getAccessFlags()           { return access_flags; }

  private byte[] getKeyBytes(int keyIndex) {
    if (keyIndex == 0)
        return null;
    ConstantPublicKey key = (ConstantPublicKey)
        secure_pool.getConstant(keyIndex,Constants.CONSTANT_PublicKey);
    return key.getKey();
  }

  /**
   * Return the bytes of the subclass key or null if there is no such key.
   */
  public byte[] getSubclassKeyBytes() {
    return getKeyBytes(subclass_key);
  }
  /**
   * Return the bytes of the class resource access key or null if there is no such key.
   */
  public byte[] getClassResourceAccessKeyBytes() {
    return getKeyBytes(class_resource_access_key);
  }

  /**
   * Return the index of the subclassing public key in the secure constant
   * pool.
   */
  public int getSubclassKey()      { return subclass_key; }

  /**
   * Return the index of the class resource access public key in the secure constant
   * pool.
   */
  public int getClassResourceAccessKey()   { return class_resource_access_key; }

  /**
   * Return the domains.
   */
  public Domain[] getDomains() { return domains; }

  public int getCPExtraEntryOffset()    { return cp_extra_entry_offset; }

  /**
   * Return a copy (clone) of the subclass permits.
   */
  public Permit[] getSubclassPermits() {
      return Permit.clone(subclass_permits);
  }

  /**
   * Return a copy (clone) of the class resource access permits.
   */
  public Permit[] getClassResourceAccessPermits() {
      return Permit.clone(class_resource_access_permits);
  }

  /**
   * Return a copy (clone) of the reflection class resource access permits.
   */
  public Permit[] getRefClassResourceAccessPermits() {
      return Permit.clone(ref_class_resource_access_permits);
  }

    /**
     * Return the default access for all public and protected fields in this class
     * that don't have an entry in the member_access_policies table.
     */
    public boolean getDefaultFieldAccessibility() { return default_field_accessibility; }

    /**
     * Return the default access for all public and protected methods in this class
     * that don't have an entry in the member_access_policies table.
     */
    public boolean getDefaultMethodAccessibility() { return default_method_accessibility; }

  /**
   * Return a copy (clone) of the field access policies.
   */
  public int[] getFieldsAccessibility() {
      return (int[])non_default_fields.clone();
  }

  /**
   * Return a copy (clone) of the method access policies.
   */
  public int[] getMethodsAccessibility() {
      return (int[])non_default_methods.clone();
  }

  /**
   * Called by objects that are traversing the nodes of the tree implicitely
   * defined by the contents of a Java class. I.e., the hierarchy of methods,
   * fields, attributes, etc. spawns a tree of objects.
   *
   * @param v Visitor object
   */
  public void accept(Visitor v) {
    v.visitTrusted(this);
  }

  /**
   * Dump source file attribute to file stream in binary format.
   *
   * @param file Output file stream
   * @throw IOException
   */
  public final void dump(DataOutputStream file) throws IOException
  {
    super.dump(file);
    int written = file.size();
    secure_pool.dump(file);
    file.writeInt(cp_extra_entry_offset);
    file.writeShort(csp_identifier);
    file.writeShort(access_flags);
    file.writeShort(subclass_key);
    file.writeShort(class_resource_access_key);

    file.writeByte(default_field_accessibility? 1 : 0);
    file.writeShort(non_default_fields.length);
        for (int i = 0; i != non_default_fields.length; i++)
            file.writeShort(non_default_fields[i]);

    file.writeByte(default_method_accessibility? 1 : 0);
    file.writeShort(non_default_methods.length);
        for (int i = 0; i != non_default_methods.length; i++)
            file.writeShort(non_default_methods[i]);

    // Dump permits
    Permit allPermits[][] = { subclass_permits, class_resource_access_permits,
                              ref_class_resource_access_permits };
    for (int p = 0; p != allPermits.length; ++p)
        file.writeShort(allPermits[p].length);
    for (int p = 0; p != allPermits.length; ++p) {
        Permit permits[] = allPermits[p];
        for (int i = 0; i != permits.length; i++)
          permits[i].dump(file);
    }

    // Dump domains
    file.writeShort(domains.length);
    for (int i = 0; i != domains.length; i++)
      domains[i].dump(file);

    int bytesDumped = file.size() - written;
    if (bytesDumped != getLength())
        throw new ClassFormatError("Trusted attribute: length = "+getLength()+
            ", bytes dumped = "+bytesDumped);
  }

  /**
   * @return String representation.
   */
  public String toString() { return toString(null); }

  /**
   * Return a string representation of this attribute.
   * @param clazz This value is used to display more meaningful symbolic
   * representations of values in this attribute (if it is non-null).
   */
  public String toString(JavaClass clazz) {
    StringBuffer buf = new StringBuffer();

    buf.append(secure_pool.toString() + "\n");
    buf.append("cp_extra_entry_offset: "+cp_extra_entry_offset+"\n");
    buf.append("csp_identifier: " + secure_pool.constantToString(csp_identifier,
      Constants.CONSTANT_Utf8) + "\n");
    buf.append("access_flags: 0x"+Integer.toHexString(access_flags).
      toUpperCase()+": ");
    if ((access_flags & TACC_SUBCLASS) != 0)
      buf.append(TACC_SUBCLASS_NAME+" ");
    if ((access_flags & TACC_CLASS_RESOURCE_ACCESS) != 0)
      buf.append(TACC_CLASS_RESOURCE_ACCESS_NAME+" ");
    if ((access_flags & TACC_EXCEPTION) != 0)
      buf.append(TACC_EXCEPTION_NAME+" ");
    buf.append("\n");
    buf.append("subclass_key: "+subclass_key+"\n");
    buf.append("class_resource_access_key: "+class_resource_access_key+"\n");
    buf.append("default_field_accessibility: " + default_field_accessibility+"\n");
    Field[]  fields = (clazz == null ? null : clazz.getFields());
    buf.append("non_default_fields:\n");
    for (int i = 0; i != non_default_fields.length; i++) {
      int index = non_default_fields[i];
      if (fields != null)
        buf.append("\t"+index +": " +fields[index]+"\n");
      else
        buf.append("\t"+index+"\n");
    }
    buf.append("default_method_accessibility: " + default_method_accessibility+"\n");
    Method[] methods = (clazz == null ? null : clazz.getMethods());
    buf.append("non_default_methods:\n");
    for (int i = 0; i != non_default_methods.length; i++) {
      int index = non_default_methods[i];
      if (methods != null)
        buf.append("\t"+index +": " +methods[index]+"\n");
      else
        buf.append("\t"+index+"\n");
    }

    ConstantPool cp = null;
    String[] interfaceNames = null;
    String superClassName = null;
    if (clazz != null) {
        cp = clazz.getConstantPool();
        interfaceNames = clazz.getInterfaceNames();
        superClassName = clazz.getSuperclassName();
    }
    buf.append("subclass_permits ["+subclass_permits.length+"]:\n");
    for (int i = 0; i != subclass_permits.length; i++)
      buf.append("\t"+subclass_permits[i].toString(interfaceNames,superClassName)+"\n");
    buf.append("class_resource_access_permits ["+class_resource_access_permits.length+"]:\n");
    for (int i = 0; i != class_resource_access_permits.length; i++)
        buf.append("\t"+class_resource_access_permits[i].toString(cp)+"\n");
    buf.append("ref_class_resource_access_permits ["+ref_class_resource_access_permits.length+"]:\n");
    for (int i = 0; i != ref_class_resource_access_permits.length; i++)
        buf.append("\t"+class_resource_access_permits[i].toString(secure_pool)+"\n");
    buf.append("domains ["+domains.length+"]:\n");
    for (int i = 0; i != domains.length; i++)
      buf.append(domains[i].toString()+"\n");
    buf.append("\n");
    return buf.toString();
  }

  /**
   * @return deep copy of this attribute
   */
  public Attribute copy(ConstantPool constant_pool) {
    Trusted c = (Trusted)clone();
    return c;
  }
}
