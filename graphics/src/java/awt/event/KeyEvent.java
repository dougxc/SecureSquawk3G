// KeyEvent.java
//
// 2000-09-08 MK Added new Licensetext
//
//#include ..\..\..\license.txt
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

package java.awt.event;

import java.awt.*;

public class KeyEvent extends InputEvent {

    public static final int KEY_TYPED = 400;
    public static final int KEY_PRESSED = 401;
    public static final int KEY_RELEASED = 402;

    public static final char CHAR_UNDEFINED = 0;
    public static final int VK_UNDEFINED = 0;

    public static final int VK_ACCEPT = 0x01e;
    public static final int VK_ALT = 0x12;
    public static final int VK_BACK_SPACE = '\b';
    public static final int VK_CANCEL = 0x03;
    public static final int VK_CAPS_LOCK = 0x14;
    public static final int VK_CLEAR = 0x0c;
    public static final int VK_CONTROL = 0x11;
    public static final int VK_DOWN = 0x28;
    public static final int VK_ENTER = '\n';
    public static final int VK_ESCAPE = 0x1B;
    public static final int VK_END = 0x23;
    public static final int VK_HOME = 0x24;
    public static final int VK_LEFT = 0x25;
    public static final int VK_PAGE_DOWN = 0x22;
    public static final int VK_PAGE_UP = 0x21;
    public static final int VK_PAUSE = 0x13;
    public static final int VK_PROPS = 0x0ffca;
    public static final int VK_RIGHT = 0x27;
    public static final int VK_SHIFT = 0x10;
    public static final int VK_SPACE = 0x20;
    public static final int VK_TAB = '\t';
    public static final int VK_UP = 0x26;

    int keyCode;
    char keyChar;

    public KeyEvent (Component source, int id, long when,
             int modifiers, int keyCode) {
    this (source, id, when, modifiers, keyCode, CHAR_UNDEFINED);
    }

    public KeyEvent (Component source, int id, long when, int modifiers,
             int keyCode, char keyChar) {

    super (source, id);

    this.keyCode = keyCode;
    this.keyChar = keyChar;
    }

    public char getKeyChar () {
    return keyChar;
    }

    public int getKeyCode () {
    return keyCode;
    }
}
