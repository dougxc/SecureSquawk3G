package javax.microedition.lcdui;

public class Screen extends Displayable {
    protected String title;

    public Screen(String title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
