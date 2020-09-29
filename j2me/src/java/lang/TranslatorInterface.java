/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

/**
 * The <code>TranslatorInterface</code> is the interface by which new
 * classes can be created and loaded into the runtime system.<p>
 *
 * The runtime system (per isolate) can have at most one open connection with
 * a translator (i.e. an object that implements this interface). The
 * correct usage of a translator is described by the following state
 * transistion machine:
 * <p><blockquote><pre>
 *
 *             +----------- open() ----------+     +---------+
 *             |                             |     |         |
 *             |                             V     V         |
 *        +--------+                       +---------+       |
 *   ---> | CLOSED |                       |  OPEN   |   findClass() / load() / convert()
 *        +--------+                       +---------+       |
 *             ^                             |     |         |
 *             |                             |     |         |
 *             +---------- close() ----------+     +---------+
 *
 * </pre></blockquote><p>
 *
 * That is, a translator can be {@link #open(Suite) opened} and then have any
 * number of {@link #findClass(String, int, boolean) findClass},
 * {@link #load(Klass) load} and {@link #convert(Klass) convert} operations
 * performed on it before being {@link #close() closed}.<p>
 *
 * @author  Doug Simon
 */
public interface TranslatorInterface {

    /**
     * Opens a connection with the translator to load & create classes in
     * the context of a given suite.
     *
     * @param  suite  the suite in which classes created during the connection
     *                with this translator will be installed. This suite must
     *                already be installed in the {@link SuiteManager}
     */
    public void open(Suite suite);

    /**
     * Gets a class corresponding to a given name. If the class does not
     * exist, it will be created and installed in the current suite. If the
     * class represents an array, then this method also ensures that the
     * class representing the component type of the array also exists.<p>
     *
     * The class will not be created if it already exists in the current
     * suite or any of the suites the current suite binds to.
     *
     * @param   name       the name of the class to get
     * @param   classID    the unique identifier to use when creating the
     *                     class or -1 if the next available unique identifier
     *                     is to be used
     * @param   enforceValidName if true and <code>name</code> is not a valid class name then
     *          null is returned
     * @return the klass corresponding to <code>name</code> or null if <code>name</code>
     *          is not a valid class name and <code>enforceValidName</code> is true
     */
    public Klass findClass(String name, int classID, boolean enforceValidName);

    /**
     * Ensures that a given class has had its definition initialized, loading
     * it from a class file if necessary. This does not include verifying the
     * bytecodes for the methods (if any) and converting them to Squawk
     * bytecodes.
     *
     * @param   klass  the klass whose definition must be initialized
     */
    public void load(Klass klass);

    /**
     * Ensures that all the methods (if any) in a given class have been verified
     * and converted to Squawk bytecodes.
     *
     * @param   klass  the klass whose methods are to be verified and converted
     */
    public void convert(Klass klass);

    /**
     * Loads and converts the closure of classes in the open suite.
     */
    public void computeClosure();

    /**
     * Closes the connection with the translator. This computes the closure
     * of the classes in the current suite and ensures they are all loaded and
     * converted.
     */
    public void close();
}
