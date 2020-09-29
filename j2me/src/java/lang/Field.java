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
 * An instance of <code>Field</code> encapsulates the information about the
 * field of a class. This includes the name of the field, its type, access
 * flags etc.
 *
 * @author  Doug Simon
 */
public final class Field extends Member {

    /**
     * Creates a new <code>Field</code>.
     *
     * @param  metadata the metadata of the class that declared the field
     * @param  id       the index of this field within <code>metadata</code>
     */
    Field(KlassMetadata metadata, int id) {
        super(metadata, id);
    }


    /*---------------------------------------------------------------------------*\
     *              Access permissions and member property queries               *
    \*---------------------------------------------------------------------------*/

    /**
     * Determines if this field is transient.
     *
     * @return  true if this field is transient
     */
    public boolean isTransient() {
        return Modifier.isTransient(parser().getModifiers());
    }

    /**
     * Determines is this field had a ConstantValue attribute in its class file
     * definition. Note that this does not necessarily mean that the field is 'final'.
     *
     * @return  if there is a constant value associated with this field
     */
    public boolean hasConstant() {
        return Modifier.hasConstant(parser().getModifiers());
    }


    /*---------------------------------------------------------------------------*\
     *                        Field component getters                            *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets this declared type of this field.<p>
     *
     * @return   this declared type of this field
     */
    public Klass getType() {
        return SuiteManager.getKlass(parser().getSignatureAt(0));
    }

    /**
     * Gets the constant value of this primitive static final field. It is
     * an error to call this method for a field that did not have a ConstantValue
     * attribute in its class file.
     *
     * @return  the constant value encoded in a long
     */
    public long getConstantValue() {
        if (!hasConstant()) {
            throw new Error();
        }
        return parser().getConstantValue();
    }
}
