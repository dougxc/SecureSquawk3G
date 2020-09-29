// kAWT - kilobyte Abstract Window Toolkit
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

/*  $Author: nshaylor $
 *  $Date: 2004/04/01 03:14:29 $
 *  $Revision: 1.1 $
 *  $State: Exp $
 */
package awtcore;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;


class TabControl extends Canvas
    implements MouseListener, KeyListener, FocusListener {

    int sel;  // selected
    int focus;

    int first = 0;
    int last;

    boolean mono = SystemColor.control.equals (Color.white);
    boolean hasFocus;

    Vector titles = new Vector ();
    TabbedPane tp;
    FontMetrics fm;


    TabControl (TabbedPane _tp) {
        tp = _tp;
        setBackground (SystemColor.control);
        addMouseListener (this);
        addKeyListener (this);
    addFocusListener (this);
    //  setBackground (Color.green);
    }


    public void add (String title) {
        titles.addElement (title);
        if (sel < 0)
            sel = 0;
        repaint ();
    }


    public void remove (int index) {
        titles.removeElementAt (index);
        if (sel >= index)
            sel--;
        repaint ();
    }


    public Dimension getMinimumSize () {
        fm = getFontMetrics (getFont ());
    int mx = 0;
    for (int i = titles.size ()-1; i >= 0; i--)
        mx = Math.max (mx, 10 + 2 * fm.stringWidth
               (" |< " + titles.elementAt (i)));

        return new Dimension (mx, fm.getHeight () + (mono ? 5 : 8));
    }


    public void focusGained (FocusEvent fe) {
    hasFocus = true;
    repaint ();
    }


    public void focusLost (FocusEvent fe) {
    hasFocus = false;
    repaint ();
    }


    public Dimension getPreferredSize () {
        return getMinimumSize ();
    }


    public void drawTab (Graphics g, int x, int y, int w, int h,
                         int dt, String text, boolean focus) {

    focus = focus & hasFocus;

        if (mono) {
            g.setColor (Color.black);
            g.drawLine (x, y+h-1, x, y+2);
            g.drawLine (x, y+2, x+2, y);
            g.drawLine (x+2, y, x+w-2, y);
            g.drawLine (x+w-2, y, x+w, y+2);  // one pixel wider than spec.
            g.drawLine (x+w, y+2, x+w, y+h-1);
            g.setColor (focus ? Color.black : Color.white);
        g.fillRect (x + 5 + dt, y + 2, w - 10 - dt, h - 5);
            g.setColor (focus ? Color.white : Color.black);
            g.drawString (text, x + 5 + dt, y + 2 + fm.getAscent ());
        }
        else {
            g.setColor (Color.white);
            g.drawLine (x,   y+h-1, x,   y+2);     // ganz links
            g.drawLine (x+1, y+h-1, x+1, y+1);

            g.drawLine (x+3, y,   x+w-3, y);       // ganz oben
            g.drawLine (x+2, y+1, x+w-2, y+1);

            g.setColor (Color.black);           // ganz rechts
            g.drawLine (x+w-1, y+2, x+w-1, y+h-1);

            g.setColor (Color.gray);
            g.drawLine (x+w-2, y+1, x+w-2, y+h-2);

            g.setColor (SystemColor.controlText);
            g.drawString (text, x + 5 + dt, y + 4 + fm.getAscent ());
        }
    }


    public void paint (Graphics g) {

        int overlap = mono ? 0 : 2;
        int ofs = overlap;
        int selOfs = ofs;
        int selW = 0;


        fm = g.getFontMetrics();

    Dimension d = getSize ();

    int cnt = titles.size ();
    int ddd = fm.stringWidth ("|<")+10;

        if (cnt > 0) {

        if (first >= cnt) first = cnt-1;

            for (int i = first; i < cnt; i++) {

        String title = (String) titles.elementAt (i);
                int w = fm.stringWidth (title) + 10;

        if (last != first && i != first && ofs + w + ddd > d.width) {
            drawTab (g, ofs, 2, ddd, d.height, 0, ">", focus == i);
            break;
        }
                else if (i == sel) {
                    selOfs = ofs;
                    selW = w;
                }
                else {
                    drawTab (g, ofs, 2, w, d.height, 0, title, focus == i);
        }
                ofs += w;
        last = i;
            }

        if (first != 0 && last == cnt-1)
        drawTab (g, ofs, 2, ddd, d.height, 0, "|<", focus == cnt);

        if (sel >= first && sel <= last)
        drawTab (g, selOfs-overlap, 0,
             selW+overlap+overlap, d.height, overlap,
             (String) titles.elementAt (sel),
             hasFocus  &&  focus == sel);

            if (!mono) {
                g.setColor (Color.white);
                g.drawLine (0, d.height-2, selOfs-1, d.height-2);

                g.drawLine (selOfs + selW+2, d.height-2,
                            getSize ().width, d.height-2);
            }
            else
        g.setColor (Color.black);

            g.drawLine (0, d.height-1, selOfs-1, d.height-1);
            g.drawLine (selOfs + selW+1, d.height-1,
                        getSize ().width, d.height-1);
        }
    }

    public void mouseClicked (MouseEvent e) {}
    public void mouseEntered (MouseEvent e) {}
    public void mouseExited (MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent me) {

        int xofs = 0;
        int mx = me.getX ();

        Graphics g = getGraphics();
        if (g == null) return;


        for (int i = first; i <= last; i++) {

            int nextOfs = xofs + 10 +
                fm.stringWidth ((String) titles.elementAt(i));

            if (mx >= xofs && mx < nextOfs) {
                tp.setSelectedIndex (i);
                return;
            }
            xofs = nextOfs;
        }

    if (last != titles.size ()-1) {
        first = last+1;
        repaint ();
    }
    else if (first != 0) {
        first = 0;
        repaint ();
    }
    }


    public void keyPressed (KeyEvent e) {
        switch (e.getKeyCode ()) {
        case KeyEvent.VK_RIGHT:
        case KeyEvent.VK_DOWN:
        case KeyEvent.VK_TAB:
            if (focus < last + (first != 0 || last < titles.size()-1 ? 1 : 0)) {
                focus++;
                e.consume ();
                repaint ();
            }
            break;

        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_UP:
            if (focus > first) {
                focus--;
                e.consume ();
                repaint ();
            }
            break;
        case KeyEvent.VK_SPACE:
        case KeyEvent.VK_ENTER:
        case KeyEvent.VK_ACCEPT:
        if (focus > last) {
        if (focus >= titles.size ()) {
            first = 0;
            focus = 0;
        }
        else {
            first = last;
            focus = last;
        }
        repaint ();
        }
        else
        tp.setSelectedIndex (focus);
            e.consume ();
            break;
        }
    }


    public void keyReleased (KeyEvent e) {
    }


    public void keyTyped (KeyEvent e) {
    }
}



public class TabbedPane extends Panel implements ItemSelectable {

    CardLayout cardLayout;
    Panel mainPanel;
    TabControl tabControl;
    ItemListener itemListener;

    public TabbedPane() {
        super (new BorderLayout ());

        cardLayout = new CardLayout();
        mainPanel = new Panel (cardLayout);
        tabControl = new TabControl (this);

        add ("North", tabControl);
        add ("Center", mainPanel);
    }


    public void addItemListener (ItemListener l) {
        if (itemListener != null)
            throw new RuntimeException ("Too Many Listeners");

        itemListener = l;
    }

    /**
     * Adds a component represented by a title to this tabbedpane.
     */

    public void addTab (String title, Component component) {
        mainPanel.add (title, component);
        tabControl.add (title);
    }


    /**
     * Returns the currently selected index for this tabbedpane.
     */

    public int getSelectedIndex () {
        return tabControl.sel;
    }

    /**
     * Sets the selected index for this tabbedpane and displays corresponding component.
     */


    public Object [] getSelectedObjects () {
        Object [] res = new Object [] {mainPanel.getComponent (tabControl.sel)};
        return res;
    }


    public void setSelectedIndex (int index) {
        int oldsel = tabControl.sel;

        if (index >=
            0 && index < tabControl.titles.size()
            && index != tabControl.sel) {

            tabControl.sel = index;

            if (itemListener != null)
                itemListener.itemStateChanged
                    (new ItemEvent
                        (this, ItemEvent.ITEM_STATE_CHANGED,
                         mainPanel.getComponent (oldsel),
                         ItemEvent.DESELECTED));


            cardLayout.show
                (mainPanel, (String) tabControl.titles.elementAt
                 (tabControl.sel));

            tabControl.repaint ();

            if (itemListener != null)
                itemListener.itemStateChanged
                    (new ItemEvent
                        (this, ItemEvent.ITEM_STATE_CHANGED,
                         mainPanel.getComponent (tabControl.sel),
                         ItemEvent.SELECTED));
        }
    }


    public void removeItemListener (ItemListener l) {
        if (itemListener == l) itemListener = null;
    }


    public void removeTab (int index) {
        mainPanel.remove (mainPanel.getComponent (index));
        tabControl.remove (index);
    }
}










