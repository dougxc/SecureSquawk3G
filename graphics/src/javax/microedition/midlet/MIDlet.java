package javax.microedition.midlet;

public abstract class MIDlet {
    protected abstract void startApp() throws MIDletStateChangeException;

    protected abstract void pauseApp();

    protected abstract void destroyApp(boolean unconditional)
	throws MIDletStateChangeException;

    // ### FIXME
    public final void publicStartApp() throws MIDletStateChangeException {
        startApp();
    }

	public final void notifyDestroyed() {
		System.out.println("bye...");
		System.exit(0);
	}
}
