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

public class SubclassPermitsPanel extends PermitsPanel {

    public SubclassPermitsPanel(LoadedClass clazz, PermitManager pm) {
        super(clazz,pm,"Subclass Permits");
    }

    /**
     * Populate a given table of rows with the revelant permits (if any)
     * that can be extracted from a set of permits.
     * @param db A table of permits indexed by their key.
     * @param rows The table of rows to populate. For each entry, the key
     * must be a String (i.e. the name of the grantor) and the value is a
     * PermitRow object.
     */
    protected void extractPermitsFromDB(Hashtable db, Hashtable rows) {
        // Filter out permits that don't apply (e.g. a subclass permit
        // whose grantor this class does not subclass or implement)
        JavaClass jc = clazz.getModel().getOriginalClass();
        Set classNames = new HashSet(Arrays.asList(jc.getInterfaceNames()));
        classNames.add(jc.getSuperclassName());
        for (Enumeration e = db.elements(); e.hasMoreElements();) {
            PermitManager.Permit permit = (PermitManager.Permit)e.nextElement();
            if (permit.type.equals(permit.SUBCLASS)) {
                String grantor = permit.grantor;
                if (classNames.contains(grantor)) {
                    byte[] sig = permit.getSignature();
                    PermitChoice choice = (PermitChoice)rows.get(grantor);
                    if (choice == null) {
                        choice = new PermitChoice(grantor,sig,true);
                        rows.put(grantor,choice);
                    }
                    else {
                        choice.newPermit = sig;
                    }
                }
            }
        }
    }

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
    protected void extractExistingPermits(Trusted trusted, JavaClass jc,
        Hashtable rows)
    {
        String grantee = jc.getClassName();
        ConstantPool cp = jc.getConstantPool();
        ConstantPool sp = trusted.getTrustedConstantPool();

        Trusted.Permit permits[] = trusted.getSubclassPermits();
        String interfaceNames[] = jc.getInterfaceNames();
        for (int i = 0; i != permits.length; ++i) {
            Trusted.Permit permit = permits[i];
            String grantor;
            // The subclass permit (if any) will be first and is
            // distinguished by having no interface index
            if (i == 0 && permit.getClassIndex() == interfaceNames.length)
                grantor = jc.getSuperclassName();
            else {
                grantor = jc.getInterfaceNames()[permit.getClassIndex()];
            }
            ConstantDigitalSignature c = (ConstantDigitalSignature)
                sp.getConstant(permit.getSigIndex());
            byte[] sig = c.getSignature();
            PermitChoice choice = (PermitChoice)rows.get(grantor);
            if (choice == null) {
                choice = new PermitChoice(grantor,sig,false);
                rows.put(grantor,choice);
            }
            else {
                choice.oldPermit = sig;
            }
        }
    }

    /**
     * Update the underlying model based on a user's selection of a permit.
     */
    public void setPermit(String grantor, byte[] permit) {
        clazz.getModel().setSubclassPermit(grantor,permit);
    }
}
