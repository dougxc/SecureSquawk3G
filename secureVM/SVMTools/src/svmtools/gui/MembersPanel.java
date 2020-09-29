package svmtools.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.apache.bcel.generic.TrustedJavaClassGen;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Trusted;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.Utility;
import svmtools.*;

/**
 * This class implements a JPanel that displays options for setting the
 * accessibilility foira table of all the
 * public or protected methods or fields in a class. If there are no
 * such members in a class, then a centered label explaining this is
 * shown instead.
 */
abstract public class MembersPanel extends TrustedPanel
                implements ActionListener, ItemListener {

    private static final int LIST_VIEW_WIDTH = 300;
    private static final int MAX_VISIBLE_ROWS = 8;

    String         type;

    DefaultListModel  defaultModel;
    DefaultListModel  nonDefaultModel;
    JList             defaultList;
    JList             nonDefaultList;

    JCheckBox         defaultSetting;
    JCheckBox         lockAllToDefault;

    JPanel            memberChooser;

    JButton           moveToDefault;
    JButton           moveFromDefault;

    /**
     * These need to be implemented by the FieldsPanel and MethodsPanel
     * subclasses. They mostly just delegate to an appropriate call to
     * the underlying TrustedJavaClassGen model.
     */
    abstract protected FieldOrMethod[] getMembers();
    abstract protected boolean getDefaultSetting(Trusted t);
    abstract protected int[] getNonDefaultMembers(Trusted t);
    abstract protected String memberToName(FieldOrMethod m);
    abstract protected void setDefaultMemberAccessibility(boolean b);
    abstract protected void clearNonDefaultMembers();
    abstract protected void addNonDefaultMember(int m);


    /**
     * Construct a new MembersPanel.
     * @param clazz The data model.
     * @param type "field" or "method"
     * false otherwise.
     */
    protected MembersPanel(LoadedClass clazz, String type) {
        super(clazz,"Public/protected "+type+ "s");
        this.type = type;

        // Extract current settings from an existing Trusted
        // attribute (if any).
        JavaClass jc = model.getOriginalClass();
        Trusted trusted = model.getOriginalTrusted();

        // Choose a fail-safe/conservative default.
        boolean defaultSetting = false;

        FieldOrMethod members[] = (FieldOrMethod[])getMembers().clone();
        filterMembers(members);
        Set defaultMembers = new TreeSet();
        for (int i = 0; i != members.length; ++i)
            if (members[i] != null) {
                FieldOrMethod member = members[i];
                defaultMembers.add(memberToName(member));
            }
        Set nonDefaultMembers;

        if (trusted != null) {
            int non_default_members[] = getNonDefaultMembers(trusted);
            nonDefaultMembers = new TreeSet();
            for (int i = 0; i != non_default_members.length; ++i) {
                FieldOrMethod member = members[non_default_members[i]];
                if (member != null)
                    nonDefaultMembers.add(memberToName(member));
            }
            defaultSetting = getDefaultSetting(trusted);
            defaultMembers.removeAll(nonDefaultMembers);
        }
        else
            nonDefaultMembers = new TreeSet();
        initialize(defaultMembers,nonDefaultMembers,defaultSetting);
    }

    /**
     * Helper method that filters out only the public and protected members
     * from an array of members and returns this in a new array.
     */
    static private void filterMembers(FieldOrMethod members[]) {
        for (int i = 0; i != members.length; ++i) {
            FieldOrMethod member = members[i];
            if (!member.isPublic() && !member.isProtected())
                members[i] = null;
        }
    }

    static private Component buildListHeader(String title) {
        JLabel header = new JLabel(title,SwingConstants.CENTER);
        header.setBorder(new javax.swing.border.EtchedBorder());
        return header;
    }

    /**
     * Initialize the UI of this panel.
     */
    protected void initialize(Set defaultMembers, Set nonDefaultMembers,
        boolean defaultSetting)
    {
        if (defaultMembers.size() == 0 && nonDefaultMembers.size() == 0) {
            add(new JLabel("No public or protected "+type+"s."));
            return;
        }

        int visibleRows = Math.min(Math.max(defaultMembers.size(),
            nonDefaultMembers.size()),MAX_VISIBLE_ROWS);

        lockAllToDefault = new JCheckBox("Use default setting for all "+type+"s.",
            nonDefaultMembers.isEmpty());
        lockAllToDefault.addItemListener(this);
        this.defaultSetting = new JCheckBox("All "+type+"s are accessible to U-classes by default.",
            defaultSetting);
        this.defaultSetting.addItemListener(this);

        nonDefaultModel = new DefaultListModel();
        for (Iterator i = nonDefaultMembers.iterator(); i.hasNext(); )
            nonDefaultModel.addElement(i.next());
        nonDefaultList = new JList(nonDefaultModel);
        nonDefaultList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane nonDefaultScrollPane = new JScrollPane(nonDefaultList);
        nonDefaultScrollPane.setColumnHeaderView(buildListHeader("Non-default "+type+"s"));
        nonDefaultList.setVisibleRowCount(visibleRows);


        defaultModel = new DefaultListModel();
        for (Iterator i = defaultMembers.iterator(); i.hasNext(); )
            defaultModel.addElement(i.next());
        defaultList = new JList(defaultModel);
        defaultList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane defaultScrollPane = new JScrollPane(defaultList);
        defaultScrollPane.setColumnHeaderView(buildListHeader("Default "+type+"s"));
        defaultList.setVisibleRowCount(visibleRows);

//        defaultScrollPane.getViewport().setSize(LIST_VIEW_WIDTH,100);
//        nonDefaultScrollPane.getViewport().setSize(LIST_VIEW_WIDTH,100);
        nonDefaultList.setFixedCellWidth(LIST_VIEW_WIDTH);
        defaultList.setFixedCellWidth(LIST_VIEW_WIDTH);

        moveToDefault = new JButton(">>");
        moveToDefault.addActionListener(this);
        moveFromDefault = new JButton("<<");
        moveFromDefault.addActionListener(this);

        memberChooser = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // Now place the lists themself
        gc.fill = gc.BOTH;
        gc.gridheight = 2;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0.5;
        memberChooser.add(nonDefaultScrollPane,gc);
        gc.gridx = 2;
        memberChooser.add(defaultScrollPane,gc);

        // Place "<<" and ">>" buttons
        gc.weightx = 0.0;
        gc.weighty = 0.5;
        gc.insets = new Insets(0,3,0,3);
        gc.gridheight = 1;
        gc.fill = gc.NONE;
        gc.gridx = 1;
        gc.gridy = 0;
        memberChooser.add(moveFromDefault,gc);
        gc.gridy = 1;
        memberChooser.add(moveToDefault,gc);

        // Create the layered panel.
        setLayout(new GridBagLayout());
        gc.gridx = gc.REMAINDER;
        gc.gridy = gc.RELATIVE;
        gc.fill = gc.BOTH;
        gc.anchor = gc.WEST;
        add(this.defaultSetting,gc);
        add(lockAllToDefault,gc);
        gc.insets = new Insets(10,3,0,3);
        add(memberChooser,gc);
        setEnabled(memberChooser,!lockAllToDefault.isSelected());
    }

    /**
     * Handle pressing of the ">>" or "<<" button.
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        boolean changed = false;
        if (source == moveToDefault) {
            Object selected[] = nonDefaultList.getSelectedValues();
            for(int i = 0; i!= selected.length; i++) {
                defaultModel.addElement(selected[i]);
                nonDefaultModel.removeElement(selected[i]);
            }
            changed = selected.length != 0;
        }
        else if (source == moveFromDefault) {
            Object selected[] = defaultList.getSelectedValues();
            for(int i = 0; i!= selected.length; i++) {
                nonDefaultModel.addElement(selected[i]);
                defaultModel.removeElement(selected[i]);
            }
            changed = selected.length != 0;
        }
        if (changed)
            itemStateChanged(null);
    }

    /**
     * Recursively disabled a container and all its subcomponents.
     */
    private void setEnabled(Container c, boolean enabled) {
        c.setEnabled(enabled);
        Component[] all = c.getComponents();
        for (int i = 0; i != all.length; i++) {
            Component cmp = all[i];
            if (cmp instanceof Container)
                setEnabled((Container)cmp,enabled);
            else {
                cmp.setEnabled(enabled);
            }
        }
    }

    /**
     * Synchronize state of GUI with underlying data model when a
     * any of the state changes in this panel.
     */
    public void itemStateChanged(ItemEvent e) {
        if (e != null && e.getItemSelectable() == lockAllToDefault) {
            setEnabled(memberChooser,!lockAllToDefault.isSelected());
        }
        Set nonDefaultMembers =
            new TreeSet(Arrays.asList(nonDefaultModel.toArray()));
        setDefaultMemberAccessibility(defaultSetting.isSelected());
        clearNonDefaultMembers();
        if (lockAllToDefault.isSelected())
            return;
        FieldOrMethod members[] = getMembers();
        for (int i = 0; i != members.length; i++) {
            FieldOrMethod m = members[i];
            if (nonDefaultMembers.contains(memberToName(m)))
                addNonDefaultMember(i);
        }
    }

}
