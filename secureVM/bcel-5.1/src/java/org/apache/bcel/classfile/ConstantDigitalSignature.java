package org.apache.bcel.classfile;

import  org.apache.bcel.Constants;
import  java.io.*;

/**
 * This class is derived from the abstract
 * <A HREF="de.fub.bytecode.classfile.Constant.html">Constant</A> class
 * and represents a signature in a secure constant pool.
 *
 * @see     Constant
 */
public final class ConstantDigitalSignature extends Constant {
  private byte[] signature;

  /**
   * Initialize from another object.
   */
  public ConstantDigitalSignature(ConstantDigitalSignature c) {
    this((byte[])c.signature.clone());
  }

  /**
   * Initialize instance from file data.
   *
   * @param file Input stream
   * @throw IOException
   */
  ConstantDigitalSignature(DataInputStream file) throws IOException
  {
    super(Constants.CONSTANT_DigitalSignature);
    int length = (int)file.readUnsignedShort();
    signature = new byte[length];
    file.read(signature);
  }

  /**
   * @param signature the signature data
   */
  public ConstantDigitalSignature(byte[] signature)
  {
    super(Constants.CONSTANT_DigitalSignature);
    this.signature = signature;
  }

  /**
   * Called by objects that are traversing the nodes of the tree implicitely
   * defined by the contents of a Java class. I.e., the hierarchy of methods,
   * fields, attributes, etc. spawns a tree of objects.
   *
   * @param v Visitor object
   */
  public void accept(Visitor v) {
    v.visitConstantDigitalSignature(this);
  }
  /**
   * Dump name and signature index to file stream in binary format.
   *
   * @param file Output file stream
   * @throw IOException
   */
  public final void dump(DataOutputStream file) throws IOException
  {
    file.writeByte(tag);
    file.writeShort(signature.length);
    file.write(signature,0,signature.length);
  }
  /**
   * @return signature data
   */
  public final byte[] getSignature()      { return signature; }
  /**
   * @return String representation
   */
  public final String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(super.toString() + "(length = " + signature.length +
      ", signature = ");
    for (int i = 0; i != signature.length; i++)
      buf.append(Integer.toHexString(signature[i]&0xFF)+" ");
    buf.append(")");
    return buf.toString();
  }
}
