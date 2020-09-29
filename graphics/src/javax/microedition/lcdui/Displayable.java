package javax.microedition.lcdui;

public class Displayable {
    protected java.awt.Panel proxy;
    private java.util.Vector commands;
    private CommandListener cmd_listener;
    private Graphics midpg;
    private long last_repaint;
    private static int delay = 0;

    static final int KEYCODE_F1 = 1000000;
    static final int KEYCODE_F2 = 1000001;
    static final int KEYCODE_F3 = 1000002;
    static final int KEYCODE_F4 = 1000003;
    static final int KEYCODE_UP = 1000004;
    static final int KEYCODE_DOWN = 1000005;
    static final int KEYCODE_LEFT = 1000006;
    static final int KEYCODE_RIGHT = 1000007;

    static {
        String s = System.getProperty("mojo.repaint.delay");
        if (s != null) {
            try {delay = Integer.parseInt(s);}
            catch (NumberFormatException e) {}
        }
    }

    public Displayable() {
        commands = new java.util.Vector();
        midpg = new Graphics();
        proxy = new DisplayableProxy(this);
    }

    public Graphics getMIDPGraphics() {
        return midpg;
    }

    public java.awt.Panel getProxy() {
        return proxy;
    }

    public void repaint(int x, int y, int w, int h) {
//System.out.println("repaint "+x+":"+y+":"+w+":"+h);
 //       midpg.setClip(x, y, w, h);

        if (Display.getDisplay().getCurrent() != this) return;

        //try {Thread.sleep(delay);}
        //catch (InterruptedException e) {}

        // safeguard against tight-spinning animation
        // threads. I don't want to name names, but an
        // example that comes to mind starts with "M..."
        // and ends with "...anyballs"
        //if (System.currentTimeMillis() - last_repaint <= 10) {
        //    try {Thread.sleep(1);}
        //    catch (InterruptedException e) {}
        //}

        //last_repaint = System.currentTimeMillis();

        proxy.repaint(/*30,*/ x, y, w, h);
    }

    public void repaint() {
        repaint(0, 0, getWidth(), getHeight());
    }

    protected void paint(Graphics g) {
    }

    public int getWidth() {
        return Display.WIDTH;
    }

    public int getHeight() {
        return Display.HEIGHT;
    }

    protected void keyPressed(int keycode) {
    }

    protected void keyReleased(int keycode) {
    }

    public void keyTyped(char c) {
    }

    protected void pointerPressed(int x, int y) {
        //System.out.println("Canvas.pointerPressed(" + x + ", " + y + ")");
    }

    public void pointerReleased(int x, int y) {
        //System.out.println("Canvas.pointerReleased(" + x + ", " + y + ")");
    }

    public void pointerDragged(int x, int y) {
        //System.out.println("Canvas.pointerDragged(" + x + ", " + y + ")");
    }

    public void addCommand(Command cmd) {
        if (!commands.contains(cmd)) {
            commands.addElement(cmd);
        }
    }

    public java.util.Vector getCommands() {
        return commands;
    }

    public void removeCommand(Command cmd) {
    }

    public void setCommandListener(CommandListener cl) {
        cmd_listener = cl;
    }

    public CommandListener getCommandListener() {
        return cmd_listener;
    }







    class DisplayableProxy extends java.awt.Panel implements
                                                      java.awt.event.KeyListener,
                                                      java.awt.event.MouseListener,
                                                      java.awt.event.MouseMotionListener {
        private Displayable owner;

        DisplayableProxy(Displayable owner) {
            this.owner = owner;
            addKeyListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            setNoBackground();
        }

        private int getKeyCode(int c, int code) {
            switch (code) {
                //case java.awt.event.KeyEvent.VK_F1: return KEYCODE_F1;
                //case java.awt.event.KeyEvent.VK_F2: return KEYCODE_F2;
                //case java.awt.event.KeyEvent.VK_F3: return KEYCODE_F3;
                //case java.awt.event.KeyEvent.VK_F4: return KEYCODE_F4;

                case java.awt.event.KeyEvent.VK_UP: return KEYCODE_UP;
                case java.awt.event.KeyEvent.VK_DOWN: return KEYCODE_DOWN;
                case java.awt.event.KeyEvent.VK_LEFT: return KEYCODE_LEFT;
                case java.awt.event.KeyEvent.VK_RIGHT: return KEYCODE_RIGHT;
                case java.awt.event.KeyEvent.VK_ENTER: return c;
            }

            if ("0123456789*#".indexOf(c) >=0 ) return c;
            return -1;
        }

        public void keyPressed(java.awt.event.KeyEvent e) {
e.consume();
            int code = getKeyCode(e.getKeyChar(), e.getKeyCode());
//System.out.println("keyPressed "+code);
            if (code >= 0) owner.keyPressed(code);
        }

        public void keyReleased(java.awt.event.KeyEvent e) {
e.consume();
            int code = getKeyCode(e.getKeyChar(), e.getKeyCode());
//System.out.println("keyReleased "+code);
            if (code >= 0) owner.keyReleased(code);
        }

        public void keyTyped(java.awt.event.KeyEvent e) {
e.consume();
            int code = getKeyCode(e.getKeyChar(), e.getKeyCode());
//System.out.println("keyTyped "+code);
            if (code >= 0) owner.keyTyped(e.getKeyChar());
        }

        public void mouseClicked(java.awt.event.MouseEvent e) {
e.consume();
        }

        public void mouseEntered(java.awt.event.MouseEvent e) {
e.consume();
        }

        public void mouseExited(java.awt.event.MouseEvent e) {
e.consume();
        }

        public void mousePressed(java.awt.event.MouseEvent e) {
e.consume();
            owner.pointerPressed(e.getX(), e.getY());
        }

        public void mouseReleased(java.awt.event.MouseEvent e) {
e.consume();
            owner.pointerReleased(e.getX(), e.getY());
        }

        public void mouseDragged(java.awt.event.MouseEvent e) {
e.consume();
            owner.pointerDragged(e.getX(), e.getY());
        }

        public void mouseMoved(java.awt.event.MouseEvent e) {
e.consume();
        }

        public void update(java.awt.Graphics g) {
            super.update(g);
            java.awt.Toolkit.getDefaultToolkit().flushScreen();
        }

        public void paint(java.awt.Graphics g) {
            owner.midpg.setGraphicsImpl(g);
            owner.paint(owner.midpg);
        }
    }
}
