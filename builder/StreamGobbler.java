/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

import java.io.*;

public class StreamGobbler extends Thread {
    final InputStream is;
    final PrintStream out;
    final String prefix;
    final Filter filter;

    public static interface Filter {
        boolean match(String line);
    }

    StreamGobbler(InputStream is, PrintStream out) {
        this(is, out, null, null);
    }

    StreamGobbler(InputStream is, PrintStream out, String prefix, Filter filter) {
        this.is = is;
        this.out = out;
        this.prefix = prefix;
        this.filter = filter;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ((line = br.readLine()) != null) {
                if (prefix != null) {
                    out.print(prefix);
                }
                if (filter == null || filter.match(line)) {
                    out.println(line);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        //out.println("StreamGobbler terminated");
    }
}

