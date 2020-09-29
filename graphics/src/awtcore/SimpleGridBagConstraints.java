package awtcore;

public class SimpleGridBagConstraints {

    int gridx;
    int gridy;
    int gridwidth;
    int gridheight;
    int weightx;
    int weighty;


    public Object clone () {
    SimpleGridBagConstraints c = new SimpleGridBagConstraints ();
    c.gridx = gridx;
    c.gridy = gridy;
    c.gridwidth = gridwidth;
    c.gridheight = gridheight;
    c.weightx = weightx;
    c.weighty = weighty;
    return c;
    }

}
