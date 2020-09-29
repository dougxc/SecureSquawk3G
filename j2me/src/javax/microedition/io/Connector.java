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
import com.sun.squawk.io.*;

/**
 * This class is a placeholder for the static methods that are used
 * for creating all the Connection objects.
 * <p>
 * The creation of Connections is performed dynamically by looking
 * up a protocol implementation class whose name is formed from the
 * platform name (read from a system property) and the protocol name
 * of the requested connection (extracted from the parameter string
 * supplied by the application programmer.)
 *
 * The parameter string that describes the target should conform
 * to the URL format as described in RFC 2396.
 * This takes the general form:
 * <p>
 * <code>{scheme}:[{target}][{parms}]</code>
 * <p>
 * where <code>{scheme}</code> is the name of a protocol such as
 * <i>http</i>}.
 * <p>
 * The <code>{target}</code> is normally some kind of network
 * address.
 * <p>
 * Any <code>{parms}</code> are formed as a series of equates
 * of the form ";x=y".  Example: ";type=a".
 * <p>
 * An optional second parameter may be specified to the open
 * function. This is a mode flag that indicates to the protocol
 * handler the intentions of the calling code. The options here
 * specify if the connection is going to be read (READ), written
 * (WRITE), or both (READ_WRITE). The validity of these flag
 * settings is protocol dependent. For instance, a connection
 * for a printer would not allow read access, and would throw
 * an IllegalArgumentException. If the mode parameter is not
 * specified, READ_WRITE is used by default.
 * <p>
 * An optional third parameter is a boolean flag that indicates
 * if the calling code can handle timeout exceptions. If this
 * flag is set, the protocol implementation may throw an
 * InterruptedIOException when it detects a timeout condition.
 * This flag is only a hint to the protocol handler, and it
 * does not guarantee that such exceptions will actually be thrown.
 * If this parameter is not set, no timeout exceptions will be
 * thrown.
 * <p>
 * Because connections are frequently opened just to gain access
 * to a specific input or output stream, four convenience
 * functions are provided for this purpose.
 *
 * See also: {@link DatagramConnection DatagramConnection}
 * for information relating to datagram addressing
 *
 * @author  Nik Shaylor
 * @version 1.1 1/7/2000
 * @version 1.2 12/8/2000 (comments revised)
 */

public class Connector {

/*
 * Implementation note: The open parameter is used for dynamically
 * constructing a class name in the form:
 * <p>
 * <code>com.sun.cldc.io.{platform}.{protocol}.Protocol</code>
 * <p>
 * The platform name is derived from the system by looking for
 * the system property "microedition.platform".  If this property
 * key is not found or the associated class is not present, then
 * one of two default directories are used. These are called
 * "j2me" and "j2se". If the property "microedition.configuration"
 * is non-null, then "j2me" is used, otherwise "j2se" is assumed.
 * <p>
 * The system property "microedition.protocolpath" can be used to
 * change the root of the class space that is used for looking
 * up the protocol implementation classes.
 * <p>
 * The protocol name is derived from the parameter string
 * describing the target of the connection. This takes the from:
 * <p>
 * <code> {protocol}:[{target}][ {parms}] </code>
 * <p>
 * The protocol name is used for dynamically finding the
 * appropriate protocol implementation class.  This information
 * is stripped from the target name that is given as a parameter
 * to the open() method.
 */

    /**
     * Access mode READ.
     */
    public final static int READ  = 1;

    /**
     * Access mode WRITE.
     */
    public final static int WRITE = 2;

//    /**
//     * Access mode WRITE_BEHIND.
//     */
//    public final static int WRITE_BEHIND = 4;

    /**
     * Access mode READ_WRITE.
     */
    public final static int READ_WRITE = (READ|WRITE);

    /**
     * Name of host system. (j2se/j2me/parm/ipaq etc.)
     */
    private static String host;

    /**
     * The root of the classes.
     */
    private static String classRoot;

    /**
     * Class initializer.
     */
    static {
        host = "j2me";
        classRoot = "com.sun.squawk.io";

        /* Get the system configuration name */
        if (System.getProperty("microedition.configuration") == null) {
            host = "j2se"; /* Use "j2se" if none is specified */
        }

        /* See if there is an alternate protocol class root path */
        String propertyClassRoot = System.getProperty("javax.microedition.io.Connector.protocolpath");
        if (propertyClassRoot != null) {
            classRoot = propertyClassRoot;
        }
    }

    /**
     * Prevent instantiation of this class.
     */
    private Connector(){}

    /**
     * Create and open a Connection.
     *
     * @param name             The URL for the connection.
     * @return                 A new Connection object.
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the requested connection
     *   cannot be make, or the protocol type does not exist.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    public static Connection open(String name) throws IOException {
        return open(name, READ_WRITE);
    }

    /**
     * Create and open a Connection.
     *
     * @param name             The URL for the connection.
     * @param mode             The access mode.
     * @return                 A new Connection object.
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the requested connection
     *   cannot be make, or the protocol type does not exist.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    public static Connection open(String name, int mode) throws IOException {

        return open(name, mode, false);
    }

    /**
     * Create and open a Connection.
     *
     * @param name             The URL for the connection
     * @param mode             The access mode
     * @param timeouts         A flag to indicate that the caller
     *                         wants timeout exceptions
     * @return                 A new Connection object
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException if the requested connection
     * cannot be make, or the protocol type does not exist.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    public static Connection open(String name, int mode, boolean timeouts) throws IOException {
        /* Test for null argument */
        if (name == null) {
            throw new IllegalArgumentException("Null URI");
        }

        /* Look for : as in "http:", "file:", or whatever */
        int colon = name.indexOf(':');

        /* Test for null argument */
        if (colon < 1) {
            throw new IllegalArgumentException("no ':' in URI");
        }

        /* Strip off the protocol name */
        String protocol = name.substring(0, colon);

        /* Strip the protocol name from the rest of the string */
        name = name.substring(colon+1);

        /* First try for specific host class */
        try {
            return openPrim(protocol, protocol, name, mode, timeouts, host);
        } catch(ClassNotFoundException x) {
//System.out.println("specific host class failed: "+x.getMessage());

            /* Then try for generic j2me class */
            try {
                return openPrim(protocol, protocol, name, mode, timeouts, "j2me");
            } catch(ClassNotFoundException y) {
//System.out.println("generic j2me class failed: "+y.getMessage());

                /* Then try for generic channel class */
                try {
                    return openPrim("channel", protocol, name, mode, timeouts, host);
                } catch(ClassNotFoundException z) {
//System.out.println("generic channel class failed: "+z.getMessage());
                }
            }
        }

        throw new ConnectionNotFoundException("The '"+protocol+"' protocol does not exist");
    }

    /**
     * Create and open a Connection.
     *
     * @param protocolClassName The URL protocol
     * @param name             The URL for the connection
     * @param mode             The access mode
     * @param timeouts         A flag to indicate that the caller
     *                         wants timeout exceptions
     * @param platform         Platform name
     * @return                 A new Connection object
     *
     * @exception ClassNotFoundException  If the protocol cannot be found.
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the connection cannot
     *                                        be found.
     * @exception IOException If some other kind of I/O error occurs.
     * @exception IllegalArgumentException If a parameter is invalid.
     */
    private static Connection openPrim(String protocolClassName, String protocolName, String name, int mode, boolean timeouts, String platform) throws IOException, ClassNotFoundException {
        try {
            /* Use the platform and protocol names to look up */
            /* a class to implement the connection */
            Class clazz = Class.forName(classRoot + "." + platform + "." + protocolClassName + ".Protocol");

            /* Construct a new instance */
            ConnectionBase con = (ConnectionBase)clazz.newInstance();

            /* Open the connection, and return it */
            return con.open(protocolName, name, mode, timeouts);

        } catch (InstantiationException x) {
            throw new IOException(x.toString());
        } catch (IllegalAccessException x) {
            throw new IOException(x.toString());
        } catch (ClassCastException x) {
            throw new IOException(x.toString());
        }
    }

    /**
     * Create and open a connection input stream.
     *
     * @param  name            The URL for the connection.+
     * @return                 An InputStream.
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the connection cannot
     *                                        be found.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    public static InputStream openInputStream(String name) throws IOException {
        InputConnection con = (InputConnection)Connector.open(name, Connector.READ);
        try {
            return con.openInputStream();
        } finally {
            con.close();
        }
    }

    /**
     * Create and open a connection output stream.
     *
     * @param  name            The URL for the connection.
     * @return                 An OutputStream.
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the connection cannot
     *                                        be found.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    public static OutputStream openOutputStream(String name) throws IOException {
        OutputConnection con = (OutputConnection)Connector.open(name, Connector.WRITE);
        try {
            return con.openOutputStream();
        } finally {
            con.close();
        }
    }

    /**
     * Create and open a connection input stream.
     *
     * @param  name            The URL for the connection.
     * @return                 A DataInputStream.
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the connection cannot
     *                                        be found.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    public static DataInputStream openDataInputStream(String name) throws IOException {
        InputConnection con = (InputConnection)Connector.open(name, Connector.READ);
        try {
            return con.openDataInputStream();
        } finally {
            con.close();
        }
    }

    /**
     * Create and open a connection output stream.
     *
     * @param  name            The URL for the connection.
     * @return                 A DataOutputStream.
     *
     * @exception IllegalArgumentException If a parameter is invalid.
     * @exception ConnectionNotFoundException If the connection cannot
     *                                        be found.
     * @exception IOException  If some other kind of I/O error occurs.
     */
    public static DataOutputStream openDataOutputStream(String name) throws IOException {
        OutputConnection con = (OutputConnection)Connector.open(name, Connector.WRITE);
        try {
            return con.openDataOutputStream();
        } finally {
            con.close();
        }
    }
}


