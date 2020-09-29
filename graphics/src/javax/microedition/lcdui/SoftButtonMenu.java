package javax.microedition.lcdui;

public class SoftButtonMenu extends Displayable implements CommandListener {
    private static SoftButtonMenu instance = new SoftButtonMenu();
    private Displayable previous;
    private Command menu;
    private Command back;

    private SoftButtonMenu() {
        menu = new Command("Menu", Command.SCREEN, 99);
        back = new Command("Back", Command.BACK, 99);
        addCommand(back);
        setCommandListener(this);
    }

    public static SoftButtonMenu getInstance() {
        return instance;
    }

    public static Command getCommand() {
        return instance.menu;
    }

    public void paint(Graphics g) {
        java.util.Vector cmds;
        String str;
        Command c;

        g.setColor(0xffffff);
        g.fillRect(0, 0, Display.WIDTH, Display.HEIGHT);
        g.setColor(0);
        g.drawString("Menu", Display.WIDTH/2, 10, Graphics.TOP | Graphics.HCENTER);
        g.drawLine(0, 30, Display.WIDTH, 30);

        if (previous == null) return;

        cmds = previous.getCommands();
        for (int i=0; i<cmds.size(); i++) {
            c = (Command)cmds.elementAt(i);
            str = "" + (i + 1) + " " + c.getLabel();
            g.drawString(str, 20, 40 + 15 * i, Graphics.TOP | Graphics.LEFT);
        }
    }

    public void keyPressed(int key) {
        if (previous == null) return;

        int n = previous.getCommands().size();
        if (key < '1' || key > '0' + n) return;

     //   System.out.println("SoftButtonMenu.keyPressed(): menu choice = " + (char)key);
        Command c = (Command)previous.getCommands().elementAt(key - '1');
        CommandListener cl = previous.getCommandListener();
        if (cl != null) cl.commandAction(c, this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == menu) {
            previous = d;
            Display.getDisplay().setCurrent(this);
        }

        if (c == back) {
            Display.getDisplay().setCurrent(previous);
            previous = null;
        }
    }
}
