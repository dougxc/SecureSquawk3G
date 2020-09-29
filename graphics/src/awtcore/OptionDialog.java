//
// kAWT - Kilobyte Abstract Window Toolkit
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

package awtcore;

import java.awt.*;
import java.awt.event.*;

public class OptionDialog extends Dialog implements ActionListener {

    public static final int CLOSED_OPTION = 0;
    public static final int OK_OPTION = 1;
    public static final int YES_OPTION = 2;
    public static final int NO_OPTION = 4;
    public static final int CANCEL_OPTION = 8;
    public static final int INPUT_OPTION = 16;


    TextField inputLine;
    Panel buttons = new Panel ();
    int pressed;


    /** The OptionDialog is similar to the swing class JOptionPane,
    except that it is derived from dialog directly.  However,
    several of the static showXXX methods of JOptionPane are also
    available here. */


    public OptionDialog (Frame owner, String message, String title,
             String deflt, int options) {

        super (owner, title, true);

    Panel labels = new Panel (new GridLayout (0,1));
    Label label0 = new Label (message);
    labels.add (label0);

    add ("North", labels);
    if ((options & INPUT_OPTION) != 0) {
        inputLine = new TextField (deflt);
        add ("Center", inputLine);
        inputLine.requestFocus ();
    }

    addButton (options, YES_OPTION, "yes");
    addButton (options, NO_OPTION, "no");
    addButton (options, OK_OPTION, "ok");
    addButton (options, CANCEL_OPTION, "cancel");

    add ("South", buttons);
    pack ();

    WordWrap ww = new WordWrap
        (getFontMetrics (getFont ()), message, labels.getSize ().width);

    labels.removeAll ();

    int i0 = 0;

    while (true) {
        int i1 = ww.next ();
        if (i1 == -1) break;
        int lend = i1;
        if (lend > 0 && message.charAt (lend-1) <= ' ')
        lend--;

        labels.add (new Label (message.substring (i0, lend)));
        i0 = i1;
    }

        pack ();
    show ();
    }

    /*
    public static Frame getFrame (Component component) {

    while (component.getParent () != null)
        component = component.getParent ();

    Window win = (Window) component;

    while (win.getOwner () != null) {
        win = win.getOwner ();
    }

    return (Frame) win;
    }
    */

    void addButton (int options, int code, String label) {
    if ((options & code) != 0) {
        Button button = new Button (label);
        buttons.add (button);
        button.setActionCommand (""+code);
        button.addActionListener (this);
    }
    }


    public void actionPerformed (ActionEvent e) {

    pressed = Integer.parseInt (e.getActionCommand ());
    dispose ();
    }


    public static int showConfirmDialog (Frame owner,
                     String msg, String title) {
    return showConfirmDialog
        (owner, msg, title, YES_OPTION | NO_OPTION | CANCEL_OPTION);
    }


    public static int showConfirmDialog (Frame owner,
                     String msg, String title,
                     int type) {

    return new OptionDialog (owner, msg, title, null, type).pressed;
    }


    public static String showInputDialog (Frame owner, String msg) {
    OptionDialog od = new OptionDialog
        (owner, msg, "Input", "",
         OK_OPTION | CANCEL_OPTION | INPUT_OPTION);

    return od.pressed == OK_OPTION ? od.inputLine.getText () : null;
    }


    public static void showMessageDialog (Frame owner, String msg) {
    OptionDialog od = new OptionDialog
        (owner, msg, "Message", "Message", OK_OPTION);
    }
}












