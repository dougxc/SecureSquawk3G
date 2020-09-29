package svmtools;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListDataListener;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.Constants;
import java.util.*;
import java.io.*;

/**
 * This class manages a set of loaded classes.
 */
public class ClassManager extends AbstractListModel
                          implements ListSelectionListener {

    /**
     * The CSP.
     */
    private CSP csp;

    /**
     * This is the collection of loaded classes.
     */
    private SortedMap classes = new TreeMap();
    /**
     * This is the sorted set of keys.
     */
    private String classNames[] = new String[0];

    /**
     * Construct a new class manager.
     * @param csp The CSP that helps with the loading of classes.
     */
    public ClassManager(CSP csp) {
        this.csp = csp;
    }

    /**
     * Load a class from a given file.
     * @param source The stream containing the classfile.
     * @param path The path of the file that the classfile was loaded from.
     * If the class was not loaded from a file (e.g. it came from a JAR) then
     * this value is null.
     * @exception IOException if the class could not be loaded.
     */
    public void loadClass(InputStream source, String path)
        throws IOException
    {
        String errMsg = null;
        JavaClass clazz = null;

        ClassParser parser = new ClassParser(source,path);
        clazz = parser.parse();
        String name = clazz.getClassName();

        if (classes.containsKey(name))
            throw new IOException("Class '"+name+"' already loaded - close class first");

        LoadedClass lc = new LoadedClass(clazz,new File(path), csp);
        classes.put(name,lc);
        int index = updateListView();
        fireIntervalAdded(this,index,index);
        fireClassEvent(CLASS_LOADED, name, lc);
    }

    /**
     * Return the LoadedClass for a given name.
     * @param name The name of the class to find.
     * @return the class corresponding to name or null if it doesn't exist.
     */
    public LoadedClass findClass(String name) {
        return (LoadedClass)classes.get(name);
    }

    /**
     * Remove a class.
     * @param name The name of class to remove.
     * @return the removed class or null if it didn't exist.
     */
    public LoadedClass removeClass(String name) {
        LoadedClass lc = null;
        if (classes.containsKey(name)) {
            lc = (LoadedClass)classes.remove(name);

            int index = updateListView();
            // Inform the JList that the row was removed
            fireIntervalRemoved(this,index,index);

            // Inform the JList to deselect the row that was removed

            fireClassEvent(CLASS_REMOVED, name, lc);
        }
        return lc;
    }

    /**
     * Translates a list selection event into a class selection event and
     * subsequently notifies all class event listeners.
     */
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting())
            return;
        ListSelectionModel model = (ListSelectionModel)e.getSource();
        int index = model.getMaxSelectionIndex();
        String name = classNames[index];
        LoadedClass lc = (LoadedClass)classes.get(name);
        if (lc != null)
            fireClassEvent(CLASS_SELECTED, name, lc);

    }

    // --------- ClassEventListener interface and related methods ----------

    private static final int CLASS_SELECTED = 1;
    private static final int CLASS_LOADED   = 2;
    private static final int CLASS_REMOVED  = 3;

    /**
     * The listener that's notified when a class manager event occurs.
     */
    static interface ClassEventListener extends EventListener {
        /**
         * A class is made the current class in some view.
         */
        void classSelected(String name, LoadedClass lc);

        /**
         * A class is loaded in the manager.
         */
        void classLoaded(String name, LoadedClass lc);

        /**
         * A class is removed from the manager.
         */
        void classRemoved(String name, LoadedClass lc);
    }

    /**
     * Add a listener from the list that's notified each time a
     * class manager event occurs.
     * @param l the ListDataListener
     */
    public void addClassEventListener(ClassEventListener l) {
        listenerList.add(ClassEventListener.class,l);
    }

    /**
     * Remove a listener from the list that's notified each time a
     * class manager event occurs.
     * @param l the ListDataListener
     */
    public void removeListDataListener(ClassEventListener l) {
    listenerList.remove(ClassEventListener.class, l);
    }

    /**
     * Notify all ClassEventListeners of a class event.
     */
    private void fireClassEvent(int type, String name, LoadedClass lc) {
        Object[] listeners = listenerList.getListenerList();

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
        if (listeners[i] == ClassEventListener.class) {
                ClassEventListener l = (ClassEventListener)listeners[i+1];
                switch (type) {
                case CLASS_SELECTED: l.classSelected(name, lc); break;
                case CLASS_LOADED: l.classLoaded(name, lc); break;
                case CLASS_REMOVED: l.classRemoved(name, lc); break;
        }
        }
    }
    }

    // ------------------------------------------------------------------------

    /**
     * Update the list view of the collection of classes.
     * @return the index of the element that was added/deleted
     */
    private int updateListView() {

        String newClassNames[] = new String[classes.size()];
        classes.keySet().toArray(newClassNames);

        // Find where the class was added to/deleted from the list
        int length = Math.min(newClassNames.length,classNames.length);
        int index = 0;
        while (index != length && classNames[index].equals(newClassNames[index]))
            index++;
        classNames = newClassNames;
        return index;
    }

    // -------------------- ListModel implementation -------------------------
    public int    getSize()              { return classNames.length; }
    public Object getElementAt(int index){ return classNames[index];}
}
