package javax.microedition.lcdui;

public abstract class Item {
    protected String label;

    public Item(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
