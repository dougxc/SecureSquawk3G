package awtcore.impl.squawk;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import com.sun.squawk.vm.ChannelConstants;

public class EventDispatcher extends Thread {

    private static void post(AWTEvent e) {
//System.err.println("Posting event: "+e);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
    }

    public void run() {
        setDaemon(true);
        while(true) {
            long key = VM.getGUIEvent();

            int key1 = (int)(key >> 32);
            int key2 = (int)(key);

            int key1_H = (key1 >> 16) & 0xFFFF;
            int key1_L =  key1 & 0xFFFF;
            int key2_H = (key2 >> 16) & 0xFFFF;
            int key2_L =  key2 & 0xFFFF;

//System.err.println("Got event "+key1_H+":"+key1_L+":"+key2_H+":"+key2_L);

            switch (key1_H) {
                case ChannelConstants.GUIIN_REPAINT: {
                    if(Toolkit.getTopWindow() != null) {
                        Toolkit.getTopWindow().repaint();
                    }
                    break;
                }
                case ChannelConstants.GUIIN_KEY: {
                    post(new KeyEvent(null, key1_L, 0, 0, key2_H, (char)key2_L));
                    break;
                }
                case ChannelConstants.GUIIN_MOUSE: {
                    post(new MouseEvent (null, key1_L, 0, 0, key2_H, key2_L, 0, false));
                    break;
                }
                case ChannelConstants.GUIIN_EXIT: {
                    System.exit(0);
                    break;
                }
                case ChannelConstants.GUIIN_HIBERNATE: {
                    try {
                        Runtime.getRuntime().hibernate(); // Hibernate the application
                    } catch (IOException ex) {
                        System.err.println("Error while hibernating isolate: ");
                        ex.printStackTrace();
                    }
                    break;
                }
                default: {
                    System.out.println("Bad GUI input event "+key1_H+":"+key1_L+":"+key2_H+":"+key2_L);
                }
            }
        }
    }
}
