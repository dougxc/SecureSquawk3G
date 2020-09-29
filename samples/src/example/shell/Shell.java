package example.shell;

import java.awt.*;
import java.awt.event.*;
import awtcore.*;
import java.io.*;


public class Shell extends Applet implements ActionListener {

    boolean openSuiteHack;
    final static char pathSeparatorChar;
    static {
        String sep = System.getProperty("path.separator");
        if (sep != null) {
            pathSeparatorChar = sep.charAt(0);
        } else {
            pathSeparatorChar = ':';
        }
    }


    class MainPanel extends Panel implements ActionListener {

        MainPanel() {
            super(new GridLayout (0, 2));
            addButton("cubes");
            addButton("chess");
            addButton("mpeg");
            addButton("spaceinv");
            addButton("manyballs");
            addButton("awtdem");
            addButton("shell");
            addButton("restart");
        }

        void addButton(String app) {
            Button b = new Button(app);
            add(b);
            b.addActionListener(this);
            b.setActionCommand("example." + app + ".Main");
        }

        public void actionPerformed(ActionEvent ev) {
            String cmd = ev.getActionCommand();
            if (cmd.equals("example.restart.Main")) {
                restart();
            } else if (cmd.equals("exit")) {
                destroyApp(true);
                notifyDestroyed();
            } else {
                start(cmd, false);
            }
        }


        private void start(final String cmd, final boolean autoStarted) {
            new Thread() {
                public void run() {
                    Isolate currentIsolate = Thread.currentThread().getIsolate();
                    String cp = currentIsolate.getClassPath();

                    String parentURL;
                    Suite suite = currentIsolate.getOpenSuite();
                    if (suite == null) {
                        parentURL = currentIsolate.getBootstrapSuite().getURL();
                    } else {
                        parentURL = suite.getURL();
                        while (parentURL == null) {
                            suite = suite.getParent();
                            parentURL = suite.getURL();
                        }
                    }

                    String[] args = new String[0];
                    if (cmd.equals("example.shell.Main") && openSuiteHack) {
                        args = new String[]{ "share" };
                    }

                    Isolate childIsolate = new Isolate(cmd, args, cp, parentURL);
                    if (openSuiteHack) {
                        childIsolate.forceOpenSuite(currentIsolate.getOpenSuite());
                    }
                    childIsolate.start();
                    if (autoStarted) {
                        for (int i = 0; i != 100; ++i) {
                            Thread.yield();
                        }
                        try {
                            childIsolate.hibernate();
                        }
                        catch (IOException ex1) {
                            ex1.printStackTrace();
                        }
                    }

                    childIsolate.join();

//System.out.println(cmd + " finished");

                    if (childIsolate.isHibernated()) {
                        try {
                            String url = childIsolate.save();
                            System.out.println("Saved isolate to " + url);
//VM.println("Saved isolate to " + url);
                        } catch (java.io.IOException ioe) {
                            System.err.println("I/O error while trying to save isolate: ");
                            ioe.printStackTrace();
                        }
                        catch (LinkageError le) {
                            System.err.println("Linkage error error while trying to save isolate: ");
                            le.printStackTrace();
                        }
                    }
                }
            }.start();
        }

        private void restart() {
            final String name = OptionDialog.showInputDialog(frame, "File name?");
            if (name != null) {
                new Thread() {
                    public void run() {
//VM.println("restart " + name);
                        Isolate isolate = Isolate.load("file://" + name + ".isolate");
                        isolate.unhibernate();
                        isolate.join();
                        if (isolate.isHibernated()) {
                            try {
                                String url = isolate.save();
                                System.out.println("Saved isolate to " + url);
                            }
                            catch (java.io.IOException ioe) {
                                System.err.println("I/O error while trying to load isolate: ");
//VM.println("I/O error while trying to load isolate: ");
                                ioe.printStackTrace();
                            }
                            catch (LinkageError le) {
                                System.err.println("Linkage error error while trying to load isolate: ");
                                le.printStackTrace();
                            }
                        }
                    }
                }.start();
            }
        }
    }


    Frame frame;
    MainPanel mainPanel;

    public Shell() {
        mainPanel = new MainPanel();
        frame = new Frame("Shell");
        frame.add("Center", mainPanel);
        frame.pack();
    }

    public void startApp() {
        frame.show();
//        mainPanel.start("example.chess.Main", false);
    }

    public void destroyApp(boolean uncond) {
        frame.dispose();
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
    }

    public static void main (String [] argv) {
        Shell shell = new Shell();
        if (argv.length > 0) {
            shell.openSuiteHack = true;  // Any parameter turns on this hack
        }
        shell.startApp();
    }


}






