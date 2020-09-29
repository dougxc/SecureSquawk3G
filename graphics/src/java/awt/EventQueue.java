// EventQueue.java
//
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

import java.awt.event.*;
import java.util.*;

/** The kAWT event queue. In kAWT, the event queue has no
    entries an delivers all events immediately. */

public class EventQueue {

    Component mouseOwner;
    int mouseOrgX;
    int mouseOrgY;

    Vector events = new Vector ();

    boolean dragOnTop;

    /** Posts an event.  */

    public void postEvent (AWTEvent event) {
    synchronized (events) {
        if (event.id == MouseEvent.MOUSE_DRAGGED) {
        if (dragOnTop && events.size () > 0)
            events.removeElementAt (events.size ()-1);

        dragOnTop = true;
        }
        else
        dragOnTop = false;

        events.addElement (event);
        events.notify ();
    }
    }


     void dispatch () {

     synchronized (Toolkit.Lock) {

         AWTEvent event;

         synchronized (events) {
         if (events.size () == 0) return;

         event = (AWTEvent) events.elementAt (0);
         events.removeElementAt (0);
         }

         if (event instanceof MouseEvent)
         dispatchMouseEvent ((MouseEvent) event);
         else if (event instanceof KeyEvent)
         dispatchKeyEvent ((KeyEvent) event);
         else if (event instanceof ActiveEvent)
         ((ActiveEvent) event).dispatch ();
         else if (event.id == awtcore.impl.Laf.SCREEN_EVENT) {
         for (int i = 0; i < Toolkit.windows.size (); i++) {
             Window w = (Window) Toolkit.windows.elementAt (i);

             if (w instanceof Frame)
             w.pack ();

             w.repaint ();
         }
         }
         else {
         Object source = event.getSource ();
         if (source instanceof Component)
             ((Component) source).dispatchEvent (event);
         }
     }
     }


    void dispatchMouseEvent (MouseEvent me) {

    Object source = me.getSource ();

    if (!(source instanceof Component)) {
        Window top = Toolkit.getTopWindow ();
        if (top == null) return;

        if (me.id == MouseEvent.MOUSE_PRESSED) {

        while (true) {
            Point p = top.getLocationOnScreen ();
            mouseOwner = top.findComponentAt
            (me.getX () - p.x, me.getY () - p.y);

            if (mouseOwner != null) break;
            if (top.isMenu) {
            top.setVisible (false);
            top = Toolkit.getTopWindow ();
            if (top == null || !top.isMenu) return;
            }
            else return;
        }

        Point p = mouseOwner.getLocationOnScreen ();
        mouseOrgX = p.x;
        mouseOrgY = p.y;
        }

        if (mouseOwner == null) return;

        source = mouseOwner;
        ((AWTEvent) me).setSource (source);
        me.translatePoint (-mouseOrgX, -mouseOrgY);
    }

    ((Component) source).dispatchEvent (me);
    }


    void dispatchKeyEvent (KeyEvent ke) {

    Object source = ke.getSource ();

    if (!(source instanceof Component)) {

        Window top = Toolkit.getTopWindow ();
        if (top == null || top.focus == null) return;

        source = top.focus;
        ((AWTEvent) ke).setSource (source);
    }

    Component target = (Component) source;

    while (target != null) {
        target.dispatchEvent (ke);

        if (ke.consumed) return;

        target = target.parent;
    }

    target = (Component) source;

    if (ke.getID () != KeyEvent.KEY_PRESSED) return;

    switch (ke.getKeyCode ()) {
    case KeyEvent.VK_PROPS:
    case KeyEvent.VK_CANCEL:
        {
        Window top = Toolkit.getTopWindow ();
        if (top == null) break;
        if (top.isMenu) {
            do {
            top.setVisible (false);
            top = Toolkit.getTopWindow ();
            }
            while (top != null && top.isMenu);
            return;
        }

        if (top instanceof Frame) {
            Frame frame = (Frame) top;
            if (frame.menuBar != null) {
            frame.menuBar.show ();
            return;
            }
        }
        }
    case KeyEvent.VK_ENTER:
    case KeyEvent.VK_SPACE:
    case KeyEvent.VK_ACCEPT:
        target.action (ke);
        break;

    case KeyEvent.VK_DOWN:
    case KeyEvent.VK_TAB:
    case KeyEvent.VK_RIGHT:
        target.getNext (true, true).requestFocus ();
        break;

    case KeyEvent.VK_UP:
    case KeyEvent.VK_LEFT:
        target.getPrev (true, false).requestFocus ();
        break;
    }
    }



    public void invokeLater (Runnable runit) {

    postEvent (new InvocationEvent (this, runit, null, true));
    }


    public void invokeAndWait (Runnable runit)
    throws InterruptedException,
           java.lang.reflect.InvocationTargetException {

    Object lock = new Object ();

    synchronized (lock) {

        postEvent (new InvocationEvent (this, runit, lock, true));

        lock.wait ();
    }
    }
}

