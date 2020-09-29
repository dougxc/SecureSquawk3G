/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.ChannelConstants;

/**
 * Special channel for graphics.
 */
public class GUIOutputChannel extends Channel implements FocusListener, KeyListener, MouseListener, MouseMotionListener {

    private static final boolean isHeadless = System.getProperty("java.awt.headless", "false").equals("true");
    static {
        if (isHeadless) {
            System.out.println("[Running in headless graphics environment]");
        }
    }

    /**
     * The channel to which events are queued.
     */
    GUIInputChannel guiInputChannel;

    /**
     * The table of font metrics created for this channel.
     */
    IntHashtable fonts  = new IntHashtable();

    /**
     * The table of images.
     */
    IntHashtable images = new IntHashtable();

    /**
     * The name of the applet class.
     */
    String mainClassName = "?";

    /**
     * Image enumberation.
     */
    int nextImageNumber = 0;

    /**
     * The frame implementing the display for this graphics instance.
     */
    transient Frame frame;

    /**
     * The panel implementing the display for this graphics instance.
     */
    transient Panel panel;

    /**
     * The object used to render to the on-screen panel.
     */
    transient Graphics display;

    /**
     * The off-screen buffer.
     */
    transient Image offScreenBuffer;

    /**
     * The object used to render to the off-screen buffer.
     */
    transient Graphics offScreenDisplay;

    /**
     * Tracks the images.
     */
    transient MediaTracker tracker;

    /**
     * Flags whether or not graphics operation are to be applied to the off-screen buffer.
     */
    boolean offScreen = false;

    int screenWidth  = 300;
    int screenHeight = 300;

    /*
     * Constructor
     */
    public GUIOutputChannel(ChannelIO cio, int index, GUIInputChannel guiInputChannel) {
        super(cio, index);
        this.guiInputChannel = guiInputChannel;
    }


    /*
     * setupGraphics
     */
    private void setupGraphics() {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("setupGraphics "+screenWidth+":"+screenHeight);
        frame = new Frame(mainClassName);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("bye...");
                addEvent(ChannelConstants.GUIIN_EXIT, 0, 0, 0);
            }
        });

        panel = new Panel() {
            public void paint(Graphics g) {
                addEvent(ChannelConstants.GUIIN_REPAINT, 0, 0, 0);
            }
        };

        panel.addKeyListener(this);
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
        frame.addFocusListener(this);

        frame.setSize(screenWidth+8, screenHeight+27);
        frame.add(panel);
        frame.setVisible(true);
        display = panel.getGraphics();
        tracker = new MediaTracker(panel);
    }


    /*
     * getGraphics
     */
    Graphics getGraphics() {
        if (isHeadless) {
            throw new RuntimeException("cannot get a Graphics object in a headless graphics environment");
        }
        if (display == null) {
            setupGraphics();
        }
        // MIDP apps are double buffered, regular kawt apps are not
        if (offScreen && offScreenBuffer == null) {
//System.out.println("get imgBuf");
            panel.setBackground(Color.black);
            offScreenBuffer = panel.createImage(frame.getWidth(), frame.getHeight());
            offScreenDisplay = offScreenBuffer.getGraphics();
            offScreenDisplay.setColor(Color.blue);
            offScreenDisplay.fillRect(0, 0, frame.getWidth(), frame.getHeight());

        }

        if (offScreen) {
            return offScreenDisplay;
        } else {
            return display;
        }
    }

    MediaTracker getTracker() {
        if (tracker == null) {
            getGraphics();
        }
        return tracker;
    }

    /*
     * flushScreen
     */
    void flushScreen() {
        if (offScreen && display != null && offScreenBuffer != null) {
            display.drawImage(offScreenBuffer, 0, 0, panel);
            if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("**flushScreen**");
        }
    }


   /*
    * execute
    */
    int execute(
                 int op,
                 int i1,
                 int i2,
                 int i3,
                 int i4,
                 int i5,
                 int i6,
                 Object o1,
                 Object o2
              ) {
        try {
            switch (op) {
                case ChannelConstants.SETWINDOWNAME: {
                    String s = (String)o1;
                    mainClassName = s;
                    break;
                }
                case ChannelConstants.SCREENWIDTH: {
                    result = screenWidth;
                    break;
                }
                case ChannelConstants.SCREENHEIGHT: {
                    result = screenHeight;
                    break;
                }
                case ChannelConstants.BEEP: {                                 // in awtcore.impl.squawk.ToolkitImpl
                    Toolkit.getDefaultToolkit().beep();
                    break;
                }
                case ChannelConstants.SETOFFSCREENMODE: {                     // in awtcore.impl.squawk.ToolkitImpl
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("setOffScreenMode");
                    offScreen = true;
                    break;
                }
                case ChannelConstants.FLUSHSCREEN: {                          // in awtcore.impl.squawk.ToolkitImpl
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("setOnScreen");
                    flushScreen();
                    break;
                }
                case ChannelConstants.CREATEIMAGE: {                          // in awtcore.impl.squawk.ImageImpl
                    byte[] buf = (byte[])o1;
                    MemImage memImage = new MemImage(buf);
                    result = nextImageNumber++;
                    images.put((int)result, memImage);
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("createImage "+result);
                    break;
                }
                case ChannelConstants.CREATEMEMORYIMAGE: {                    // in awtcore.impl.squawk.ImageImpl
                    int hs     =        i1;
                    int vs     =        i2;
                    int rgblth =        i3;
                    int stride =        i4;
                    RgbImage rgbImage = new RgbImage(hs, vs, rgblth, stride);
                    result = nextImageNumber++;
                    images.put((int)result, rgbImage);
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("createMemoryImage "+result);
                    break;
                }
                case ChannelConstants.GETIMAGE: {                             // in awtcore.impl.squawk.ImageImpl
                    String s = (String)o1;
                    FileImage fileImage = new FileImage(s);
                    result = nextImageNumber++;
                    images.put((int)result, fileImage);
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("getImage "+result+" "+s);
                    break;
                }
                case ChannelConstants.IMAGEWIDTH: {                           // in awtcore.impl.squawk.ImageImpl
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("imageWidth");
                    int index = i1;
                    C2Image c2i = (C2Image)images.get(index);
                    Image img = c2i.getImage(getTracker());
                    result = img.getWidth(null);
                    break;
                }
                case ChannelConstants.IMAGEHEIGHT: {                          // in awtcore.impl.squawk.ImageImpl
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("imageHeight");
                    int index = i1;
                    C2Image c2i = (C2Image)images.get(index);
                    Image img = c2i.getImage(getTracker());
                    result = img.getHeight(null);
                    break;
                }
                case ChannelConstants.DRAWIMAGE: {                            // in awtcore.impl.squawk.ImageImpl
                    int index = i1;
                    int     x = i2;
                    int     y = i3;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("drawImage0 "+index+" at "+x+":"+y );
                    if (!isHeadless) {
                        C2Image c2i = (C2Image)images.get(index);
                        Image img = c2i.getImage(getTracker());
                        getGraphics().drawImage(img, x, y, null);
                    }
                    break;
                }
                case ChannelConstants.FLUSHIMAGE: {                           // in awtcore.impl.squawk.ImageImpl
                    int   index =        i1;
                    int[] rgb   = (int[])o1;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("flush0 "+index+" "+rgb);
                    if (!isHeadless) {
                        RgbImage rgbImage = (RgbImage)images.get(index);
                        rgbImage.getImage(getTracker());
                        rgbImage.flush(rgb);
                    }
                    break;
                }
                case ChannelConstants.CREATEFONTMETRICS: {                    // in awtcore.impl.squawk.FontMetricsImpl
                    int size   = i1;
                    int isBold = i2;
                    int sizeBold = size << 16 + isBold;
                    FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                    if (metrics == null) {
                        metrics = Toolkit.getDefaultToolkit().getFontMetrics(new Font("TimesRoman", isBold==1 ? Font.BOLD : Font.PLAIN, size));
                        fonts.put(sizeBold, metrics);
                    }
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("createFontMetrics "+sizeBold+" = "+(metrics == null ? null : metrics.getFont()));
                    result = sizeBold;
                    break;
                }
                case ChannelConstants.FONTSTRINGWIDTH: {                      // in awtcore.impl.squawk.FontMetricsImpl
                    int sizeBold = i1;
                    String s     = (String)o1;
                    FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                    result = metrics.stringWidth(s);
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("fontStringWidth "+sizeBold+ ":"+s+" = "+result);
                    break;
                }
                case ChannelConstants.FONTGETHEIGHT: {                        // in awtcore.impl.squawk.FontMetricsImpl
                    int sizeBold = i1;
                    FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                    result = metrics.getHeight();
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("fontGetHeight "+sizeBold+" = "+result);
                    break;
                }
                case ChannelConstants.FONTGETASCENT: {                        // in awtcore.impl.squawk.FontMetricsImpl
                    int sizeBold = i1;
                    FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                    result = metrics.getAscent();
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("fontGetHeight "+sizeBold+" = "+result);
                    break;
                }
                case ChannelConstants.FONTGETDESCENT: {                       // in awtcore.impl.squawk.FontMetricsImpl
                    int sizeBold = i1;
                    FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                    result = metrics.getDescent();
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("fontGetHeight "+sizeBold+" = "+result);
                    break;
                }

                case ChannelConstants.SETFONT: {                              // awtcore.impl.squawk.GraphicsImpl
                    int sizeBold = i1;
                    FontMetrics metrics = (FontMetrics)fonts.get(sizeBold);
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("setFont0 "+metrics.getFont());
                    if (!isHeadless) {
                        getGraphics().setFont(metrics.getFont());
                    }
                    break;
                }
                case ChannelConstants.SETCOLOR: {                             // awtcore.impl.squawk.GraphicsImpl
                    int c  = i1;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("setColor0 "+c);
                    if (!isHeadless) {
                        getGraphics().setColor(new Color(c));
                    }
                    break;
                }
                case ChannelConstants.SETCLIP: {                              // awtcore.impl.squawk.GraphicsImpl
                    int x  = i1;
                    int y  = i2;
                    int w  = i3;
                    int h  = i4;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("setClip0 "+x+":"+y+":"+w+":"+h);
                    if (!isHeadless) {
                        getGraphics().setClip(x, y, w, h);
                    }
                    break;
                }

                case ChannelConstants.DRAWSTRING: {                            // awtcore.impl.squawk.GraphicsImpl
                    String s = (String)o1;
                    int x    =         i1;
                    int y    =         i2;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("drawString0 \""+s+"\" "+x+":"+y);
                    if (!isHeadless) {
                        getGraphics().drawString(s, x, y);
                    }
                    break;
                }

                case ChannelConstants.DRAWLINE: {                             // awtcore.impl.squawk.GraphicsImpl
                    int x  = i1;
                    int y  = i2;
                    int w  = i3;
                    int h  = i4;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("drawLine0 "+x+":"+y+":"+w+":"+h);
                    if (!isHeadless) {
                        getGraphics().drawLine(x, y, w, h);
                    }
                    break;
                }
                case ChannelConstants.DRAWOVAL: {                             // awtcore.impl.squawk.GraphicsImpl
                    int x  = i1;
                    int y  = i2;
                    int w  = i3;
                    int h  = i4;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("drawOval0 "+x+":"+y+":"+w+":"+h);
                    if (!isHeadless) {
                        getGraphics().drawOval(x, y, w, h);
                    }
                    break;
                }

                case ChannelConstants.DRAWRECT: {                             // awtcore.impl.squawk.GraphicsImpl
                    int x  = i1;
                    int y  = i2;
                    int w  = i3;
                    int h  = i4;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("drawRect0 "+x+":"+y+":"+w+":"+h);
                    if (!isHeadless) {
                        getGraphics().drawRect(x, y, w, h);
                    }
                    break;
                }
                case ChannelConstants.FILLRECT: {                             // awtcore.impl.squawk.GraphicsImpl
                    int x  = i1;
                    int y  = i2;
                    int w  = i3;
                    int h  = i4;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("fillRect0 "+x+":"+y+":"+w+":"+h);
                    if (!isHeadless) {
                        getGraphics().fillRect(x, y, w, h);
                    }
                    break;
                }
                case ChannelConstants.DRAWROUNDRECT: {                        // awtcore.impl.squawk.GraphicsImpl
                    int x  = i1;
                    int y  = i2;
                    int w  = i3;
                    int h  = i4;
                    int aw = i5;
                    int ah = i6;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("drawRoundRect0 "+x+":"+y+":"+w+":"+h+":"+aw+":"+ah);
                    if (!isHeadless) {
                        getGraphics().drawRoundRect(x, y, w, h, aw, ah);
                    }
                    break;
                }
                case ChannelConstants.FILLROUNDRECT: {                        // awtcore.impl.squawk.GraphicsImpl
                    int x  = i1;
                    int y  = i2;
                    int w  = i3;
                    int h  = i4;
                    int aw = i5;
                    int ah = i6;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("fillRoundRect0 "+x+":"+y+":"+w+":"+h+":"+aw+":"+ah);
                    if (!isHeadless) {
                        getGraphics().fillRoundRect(x, y, w, h, aw, ah);
                    }
                    break;
                }
                case ChannelConstants.FILLARC: {                              // awtcore.impl.squawk.GraphicsImpl
                    int x  = i1;
                    int y  = i2;
                    int w  = i3;
                    int h  = i4;
                    int ba = i5;
                    int ea = i6;
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("fillArc0 "+x+":"+y+":"+w+":"+h+":"+ba+":"+ea);
                    if (!isHeadless) {
                        getGraphics().fillArc(x, y, w, h, ba, ea);
                    }
                    break;
                }
                case ChannelConstants.FILLPOLYGON: {                          // awtcore.impl.squawk.GraphicsImpl
                    int[] comb  = (int[])o1;
                    int   count =        i1;
                    int[] x     = new int[comb.length/2];
                    int[] y     = new int[comb.length/2];
                    for (int i = 0 ; i < x.length ; i++) {
                        x[i] = comb[i];
                    }
                    for (int i = 0 ; i < y.length ; i++) {
                        y[i] = comb[x.length+i];
                    }
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("fillPolygon0 "+count);
                    if (!isHeadless) {
                        getGraphics().fillPolygon(x, y, count);
                    }
                    break;
                }
                case ChannelConstants.REPAINT: {                              // awtcore.impl.squawk.GraphicsImpl
                    if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("repaint0");
                    if (!isHeadless) {
                        panel.repaint();
                    }
                    break;
                }

                default: throw new RuntimeException("Illegal channel operation "+op);
            }
        } catch (InternalError ie) {
            // If this is the message that occurs on unix when the DISPLAY variable is
            // not set correctly, it's useful to see that now
            String message = ie.getMessage();
            if (message.indexOf("X11") != -1 || message.indexOf("DISPLAY") != -1) {
                ie.printStackTrace();
            }
            throw ie;
        }
        return 0;
    }


    /*
     * focusGained
     */
    public void focusGained(FocusEvent e) {
        panel.requestFocus();
        flushScreen();
    }

    /*
     * focusLost
     */
    public void focusLost(FocusEvent e) {
    }

    /*
     * addEvent
     */
    void addEvent(int key1_high, int key1_low, int key2_high, int key2_low) {
        guiInputChannel.addToGUIInputQueue(key1_high, key1_low, key2_high, key2_low);
    }

    /*
     * keyPressed
     */
    public void keyPressed(KeyEvent e) {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("keyPressed "+e.getKeyCode()+":"+e.getKeyChar());
        if (e.getKeyCode() >= 32 /*|| e.getKeyCode() == 0xA*/) {
            addEvent(ChannelConstants.GUIIN_KEY, e.getID(), e.getKeyCode(), e.getKeyChar());
        }
    }

    /*
     * keyTyped
     */
    public void keyTyped(KeyEvent e) {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("keyTyped "+e);
        if (e.getKeyChar() >= 32) {
            addEvent(ChannelConstants.GUIIN_KEY, e.getID(), e.getKeyCode(), e.getKeyChar());
        } else {
            addEvent(ChannelConstants.GUIIN_KEY, 401, e.getKeyChar(), e.getKeyChar());
        }
    }

    /*
     * keyReleased
     */
    public void keyReleased(KeyEvent e) {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("keyReleased "+e);
        addEvent(ChannelConstants.GUIIN_KEY, e.getID(), e.getKeyCode(), e.getKeyChar());
    }

    /*
     * mousePressed
     */
    public void mousePressed (MouseEvent e) {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("mousePressed "+e);

        // To support single-button Mac users, the combination of left mouse
        // button + CTRL is regarded mouse button 2
        if (isButton1(e) && (e.getModifiers() & InputEvent.CTRL_MASK) == 0) {
            addEvent(ChannelConstants.GUIIN_MOUSE, e.getID(), e.getX(), e.getY());
        } else /*if (isButton2(e))*/ {
            new HibernateDialog(frame, guiInputChannel);
        }
    }

    /*
     * mouseReleased
     */
    public void mouseReleased (MouseEvent e) {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("mouseReleased "+e);
        if (isButton1(e)) {
            addEvent(ChannelConstants.GUIIN_MOUSE, e.getID(), e.getX(), e.getY());
        }
    }

    /*
     * mouseClicked
     */
    public void mouseClicked (MouseEvent e) {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("mouseClicked "+e);
        if (isButton1(e)) {
            addEvent(ChannelConstants.GUIIN_MOUSE, e.getID(), e.getX(), e.getY());
        }
    }

    /**
     * JDK 1.4 doesn't work properly via Invocation API on Mac OS X so the 'MouseEvent.getButton()'
     * method is not available.
     */
    private boolean isButton1(MouseEvent e) {
//        return (e.getButton() == MouseEvent.BUTTON1);
        return ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0);
    }

    /**
     * JDK 1.4 doesn't work properly via Invocation API on Mac OS X so the 'MouseEvent.getButton()'
     * method is not available.
     */
    private boolean isButton2(MouseEvent e) {
//        return (e.getButton() == MouseEvent.BUTTON2);
        return ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0);
    }

    /*
     * mouseEntered
     */
    public void mouseEntered (MouseEvent e) {
    }

    /*
     * mouseExited
     */
    public void mouseExited (MouseEvent e) {
    }

    /*
     * mouseMoved
     */
    public void mouseMoved (MouseEvent e) {
 //       if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("mouseMoved "+e);
 //       addEvent(ChannelConstants.GUIIN_MOUSE, e.getID(), e.getX(), e.getY());
    }

    /*
     * mouseDragged
     */
    public void mouseDragged (MouseEvent e) {
        if (ChannelIO.TRACING_ENABLED) ChannelIO.trace("mouseDragged "+e);
        addEvent(ChannelConstants.GUIIN_MOUSE, e.getID(), e.getX(), e.getY());
    }

    /*
     * close
     */
    public void close() {
        if (frame != null) {
            Frame temp = frame;
            temp.dispose();
            frame = null;
            panel = null;
            display = null;
            offScreenBuffer = null;
            offScreenDisplay = null;
            tracker = null;
        }
    }

}



abstract class C2Image implements java.io.Serializable {
    transient Image img;
    abstract Image getImage(MediaTracker tracker);
}

class RgbImage extends C2Image {
    int hs;
    int vs;
    int stride;
    int[] rgb;

    RgbImage(int hs, int vs, int rgblth, int stride) {
        this.hs = hs;
        this.vs = vs;
        this.stride = stride;
        rgb = new int[rgblth];
    }

    Image getImage(MediaTracker tracker) {
        if (img == null) {
            DirectColorModel colormodel = new DirectColorModel(24, 0x0000ff, 0x00ff00, 0xff0000);
            MemoryImageSource imageSource = new MemoryImageSource(hs, vs, colormodel, rgb, 0, stride );
            img = Toolkit.getDefaultToolkit().createImage(imageSource);
        }
        return img;
    }

    void flush(int[] rgb) {
        if (rgb != null) {
            int[] realrgb = this.rgb;
            if (realrgb.length != rgb.length) {
                System.out.println("Bad flushimage rgb buffer length -- realrgb.length = "+realrgb.length+"rgb.length = "+rgb.length);
                System.exit(1);
            }
            System.arraycopy(rgb, 0, realrgb, 0, realrgb.length);
        }
        img.flush();
    }
}

class MemImage extends C2Image {
    byte[] buf;
    MemImage(byte[] buf) {
        this.buf = buf;
    }
    Image getImage(MediaTracker tracker) {
        if (img == null) {
            img = Toolkit.getDefaultToolkit().createImage(buf);
            tracker.addImage(img, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return img;
    }
}

class FileImage extends C2Image {
    String name;
    FileImage(String name) {
        this.name = name;
    }
    Image getImage(MediaTracker tracker) {
        if (img == null) {
            img = Toolkit.getDefaultToolkit().getImage(name.replace('/', File.separatorChar));
            tracker.addImage(img, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return img;
    }
}



class HibernateDialog extends Dialog {

    GUIInputChannel guiInputChannel;
    final static int HEIGHT=60, WIDTH=100;

    public HibernateDialog(Frame parent, final GUIInputChannel guiInputChannel) {
        super(parent, true);

        Point parentLocation = parent.getLocation();
        int hwidth  = parent.getWidth()/2;
        int hheight = parent.getHeight()/2;
        setLocation((int)parentLocation.getX() + hwidth -(WIDTH/2), (int)parentLocation.getY() + hheight -(HEIGHT/2));

        this.guiInputChannel = guiInputChannel;
        Button y = new Button("Yes");
        Button n = new Button("No");
        setTitle("Hibernate?");
        setLayout(new GridLayout(1,2));
        add(y);
        add(n);
        setSize(new Dimension(WIDTH, HEIGHT));
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we){
                dispose();
            }
        });
        ActionListener action = new ActionListener() {
            public void actionPerformed(ActionEvent ev){
                String cmd = ev.getActionCommand();
                if (cmd.equals("Yes")) {
                    //System.out.println("Hibernating...");
                    guiInputChannel.addToGUIInputQueue(ChannelConstants.GUIIN_HIBERNATE, 0, 0, 0);
                }
                dispose();
            }
        };
        y.addActionListener(action);
        n.addActionListener(action);
        setVisible(true);
    }
}
