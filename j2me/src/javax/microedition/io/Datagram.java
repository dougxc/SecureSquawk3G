/*
 *  Copyright (c) 1999-2001 Sun Microsystems, Inc., 901 San Antonio Road,
 *  Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 *  Sun Microsystems, Inc. has intellectual property rights relating
 *  to the technology embodied in this software.  In particular, and
 *  without limitation, these intellectual property rights may include
 *  one or more U.S. patents, foreign patents, or pending
 *  applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 *  and all Sun-based and Java-based marks are trademarks or
 *  registered trademarks of Sun Microsystems, Inc.  in the United
 *  States and other countries.
 *
 *  This software is distributed under licenses restricting its use,
 *  copying, distribution, and decompilation.  No part of this
 *  software may be reproduced in any form by any means without prior
 *  written authorization of Sun and its licensors, if any.
 *
 *  FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 *  Subject to Standard License Terms and Conditions
 */

package javax.microedition.io;

import java.io.*;

/**
 * This is the generic datagram interface. It represents an object that
 * will act as the holder of data to be sent or received from a datagram
 * connection.
 * <p>
 * The DataInput and DataOutput interfaces are extended by this interface
 * to provide a simple way to read and write binary data in and out of
 * the datagram buffer. An additional function reset() may be called to
 * reset the read/write point to the beginning of the buffer.
 * <p>
 * It should be noted that in the interests of reducing space and speed
 * concerns, these mechanisms are very simple. In order to use them
 * correctly the following restrictions should be observed:
 * <p>
 * 1) The use of the standard DataInput and DataOutput interfaces is
 * done in order to provide a familiar API for reading and writing
 * data into and out of a Datagram buffer.
 * It should be understood however that this is not an API to a Java
 * stream and does not exhibit all of the features normally associated
 * with one. The most important difference here is that a Java stream
 * is either an InputStream or an OutputStream. The interface presented
 * here is, essentially, both at the same time. As the datagram object
 * does not have a mode for reading and writing, it is necessary for the
 * application programmer to realize that no automatic detection of the
 * wrong mode usage can be done.
 * <p>
 * 2) The DataInput and DataOutput interfaces will not work with any
 * arbitrary settings of the Datagram state variables. The main
 * restriction here is that the <I>offset</I> state variable must
 * at all times be zero. Datagrams may be used in the normal way
 * where the offset is non-zero but when this is done the DataInput
 * and DataOutput interfaces cannot be used.
 * <p>
 * 3) The DataInput and DataOutput read() and write() functions work
 * by using an invisible state variable of the Datagram object. Before
 * any data is read from or written to the datagram buffer, this state
 * variable must be zeroed using the reset() function. This variable
 * is not the <I>offset</I> state variable but an additional state
 * variable used only for the read() and write() functions.
 * <p>
 * 4) Before data is to be received into the datagram's buffer, the
 * <I>offset</I> state variable and the <I>length</I> state variable
 * must first be set up to the part of the buffer the data should be
 * written to. If the intention is to use the read() functions, the
 * offset must be zero. After receive() is called, the data can be
 * read from the buffer using the read() functions until an EOF
 * condition is found. This will occur when the number of characters
 * represented by the length parameter have been read.
 * <p>
 * 5) To write data into the buffer prior to a send() operation,
 * the reset() function should first be called.  This will zero the
 * read/write pointer along with the offset and length parameters
 * of the Datagram object. Then the data can be written using the
 * write() functions. When this process is complete, the <I>length</I>
 * state variable will be set to the correct value for the send()
 * function of the datagram's connection, and so the send operation
 * can take place. An IndexOutOfBoundsException will be thrown if
 * the number of characters written exceeds the size of the buffer.
 *
 * @author  Brian Modra
 * @author  Nik Shaylor
 * @version 1.2 1/17/2000
 */
public interface Datagram extends DataInput, DataOutput {

    /**
     * Get the address in the datagram.
     *
     * @return the address in string form, or null if no address was set
     *
     * @see #setAddress
     */
    public String getAddress();

    /**
     * Get the buffer.
     *
     * @return the data buffer
     *
     * @see #setData
     */
    public byte[] getData();

    /**
     * Get the length.
     *
     * @return the length of the data
     *
     * @see #setLength
     */
    public int getLength();

    /**
     * Get the offset.
     *
     * @return the offset into the data buffer
     */
    public int getOffset();

    /**
     * Set datagram address.
     * <p>
     * The actual addressing scheme is implementation-dependent.
     * Please read the general comments on datagram addressing
     * in <I>DatagramConnection.java</I>.
     * <p>
     * Note that if the address of a datagram is not specified, then it
     * defaults to that of the connection.
     *
     * @param addr the new target address as a URL
     * @exception IllegalArgumentException if the address is not valid
     * @exception IOException if a some kind of I/O error occurs
     *
     * @see #getAddress
     */
    public void setAddress(String addr) throws IOException;

    /**
     * Set datagram address, copying the address from another datagram.
     *
     * @param reference the datagram who's address will be copied as
     * the new target address for this datagram.
     * @exception IllegalArgumentException if the address is not valid
     *
     * @see #getAddress
     */
    public void setAddress(Datagram reference);

    /**
     * Set the length.
     *
     * @param len the new length of the data
     * @exception IllegalArgumentException if the length is negative
     * or larger than the buffer
     *
     * @see #getLength
     */
    public void setLength(int len);

    /**
     * Set the buffer, offset and length.
     *
     * @param buffer the data buffer
     * @param offset the offset into the data buffer
     * @param len the length of the data in the buffer
     * @exception IllegalArgumentException if the length or offset
     *                                     fall outside the buffer
     *
     * @see #getData
     */
    public void setData(byte[] buffer, int offset, int len);

    /**
     * Zero the read/write pointer as well as the offset and
     * length parameters.
     */
    public void reset();

}


