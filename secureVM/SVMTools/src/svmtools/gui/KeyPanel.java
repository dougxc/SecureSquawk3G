package svmtools.gui;

import java.awt.*;
import java.awt.event.ItemEvent;
import javax.swing.*;
import java.security.*;
import svmtools.LoadedClass;
import svmtools.KeyManager;
import svmtools.CSP;
import svmtools.CSPException;
import org.apache.bcel.generic.TrustedJavaClassGen;
import org.apache.bcel.classfile.Trusted;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPublicKey;

/**
 * This class implements a JPanel that is used for selecting the public keys for
 * verifying the subclass, instantiation and package membership priviliges. The
 * selection offered for the package key is limited to keys for which there is a
 * corresponding private key as this is required to generate the package permit.
 */

public class KeyPanel extends TrustedPanel {

    /**
     * Constants denoting each type of key.
     */
    private static final int SUBCLASS = 0;
    private static final int CLASS_RESOURCE_ACCESS = 1;
    private static final int PRIMARY_DOMAIN = 2;
    private static final int PRIMARY_DOMAIN_PRI = 3;

    /**
     * The labels displayed with the combo box for each type of key.
     */
    private static final String KEY_LABELS[] = {
        "Subclass privilege verification key",
        "Class resource access privilege verification key",
        "Primary domain membership signing and verification key pair"
    };

    /**
     * Constant option labels.
     */
    static private final String EXISTING_OPTION = "<existing>";
    static private final String NONE_OPTION = "<none>";


    /**
     * The key database that keys can be chosen from.
     */
    private KeyManager km;

    /**
     * The CSP used for recovering and encoding keys.
     */
    private CSP csp;

    /**
     * The keys (if any) that was already attached to the class.
     */
    private final Key oldKeys[] = new Key[4];

    /**
     * The combo boxes for selecting the keys.
     */
    private JComboBox keys[] = new JComboBox[3];

    /**
     * Construct a new panel with inputs for specifying a public key.
     * @param clazz The class associated with the keys.
     * @param km The key manager that provides the database of keys from
     * which the user can make a selection.
     * @param csp The CSP that will be used to decode any existing keys in the
     * class and encode them when we update. If the decoding of a key fails
     * for some reason, then that key is discarded.
     */
    public KeyPanel(LoadedClass clazz, KeyManager km, CSP csp) {
        super(clazz, "Keys");
        this.km = km;
        this.csp = csp;

        Trusted trusted = model.getOriginalTrusted();
        if (trusted != null) {
            ConstantPool cp = trusted.getTrustedConstantPool();
            oldKeys[SUBCLASS] = recoverKey(cp,trusted.getSubclassKey());
            oldKeys[CLASS_RESOURCE_ACCESS] =
                recoverKey(cp,trusted.getClassResourceAccessKey());

            // Can only use the existing primary domain key if we also have access to
            // the corresponding private key
            PublicKey key = null;
            try {
                key = csp.decodePublicKey(trusted.getDomains()[0].getKey());
            } catch (CSPException e) {
            }
            if (key != null) {
                KeyPair kp = km.getKeyPair(key);
                if (kp != null) {
                    oldKeys[PRIMARY_DOMAIN] = key;
                    oldKeys[PRIMARY_DOMAIN_PRI] = kp.getPrivate();
                }
            }
        }

        // Now initialize the UI of the panel.
        String[] pubAliases = km.getKeyAliases(true);
        String[] priAliases = km.getKeyAliases(false);

        setLayout(new GridBagLayout());

        GridBagConstraints lc = new GridBagConstraints();
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

        for (int i = 0; i != PRIMARY_DOMAIN_PRI; ++i) {
            // Build key choice boxes
            JLabel label = new JLabel(KEY_LABELS[i]);
            String aliases[] = (i != PRIMARY_DOMAIN ? pubAliases : priAliases);
            String options[] = buildOptions(oldKeys[i] != null,aliases);
            keys[i] = new JComboBox(options);
            keys[i].addItemListener(this);
            lc.gridy = cc.gridy = (i+1);
            add(label,lc);
            add(keys[i],cc);

            // Ensure the existing package
            if (options[0] == EXISTING_OPTION)
                itemStateChanged(new ItemEvent(keys[i],
                    ItemEvent.ITEM_STATE_CHANGED,EXISTING_OPTION,
                    ItemEvent.SELECTED));
        }

    }

    /**
     * Recover a public key from a Trusted attribute and a constant pool index.
     * @param cp The ConstantPool to search.
     * @param index The index of a PublicKeyConstant in the constant pool.
     * @param csp The CSP used to do the recovery.
     * @return the reconstructed PublicKey if:
     *    i) index is not zero and is an index to a ConstantPublicKey entry
     *   ii) the key is compatible with the current CSP
     * Otherwise, return null.
     */
    private PublicKey recoverKey(ConstantPool cp, int index) {
        if (index == 0)
            return null;
        Constant c = cp.getConstant(index);
        if (!(c instanceof ConstantPublicKey))
            return null;
        ConstantPublicKey pk = (ConstantPublicKey)c;
        try {
            return csp.decodePublicKey(pk.getKey());
        } catch (CSPException e) {
            return null;
        }
    }

    /**
     * Synchronize state of GUI with underlying data model when a
     * key's value changes.
     */
    public void itemStateChanged(ItemEvent e) {
        // Find the relevant key index
        JComboBox key = (JComboBox)e.getSource();
        int index = 0;
        while (keys[index] != key)
            index++;
        String choice = (String)key.getSelectedItem();

        if (choice == NONE_OPTION) {
            if (index == SUBCLASS)
                model.setSubclassKey(null);
            else if (index == CLASS_RESOURCE_ACCESS)
                model.setClassResourceAccessKey(null);
            else
                clazz.setPrimaryDomainKeyPair(null);
            return;
        }
        try {
            if (choice == EXISTING_OPTION) {
                if (index != PRIMARY_DOMAIN) {
                    byte[] keyData = csp.encodePublicKey(
                        (PublicKey)oldKeys[index]);
                    if (index == SUBCLASS)
                        model.setSubclassKey(keyData);
                    else
                        model.setClassResourceAccessKey(keyData);
                } else {
                    clazz.setPrimaryDomainKeyPair(
                        new KeyPair((PublicKey)oldKeys[PRIMARY_DOMAIN],
                                    (PrivateKey)oldKeys[PRIMARY_DOMAIN_PRI]));
                }
            } else {
                if (index != PRIMARY_DOMAIN) {
                    byte[] keyData = csp.encodePublicKey(
                        km.getKeyPair(choice).getPublic());
                    if (index == SUBCLASS)
                        model.setSubclassKey(keyData);
                    else
                        model.setClassResourceAccessKey(keyData);
                } else {
                    clazz.setPrimaryDomainKeyPair(km.getKeyPair(choice));
                }
            }
        } catch (CSPException cspe) {
            // Should never happen as the keys provided by the key
            // manager should only be compatible with the CSP.
            throw new RuntimeException("Encoding of key failed: "+
                        cspe.getMessage());
        }
    }

    /**
     * Helper method to build the set of options for a key choice that will
     * populate a combo box.
     */
    private String[] buildOptions(boolean existing, String aliases[]) {
        String options[];
        int pos = 0;
        if (!existing)
            options = new String[aliases.length + 1];
        else {
            options = new String[aliases.length + 2];
            options[pos++] = EXISTING_OPTION;
        }
        options[pos++] = NONE_OPTION;
        System.arraycopy(aliases,0,options,pos,aliases.length);
        return options;
    }
}
