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
 * An instance of <code>ClassFileConstantField</code> encapsulates all the
 * symbolic information of a field declaration in a class file that has a
 * ConstantValue attribute. This can only be used for primitive constants.
 * This class is provided for a subsystem (such as the translator) that
 * loads a class definition from a class file.
 *
 * @author  Doug Simon
 */
public final class ClassFileConstantField extends ClassFileField {

    /**
     * The value in the ConstantValue attribute.
     */
    private final long constantValue;

    /**
     * Creates a new <code>ClassFileField</code> instance.
     *
     * @param   name          the name of the field
     * @param   modifiers     the modifiers of the field
     * @param   type          the type of the field
     * @param   constantValue the constant value
     */
    public ClassFileConstantField(String name, int modifiers, Klass type, long constantValue) {
        super(name, modifiers, type);
        this.constantValue = constantValue;
    }

    /**
     * Gets the constant value for this field.
     *
     * @return  the constant value for this field
     */
    public long getConstantValue() {
        return constantValue;
    }
}
