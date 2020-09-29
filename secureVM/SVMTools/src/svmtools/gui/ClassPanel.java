package svmtools.gui;

import java.awt.*;
import javax.swing.JPanel;
import svmtools.*;

/**
 * This is the panel that contains all the panels for displaying and editing
 * the parts of a Trusted attribute for a class.
 */
public class ClassPanel extends JPanel {

    public ClassPanel(LoadedClass clazz, CSP csp, KeyManager km,
        PermitManager pm)
    {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.CENTER;
        gc.gridx = GridBagConstraints.REMAINDER;
        gc.insets = new Insets(10,5,10,5);
        gc.fill = GridBagConstraints.HORIZONTAL;
        setLayout(layout);

        AccessPanel access = new AccessPanel(clazz);
        add(access,gc);

        KeyPanel keys = new KeyPanel(clazz,km,csp);
        add(keys,gc);

        MembersPanel fields = new FieldsPanel(clazz);
        add(fields,gc);

        MembersPanel methods = new MethodsPanel(clazz);
        add(methods,gc);

        PermitsPanel subclassPermits = new SubclassPermitsPanel(clazz,pm);
        add(subclassPermits,gc);

        PermitsPanel classResourceAccessPermits = new ClassResourceAccessPermitsPanel(clazz,pm);
        add(classResourceAccessPermits,gc);
    }
}