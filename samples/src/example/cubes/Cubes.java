package example.cubes;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;

public class Cubes extends MIDlet {
	private CubeCanvas canvas;

    public Cubes() {
        canvas = new CubeCanvas();
	}

	public void startApp() {
        Display.getDisplay(this).setCurrent(canvas);
        Thread thread = new Thread(canvas);
        thread.start();
	}

	public void pauseApp() {
    }

	public void destroyApp(boolean unc) { }
}
