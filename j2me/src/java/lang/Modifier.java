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
 * The Modifier class provides
 * constants to decode class and member access modifiers.  The sets of
 * modifiers are represented as integers with distinct bit positions
 * representing different modifiers.  The values for the constants
 * representing the modifiers are taken from <a
 * href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/VMSpecTOC.doc.html"><i>The
 * Java</i><sup><small>TM</small></sup> <i>Virtual Machine Specification, Second
 * edition</i></a> tables
 * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#75734">4.1</a>,
 * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#88358">4.4</a>,
 * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#75568">4.5</a>, and
 * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#88478">4.7</a>.
 * <p>
 * The modifiers in the Squawk system augment those defined in the JVM
 * specification to include flags denoting Squawk specific properties of
 * classes and members.
 *
 * @see Klass#getModifiers()
 * @see Member#getModifiers()
 *
 * @author Nakul Saraiya
 * @author Kenneth Russell
 */
public final class Modifier {

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>public</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>public</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isPublic(int mod) {
        return (mod & PUBLIC) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>private</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>private</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isPrivate(int mod) {
        return (mod & PRIVATE) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>protected</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>protected</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isProtected(int mod) {
        return (mod & PROTECTED) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>static</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>static</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isStatic(int mod) {
        return (mod & STATIC) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>final</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>final</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isFinal(int mod) {
        return (mod & FINAL) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>synchronized</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>synchronized</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isSynchronized(int mod) {
        return (mod & SYNCHRONIZED) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>volatile</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>volatile</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isVolatile(int mod) {
        return (mod & VOLATILE) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>transient</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>transient</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isTransient(int mod) {
        return (mod & TRANSIENT) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>native</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>native</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isNative(int mod) {
        return (mod & NATIVE) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>interface</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>interface</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isInterface(int mod) {
        return (mod & INTERFACE) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>abstract</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>abstract</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isAbstract(int mod) {
        return (mod & ABSTRACT) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>strictfp</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>strictfp</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isStrict(int mod) {
        return (mod & STRICT) != 0;
    }

    /**
     * Return a string describing the access modifier flags in
     * the specified modifier. For example:
     * <blockquote><pre>
     *    public final synchronized strictfp
     * </pre></blockquote>
     * The modifier names are returned in an order consistent with the
     * suggested modifier orderings given in <a
     * href="http://java.sun.com/docs/books/jls/second_edition/html/j.title.doc.html"><em>The
     * Java Language Specification, Second Edition</em></a> sections
     * <a href="http://java.sun.com/docs/books/jls/second_edition/html/classes.doc.html#21613">&sect;8.1.1</a>,
     * <a href="http://java.sun.com/docs/books/jls/second_edition/html/classes.doc.html#78091">&sect;8.3.1</a>,
     * <a href="http://java.sun.com/docs/books/jls/second_edition/html/classes.doc.html#78188">&sect;8.4.3</a>,
     * <a href="http://java.sun.com/docs/books/jls/second_edition/html/classes.doc.html#42018">&sect;8.8.3</a>, and
     * <a href="http://java.sun.com/docs/books/jls/second_edition/html/interfaces.doc.html#235947">&sect;9.1.1</a>.
     * The full modifier ordering used by this method is:
     * <blockquote> <code>
     * public protected private abstract static final transient
     * volatile synchronized native strictfp
     * interface </code> </blockquote>
     * The <code>interface</code> modifier discussed in this class is
     * not a true modifier in the Java language and it appears after
     * all other modifiers listed by this method.  This method may
     * return a string of modifiers that are not valid modifiers of a
     * Java entity; in other words, no checking is done on the
     * possible validity of the combination of modifiers represented
     * by the input.
     *
     * @param   mod a set of modifers
     * @return  a string representation of the set of modifers
     * represented by <code>mod</code>
     */
    public static String toString(int mod) {
        StringBuffer sb = new StringBuffer();
        int len;

        if ((mod & PUBLIC) != 0)        sb.append("public ");
        if ((mod & PROTECTED) != 0)     sb.append("protected ");
        if ((mod & PRIVATE) != 0)       sb.append("private ");

        /* Canonical order */
        if ((mod & ABSTRACT) != 0)      sb.append("abstract ");
        if ((mod & STATIC) != 0)        sb.append("static ");
        if ((mod & FINAL) != 0)         sb.append("final ");
        if ((mod & TRANSIENT) != 0)     sb.append("transient ");
        if ((mod & VOLATILE) != 0)      sb.append("volatile ");
        if ((mod & SYNCHRONIZED) != 0)  sb.append("synchronized ");
        if ((mod & NATIVE) != 0)        sb.append("native ");
        if ((mod & STRICT) != 0)        sb.append("strictfp ");
        if ((mod & INTERFACE) != 0)     sb.append("interface ");

/*if[TRUSTED]*/
        if ((mod & TACC_SUBCLASS) != 0)                  sb.append("T_subclass ");
        if ((mod & TACC_CLASS_RESOURCE_ACCESS) != 0)     sb.append("T_cra ");
        if ((mod & TACC_EXCEPTION) != 0)                 sb.append("T_exception ");
/*end[TRUSTED]*/

        if ((len = sb.length()) > 0) {   /* trim trailing space */
            return sb.toString().substring(0, len-1);
        }
        return "";
    }

    /*
     * Access modifier flag constants from <em>The Java Virtual
     * Machine Specification, Second Edition</em>, tables 4.1, 4.4,
     * 4.5, and 4.7.
     */

    /**
     * The <code>int</code> value representing the <code>public</code>
     * modifier.
     */
    public static final int PUBLIC          = 0x00000001;

    /**
     * The <code>int</code> value representing the <code>private</code>
     * modifier.
     */
    public static final int PRIVATE         = 0x00000002;

    /**
     * The <code>int</code> value representing the <code>protected</code>
     * modifier.
     */
    public static final int PROTECTED       = 0x00000004;

    /**
     * The <code>int</code> value representing the <code>static</code>
     * modifier.
     */
    public static final int STATIC          = 0x00000008;

    /**
     * The <code>int</code> value representing the <code>final</code>
     * modifier.
     */
    public static final int FINAL           = 0x00000010;

    /**
     * The <code>int</code> value representing the <code>synchronized</code>
     * modifier.
     */
    public static final int SYNCHRONIZED    = 0x00000020;

    /**
     * The <code>int</code> value representing the <code>volatile</code>
     * modifier.
     */
    public static final int VOLATILE        = 0x00000040;

    /**
     * The <code>int</code> value representing the <code>transient</code>
     * modifier.
     */
    public static final int TRANSIENT       = 0x00000080;

    /**
     * The <code>int</code> value representing the <code>native</code>
     * modifier.
     */
    public static final int NATIVE          = 0x00000100;

    /**
     * The <code>int</code> value representing the <code>interface</code>
     * modifier.
     */
    public static final int INTERFACE       = 0x00000200;

    /**
     * The <code>int</code> value representing the <code>abstract</code>
     * modifier.
     */
    public static final int ABSTRACT        = 0x00000400;

    /**
     * The <code>int</code> value representing the <code>strictfp</code>
     * modifier.
     */
    public static final int STRICT          = 0x00000800;


    /*---------------------------------------------------------------------------*\
     *                         Squawk specific modifiers                         *
    \*---------------------------------------------------------------------------*/

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>MUSTCLINIT</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>MUSTCLINIT</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean mustClinit(int mod) {
        return (mod & MUSTCLINIT) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>HASFINALIZER</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>HASFINALIZER</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean hasFinalizer(int mod) {
        return (mod & HASFINALIZER) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>PRIMITIVE</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>PRIMITIVE</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isPrimitive(int mod) {
        return (mod & PRIMITIVE) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>SYNTHETIC</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>SYNTHETIC</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isSynthetic(int mod) {
        return (mod & SYNTHETIC) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>DOUBLEWORD</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>DOUBLEWORD</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isDoubleWord(int mod) {
        return (mod & DOUBLEWORD) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>ARRAY</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>ARRAY</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isArray(int mod) {
        return (mod & ARRAY) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>SQUAWKARRAY</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>SQUAWKARRAY</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isSquawkArray(int mod) {
        return (mod & SQUAWKARRAY) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>SQUAWKPRIMITIVE</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>SQUAWKPRIMITIVE</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isSquawkPrimitive(int mod) {
        return (mod & SQUAWKPRIMITIVE) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>SQUAWKPRIMITIVE</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>SQUAWKPRIMITIVE</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isSquawkNative(int mod) {
        return (mod & SQUAWKNATIVE) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>CONSTRUCTOR</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>CONSTRUCTOR</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isConstructor(int mod) {
        return (mod & CONSTRUCTOR) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>CLASSINITIALIZER</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>CLASSINITIALIZER</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean isClassInitializer(int mod) {
        return (mod & CLASSINITIALIZER) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>CONSTANT</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>CONSTANT</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean hasConstant(int mod) {
        return (mod & CONSTANT) != 0;
    }

    /**
     * The <code>int</code> value denoting that a method is a constructor.
     */
    public static final int CONSTRUCTOR         = 0x00001000;

    /**
     * The <code>int</code> value denoting that a method is a class constructor.
     */
    public static final int CLASSINITIALIZER    = 0x00002000;

    /**
     * The <code>int</code> value denoting that a field has a ConstantValue.
     */
    public static final int CONSTANT            = 0x00004000;

    /**
     * The <code>int</code> value denoting that superclass methods should
     * be treated specially when invoked by the <i>invokespecial</i> instruction.
     */
    public static final int SUPER               = 0x00000020;

    /**
     * The <code>int</code> value denoting that a class must have its
     * class initializer executed before it is used.
     */
    public static final int MUSTCLINIT          = 0x00010000;

    /**
     * The <code>int</code> value denoting that a class overrides the
     * {@link Object#finalize()} method.
     */
    public static final int HASFINALIZER        = 0x00020000;

    /**
     * The <code>int</code> value denoting that a class represents a primitive
     * type.
     */
    public static final int PRIMITIVE           = 0x00040000;

    /**
     * The <code>int</code> value denoting that a class that does not have
     * a class file representation.
     */
    public static final int SYNTHETIC           = 0x00080000;

    /**
     * The <code>int</code> value denoting that a class represents a double
     * word type (i.e. <code>long</code> or <code>double</code>).
     */
    public static final int DOUBLEWORD          = 0x00100000;

    /**
     * The <code>int</code> value denoting that a class represents a Java array.
     */
    public static final int ARRAY               = 0x00200000;

    /**
     * The <code>int</code> value denoting that a class represents an array in the
     * Squawk sense i.e. it is a Java array or some kind of string.
     */
    public static final int SQUAWKARRAY         = 0x00400000;

    /**
     * The <code>int</code> value denoting that a class represents a special class
     * that the Squawk translator and compiler convert into a primitive type. Values
     * of these types are not compatible with any other types and requires explicit
     * conversions.
     * <p>
     * For efficiency and to avoid meta-circularity, the Squawk primitive variables are
     * intercepted by the translator and converted into the base type (int or long) so
     * no real object is created at run-time.
     * <p>
     * There are a number of restrictions that must be observed when programming with
     * these classes. Some of these constraints are imposed to keep the job of the
     * translator simple. All of these constraints are currently enforced by the
     * translator. The constraints are:
     * <ul>
     *   <li>
     *       A local variable slot allocated by javac for a Squawk primitive variable
     *       must never be used for a value of any other type (including a different
     *       Squawk primitive type). This is required as the translator cannot currently
     *       de-multiplex reference type slots into disjoint typed slots. This restriction
     *       on javac is achieved by declaring all Squawk primitive local variables at
     *       the outer most scope (as javac using lexical based scoping for register
     *       allocation liveness).
     *   </li>
     *   <li>
     *       A Squawk primitive value of type T cannot be assigned to or compared with
     *       values of any other type (including <code>null</code>) than T.
     *   </li>
     * </ul>
     */
    public static final int SQUAWKPRIMITIVE     = 0x00800000;

    /**
     * The <code>int</code> value denoting that a class will have all its public methods
     * (apart from those overriding a super class) converted into native methods by the
     * translator.
     */
    public static final int SQUAWKNATIVE        = 0x01000000;

    /**
     * Gets the mask of modifiers that are defined the JVM specification that
     * pertain to a class.
     *
     * @return  the mask of values defined in table
     * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#75734">4.1</a>
     *          in the JVM specification
     */
    public static int getJVMClassModifiers() {
        return PUBLIC | FINAL | SUPER | INTERFACE | ABSTRACT;
    }

    /**
     * Gets the mask of modifiers that are defined the JVM specification that
     * pertain to a method.
     *
     * @return  the mask of values defined in table
     * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#75568">4.5</a>
     *          in the JVM specification
     */
    public static int getJVMMethodModifiers() {
        return PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL | SYNCHRONIZED | NATIVE | ABSTRACT | STRICT;
    }

    /**
     * Gets the mask of modifiers that are defined the JVM specification that
     * pertain to a field.
     *
     * @return  the mask of values defined in table
     * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#88358">4.4</a>
     *          in the JVM specification
     */
    public static int getJVMFieldModifiers() {
        return PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL | VOLATILE | TRANSIENT;
    }


/*if[TRUSTED]*/
    /*---------------------------------------------------------------------------*\
     *                           Secure Squawk modifiers                         *
    \*---------------------------------------------------------------------------*/

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>TACC_SUBCLASS</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>TACC_SUBCLASS</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean allowUntrustedSubclass(int mod) {
        return (mod & TACC_SUBCLASS) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>TACC_CLASS_RESOURCE_ACCESS</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>TACC_CLASS_RESOURCE_ACCESS</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean allowUntrustedClassResourceAccess(int mod) {
        return (mod & TACC_CLASS_RESOURCE_ACCESS) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>TACC_EXCEPTION</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>TACC_EXCEPTION</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean allowPackagePrivateException(int mod) {
        return (mod & TACC_EXCEPTION) != 0;
    }

    /**
     * Return <tt>true</tt> if the integer argument includes the
     * <tt>TACC_ALLOW_UNTRUSTED_FIELD_METHOD</tt> modifer, <tt>false</tt> otherwise.
     *
     * @param   mod a set of modifers
     * @return <tt>true</tt> if <code>mod</code> includes the
     * <tt>TACC_ALLOW_UNTRUSTED_FIELD_METHOD</tt> modifier; <tt>false</tt> otherwise.
     */
    public static boolean allowUntrustedAccessToMember(int mod) {
        return (mod & TACC_ALLOW_UNTRUSTED_FIELD_METHOD) != 0;
    }


    /**
     * The <code>int</code> value denoting whether an unprivileged class
     * may subclass this class.
     */
    public static final int TACC_SUBCLASS                   = 0x10000000;


    /**
    * The <code>int</code> value denoting whether an unprivileged class
    * may access resources of this class.  When this modifier is set,
    * an unprivileged class may instanciate and access static methods
    * and fields.
    */
    public static final int TACC_CLASS_RESOURCE_ACCESS      = 0x20000000;


    /**
    * The <code>int</code> value denoting how non-public exceptions
    * are handled.  When this modifier is set, standard Java semantics
    * are used - i.e. the non-public exception class thrown by this class
    * can be caught via a publicly accessible base class of the exception.
    * When the flag is not set, only classes in the same package as this
    * class can catch these types of exceptions.
    */
    public static final int TACC_EXCEPTION                  = 0x40000000;


    /**
     * The <code>int</code> value denoting whether an unprivileged class
     * may access this field or method.
     */
    public static final int TACC_ALLOW_UNTRUSTED_FIELD_METHOD = 0x80000000;


    /**
     * The trusted access modifiers in the Trusted attribute appear in the
     * first byte.  This value is the number of bit positions that it must be shifted
     * left to coincide with the bit positions used here.
     */
    private static final int TRUSTED_SHIFT_AMOUNT                  = 28;

    /**
    * Gets the mask of modifiers that pertain to the Trusted class attributes.
    *
    * @return  the mask of values defined in table
    */
   public static int getTrustedModifiers() {
       return TACC_SUBCLASS | TACC_CLASS_RESOURCE_ACCESS | TACC_EXCEPTION;
   }

   /**
    * Convert the Trusted modifers found in the Trusted Attribute to those compatible
    * with those found here
    */
   public static int convertToStandardModifier(int trustedModifier) {
       System.out.println("Raw Trusted Modifier: " + trustedModifier);
       System.out.println("Shifted Trusted Modifier: " + (trustedModifier << TRUSTED_SHIFT_AMOUNT));
       int i = (getTrustedModifiers() & (trustedModifier << TRUSTED_SHIFT_AMOUNT));
       System.out.println("Trusted Modifier: " + i);
       return i;
   }

/*end[TRUSTED]*/

}
