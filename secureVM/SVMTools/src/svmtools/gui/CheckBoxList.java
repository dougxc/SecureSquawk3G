package svmtools.gui;

import java.awt.Component;
import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.ListCellRenderer;
import javax.swing.AbstractListModel;
import javax.swing.border.*;
import javax.swing.JCheckBox;
import java.awt.event.*;
import java.util.Vector;
import java.util.Hashtable;

/**
 * A scrollable list of checkboxes.
 */

public class CheckBoxList extends JList {

    /**
     * The data model.
     */
    Selectable[] items;

    /**
     * Interface that elements of the list must implement.
     */
    static protected class Selectable {
        boolean selected;
        Object  object;

        public Selectable(Object object, boolean selected) {
            this.object = object;
            this.selected = selected;
        }
    }

    protected static class Renderer extends JCheckBox implements ListCellRenderer {
        protected Renderer() {
            super();
            setOpaque(true);
            setBorder(new EmptyBorder(1,15,1,1));
        }
        public Component getListCellRendererComponent(JList list,
            Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            setText(((Selectable)value).object.toString());
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            setSelected(((Selectable)value).selected);
            setFont(list.getFont());
            return this;
        }
    }

    /**
     * (Re)set the model.
     */
    public void setModel(Object[] model) {
        Hashtable map = new Hashtable();
        if (items != null) {
            for (int i = 0; i != items.length; i++) {
                Object key = items[i].object;
                Boolean value = new Boolean(items[i].selected);
                map.put(key,value);
            }
        }
        items = new Selectable[model.length];
        for (int i = 0; i != items.length; i++) {
            Object key = model[i];
            Object value = map.get(key);
            boolean checked = (value != null &&
                ((Boolean)(value)).booleanValue());
            items[i] = new Selectable(model[i],checked);
        }
        setModel(new AbstractListModel() {
                public int getSize() { return items.length; }
                public Object getElementAt(int i) { return items[i]; }
            });
    }

    /**
     * Construct a new check list.
     * @param model
     * @param
     */
    public CheckBoxList() {
        super();
        setCellRenderer(new Renderer());
        addMouseListener(new MouseListener() {
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
            public void mouseClicked(MouseEvent e) {
                int index = CheckBoxList.this.getSelectedIndex();
                if (index < 0)
                    return;
                Selectable item = CheckBoxList.this.items[index];
                item.selected = !item.selected;
                CheckBoxList.this.repaint();
            }
        });
    }

    /**
     * Select all.
     */
     public void checkAll(boolean selected) {
        for (int i = 0; i != items.length; ++i)
            items[i].selected = selected;
        repaint();
     }

     /**
      * Return all selected items.
      */
    public Object[] getCheckedItems() {
        Vector v = new Vector(items.length);
        for (int i = 0; i != items.length; i++)
            if (items[i].selected)
                v.add(items[i].object);
        return v.toArray();
    }

}