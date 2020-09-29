package com.sun.squawk.traces;

import javax.swing.event.*;

/**
 * An adapter that's notified when a tree expands or collapses a node.
 */
public abstract class ModifiedTreeExpansionAdapter implements TreeExpansionListener {

    final int modifiers;
    private boolean modify;

    /**
     * Creates a listener that is interested to know if certain modifiers were present in the event
     * that caused a tree node to be expanded or collapsed.
     *
     * @param modifiers  the modifiers that the listener is interested in
     */
    public ModifiedTreeExpansionAdapter(int modifiers) {
        this.modifiers = modifiers;
    }

    /**
     * Called whenever an item in the tree has been expanded.
     *
     * @param e         the event detailing the expansion
     * @param modified  true if one of the modifiers specified at construction of this
     *                  object was present in the event that caused the expansion
     */
    public void treeExpanded(TreeExpansionEvent e, boolean modified) {}

    /**
     * Called whenever an item in the tree has been collapsed.
     *
     * @param e         the event detailing the collapse
     * @param modified  true if one of the modifiers specified at construction of this
     *                  object was present in the event that caused the collapse
     */
    public void treeCollapsed(TreeExpansionEvent e, boolean modified) {}

    void setModifiers(int modifiers) {
        modify |= ((this.modifiers & modifiers) != 0);
    }

    /**
     * Redirects notification to {@link #treeExpanded(TreeExpansionEvent, boolean)}.
     */
    public final void treeExpanded(TreeExpansionEvent event) {
        boolean modified = modify;
        modify = false;
        treeExpanded(event, modified);
    }

    /**
     * Redirects notification to {@link #treeCollapsed(TreeExpansionEvent, boolean)}.
     */
    public final void treeCollapsed(TreeExpansionEvent event) {
        boolean modified = modify;
        modify = false;
        treeCollapsed(event, modified);
    }
}
