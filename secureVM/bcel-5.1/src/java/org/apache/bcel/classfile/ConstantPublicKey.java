package org.apache.bcel.classfile;

import  org.apache.bcel.Constants;
import  java.io.*;

/**
 * This class is derived from the abstract
 * <A HREF="de.fub.bytecode.classfile.Constant.html">Constant</A> class
 * and represents a public key in a secure constant pool.
 *
 * @see     Constant
 */
public final class ConstantPublicKey extends Constant {
  private byte[] key;

  /**
   * Initialize from another object.
   */
  public ConstantPublicKey(ConstantPublicKey c) {
    this((byte[])c.key.clone());
  }

  /**
   * Initialize instance from file data.
   *
   * @param file Input stream
   * @throw IOException
   */
  ConstantPublicKey(DataInputStream file) throws IOException
  {
    super(Constants.CONSTANT_PublicKey);
    int length = (int)file.readUnsignedShort();
    key = new byte[length];
    file.read(key);
  }

  /**
   * @param key the key data
   */
  public ConstantPublicKey(byte[] key)
  {
    super(Constants.CONSTANT_PublicKey);
    this.key = key;
  }

  /**
   * Called by objects that are traversing the nodes of the tree implicitely
   * defined by the contents of a Java class. I.e., the hierarchy of methods,
   * fields, attributes, etc. spawns a tree of objects.
   *
   * @param v Visitor object
   */
  public void accept(Visitor v) {
    v.visitConstantPublicKey(this);
  }
  /**
   * Dump name and key index to file stream in binary format.
   *
   * @param file Output file stream
   * @throw IOException
   */
  public final void dump(DataOutputStream file) throws IOException
  {
    file.writeByte(tag);
    file.writeShort(key.length);
    file.write(key,0,key.length);
  }
  /**
   * @return key data
   */
  public final byte[] getKey()      { return key; }
  /**
   * @return String representation
   */
  public final String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(super.toString() + "(length = " + key.length +
      ", key = ");
    for (int i = 0; i != key.length; i++)
      buf.append(Integer.toHexString(key[i]&0xFF)+" ");
    buf.append(")");
    return buf.toString();
  }
}
