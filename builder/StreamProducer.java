/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

import java.io.*;

public class StreamProducer extends Thread {
    OutputStream out;
    InputStream in;

    StreamProducer(OutputStream out, InputStream in) {
        this.in = in;
        this.out = out;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ((line = br.readLine()) != null) {
//System.err.println("StreamProducer: "+line);
                out.write(line.getBytes());
                out.flush();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        //out.println("StreamProducer terminated");
    }
}

