package mojo;

import java.awt.*;
import java.awt.event.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import com.sun.squawk.vm.ChannelConstants;

public class Main implements WindowListener {

    static int screenWidth() {
        return (int)VM.execGraphicsIO(ChannelConstants.SCREENWIDTH, 0, 0, 0, 0, 0, 0, null, null);
    }
    static int screenHeight() {
        return (int)VM.execGraphicsIO(ChannelConstants.SCREENHEIGHT, 0, 0, 0, 0, 0, 0, null, null);
    }
    private static void setOffScreenMode() {
        VM.execGraphicsIO(ChannelConstants.SETOFFSCREENMODE, 0, 0, 0, 0, 0, 0, null, null);
    }

    public static final int SOFTBUTTON_HEIGHT = 20;
    public static int WIDTH = screenWidth();
    public static int HEIGHT = screenHeight() - SOFTBUTTON_HEIGHT - 16;
    private static MojoScreen screen = new MojoScreen();

    public Main(String s) {
        String string;
        MIDlet midlet = null;
        setOffScreenMode();

        string = System.getProperty("mojo.screen.width");
        if (string != null) {
            try {WIDTH = Integer.parseInt(string);}
            catch (NumberFormatException nfe) {}
        }

        string = System.getProperty("mojo.screen.height");
        if (string != null) {
            try {HEIGHT = Integer.parseInt(string);}
            catch (NumberFormatException nfe) {}
        }

        Frame f = new Frame("Mojo");
        f.setNoBackground();
        f.addWindowListener(this);
        f.setSize(WIDTH, HEIGHT + 16 + SOFTBUTTON_HEIGHT);
        f.add("Center", screen);
        f.setVisible(true);

        try {midlet = (MIDlet)Class.forName(s).newInstance();}
        catch (Exception e) {
            System.out.println("problem instantiating MIDlet: " + e);
            e.printStackTrace();
            System.exit(-1);
        }

        try {midlet.publicStartApp();}
        catch (MIDletStateChangeException e) {
            System.out.println("problem starting MIDlet: " + e);
            System.exit(-1);
        }
    }

    public static MojoScreen getScreen() {
        return screen;
    }

    public void windowActivated(WindowEvent e) {
        Display d = Display.getDisplay();
        Displayable current = d.getCurrent();
        if (current != null) {
            Panel proxy = current.getProxy();
            proxy.repaint();
            System.out.println("proxy.repaint()");
            proxy.requestFocus();
            System.out.println("proxy.requestFocus()");
        }

        System.out.println("windowActivated()");
    }

    public void windowClosed(WindowEvent e) {
        System.out.println("Main.windowClosed()");
    }

    public void windowClosing(WindowEvent e) {
        System.out.println("Main.windowClosing()");
        System.out.println("bye...");
        System.exit(0);
    }

    public void windowDeactivated(WindowEvent e) {
        System.out.println("Main.windowDeactivated()");
    }

    public void windowDeiconified(WindowEvent e) {
        System.out.println("Main.windowDeiconified()");
    }

    public void windowIconified(WindowEvent e) {
        System.out.println("Main.windowIconified()");
    }

    public void windowOpened(WindowEvent e) {
        System.out.println("Main.windowOpened()");
    }

    public static void main(String[] arg) {
        if (arg.length < 1) {
            System.out.println("usage: Main <midlet>");
            System.exit(-1);
        }

        new Main(arg[0]);
    }
}
