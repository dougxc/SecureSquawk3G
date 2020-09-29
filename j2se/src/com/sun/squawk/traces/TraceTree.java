package com.sun.squawk.traces;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import javax.swing.event.*;

/**
 * This subclass of JTree provides extended tree functionality.
 *
 * @author  Doug Simon
 */
public final class TraceTree extends JTree {

    /**
     * Returns an instance of <code>JTree</code> which displays the root node
     * -- the tree is created using the specified data model.
     *
     * @param model  the <code>DefaultTreeModel</code> to use as the data model
     * @return a <code>JTree</code> based on <code>model</code>
     */
    public TraceTree(DefaultTreeModel model) {
        super(model);
        installMouseAndKeyListeners();
    }

    /**
     * Updates all the registered <code>ModifiedTreeExpansionAdapter</code>s to inform them
     * of the modifiers that were present in the last mouse or keyboard event.
     *
     * @param modifiers  the modifiers that were present in the last mouse or keyboard event
     */
    private void updateModifiedTreeExpansionAdapters(int modifiers) {
        TreeExpansionListener[] listeners = getTreeExpansionListeners();
        for (int i = 0; i != listeners.length; ++i) {
            if (listeners[i] instanceof ModifiedTreeExpansionAdapter) {
                ModifiedTreeExpansionAdapter mtea = (ModifiedTreeExpansionAdapter)listeners[i];
                mtea.setModifiers(modifiers);
            }
        }
    }

    /**
     * Installs the mouse and key listener that will modify node expansion. These listeners
     * must preceed and other already installed mouse and key listeners as they must
     * communicate with any registered <code>ModifiedTreeExpansionAdapter</code>s before
     * these expansion listeners are fired.
     */
    private void installMouseAndKeyListeners() {
        MouseListener[] mouseListeners = getMouseListeners();
        for (int i = 0; i != mouseListeners.length; ++i) {
            removeMouseListener(mouseListeners[i]);
        }
        KeyListener[] keyListeners = getKeyListeners();
        for (int i = 0; i != keyListeners.length; ++i) {
            removeKeyListener(keyListeners[i]);
        }

        addMouseListener(new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                updateModifiedTreeExpansionAdapters(e.getModifiers());
            }
        });

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                updateModifiedTreeExpansionAdapters(e.getModifiers());
            }
        });

        for (int i = 0; i != mouseListeners.length; ++i) {
            addMouseListener(mouseListeners[i]);
        }
        for (int i = 0; i != keyListeners.length; ++i) {
            addKeyListener(keyListeners[i]);
        }
    }
}