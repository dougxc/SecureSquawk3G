/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import com.sun.squawk.util.Assert;
import com.sun.squawk.util.ArrayHashtable;

/**
 * This class provides a number of static methods that comprise the interface
 * to the suites of classes loaded in the system.
 */
public final class SuiteManager {

    /**
     * A sentinel object that occupies a slot in a suite table whose slot
     * index has been allocated for a suite currently being loaded.
     */
    private static Suite RESERVED;

    /**
     * The list of suites.
     */
    private static Suite[] suites;

    /**
     * Initialize the suite manager.
     *
     * @param suites initial list of suites
     */
    static void initialize(Suite[] suites) {
        RESERVED = new Suite();
        SuiteManager.suites = suites;
    }

    /**
     * Ensures the suite table (and its transitive closure of objects) is in NVM or ROM.
     *
     * @return  the address of the suite table in NVM or ROM
     */
    static Suite[] persistSuiteTable() {
        ArrayHashtable saved = new ArrayHashtable();
//        suites = (Suite[])NonvolatileMemory.save(suites, saved);
        return suites;
    }

    /**
     * Get the suite table.
     *
     * @return the suite table
     */
    static Suite[] getSuiteTable() {
        return suites;
    }

    /**
     * Get the <code>Klass</code> instance corresponding to a specified
     * class identifier.
     *
     * @param    classID  the class identifier
     * @return   the <code>Klass</code> instance corresponding to
     *                   <code>classID</code> or null if there isn't one
     */
    public static Klass getKlass(int classID) {
        int sno = Klass.toSuiteNumber(classID);
        int cno = Klass.toClassNumber(classID);
        return suites[sno].getKlass(cno);
    }

    /**
     * Gets a suite corresponding to a given name.
     *
     * @param   name  the name of the suite to retrieve
     * @return  the suite whose name is <code>name</code> or null if there
     *                is no suite of that name
    public static Suite lookupSuite(String name) {
        for (int i = 0; i < suites.length; i++) {
            Suite suite = suites[i];
            if (suite.getName().equals(name)) {
                return suite;
            }
        }
        return null;
    }
*/

    /**
     * Gets a suite corresponding to a given suite number.
     *
     * @param   sno  the number of the suite to retrieve
     * @return  the suite whose number is <code>sno</code> or null if there
     *                is no suite for that number
     */
    public static Suite getSuite(int sno) {
        if (sno >= 0 && sno < suites.length) {
            return suites[sno];
        }
        return null;
    }

    /**
     * Gets an enumeration of all the suites loaded into this suite manager.
     * The enumeration is in ascending order by suite numbers.
     *
     * @return an enumeration of all the suites loaded into this suite manager
     */
    public static Enumeration getSuites() {
        return new Enumeration() {
            int lastSno = getLastSuiteNumbert();
            int sno;
            private int getLastSuiteNumbert() {
                int sno = -1;
                for (int i = 0; i != suites.length; ++i) {
                    if (suites[i] != null) {
                        sno = suites[i].getSuiteNumber();
                    }
                }
                return sno;
            }
            public boolean hasMoreElements() {
                return sno <= lastSno;
            }
            public Object nextElement() {
                if (sno > lastSno) {
                    throw new NoSuchElementException();
                }
                while (suites[sno] == null) {
                    sno++;
                }
                return suites[sno++];
            }
        };
    }

    /**
     * Get the <code>KlassMetadata</code> instance corresponding to a specified
     * class identifier.
     *
     * @param    classID  the class identifier
     * @return   the <code>KlassMetadata</code> instance corresponding to
     *                   <code>classID</code> or null if there isn't one
     */
    public static KlassMetadata getMetadata(int classID) {
        int sno = Klass.toSuiteNumber(classID);
        int cno = Klass.toClassNumber(classID);
        return suites[sno].getMetadata(cno);
    }

    /**
     * Adjust the child count of a specified suite by a specified value.
     *
     * @param parent  the suite whose child count is to be adjusted
     * @param delta   the adjustment value
     */
    public static void adjustChildren(Suite parent, int delta) {
        parent.adjustChildCount(delta);
    }

    /**
     * Allocates a free suite number. The number returned will not be
     * reallocated until the suite created with this number is
     * {@link #removeSuite(Suite) removed} from the system.
     *
     * @return the next available suite number.
     */
    public static int allocateFreeSuiteNumber() {
        for (int i = 0 ; i < suites.length ; i++) {
            if (suites[i] == null) {
                suites[i] = RESERVED;
                return i;
            }
        }
        int sno = suites.length;
        Suite[] newsuites = new Suite[sno + 1];
        // Avoid using System.arraycopy() for bootstrapping reasons -- was: System.arraycopy(suites, 0, newsuites, 0, sno);
        for (int i = 0 ; i < suites.length ; i++) {
            newsuites[i] = suites[i];
        }
        suites = newsuites;
        suites[sno] = RESERVED;
        return sno;
    }

    /**
     * Installs a suite it to the list of suites in the system. This may be
     * used to replace a suite but only if the replaced suite is
     * {@link Suite#isReplaceable() replaceable}.
     *
     * @param   suite  the suite to install
     */
    public static void installSuite(Suite suite) {
        int sno = suite.getSuiteNumber();
        Assert.that(sno < suites.length, "unallocated suite number");

        Suite oldSuite = suites[sno];
        if (oldSuite != RESERVED) {
            if (!oldSuite.isReplaceable()) {
                throw new InternalError("cannot replace non-replaceable suite");
            }
            /*
             * decrement the child count of all the parents of the old suite
             */
            oldSuite.remove();
        }

        /*
         * increment the child count of all the parent suites
         * by the new suite
         */
        Suite parent = suite.getParent();
        if (parent != null) {
            adjustChildren(parent, 1);
        }

        /*
         * Atomic update of word in persistent memory
         */
        suites[sno] = suite;
    }

    /**
     * Attempt to remove an existing suite from the system. The suite is only
     * removed if its child count is 0 (i.e. no other suite depends on the
     * suite).
     *
     * @param    suite   the suite to remove
     * @return   true if the suite was actually removed
     */
    static boolean removeSuite(Suite suite) {
        int sno = suite.getSuiteNumber();
        Assert.that(suites[sno] == suite);
        if (suite.remove()) {
            suites[sno] = null; // Atomic update of word in persistent memory
            return true;
        } else {
            return false;
        }
    }
}
