package example.server;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;

public class Server {

    public static void main (String [] argv) {

        int port = 9999;
        try {
            System.out.println("Starting...");
            StreamConnectionNotifier ssocket = (StreamConnectionNotifier)Connector.open("serversocket://:"+port);
            for (;;) {
                try {
                    System.out.println("listening on port " + port);
                    StreamConnection con = ssocket.acceptAndOpen();
                    System.out.println("Got connection");
                    InputStream  in  = con.openInputStream();
                    OutputStream out = con.openOutputStream();

                    in.read(); // Just waiting for a single byte is enough

                    String date = new Date().toString();
                    String msg = "HTTP/1.0 " + 200 + " OK"    + "\r\n"
                               + "Date: " + date              + "\r\n"
                               + "Server: Squawk"             + "\r\n"
                               + "Content-Type: text/html"    + "\r\n"
                               + "Last-modified: " + date     + "\r\n"
                               +                                "\r\n"
                               + "The time is " + date        + "\r\n";

                    out.write(msg.getBytes());

                    out.close();
                    in.close();
                    con.close();
                    System.out.println("Done");
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }


    }

}




/*
                    int ch = in.read();
                    while (ch != -1) {
                        //System.out.print((char)ch);
                        if (in.available() == 0) {
                            break;
                        }
                        ch = in.read();
                    }
*/
