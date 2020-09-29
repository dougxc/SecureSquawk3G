package javax.microedition.lcdui;

import mojo.*;

public class Display {
    public static final int WIDTH = Main.WIDTH;
    public static final int HEIGHT = Main.HEIGHT;
    private static Display instance = new Display();
    private static MojoScreen screen = Main.getScreen();
    private static SoftButtonPanel sbp = screen.getSoftButtonPanel();
    private Displayable current;
    private Displayable previous;

    public static Display getDisplay(javax.microedition.midlet.MIDlet m) {
        return instance;
    }

    public static Display getDisplay() {
        return instance;
    }

    public void setCurrent(Alert alert, Displayable nextDisplayable) {
        if ((alert == null) || (nextDisplayable == null)) {
            throw new NullPointerException();
        }

        if (nextDisplayable instanceof Alert) {
            throw new IllegalArgumentException();
        }

        setCurrent(alert);
        previous = nextDisplayable;
    }

    public void setCurrent(Displayable d) {
        if (d instanceof Alert) previous = current;

//System.out.println("Adding  d="+d);
//System.out.println("   proxy ="+ d.proxy);

        screen.setCenter(d.proxy);
        screen.invalidate();
        screen.validate();
        d.proxy.requestFocus();
        current = d;
        sbp.reset(current);
        current.repaint();
    }

    void clearAlert() {
        setCurrent(previous);
    }

    public Displayable getCurrent() {
        return current;
    }

    public int numColors() {
        return 256;
    }

    public boolean isColor() {
        return true;
    }

    public void commandAction(int id) {
        java.util.Vector cmds;
        CommandListener listener;
        Command c;

        if (current == null) return;

        c = SoftButtonMenu.getInstance().getCommand();
        if (id == c.getID()) {
            SoftButtonMenu.getInstance().commandAction(c, current);
        }

        if ((listener = current.getCommandListener()) == null) return;

        cmds = current.getCommands();
        for (int i=0; i<cmds.size(); i++) {
            c = (Command)cmds.elementAt(i);
            if (c.getID() == id) {
                listener.commandAction(c, current);
            }
        }

        current.proxy.requestFocus();
    }
}
