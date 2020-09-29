package mojo;

import java.awt.*;
import java.awt.event.*;

public class MojoScreen extends Panel {

    private SoftButtonPanel softButtonPanel;

    public MojoScreen() {
        super(new BorderLayout());
        Panel dummy = new Panel();
        softButtonPanel = new SoftButtonPanel();
       // setBackground(Color.lightGray);
        setNoBackground();
        setCenter(dummy);
        add(softButtonPanel, "South");
        validate();
    }

    public void setCenter(Panel p) {
        add(p, "Center");
    }

    public SoftButtonPanel getSoftButtonPanel() {
        return softButtonPanel;
    }
}
