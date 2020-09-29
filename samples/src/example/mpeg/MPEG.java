/*
 *   @(#) MPEG-I Vide Decoder 0.9 Yu Tianli (yu-tianli@usa.net)
 *
 *   MPEGApplet.java   1999-2-24 09:11 am
 *
 *   Copyright (C) 1999  Yu Tianli
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *
 *   The Video Stream Decoder part of this program is based on
 *   Carlos Hasan's MPEG-1 video stream decoder version 0.9 1998
 */

package example.mpeg;
import java.io.*;

import java.awt.*;

/**
 * The MPEG-1 system & video stream decoder applet that is intended to run
 * embedded inside of a Web page or another application.
 *
 * @author  Yu Tianli
 * @version 0.9, 99-2-23 10:05 am
 */
public class MPEG extends Panel implements Runnable {


    /**
     *   DisplayPanel that implements the Displayable interface
     */
    private DisplayPanel displayer = null;

    /**
     *   The applet's execution thread
     */
    private Thread playerThread = null;

    /**
     * The repeat boolean parameter
     */
    private boolean repeat = true;

    /**
     *   System stream decoder
     */
    private SystemDecoder systemDecoder;

    /**
     *   Video stream decoder
     */
    private VideoDecoder videoDecoder;

    /**
     *   Audio stream decoder ( for future development )
     */
    private AudioDecoder audioDecoder;

    /**
     *   The inputstream where the MPEG data comes
     */
    private InputStream mpegIn = null;

    /**
     *   The playing state of MPEGApplet
     */
    private int state;

    // state const

    private String mpegSourceUrl;

    MPEG(String url) {
        mpegSourceUrl = url;
    }

    /**
     *   The Applet stops playing
     */
    private static final int STOP = 1;

    /**
     *   The Applet is playing
     */
    private static final int PLAY = 2;

    /**
     *   Threads for different decoder
     */
    private Thread sysThread, videoThread, audioThread;

    /**
     *   Queues for the buffer exchange between decoders
     */
    private BufferQueue systemQueue, videoQueue, audioQueue;


    // methods

    /**
     * Applet information
     */
    public String getAppletInfo() {
        return "MPEGApplet 0.9 (23 Feb 1999), Yu Tianli (yu-tianli@usa.net) ";
    }


    public void init() {
//System.out.println("init");

        displayer = new DisplayPanel();
        setLayout(null);
        add(displayer);

        systemQueue = new BufferQueue();
        videoQueue = new BufferQueue();
        audioQueue = new BufferQueue();

        systemDecoder = new SystemDecoder(systemQueue, videoQueue, audioQueue);

        videoDecoder = new VideoDecoder(new BufferedBitStream(videoQueue, systemQueue, new ByteBuffer()), displayer);
        audioDecoder = new AudioDecoder(new BufferedBitStream(audioQueue, systemQueue, new ByteBuffer()));


        state = STOP;

    }

    /**
     * Start the execution of the applet
     */
    public void start()
    {
//System.out.println("start ");

        if (playerThread == null) {
            playerThread = new Thread(this);
            state = PLAY;
            playerThread.start();
            //showStatus(getAppletInfo());
        }
    }


    /**
     * Stop the execution of the applet
     */
    public void stop() {
//System.out.println("stop ");

        if (playerThread != null && playerThread.isAlive()) {
            systemDecoder.stop();
            state = STOP;
            try {
                playerThread.join();
            } catch (InterruptedException e) {
                showStatus("Interrupted while waiting for player Thread to die");
            }

        }
        playerThread = null;
    }


    /**
     *   Decoding process
     */
    public void run()
    {

        do {

            try {
                //mpegIn = url.openStream();
                mpegIn = getInputStream();

//System.out.println("*** input bytes available() "+mpegIn.available());

                long time = System.currentTimeMillis();

                systemDecoder.setSystemStream(mpegIn);
                // Adjust the thread priority for better awt event respond
                // maybe we should use a ThreadGroup next time.
                Thread sysThread = new Thread(systemDecoder);
                sysThread.setPriority(Thread.MIN_PRIORITY);
                Thread videoThread = new Thread(videoDecoder);
                videoThread.setPriority(Thread.MIN_PRIORITY);
                Thread audioThread = new Thread(audioDecoder);
                audioThread.setPriority(Thread.MIN_PRIORITY);

                sysThread.start();
                videoThread.start();
                audioThread.start();

                // now wait for all the threads to die and perform the clean up
                try {
                    sysThread.join();
                    videoThread.join();
                    audioThread.join();
                } catch (InterruptedException e) {
                    showStatus("Interrupted while waiting for threads to die:"+e);
                }
                systemDecoder.reset();
                videoDecoder.reset();
                audioDecoder.reset();
                mpegIn.close();

System.out.println("time = "+(System.currentTimeMillis() - time));

            } catch (IOException e) {
            showStatus("IOException at playThread: "+e);
            }
        } while ( repeat && (state == PLAY));
    }

    public static void main(String args[]) {
        Frame f = new Frame("mpeg");
        MPEG app = new MPEG(args.length == 0 ? "samples/src/example/mpeg/random.mpg" : args[0]);
        app.init();
        app.start();

        f.add("Center", app);
        f.setSize(500, 500);
        f.show();
    }

    public void showStatus(String s) {
        System.out.println(s);
    }






    static byte[] inputData = null;


    InputStream getInputStream() {
        InputStream img = getInputStream0("resource:"+mpegSourceUrl);
        if (img == null) {
            img = getInputStream0("file://"+mpegSourceUrl);
        }
        if (img == null) {
            System.err.println("Could not find '" + mpegSourceUrl + "' -- make sure that the base directory of this file (most likely '.') is part of the classpath");
            System.exit(-1);
        }
        return img;
    }

    private InputStream getInputStream0(String url) {
        if (inputData == null) {
            try {
                //InputStream in = new FileInputStream("random.mpg");
                InputStream in = javax.microedition.io.Connector.openInputStream(url);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                int ch;
                while ((ch = in.read()) != -1) {
                    os.write((byte)ch);
                }
                inputData = os.toByteArray();
                in.close();
                os.close();
            } catch(javax.microedition.io.ConnectionNotFoundException ex) {
                return null;
            } catch(IOException ex) {
                System.out.println(ex);
                System.exit(-1);
            }
        }
        return new ByteArrayInputStream(inputData);
    }

}