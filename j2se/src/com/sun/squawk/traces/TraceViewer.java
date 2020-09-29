/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.traces;

import java.io.*;
import java.util.*;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.*;
import javax.microedition.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;

import com.sun.squawk.io.connections.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;

/**
 * Show a tree based, foldable representation of a VM trace. A trace may include
 * trace lines and non-trace lines.
 *
 * @author  Doug Simon
 */
public class TraceViewer extends JFrame implements WindowListener, ComponentListener {

    /**
     * The map from addresses to symbolic information.
     */
    private Symbols symbols = new Symbols();

    /**
     * The JTree component used to render the trace as a tree.
     */
    private TraceTree tree;

    /**
     * The root node in the trace's data model.
     */
    private TraceNode root;

    /**
     * The search path for finding and opening source files.
     */
    private ClasspathConnection sourcePath;

    /**
     * A cache of JTextAreas for previously viewed source files.
     */
    private HashMap sourceFileCache;

    /**
     * The panel with the checkboxes controlling the level of detail displayed for tree nodes.
     */
    private final NodeDetailOptionsPanel nodeOptionsPanel = new NodeDetailOptionsPanel();

    /**
     * Identifier for the service thread.
     */
    private static final String SERVICE_THREAD_ID = "0";

    /**
     * Pads a given string buffer with spaces until its length is equal to the length
     * of a given array of space characters.
     *
     * @param buf     the buffer to pad
     * @param spaces  the spaces to pad with
     */
    public static void pad(StringBuffer buf, char[] spaces) {
        int diff = spaces.length - buf.length();
        if (diff > 0) {
            buf.append(spaces, 0, diff);
        }
    }

    /**
     * Constructs a TraceViewer.
     */
    private TraceViewer() {
        root = new TraceNode() {
            public int getCallDepth() {
                return -2;
            }
        };
        sourceFileCache = new HashMap();
    }

    /**
     * Creates and returns an AddressRelocator based on the first line of the trace.
     *
     * @param line   the first line of the trace
     * @return the created AddressRelocator
     */
    private AddressRelocator createAddressRelocator(String line) {
        Matcher m = Pattern.compile("\\*TRACE\\*:\\*ROM\\*:(\\d+):(\\d+):\\*NVM\\*:(\\d+):(\\d+):\\*(\\d+)\\*").matcher(line);
        if (!m.matches()) {
            throw new TraceParseException(line, m.pattern().pattern());
        }

        long romStart = Long.parseLong(m.group(1));
        long romEnd   = Long.parseLong(m.group(2));
        long nvmStart = Long.parseLong(m.group(3));
        long nvmEnd   = Long.parseLong(m.group(4));
        boolean is64bit = m.group(5).equals("64");
        nodeOptionsPanel.getValueFormatter().change64BitFlag(is64bit);

        return new AddressRelocator(romStart, (int)(romEnd - romStart), nvmStart, (int)(nvmEnd - nvmStart));
    }

    /**
     * Gets the ordinal position of the last slice of a thread identified by a given ID.
     *
     * @param threadID  a numerical thread ID
     * @return the ordinal position of the last slice of the thread identified by <code>threadID</code>
     */
    private int getLastSliceForThread(String threadID) {

        int count = root.getChildCount();
        while (--count >= 0) {
            TraceNode child = (TraceNode)root.getChildAt(count);
            if (child instanceof ThreadSliceNode) {
                ThreadSliceNode thread = (ThreadSliceNode)child;
                if (thread.getThreadID().equals(threadID)) {
                    return thread.slice;
                }
            }
        }
        return -1;
    }

    /**
     * Pattern for matching a line that starts a non-profile stack trace.
     * Capturing group 1 is the backward branch count at which the sample was taken and group 2
     * is the message describing the point of execution.
     */
    public static Pattern STACK_TRACE_START = Pattern.compile("\\*STACKTRACESTART\\*:(-?\\d+):(.*)");

    /**
     * Attempts to match a given line against the {@link #STACK_TRACE_START} pattern.
     *
     * @param  line  the line to match
     * @return the matcher used for a successful match or null
     */
    private Matcher matchesStackTraceStart(String line) {
        Matcher m = STACK_TRACE_START.matcher(line);
        if (m.matches()) {
            return m;
        } else {
            return null;
        }
    }

    /**
     * Attempts to match a given line against the {@link ProfileViewer#STACK_TRACE_START} pattern.
     *
     * @param  line  the line to match
     * @return the matcher used for a successful match or null
     */
    private Matcher matchesProfileStackTraceStart(String line) {
        Matcher m = ProfileViewer.STACK_TRACE_START.matcher(line);
        if (m.matches()) {
            return m;
        } else {
            return null;
        }
    }

    /**
     * Attempts to match a given line against the {@link ProfileViewer#STACK_TRACE_ELEMENT} pattern.
     *
     * @param  line  the line to match
     * @return the matcher used for a successful match or null
     */
    private Matcher matchesStackTraceElement(String line) {
        Matcher m = ProfileViewer.STACK_TRACE_ELEMENT.matcher(line);
        if (m.matches()) {
            return m;
        } else {
            return null;
        }
    }

    /**
     * Attempts to match a given line against the {@link ProfileViewer#STACK_TRACE_END} pattern.
     *
     * @param  line  the line to match
     * @return the matcher used for a successful match or null
     */
    private Matcher matchesStackTraceEnd(String line) {
        Matcher m = ProfileViewer.STACK_TRACE_END.matcher(line);
        if (m.matches()) {
            return m;
        } else {
            return null;
        }
    }

    /**
     * Regular expression matching a stack trace line. The captured groups are:
     *   1 - the fully qualified method name (e.g. "java.lang.Object.wait")
     *   2 - the source file name (e.g. "Object.java")
     *   3 - the source line number (e.g. "234")
     */
    public static final Pattern JAVA_STACK_TRACE_ELEMENT = Pattern.compile("([A-Za-z_][A-Za-z0-9_\\.\\$]*)\\((.*\\.java):([1-9][0-9]*)\\)");

    /**
     * Attempts to match a given line against the {@link #JAVA_STACK_TRACE_ELEMENT} pattern.
     *
     * @param  line  the line to match
     * @return the matcher used for a successful match or null
     */
    private Matcher matchesJavaStackTraceElement(String line) {
        Matcher m = JAVA_STACK_TRACE_ELEMENT.matcher(line.trim());
        if (m.matches()) {
            return m;
        } else {
            return null;
        }
    }

    /**
     * Parses a single line from the trace and update the model accordingly.
     *
     * @param line               the line to parse
     * @param currentThread      the current thread
     * @param showServiceThread  specifies if the service thread should be included
     * @return the current thread
     */
    private ThreadSliceNode parseTraceLine(String line, ThreadSliceNode currentThread, boolean showServiceThread) {

        Matcher m;

        // Most common case first
        if (line.startsWith("*TRACE*:")) {
            TraceLine trace = new TraceLine(line.substring("*TRACE*:".length()), symbols);
            if (currentThread == null) {
                Assert.that(trace.threadID.equals(SERVICE_THREAD_ID));
                return currentThread;
            }

            if (!showServiceThread && trace.threadID.equals(SERVICE_THREAD_ID)) {
                return currentThread;
            }

            MethodNode currentMethod = currentThread.getCurrentMethod();
            if (currentMethod == null) {
                Assert.that(trace.depth == 0);
                currentMethod = currentThread.enterMethod(trace.method, 0);
            } else {
                if (currentMethod.depth > trace.depth) {
                    currentMethod = currentThread.exitToMethod(trace.depth, trace.method);
                } else if (currentMethod.depth < trace.depth) {
                    currentMethod = currentThread.enterMethod(trace.method, trace.depth);
                }
            }

            Assert.that(currentMethod.getMethod() == trace.method && currentMethod.depth == trace.depth);
            currentMethod.add(new InstructionNode(trace));
            return currentThread;
        }

        // Now look for a thread switch line
        if (line.startsWith("*THREADSWITCH*:")) {

            // strip prefix
            line = line.substring("*THREADSWITCH*:".length());

            int index = line.indexOf(':');
            String threadID;
            String stackTrace;
            if (index != -1) {
                threadID = line.substring(0, index);
                stackTrace = line.substring(index + 1);
            } else {
                threadID = line;
                stackTrace = null;
            }

            if (!showServiceThread && threadID.equals(SERVICE_THREAD_ID) ||
                (currentThread != null && currentThread.getThreadID().equals(threadID)))
            {
                return currentThread;
            }

            int slice = getLastSliceForThread(threadID) + 1;
            currentThread = new ThreadSliceNode(threadID, slice, stackTrace, symbols);
            root.add(currentThread);
            return currentThread;
        }

        // Find the right node to attach all other trace lines to
        TraceNode currentNode = root;
        if (currentThread != null) {
            currentNode = currentThread.getCurrentMethod();
            Assert.that(currentNode != null);
        }


        // Match *STACKTRACESTART*...
        if ((m = matchesStackTraceStart(line)) != null || (m = matchesProfileStackTraceStart(line)) != null) {
            currentNode.add(new OtherTraceNode(m.group(2) + " (bcount=" + m.group(1) + ")"));
            return currentThread;
        }

        // Match *STACKTRACE*...
        if ((m = matchesStackTraceElement(line)) != null) {
            Symbols.Method method = symbols.lookupMethod(Long.parseLong(m.group(1)));
            int pc = Integer.parseInt(m.group(2));
            ExecutionPoint ep = new ExecutionPoint(method, method.getSourceLineNumber(pc));
            currentNode.add(new StackTraceNode(ep));
            return currentThread;
        }

        // Match *STACKTRACEEND*
        if ((m = matchesStackTraceEnd(line)) != null) {
            return currentThread;
        }

        // Match a line from a Java stack trace dump
        if ((m = matchesJavaStackTraceElement(line)) != null) {
            String name = m.group(1);
            String fileName = m.group(2);
            String lineNumber = m.group(3);

            Symbols.Method method = new Symbols.Method();
            method.setSignature("void " + name + "()");


            // Strip off class name and method name
            File path = new File(name.replace('.', File.separatorChar));
            path = path.getParentFile().getParentFile();
            method.setFilePath(path.getPath() + File.separatorChar + ".java");

            ExecutionPoint ep = new ExecutionPoint(method, Integer.parseInt(lineNumber));
            currentNode.add(new StackTraceNode(ep));
            return currentThread;

        }

        // Catch all for any other lines
System.err.println("other node: " + line);
        currentNode.add(new OtherTraceNode(line));
        return currentThread;
    }

    /**
     * Reads the trace from a file and builds the corresponding trace model.
     *
     * @param name   the name of the file containing the trace
     */
    private void readTrace(String name, boolean showServiceThread) {
        InputFile in = new InputFile(name);

        // Create a relocator from the first line and use it to relocate any canonical
        // address in the symbols
        AddressRelocator relocator = createAddressRelocator(in.readLine());
        symbols.relocate(relocator);

        // Read the remainder of the trace
        String line;
        ThreadSliceNode currentThread = null;
        while ((line = in.readLine()) != null) {
            try {
                currentThread = parseTraceLine(line, currentThread, showServiceThread);
            } catch (TraceParseException tpe) {
                System.err.println(in.formatErrorMessage("error while reading trace - skipping the remainder"));
                tpe.printStackTrace();
            } catch (OutOfMemoryError ome) {
                throw new RuntimeException(in.formatErrorMessage("out of memory"));
            } catch (RuntimeException e) {
                throw new RuntimeException(in.formatErrorMessage(e.toString()), e);
            }
        }
    }

    /**
     * Gets a JTextArea component holding the source file corresponding to a given path.
     *
     * @param path  the path to a source file
     * @return a JTextArea component showing the source file annotated with line numbers
     * @throws IOException if the was an error locating, opening or reading the source file
     */
    private JTextArea getSourceFile(String path) throws IOException {
        JTextArea text = (JTextArea)sourceFileCache.get(path);
        if (text == null) {
            text = new JTextArea();
            text.setEditable(false);
            text.setFont(new Font("monospaced", Font.PLAIN, 12));
            InputStream is = sourcePath.openInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuffer sb = new StringBuffer(is.available() * 2);
            String line = br.readLine();
            int lineNo = 1;
            while (line != null) {
                sb.append(lineNo++);
                sb.append(":\t");
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            text.setText(sb.toString());
            sourceFileCache.put(path, text);
        }
        return text;
    }

    /**
     * Prints the usage message.
     *
     * @param errMsg   an optional error message or null
     */
    private void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: TraceViewer [-options] trace ");
        out.println("where options include:");
        out.println("    -map:<file>         map file containing method meta info");
        out.println("    -sp:<path>          where to find source files");
        out.println("    -noservice          exclude service thread trace");
        out.println("    -h                  show this message and exit");
        out.println();
    }

    /**
     * Parses the command line arguments and starts the trace viewer.
     *
     * @param args   command line arguments
     */
    private void run(String[] args) {
        Vector symbolsToLoad = new Vector();
        symbolsToLoad.addElement("squawk.sym");
        symbolsToLoad.addElement("squawk_dynamic.sym");

        String sourcePath = ".";
        boolean showServiceThread = true;

        int argc = 0;
        while (argc != args.length) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            } else if (arg.startsWith("-sp:")) {
                sourcePath = arg.substring("-sp:".length());
            } else if (arg.startsWith("-map:")) {
                symbolsToLoad.addElement(arg.substring("-map:".length()));
            } else if (arg.equals("-noservice")) {
                showServiceThread = false;
            } else if (arg.equals("-h")) {
                usage(null);
                return;
            } else {
                usage("Unknown option: " + arg);
                return;
            }
            argc++;
        }

        if (argc == args.length) {
            usage("Missing trace");
            return;
        }

        // Load the symbols
        for (Enumeration e = symbolsToLoad.elements(); e.hasMoreElements();) {
            symbols.loadIfFileExists((String)e.nextElement());
        }

        // Initialize the source path
        try {
            sourcePath = sourcePath.replace(':', File.pathSeparatorChar);
            this.sourcePath = (ClasspathConnection)Connector.open("classpath://" + sourcePath);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }

        String traceFileName = args[argc];
        readTrace(traceFileName, showServiceThread);
        initializeUI();
    }

    /**
     * Command line entry point.
     *
     * @param args  command line arguments
     */
    public static void main(String[] args) {
        TraceViewer viewer = new TraceViewer();
        viewer.run(args);
    }

    /*---------------------------------------------------------------------------*\
     *                                  GUI                                      *
    \*---------------------------------------------------------------------------*/

    /**
     * WindowListener implementation.
     */
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }
    public void windowClosed(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}

    /**
     * ComponentListener implementation.
     */
    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}
    public void componentResized(ComponentEvent e) {
        validate();
    }

    /**
     * The prefix to be used for the application GUI frame.
     */
    private static final String FRAME_TITLE_PREFIX = "Squawk trace viewer";

    /**
     * The tree selection listener that updates the source pane when a new node is selected.
     */
    class SourcePaneUpdater implements TreeSelectionListener {

        private final JScrollPane sourcePane;

        public SourcePaneUpdater(JScrollPane sourcePane) {
            this.sourcePane = sourcePane;
        }

        /**
         * {@inheritDoc}
         */
        public void valueChanged(TreeSelectionEvent e) {

            // Get the selected node
            TraceNode node = (TraceNode)tree.getLastSelectedPathComponent();

            // Source cannot be shown when there is no selected node
            if (node == null) {
                return;
            }

            ExecutionPoint ep = node.getExecutionPoint();
            if (ep == null) {
                return;
            }

            // Don't show source for methods that are collapsed and have children.
            if (node instanceof MethodNode) {
                MethodNode methodNode = (MethodNode)node;
                if (methodNode.computeNestedInstructionCount() != 0 && tree.isCollapsed(new TreePath(node.getPath()))) {
                    return;
                }
            }

            String path = ep.method.getFilePath();
            JTextArea text;
            try {
                text = getSourceFile(path);
            } catch (IOException ioe) {
                setTitle(FRAME_TITLE_PREFIX + " - ??/" + path);
                sourcePane.getViewport().setView(new JTextArea("An exception occurred while reading "+path+":\n\t"+ioe));
                return;
            }

            final int lineNo = ep.lineNumber;
            sourcePane.getViewport().setView(text);
            try {
                final int startPos = text.getLineStartOffset(lineNo - 1);
                final int endPos   = text.getLineEndOffset(lineNo - 1);
                text.setCaretPosition(endPos);
                text.moveCaretPosition(startPos);
                text.getCaret().setSelectionVisible(true);

                setTitle(FRAME_TITLE_PREFIX + " - " + path + ":" + lineNo);

                final JTextArea textArea = text;

                // Scroll so that the highlighted text is in the center
                // if is not already visible on the screen. The delayed
                // invocation is necessary as the view for the text
                // area will not have been computed yet.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // Compute the desired area of the text to show.
                        Rectangle textScrollRect = new Rectangle();
                        textScrollRect.y = (lineNo - 1) * textArea.getFontMetrics(textArea.getFont()).getHeight();
                        Rectangle visible = textArea.getVisibleRect();

                        textScrollRect.height = visible.height;
                        textScrollRect.y -= (visible.height >> 1);

                        // Compute the upper and lower bounds of what
                        // is acceptable.
                        int upper = visible.y + (visible.height >> 2);
                        int lower = visible.y - (visible.height >> 1);

                        // See if we really should scroll the text area.
                        if ((textScrollRect.y < lower) ||
                            (textScrollRect.y > upper)) {
                            // Check that we're not scrolling past the
                            // end of the text.
                            int newbottom = textScrollRect.y +
                                textScrollRect.height;
                            int textheight = textArea.getHeight();
                            if (newbottom > textheight) {
                                textScrollRect.y -= (newbottom - textheight);
                            }
                            // Perform the text area scroll.
                            textArea.scrollRectToVisible(textScrollRect);
                        }
                    }
                });

            } catch (BadLocationException ble) {
                ble.printStackTrace();
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
            }
        }
    }

    /**
     * Initializes the UI of the TraceViewer. This must be called only after the
     * data in the tree is complete.
     */
    private void initializeUI() {

        final DefaultTreeModel model = new DefaultTreeModel(root);
        tree = new TraceTree(model);

        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new TraceTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setScrollsOnExpand(false);
        tree.setRootVisible(false);

        // Enable tool tips.
        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.setInitialDelay(500);
        ttm.registerComponent(tree);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // Put the tree in a scrollable pane
        JScrollPane treeScrollPane = new JScrollPane(tree);

        // Place holder until a node is selected
        JPanel noSourcePanel = new JPanel(new GridBagLayout());
        noSourcePanel.add(new JLabel("No source file selected/available"));
        final JScrollPane sourceView = new JScrollPane(noSourcePanel);

        // Prevent the scroll panel from jumping left when scrolling down to a node
        // that doesn fit on the screen
        sourceView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Create the source pane updater
        tree.addTreeSelectionListener(new SourcePaneUpdater(sourceView));

        // Create search panel
        SearchPanel searchPanel = new SearchPanel();
        mainPanel.add("North", searchPanel);

        // Create node options panel
        mainPanel.add("South", nodeOptionsPanel);

        // Create the split pane for the tree and source panes
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, sourceView);
        splitPane.setDividerLocation(500);
        mainPanel.add("Center", splitPane);

        setTitle(FRAME_TITLE_PREFIX);

        // Maximize the window
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dimension = toolkit.getScreenSize();
        mainPanel.setPreferredSize(dimension);
        getContentPane().add(mainPanel);

        addWindowListener(this);
        addComponentListener(this);
        validate();
        pack();

        // Add a tree expansion listener that will modify what is expanded if SHIFT is pressed
        tree.addTreeExpansionListener(new ModifiedTreeExpansionAdapter(InputEvent.SHIFT_MASK) {
            public void treeExpanded(TreeExpansionEvent e, boolean modified) {
                TreePath path = e.getPath();

                // If the expansion occurred with a mouse click while SHIFT was pressed,
                // the complete path from the expanded node to its last leaf is expanded
                if (modified) {

                    TraceNode node = (TraceNode)path.getLastPathComponent();
                    if (!node.isLeaf()) {
                        node.expandInterestingPath(tree);
                    }
                }

                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
            }
        });

        // Expand the 'interesting' path in last thread slice
        if (root.getChildCount() != 0) {
            TraceNode lastSlice = (TraceNode)root.getLastChild();
            TraceNode lastPointOfExecution = lastSlice.expandInterestingPath(tree);

            // Set the selection at the last point of execution
            if (lastPointOfExecution != null) {
//System.err.println("Setting last path of execution to " + lastPointOfExecution);
                TreePath path = new TreePath(lastPointOfExecution.getPath());
                tree.expandPath(path);
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
            }
        }

        // Display the GUI
        setVisible(true);
    }

    /*---------------------------------------------------------------------------*\
     *                            Searching                                      *
    \*---------------------------------------------------------------------------*/

    /**
     * A Search instance encapsulate the criteria for performing a search for a node.
     */
    abstract class Search {

        /**
         * Specifies if the search should start from the top of the tree.
         */
        public final boolean searchFromTop;

        /**
         * Specifies if the search should be in reverse.
         */
        public final boolean backwards;

        /**
         * Creates a search object.
         *
         * @param searchFromTop boolean
         * @param backwards boolean
         */
        public Search(boolean searchFromTop, boolean backwards) {
        this.searchFromTop = searchFromTop;
            this.backwards = backwards;
        }

        /**
         * Determines if a given node's label is matched by this search.
         *
         * @param label  the label to test
         * @return true if <code>label</code> is matched by this search
         */
        public abstract boolean matches(String label);

        /**
         * Execute the search.
         */
        public final void execute() {

            TraceNode node;
            if (searchFromTop) {
                node = (TraceNode)tree.getModel().getRoot();
            } else {
                node = (TraceNode)tree.getLastSelectedPathComponent();
                node = (TraceNode)(backwards ? node.getPreviousNode() : node.getNextNode());
            }

            // Do the search
            while (node != null) {
                String label = (String)node.getTreeLabel(false, nodeOptionsPanel);
                if (label != null) {
                    if (matches(label)) {
                        break;
                    }
                }
                node = (TraceNode)(backwards ? node.getPreviousNode() : node.getNextNode());
            }

            if (node == null) {
                JOptionPane.showMessageDialog(null, "Finished searching trace");
            } else {
                TreePath path = new TreePath(node.getPath());
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                tree.setSelectionPath(path);
            }
        }
    }

    /**
     * A JPanel containing components that enable a JTree to be searched.
     */
    final class SearchPanel extends JPanel {

        /**
         * Creates the serach panel.
         */
        public SearchPanel() {

            FlowLayout layout = new FlowLayout(FlowLayout.LEADING);
            setLayout(layout);

            // Search text panel
            final JTextField textToFind = new JTextField(30);
            add(new JLabel("Find: "));
            add(textToFind);

            // Options
            final JCheckBox searchFromTop = new JCheckBox("Search from top of tree");
            final JCheckBox regex = new JCheckBox("Regular expression");
            final JCheckBox caseSensitive = new JCheckBox("Match case");
            final JCheckBox backward = new JCheckBox("Backward");

            add(searchFromTop);
            add(regex);
            add(caseSensitive);
            add(backward);

            // Buttons
            final JButton find = new JButton("Find");
            add(find);

            // Actions for buttons
            ActionListener buttonListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Search search;
                    if (regex.isSelected()) {
                        search = new Search(searchFromTop.isSelected(), backward.isSelected()) {
                            private final Pattern pattern = Pattern.compile(textToFind.getText(), caseSensitive.isSelected() ? 0 : Pattern.CASE_INSENSITIVE);
                            public boolean matches(String label) {
                                return pattern.matcher(label).matches();
                            }
                        };
                    } else {
                        search = new Search(searchFromTop.isSelected(), backward.isSelected()) {
                            private final String pattern = caseSensitive.isSelected() ? textToFind.getText() : textToFind.getText().toUpperCase();
                            public boolean matches(String label) {
                                if (!caseSensitive.isSelected()) {
                                    label = label.toUpperCase();
                                }
                                return label.indexOf(pattern) != -1;
                            }
                        };
                    }
                    search.execute();
                }
            };
            find.addActionListener(buttonListener);
            textToFind.addActionListener(buttonListener);
        }
    }


    /*---------------------------------------------------------------------------*\
     *                            Tree node types                                *
    \*---------------------------------------------------------------------------*/

    /**
     * A panel which check boxes for controlling what is displayed for the nodes in the tree.
     */
    final class NodeDetailOptionsPanel extends JPanel {

        final public JCheckBox showLocals;
        final public JCheckBox showStack;
        final public JCheckBox showVMState;

        final public JRadioButton decimal;
        final public JRadioButton hex;
        final public JRadioButton binary;

        private ValueFormatter formatter;

        /**
         * Creates the serach panel.
         *
         * @param tree  the JTree that will be searched
         */
        public NodeDetailOptionsPanel() {

            FlowLayout layout = new FlowLayout(FlowLayout.LEADING);
            setLayout(layout);

            // Options
            showLocals = new JCheckBox("Locals", true);
            showStack = new JCheckBox("Stack", true);
            showVMState = new JCheckBox("VM State", true);

            add(new JLabel("Node detail: "));
            add(showLocals);
            add(showStack);
            add(showVMState);

            // Format
            decimal = new JRadioButton("Decimal", true);
            hex = new JRadioButton("Hex", false);
            binary = new JRadioButton("Binary", false);
            final ButtonGroup group = new ButtonGroup();
            group.add(decimal);
            group.add(hex);
            group.add(binary);

            add(new JLabel("  Format: "));
            add(decimal);
            add(hex);
            add(binary);

            formatter = new ValueFormatter(10, false);

            ItemListener itemListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (decimal.isSelected()) {
                        formatter.changeBase(10);
                    } else if (hex.isSelected()) {
                        formatter.changeBase(16);
                    } else {
                        formatter.changeBase(2);
                    }
                    TraceViewer.this.repaint();
                }
            };

            showLocals.addItemListener(itemListener);
            showStack.addItemListener(itemListener);
            showVMState.addItemListener(itemListener);


            decimal.addItemListener(itemListener);
            hex.addItemListener(itemListener);
            binary.addItemListener(itemListener);
        }

        /**
         * Gets the object used to format the local variable and stack operand values in a node.
         *
         * @return the formatter
         */
        public ValueFormatter getValueFormatter() {
            return formatter;
        }
    }

    /**
     * The base type for all nodes in a trace viewer tree.
     */
    abstract static class TraceNode extends DefaultMutableTreeNode {

        /**
         * Configures a given renderer used to draw this node in a JTree.
         *
         * @param renderer    the renderer to configure
         * @param isExpanded  specifies if the node is expanded
         */
        public void configureRenderer(TraceTreeCellRenderer renderer, boolean isExpanded) {
        }

        /**
         * Gets the String to be used as the label for this node in a JTree.
         *
         * @param isExpanded  specifies if the node is expanded
         * @param options     the panel with options controlling what is show in the tree for each node
         * @return the label to display
         */
        public String getTreeLabel(boolean isExpanded, NodeDetailOptionsPanel options) {
            return "";
        }

        /**
         * Gets the object describing an execution point that this node corresponds to.
         *
         * @return an ExecutionPoint or null
         */
        public ExecutionPoint getExecutionPoint() {
            return null;
        }

        /**
         * Returns the number of levels between this node and the method method in the thread.
         * If this is the entry method in a thread, returns 0.
         *
         * @return  the number of levels above this node to the entry method
         */
        public int getCallDepth() {
            TraceNode parent = (TraceNode)getParent();
            return 1 + parent.getCallDepth();
        }

        /**
         * Expands the 'interesting' path of execution from this node. This is the path
         * from this node to it last leaf and includes
         * any exceptions that were thrown along the way.
         *
         * @param tree  the JTree in which to expand the path
         * @return the last instruction in the slice
         */
        public TraceNode expandInterestingPath(JTree tree) {
            TraceNode previous = null;
            if (getChildCount() != 0) {
                TraceNode node = (TraceNode)getFirstChild();
                Enumeration e = node.preorderEnumeration();
                if (e.hasMoreElements()) {
                    previous = (TraceNode)e.nextElement();
                    while (e.hasMoreElements()) {
                        node = (TraceNode)e.nextElement();
                        if (previous.isLeaf()) {
                            // If the previous node jumps back more than one frame (i.e. an
                            // exception throw), then expand it.
                            if ((previous.getCallDepth() - 1) > node.getCallDepth()) {
                                TraceNode parent = (TraceNode)previous.getParent();
                                tree.expandPath(new TreePath(parent.getPath()));
                            }
                        }
                        previous = node;
                    }

                    // Expand the last node
                    TraceNode parent = (TraceNode)previous.getParent();
                    tree.expandPath(new TreePath(parent.getPath()));
                }
                return previous;
            } else {
                return null;
            }
        }

    }

    /**
     * The pattern for a stack trace element.
     * Capturing group 1 is the method address and group 2 bytecode offset.
     */
    private static final Pattern STACKTRACE_ELEMENT = Pattern.compile("(\\d+)@(\\d+)");

    /**
     * A ThreadSliceNode encapsulates the trace data for an execution on a particular thread
     * before a switch to another thread occurred.
     */
    final static class ThreadSliceNode extends TraceNode {

        /**
         * The slice of the thread represented by this node.
         */
        public final int slice;

        /**
         * The current method.
         */
        private MethodNode currentMethod;

        /**
         * Creates a ThreadSliceNode.
         *
         * @param threadID   the ID of the thread
         * @param slice      the ordinal position of this slice with respect to all other slices for the thread
         * @param stackTrace the stack trace encapsulating the current call stack for the thread
         * @param symbols    the database of method symbols
         */
        public ThreadSliceNode(String threadID, int slice, String stackTrace, Symbols symbols) {
            setUserObject(threadID);
            this.slice = slice;

            if (stackTrace != null) {
                initializeCallStack(stackTrace, symbols);
            }
        }

        /**
         * {@inheritDoc}
         */
        public int getCallDepth() {
            return -1;
        }

        /**
         * Gets the ID of the thread.
         *
         * @return the ID of the thread
         */
        public String getThreadID() {
            return (String)getUserObject();
        }

        /**
         * Gets the current insertion point for new methods or instructions.
         *
         * @return the current insertion point for new methods or instructions
         */
        public MethodNode getCurrentMethod() {
            return currentMethod;
        }

        /**
         * Initializes the call stack and current method of this slice based on a given stack trace.
         *
         * @param stackTrace the stack trace encapsulating the current call stack for the thread
         * @param symbols    the database of method symbols
         */
        private void initializeCallStack(String stackTrace, Symbols symbols) {
            StringTokenizer st = new StringTokenizer(stackTrace, ":");
            Stack stack = new Stack();
            stack.ensureCapacity(st.countTokens());
            while (st.hasMoreTokens()) {
                stack.push(st.nextToken());
            }

            int depth = 0;
            while (!stack.empty()) {
                String element = (String)stack.pop();
                Matcher m = STACKTRACE_ELEMENT.matcher(element);

                if (!m.matches()) {
                    throw new TraceParseException(element, m.pattern().pattern());
                }

                Symbols.Method method = symbols.lookupMethod(Long.parseLong(m.group(1)));
                enterMethod(method, depth++);
            }
        }

        /**
         * Adds a node representing the entry to a new method from the current method.
         * The call depth of the new method frame must be one greater than the call depth of
         * the current method.
         *
         * @param method  the method called from the current method
         * @param depth   the depth of the new frame
         * @return the new current method
         */
        public MethodNode enterMethod(Symbols.Method method, int depth) {
            if (currentMethod == null) {
                Assert.that(depth == 0);
                currentMethod = new MethodNode(method, depth);
                add(currentMethod);
            } else {
                Assert.that(currentMethod.depth == depth - 1);
                MethodNode methodNode = new MethodNode(method, depth);
                currentMethod.add(methodNode);
                currentMethod = methodNode;
            }
            return currentMethod;
        }

        /**
         * Resets the current method to a method frame higher up on the current call stack.
         *
         * @param depth   the depth to which the call stack is unwound
         * @param method  the method that must be at the frame unwound to
         * @return the new current method
         */
        public MethodNode exitToMethod(int depth, Symbols.Method method) {
            Assert.that(currentMethod != null);
            Assert.that(currentMethod.depth > depth);

            currentMethod = (MethodNode)currentMethod.getParent();
            while (currentMethod.getMethod() != method || currentMethod.depth != depth) {
                currentMethod = (MethodNode)currentMethod.getParent();
                Assert.that(currentMethod != null);
            }
            return currentMethod;
        }

        /**
         * {@inheritDoc}
         */
        public void configureRenderer(TraceTreeCellRenderer renderer, boolean isExpanded) {
            renderer.setFont(renderer.plain);
        }

        /**
         * {@inheritDoc}
         */
        public String getTreeLabel(boolean isExpanded, NodeDetailOptionsPanel options) {
            return "Thread-" + getUserObject() + ":" + slice;
        }
    }

    /**
     * A <code>MethodNode</code> instance encapsulates the trace of one or more instructions within
     * a given method in a frame on a thread's call stack.
     */
    final static class MethodNode extends TraceNode {

        /**
         * The call depth of this method's frame within its thread's call stack.
         */
        public final int depth;

        /**
         * The maximum width of the String representation for each component in all the trace
         * lines of this method.
         */
        private final int[] componentWidths;

        /**
         * Creates a MethodNode.
         *
         * @param method   the represented method
         * @param depth    the call depth of this method's frame within its thread's call stack
         */
        public MethodNode(Symbols.Method method, int depth) {
            setUserObject(method);
            this.depth = depth;
            this.componentWidths = new int[TraceLine.COMPONENT_COUNT];
        }

        /**
         * {@inheritDoc}
         */
        public ExecutionPoint getExecutionPoint() {
            Symbols.Method method = getMethod();
            return new ExecutionPoint(method, method.getSourceLineNumber(0));
        }

        /**
         * {@inheritDoc}
         */
        public int getCallDepth() {
            return depth;
        }

        /**
         * Overrides parent so that component widths are updated when an InstructionNode is added.
         *
         * @param newChild  the child being added
         */
        public void add(MutableTreeNode newChild) {
            if (newChild instanceof InstructionNode) {
                ((InstructionNode)newChild).getTraceLine().updateComponentWidths(componentWidths);
            }
            super.add(newChild);
        }

        /**
         * Counts the number of descendants of this node that an instance of InstructionNode.
         *
         * @return the number of InstructionNode descendants of this node
         */
        public int computeNestedInstructionCount() {
            int count = 0;
            Enumeration e = breadthFirstEnumeration();
            while (e.hasMoreElements()) {
                if (e.nextElement() instanceof InstructionNode) {
                    ++count;
                }
            }
            return count;
        }

        /**
         * Gets the method represented by this node.
         *
         * @return the method represented by this node
         */
        public Symbols.Method getMethod() {
            return (Symbols.Method)getUserObject();
        }

        /**
         * {@inheritDoc}
         */
        public void configureRenderer(TraceTreeCellRenderer renderer, boolean isExpanded) {
            renderer.setFont(renderer.bold);
        }

        /**
         * {@inheritDoc}
         */
        public String getTreeLabel(boolean isExpanded, NodeDetailOptionsPanel options) {
            Symbols.Method method = (Symbols.Method)getUserObject();
            if (!isExpanded) {
                int count = computeNestedInstructionCount();
                return "[" + count + "] " + method.getSignature();
            } else {
                return method.getSignature();
            }
        }
    }

    /**
     * An <code>InstructionNode</code> instance represents the VM state immediately before executing a particular instruction.
     */
    final static class InstructionNode extends TraceNode {

        /**
         * Creates an InstructionNode to represent a given trace line
         *
         * @param trace   the trace line
         */
        public InstructionNode(TraceLine trace) {
            setUserObject(trace);
        }

        /**
         * Gets the TraceLine represents by this node.
         * @return  the TraceLine represents by this node
         */
        public TraceLine getTraceLine() {
            return (TraceLine)getUserObject();
        }

        /**
         * {@inheritDoc}
         */
        public ExecutionPoint getExecutionPoint() {
            TraceLine trace = getTraceLine();
            return new ExecutionPoint(trace.method, trace.method.getSourceLineNumber(trace.pc));
        }

        /**
         * {@inheritDoc}
         */
        public void configureRenderer(TraceTreeCellRenderer renderer, boolean isExpanded) {
            renderer.setFont(renderer.fixed);
        }

        /**
         * {@inheritDoc}
         */
        public String getTreeLabel(boolean isExpanded, NodeDetailOptionsPanel options) {
            boolean showLocals = options.showLocals.isSelected();
            boolean showStack = options.showStack.isSelected();
            boolean showVMState = options.showVMState.isSelected();
            int[] componentWidths = ((MethodNode)getParent()).componentWidths;
            return getTraceLine().getTreeNodeLabel(showLocals, showStack, showVMState, options.getValueFormatter(), componentWidths);
        }

    }

    /**
     * A <code>StackTraceNode</code> instance represents an element in a stack trace.
     */
    final static class StackTraceNode extends TraceNode {

        /**
         * Creates a node for an element in a stack trace.
         *
         * @param ep  the execution point corresponding to the stack trace element
         */
        public StackTraceNode(ExecutionPoint ep) {
            setUserObject(ep);
        }

        /**
         * {@inheritDoc}
         */
        public ExecutionPoint getExecutionPoint() {
            return (ExecutionPoint)getUserObject();
        }

        /**
         * {@inheritDoc}
         */
        public void configureRenderer(TraceTreeCellRenderer renderer, boolean isExpanded) {
            renderer.setFont(renderer.plain);
        }

        /**
         * {@inheritDoc}
         */
        public String getTreeLabel(boolean isExpanded, NodeDetailOptionsPanel options) {
            ExecutionPoint ep = getExecutionPoint();
            Symbols.Method method = ep.method;
            return method.getName(true) + '(' + method.getFile() + ':' + ep.lineNumber + ')';
        }

    }

    /**
     * An <code>OtherTraceNode</code> instance represents a miscellaneous line of output in the trace file.
     */
    final static class OtherTraceNode extends TraceNode {

        /**
         * Creates a node that represents a miscellaneous line of output in the trace file.
         *
         * @param line  the line
         */
        public OtherTraceNode(String line) {
            setUserObject(line);
        }

        /**
         * {@inheritDoc}
         */
        public void configureRenderer(TraceTreeCellRenderer renderer, boolean isExpanded) {
            renderer.setFont(renderer.italic);
            renderer.setIcon(null);
        }

        /**
         * {@inheritDoc}
         */
        public String getTreeLabel(boolean isExpanded, NodeDetailOptionsPanel options) {
            return (String)getUserObject();
        }
    }

    /*---------------------------------------------------------------------------*\
     *                            Tree node renderer                             *
    \*---------------------------------------------------------------------------*/

    /**
     * A tree renderer that can render TraceNodes.
     */
    final class TraceTreeCellRenderer extends DefaultTreeCellRenderer {

        /**
         * Some preallocated fonts for differentiating the nodes' text.
         */
        public final Font plain, italic, bold, fixed;

        /**
         * Constructs a TraceTreeCellRenderer.
         */
        public TraceTreeCellRenderer() {
            plain  = new Font(null, Font.PLAIN, 12);
            italic = plain.deriveFont(Font.ITALIC);
            bold   = plain.deriveFont(Font.BOLD);
            fixed  = new Font("monospaced", Font.PLAIN, 12);
        }

        /**
         * {@inheritDoc}
         */
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus)
        {
            TraceNode node = (TraceNode)value;
            node.configureRenderer(this, expanded);
            String label = node.getTreeLabel(expanded, nodeOptionsPanel);
//System.err.println("" + (++count) + ": " + label);
            super.getTreeCellRendererComponent(tree, label, sel, expanded, leaf, row, hasFocus);

            if (!leaf && !expanded) {
                setToolTipText("Hold SHIFT key when expanding to completely show last execution path");
            } else {
                setToolTipText(null);
            }
            return this;
        }
    }

}

/**
 * The exception thrown when a component in a trace is not formed as expected.
 */
class TraceParseException extends RuntimeException {
    public TraceParseException(String input, String pattern) {
        super("\"" + input + "\" failed to match \"" + pattern + "\"");
    }
}

/**
 * A TraceLine instance encapsulates all the information parsed from a single trace line.
 */
final class TraceLine {

    /**
     * The number of logical number of components in a trace line.
     */
    public static final int COMPONENT_COUNT = 5;

    /**
     * An Instruction represents the information about the instruction in a trace line.
     */
    static class Instruction {

        /**
         * The pattern for an instruction within a trace line.
         * Capturing group 1 is the instruction opcode, group 2 is the mutation type of
         * the instruction (which may not be present), group 3 is the WIDE_* or ESCAPE_*
         * prefix (or -1 if there is no prefix) and group 4 is the immediate operand (if any).
         */
        static final Pattern INSTRUCTION = Pattern.compile("(\\d+)(?:#(\\d+))?,(-?\\d+)(?:,(-?\\d+))?");

        /**
         * Primary opcode.
         */
        public final int opcode;

        /**
         * Opcode of WIDE_* or ESCAPE_* prefix or -1 if there is not prefix.
         */
        public final int prefix;

        /**
         * The operand (if any).
         */
        public final String operand;

        /**
         * The mutation type of the instruction or -1 if VM was not built with the typemap.
         */
        public final int mutationType;

        /**
         * Creates an Instruction from the instruction substring in a trace line.
         *
         * @param trace  the substring which must match the {@link #INSTRUCTION} pattern
         */
        public Instruction(String trace, Symbols symbols) {
            Matcher m = INSTRUCTION.matcher(trace);
            if (!m.matches()) {
                throw new TraceParseException(trace, m.pattern().pattern());
            }

            // Parse the opcode
            opcode = Integer.parseInt(m.group(1));

            // Parse the mutation type (if any)
            String mutationTypeGroup = m.group(2);
            if (mutationTypeGroup != null) {
                mutationType = Byte.parseByte(mutationTypeGroup);
            } else {
                mutationType = -1;
            }

            // Parse the WIDE_* or ESCAPE_* prefix
            prefix = Integer.parseInt(m.group(3));

            // Parse the operand
            String operandGroup = m.group(4);
            if (operandGroup == null) {
                operand = null;
            } else {
                switch (opcode) {
                    case OPC.INVOKENATIVE_I:
                    case OPC.INVOKENATIVE_I_WIDE:
                    case OPC.INVOKENATIVE_L:
                    case OPC.INVOKENATIVE_L_WIDE:
                    case OPC.INVOKENATIVE_O:
                    case OPC.INVOKENATIVE_O_WIDE:
                    case OPC.INVOKENATIVE_V:
                    case OPC.INVOKENATIVE_V_WIDE:
/*if[FLOATS]*/
                    case OPC.INVOKENATIVE_F:
                    case OPC.INVOKENATIVE_F_WIDE:
                    case OPC.INVOKENATIVE_D:
                    case OPC.INVOKENATIVE_F_WIDE:
/*end[FLOATS]*/
                    {
                        int nativeMethodIdentifier = Integer.parseInt(operandGroup);
                        operand = symbols.lookupNativeMethod(nativeMethodIdentifier);
                        break;
                    }
                    default:
                        operand = operandGroup;
                }
            }
        }

        /**
         * Appends instruction to a given StringBuffer.
         *
         * @param buf  the buffer to append to
         */
        public void appendTo(StringBuffer buf) {
            if (prefix != -1) {
                buf.append(Mnemonics.OPCODES[prefix]).append(' ');
            }
            buf.append(Mnemonics.OPCODES[opcode]);
            if (mutationType != -1) {
                int type = mutationType & AddressType.TYPE_MASK;
                buf.append(':').append(AddressType.Mnemonics.charAt(type));
            }
            buf.append(' ');
            if (operand != null) {
                buf.append(' ').append(operand);
            }
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            StringBuffer buf = new StringBuffer(200);
            appendTo(buf);
            return buf.toString();
        }
    }

    /**
     * A Words instance represents the values in a contiguous set of words present in a trace line.
     */
    static class Words {

        /**
         * Shared constant for an empty set of values.
         */
        private final long[] NO_VALUES = {};

        /**
         * The values in the words.
         */
        private final long[] values;

        /**
         * The type of the values. This will be null if the VM was not compiled with type map support.
         */
        private final byte[] types;

        /**
         * Creates a Words instance.
         *
         * @param trace     the substring from a trace line containing zero or more word values
         * @param hasTypes  specifies if the word values have an annotated type
         */
        public Words(String trace, boolean hasTypes) {
            StringTokenizer st = new StringTokenizer(trace, ",");
            if (st.hasMoreTokens()) {
                int count = st.countTokens();
                long[] values = new long[count];
                byte[] types = (hasTypes) ? new byte[count] : null;

                for (int i = 0; i != count; ++i) {
                    String token = st.nextToken();
                    if (hasTypes) {
                        int index = token.indexOf('#');
                        String value = token.substring(0, index);
                        if (value.equals("X")) {
                            values[i] = 0xdeadbeef;
                        } else {
                            values[i] = Long.parseLong(value);
                        }
                        types[i] = Byte.parseByte(token.substring(index + 1));
                    } else {
                        if (token.equals("X")) {
                            values[i] = 0xdeadbeef;
                        } else {
                            values[i] = Long.parseLong(token);
                        }
                    }
                }

                this.values = values;
                this.types = types;
            } else {
                values = NO_VALUES;
                types = null;
            }
        }

        /**
         * Gets the number of values in this object.
         *
         * @return the number of values in this object
         */
        public int getSize() {
            return values.length;
        }

        /**
         * Gets the value at a given index.
         *
         * @param i  the index
         * @return the value at index <code>i</code>
         */
        public long getValue(int i) {
            return values[i];
        }

        /**
         * Gets the type of the value at a given index.
         *
         * @param i  the index
         * @return the type of the value at index <code>i</code> or -1 if there is no type information
         *
         */
        public int getType(int i) {
            if (types == null) {
                return -1;
            }
            return types[i] & 0xFF;
        }

        /**
         * Appends values to a given StringBuffer.
         *
         * @param buf  the buffer to append to
         */
        public void appendTo(StringBuffer buf, ValueFormatter formatter) {
            boolean hasTypes = (types != null);
            for (int i = 0; i != values.length; ++i) {
                long value = values[i];
                if (value == 0xdeadbeef) {
                    buf.append('X');
                } else {
                    if (formatter == null) {
                        buf.append(value);
                    } else {
                        buf.append(formatter.format(value));
                    }
                }
                if (hasTypes) {
                    buf.append(':').append(AddressType.getMnemonic(types[i]));
                }
                if (i != values.length - 1) {
                    buf.append(' ');
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            StringBuffer buf = new StringBuffer(200);
            appendTo(buf, null);
            return buf.toString();
        }
    }

    /**
     * Used to interpret a words component that really says what the max_stack value for a method is.
     */
    static class MaxStack extends Words {

        /**
         * Creates a MaxStack instance.
         *
         * @param trace     the substring from a trace line containing zero or more word values
         */
        public MaxStack(String trace) {
            super(trace, false);
            Assert.that(getSize() == 2);
        }

        /**
         * {@inheritDoc}
         */
        public void appendTo(StringBuffer buf) {
            buf.append(this.toString());
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return "max_stack=" + getValue(0);
        }
    }

    /**
     * Used to interpret a words component that really says what the max_locals value for a method is.
     */
    static class MaxLocals extends Words {

        /**
         * Creates a MaxLocals instance.
         *
         * @param trace     the substring from a trace line containing zero or more word values
         */
        public MaxLocals(String trace) {
            super(trace, false);
            Assert.that(getSize() == 1);
        }

        /**
         * {@inheritDoc}
         */
        public void appendTo(StringBuffer buf) {
            buf.append(this.toString());
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return "max_locals=" + getValue(0);
        }
    }

    /**
     * The numeric identifier of the thread.
     */
    public final String threadID;

    /**
     * The call depth of the trace.
     */
    public final int depth;

    /**
     * The method.
     */
    public final Symbols.Method method;

    /**
     * The bytecode offset of the instruction.
     */
    public final int pc;

    /**
     * The instruction.
     */
    public final Instruction instruction;

    /**
     * The values of the local variable slots.
     */
    public final Words locals;

    /**
     * The values on the operand stack.
     */
    public final Words stack;

    /**
     * The address of the top of the operand stack.
     */
    public final long stackPointer;

    /**
     * Backward branch count.
     */
    public final long branchCount;

    /**
     * Number of words remaining on the thread's stack.
     */
    public final long remainingStack;

    /**
     * The pattern for a trace line after the "*TRACE*:" prefix.
     * Capturing group 1 is the thread ID, group 2 is the call depth,
     * group 3 is the method address, group 4 is the bytecode offset, group 5
     * is the instruction opcode, prefix and operand, group 6
     * is the local variable values, group 7 is the operand stack values, group
     * 8 is the stack pointer, group 9 is the backward branch count, group 10
     * is the remaining number of stack words and group 11 is the rest of the
     * line.
     */
    static final Pattern TRACELINE = Pattern.compile("([^:]+):([^:]+):([^:]+):([^:]+):([^:]+):([^:]*):([^:]*):([^:]+):([^:]+):([^:]+)(?::(.*))?");

    /**
     * Most recently constructed TraceLine instance. This is used to share common immutable components.
     */
    private static TraceLine previous = null;
    private static Matcher previousMatcher = null;

    /**
     * Creates a TraceLine from a given line from the trace file/stream.
     *
     * @param line a trace line with the "*TRACE*:" prefix removed.
     */
    public TraceLine(String line, Symbols symbols) {

        Matcher m = TRACELINE.matcher(line);
        if (!m.matches()) {
            throw new TraceParseException(line, m.pattern().pattern());
        }

        // Parse thread ID
        threadID = m.group(1);

        // Parse the call depth
        depth = Integer.parseInt(m.group(2));

        // Parse the method address
        method = symbols.lookupMethod(Long.parseLong(m.group(3)));

        // Parse the bytecode offset
        pc = Integer.parseInt(m.group(4));

        // Parse the instruction
        instruction = new Instruction(m.group(5), symbols);

        // Parse the local variable values
        String group = m.group(6);
        if (previous != null && group.equals(previousMatcher.group(6))) {
            locals = previous.locals;
        } else {
            switch (instruction.opcode) {
                case OPC.EXTEND:
                case OPC.EXTEND0:
                case OPC.EXTEND_WIDE: {
                    locals = new MaxLocals(group);
                    break;
                }
                default: {
                    locals = new Words(group, instruction.mutationType != -1);
                    break;
                }
            }
        }

        // Parse the operand stack values
        group = m.group(7);
        if (previous != null && group.equals(previousMatcher.group(7))) {
            stack = previous.stack;
        } else {
            switch (instruction.opcode) {
                case OPC.EXTEND:
                case OPC.EXTEND0:
                case OPC.EXTEND_WIDE: {
                    stack = new MaxStack(group);
                    break;
                }
                default: {
                    stack = new Words(group, instruction.mutationType != -1);
                    break;
                }
            }
        }

        // Parse the VM state stuff
        stackPointer = Long.parseLong(m.group(8));
        branchCount = Long.parseLong(m.group(9));
        remainingStack = Integer.parseInt(m.group(10));

        previous = this;
        previousMatcher = m;
    }

    /**
     * Updates the maximum width for a component.
     *
     * @param widths the table of maximum component widths
     * @param index  the index of the component to be updated
     * @param width  the width of an component instance
     */
    private static void updateWidth(int[] widths, int index, int width) {
        if (widths[index] < width) {
            widths[index] = width;
        }
    }

    /**
     * Updates the maximum widths for all the components in a trace line.
     *
     * @param widths the table of maximum component widths
     */
    void updateComponentWidths(int[] widths) {
        Assert.that(widths.length == COMPONENT_COUNT);
        StringBuffer buf = new StringBuffer(200);
        updateWidth(widths, 0, Integer.toString(pc).length());
        updateWidth(widths, 1, locals.toString().length());
        updateWidth(widths, 2, stack.toString().length());
        updateWidth(widths, 3, instruction.toString().length());
        updateWidth(widths, 4, getVMState().length());
    }

    private static void pad(StringBuffer buf, int width) {
        while (buf.length() < width) {
            buf.append(' ');
        }
    }

    /**
     * Gets the label for the node in a JTree representing this trace line.
     *
     * @param showLocals   include the locals in the label
     * @param showStack    include the operand stack in the label
     * @param showStack    include the VM state in the label
     * @param formatter    formats the locals and stack values
     * @param componentWidths the table of maximum component widths or null
     * @return the label
     */
    public String getTreeNodeLabel(boolean showLocals, boolean showStack, boolean showVMState, ValueFormatter formatter, int[] componentWidths) {
        StringBuffer buf = new StringBuffer(400);
        int pad = (componentWidths == null ? 0 : componentWidths[0]) + 2;

        // Append the bytecode offset
        buf.append(pc).append(": ");
        pad(buf, pad);

        // Append the locals
        if (showLocals) {
            buf.append('[');
            locals.appendTo(buf, formatter);
            buf.append("] ");
            if (componentWidths != null) {
                pad += componentWidths[1] + 3;
                pad(buf, pad);
            }
        }

        // Append the stack
        if (showStack) {
            buf.append('{');
            stack.appendTo(buf, formatter);
            buf.append("} ");
            if (componentWidths != null) {
                pad += componentWidths[2] + 3;
                pad(buf, pad);
            }
        }

        // Append the instruction
        instruction.appendTo(buf);
        buf.append(' ');
        if (componentWidths != null) {
            pad += componentWidths[3] + 1;
            pad(buf, pad);
        }

        if (showVMState) {
            // Append the VM state
            appendVMState(buf);
        }

        return buf.toString();
    }

    /**
     * Appends the string representation of the VM stae encapsulated in this trace line
     * to a given StringBuffer
     *
     * @param buf the buffer to append to
     */
    public void appendVMState(StringBuffer buf) {
        buf.append("sp=").
            append(stackPointer).
            append(" bcount=").
            append(branchCount).
            append(" sp-sl=").
            append(remainingStack);
    }

    /**
     * Gets the string representation of the VM stae encapsulated in this trace line.
     *
     * @return the string representation of the VM stae encapsulated in this trace line
     */
    public String getVMState() {
        StringBuffer buf = new StringBuffer(100);
        appendVMState(buf);
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return getTreeNodeLabel(true, true, true, null, null);
    }

}

/**
 * Formatter for local and operand stack values.
 */
final class ValueFormatter {

    /**
     * The radix used to format a value as a string
     */
    private int radix;

    /**
     * Specifies if the values are 32 or 64 bit quantities.
     */
    private boolean is64bit;

    /**
     * Creates a ValueFormatter.
     *
     * @param radix    the radix to use in the string representation
     * @param is64bit  specifies if the values are 32 or 64 bit quantities
     */
    public ValueFormatter(int radix, boolean is64bit) {
        this.radix = radix;
        this.is64bit = is64bit;
    }

    /**
     * Formats a given value as a String.
     *
     * @param value   the value to format
     * @return the String representation of <code>value</code>
     */
    public String format(long value) {
        String s = is64bit ? Long.toString(value, radix) : Integer.toString((int)value, radix);
        if (radix == 16) {
            s = "0x" + s.toUpperCase();
        }
        return s;
    }

    /**
     * Updates the radix used to format the values.
     *
     * @param radix  the new radix
     */
    public void changeBase(int radix) {
        this.radix = radix;
    }

    /**
     * Updates the flag specifying if the values are to be formatted as 32 or 64 bit values.
     *
     * @param is64bit  the new value of the flag
     */
    public void change64BitFlag(boolean is64bit) {
        this.is64bit = is64bit;
    }
}

