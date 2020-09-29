package svmtools;

import java.security.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import svmtools.gui.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;


/**
 * This class represents the entrance point to the Wobulator as well as
 * being the central point for most GUI components to communicate with each
 * other. It also implements the main window of the program.
 *
 * This is a singleton class which means that all components can get a
 * reference to the single instance via the instanceOf method. This also
 * saves having to pass around a reference to the app object.
 */
public class Wobulator extends JFrame implements ClassManager.ClassEventListener {

    // --------------------------- Constants ---------------------------------

    /**
     * These could be overridden by user-saved preferences at some later time...
     */
    private static final int WINDOW_WIDTH_DEFAULT   = 1000;
    private static final int WINDOW_HEIGHT_DEFAULT  = 800;
    private static final int VERT_SPLITTER  = 170;

    // ------------------------- Fields --------------------------------------

    /**
     * The list used to display the loaded classes.
     */
    private JList classList;

    /**
     * The split panel that has the class list on the left and the panel
     * for the currently selected class on the right.
     */
    private JSplitPane vertSplitPanel;

    /**
     * Displayed in the right side of vertSplitPanel when there is no
     * currently selected class.
     */
    private JPanel emptyPanel;

    /**
     * The table of display panels for each loaded class.
     */
    private Hashtable classPanels = new Hashtable();

    /**
     * The Cryptographic Services Provider specified on the command line.
     */
    private CSP csp;

    /**
     * This KeyManager component provides the set of keys that can be used
     * during wobulation.
     */
    private KeyManager keyManager = new KeyManager();

    /**
     * This Permitmanager component provides a database of permits.
     */
    private PermitManager permitManager = new PermitManager();

    /**
     * The component that manages the classes that are currently being
     * wobulated as well as the GUI JTree used to display these classes.
     */
    private ClassManager classManager;

    /**
     * Table of actions available.
     */
    OpenAction openAction;
    SaveAction saveAction;
    VerifyPermitsAction verifyPermitsAction;
    CloseAction closeAction;
    CreateBatchFileAction batchAction;
    Action exitAction;
    Set currentClassActions;

    // ------------------------- Public Methods --------------------------------

    /**
     * Return the ClassManagere object that manages the loaded classes.
     * @return the ClassManager.
     */
    public ClassManager getClassManager() {
        return classManager;
    }

    /**
     * Display an error message in a dialog rooted to a given parent.
     * @param msg The error message to display.
     */
    public void displayError(String msg) {
        JOptionPane.showMessageDialog
            (this, msg,
             "Wobulator Error Dialog",
             JOptionPane.ERROR_MESSAGE);
    }

    /**
     * This is the central point for exiting the application. This makes
     * it easier to provide extra actions (such as user confirmation) before
     * exiting.
     */
    public void exit() {
        System.exit(0);
    }

    /**
     * Return the panel for editing a given class. The panel will be created
     * if it didn't already exist.
     * @param name Name of class for which panel is to be returned.
     */
    private ClassPanel getClassPanel(String name) {
        ClassPanel cp = (ClassPanel)classPanels.get(name);
        if (cp == null) {
            LoadedClass lc = classManager.findClass(name);
            cp = new ClassPanel(lc,csp,keyManager,permitManager);
            classPanels.put(name,cp);
        }
        return cp;
    }

    /**
     * Application entry point.
     */
    public static void main(String args[]) throws Exception {
        int i;
        String cspArg = "svmtools.csp.MD5RSABasic",
               ksArg = null,
               passwdArg = null,
               batchArg = null,
               cpArg = null,
               destArg = null,
               dbArg = null;
        boolean autoWobulate = false;
        try {
            ksArg = System.getProperty("user.home") + File.separator + ".keystore";
        } catch (SecurityException se) {
        }
        for (i = 0; i != args.length; ++i) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.equals("-csp")) {
                    cspArg = getArg(args,++i);
                } else if (arg.equals("-keystore")) {
                    ksArg = getArg(args,++i);
                } else if (arg.equals("-storepass")) {
                    passwdArg = getArg(args,++i);
                } else if (arg.equals("-batch")) {
                    batchArg = getArg(args,++i);
                } else if (arg.equals("-cp")) {
                    cpArg = getArg(args,++i);
                } else if (arg.equals("-d")) {
                    destArg = getArg(args,++i);
                } else if (arg.equals("-db")) {
                    dbArg = getArg(args,++i);
                } else if (arg.equals("-auto")) {
                    autoWobulate = true;
                } else {
                    Utility.error("Unknown option: "+arg);
                    usage();
                    System.exit(1);
                }
            }
            else
                break;
        }

        // Ensure that all required options were provided
        if (cspArg == null || (batchArg == null && ksArg == null)) {
            error("A required command line argument was not provided.");
            usage();
            System.exit(1);
        }

        CSP csp = CSP.getInstance(cspArg);
        if (csp == null)
            error("Unknown CSP class: "+cspArg,true);

        PermitManager permitManager = new PermitManager();
        if (dbArg != null) {
            try {
                FileInputStream fis = new FileInputStream(dbArg);
                DataInputStream dis = new DataInputStream(fis);
                permitManager.load(dis,true);
            } catch (IOException ioe) {
                Utility.error("Error opening permits database: "+ioe.getMessage(),true);
            }
        }

        // Prompt for the keystore password if one wasn't provided
        KeyManager keyManager = null;
        if (ksArg != null) {
            char storepass[] = null;
            if (passwdArg == null) {
                System.err.print("Enter keystore password:  ");
                System.err.flush();
                storepass = null;
                try {
                    storepass = Utility.readPasswd(System.in);
                } catch (IOException ioe) {
                    System.err.println("Error while reading password.");
                    ioe.printStackTrace(System.err);
                }
                if (storepass == null )
                    error("A keystore password is required.",true);
            }
            else
                storepass = passwdArg.toCharArray();

            // Create and initialize key manager.
            try {
                keyManager = new KeyManager();
                keyManager.initialize(ksArg,storepass,csp);
            } catch (KeyStoreException kse) {
                    System.err.println(kse);
            }
        }

        // Create instance of this class.
        Wobulator instance = new Wobulator(csp,keyManager,permitManager);

        /*
         * Auto wobulate. Uses keystore entries: domain, cra, subclass and wobulates all files contained
         * within the supplied class path.
         */
        if (autoWobulate) {
          if(batchArg != null) {
            error("Batch wobulation and auto wobulation are not compatible", true);
          }
        }


        if (batchArg != null || autoWobulate) {
          if (cpArg == null || destArg == null)
            error("A classpath and destination directory must be given for batch processing.", true);
          File destDir = new File(destArg);
          if (!destDir.isDirectory() || !destDir.canWrite())
            error(destArg + " is not a directory or is not writeable.", true);
          JavaClass processed[] = null;
          Reader reader;
          try {
            if(autoWobulate) {
              reader = AutoWobulate.processFiles(cpArg);
            } else {
              reader = new FileReader(batchArg);
            }

            processed = BatchWobulate.processBatchFile(reader, cpArg, csp,
                permitManager, keyManager);
          }
          catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
          }
          for (int j = 0; j != processed.length; j++) {
            File path = null;
            try {
              JavaClass jc = processed[j];
              if (jc == null)
                continue;
              String name = jc.getClassName();
              path = new File(destArg + File.separator +
                              name.replace('.', File.separatorChar) + ".class");
              String parent = path.getParent();
              if (parent != null)
                new File(parent).mkdirs();
              jc.dump(path);
            }
            catch (IOException ioe) {
              error("Error while writing " + path.getPath() + ": " +
                    ioe.getMessage(), true);

            }
          }
            return;
        }

        // Initialize the UI
        instance.initializeUI();

        // Load any classes specified on the command line.
        if (args.length != i)
            for (; i != args.length; i++) {
                String file = args[i];
                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                    instance.getClassManager().loadClass(fis,file);
                } catch (IOException ioe) {
                    System.err.println(
                        "Exception occurred while loading class from "+
                            file);
                    ioe.printStackTrace(System.err);
                }
            }

        instance.setTitle("Wobulator - <no class selected>");

        // Calculate the screen size
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        // Pack frame on screen
        instance.pack();

        // Centre frame on screen
        Dimension frameSize = instance.getSize();
        instance.setLocation(
            (screen.width - Math.min(screen.width,frameSize.width)) / 2,
            (screen.height - Math.min(screen.height,frameSize.height)) / 2);

        instance.setVisible(true);
    }

    // ------------------------- Private Methods -------------------------------

    /**
     * The constructor initializes an instance from command line args.
     */
    public Wobulator(CSP csp, KeyManager keyManager, PermitManager permitManager) {
        this.csp = csp;
        this.keyManager = keyManager;
        this.permitManager = permitManager;
        classManager = new ClassManager(csp);
    }

    /**
     * Initialize the UI for the application object.
     */
    private void initializeUI() {
        // Exiting the Wobulator should all go through the one point.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Wobulator.this.exit();
            }
        });

        emptyPanel = new JPanel(new GridBagLayout());
        emptyPanel.add(new JLabel("No Class Selected"));

        // Build menu actions
        openAction = new OpenAction();
        saveAction = new SaveAction(this);
        verifyPermitsAction = new VerifyPermitsAction();
        closeAction = new CloseAction();
        batchAction = new CreateBatchFileAction(this);
        exitAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Wobulator.this.exit();
            }
        };
        exitAction.putValue(Action.NAME,"Exit");

        currentClassActions = new HashSet();
        currentClassActions.add(saveAction);
        currentClassActions.add(closeAction);
        currentClassActions.add(verifyPermitsAction);

        // Create and set the menu bar
        setJMenuBar(buildMenuBar());

        // Build the main content pane
        JPanel mainPanel = new JPanel(new BorderLayout());

        /* Make sure it fits on the screen */
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        mainPanel.setPreferredSize(new Dimension(Math.min(WINDOW_WIDTH_DEFAULT, (int)dim.getWidth()),
                                                 Math.min(WINDOW_HEIGHT_DEFAULT, (int)dim.getHeight())));


        // Build the class list and place it in a scroll pane.
        classList = new JList(classManager);
        classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(classList);

        // Build the split panel that has the class list on the left
        // and the panel corresponding to the currently selected node
        // one the right.
        vertSplitPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
        vertSplitPanel.setOneTouchExpandable(true);
        vertSplitPanel.setLeftComponent(listScrollPane);
        vertSplitPanel.setRightComponent(new JScrollPane(emptyPanel));
        vertSplitPanel.setDividerLocation(VERT_SPLITTER);
        mainPanel.add(vertSplitPanel,BorderLayout.CENTER);

        // Attach the class manager to the list so that it can notify
        // the appropriate ClassEventListeners when a class is selected in the
        // list.
        classList.getSelectionModel().addListSelectionListener(classManager);

        // Attach the class list listener to the list and the class manager
        classManager.addClassEventListener(this);



        setContentPane(mainPanel);
    }

    /**
     * Helper method to build the menu bar.
     * @return the constructed menu bar.
     */
    private JMenuBar buildMenuBar() {
        // Build the "Open" menu item
        JMenuItem openItem = new JMenuItem();
        openItem.setAction(openAction);

        // Build the "Save" menu item
        JMenuItem saveItem = new JMenuItem();
        saveItem.setAction(saveAction);

        // Build the "Verify Permits" menu item
        JMenuItem verifyPermitsItem = new JMenuItem();
        verifyPermitsItem.setAction(verifyPermitsAction);

        // Build the "Close" menu item
        JMenuItem closeItem = new JMenuItem();
        closeItem.setAction(closeAction);

        // Build the "Save template" menu item
        JMenuItem saveTemplateItem = new JMenuItem();
        saveTemplateItem.setAction(batchAction);

        // Build the "Exit" menu item
        JMenuItem exitItem = new JMenuItem();
        exitItem.setAction(exitAction);

        // Build the "Class" menu which contains the above items
        JMenu classMenu = new JMenu("Class");
        classMenu.add(openItem);
        classMenu.add(saveItem);
        classMenu.add(verifyPermitsItem);
        classMenu.add(closeItem);
        classMenu.add(new JSeparator());
        classMenu.add(saveTemplateItem);
        classMenu.add(new JSeparator());
        classMenu.add(exitItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(classMenu);
        return menuBar;
    }

    /**
     * Print out a message describing the command line options supported
     * and exit.
     */
    private static void usage() {
    System.err.println(
    "Usage:\n"+
    "Wobulator [ -csp <cspClass> ] [ -keystore <ksfile> ]\n"+
    "     [ -batch <batch> -cp <classpath> -d <dir>] [ -db <permitDB> ]\n"+
    "     [ -storepass <passwd> ] [ -auto ]\n"+
    "where:\n"+
    "   <cspClass> - the Cryptographics Service Provider to use. This is the\n"+
    "                name of a class implementing the svm.CSP interface.\n"+
    "                (default: svmtools.csp.MD5RSABasic).\n"+
    "   <ksfile>   - the JKS keystore providing the keys for signing and\n"+
    "                privilege verification (default: {user.home}/.keystore).\n"+
    "   <passwd>   - the password for the keystore. The user will be prompted\n"+
    "                for this password if it is not given here.\n"+
    "   <batch>    - file generated by the \"Save template\" action in the Wobulator.\n"+
    "   <classpath>- path for finding classes referenced in <batch>.\n"+
    "   <dir>      - where to place batched files.\n"+
    "   <permitDB> - a file containing a permit DB.");
    }

    /**
     * Handle displaying of an error message.
     * @param msg The error message to display.
     * @param exit Call System.exit if true after displaying the message.
     */
    static private void error(String msg, boolean exit) {
        System.err.println(msg);
        if (exit)
            System.exit(1);
    }
    static private void error(String msg) {
        error(msg,false);
    }

    /**
     * Helper method to extract the next command line arg. Includes a bounds
     * check which results to a call to System.exit if it fails.
     * @param args The command line args.
     * @param index The index of the next arg.
     * @return the next arg.
     */
    private static String getArg(String args[], int index) {
        if (index >= args.length) {
            usage();
            System.exit(1);
        }
        return args[index];
    }


    /**
     * Helper method to center a dialog on top of a given frame.
     */
    private static void centerDialog(JFrame frame, JDialog dialog) {
        Dimension dialogSize = dialog.getPreferredSize();
    Dimension frameSize  = frame.getSize();

    Point loc = frame.getLocation();
    dialog.setLocation((frameSize.width - dialogSize.width) / 2 +
            loc.x, (frameSize.height - dialogSize.height) / 2 + loc.y);
    }

    /**
     * Class implementing "Open" action.
     */
    class OpenAction extends AbstractAction {
        /**
         * Keep a permanent reference to the file chooser so that
         * it remembers the last working directory.
         */
        JFileChooser fc;

        /**
         * Constructor configures file chooser.
         */
        OpenAction() {
            fc = new JFileChooser();
            try {
                fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
            } catch (Exception e)
            {}
            fc.addChoosableFileFilter(fc.getAcceptAllFileFilter());
            fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File path) {
                    return path.isDirectory() ||
                        path.getName().toLowerCase().endsWith(".class");
                }
                public String getDescription() {
                    return "Java Class files";
                }
            });
            super.putValue(Action.NAME,"Open...");
        }

        /**
         * Show the file chooser and if a file is selected, attempt to open it.
         */
        public void actionPerformed(ActionEvent e) {

            if (fc.showOpenDialog(Wobulator.this) == JFileChooser.APPROVE_OPTION) {
                String path = fc.getSelectedFile().getAbsolutePath();
                try {
                    FileInputStream fis = new FileInputStream(path);
                    classManager.loadClass(fis,path);
                } catch (IOException ioe) {
                    displayError(ioe.getMessage());
                }
            }
        }
    }

    /**
     * Abstract class that is the base for all class based actions. It keeps a
     * reference to the currently selected class.
     */
    abstract class CurrentClassAction extends AbstractAction {
        protected String name;
        protected LoadedClass lc;
        /**
         * Establish/remove a link to the currently selected class
         */
        void setCurrentClass(String name, LoadedClass lc) {
            this.name = name;
            this.lc = lc;
            setEnabled(name != null && lc != null);
        }
    }
    /**
     * Class implementing "Save" action.
     */
    class SaveAction extends CurrentClassAction {

        /**
         * All saves require the use of a file chooser. This
         * helps to prevent accidental saves that overwrite a
         * file that should not be overwritten.
         */
        private JFileChooser fc;


        /**
         * Constructor sets back reference to a Wobulator instance as well as
         * initialising the file chooser.
         */
        SaveAction(Wobulator app) {
            fc = new JFileChooser();
            setEnabled(false);
            putValue(Action.NAME,"Save...");
        }

        /**
         * Implements Action interface.
         */
        public void actionPerformed(ActionEvent e) {
            // 'name' will never be null as the save action is disabled
            // if there is no selected class
            File file = lc.getSourceFile();

            fc.setSelectedFile(file);
            if (fc.showSaveDialog(Wobulator.this) == JFileChooser.APPROVE_OPTION)
                file = fc.getSelectedFile();
            else
                return;

            // Synchronize changes to underlying model and save
            // to the chosen file.
            try {

                lc.createJavaClass(csp).dump(file);
            }catch (IOException ioe) {
                displayError(ioe.getMessage());
            }catch (VerifyError ve) {
                displayError(ve.getMessage());
            }
        }
    }

    /**
     * This class implements the verification of a class's permits.
     */
    class VerifyPermitsAction extends CurrentClassAction {

        /**
         * This is the interface used by the PermitsVerifier class to
         * access info it needs from grantor classes while verifying
         * permits for another class.
         */
        LoadedGrantorDB db = new LoadedGrantorDB();

        /**
         * Constructor sets enabled state to false and gives this action a name.
         */
        VerifyPermitsAction() {
            setEnabled(false);
            putValue(Action.NAME,"Verify permits");
        }

        /**
         * Retrieve the grantor of a permit.
         */
        private class LoadedGrantorDB implements PermitsVerifier.GrantorDB {

            private Hashtable cache = new Hashtable();

            public byte[] getKey(String grantor, boolean isSubclass)
                throws NoSuchElementException
            {
                TrustedJavaClassGen gen = lookup(grantor);
                if (isSubclass)
                    return gen.getSubclassKey();
                else
                    return gen.getClassResourceAccessKey();
            }

            public int getClassAccessFlags(String grantor)
                throws NoSuchElementException
            {
                return lookup(grantor).getClassAccessFlags();
            }

            private TrustedJavaClassGen lookup(String grantor)
                throws NoSuchElementException
            {
                TrustedJavaClassGen gen =
                    (TrustedJavaClassGen)cache.get(grantor);
                if (gen == null) {
                    LoadedClass lc = classManager.findClass(grantor);
                    if (lc == null) {
                        throw new NoSuchElementException(
                            grantor + " is not loaded");
                    }
                    gen = lc.getModel();
                    cache.put(grantor,gen);
                }
                return gen;
            }
        }

        /**
         * Implements Action interface.
         */
        public void actionPerformed(ActionEvent e) {
            // Synchronize with GUI panels and create package permit
            JavaClass clazz = null;
            try {
                clazz = lc.createJavaClass(csp);
            } catch (IOException ioe) {
                displayError(ioe.getMessage());
                return;
            } catch (VerifyError ve) {
                displayError(ve.getMessage());
                return;
            }

            Trusted t = clazz.getTrustedAttribute();
            ConstantPool cp = clazz.getConstantPool();
            ConstantPool sp = t.getTrustedConstantPool();

            Vector msgs = PermitsVerifier.verifyPermits(clazz,csp,db);

            // Display result of verification
            if (msgs == null || msgs.size() == 0)
                JOptionPane.showMessageDialog(Wobulator.this,
                    "Permits for "+name+ " verified OK.",
                    "Wobulator Message Dialog",
                     JOptionPane.INFORMATION_MESSAGE);
            else {
                StringBuffer buf = new StringBuffer(msgs.size() * 40);
                buf.append("The following errors occured during permit verification for "+name+":\n");
                for (Iterator i = msgs.iterator(); i.hasNext();) {
                    String msg = (String)i.next();
                    buf.append(msg+"\n");
                }
                displayError(buf.toString());
            }
        }
    }

    /**
     * This class implements the semantics of closing (without saving)
     * the currently selected class.
     */
    class CloseAction extends CurrentClassAction {

        /**
         * Constructor sets enabled state to false and gives this action a name.
         */
        CloseAction() {
            setEnabled(false);
            putValue(Action.NAME,"Close");
        }

        /**
         * Implements Action interface.
         */
        public void actionPerformed(ActionEvent e) {
            // 'name' will never be null as the save action is disabled
            // if there is no selected class
            classManager.removeClass(name);
            // Set the class panel to be the empty panel
            JScrollPane js = (JScrollPane)vertSplitPanel.getRightComponent();
            js.setViewportView(emptyPanel);
        }
    }

    /**
     * Class implementing "Save template..." action.
     */
    class CreateBatchFileAction extends AbstractAction {

        /**
         * All saves require the use of a file chooser. This
         * helps to prevent accidental saves that overwrite a
         * file that should not be overwritten.
         */
        private JFileChooser fc;

        private CheckBoxList list;
        private JDialog listDialog;
        private OKCancelListener listAction;

        /**
         * Constructor sets back reference to a Wobulator instance as well as
         * initialising the file chooser.
         */
        CreateBatchFileAction(Wobulator app) {
            fc = new JFileChooser();
            try {
                fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
            } catch (Exception e)
            {}
            fc.setDialogTitle("Create Batch File");
            putValue(Action.NAME,"Create batch file...");
            setEnabled(false);
            initList();
        }

        class CheckAllNoneListener implements ActionListener {
            JButton all;
            CheckBoxList list;
            public CheckAllNoneListener(JButton all, CheckBoxList list) {
                this.all = all;
                this.list = list;
            }
            public void actionPerformed(ActionEvent e) {
                list.checkAll(e.getSource() == all);
            }
        }

        class OKCancelListener implements ActionListener {
            JDialog owner;
            JButton OK;
            boolean pressedOK;
            public OKCancelListener(JDialog owner, JButton OK) {
                this.owner = owner;
                this.OK = OK;
            }
            public void actionPerformed(ActionEvent e) {
                pressedOK = (e.getSource() == OK);
                owner.setVisible(false);
            }
        }
        private void initList() {
            list = new CheckBoxList();
            JScrollPane ps = new JScrollPane();
            ps.getViewport().add(list);

            listDialog = new JDialog(Wobulator.this,"Select Classes for Batch File",true);
            JPanel buttonPanel = new JPanel();
            JButton button = new JButton("All");
            CheckAllNoneListener checkListener =
                new CheckAllNoneListener(button,list);
            button.addActionListener(checkListener);
            buttonPanel.add(button);
            button = new JButton("None");
            button.addActionListener(checkListener);
            buttonPanel.add(button);

            final JButton okButton = new JButton("OK");
            final JButton cancelButton = new JButton("Cancel");
            listAction = new OKCancelListener(listDialog,okButton);
            okButton.addActionListener(listAction);
            cancelButton.addActionListener(listAction);
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(ps,BorderLayout.CENTER);
            panel.add(buttonPanel,BorderLayout.SOUTH);
            listDialog.getContentPane().add(panel);
        }

        void refreshList() {
            String[] classes = new String[classManager.getSize()];
            for (int i = 0; i != classes.length; ++i) {
                classes[i] = (String)classManager.getElementAt(i);
            }
            list.setModel((Object[])classes);
        }
        private String[] getSelectedClasses() {
            listDialog.pack();
            centerDialog(Wobulator.this,listDialog);
            listDialog.setVisible(true);

            if (!listAction.pressedOK)
                return null;

            Object[] items = list.getCheckedItems();
            String[] result = new String[items.length];
            System.arraycopy(items,0,result,0,items.length);
            return result;
        }

        /**
         * Implements Action interface.
         */
        public void actionPerformed(ActionEvent e) {
            String classNames[] = getSelectedClasses();
            if (classNames == null)
                return;
            File file;
            if (fc.showSaveDialog(Wobulator.this) == JFileChooser.APPROVE_OPTION)
                file = fc.getSelectedFile();
            else
                return;

            JavaClass classes[] = new JavaClass[classNames.length];
            for (int i = 0; i != classNames.length; i++) {
                String name = classNames[i];
                LoadedClass lc = classManager.findClass(name);
                try {
                    classes[i] = lc.createJavaClass(csp);
                } catch (IOException ioe) {
                    displayError("Error while generating template for "+name+": "+
                        ioe.getMessage());
                    return;
                }
            }
            PrintStream out = null;
            try {
                out = new PrintStream(new FileOutputStream(file));
            } catch (IOException ioe) {
                displayError("Couldn't open "+file.getName()+" for writing: "+
                    ioe.getMessage());
            }
            BatchWobulate.createBatchFile(out,classes,keyManager);
            out.close();
        }
    }

    // -------------- Implementation of ClassEventListener -------------------

    /**
     * Do need to do anything here.
     */
    public void classLoaded(String name, LoadedClass lc) {
        setTitle("Wobulator - "+name);
        batchAction.setEnabled(true);
        batchAction.refreshList();
    }

    /**
     * Remove the panel for the removed class.
     */
    public void classRemoved(String name, LoadedClass lc) {
        setTitle("Wobulator - <no class selected>");
        classPanels.remove(name);
         // Disable class specific actions
        for (Iterator i = currentClassActions.iterator(); i.hasNext();) {
            CurrentClassAction a = (CurrentClassAction)i.next();
            a.setCurrentClass(null,null);
        }
        batchAction.setEnabled(classManager.getSize() != 0);
        batchAction.refreshList();
    }

    /**
     * Called whenever the value of the selection changes.
     * @param e the event that characterizes the change.
     */
    public void classSelected(String name, LoadedClass lc) {
        setTitle("Wobulator - "+name);
        JScrollPane js = (JScrollPane)vertSplitPanel.getRightComponent();
        js.setViewportView(getClassPanel(name));
         // Enable/Disable class specific actions
        for (Iterator i = currentClassActions.iterator(); i.hasNext();) {
            CurrentClassAction a = (CurrentClassAction)i.next();
            a.setCurrentClass(name,lc);
        }
    }
}

