/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.vm.CID;
import com.sun.squawk.util.Assert;

/**
 * A suite is the unit of deployment/compilation in the Squawk system.
 */
public final class Suite {

    /*---------------------------------------------------------------------------*\
     *                            Fields and constructor                         *
    \*---------------------------------------------------------------------------*/

    /**
     * The unique identifier for the suite.
     */
    private final int sno;

    /**
     * The classes in the suite.
     */
    private Klass[] classes;

    /**
     * The name of the suite.
     */
    private /*final*/ String name;

    /**
     * The class path the suite was constructed with.
     */
    private final String classPath;

    /**
     * The array of metadata objects for the classes in this suite. The
     * metadata object for the class at index <i>idx</i> in the
     * <code>classes</code> array is at index <i>idx</i> in the
     * <code>metadata</code> array.
     */
    private KlassMetadata[] metadatas;

    /**
     * The suite that this suite is bound against. That is, the classes of this
     * suite reference classes in the parent suite and its parents.
     */
    private final Suite parent;

    /**
     * Count of references to this suite from other suites whose classes
     * link to the classes of this suite.
     */
    private int childCount;

    /**
     * Specifies whether or not this suite is open. Only an open suite can have
     * classes installed in it.
     */
    private boolean closed;

    /**
     * Creates a new <code>Suite</code> instance.
     *
     * @param  name        the name of the suite
     * @param  sno         the unique identifier for the suite
     * @param  parent      suite whose classes are linked to by the classes of this suite
     * @param  classPath   the class path the suite was constructed with
     */
    Suite(String name, int sno, Suite parent, String classPath) {
        this.name = name;
        this.sno = sno;
        this.parent = parent;
        int count = (sno == 0 ? CID.LAST_CLASS_ID + 1 : 0);
        classes = new Klass[count];
        metadatas = new KlassMetadata[count];
        this.classPath = classPath;
    }

    /**
     * Constructor for the RESERVED <code>Suite</code> instance in SuiteManager.
     */
    Suite() {
        sno = -1;
        name = "-invalid-";
        classPath = null;
        parent = null;
    }


    /*---------------------------------------------------------------------------*\
     *                                  Getters                                  *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the identifier assigned to this suite.
     *
     * @return  the identifier assigned to this suite
     */
    public int getSuiteNumber() {
        return sno;
    }

    /**
     * Gets this suite's name.
     *
     * @return  this suite's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the class path used to load the classes in the suite.
     *
     * @return the class path used to load the classes in the suite
     */
    public String getClassPath() {
        return classPath;
    }

    /**
     * Gets the URL from which this suite was loaded.
     *
     * @return the URL from which this suite was loaded or null if the suite was dynamically created
     */
    public String getURL() {
        ObjectMemory om = GC.lookupObjectMemoryByRoot(this);
        if (om != null) {
            return om.getURL();
        } else {
            return null;
        }
    }

    /**
     * Gets the number of classes in this suite.
     *
     * @return the number of classes in this suite
     */
    public int getClassCount() {
        return classes.length;
    }

    /**
     * Determines if this suite is closed. Open an open suite can have classes installed in it.
     *
     * @return boolean
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Gets the next available number for a class that will be installed in this suite.
     * The value returned by this method will never be the same for this suite.
     *
     * @return the next available number for a class that will be installed in this suite
     */
    public int getNextAvailableClassNumber() {
        return getClassCount();
    }

    /**
     * Gets the class in this suite corresponding to a given class number.
     *
     * @param   cno  the class number of the class to retrieve
     * @return  the class corresponding to <code>cno</code>
     */
    public Klass getKlass(int cno) {
        Assert.that(cno < classes.length);
        return classes[cno];
    }

    /**
     * Determines if this suite can have more classes loaded into it. This
     * is only possible if no other suite references this suite.
     *
     * @return  true if there is no reference to this suite from another suite
     */
    public boolean isReplaceable() {
        return childCount == 0;
    }

    /**
     * Gets a string representation of this suite. The string returned is
     * name of this suite with "suite " prepended.
     *
     * @return  the name of this suite with "suite " prepended
     */
    public String toString() {
        return "suite " + name;
    }

    /*---------------------------------------------------------------------------*\
     *                            Name updating                                  *
    \*---------------------------------------------------------------------------*/

    /**
     * Updates the name of this suite. This can only be done before there are
     * any classes installed in the suite.
     *
     * @param  name  the new suite name
     */
    void updateName(String name) {
        if (classes.length != 0) {
            throw new IllegalStateException("cannot change the name of a non-empty suite");
        }
        this.name = name;
    }

    /*---------------------------------------------------------------------------*\
     *                            Class installation                             *
    \*---------------------------------------------------------------------------*/

    /**
     * Installs a given class into this suite. The
     * {@link Klass#getClassID() identifier} for the class must not conflict
     * conflict with the identifier for any class already installed in this
     * suite.
     *
     * @param klass  the class to install
     */
    public void installClass(Klass klass) {
        checkWrite();
        int cno = Klass.toClassNumber(klass.getClassID());
        if (cno < classes.length) {
            Assert.that(classes[cno] == null, klass + " already installed");
        } else {
            Klass[] old = classes;
            classes = new Klass[cno + 1];
            System.arraycopy(old, 0, classes, 0, old.length);
        }
        classes[cno] = klass;
    }

    /**
     * Installs the metadata for a class into this suite. This class to which
     * the metadata pertains must already have been installed and there must
     * be no metadata currently installed for the class.
     *
     * @param metadata  the metadata to install
     */
    public void installMetadata(KlassMetadata metadata) {
        checkWrite();
        Klass klass = metadata.getDefinedClass();
        int cno = Klass.toClassNumber(klass.getClassID());
        Assert.that(cno < classes.length && classes[cno] != null, klass + " not yet installed");
        if (cno < metadatas.length) {
            Assert.that(metadatas[cno] == null, "metadata for " + klass + "already installed");
        } else {
            KlassMetadata[] old = metadatas;
            metadatas = new KlassMetadata[cno + 1];
            System.arraycopy(old, 0, metadatas, 0, old.length);
        }
        metadatas[cno] = metadata;
    }

    /**
     * Delete the metadata for the class.
     */
     void deleteMetadata() {
         checkWrite();
         metadatas = null;
     }

    /*---------------------------------------------------------------------------*\
     *                              Class lookup                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the <code>KlassMetadata</code> instance from this suite
     * corresponding to a specified class number.
     *
     * @param    cno  a 16-bit class number
     * @return   the <code>KlassMetadata</code> instance corresponding to
     *                <code>cno</code> or <code>null</code> if there isn't one
     */
    public KlassMetadata getMetadata(int cno) {
        if (metadatas == null || cno < 0 || cno >= metadatas.length) {
            return null;
        }
        return metadatas[cno];
    }

    /**
     * Gets the <code>Klass</code> instance from this suite corresponding
     * to a specified class name in internal form.
     *
     * @param   name     the name (in internal form) of the class to lookup
     * @return  the <code>Klass</code> instance corresponding to
     *                   <code>internalName</code> or <code>null</code> if there
     *                   isn't one.
     */
    public Klass lookup(String name) {
        /*
         * look in parent suites first
         */
        if (sno != 0) {
            Klass klass = parent.lookup(name);
            if (klass != null) {
                return klass;
            }
        }

        for (int i = 0 ; i < classes.length ; i++) {
            Klass klass = classes[i];
            if (klass != null) {
                if (klass.getInternalName().compareTo(name) == 0) { // bootstrapping issues prevent the use of equals()
                    return klass;
                }
            }
        }
        return null;
    }

    /**
     * Ensures that this suite is not in read-only memory before being updated.
     *
     * @throws IllegalStateException if this suite is closed
     * @throws IllegalStoreException if this suite is in read-only memory
     */
    private void checkWrite() {
        if (closed) {
            throw new IllegalStateException(this + " is closed");
        }
        if (!GC.inRam(this)) {
            throw new IllegalStoreException("trying to update read-only object: " + this);
        }
    }

    /*---------------------------------------------------------------------------*\
     *                                 Removal                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * Adjust the reference count of this suite by a specified value.
     *
     * @param delta the adjustment value.
     */
    void adjustChildCount(int delta) {
        if (GC.inRam(this)) {
            childCount += delta; // Potentially
        }
    }

    /**
     * Gets the direct parent suite of this suite.
     *
     * @return the direct parent suite of this suite
     */
    public Suite getParent() {
        return parent;
    }

    /**
     * Attempt to remove this suite from the system. This will only be successful
     * if there are no references to this suite from another suite. If the suite
     * is actually removed, then the references counts of all other suites
     * referenced by this one are decremented by one.
     *
     * @return whether or not the suite was removed.
     */
    boolean remove() {
        if (childCount == 0) {
            Assert.that(parent != null);
            SuiteManager.adjustChildren(parent, -1);
            return true;
        } else {
            return false;
        }
    }


    /*---------------------------------------------------------------------------*\
     *                            hashcode & equals                              *
    \*---------------------------------------------------------------------------*/

    /**
     * Compares this suite with another object for equality. The result is true
     * if and only if <code>other</code> is a <code>Suite</code> instance
     * and its name is equal to this suite's name.
     *
     * @param   other   the object to compare this suite against
     * @return  true if <code>other</code> is a <code>Suite</code> instance
     *                  and its name is equal to this suite's name
     */
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Suite) {
            return name.equals(((Suite)other).name);
        }
        return false;
    }

    /**
     * Returns a hashcode for this suite which is derived solely from the
     * suite's name.
     *
     * @return  the hashcode of this suite's name
     */
    public final int hashCode() {
        return name.hashCode();
    }

    /*---------------------------------------------------------------------------*\
     *                            Persistence                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Describes the configuration of the suite.
     */
    private String configuration;

    /**
     * Gets the configuration of the suite.
     *
     * @return the configuration of the suite
     */
    public String getConfiguration() {
        if (configuration == null) {
            return "complete symbolic information available";
        }
        return configuration;
    }

    /**
     * Serializes and saves to a file the object graph rooted by this suite.
     *
     * @return the URL to which the suite was saved
     * @throws IOException if there was insufficient memory to do the save or there was
     *         some other IO problem while writing the file.
     */
    public String save() throws java.io.IOException {
        ObjectMemorySerializer.ControlBlock cb = VM.copyObjectGraph(this);
        if (cb == null) {
            throw new java.io.IOException("insufficient memory for object graph copying");
        }
        String url = "file://" + name + ".suite";
        ObjectMemorySerializer.save(url, cb, GC.lookupObjectMemoryByRoot(parent));
        return url;
    }

    /**
     * Denotes a suite that encapsulates an application. The classes of an application
     * can not be linked against.
     */
    public static final int APPLICATION = 0;

    /**
     * Denotes a suite that encapsulates a library. The classes of a library
     * can be linked against but the library itself cannot be extended by virtue
     * of other classes linking against it's package private components.
     */
    public static final int LIBRARY = 1;

    /**
     * Denotes a suite that encapsulates an open library. The classes of an open library
     * can be linked against and the library itself can be extended by virtue
     * of other classes linking against it's package private components.
     */
    public static final int EXTENDABLE_LIBRARY = 2;

    /**
     * Denotes a suite that is being debugged. This suite retains all its symbolic information
     * when closed.
     */
    public static final int DEBUG = 3;

    /**
     * Closes this suite. Once closed, a suite is immutable (and may well reside in
     * read-only memory) and cannot have any more classes installed in it
     *
     * @param type  specifies the type of the suite after closing. Must be
     *              {@link #APPLICATION}, {@link #LIBRARY}, {@link #EXTENDABLE_LIBRARY} or {@link #DEBUG}.
     * @param retainLNTs if true and the classes currently have line number tables
     *              as derived from a class file LineNumberTable attribute, then these tables
     *              preserved. Exception stack traces can only express source file positions
     *              if a class has it's line number table preserved.
     * @param retainLVTs if true and the classes currently have local variable tables
     *              as derived from a class file LineNumberTable attribute, then these tables
     *              preserved
     */
    public void close(int type, boolean retainLNTs, boolean retainLVTs) {
        if (type < APPLICATION || type > DEBUG) {
            throw new IllegalArgumentException();
        }
        metadatas = KlassMetadata.prune(this, metadatas, type, retainLNTs, retainLVTs);
        updateConfiguration(type, retainLNTs, retainLVTs);
        closed = true;
    }

    /**
     * Updates the configuration description of this suite based on the parameters that it
     * is {@link #close(int, boolean, boolean) closed} with.
     */
    void updateConfiguration(int type, boolean retainLNTs, boolean retainLVTs) {
        if (type == DEBUG) {
            configuration = "symbols not pruned";
        } else {
            configuration = "symbols pruned in ";
            switch (type) {
                case APPLICATION: configuration += "application"; break;
                case LIBRARY: configuration += "library"; break;
                case EXTENDABLE_LIBRARY: configuration += "extendable library"; break;
            }
            configuration += " mode";
        }
        configuration += ", source line number info " + (retainLNTs ? "available" : "discarded");
        configuration += ", source local variable info " + (retainLVTs ? "available" : "discarded");
    }
}
