//if[TRUSTED]
/* This will selectively exclude the entire file from the build */
/*
 * @(#)PublicKey.java	1.31 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.squawk.csp.key;

/**
 * <p>A public key. This interface contains no methods or constants.
 * It merely serves to group (and provide type safety for) all public key
 * interfaces.
 *
 * Note: The specialized public key interfaces extend this interface.
 * See, for example, the DSAPublicKey interface in
 * <code>java.security.interfaces</code>.
 *
 * @see Key
 * @see PrivateKey
 * @see Certificate
 * @see Signature#initVerify
 * @see java.security.interfaces.DSAPublicKey
 * @see java.security.interfaces.RSAPublicKey
 *
 * @version 1.31 03/01/23
 */

public interface PublicKey extends Key {
    // Declare serialVersionUID to be compatible with JDK1.1
    /**
     * The class fingerprint that is set to indicate serialization
     * compatibility with a previous version of the class.
     */
    static final long serialVersionUID = 7187392471159151072L;
}
