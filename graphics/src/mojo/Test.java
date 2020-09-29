package mojo;

import java.awt.*;

public class Test  {
    public static int WIDTH = 200;
    public static int HEIGHT = 200;


    public static void main(String[] arg) {
        Frame f = new Frame(/*"Test"*/);
        f.setSize(WIDTH, HEIGHT);
        Panel p = new LayoutPanel();
        p.setSize(WIDTH, HEIGHT);
        f.add(p);
        f.setVisible(true);
    }
}


class DoublePanel extends Panel {

    DoublePanel () {
        super (new BorderLayout());
        Panel p = new LayoutPanel();
        p.setSize(Test.WIDTH, Test.HEIGHT/2);
        add("North", p);
        p = new LayoutPanel();
        p.setSize(Test.WIDTH, Test.HEIGHT/2);
        add("South", p);
    }
}



class LayoutPanel extends Panel {

    LayoutManager saveBorder;

    LayoutPanel () {
        super (new BorderLayout());
        addButton ("North", "grid");
        addButton ("West", "exit");
        addButton ("Center", "dialog");
        addButton ("East", "flow");
        addButton ("South", "border");
        saveBorder = getLayout ();
        //validate();
    }

    void addButton (String where, String label) {
        Button b = new Button (label);
        add (where, b);
        //b.addActionListener (this);
        b.setActionCommand (label);
    }
}