//if[TRANSLATOR.TCKERRORLOGGER]
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.util;

import java.io.*;
import java.util.*;
import com.sun.squawk.util.Vector;    // Version without synchronization
import com.sun.squawk.util.Hashtable; // Version without synchronization

/**
 * This is a singleton class that provides support for logging linkage errors
 * while processing TCK classes. The logs can either be in plain text or
 * formatted as a HTML page with links to the description pages (in the
 * TCK distribution) for the failed tests.<p>
 *
 * It is intended for usage of this
 * class to be statically conditional. That is, it need not exist in a
 * production deployment of the translator. As such, all calls/references
 * to this class should be wrapped in a conditional test of the final static
 * boolean variable
 * {@link com.sun.squawk.translator.Translator#TCKERRORLOGGER}. For example:<p>
 *
 * <p><hr><blockquote><pre>
 *       try {
 *           // some code that may throw LinkageError for 'klass' ...
 *       } catch (LinkageError e) {
 *           if (TCKERRORLOGGER && TCKErrorLogger.isLogging()) {
 *               TCKErrorLogger.log(klass, e);
 *           } else {
 *               throw e;
 *           }
 *       }
 * </pre></blockquote><hr><p>
 *
 * If the value of <code>Translator.TCKERRORLOGGER</code> was statically
 * determined to be false by javac, then this whole block of code would be
 * omitted from the compiled class.
 */
public class TCKErrorLogger {

    private static final class Log {
        final Klass klass;
        final String reason;
        final int anchor;
        Log(Klass klass, String reason, int anchor) {
            this.klass = klass;
            this.reason = reason;
            this.anchor = anchor;
        }
    }

    /**
     * The print stream to which the log is written.
     */
    private static PrintStream out;

    /**
     * A map from TCK class names to the HTML file that describes the tests
     * in which the classes participate.
     */
    private static Hashtable map;

    /**
     * The base for the relative URLs to the HTML pages deescribing the tests.
     */
    private static String base;

    /**
     * The logs written that will be used to create an index.
     */
    private static Vector logs = new Vector();

    /**
     * Prevents construction.
     */
    private TCKErrorLogger() {}

    /**
     * Determines if the logger is actually logging which will be the case
     * if the log stream is not null.
     *
     * @return  true if the underlying log stream is not null
     */
    public static boolean isLogging() {
        return out != null;
    }

    /**
     * Initializes (or re-initializes) the logger. If the value of
     * <code>htmlMap</code> is not <code>null</code>, then the logs are written
     * in HTML format.
     *
     * @param  out   the print stream to which the log is written
     * @param  map   a map from class name to relative URLs describing classes
     * @param  base  the base for the relative URLs
     */
    public static void initialize(PrintStream out, Hashtable map, String base) {
        TCKErrorLogger.map = map;
        TCKErrorLogger.out = out;
        TCKErrorLogger.base = base;
        if (map != null) {
            out.println("<html>");
            out.println("  <head>");
            out.println("    <title>TCK Error Report</title>");
            out.println("  </head>");
            out.println("  <body>");
            out.println("  <p><a href=\"#index\">Index</a></p>");
            out.println("  <p><a href=\"#summary\">Summary</a></p>");
            logs.removeAllElements();
        }
    }

    /**
     * Writes a summary of the logged errors which includes both the total
     * number of classes for which an error was logged as well as a
     * histogram of the frequencies for each unique error type.
     */
    private static void writeSummary() {
        Hashtable reasonCounts = new Hashtable();
        Hashtable reasonAnchors = new Hashtable();
        for (Enumeration e = logs.elements(); e.hasMoreElements(); ) {
            Log log = (Log)e.nextElement();
            Integer count = (Integer)reasonCounts.get(log.reason);
            if (count == null) {
                count = new Integer(1);
            } else {
                count = new Integer(count.intValue()+1);
            }
            reasonCounts.put(log.reason, count);
            reasonAnchors.put(log.reason, new Integer(log.anchor));
        }

        /*
         * Write the summary
         */
        out.println("    <a name=\"summary\"/>");
        out.println("    <p><b>Summary of failed reasons</b> (total failures: "+logs.size()+")</p>");
        out.println("    <table border=\"1\" cellpadding=\"5\">");
        out.println("      <tr>");
        out.println("        <th>Reason (link is to last the class failure with the reason)</th>");
        out.println("        <th>Total</th>");
        out.println("      </tr>");
        Enumeration reasons = reasonCounts.keys();
        Enumeration counts = reasonCounts.elements();
        while (reasons.hasMoreElements()) {
            String reason = (String)reasons.nextElement();
            Integer anchor = (Integer)reasonAnchors.get(reason);
            if (reason.length() > 160) {
                reason = reason.substring(0, 160) + "...";
            }
            out.println("      <tr>");
            out.println("        <td><a href=\"#"+anchor+"\">"+reason+"</a></td>");
            out.println("        <td>"+counts.nextElement()+"</td>");
            out.println("      </tr>");
        }
        out.println("    </table>");

    }

    /**
     * Writes an index from each class for which an error was logged to the
     * details of the error log for that class.
     */
    private static void writeIndex() {
        out.println("    <a name=\"index\"/>");
        out.println("    <p><b>Index to Failed Classes</b></p>");
        for (Enumeration e = logs.elements(); e.hasMoreElements(); ) {
            Log log = (Log)e.nextElement();
            out.println("    <a href=\"#"+log.anchor+"\">"+ log.klass.getName()+"</a>: "+ log.reason+"<br/>");
        }
    }

    /**
     * Closes the underlying log stream if it is open. This disables the logger
     * until {@link #initialize(PrintStream, Hashtable, String)} is called
     * again.<p>
     *
     * If the logger is generating a HTML report, this method also prints out
     * a summary of the errors logged as well as an index to details of each
     * log. It also completes the HTML page.
     */
    public static void close() {
        if (out != null) {
            if (map != null) {
                out.println("    <hr/>");
                out.println("    <hr/>");

                writeSummary();
                writeIndex();

                logs.removeAllElements();
                out.println("  </body>");
                out.println("</html>");
            }
            out.close();
            out = null;
        }
    }

    /**
     * Gets the URL corresponding to a given class name. If there is no explicit
     * URL entry in the map for <code>name</code>, then it is repeatively
     * trimmed by one character until it matches the prefix of an entry which
     * is subsequently returned.
     *
     * @param   name  the name of a class
     * @return  the URL for the HTML description of the TCK test in which
     *                <code>name</code> participates
     */
    private static String getURL (String name) {
        String url = (String)map.get(name);
        if (url == null) {
            String base = name;
            while (base.length() > 1) {
                base = base.substring(0, base.length()-1);
                Enumeration keys = map.keys();
                Enumeration urls = map.elements();
                while (keys.hasMoreElements()) {
                    String key = (String)keys.nextElement();
                    url = (String)urls.nextElement();
                    if (key.startsWith(base)) {
                        return url;
                    }
                }
            }
        }
        return url;
    }

    private static String htmlEncode(String name) {
        int index = 0;
        while ((index = name.indexOf('<', index)) != -1) {
            name = name.substring(0, index) + "&lt;" + name.substring(index + 1);
        }
        index = 0;
        while ((index = name.indexOf('>', index)) != -1) {
            name = name.substring(0, index) + "&gt;" + name.substring(index + 1);
        }
        return name;
    }

    /**
     * Logs a LinkageError for a given class.
     *
     * @param  klass  the class that caused a linkage error during its loading
     *                or conversion
     * @param  error  the linkage error that was thrown
     */
    public static void log(Klass klass, LinkageError error) {
        String name = klass.getName();
        if (map != null) {
            String url = getURL(name);
            out.println("    <hr/>");
            int anchor = logs.size();
            if (url != null) {
                out.println("    <p><b><a name=\""+anchor+"\" href=\""+base+url+ "\"/>"+name+"</a></b></p>");
            } else {
                out.println("    <p><b><a name=\""+anchor+"\">"+name+ "</a></b> (<i>no description URL found</i>)</p>");
            }
            String message = error.getMessage();
            StringTokenizer st = new StringTokenizer(message, ":");
            String reason = message;
            if (st.countTokens() > 1) {
                out.println("    <p><pre>");
                String padding = "";
                while (st.hasMoreTokens()) {
                    reason = htmlEncode(st.nextToken());
                    out.print(padding + reason);
                    if (st.hasMoreTokens()) {
                        out.println(':');
                    } else {
                        out.println();
                    }
                    padding += "  ";
                }
                out.println("    </pre></p>");
            }
            logs.addElement(new Log(klass, reason, anchor));

            out.println("    <p><pre>");
            error.printStackTrace(out);
            out.println("    </pre></p>");
        } else {
            out.print(name + ": ");
            error.printStackTrace(out);
        }
    }
}