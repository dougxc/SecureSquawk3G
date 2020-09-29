/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ci;

import com.sun.squawk.util.Assert;

/**
 * This class represents the type of the value pushed to the operand stack
 * by a <i>new</i> bytecode before the corresponding constructor is
 * called on it.
 *
 * @author  Doug Simon
 */
public final class UninitializedObjectClass extends Klass {

    /**
     * The class specified by the operand of the <i>new</i> bytecode.
     */
    private Klass initializedType;

    /**
     * Creates a new <code>UninitializedObjectClass</code> instance to
     * represent the type of the value pushed on the operand stack by a
     * <i>new</i> bytecode. The name must be <code>"new@"</code> with the
     * address of the <i>new</i> bytecode appended. For example:<p>
     * <p><blockquote><pre>
     *     "new@45"
     * </pre></blockquote><p>
     *
     * @param name            the name of the type
     * @param initializedType the class specified by the operand of the
     *                        <i>new</i> bytecode (may be null)
     */
    public UninitializedObjectClass(String name, Klass initializedType) {
        super(name, Klass.UNINITIALIZED_NEW);
        this.initializedType = initializedType;
    }

    /**
     * Determines if the initialized type has been set. The value will not have
     * been set if this instance is the result of a
     * <code>ITEM_Uninitialized</code> entry in a stack map.
     *
     * @return  true if the initialized type has been set
     * @see     StackMap
     */
    public boolean hasInitializedTypeBeenSet() {
        return initializedType != null;
    }

    /**
     * Updates the initialized type. This must only be called once per instance
     * of <code>UninitializedObjectClass</code>.
     *
     * @param initializedType  the class specified by the operand of the
     *                         <i>new</i> bytecode
     * @see   #hasInitializedTypeBeenSet()
     */
    public void setInitializedType(Klass initializedType) {
        Assert.that(this.initializedType == null, "cannot change initialized type");
        this.initializedType = initializedType;
    }

    /**
     * Gets the class specified by the operand of the <i>new</i> bytecode.
     *
     * @return the class specified by the operand of the <i>new</i> bytecode
     */
    public Klass getInitializedType() {
        Assert.that(initializedType != null, "initialized type not yet set");
        return initializedType;
    }

    /**
     * {@inheritDoc}
     */
//    public int getClassID() {
//        Assert.that(initializedType != null);
//        return initializedType.getClassID();
//    }

}