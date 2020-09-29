//if[TRUSTED]
/* This will selectively exclude the entire file from the build */
/*
 * @(#)RSAKey.java	1.5 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.squawk.csp.key;

import com.sun.squawk.csp.math.BigInteger;

/**
 * The interface to an RSA public or private key.
 *
 * @author Jan Luehe
 * @version 1.5 01/23/03
 *
 * @see RSAPublicKey
 * @see RSAPrivateKey
 *
 * @since 1.3
 */

public interface RSAKey {

    /**
     * Returns the modulus.
     *
     * @return the modulus
     */
    public BigInteger getModulus();

    /**
     * {@inheritDoc}
     *
     */
    public abstract boolean equals(Object o);

    /**
     * {@inheritDoc}
     *
     */
    public abstract int hashCode();

}
