/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.util.Assert;

/**
 * An Object association is the logical extension of an object that is used
 * to hold rarely used information like the monitor and hashcode.
 * This data structure is placed between an object and its class when
 * a hash code or monitor is needed for the object. The first two words
 * of this data structure match exactly the first two in java.lang.Klass
 */
class ObjectAssociation {

    /**
     * The klass of the object for which this is the association.
     * *** This must be the first instance variable to match the first variable in java.lang.Klass ***
     */
    private Klass klass;

    /**
     * The copy of the vtable of the target class. This is used to speed up virtual method dispatching.
     * *** This must be the second instance variable to match the second variable in java.lang.Klass ***
     */
    private Object[] virtualMethods;

    /**
     * The monitor for the object
     */
    private Monitor monitor;

    /**
     * The hashcode the object
     */
    private int hashCode;

    /**
     * Constructor.
     *
     * @param klass the klass of the object requiring an ObjectAssociation
     */
    ObjectAssociation(Klass klass) {
        this.klass          = klass;
        this.virtualMethods = klass.getVirtualMethods();
    }

    /**
     * Set the monitor.
     *
     * @param monitor the monitor
     */
    void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Get the monitor.
     *
     * @return the monitor
     */
    Monitor getMonitor() {
        return monitor;
    }

    /**
     * Get the hashcode.
     *
     * @return the hashcode
     */
    int getHashCode() {
        if (hashCode == 0) {
            hashCode = VM.getNextHashcode();
        }
        Assert.that(hashCodeInUse());
        return hashCode;
    }

    /**
     * Test to see if the hash code was used.
     *
     * @return true if is was
     */
    boolean hashCodeInUse() {
        return hashCode != 0;
    }

}
