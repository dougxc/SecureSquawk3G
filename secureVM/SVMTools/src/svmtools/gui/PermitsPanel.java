package svmtools.gui;

import svmtools.PermitManager;
import svmtools.LoadedClass;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.TrustedJavaClassGen;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Trusted;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantDigitalSignature;
import java.util.*;

/**
 * This panel displays a table of the permits that have been attached to a
 * class.
 */

abstract public class PermitsPanel extends TrustedPanel {

    private static final String NEW_CHOICE_COMMAND = "NEW";
    private static final String OLD_CHOICE_COMMAND = "OLD";
    private static final String NEITHER_CHOICE_COMMAND = "NEITHER";

    /**
     * The permits displayed in this panel.
     */
    Hashtable permits;

    /**
     * Populate a given table of rows with the revelant permits (if any)
     * that can be extracted from a set of permits.
     * @param db A table of permits indexed by their key.
     * @param rows The table of rows to populate. For each entry, the key
     * must be a String (i.e. the name of the grantor) and the value is a
     * PermitRow object.
     */
    abstract protected void extractPermitsFromDB(Hashtable db, Hashtable rows);

    /**
     * Populate a given table of rows with the revelant permits (if any)
     * that can be extracted from a Trusted attribute. If there is already
     * an entry in rows for an existing permit, then its 'oldPermit' field
     * is simply updated. Otherwise a new entry is created with a null
     * 'newPermit' value and inserted to rows.
     * @param trusted A non-null Trusted attribute.
     * @param jc The JavaClass encapsulating the Trusted attribute.
     * @param rows The table of rows to populate. For each entry, the key
     * must be a String (i.e. the name of the grantor) and the value is a
     * PermitRow object.
     */
    abstract protected void extractExistingPermits(Trusted trusted, JavaClass jc,
        Hashtable rows);

    /**
     * Update the underlying model based on a user's selection of a permit.
     */
    public abstract void setPermit(String grantor, byte[] permit);

    /**
     * Construct a new PermitsPanel.
     * @param clazz The data model.
     */
    protected PermitsPanel(LoadedClass clazz, PermitManager pm, String title) {
        super(clazz,title);

        JavaClass jc = model.getOriginalClass();
        Trusted trusted = model.getOriginalTrusted();

        permits = new Hashtable();

        // Extract relevant permits from the PermitManager
        Hashtable dbPermits = pm.getPermitsByGrantee(jc.getClassName());
        extractPermitsFromDB(dbPermits, permits);

        // Extract current permits from an existing Trusted
        // attribute (if any).
        if (trusted != null) {
            extractExistingPermits(trusted,jc,permits);
        }

        if (permits.size() == 0) {
            JLabel label = new JLabel("There are no permits available for this class.");
            add(label);
            return;
        }

        // Now initialize the UI of the panel.
        setLayout(new GridBagLayout());

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = lc.CENTER;
        lc.insets = new Insets(3,5,3,5);

        int row = 0;
        for (Iterator i = permits.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry)i.next();
            String name = (String)e.getKey();
            PermitChoice choice = (PermitChoice)e.getValue();

            lc.gridy = row++;

            lc.gridx = 0;
            lc.anchor = lc.EAST;
            add(new JLabel(name+":"),lc);

            lc.anchor = lc.CENTER;
            ButtonGroup group = new ButtonGroup();

            JRadioButton newButton = new JRadioButton("DB permit");
            if (choice.newPermit != null) {
                newButton.setActionCommand(NEW_CHOICE_COMMAND);
                group.add(newButton);
                newButton.addActionListener(choice);
            }
            else
                newButton.setEnabled(false);
            lc.gridx = 1;
            add(newButton,lc);

            JRadioButton oldButton = new JRadioButton("Current permit");
            if (choice.oldPermit != null) {
                oldButton.setActionCommand(OLD_CHOICE_COMMAND);
                group.add(oldButton);
                oldButton.addActionListener(choice);
            }
            else
                oldButton.setEnabled(false);
            lc.gridx = 2;
            add(oldButton,lc);

            JRadioButton neitherButton = new JRadioButton("No permit");
            neitherButton.setActionCommand(NEITHER_CHOICE_COMMAND);
            group.add(neitherButton);
            neitherButton.addActionListener(choice);
            lc.gridx = 3;
            add(neitherButton,lc);

            // Programatically select the "<existing>" or "No permit" button
            // so that the
            // choice is reflected in the underlying model
            if (oldButton.isEnabled())
                oldButton.doClick();
            else
                neitherButton.doClick();
        }


        GridBagConstraints cc = new GridBagConstraints();
        // Set up the constraints for the placement of the labels.
        lc.anchor = lc.WEST;
        lc.fill = lc.BOTH;
        lc.gridx  = 2;
        lc.insets = new Insets(5,5,5,5);
        lc.weightx = 1.0;
        // Set up the constraints for the placement of the combo boxes.
        cc.anchor = cc.WEST;
        cc.fill = cc.BOTH;
        cc.gridx = 1;
        cc.insets = lc.insets;

    }

    /**
     * Provide this dummy implementation to satisfy the contract imposed
     * by the TrustedPanel superclass.
     */
    public void itemStateChanged(ItemEvent e) {
    }

    /**
     * This is the model for the permit choices for a single grantor. It
     * handles synchronization of user actions to the underlying model.
     */
    class PermitChoice implements ActionListener {
        PermitChoice(String grantor, byte[] permit, boolean isNew) {
            this.grantor = grantor;
            if (!isNew)
                this.oldPermit = permit;
            else
                this.newPermit = permit;
        }
        String grantor;
        byte[] oldPermit;
        byte[] newPermit;
        String lastChoice;

        public void actionPerformed(ActionEvent e) {
            String choice = e.getActionCommand();
            if (choice == lastChoice)
                return;
            lastChoice = choice;
            byte[] chosen;
            if (choice == NEW_CHOICE_COMMAND)
                chosen = newPermit;
            else if (choice == OLD_CHOICE_COMMAND)
                chosen = oldPermit;
            else
                chosen = null;
            setPermit(grantor,chosen);
        }
    }
}
