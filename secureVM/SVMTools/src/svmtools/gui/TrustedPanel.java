package svmtools.gui;

import java.awt.event.ItemListener;
import javax.swing.JPanel;
import javax.swing.border.*;
import svmtools.LoadedClass;

import org.apache.bcel.generic.TrustedJavaClassGen;

/**
 * Super class for panels that are used for displaying and editing some part
 * of a Trusted attribute for a class.
 */
public abstract class TrustedPanel extends JPanel implements ItemListener {

    /**
     * The encapsulating class.
     */
    final protected LoadedClass clazz;

    /**
     * The underlying data model for the panel. All
     * panels initialize their display from this model as
     * well as update changes to it made by the user.
     */
    final protected TrustedJavaClassGen model;

    /**
     * This constructor sets the underlying model for the panel.
     * @param model The underlying model for the panel.
     */
    protected TrustedPanel(LoadedClass clazz, String title) {
        this.clazz = clazz;
        this.model = clazz.getModel();

        // Set the border of the panel to contain the title.
        Border       etched = new EtchedBorder();
    TitledBorder border = new TitledBorder(etched, " " + title + " ");
    border.setTitleJustification(TitledBorder.LEFT);
        setBorder(border);
    }

    /**
     * Update the underlying model from the current state of the user's inputs.
     */
//    public abstract void update();
}
