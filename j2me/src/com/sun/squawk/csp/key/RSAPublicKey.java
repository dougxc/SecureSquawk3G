//if[TRUSTED]
/* This will selectively exclude the entire file from the build */
/*
 * @(#)RSAPublicKey.java	1.8 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.squawk.csp.key;

import com.sun.squawk.csp.math.BigInteger;

/**
 * The interface to an RSA public key.
 *
 * @author Jan Luehe
 *
 * @version 1.8 03/01/23
 */

public interface RSAPublicKey extends PublicKey, RSAKey
{
    /**
     * Returns the public exponent.
     *
     * @return the public exponent
     */
    public BigInteger getPublicExponent();
}
