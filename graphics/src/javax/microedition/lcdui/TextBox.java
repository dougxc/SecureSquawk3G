package javax.microedition.lcdui;

public class TextBox extends Screen {
    protected String string;

    public TextBox(String title, String string, int a, int b) {
        super(title);
        this.string = string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
