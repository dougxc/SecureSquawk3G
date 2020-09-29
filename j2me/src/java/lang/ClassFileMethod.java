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
 * An instance of <code>ClassFileMethod</code> encapsulates all the
 * symbolic information of a method declaration in a class file.
 * This class is provided for a subsystem (such as the translator) that
 * loads a class definition from a class file.
 *
 * @author  Doug Simon
 */
public final class ClassFileMethod extends ClassFileMember {

    /**
     * A zero-length array of <code>ClassFileMethod</code>s.
     */
    public static final ClassFileMethod[] NO_METHODS = {};

    /**
     * The return type of this method.
     */
    private final Klass returnType;

    /**
     * The types of the parameters of this method.
     */
    private final Klass[] parameterTypes;

    /**
     * The bytecode array of this method.
     */
    private byte[] code;

    /**
     * Creates a new <code>ClassFileMethod</code> instance.
     *
     * @param   name           the name of the method
     * @param   modifiers      the modifiers of the method
     * @param   returnType     the return type of the method
     * @param   parameterTypes the parameters types of the method
     */
    public ClassFileMethod(String name, int modifiers, Klass returnType, Klass[] parameterTypes) {
        super(name, modifiers);
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    /**
     * Gets the return type of this method.
     *
     * @return  the return type of this method
     */
    public Klass getReturnType() {
        return returnType;
    }

    /**
     * Gets the parameter types of this method.
     *
     * @return  the parameter types of this method
     */
    public Klass[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Gets the byte array corresponding to the "Code" attribute in the
     * class file. This can only be called for a non-native, non-abstract
     * method.
     *
     * @return  the data in the "Code" attribute for this method
     */
    public byte[] getCode() {
        Assert.that(!isAbstract() && !isNative());
        return code;
    }

    /**
     * Sets the byte array corresponding to the "Code" attribute in the
     * class file. This can only be called for a non-native, non-abstract
     * method.
     *
     * @param  code  the data in the "Code" attribute for this method
     */
    public void setCode(byte[] code) {
        Assert.that(!isAbstract() && !isNative());
        this.code = code;
    }

    /**
     * Determines if this is a <code>native</code> method.
     *
     * @return  true if this is a <code>native</code> method
     */
    public boolean isNative() {
        return Modifier.isNative(modifiers);
    }

    /**
     * Determines if this is a <code>abstract</code> method.
     *
     * @return  true if this is a <code>abstract</code> method
     */
    public boolean isAbstract() {
        return Modifier.isAbstract(modifiers);
    }

    /**
     * Determines if this is a <code>protected</code> method.
     *
     * @return  true if this is a <code>protected</code> method
     */
    public boolean isProtected() {
        return Modifier.isProtected(modifiers);
    }

    /**
     * Determines if this method is <init>.
     *
     * @return  true if it is
     */
    public boolean isInit() {
        return getName().equals("<init>") && parameterTypes.length == 0;
    }

    /**
     * Determines if this method is <clinit>.
     *
     * @return  true if it is
     */
    public boolean isClinit() {
        return getName().equals("<clinit>") && parameterTypes.length == 0;
    }

    /**
     * Determines if this method is "finalize()".
     *
     * @return  true if it is
     */
    public boolean isFinalize() {
        return getName().equals("finalize");
    }

    /**
     * Determines if this method is a static void main(String[]).
     *
     * @return  true if it is
     */
    public boolean isMain() {
        return isStatic() &&
               getName().equals("main") &&
               returnType == Klass.VOID &&
               parameterTypes.length == 1 &&
               parameterTypes[0] == Klass.STRING_ARRAY;
    }
}