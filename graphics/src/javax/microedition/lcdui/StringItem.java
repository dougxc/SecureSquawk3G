package javax.microedition.lcdui;

public class StringItem extends Item {
    protected String text;

    public StringItem(String label, String text) {
        super(label);
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
