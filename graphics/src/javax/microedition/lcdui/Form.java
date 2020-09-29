package javax.microedition.lcdui;

public class Form extends Screen {
    private java.awt.Label title;

    public Form(String txt) {
        super(txt);

        proxy.setBackground(java.awt.Color.white);

        title = new java.awt.Label(txt);
        proxy.add("Center", title);
    }

    public Form(String title, Item[] items) {
        super(title);
    }

    private String asciifyString(String s) {
        StringBuffer buf = new StringBuffer();
        char c;

        for (int i=0; i<s.length(); i++) {
            switch (c = s.charAt(i)) {
                case '\n': buf.append(" "); break;
                default: buf.append(c); break;
            }
        }

        return buf.toString();
    }

    public void setTitle(String txt) {
        title.setText(txt);
        title.repaint();
    }

    public void append(String s) {
        proxy.add("Center", new java.awt.Label(asciifyString(s)));
    }

    public void append(StringItem s) {
        proxy.add("Center", new java.awt.Label(asciifyString(s.getLabel() + s.getText())));
    }

    public void append(Item s) {
    }

    public Item get(int index) {
        return null;
    }
}
