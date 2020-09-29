package svmtools.gui;

import javax.swing.JTable;
import javax.swing.table.*;

/**
 * This class provides functions for building certain Swing components in a
 * consistent manner.
 */

public class Factory {

    /**
     * Create a JTable that doesn't allow row or column selection or for
     * columns to be re-ordered.
     */
    static public JTable createTable() {
        JTable table = new JTable();

        table.getTableHeader().setReorderingAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(false);
        return table;
    }
}
