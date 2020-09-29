// Toolkit.java
//
// 2000-10-06 SH Splitted into abstract and implementation classes
// 2000-09-08 MK Added new Licensetext
//
//#include ..\..\license.txt
//
// kAWT version 0.95
//
// Copyright (C) 1999-2000 by Michael Kroll & Stefan Haustein GbR, Essen
//
// Contact: kawt@kawt.de
// General Information about kAWT is available at: http://www.kawt.de
//
// Using kAWT for private and educational and in GPLed open source
// projects is free. For other purposes, a commercial license must be
// obtained. There is absolutely no warranty for non-commercial use.
//
//
// 1. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO
//    WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE
//    LAW.  EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT
//    HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT
//    WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT
//    NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
//    FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS TO THE
//    QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE
//    PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY
//    SERVICING, REPAIR OR CORRECTION.
//
// 2. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN
//    WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY
//    MODIFY AND/OR REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE
//    LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL,
//    INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR
//    INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF
//    DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU
//    OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY
//    OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN
//    ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//
//    END OF TERMS AND CONDITIONS
//
//#endinclude


package java.awt;

import java.io.*;
import java.util.*;
import java.awt.image.ColorModel;
import awtcore.impl.Laf;

public abstract class Toolkit {

    // some package-internal constants / shortcuts


    static int pixelBits = System.getProperty ("awtcore.colordepth") == null
        ? 1 : Integer.parseInt (System.getProperty ("awtcore.colordepth"));
    static boolean colorKvm = System.getProperty ("awtcore.colordepth") != null;

    //   static int scrW = 160;
    //    static int scrH = 160;

    static Vector windows = new Vector ();
    static EventQueue eventQueue = new EventQueue ();
    static AwtThread awtThread = new AwtThread ();

    static Object Lock = new Object ();

    public static Window getTopWindow () {
        int sz = windows.size ();
        return sz == 0 ? null : (Window) windows.elementAt (sz-1);
    }


    static String platform = System.getProperty  ("microedition.platform");
    static String classbase = initclassbase ();

    static Class graphicsImpl = null;

    // last call, others must have been init.ed already!
    static Toolkit defaultToolkit = init ();

    static FontMetrics defaultFontMetrics =
        defaultToolkit.getFontMetrics (new Font ("plain", Font.PLAIN, 8));


    static String initclassbase () {

    // don't ask me why this shortcut is necessary....!
    //System.out.println ("icb0:"+platform);

    if ("Jbed".equals(platform))
        return "awtcore.impl.kjava";

    if ("palm".equals (platform))
        return "awtcore.impl.kjava";


    if (System.getProperty ("awtcore.classbase") != null)
        return System.getProperty ("awtcore.classbase");

    try {
            Class.forName ("com.sun.kjava.Graphics");
        if (platform == null) platform = "palm";
            return "awtcore.impl.kjava";
        }
        catch (Exception e) {}
        try {
            Class.forName ("javax.microedition.lcdui.Graphics");
        if (platform == null) platform = "midp";
            return "awtcore.impl.midp";
        }
        catch (Exception e) {}
        try {
            Class.forName ("net.rim.device.api.system.Graphics");
        if (platform == null) platform = "rim";
            return "awtcore.impl.rim";
        }
        catch (Exception e) {}

        throw new RuntimeException
        ("unknown base lib and property awtcore.classbase not set!");

    }

   /** creates default toolkit and fills static shortcut variables */

    static Toolkit init () {

    //System.out.println ("ti0");

    Runtime.getRuntime ().gc (); // dont ask why this is neccessary...

    //System.out.println ("ti1");

    try {
        Laf.laf = (Laf) Class.forName (classbase+".LafImpl").newInstance ();
    }
    catch (Exception e) {
        Laf.laf = new Laf ();
    }

    //System.out.println ("*** ti2/cb: "+classbase);

        try {
        //System.out.println ("*** ti3");
            graphicsImpl = Class.forName (classbase+".GraphicsImpl");
        //System.out.println ("*** ti4");
            return (Toolkit) Class.forName
        (classbase+".ToolkitImpl").newInstance ();
        }
        catch (Exception e) {
        //System.out.println ("*** ti5: "+e);
            throw new RuntimeException ("awtcore init failure: "+e.toString ());
        }
    }

    /** starts the awtThread */

    protected Toolkit () {
        awtThread.start ();
    }

    /** not abstract, just does nothing by default */

    public void beep () {
    }


    public ColorModel getColorModel () {
        return new ColorModel (pixelBits);
    }


    static public Toolkit getDefaultToolkit () {
        return defaultToolkit;
    }


    public EventQueue getSystemEventQueue () {
        return eventQueue;
    }


    public abstract Dimension getScreenSize ();


    public static void flushRepaint () {
    synchronized (Lock) {

        Window top = getTopWindow ();
        if (top != null) {
        top.flushRepaint ();
        defaultToolkit.sync ();
        }
    }
    }


    public void sync () {
    }

    static Graphics createGraphics () {
        try {
            return (Graphics) graphicsImpl.newInstance ();
        }
        catch (Exception e) {
            throw new RuntimeException ("createGraphics failed: "+e.toString ());
        }
    }


    public Image createImage (String ressourceName) {
        throw new RuntimeException ("jar ressources not yet supported");
    }

    public Image getImage (String ressourceName) {
        throw new RuntimeException ("getImage not yet supported");
    }

    public Image createImage (java.awt.image.ImageProducer producer) {
        throw new RuntimeException ("ImageProducer not yet supported");
    }

    public Image createImage(byte [] data) {
        return createImage(data, 0, data.length);
    }
    public abstract Image createImage(byte[] data, int offset, int length);



    public abstract FontMetrics getFontMetrics (Font font);


    public static String getProperty (String key, String dflt) {
    if (key.equals ("awtcore.classbase"))
        return classbase;
    else
        return dflt;
    }

    public void flushScreen() {};

}



