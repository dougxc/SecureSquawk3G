package javax.microedition.lcdui;

public class Alert extends Screen implements CommandListener {
    public final static int FOREVER = -2;

    private int timeout;
    private String string;
    private Image image;
    private final static Command DONE = new Command("Done", Command.OK, 99);

	public Alert(String title) {
        super(title);
        super.addCommand(DONE);
        super.setCommandListener(this);
	}

	public void setTimeout(int t) {
        timeout = t;
	}

	public void setImage(Image img) {
        image = img;
	}

	public void setString(String s) {
        string = s;
	}

    public void addCommand(Command cmd) {
        throw new IllegalStateException();
    }

    public void setCommandListener(CommandListener l) {
        throw new IllegalStateException();
    }

    public void paint(Graphics g) {
        g.setColor(0xffffff);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(0);
        g.drawString(title, Display.WIDTH/2, 20, Graphics.TOP | Graphics.HCENTER);

        g.drawImage(image, Display.WIDTH/2, 30, Graphics.TOP | Graphics.HCENTER);

        g.drawString(string, 20, 40, Graphics.TOP | Graphics.LEFT);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == DONE) Display.getDisplay().clearAlert();
    }
}
