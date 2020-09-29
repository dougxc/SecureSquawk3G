// ProgressBar.java
//
// 2000-09-08 MK Added new Licensetext
// 2000-06-26 MK Fixed a repaint bug
// 2000-05-20 SH Package changed
// 2000-05-19 MK drawing improved
// 2000-05-18 SH initial version (copied from infolayer)
//
//#include ..\license.txt
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


package awtcore;

import java.awt.*;
import java.awt.event.*;


public class ProgressBar extends Canvas {
    int curr = 0;
    int max;

    public ProgressBar () {
    max = 100;
    }

    public ProgressBar (int _max) {
    max = _max;
    }


    public void setValue (int _curr) {
    curr = _curr;
    paint (getGraphics ());
    }

    public void setMax (int _max) {
    max = _max;
    }

    public Dimension getPreferredSize () {
    return new Dimension (100, 20);
    }

    public Dimension getMinimumSize () {
    return new Dimension (10, 10);
    }



    public void paint (Graphics g) {

    Dimension d = getSize ();
    int xp = (d.width-4) * curr / max;

    g.setColor (Color.black);
    g.drawRect (0, 0, d.width-1, d.height-1);

    g.setColor (Color.white);
    g.drawRect (1, 1, d.width-3, d.height-3);

    g.setColor (SystemColor.activeCaption);

    g.fillRect (2, 2, xp, d.height-4);
    g.setColor (Color.white);
    g.fillRect (xp+2, 2, d.width-xp-4, d.height-4);
    }
}




