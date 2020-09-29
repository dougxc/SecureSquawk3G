package svmtools.gui;

import java.awt.event.ItemEvent;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import svmtools.LoadedClass;
import org.apache.bcel.classfile.Trusted;

/**
 * This panel is for displaying and editing the calss access flags for a
 * Trusted class.
 */
public class AccessPanel extends TrustedPanel {

    private JCheckBox subclassFlag;
    private JCheckBox classResourceAccessFlag;
    private JCheckBox exceptionFlag;

    /**
     * Construct a new AccessPanel for a given loaded class.
     * @param clazz The data model.
     */
    public AccessPanel(LoadedClass clazz) {
        super(clazz, "Class access flags");

        int flags = 0;
        Trusted trusted = model.getOriginalTrusted();
        if (trusted != null)
            flags = trusted.getAccessFlags();

        setLayout(new GridLayout(0,1));

        subclassFlag = new JCheckBox("Allow unprivileged subclassing.",
            (flags & Trusted.TACC_SUBCLASS) != 0);
        classResourceAccessFlag = new JCheckBox("Allow unprivileged instantiation.",
            (flags & Trusted.TACC_CLASS_RESOURCE_ACCESS) != 0);
        exceptionFlag = new JCheckBox("Allow package-private exceptions to be caught outside of package via base-class handlers.",
            (flags & Trusted.TACC_EXCEPTION) != 0);

        subclassFlag.addItemListener(this);
        classResourceAccessFlag.addItemListener(this);
        exceptionFlag.addItemListener(this);

        add(subclassFlag);
        add(classResourceAccessFlag);
        add(exceptionFlag);
    }

    /**
     * Convert the state of the check boxes to a flags value.
     */
    private int getFlags() {
        int flags = 0;
        if (subclassFlag.isSelected())
            flags |= Trusted.TACC_SUBCLASS;
        if (classResourceAccessFlag.isSelected())
            flags |= Trusted.TACC_CLASS_RESOURCE_ACCESS;
        if (exceptionFlag.isSelected())
            flags |= Trusted.TACC_EXCEPTION;
        return flags;
    }

    /**
     * Synchronize state of GUI with underlying data model when a
     * checkbox's value changes.
     */
    public void itemStateChanged(ItemEvent e) {
        model.setClassAccessFlags(getFlags());
    }
}
