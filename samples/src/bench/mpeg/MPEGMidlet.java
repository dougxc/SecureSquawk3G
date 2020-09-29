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

package bench.mpeg;
import java.io.*;

/**
 * The MPEG-1 system & video stream decoder applet that is intended to run
 * embedded inside of a Web page or another application.
 *
 * @author  Yu Tianli
 * @version 0.9, 99-2-23 10:05 am
 */
public class MPEGMidlet /*extends Panel implements Runnable */ {


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
    private boolean repeat = false;

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
Trace.println("MPEGMidlet::init");

        displayer = new DisplayPanel();
//        setLayout(null);
//        add(displayer);
Trace.println("MPEGMidlet::init - 2");

        systemQueue = new BufferQueue();
Trace.println("MPEGMidlet::init - 3");

        videoQueue = new BufferQueue();
Trace.println("MPEGMidlet::init - 4");

        audioQueue = new BufferQueue();
Trace.println("MPEGMidlet::init - 5");

        systemDecoder = new SystemDecoder(systemQueue, videoQueue, audioQueue);
Trace.println("MPEGMidlet::init - 6");

        videoDecoder = new VideoDecoder(new BufferedBitStream(videoQueue, systemQueue, new ByteBuffer()), displayer);
Trace.println("MPEGMidlet::init - 7");

        audioDecoder = new AudioDecoder(new BufferedBitStream(audioQueue, systemQueue, new ByteBuffer()));
Trace.println("MPEGMidlet::init - 8");


        state = STOP;
Trace.println("MPEGMidlet::init - 9");

    }

    /**
     * Start the execution of the applet
     */
/*
    public void start()
    {
Trace.println("MPEGMidlet::start ");

        if (playerThread == null) {
            playerThread = new Thread(this);
            state = PLAY;
            playerThread.start();
            showStatus(getAppletInfo());
        }
    }
*/

    /**
     * Stop the execution of the applet
     */
/*
    public void stop() {
Trace.println("MPEGMidlet::stop ");

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
*/

    /**
     *   Decoding process
     */
    public void run()
    {
        state = PLAY;
Trace.println("MPEGMidlet::run");

        do {

            try {
                //mpegIn = url.openStream();
                //mpegIn = new FileInputStream("random.mpg");
                mpegIn = new TableInputStream();

Trace.println("*** input bytes available() "+mpegIn.available());

                systemDecoder.setSystemStream(mpegIn);
                // Adjust the thread priority for better a*wt event respond
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

            } catch (IOException e) {
            showStatus("IOException at playThread: "+e);
            }
        } while ( repeat && (state == PLAY));
    }


    public void showStatus(String s) {
        Trace.println(s);
    }


    public void runMain() {
        String stats0 = System.getProperty("kvmjit.stats");
        long     res1 = run1();
        String stats1 = System.getProperty("kvmjit.stats");
        long     res2 = run1();
        String stats2 = System.getProperty("kvmjit.stats");
        long     res3 = run1();
        String stats3 = System.getProperty("kvmjit.stats");


        Trace.write(System.out);
        System.out.println("***********res0 "+stats0);
        System.out.println("***********res1 time = "+res1+ " "+stats1);
        System.out.println("***********res2 time = "+res2+ " "+stats2);
        System.out.println("***********res3 time = "+res3+ " "+stats3);
    }

    public long run1() {
        long start = System.currentTimeMillis();
        run();
        long end = System.currentTimeMillis();
        return end-start;
    }






}