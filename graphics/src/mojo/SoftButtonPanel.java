package mojo;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.SoftButtonMenu;

public class SoftButtonPanel extends Panel implements MouseListener,
                                                MouseMotionListener{
    private Dimension dim;
    private Command left;
    private Command right;
    private static final int TEXT_Y = 14;
    private static final int TEXT_X = 3;
    private FontMetrics fm;
    private boolean over_left;
    private boolean over_right;

    public SoftButtonPanel() {
        dim = new Dimension(Main.WIDTH, Main.SOFTBUTTON_HEIGHT);
        setBackground(Color.yellow);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setLeft(Command left) {
        this.left = left;
        repaint();
    }

    public void setRight(Command right) {
        this.right = right;
        repaint();
    }

    public void reset(Displayable d) {
        SoftButtonMenu sbm = SoftButtonMenu.getInstance();
        Vector cmds = d.getCommands();

        switch (cmds.size()) {
            case 0: setLeft(null);
                    setRight(null);
                    break;

            case 1: setLeft((Command)cmds.elementAt(0));
                    setRight(null);
                    break;

            case 2: setLeft((Command)cmds.elementAt(0));
                    setRight((Command)cmds.elementAt(1));
                    break;

            default: setLeft((Command)cmds.elementAt(0));
                    setRight(sbm.getCommand());
                    break;
        }
    }

    public void paint(Graphics g) {
        fm = g.getFontMetrics();
        int advance;

        g.setColor(Color.lightGray);

        if (over_left && left != null) {
            g.fillRect(0, 0, fm.stringWidth(left.getLabel()) + 2 * TEXT_X, getHeight());
        }

        if (over_right && right != null) {
            advance = fm.stringWidth(right.getLabel());

            g.fillRect(getWidth() - (advance + 2 * TEXT_X), 0, advance + 2 * TEXT_X, getHeight());
        }

        g.setColor(Color.black);

        if (left != null) {
            advance = fm.stringWidth(left.getLabel());

            g.drawString(left.getLabel(), TEXT_X, TEXT_Y);
            g.drawLine(TEXT_X, TEXT_Y + 2, TEXT_X + advance, TEXT_Y + 2);
        }

        if (right != null) {
            advance = fm.stringWidth(right.getLabel());

            g.drawString(right.getLabel(), Main.WIDTH - (advance + TEXT_X), TEXT_Y);
            g.drawLine(Main.WIDTH - (advance + TEXT_X), TEXT_Y + 2,
                                Main.WIDTH - TEXT_X, TEXT_Y + 2);
        }
    }

    public Dimension getMinimumSize() {
        return dim;
    }

    public Dimension getMaximumSize() {
        return dim;
    }

    public Dimension getPreferredSize() {
        return dim;
    }

    public void mouseClicked(java.awt.event.MouseEvent e) {
        if (fm == null || (left == null && right == null)) return;

        if (left != null && e.getX() <= fm.stringWidth(left.getLabel()) + TEXT_X) {
            Display.getDisplay().commandAction(left.getID());
        }

        if (right != null && e.getX() >= Main.WIDTH - (fm.stringWidth(right.getLabel()) + TEXT_X)) {
            Display.getDisplay().commandAction(right.getID());
        }
    }

    public void mouseEntered(java.awt.event.MouseEvent e) {
    }

    public void mouseExited(java.awt.event.MouseEvent e) {
        over_left = false;
        over_right = false;
        repaint();
    }

    public void mousePressed(java.awt.event.MouseEvent e) {
    }

    public void mouseReleased(java.awt.event.MouseEvent e) {
    }

    public void mouseMoved(java.awt.event.MouseEvent e) {
        boolean pol = over_left;
        boolean por = over_right;

        if (left != null) over_left = e.getX() <= fm.stringWidth(left.getLabel()) + TEXT_X;
        if (right != null) over_right = e.getX() >= Main.WIDTH - (fm.stringWidth(right.getLabel()) + TEXT_X);

        if (pol != over_left || por != over_right) repaint();
    }

    public void mouseDragged(java.awt.event.MouseEvent e) {
    }
}
