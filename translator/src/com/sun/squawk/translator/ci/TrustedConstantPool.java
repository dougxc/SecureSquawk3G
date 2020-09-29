//if[TRUSTED]
/* This will selectively exclude the entire file from the build */
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;


/**
 * This class extends the <code>constant_pool</code> concept to include an
 * augmented constant_pool for use with the "Trusted" attribute in the
 * secure Squawk JVM.
 *
 * @author   Andrew Crouch
 */
public class TrustedConstantPool extends ConstantPool
{

    /**
     * Contant pool tag value denoting a PublicKey value. This is an extension
     * to the constants defined in the JVM specification.
     */
    public static final int CONSTANT_PublicKey = 13;

    /**
     * Contant pool tag value denoting a DigitalSignature value. This is an
     * extension to the constants defined in the JVM specification.  Note
     * that this conflicts with CONSTANT_Object.
     */
    public static final int CONSTANT_DigitalSignature = 14;

    /**
     * {@inheritDoc}
     *
     */
    public TrustedConstantPool(ClassFileReader cfr, Klass definedClass)
    {
        super(cfr, definedClass);
    }

    /**
     * Gets the PublicKey encoded constant value at a given index.
     *
     * @param   index  the index of a PublicKey encoded constant in the pool
     * @return  the value of the PublicKey encoded constant at <code>index</code>
     */
    public byte[] getPublicKey(int index) {
        verifyEntry(index, CONSTANT_PublicKey);
        return (byte[])entries[index];
    }

    /**
     * Gets the DigitalSignature encoded constant value at a given index.
     *
     * @param   index  the index of a DigitalSignature encoded constant in the pool
     * @return  the value of the DigitalSignature encoded constant at <code>index</code>
     */
    public byte[] getDigitalSignature(int index) {
        verifyEntry(index, CONSTANT_DigitalSignature);
        return (byte[])entries[index];
    }



    /**
     * The ConstantPool constructor will call loadConstantPool.  Since the variable
     * types are overlapping (type 14), an alternate handler is required.
     */
    protected void loadConstantPool() {

        int count = getSize();
        /*
         * Read the constant pool entries from the classfile
         * and initialize the constant pool correspondingly.
         * Remember that constant pool indices start from 1
         * rather than 0 and that last index is count-1.
         */

        /*
         * Read in the values
         */
        for (int i = 1; i < count; i++) {
            int tag = cfr.readUnsignedByte("tcp-tag");
            tags[i] = (byte)tag;
            switch (tag) {
                case CONSTANT_Utf8: {
                    entries[i] = cfr.readUTF("CONSTANT_Utf8");
                    break;
                }
                case CONSTANT_PublicKey:
                case CONSTANT_DigitalSignature: {
                    int size = cfr.readUnsignedShort("size");
                    byte[] key = new byte[size];
                    cfr.readFully(key, "key");

                    /* Key or Digital signature is just an array of bytes until it is
                     * decoded by the cryptographic service provider */
                    entries[i] = key;
                    break;
                }

                default: {
                    throw cfr.formatError("invalid trusted constant pool entry");
                }
            }
        }
    }
}
