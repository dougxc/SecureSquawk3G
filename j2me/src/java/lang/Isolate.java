/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.vm.CS;
import com.sun.squawk.vm.CID;
import com.sun.squawk.vm.ChannelConstants;
import com.sun.squawk.util.Hashtable; // Version without synchronization
import com.sun.squawk.util.Assert;

import java.io.*;
import java.util.*;

/**
 * The Squawk implementation of isolates.
 *
 * @author Nik Shaylor, Doug Simon
 */
public final class Isolate implements Runnable {

    /**
     * Constant denoting that an isolate has been created but not yet {@link #start() started}.
     */
    private final static int NEW = 0;

    /**
     * Constant denoting that an isolate has been {@link #start() started} and the
     * {@link #run()} method has been called on its initial thread.
     */
    private final static int ALIVE = 1;

    /**
     * Constant denoting that an isolate has been {@link #hibernate() hibernated}.
     */
    private final static int HIBERNATED = 2;

    /**
     * Constant denoting that an isolate has been {@link #exit exited}.
     */
    private final static int EXITED = 3;

    /**
     * The name of the class to be executed.
     */
    private final String mainClassName;

    /**
     * The command line arguments for the class to be executed.
     */
    private final String[] args;

    /**
     * The suite into which any dynamically loaded classes are installed.
     * This is also the starting point when doing class look up.
     */
    private Suite openSuite;

    /**
     * The immutable bootstrap that is shared across all isolates.
     */
    private final Suite bootstrapSuite;

    /**
     * The child threads of the isolate.
     */
    private Hashtable childThreads = new Hashtable();

    /**
     * The parent isolate that created and started this isolate.
     */
    private Isolate parentIsolate;

    /**
     * The child isolates of the isolate.
     */
    private Hashtable childIsolates;

    /**
     * Flag to show that class Klass has been initialized.
     */
    private boolean classKlassInitialized;

    /**
     * The current state of the isolate.
     */
    private int state;

    /**
     * Isolate exit code.
     */
    private int exitCode;

    /**
     * The source URL of the direct parent suite of the open suite.
     */
    private final String parentSuiteSourceURL;

    /**
     * The path where class files and suite files can be found.
     */
    private final String classPath;

    /**
     * The channel I/O handle.
     */
    private int channelContext;

    /**
     * The hibernated channel context.
     */
    private int hibernatedContext;

    /**
     * The GUI input channel.
     */
    private int guiIn;

    /**
     * The GUI output channel.
     */
    private int guiOut;

    /**
     * Hashtable that holds the monitors for objects in ROM.
     */
    private Hashtable monitorHashtable = new Hashtable();

    /**
     * The translator that is to be used to locate, load and convert classes
     * that are not currently installed in the system.
     */
    private TranslatorInterface translator;

    /**
     * Pointer to first class state record.
     */
    private Object classStateQueue;

    /**
     * The interned strings for the isolate.
     */
    private Hashtable internedStrings;

    /**
     * List of finalizers that need to be run.
     */
    private Finalizer finalizers;

    /**
     * List of threads ready to run after return from hibernated state.
     */
    private Thread hibernatedRunThreads;

    /**
     * List of threads to be placed on the timer queue after return from hibernated state.
     */
    private Thread hibernatedTimerThreads;

    /**
     * List of stack chunks to be append to the global list of stack chunks after return from hibernated state.
     */
    private Object hibernatedStackChunks;

    /**
     * List of threads waiting for the isolate to exit or hibernate.
     */
    private Thread joiners;

    /**
     * Properties that can be set by the owner of the isolate.
     */
    private Hashtable properties;

    /**
     * A flag that is set when the main class of an isolate starts with "javasoft.sqe.tests.".
     * This flag is used by the thread scheduler to prevent infinite or longs hangs.
     */
    private final boolean isTckTest;

    /**
     * Creates the root isolate.
     *
     * @param mainClassName  the name of the class with bootstrap main()
     * @param args           the command line arguments
     * @param bootstrapSuite the bootstrap suite
     */
    Isolate(String mainClassName, String[] args,  Suite bootstrapSuite) {
        this.mainClassName        = mainClassName;
        this.args                 = args;
        this.bootstrapSuite       = bootstrapSuite;
        this.openSuite            = bootstrapSuite;
        this.classPath            = null;
        this.parentSuiteSourceURL = null;
        this.isTckTest            = false;
        this.state                = NEW;
        Assert.always(VM.getCurrentIsolate() == null);
    }

    /**
     * Creates an new isolate.
     *
     * @param mainClassName the name of the class with main()
     * @param args          the command line arguments
     * @param classPath     the path where classes and suites can be found
     * @param parentSuiteSourceURL
     */
    public Isolate(String mainClassName, String[] args,  String classPath, String parentSuiteSourceURL) {
        this.mainClassName        = mainClassName;
        this.args                 = args;
        this.classPath            = classPath;
        this.parentSuiteSourceURL = parentSuiteSourceURL;
        this.isTckTest            = mainClassName.startsWith("javasoft.sqe.tests.");
        this.state                = NEW;
        Assert.that(classPath != null);
        parentIsolate = VM.getCurrentIsolate();
        Assert.that(parentIsolate != null);
        parentIsolate.addIsolate(this);
        bootstrapSuite = parentIsolate.bootstrapSuite;
    }

    /**
     * Get the class path for the isolate.
     *
     * @return the class path
     */
    public String getClassPath() {
        return classPath;
    }

    /**
     * Get the name of the main class.
     *
     * @return the name
     */
    public String getMainClassName() {
        return mainClassName;
    }

    /**
     * Determines if the isolate is running a TCK test. This is true if the main class name
     * starts with "javasoft.sqe.tests.".
     *
     * @return true if the isolate is running a TCK test
     */
    boolean isTckTest() {
        return isTckTest;
    }

    /**
     * Get the arguments.
     *
     * @return the arguments
     */
    public String[] getMainClassArguments() {
        return args;
    }

    /**
     * Gets the bootstrap suite.
     *
     * @return the bootstrap suite
     */
    public Suite getBootstrapSuite() {
        return bootstrapSuite;
    }

    /**
     * Gets the suite into which any dynamically loaded classes (i.e. those
     * loaded via {@link Class#forName(String)}) installed.
     *
     * @return the suite into which any dynamically loaded classes are installed
     */
    public Suite getOpenSuite() {
        return openSuite;
    }

    /**
     * Gets the monitor hash table for the isolate
     *
     * @return the hash table
     */
    Hashtable getMonitorHashtable() {
        return monitorHashtable;
    }

    /**
     * Sets the translator.
     *
     * @param translator the translator.
     */
    void setTranslator(TranslatorInterface translator) {
        this.translator = translator;
        translator.open(getOpenSuite());
    }

    /**
     * Gets the translator that is to be used to locate, load and convert
     * classes that are not currently installed in this isolate's runtime
     * environment.
     *
     * @return  the translator for installing new classes
     */
    public TranslatorInterface getTranslator() {
        if (translator == null) {
            /*
             * Create the translator instance reflectively. This (compile and runtime) dynamic
             * binding to the translator means that it can be an optional component. This allows
             * it to be optimized by BCO as an application as opposed to as a library.
             */
            try {
                Klass klass = bootstrapSuite.lookup("com.sun.squawk.translator.Translator");
                if (klass == null) {
                    return null;
                }
                setTranslator((TranslatorInterface)klass.newInstance());
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            }
        }
        Assert.that(translator != null);
        return translator;
    }

    /**
     * Adds a named property to this isolate. These properties are included in the
     * look up performed by {@link System#getProperty(String)}.
     *
     * @param key    the name of the property
     * @param value  the value of the property
     */
    public void setProperty(String key, String value) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(key, value);
    }

    /**
     * Gets a named property of this isolate.
     *
     * @param key  the name of the property to get
     * @return the value of the property named by 'key' or null if there is no such property
     */
    public String getProperty(String key) {
        if (properties == null) {
            return null;
        }
        return (String)properties.get(key);
    }

    /*---------------------------------------------------------------------------*\
     *                           Class state management                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Get a class state.
     *
     * @param klass the class of the variable
     * @return the class state object or null if none exists
     */
    Object getClassState(Klass klass) {
        VM.extendsEnabled = false;
        Object first = classStateQueue;
        Object res = null;

        if (first != null) {
            /*
             * Do quick test for class state at the head of the queue.
             */
            if (Unsafe.getObject(first, CS.klass) == klass) {
                res = first;
            } else {
                /*
                 * Start searching.
                 */
                Object last = first;
                Object ks = Unsafe.getObject(first, CS.next);
                while (ks != null) {
                    if (Unsafe.getObject(ks, CS.klass) == klass) {
                        /*
                         * Move to head of queue.
                         */
                        if (last != null) {
                            Object ksnext = Unsafe.getObject(ks, CS.next);
                            Unsafe.setObject(last, CS.next, ksnext);
                            Unsafe.setObject(ks, CS.next, first);
                            classStateQueue = ks;
                        }
                        res = ks;
                        break;
                    }
                    last = ks;
                    ks = Unsafe.getObject(ks, CS.next);
                }
            }
        }
        VM.extendsEnabled = true;
        VM.addToClassStateCache(klass, res);
        return res;
    }

    /**
     * Add a class state to the system.
     *
     * @param ks the class state to add
     */
    void addClassState(Object ks) {
        VM.extendsEnabled = false;
        Object first = classStateQueue;
        Unsafe.setObject(ks, CS.next, first);
        classStateQueue = ks;
        VM.extendsEnabled = true;
    }

    /**
     * Get a class state in order to access a static variable.
     *
     * @param klass the class of the variable
     * @param offset the offset to the variable
     * @return the class state object
     */
    Object getClassStateForStaticVariableAccess(Klass klass, int offset) {
        /*
         * Lookup the class state in the isolate.
         */
        Object ks = getClassState(klass);

        /*
         * If the class state was not found in the list, then the class
         * is either not initialized, has suffered an initialization
         * failure, or is in the process of being initialized. In either
         * case calling initializeInternal() will either yield a pointer to the
         * class state object or result in an exception being thrown.
         */
        if (ks == null) {
            Assert.that(klass.getClassID() != CID.CLASS);
            ks = klass.initializeInternal();
        }

        /*
         * Check a few thing and return the class state record.
         */
        Assert.that(ks != null);
        Assert.that(offset >= CS.firstVariable);
        Assert.that(offset < GC.getArrayLength(ks));
        return ks;
    }


    /*---------------------------------------------------------------------------*\
     *                           String interning                                *
    \*---------------------------------------------------------------------------*/

    /**
     * Returns a canonical representation for the string object.
     * <p>
     * A pool of strings, initially empty, is maintained privately by the
     * class <code>Isolate</code>.
     * <p>
     * When the intern method is invoked, if the pool already contains a
     * string equal to this <code>String</code> object as determined by
     * the {@link #equals(Object)} method, then the string from the pool is
     * returned. Otherwise, this <code>String</code> object is added to the
     * pool and a reference to this <code>String</code> object is returned.
     * <p>
     * It follows that for any two strings <code>s</code> and <code>t</code>,
     * <code>s.intern() == t.intern()</code> is <code>true</code>
     * if and only if <code>s.equals(t)</code> is <code>true</code>.
     * <p>
     * All literal strings and string-valued constant expressions are
     * interned. String literals are defined in &sect;3.10.5 of the
     * <a href="http://java.sun.com/docs/books/jls/html/">Java Language
     * Specification</a>
     *
     * @return  a string that has the same contents as this string, but is
     *          guaranteed to be from a pool of unique strings.
     */
    public String intern(String value) {
        if (internedStrings == null) {
            internedStrings = new Hashtable();
            if (!VM.isHosted()) {
                GC.getStrings(internedStrings);
            }
        }
        String internedString = (String)internedStrings.get(value);
        if (internedString == null) {
            if (internedString == null) {
                internedStrings.put(value, value);
                internedString = value;
            }
        }
        return internedString;
    }


    /*---------------------------------------------------------------------------*\
     *                            Isolate Execution                              *
    \*---------------------------------------------------------------------------*/

    /**
     * Start the isolate.
     */
    public void primitiveThreadStart() {
        new Thread(this).primitiveThreadStart();
    }

    /**
     * Start the isolate.
     */
    public void start() {
        new Thread(this).start();
    }

    /**
     * Manually initialize java.lang.Klass.
     */
    void initializeClassKlass() {
        if (!classKlassInitialized) {
            Klass klassKlass       = bootstrapSuite.getKlass(CID.CLASS);
            Klass klassGlobalArray = bootstrapSuite.getKlass(CID.GLOBAL_ARRAY);
            Object cs = GC.newClassState(klassKlass, klassGlobalArray);
            addClassState(cs);
            klassKlass.clinit();
            classKlassInitialized = true;
//VM.print("Klass initialized for ");
//VM.println(this);
        }
    }

    /**
     * Get the Channel I/O handle.
     *
     * @return the I/O handle
     */
    int getChannelContext() {
        if (channelContext == 0 /*&& hibernated == false*/) {
            channelContext = VM.createChannelContext(hibernatedContext);
            hibernatedContext = 0;
        }
        return channelContext;
    }

    /**
     * Get the GUI input channel.
     *
     * @return the I/O handle
     */
    int getGuiInputChannel() throws IOException {
        if (guiIn == 0) {
            guiIn = VM.getChannel(ChannelConstants.CHANNEL_GUIIN);
        }
        return guiIn;
    }

    /**
     * Get the GUI output channel.
     *
     * @return the I/O handle
     */
    int getGuiOutputChannel() throws IOException {
        if (guiOut == 0) {
            guiOut = VM.getChannel(ChannelConstants.CHANNEL_GUIOUT);
            VM.execGraphicsIO(ChannelConstants.SETWINDOWNAME, 0, 0, 0, 0, 0, 0, mainClassName, null);
        }
        return guiOut;
    }

    /**
     * Create an open suite if none was specified.
     * <p>
     * Note this has the useful side effect of initializing the SuiteManager.
     * This is needed for the code in VM.do_throw() which cannot initialize the
     * SuiteManager because allocation is disabled in this routine.
     */
    private void initializeSuiteManager() {
        Assert.that(VM.getCurrentIsolate() == this);
        if (openSuite == null) {
            SuiteManager.initialize(new Suite[] { bootstrapSuite });
            Suite[] suiteTable;
            Suite parent;

            if (parentSuiteSourceURL == null) {
                suiteTable = new Suite[] { bootstrapSuite };
                parent = bootstrapSuite;
            } else {
                ObjectMemory om = ObjectMemoryLoader.load(parentSuiteSourceURL, true);

                parent = null;
                suiteTable = new Suite[om.getParentCount() + 1];
                while (om != null) {
                    Object root = om.getRoot();
                    if (!(root instanceof Suite)) {
                        throw new LinkageError("object memory in '" + om.getURL() + "' does not contain a suite");
                    }
                    Suite suite = (Suite)root;

                    int sno = suite.getSuiteNumber();
                    if (sno >= suiteTable.length) {
                        throw new LinkageError("invalid suite number for '" + suite.getName() + "' suite");
                    }
                    if (suiteTable[sno] != null) {
                        throw new LinkageError("suite '" + suite.getName() + "' has the same suite number as suite '" + suiteTable[sno].getName() + "'");
                    }
                    suiteTable[sno] = suite;

                    if (parent == null) {
                        parent = suite;
                    }

                    om = om.getParent();
                }
            }

            Assert.that(parent == suiteTable[suiteTable.length - 1]);
            SuiteManager.initialize(suiteTable);

            Assert.that(parent != null);

if (openSuiteHack != null) {
    openSuite = openSuiteHack;
    SuiteManager.allocateFreeSuiteNumber();
} else {
            openSuite = new Suite("-open" + VM.getNextHashcode() + "-", SuiteManager.allocateFreeSuiteNumber(), parent, classPath);
}
            SuiteManager.installSuite(openSuite);
        } else {
            Assert.always(openSuite == bootstrapSuite);
            Suite[] suiteTable = new Suite[] { bootstrapSuite };
            SuiteManager.initialize(suiteTable);
        }
    }

public void forceOpenSuite(Suite openSuiteHack) {
    this.openSuiteHack = openSuiteHack;
}
Suite openSuiteHack;




    /**
     * Start running the isolate.
     */
    public void run() {

        if (state != NEW) {
            throw new IllegalStateException("cannot restart isolate");
        }

        changeState(ALIVE);

        Klass klass = null;

        /*
         * Manually initialize java.lang.Klass.
         */
        initializeClassKlass();

        /*
         * Create the 'open' suite if one was not supplied.
         */
        initializeSuiteManager();

        /*
         * It is important that java.lang.System is initialized before com.sun.cldc.i18n.Helper
         * so initialized it now.
         */
        System.currentTimeMillis();

        /*
         * Verbose trace.
         */
        if (VM.isVeryVerbose()) {
            System.out.print("[Starting isolate for '" +mainClassName + "' with class path set to '" + classPath +"'");
            if (parentSuiteSourceURL != null) {
                System.out.print(" and parent suite URL set to '" + parentSuiteSourceURL + "'");
            }
            if (openSuite != null) {
                System.out.print(" and open suite '" + openSuite + "'");
            }
            System.out.println("]");
        }

        /*
         * Find the main class and call it's main().
         */
        try {
            klass = Klass.forName(mainClassName);
        } catch (ClassNotFoundException ex) {
            System.err.println("No such class " + mainClassName + ": " + ex);
            exit(999);
        }

        klass.main(args);

        System.out.flush();
        System.err.flush();

        //} catch (OutOfMemoryError ex) {
        //    VM.println("Uncaught out of memory error executing isolate");
        //    exit(999);
        //} catch (Throwable ex) {
        //    ex.printStackTrace();
        //    System.err.println("Error executing isolate "+ex);
        //}
    }

    /**
     * Wait for all the other threads and child isolates belonging to this isolate to stop.
     */
    public void join() {

        /*
         * If this isolate is has not yet been started or is still alive then wait until it has exited or been hibernated
         */
        if (state <= ALIVE) {
            Thread.isolateJoin(this);
        }

        /*
         * Join all the child isolates.
         */
        if (childIsolates != null) {
            for (Enumeration e = childIsolates.elements() ; e.hasMoreElements() ;) {
                Isolate isolate = (Isolate)e.nextElement();
                isolate.join();
            }
        }

        /*
         * Eliminate child isolates from the isolate object graph.
         */
        childIsolates = null;

        /*
         * Remove this isolate from its parent's list of children. The parentIsolate pointer
         * will be null for the bootstrap isolate as well as for unhibernated isolates
         */
        if (parentIsolate != null) {
            parentIsolate.childIsolates.remove(this);
            parentIsolate = null;
        }
    }

    /**
     * Add a child isolate to this isolate.
     *
     * @param childIsolate  the child isolate
     */
    void addIsolate(Isolate childIsolate) {
        if (childIsolates == null) {
            childIsolates = new Hashtable();
        }
        Assert.that(!childIsolates.containsKey(childIsolate));
        childIsolates.put(childIsolate, childIsolate);
    }

    /**
     * Add a thread to the isolate.
     *
     * @param thread the thread
     */
    void addThread(Thread thread) {
        Assert.that(!childThreads.containsKey(thread));
        childThreads.put(thread, thread);
    }

    /**
     * Remove a thread from the isolate.
     *
     * @param thread the thread
     */
    void removeThread(Thread thread) {
        Assert.that(childThreads.containsKey(thread));
        childThreads.remove(thread);

        /*
         * Check for rundown condition.
         */
        for (Enumeration e = childThreads.elements(); e.hasMoreElements(); ) {
            thread = (Thread)e.nextElement();
            if (!thread.isDaemon()) {
                return;
            }
        }

        /*
         * If all the non-daemon threads are dead then stop the isolate.
         */
        abort(0);
    }

    /**
     * Test to see if class Klass is initialized.
     *
     * @return true if it is
     */
    boolean isClassKlassInitialized() {
        return classKlassInitialized;
    }

    /**
     * Stop the isolate.
     *
     * @param code the exit code
     */
    public void exit(int code) {
        if (state != ALIVE) {
            throw new IllegalStateException("cannot re-exit an isolate: state=" + state);
        }
        abort(code);
    }

    /**
     * Stop the isolate.
     *
     * @param code the exit code
     */
    void abort(int code) {
        if (state == ALIVE) {
            exitCode = code;
        }
        try {
            hibernate(false, EXITED);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.shouldNotReachHere();
        }
    }

    /**
     * Serializes and saves to a file the object graph rooted by this hibernated isolate.
     *
     * @return the URL to which the isolate was saved
     * @throws IOException if there was insufficient memory to do the save or there was
     *         some other IO problem while writing the file.
     */
    public String save() throws java.io.IOException {
        Assert.that(state == HIBERNATED);

        // Null out the interned string cache as it will be rebuilt on demand
        internedStrings = null;

        ObjectMemorySerializer.ControlBlock cb = VM.copyObjectGraph(this);
        if (cb == null) {
            throw new java.io.IOException("insufficient memory for object graph copying");
        }
        String url = "file://" + hibernatedContext+ ".isolate";

        Suite readOnlySuite = openSuite;
        while (GC.inRam(readOnlySuite)) {
            readOnlySuite = readOnlySuite.getParent();
        }

        ObjectMemorySerializer.save(url, cb, GC.lookupObjectMemoryByRoot(readOnlySuite));
        return url;
    }

    /**
     * Loads a serialized isolate from a URL into RAM. It is up to the caller to unhibernate the isolate.
     *
     * @param url the source URL to load from
     */
    public static Isolate load(String url) {
        ObjectMemory om = ObjectMemoryLoader.load(url, false);
        Object root = om.getRoot();
        if (!(root instanceof Isolate)) {
            throw new LinkageError("object memory in '" + om.getURL() + "' does not contain an isolate");
        }

        if (VM.isVerbose()) {
            int old = VM.setStream(VM.STREAM_SYMBOLS);
            VM.print("UNHIBERNATED_ISOLATE.RELOCATION=");
            VM.printOffset(om.getStart().toUWord().toOffset());
            VM.println();
            VM.setStream(old);
        }

        return (Isolate)root;
    }

    /**
     * Hibernate the isolate. If the current thread is in this isolate then
     * this function will only return when the isolate is unhibernated.
     *
     * @throws IOException if the underlying IO system cannot be serialized
     */
    public void hibernate() throws java.io.IOException {
        if (state >= HIBERNATED) {
            throw new IllegalStateException("cannot hibernate a hibernated or exited isolate");
        }
        hibernate(true, HIBERNATED);
    }

    /**
     * Modifies the state of this isolate.
     *
     * @param newState  the state to which the current state should transition
     */
    private void changeState(int newState) {
        this.state = newState;
    }

    /**
     * Hibernate the isolate. If the current thread is in this isolate then
     * this function will only return when the isolate is unhibernated.
     *
     * @param  hibernateIO  if true, the underlying IO system is also serialized. Only an
     *                      isolate with a hibernated IO system can be {@link #unhibernate() resumed}
     * @param  newState    the state that this isolate should be put into once this method completes
     * @throws IOException if the underlying IO system cannot be serialized
     */
    private void hibernate(boolean hibernateIO, int newState) throws java.io.IOException {
/*if[CHUNKY_STACKS]*/
/*else[CHUNKY_STACKS]*/
//      if (hibernateIO) {
//          VM.println("Hibernation not supported");
//          hibernateIO = false;
//      }
/*end[CHUNKY_STACKS]*/
        if (hibernateIO && VM.isVeryVerbose()) {
            System.out.print("[Hibernating isolate for '" +mainClassName + "' with class path set to '" + classPath +"'");
            if (parentSuiteSourceURL != null) {
                System.out.print(" and parent suite URL set to '" + parentSuiteSourceURL + "'");
            }
            if (openSuite != null) {
                System.out.print(" and open suite '" + openSuite + "'");
            }
            System.out.println("]");
        }
        if (state != newState) {

            changeState(newState);

            /*
             * Serialize the underlying context if this is not an exiting isolate
             */
            int channelContext = getChannelContext();
            if (hibernateIO && channelContext > 0) {
                hibernatedContext = VM.hibernateChannelContext(channelContext);
                if (hibernatedContext < 0) {
                    throw new IOException("Error hibernating channel context: "+hibernatedContext);
                }

            }

            /*
             * Close the channel I/O
             */
            if (channelContext > 0) {
                VM.deleteChannelContext(channelContext);
                this.channelContext = 0;
            }

            /*
             * Hibernate all the executing threads.
             */
            Thread.hibernateIsolate(this);
        }
    }


    /*
     * Add a thread to the list of hibernated run threads.
     *
     * @param thread the thread to add
     */
    void addToHibernatedRunThread(Thread thread) {
        Assert.that(thread.nextThread == null);
        thread.nextThread = hibernatedRunThreads;
        hibernatedRunThreads = thread;
    }

    /*
     * Add a thread to the list of hibernated timer threads.
     *
     * @param thread the thread to add
     */
    void addToHibernatedTimerThread(Thread thread) {
        Assert.that(thread.nextTimerThread == null);
        thread.nextTimerThread = hibernatedTimerThreads;
        hibernatedTimerThreads = thread;
    }

    /**
     * Unhibernate the isolate.
     */
    public void unhibernate() {
/*if[CHUNKY_STACKS]*/
        if (state != HIBERNATED) {
            throw new RuntimeException("Cannot unhibernate isolate that is not in hibernation state");
        }
        changeState(ALIVE);
        Isolate parent = VM.getCurrentIsolate();
        Assert.that(parent != null);
        parent.addIsolate(this);
        Thread.unhibernateIsolate(this);
/*else[CHUNKY_STACKS]*/
//      throw new RuntimeException("Hibernation not supported");
/*end[CHUNKY_STACKS]*/
    }

    /**
     * Sets the list of stack chunks owned by this hibernating isolate.
     *
     * @param sc  the list of stack chunks owned by this isolate
     */
    void setHibernatedStackChunks(Object sc) {
        Assert.that(hibernatedStackChunks == null);
        hibernatedStackChunks = sc;
    }

    /**
     * Gets the set of stack chunks owned by this unhibernating isolate and
     * nulls the pointer in the isolate to these chunks.
     *
     * @return  the set of stack chunks owned by this isolate
     */
    Object takeHibernatedStackChunks() {
        Object sc = hibernatedStackChunks;
        Assert.that(sc != null);
        hibernatedStackChunks = null;
        return sc;
    }

    /*
     * Get all the hibernated run threads.
     *
     * @return the thread linked by thread.nextThread
     */
    Thread getHibernatedRunThreads() {
        Thread res = hibernatedRunThreads;
        hibernatedRunThreads = null;
        return res;
    }

    /*
     * Get all the hibernated timer threads.
     *
     * @return the thread linked by thread.nextTimerThread
     */
    Thread getHibernatedTimerThreads() {
        Thread res = hibernatedTimerThreads;
        hibernatedTimerThreads = null;
        return res;
    }

    /**
     * Test to see if this isolate is {@link #hibernate() hibernated}.
     *
     * @return true if it is
     */
    public boolean isHibernated() {
        return state == HIBERNATED;
    }

    /**
     * Test to see if this isolate has been (re)started and not yet (re)hibernated or exited.
     *
     * @return true if it is
     */
    public boolean isAlive() {
        return state == ALIVE;
    }

    /**
     * Test to see if this isolate is {@link #exit exited}.
     *
     * @return true if it is
     */
    public boolean isExited() {
        return state == EXITED;
    }

    /**
     * Get the isolate exit code.
     *
     * @return the exit code
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Add a finalizer.
     *
     * @param finalizer the finalizer
     */
    void addFinalizer(Finalizer finalizer) {
        finalizer.setNext(finalizers);
        finalizers = finalizer;
    }

    /**
     * Remove a finalizer.
     *
     * @return the finalizer or null if there are none.
     */
    Finalizer removeFinalizer() {
        Finalizer finalizer = finalizers;
        if (finalizer != null) {
            finalizers = finalizer.getNext();
            finalizer.setNext(null);
        }
        return finalizer;
    }

    /**
     * Add a thread to the list of threads waiting for the isolate to finish.
     *
     * @param thread the thread
     */
    void addJoiner(Thread thread) {
        thread.nextThread = joiners;
        joiners = thread;
    }

    /**
     * Get all the joining threads.
     *
     * @return all the threads
     */
    Thread getJoiners() {
        Thread res = joiners;
        joiners = null;
        return res;
    }

    /**
     * Get the string representation of the isolate.
     *
     * @return the string
     */
    public String toString() {
        String res = "isolate \"".concat(mainClassName).concat("\"");
        if (isAlive()) {
            res = res.concat(" (ALIVE)");
        } else if (isExited()) {
            res = res.concat(" (EXITED)");
        } else if (isHibernated()) {
            res = res.concat(" (HIBERNATED)");
        } else {
            res = res.concat(" (NEW)");
        }
        return res;
    }
}


//VM.print("\n\nIsolate ");
//VM.print(mainClassName);
//VM.print(" -- addClassState ");
//VM.println(VM.asKlass(CS_getKlass(ks)).getInternalName());


//void dumpAll() {
//
//    VM.print("Class states for Isolate ");
//    VM.println(mainClassName);
//
//    Object ks = classStateQueue;
//    while (ks != null) {
//
//        VM.print("     ");
//        VM.println(VM.asKlass(CS_getKlass(ks)).getInternalName());
//
//        ks = CS_getNext(ks);
//    }
//
//}
