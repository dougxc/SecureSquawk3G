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
 * An instance of <code>ClassFileField</code> encapsulates all the
 * symbolic information of a field declaration in a class file.
 * This class is provided for a subsystem (such as the translator) that
 * loads a class definition from a class file.
 *
 * @author  Doug Simon
 */
public class ClassFileField extends ClassFileMember {

    /**
     * A zero-length array of <code>ClassFileField</code>s.
     */
    public static final ClassFileField[] NO_FIELDS = {};

    /**
     * The type of this field.
     */
    private final Klass type;

    /**
     * Creates a new <code>ClassFileField</code> instance.
     *
     * @param   name       the name of the field
     * @param   modifiers  the modifiers of the field
     * @param   type       the type of the field
     */
    public ClassFileField(String name, int modifiers, Klass type) {
        super(name, modifiers);
        this.type = type;
    }

    /**
     * Gets the type of this field.
     *
     * @return  the type of this field
     */
    public Klass getType() {
        return type;
    }
}
