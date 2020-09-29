/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import com.sun.squawk.vm.*;
import com.sun.squawk.util.Assert;
import java.io.PrintStream;
import com.sun.squawk.util.Vector;    // Version without synchronization

import com.sun.squawk.util.Tracer;


/**
 * The Klass class is a Squawk VM specific version of the class Class. It is
 * transformed by the translator such that it replaces Class and the translator
 * tranforms all reference to Class to refer to this class instead. The reason
 * the name 'Class' cannot be used is that it prevents an application such as
 * the translator form being runnable on a Hotspot VM which expects a different
 * structure for the class Class.<p>
 *
 * This class is API compatible with Class but also contains extra fields and
 * methods specific to the Squawk system.<p>
 *
 * Class references take one of two forms in the Squawk system. The first form
 * is an object reference (of type <code>Class</code>). The second form is an
 * unsigned 32-bit identifier (of type
 * <code>int</code>) that is unique for each class in the system. This
 * identifier is referred to as a 'class ID' and is composed of two 16-bit
 * values. The high 16-bit value identifies the suite encapsulating the class
 * and the low 16-bit value uniquely identifies the class within its suite.<p>
 *
 * The Squawk system uses a type hierarchy to simplify verification. The diagram
 * below shows this hierarchy:<p>
 *
 * <img src="doc-files/Klass-1.jpg"><p>
 *
 * The classes in the <b>Java class hierarchy</b> have the expected
 * relationships with each other as specified by the standard Java API.
 * For example, the {@link #getSuperclass()} method returns null for
 *  <code>java.lang.Object</code> as well as for classes representing interfaces
 * primitive types. The same Java API compliance holds for all the other
 * standard API methods.
 *
 * @author  Nik Shaylor, Doug Simon
 * @version 1.0
 * @see     java.lang.KlassMetadata
 */
public class Klass {

    /*---------------------------------------------------------------------------*\
     *      Fields of Klass, some of which may be accessed directly by the VM    *
    \*---------------------------------------------------------------------------*/

    /**
     * The pointer to self. This must be the at the same offset to the class pointer
     * in ObjectAssociation.
     */
    private final Klass self = this;

    /**
     * The virtual method table (or <i>vtable</i>) for this class.
     * Vtables can be implemented in one of two ways; segmented
     * or not. Consider the following class definitions:<p>
     * <p><blockquote><pre>
     *     class X {
     *         void a() { ... }
     *         void b() { ... }
     *         void c() { ... }
     *     }
     *
     *     class Y extends X {
     *         void d() { ... }
     *         void e() { ... }
     *     }
     *
     *     class Z extends Y {
     *         void c() { ... } // overrides class X
     *         void f() { ... }
     *     }
     * </pre></blockquote></p>
     * If a segmented implementation is used, then the vtable for these
     * three classes would be as follows:
     * <p><blockquote><pre>
     *           +-----+-----+-----+
     *  class X: | X.a | X.b | X.c |
     *           +-----+-----+-----+
     *
     *                             +-----+-----+
     *  class Y:                   | Y.d | Y.e |
     *                             +-----+-----+
     *
     *                       +-----+-----+-----+-----+
     *  class Z:             | Z.c |     |     | Z.f |
     *                       +-----+-----+-----+-----+
     * </pre></blockquote></p>
     * Note that the vtable for a class only contains non-null entries for the
     * methods that it defines. This allows method lookup in the VM to
     * resolve the class pointer for the method as a side effect of the
     * lookup. A segmented implementation requires that each class specifies
     * the lowest vtable index that it includes. For the above classes, this
     * would be 0, 3 and 2 respectively for X, Y and Z.<p>
     *
     * If a non-segmented implementation is used, then the vtable for
     * these three classes would be as follows:
     * <p><blockquote><pre>
     *           +-----+-----+-----+
     *  class X: | X.a | X.b | X.c |
     *           +-----+-----+-----+
     *
     *           +-----+-----+-----+-----+-----+
     *  class Y: | X.a | X.b | X.c | Y.d | Y.e |
     *           +-----+-----+-----+-----+-----+
     *
     *           +-----+-----+-----+-----+-----+-----+
     *  class Z: | X.a | X.b | Z.c | Y.d | Y.e | Z.f |
     *           +-----+-----+-----+-----+-----+-----+
     * </pre></blockquote></p>
     * With this less space conservative implementation, method lookup in the
     * VM (or JIT'ed) code is more efficient as it is simply an array indexing
     * operation. In addition, the method must encode the class pointer itself.
     * <p>
     * <b>This system uses a non-segmented implementation.</b>
     * <p>
     * The methods are instances of <code>MethodBody</code> until this class is
     * copied into NVM or is initialized.
     */
    private Object[] virtualMethods;

    /**
     * The table of static methods declared by this class.<p>
     *
     * The methods are instances of <code>MethodBody</code> until this class is
     * copied into NVM or is initialized.
     */
    private Object[] staticMethods;

    /**
     * The name of the class in Squawk internal form.
     *
     * @see   #getInternalName()
     */
    private final String name;

    /**
     * The class representing the component type of an array.  If this class
     * does not represent an array class this is null.
     */
    private final Klass componentType;

    /**
     * The super type of this class.
     */
    private Klass superType;

    /**
     * The ordered set of interfaces implemented by this class. If this
     * is a non-interface and non-abstract class, then this array is the
     * closure of all the interfaces implemented by this class (implicitly
     * or explicitly) that are not implemented by any super class.
     */
    private Klass[] interfaces;

    /**
     * The mapping from each interface method to the virtual method that
     * implements it. The mapping for the methods of the interface at index
     * <i>idx</i> in the <code>interfaces</code> array is at index
     * <i>idx</i> in the <code>interfaceSlotTables</code> array. The
     * mapping is encoded as an array where a value
     * <i>m</i> at index <i>i</i> indicates that the method at
     * index <i>m</i> in the vtable of this class implements the interface
     * method at index <i>i</i> in the virtual methods table of the
     * interface class.
     */
    private short[][] interfaceVTableMaps;

    /**
     * The pool of object constants (including <code>Klass</code> instances)
     * used by the bytecode of this class.
     */
    private Object[] objects;

    /**
     * The bit map for an instance of this class describing which
     * words of the instance contain pointer values. If this value is null
     * then the map is described by the {@link #oopMapWord}.
     */
    private UWord[] oopMap;

    /**
     * The bit map for an instance of this class describing which
     * words of the instance contain pointer values. This version of the
     * map is used when {@link #instanceSize} is {@link HDR#BITS_PER_WORD} or less.
     */
    private UWord oopMapWord;

    /**
     * The class identifier.
     */
    private final int classID;

    /**
     * A mask of the constants defined in {@link Modifier}.
     */
    private int modifiers;

    /**
     * The translation state of the class.
     */
    private int state = State.DEFINED;

    /**
     * The size (in words) of an instance of this class.
     */
    private short instanceSize;

    /**
     * The size (in words) of the static fields of this class. As
     * static fields are not packed by the VM, each static field occupies
     * one word except for doubles and longs which occupy two words.
     */
    private short staticFieldsSize;

    /**
     * The size (in words) of the static fields of this class that are of a
     * non-primitive type. These fields are guaranteed to preceed all the
     * primitive static fields which means that only the first
     * <code>refStaticFieldsSize</code> words of the object holding that
     * static fields of this class need be considered by the garbage
     * collector as pointer fields.
     */
    private short refStaticFieldsSize;

    /**
     * The vtable index for the default constructor of this class or
     * -1 if no such method exists.
     */
    private short indexForInit = -1;

    /**
     * The vtable index for this class's <code>&lt;clinit&gt;</code> method or
     * -1 if no such method exists.
     */
    private short indexForClinit = -1;

    /**
     * The vtable index for this class's <code>public static void main(String[])</code>
     * method or -1 if no such method exists.
     */
    private short indexForMain = -1;

    /**
     * The botton 8 bits of the modifier for <init>() (if present).
     */
    private byte initModifiers;

    /**
     * True is the class, or a super class, has a <clinit>.
     */
    private boolean mustClinit;

    /**
     * The virtual slot in Object for the finalize() method.
     */
    private final static int FINALIZE_SLOT = 9;


    /*---------------------------------------------------------------------------*\
     *                       Standard java.lang.Class API                        *
    \*---------------------------------------------------------------------------*/

    /**
     * Returns the <code>Class</code> object associated with the class
     * with the given string name.
     * Given the fully-qualified name for a class or interface, this
     * method attempts to locate, load and link the class.  If it
     * succeeds, returns the Class object representing the class.  If
     * it fails, the method throws a ClassNotFoundException.
     * <p>
     * For example, the following code fragment returns the runtime
     * <code>Class</code> descriptor for the class named
     * <code>java.lang.Thread</code>:
     * <ul><code>
     *   Class&nbsp;t&nbsp;= Class.forName("java.lang.Thread")
     * </code></ul>
     *
     * @param      className   the fully qualified name of the desired class.
     * @return     the <code>Class</code> descriptor for the class with the
     *             specified name.
     * @exception  ClassNotFoundException  if the class could not be found.
     * @since      JDK1.0
     */
    public static Klass forName(String className) throws ClassNotFoundException {
        return forName(className, false, true);
    }

    /**
     * This is a package private interface that is only directly used by java.lang.SuiteCreator.
     */
    static synchronized Klass forName(String className, boolean allowSystemClasses, boolean runClassInitializer) throws ClassNotFoundException {
        // Verbose trace.
        if (VM.isVeryVerbose()) {
            VM.print("[Klass.forName(");
            VM.print(className);
            VM.println(")]");
        }

        Isolate isolate = VM.getCurrentIsolate();
        Suite suite = isolate.getOpenSuite();
        boolean canLoad = (suite != null);
        if (suite == null) {
            suite = isolate.getBootstrapSuite();
        }
        Klass klass = suite.lookup(className);
        ClassNotFoundException cnfe = null;
        if (klass == null) {
            if (!canLoad) {
                cnfe = new ClassNotFoundException(className + " [The current isolate does not have an open suite that can be extended]");
            } else if (!allowSystemClasses && (className.startsWith("java.") || className.startsWith("com.sun."))) {
                String packageName = className.substring(0, className.lastIndexOf('.'));
                cnfe = new ClassNotFoundException("Prohibited package name: " + packageName);
            } else {
                TranslatorInterface translator = isolate.getTranslator();
                if (translator != null) {
                    long freeMem = 0;
                    if (GC.isTracing(GC.TRACE_BASIC)) {
                        VM.collectGarbage();
                        freeMem = GC.freeMemory(true);
                    }

                    klass = translator.findClass(className, -1, true);
                    if (klass != null) {
                        try {
                            translator.load(klass);
                        } catch (NoClassDefFoundError e) {
                            // The class's state will be DEFINED if it was not found on the class path
                            // and in this case a ClassNotFoundException should be thrown instead
                            if (klass.getState() == Klass.State.DEFINED) {
                                klass = null;
                            } else {
                                throw e;
                            }
                        }
                        if (klass != null) {
                            // Must load complete closure
                            translator.computeClosure();
                        }

                        if (GC.isTracing(GC.TRACE_BASIC)) {
                            VM.collectGarbage();
                            VM.print("** Class.forName(\"");
                            VM.print(className);
                            VM.print("\"):  free memory before = ");
                            VM.print(freeMem);
                            VM.print(", free memory after = ");
                            VM.print(GC.freeMemory(true));
                            VM.print(", difference = ");
                            VM.print(freeMem - GC.freeMemory(true));
                            VM.println();
                        }
                    }
                }
            }
        }

        if (klass != null && klass.getState() != Klass.State.DEFINED) {
            if (runClassInitializer) {
                klass.initialiseClass();
            }
            return klass;
        }
        if (cnfe != null) {
            throw cnfe;
        }
        throw new ClassNotFoundException(className);
    }

    /**
     * Creates a new instance of a class.
     *
     * @return     a newly allocated instance of the class represented by this
     *             object. This is done exactly as if by a <code>new</code>
     *             expression with an empty argument list.
     * @exception  IllegalAccessException  if the class or initializer is
     *               not accessible.
     * @exception  InstantiationException  if an application tries to
     *               instantiate an abstract class or an interface, or if the
     *               instantiation fails for some other reason.
     * @since     JDK1.0
     */
    public final Object newInstance() throws InstantiationException, IllegalAccessException {
        /*
         * Check for a sensible object type.
         */
        if (isSquawkArray() || isInterface() || isAbstract()) {
            throw new InstantiationException();
        }

        /*
         * Check that the calling method can access this klass and that there is a default constructor.
         */
        Object callersMp    = VM.getMP(VM.getPreviousFP(VM.getFP()));
        Klass  callersKlass = VM.asKlass(Unsafe.getObject(callersMp, HDR.methodDefiningClass));
        if (!this.isAccessibleFrom(callersKlass) || indexForInit == -1 ||
           /*
            * Note that access is not allowed to a protected constructor (even if
            * callersKlass is a subclass of this class). Protected constructors
            * can only be accessed from within the constructor of a direct
            * subclass of a class. They cannot be accessed via reflection.
            */
            !isAccessibleFrom(this, initModifiers & ~Modifier.PROTECTED, callersKlass)
        ) {
            throw new IllegalAccessException();
        }

        /*
         * Allocate the object, call the default constructor, and return it.
         */
        Object res = GC.newInstance(this);
        VM.callStaticOneParm(this, indexForInit & 0xFF, res);
        return res;
    }

    /**
     * Returns the Java language modifiers for this class or interface, encoded
     * in an integer. The modifiers consist of the Java Virtual Machine's
     * constants for <code>public</code>, <code>protected</code>,
     * <code>private</code>, <code>final</code>, <code>static</code>,
     * <code>abstract</code> and <code>interface</code>; they should be decoded
     * using the methods of class <code>Modifier</code>.
     *
     * <p> If the underlying class is an array class, then its
     * <code>public</code>, <code>private</code> and <code>protected</code>
     * modifiers are the same as those of its component type.  If this
     * <code>Class</code> represents a primitive type or void, its
     * <code>public</code> modifier is always <code>true</code>, and its
     * <code>protected</code> and <code>private</code> modifiers are always
     * <code>false</code>. If this object represents an array class, a
     * primitive type or void, then its <code>final</code> modifier is always
     * <code>true</code> and its interface modifier is always
     * <code>false</code>. The values of its other modifiers are not determined
     * by this specification.
     *
     * <p> The modifier encodings are defined in <em>The Java Virtual Machine
     * Specification</em>, table 4.1.
     *
     * @return the <code>int</code> representing the modifiers for this class
     * @see     Modifier
     */
    public final int getModifiers() {
        return modifiers;
    }

    /**
     * Determines if the specified <code>Class</code> object represents an
     * interface type.
     *
     * @return  <code>true</code> if this object represents an interface;
     *          <code>false</code> otherwise.
     */
    public final boolean isInterface() {
        return Modifier.isInterface(getModifiers());
    }

    /**
     * Determines if this class represents a primitive type.
     *
     * @return   true if this class represents a primitive type
     */
    public final boolean isPrimitive() {
        return Modifier.isPrimitive(getModifiers());
    }

    /**
     * Determines if this class has a finalize() method.
     *
     * @return true if it does
     */
    public final boolean hasFinalizer() {
        return Modifier.hasFinalizer(getModifiers());
    }

    /**
     * Converts the object to a string. The string representation is the
     * string "class" or "interface", followed by a space, and then by the
     * fully qualified name of the class in the format returned by
     * <code>getName</code>.  If this <code>Class</code> object represents a
     * primitive type, this method returns the name of the primitive type.  If
     * this <code>Class</code> object represents void this method returns
     * "void".
     *
     * @return a string representation of this class object.
     */
    public final String toString() {
        return (isInterface() ? "interface " :  "class ") + getName();
    }

    /**
     * Determines if the specified <code>Object</code> is assignment-compatible
     * with the object represented by this <code>Class</code>.  This method is
     * the dynamic equivalent of the Java language <code>instanceof</code>
     * operator. The method returns <code>true</code> if the specified
     * <code>Object</code> argument is non-null and can be cast to the
     * reference type represented by this <code>Class</code> object without
     * raising a <code>ClassCastException.</code> It returns <code>false</code>
     * otherwise.
     *
     * <p> Specifically, if this <code>Class</code> object represents a
     * declared class, this method returns <code>true</code> if the specified
     * <code>Object</code> argument is an instance of the represented class (or
     * of any of its subclasses); it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents an array class, this method
     * returns <code>true</code> if the specified <code>Object</code> argument
     * can be converted to an object of the array class by an identity
     * conversion or by a widening reference conversion; it returns
     * <code>false</code> otherwise. If this <code>Class</code> object
     * represents an interface, this method returns <code>true</code> if the
     * class or any superclass of the specified <code>Object</code> argument
     * implements this interface; it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents a primitive type, this method
     * returns <code>false</code>.
     *
     * @param   obj the object to check
     * @return  true if <code>obj</code> is an instance of this class
     *
     * @since JDK1.1
     */
    public final boolean isInstance(Object obj) {
        return obj != null && isAssignableFrom(GC.getKlass(obj));
    }

    /**
     * Determines if this <code>Class</code> object represents an array class.
     *
     * @return  <code>true</code> if this object represents an array class;
     *          <code>false</code> otherwise.
     * @since   JDK1.1
     */
    public final boolean isArray() {
        return Modifier.isArray(getModifiers());
    }

    /**
     * Determines if this <code>Class</code> object represents a special class
     * that the Squawk translator and compiler convert into a primitive type. Values
     * of these types are not compatible with any other types and requires explicit
     * conversions.
     *
     * @return  <code>true</code> if this object represents a special class;
     *          <code>false</code> otherwise.
     */
    public final boolean isSquawkPrimitive() {
        return Modifier.isSquawkPrimitive(getModifiers());
    }

    /**
     * Determines if this <code>Class</code> object represents a special class
     * whose methods are converted by the Squawk translator in native methods.
     *
     * @return  <code>true</code> if this object represents a special class;
     *          <code>false</code> otherwise.
     */
    public final boolean isSquawkNative() {
        return Modifier.isSquawkNative(getModifiers());
    }

    /**
     * Determines if this <code>Class</code> object represents an array class
     * in the Squawk sense i.e. it is a Java array or some kind of string.
     *
     * @return  <code>true</code> if this object represents an array class;
     *          <code>false</code> otherwise.
     */
    final boolean isSquawkArray() {
        return isSquawkArray(this);
    }

    /**
     * Static version of {@link #isSquawkArray()} so that garbage collector can
     * invoke this method on a possibly forwarded Klass object.
     */
    static boolean isSquawkArray(Klass klass) {
        return Modifier.isSquawkArray(klass.modifiers);
    }

    /**
     * Returns the size of the elements of a Squawk array.
     *
     * @return  the size in bytes
     */
    final int getSquawkArrayComponentDataSize() {
        return getSquawkArrayComponentDataSize(this);
    }

    /**
     * Static version of {@link #getSquawkArrayComponentDataSize()} so that garbage collector can
     * invoke this method on a possibly forwarded Klass object.
     */
    static int getSquawkArrayComponentDataSize(Klass klass) {
        Assert.that(isSquawkArray(klass));
        if (klass.classID == CID.STRING_OF_BYTES) {
            return 1;
        } else if (klass.classID == CID.STRING) {
            return 2;
        } else {
            return getDataSize(getComponentType(klass));
        }
    }

    /**
     * Returns the  name of the entity (class, interface, array class,
     * primitive type, or void) represented by this <tt>Class</tt> object,
     * as a <tt>String</tt>.
     *
     * <p> If this class object represents a reference type that is not an
     * array type then the binary name of the class is returned, as specified
     * by the Java Language Specification, Second Edition.
     *
     * <p> If this class object represents a primitive type or void, then the
     * name returned is a <tt>String</tt> equal to the Java language
     * keyword corresponding to the primitive type or void.
     *
     * <p> If this class object represents a class of arrays, then the internal
     * form of the name consists of the name of the element type preceded by
     * one or more '<tt>[</tt>' characters representing the depth of the array
     * nesting.  The encoding of element type names is as follows:
     *
     * <blockquote><table summary="Element types and encodings">
     * <tr><th> Element Type <th> Encoding
     * <tr><td> boolean      <td align=center> Z
     * <tr><td> byte         <td align=center> B
     * <tr><td> char         <td align=center> C
     * <tr><td> class or interface  <td align=center> L<i>classname;</i>
     * <tr><td> double       <td align=center> D
     * <tr><td> float        <td align=center> F
     * <tr><td> int          <td align=center> I
     * <tr><td> long         <td align=center> J
     * <tr><td> short        <td align=center> S
     * </table></blockquote>
     *
     * <p> The class or interface name <i>classname</i> is the binary name of
     * the class specified above.
     *
     * <p> Examples:
     * <blockquote><pre>
     * String.class.getName()
     *     returns "java.lang.String"
     * byte.class.getName()
     *     returns "byte"
     * (new Object[3]).getClass().getName()
     *     returns "[Ljava.lang.Object;"
     * (new int[3][4][5][6][7][8][9]).getClass().getName()
     *     returns "[[[[[[[I"
     * </pre></blockquote>
     *
     * @return  the name of the class or interface
     *          represented by this object.
     */
    public String getName() {
        if (!isArray()) {
            return name;
        }
        Klass base = componentType;
        int dimensions = 1;
        while (base.isArray()) {
            base = base.getComponentType();
            dimensions++;
        }
        if (!base.isPrimitive()) {
            return name.substring(0, dimensions) + 'L' + name.substring(dimensions) + ';';
        }
        char primitive;
        switch (base.getClassID()) {
            case CID.BOOLEAN:  primitive = 'Z'; break;
            case CID.BYTE:     primitive = 'B'; break;
            case CID.CHAR:     primitive = 'C'; break;
            case CID.SHORT:    primitive = 'S'; break;
            case CID.INT:      primitive = 'I'; break;
            case CID.LONG:     primitive = 'J'; break;
            case CID.FLOAT:    primitive = 'F'; break;
            case CID.DOUBLE:   primitive = 'D'; break;
            default:           throw Assert.shouldNotReachHere();
        }
        return name.substring(0, dimensions) + primitive;
    }

    /**
     * Returns the <code>Class</code> representing the superclass of the entity
     * (class, interface, primitive type or void) represented by this
     * <code>Class</code>.  If this <code>Class</code> represents either the
     * <code>Object</code> class, an interface, a primitive type, or void, then
     * null is returned.  If this object represents an array class then the
     * <code>Class</code> object representing the <code>Object</code> class is
     * returned.
     *
     * @return the superclass of the class represented by this object.
     */
    public final Klass getSuperclass() {
        if (isInterface() || isPrimitive() || this == OBJECT) {
            return null;
        }
        return superType;
    }

    /**
     * Determines if the class or interface represented by this
     * <code>Class</code> object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * <code>Class</code> parameter. It returns <code>true</code> if so;
     * otherwise it returns <code>false</code>. If this <code>Class</code>
     * object represents a primitive type, this method returns
     * <code>true</code> if the specified <code>Class</code> parameter is
     * exactly this <code>Class</code> object; otherwise it returns
     * <code>false</code>.
     *
     * <p> Specifically, this method tests whether the type represented by the
     * specified <code>Class</code> parameter can be converted to the type
     * represented by this <code>Class</code> object via an identity conversion
     * or via a widening reference conversion. See <em>The Java Language
     * Specification</em>, sections 5.1.1 and 5.1.4 , for details.
     *
     * @param   klass   the <code>Class</code> object to be checked
     * @return  the <code>boolean</code> value indicating whether objects
     *                  of the type <code>klass</code> can be assigned to
     *                  objects of this class
     * @exception NullPointerException if the specified Class parameter is null
     * @since JDK1.1
     */
    public final boolean isAssignableFrom(Klass klass) {
        Assert.that(getState() != State.ERROR && klass.getState() != State.ERROR);

        /*
         * Quick check for equality (the most common case) or assigning to -T-.
         */
        if (this == klass || this == TOP) {
            return true;
        }

        /*
         * Check to see if 'klass' is somewhere in this class's hierarchy
         */
        if (klass.isSubtypeOf(this)) {
            return true;
        }

        /*
         * Any subclass of java.lang.Object or interface class is assignable from 'null'.
         */
        if (klass == NULL) {
            return isInterface() || isSubtypeOf(OBJECT);
        }

        /*
         * Arrays of primitives must be exactly the same type.
         */
        if (isArray()) {
            if (klass.isArray()) {
                return getComponentType().isAssignableFrom(klass.getComponentType());
            }
        } else {
            if (isInterface()) {
                return klass.isImplementorOf(this);
            }
        }

        /*
         * Otherwise there is no match.
         */
        return false;
    }

    /**
     * Finds a resource with a given name.  This method returns null if no
     * resource with this name is found.  The rules for searching
     * resources associated with a given class are profile
     * specific.
     *
     * @param name  name of the desired resource
     * @return      a <code>java.io.InputStream</code> object.
     * @since JDK1.1
     */
    public final java.io.InputStream getResourceAsStream(String name) {
        Assert.that(getState() != State.ERROR);
        try {
            if (name.length() > 0 && name.charAt(0) == '/') {
                name = name.substring(1);
            } else {
                String className = this.getName();
                int dotIndex = className.lastIndexOf('.');
                if (dotIndex >= 0) {
                    name = className.substring(0, dotIndex + 1).replace('.', '/') + name;
                }
            }
            return javax.microedition.io.Connector.openInputStream("resource:"+name);
        } catch (java.io.IOException x) {
            return null;
        }
    }


    /*---------------------------------------------------------------------------*\
     *                 Global constant for zero length class array.              *
    \*---------------------------------------------------------------------------*/

    /**
     * A zero length array of classes.
     */
    public static final Klass[] NO_CLASSES = {};


    /*---------------------------------------------------------------------------*\
     *                               Constructor                                 *
    \*---------------------------------------------------------------------------*/

    /**
     * Creates a class representing a type. If <code>name</code> starts with
     * '[' then the class being created represents an array type in which
     * case <code>componentType</code> must not be null.
     *
     * @param name           the name of the class being created
     * @param componentType  the class representing the component type or null
     *                       if the class being created does not represent an
     *                       array type
     * @param classID        the class identifier of the class being created
     */
    public Klass(String name, Klass componentType, int classID) {
        this.name = name;
        this.classID = classID;
        this.oopMapWord = UWord.zero();
        if (name.charAt(0) == '[') {
            Assert.that(componentType != null); // Component type can't be null.
            this.componentType = componentType;
            this.modifiers     = (Modifier.PUBLIC | Modifier.ARRAY | Modifier.SQUAWKARRAY | Modifier.SYNTHETIC);
            this.superType     = Klass.OBJECT;
            this.interfaces    = Klass.NO_CLASSES;
        } else {
            if (name.equals("java.lang.String") || name.equals("java.lang.StringOfBytes")) {
                this.modifiers = Modifier.SQUAWKARRAY;
            }
            this.componentType = null;
        }
    }

    /**
     * Only used by UninitializedObjectClass constructor.
     *
     * @param name       the name of the class
     * @param superType  must be {@link #UNINITIALIZED_NEW}
     */
    protected Klass(String name, Klass superType) {
        Assert.that(superType == Klass.UNINITIALIZED_NEW); // Only to be called by UninitializedObjectClass.
        this.name          = name;
        this.classID       = -1;
        this.modifiers     = Modifier.PUBLIC | Modifier.SYNTHETIC;
        this.superType     = superType;
        this.state         = State.CONVERTED;
        this.componentType = null;
        this.oopMapWord    = UWord.zero();
    }


    /*---------------------------------------------------------------------------*\
     *                                 Setters                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * Updates the modifiers for this class by setting one or more modifier
     * flags that are not currently set. This method does not unset any
     * modifier flags that are currently set.
     *
     * @param modifiers a mask of the constants defined in {@link Modifier}
     */
    public final void updateModifiers(int modifiers) {
        if ((modifiers & Modifier.SYNTHETIC) != 0) {
            state = State.CONVERTED;
            interfaces = Klass.NO_CLASSES;
        }
        this.modifiers |= modifiers;
    }


    /*---------------------------------------------------------------------------*\
     *                               Miscellaneous                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the internal class name. The names of classes in the Squawk system
     * are the same as the names returned by {@link #getName() getName} except
     * for classes representing arrays and classes representing primitive types.
     * For the former, the delimiting <code>L</code> and <code>;</code> are
     * ommitted and the internal implementation classes are returned for the
     * latter. Thus:
     *
     * <blockquote><pre>
     * (new Object[3]).getClass().getInternalName()
     * </pre></blockquote>
     *
     * returns "<code>[java.lang.Object</code>" and:
     *
     * <blockquote><pre>
     * (new int[3][4][5][6][7][8][9]).getClass().getInternalName()
     * </pre></blockquote>
     *
     * returns "<code>[[[[[[[java.lang._int_</code>". The other internal names
     * for the primitive types are as follows:
     *
     * <blockquote><pre>
     * java.lang._byte_            byte
     * java.lang._char_            char
     * java.lang._double_          double
     * java.lang._float_           float
     * java.lang._int_             int
     * java.lang._long_            long
     * java.lang._short_           short
     * java.lang._boolean_         boolean
     * java.lang.Void              void
     * </pre></blockquote>
     *
     * @return   the internal class name.
     */
    public final String getInternalName() {
        return getInternalName(this);
    }

    /**
     * Static version of {@link #getInternalName()}
     *
     * @return   the internal class name.
     */
    static String getInternalName(Klass klass) {
        return klass.name;
    }

    /**
     * Formats the names of a given array of classes into a single string
     * with each class name seperated by a space. The {@link #getName()}
     * method is used to convert each class to a name.
     *
     * @param   klasses  the classes to format
     * @return  the space separated names of the classes in <code>klasses</code>
     */
    public static String getNames(Klass[] klasses) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i != klasses.length; ++i) {
            buf.append(klasses[i].getName());
            if (i != klasses.length - 1) {
                buf .append(' ');
            }
        }
        return buf.toString();
    }

    /**
     * Gets the class identifier for this class.
     *
     * @return the class identifier for this class
     */
    public final int getClassID() {
        return getClassID(this);
    }

    /**
     * Static version of {@link #getClassID()} so that garbage collector can
     * invoke this method on a possibly forwarded Klass object.
     */
    static int getClassID(Klass klass) {
        return klass.classID;
    }

    /**
     * Returns the class representing the component type of an
     * array.  If this class does not represent an array class this method
     * returns null.
     *
     * @return the class representing the component type of this
     *         class if this class is an array
     */
    public final Klass getComponentType() {
        return getComponentType(this);
    }

    /**
     * Static version of {@link #getComponentType()} so that garbage collector can
     * invoke this method on a possibly forwarded Klass object.
     */
    static Klass getComponentType(Klass klass) {
        return klass.componentType;
    }

    /**
     * Gets the size in words of an instance of this class.
     *
     * @return the size in words of an instance of this class
     */
    int getInstanceSize() {
        return getInstanceSize(this);
    }

    /**
     * Static version of {@link #getInstanceSize()}
     *
     * @return the size in words of an instance of this class
     */
    static int getInstanceSize(Klass klass) {
        Assert.that(!Modifier.isArray(klass.modifiers) && !Modifier.isInterface(klass.modifiers) && !Modifier.isSynthetic(klass.modifiers) && !State.isBefore(klass.state, State.LOADED));
        return klass.instanceSize;
    }

    /**
     * Get the size (in words) of the static fields of this class.
     *
     * @return the number of words
     */
    int getStaticFieldsSize() {
        return staticFieldsSize;
    }

    /**
     * Get the size (in words) of the static fields of this class that are of a
     * non-primitive type.
     *
     * @return the number of words
     */
    int getRefStaticFieldsSize() {
        return getRefStaticFieldsSize(this);
    }

    /**
     * Static version of {@link #getRefStaticFieldsSize()}
     *
     * @return the number of words
     */
    static int getRefStaticFieldsSize(Klass klass) {
        return klass.refStaticFieldsSize;
    }

    /**
     * Determines if this is a public class.
     *
     * @return   true if this is a public class
     */
    public final boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    /**
     * Determines if this is an abstract class.
     *
     * @return   true if this is an abstract class
     */
    public final boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    /**
     * Determines if this class can be subclassed.
     *
     * @return  true if this class can be subclassed
     */
    public final boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    /**
     * Determines if this class is not defined by a class file. This will
     * return false for all classes representing arrays, primitive types,
     * verification-only types (e.g. TOP and LONG2) and the type for
     * <code>void</code>. For all other classes, this method will return
     * true.
     *
     * @return  true if this class is not defined by a class file
     */
    public final boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }

    /**
     * Determines if this class is a subclass of a specified class.
     *
     * @param    klass  the class to check
     * @return   true if this class is a subclass of <code>klass</code>.
     */
    final boolean isSubclassOf(Klass klass) {
        /*
         * Primitives never match non-primitives
         */
        if (this.isPrimitive() != klass.isPrimitive()) {
            return false;
        }
        Klass thisClass = this;
        while (thisClass != null) {
            if (thisClass == klass) {
                return true;
            }
            thisClass = thisClass.getSuperclass();
        }
        return false;
    }

    /**
     * Determines if this class is a subtype of a specified class. This test
     * uses the verification type hierarchy.
     *
     * @param    klass  the class to check
     * @return   true if this class is a subtype of <code>klass</code>.
     */
    static boolean isSubtypeOf(Klass thisClass, Klass klass) {
        while (thisClass != null) {
            if (thisClass == klass) {
                return true;
            }
            thisClass = thisClass.superType;
        }
        return false;
    }

    /**
     * Determines if this class is a subtype of a specified class. This test
     * uses the verification type hierarchy.
     *
     * @param    klass  the class to check
     * @return   true if this class is a subtype of <code>klass</code>.
     */
    final boolean isSubtypeOf(Klass klass) {
        return isSubtypeOf(this, klass);
    }

    /**
     * Determine if this class implements a specified class.
     *
     * @param    anInterface  the class to check
     * @return   true if <code>klass</code> is an interface class and this class implements it.
     */
    private final boolean isImplementorOf(Klass anInterface) {
        Assert.that(anInterface.isInterface());
        for (int i = 0 ; i < interfaces.length ; i++) {
            Klass iface = interfaces[i];
            if (iface == anInterface || iface.isImplementorOf(anInterface)) {
                return true;
            }
        }
        if (getSuperclass() != null) {
            return superType.isImplementorOf(anInterface);
        }
        return false;
    }

    /**
     * Return true if a given class is in the same package as this class.
     *
     * @param  klass   the class to test
     * @return true if <code>klass</code> is in the same package as this class
     */
    public /* final */ boolean isInSamePackageAs(Klass klass) {
        String name1 = this.getInternalName();
        String name2 = klass.getInternalName();
        int last1 = name1.lastIndexOf('.');
        int last2 = name2.lastIndexOf('.');
        if (last1 != last2) {
            return false;
        }
        if (last1 == -1) {
            return true;
        }
        for (int i = 0 ; i < last1 ; i++) {
            if (name1.charAt(i) != name2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether or not this class is accessible by a specified class.
     *
     * @param   klass   a class that refers to this class
     * @return  true if <code>other</code> is null or has access to this class
     */
    public final boolean isAccessibleFrom(Klass klass) {
        return (klass == null || klass == this || this.isPublic() || isInSamePackageAs(klass));
    }

    /**
     * Determines whether or not this class is a reference type.
     *
     * @return  true if it is
     */
    public final boolean isReferenceType() {
        return !isPrimitive() && this != Klass.LONG2 && this != Klass.DOUBLE2 && !isSquawkPrimitive();
    }

    /**
     * Extracts the 16-bit class number from a given 32-bit class identifier.
     * The number for a class is unique within its encapsulating suite.
     *
     * @param   classID a 32-bit class identifier
     * @return  the class number component of <code>classID</code>
     */
    public static int toClassNumber(int classID) {
        return classID & 0xFFFF;
    }

    /**
     * Extracts the 16-bit suite number from a given 32-bit class identifier.
     * The number for a suite is unique within the system.
     *
     * @param   classID a 32-bit class identifier
     * @return  the suite number component of <code>classID</code>
     */
    public static int toSuiteNumber(int classID) {
        return classID >>> 16;
    }

    /**
     * Returns the unique class identifier composed from a given suite and
     * class number.
     *
     * @param   suiteNumber  the number of a suite
     * @param   classNumber  the unique number of class within it suite
     * @return  the unique class identifier composed from
     *                       <code>suiteNumber</code> and <code>classNumber</code>
     */
    public static int toClassID(int suiteNumber, int classNumber) {
        return (suiteNumber << 16) | (classNumber & 0xFFFF);
    }

    /**
     * Gets the data size (in bytes) of the type represented by this class.
     *
     * @return the data size of a value of the type represented by this class
     */
    public final int getDataSize() {
        return getDataSize(this);
    }


    /**
     * Static version of {@link #getDataSize()} so that garbage collector can
     * invoke this method on a possibly forwarded Klass object.
     */
    static int getDataSize(Klass klass) {
        switch (getClassID(klass)) {
            case CID.BOOLEAN:
            case CID.BYTECODE:
            case CID.BYTE: {
                return 1;
            }
            case CID.CHAR:
            case CID.SHORT: {
                return 2;
            }
            case CID.DOUBLE:
            case CID.LONG: {
                return 8;
            }
            case CID.FLOAT:
            case CID.INT: {
                return 4;
            }
            default: {
                return HDR.BYTES_PER_WORD;
            }
        }
    }

    /**
     * Gets the class representing the super type of this class in the
     * verification type hierarchy.
     *
     * @return     the super type of this class
     */
    public final Klass getSuperType() {
        return superType;
    }

    /**
     * Gets the list of interfaces implemented by this class.
     *
     * @return the list of interfaces implemented by this class
     */
    final Klass[] getInterfaces() {
        return interfaces;
    }

    /**
     * Find the virtual slot number for for this class that corrisponds to the slot in an interface.
     *
     * @param iklass the interface class
     * @param islot the virtual slot of the interface
     * @return the virtual slot of this class
     */
    final int findSlot(Klass iklass, int islot) {
        Klass[] interfaces = getInterfaces();
        int     icount     = interfaces.length;
        for (int i = 0 ; i < icount ; i++) {
            if (interfaces[i] == iklass) {
                return interfaceVTableMaps[i][islot];
            }
        }
        Assert.that(superType != null);
        return superType.findSlot(iklass, islot);
    }

    /**
     * Get the vtable for virtual methods.
     *
     * @return the vtable
     */
    final Object[] getVirtualMethods() {
        return virtualMethods;
    }

    /**
     * Gets a string representation of a given field or method. If
     * <code>member</code> is a field, then the returned string will be the
     * fully qualified name (FQN) of the field's type, a space, the FQN of the
     * declaring class of the field, a period, and finally, the field's name.
     * The string returned if <code>member</code> is a method will be the same
     * as for a field (with the field type replaced by the method's return
     * type), a '(', the FQNs of the parameter types (if any) separated by a
     * ',', and finally a closing ')'. For example:
     * <p><blockquote><pre>
     *     java.lang.PrintStream java.lang.System.out
     *     int java.lang.String.indexOf(java.lang.String,int)
     * </pre></blockquote><p>
     *
     * @param   member  the field or method
     * @return  a string representation of <code>member</code>
     */
    public static String toString(Member member) {
        String s = member.getFullyQualifiedName();
        if (member instanceof Method) {
            Method method = (Method)member;
            s = method.getReturnType().getInternalName() + ' ' + s;
            Klass[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0) {
                s += "()";
            } else {
                StringBuffer buf = new StringBuffer(15);
                buf.append('(');
                for (int i = 0 ; i < parameterTypes.length ; i++) {
                    buf.append(parameterTypes[i].getInternalName());
                    if (i != parameterTypes.length - 1) {
                        buf.append(',');
                    }
                }
                buf.append(')');
                s += buf.toString();
            }
        } else {
            Field field = (Field)member;
            s = field.getType().getInternalName() + ' ' + s;
        }
        return s;
    }

    /**
     * Determines if a given field or method is accessible from a given klass.
     *
     * @param   member the field or method to test
     * @param   klass  the class accessing <code>member</code>
     * @return  true if <code>klass</code> can access <code>member</code>
     */
    public static boolean isAccessibleFrom(Member member, Klass klass) {

        if (Klass.DEBUG && Tracer.isTracing("svmload")) {
            Tracer.traceln("Checking that member: " + member.getFullyQualifiedName() + " is accessible from " + klass.getName());
        }
/*if[TRUSTED]*/
        /**
         * Check if the accessingKlass has access to the member. If not return false,
         * otherwise, continue with standard checks.
         */
        if (!TrustedKlass.classHasTrustedAccessToPublicOrProtectedMember(member, klass)) {
            return false;
        }

/*end[TRUSTED]*/

        return isAccessibleFrom(member.getDefiningClass(), member.getModifiers(), klass);
    }


    /**
     * Determines if a given field or method is accessible from a given klass.
     *
     * @param   definingClass  the class in which the member is defined
     * @param   modifiers      at least the last 8 bits of the method modifiers
     * @param   accessingKlass the class accessing the member
     * @return  true if <code>klass</code> can access <code>member</code>
     */
    public static boolean isAccessibleFrom(Klass definingClass, int modifiers, Klass accessingKlass) {


        Assert.that(Modifier.PUBLIC < 255);
        Assert.that(Modifier.PRIVATE < 255);
        Assert.that(Modifier.PROTECTED < 255);
        if (accessingKlass == null || definingClass == accessingKlass) {
            return true;
        }

        if (Modifier.isPublic(modifiers)) {
            return true;
        }

        if (Modifier.isPrivate(modifiers)) {
            return false;
        }
        if (definingClass.isInSamePackageAs(accessingKlass)) {
            return true;
        }
        if (Modifier.isProtected(modifiers)) {
            return accessingKlass.getSuperclass().isSubclassOf(definingClass);
        }
        return false;
    }



    /**
     * Determines if this class represents a type whose values occupy two
     * 32-bit words.
     *
     * @return true if this class represents a type whose values occupy two
     *              32-bit words, false otherwise
     */
    public final boolean isDoubleWord() {
        return Modifier.isDoubleWord(getModifiers());
    }


    /*---------------------------------------------------------------------------*\
     *                                   DEBUG                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * A flag that controls conditional features.
     */
    public static final boolean DEBUG = /*VAL*/false/*J2ME.DEBUG*/;

    /**
     * A flag that controls conditional 64-bitness.
     */
    public static final boolean SQUAWK_64 = /*VAL*/false/*SQUAWK_64*/;

    void dumpMethodSymbols(PrintStream out, String fileName, Object body) {
        if (Klass.DEBUG && body != null) {
            Method method = findMethod(body);
            if (method != null) {
                UWord address = Address.fromObject(method).toUWord();
                out.println("METHOD."+address.toPrimitive()+".NAME="+method);
                out.println("METHOD."+address.toPrimitive()+".FILE="+fileName);
                out.println("METHOD."+address.toPrimitive()+".LINETABLE="+Method.lineNumberTableAsString(method.getLineNumberTable()));
            }
        }
    }

    void dumpMethodSymbols(PrintStream out) {
        if (Klass.DEBUG && !isArray() && !isSynthetic()) {
            String fileName = getSourceFilePath();
            if (fileName == null) {
                fileName = "**UNKNOWN**";
            }
            for (int i = 0; i != virtualMethods.length; ++i) {
                dumpMethodSymbols(out, fileName, virtualMethods[i]);
            }
            for (int i = 0; i != staticMethods.length; ++i) {
                dumpMethodSymbols(out, fileName, staticMethods[i]);
            }
        }
    }

    /*---------------------------------------------------------------------------*\
     *                                   State                                   *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the state of this class.
     *
     * @return  the state of this class
     */
    public final int getState() {
        return state;
    }

    /**
     * Updates the state of this class. The new state must be
     * logically later than the current state.
     *
     * @param  state  the new state of this class
     */
    public final void changeState(int state) {
        Assert.that(State.isBefore(this.state, state) || state == State.ERROR);
        this.state = state;

        /*
         * Complete the vtable by inheriting methods
         */
        if (isArray()) {
            virtualMethods = superType.virtualMethods;
            Assert.that(virtualMethods != null);
        } else if (state == State.CONVERTED) {
            if (!isSynthetic() && this != Klass.OBJECT) {
                if (!isInterface()) {
                    for (int i = 0; i != virtualMethods.length; i++) {
                        if (virtualMethods[i] == null && i < superType.virtualMethods.length) {
                            virtualMethods[i] = superType.virtualMethods[i];
                        }
                        if (!isAbstract()) {
                            if (virtualMethods[i] == null && !isSquawkPrimitive()) {
                                // This is probably too eager for some TCK test in which case
                                // a stub method must be generated to throw the AbstractMethodError
                                // at the appropriate point
                                throw new LinkageError("AbstractMethodError in " + this + " for method " + i);
                            }
                        }
                    }
                }

                /*
                 * If the execution environment is not the romizer, then the methods must
                 * be converted to their special object form now
                 */
                if (!VM.isHosted()) {
                    fixupMethodTables();
                }
            }

            /*
             * Verbose trace.
             */
            if (VM.isVerbose() && !isSynthetic()) {
                System.out.println("[Loaded " + name + "]");
            }
        }
    }

    /**
     * The <code>Klass.State</code> class provides an enumerated
     * type for the different states a class may be in.
     */
    public static final class State {

        /**
         * Constant denoting the intial state of a Klass.
         */
        public final static int DEFINED = 0;

        /**
         * Constant denoting that a Klass is currently loading.
         */
        public final static int LOADING = 1;

        /**
         * Constant denoting that a Klass is loaded.
         */
        public final static int LOADED = 2;

        /**
         * Constant denoting that a Klass is currently having
         * its methods translated.
         */
        public final static int CONVERTING = 3;

        /**
         * Constant denoting that a Klass has had its methods
         * translated.
         */
        public final static int CONVERTED = 4;

        /**
         * Constant denoting that loading or converting a Klass
         * cause a linkage error.
         */
        public final static int ERROR = 5;

        /**
         * Determines if a state is before another state.
         *
         * @param   first  one of the states to compare
         * @param   second one of the states to compare
         * @return  true if <code>first</code> is before <code>second</code>
         */
        public static boolean isBefore(int first, int second) {
            return first < second;
        }

        /**
         * Determines if a state is after another state.
         *
         * @param   first  one of the states to compare
         * @param   second one of the states to compare
         * @return  true if <code>first</code> is after <code>second</code>
         */
        public static boolean isAfter(int first, int second) {
            return first > second;
        }
    }


    /*---------------------------------------------------------------------------*\
     *                                Setters                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Sets the verification hierarchy super type of this class. This method
     * should only be called when creating the bootstrap types.
     *
     * @param  superType  the verification hierarchy super type of this class
     * @see    Klass
     */
    protected final void setSuperType(Klass superType) {
        Assert.that(this.superType == null || this.superType == superType); // Cannot change super type
        this.superType = superType;
    }

    /**
     * Completes the definition of this class (apart from its bytecodes) based on the
     * information parsed from a class file.
     *
     * @param  superClass             the super class
     * @param  interfaces             the implemented interfaces
     * @param  virtualMethods         the virtual methods declared
     * @param  staticMethods          the static methods declared (including all constructors)
     * @param  instanceFields         the instance fields declared
     * @param  staticFields           the static fields declared
     * @param  sourceFile             the value of the "SourceFile" attribute
     */
    public void setClassFileDefinition(
                                        Klass             superClass,
                                        Klass[]           interfaces,
                                        ClassFileMethod[] virtualMethods,
                                        ClassFileMethod[] staticMethods,
                                        ClassFileField[]  instanceFields,
                                        ClassFileField[]  staticFields,
                                        String            sourceFile
                                      ) {

        Assert.that(this.interfaces == null); // Cannot re-define class.

        /*
         * Set the super class
         */
        if (superClass != null) {
            setSuperType(superClass);
            if (superClass.hasFinalizer()) {
                modifiers |= Modifier.HASFINALIZER;
            }
        }

        /*
         * Initialize the information pertaining to the fields.
         */
        if (!isInterface()) {
            initializeInstanceFields(instanceFields);
        }
        staticFields = initializeStaticFields(staticFields);

        /*
         * Initialize the method tables and set the offsets.
         */
        initializeVTable(virtualMethods);
        initializeSTable(staticMethods);

        /*
         * Create and install the metadata for this class.
         */
        Suite suite = SuiteManager.getSuite(toSuiteNumber(classID));
        KlassMetadata metadata = new KlassMetadata(
                                                    this,
                                                    virtualMethods,
                                                    staticMethods,
                                                    instanceFields,
                                                    staticFields,
                                                    sourceFile,
                                                    this.virtualMethods.length,
                                                    this.staticMethods.length
                                                  );
        suite.installMetadata(metadata);

        /*
         * Compute and set the interface table and interface index table.
         */
        setInterfaces(interfaces);
    }


    /*---------------------------------------------------------------------------*\
     *                        Field offset computation                           *
    \*---------------------------------------------------------------------------*/

    /**
     * Initializes the static field information for this class based on a
     * given set of class file field definitions. The {@link #staticFieldsSize}
     * and {@link #refStaticFieldsSize} values are initialized and the offset
     * of each field is computed. The offsets for all the non-primitive fields
     * are gauranteed to be lower than the offset of any primitive field.
     *
     * @param fields  class file field definitions for the static fields
     * @return a copy of the given array sorted by offsets
     * @see   Member#getOffset()
     */
    private ClassFileField[] initializeStaticFields(ClassFileField[] fields) {
        if (fields.length == 0) {
            return fields;
        }
        int primitiveOffset = 0;
        int constantOffset = 0;
        int refOffset  = 0;

        int primitiveIndex = 0;
        int constantIndex = 0;
        int refIndex  = 0;

        // Set the field offsets (which are word offsets)
        for (int i = 0; i != fields.length; ++i) {
            ClassFileField field = fields[i];
            Klass type = field.getType();
            if (type.isPrimitive()) {
                int fieldModifiers = field.getModifiers();
                if (!Modifier.hasConstant(fieldModifiers) || !Modifier.isFinal(fieldModifiers)) {
                    field.setOffset(primitiveOffset++);
                    if (!Klass.SQUAWK_64 && type.isDoubleWord()) {
                        primitiveOffset++;
                    }
                    constantIndex++;
                } else {
                    field.setOffset(constantOffset++);
                    if (!Klass.SQUAWK_64 && type.isDoubleWord()) {
                        constantOffset++;
                    }
                }
            } else {
                field.setOffset(refOffset++);
                primitiveIndex++;
                constantIndex++;
            }
        }

        // Adjust the offsets of the primitive and constant fields and sort the fields
        // into a new array by these adjusted offsets
        ClassFileField[] sortedFields = new ClassFileField[fields.length];
        constantOffset = primitiveOffset + refOffset;
        for (int i = 0; i != fields.length; ++i) {
            ClassFileField field = fields[i];
            Klass type = field.getType();
            int offset = field.getOffset();
            if (type.isPrimitive()) {
                int fieldModifiers = field.getModifiers();
                if (!Modifier.hasConstant(fieldModifiers) || !Modifier.isFinal(fieldModifiers)) {
                    offset += refOffset;
                    Assert.that(sortedFields[primitiveIndex] == null);
                    sortedFields[primitiveIndex++] = field;
                } else {
                    offset += constantOffset;
                    Assert.that(sortedFields[constantIndex] == null);
                    sortedFields[constantIndex++] = field;
                }
                field.setOffset(offset);
            } else {
                Assert.that(sortedFields[refIndex] == null);
                sortedFields[refIndex++] = field;
            }
        }

        // Initialize the 'staticFieldsSize' and 'refStaticFieldsSize' values
        staticFieldsSize    = (short)(primitiveOffset + refOffset);
        refStaticFieldsSize = (short) refOffset;

        // Ensure that the offsets all fit in 16 bits
        if ((staticFieldsSize & 0xFFFF) != (primitiveOffset + refOffset)) {
            throw new LinkageError("static fields overflow");
        }

        return sortedFields;
    }

    /**
     * Initializes the instance field information for this class based on a
     * given set of class file field definitions. The {@link #instanceSize}
     * {@link #oopMap} and {@link #oopMapWord} are initialized and the offset
     * of each field is computed.
     *
     * @param fields  class file field definitions for the instance fields
     * @see   Member#getOffset()
     */
    private void initializeInstanceFields(ClassFileField[] fields) {
        UWord bit;
        Assert.that(!this.isInterface());

        /*
         * Special handling for java.lang.Object
         */
        if (this == Klass.OBJECT) {
            Assert.that(fields.length == 0); // Object cannot have instance fields.
            Assert.that(instanceSize == 0);
            oopMap     = null;
            oopMapWord.eq(UWord.zero());
            return;
        }

        /*
         * Inherit 'instanceSize' and 'oopMap' from super class if there are no instance fields in this class
         */
        if (fields.length == 0) {
            instanceSize = superType.instanceSize;
            oopMap       = superType.oopMap;
            oopMapWord   = superType.oopMapWord;
            return;
        }

        /*
         * Set the field offsets
         */
        int offset = superType.instanceSize * HDR.BYTES_PER_WORD;
        for (int i = 0; i != fields.length; ++i) {
            ClassFileField field = fields[i];
            Klass type = field.getType();
            switch (type.getClassID()) {
                case CID.BOOLEAN:
                case CID.BYTE: {
                    field.setOffset(offset++);
                    break;
                }
                case CID.CHAR:
                case CID.SHORT: {
                    if (offset % 2 != 0) {
                        offset++;
                    }
                    field.setOffset(offset / 2);
                    offset += 2;
                    break;
                }
                case CID.DOUBLE:
                case CID.LONG: {
                    // Doubles and longs are word aligned ...
                    int modWord = offset % HDR.BYTES_PER_WORD;
                    if (modWord != 0) {
                        offset += (HDR.BYTES_PER_WORD - modWord);
                    }
                    field.setOffset(offset / HDR.BYTES_PER_WORD);
                    // ... but always occupy 8 bytes
                    offset += 8;
                    break;
                }
                case CID.INT:
                case CID.FLOAT: {
                    int mod4 = offset % 4;
                    if (mod4 != 0) {
                        offset += (4 - mod4);
                    }
                    field.setOffset(offset / 4);
                    offset += 4;
                    break;
                }
                default: {
                    // References are word aligned ...
                    int modWord = offset % HDR.BYTES_PER_WORD;
                    if (modWord != 0) {
                        offset += (HDR.BYTES_PER_WORD - modWord);
                    }
                    field.setOffset(offset / HDR.BYTES_PER_WORD);
                    // ... and occupy 4 or 8 bytes
                    offset += HDR.BYTES_PER_WORD;
                    break;
                }
            }
        }

        /*
         * Set the size of an instance (in words) of this class
         */
        instanceSize = (short)((offset + (HDR.BYTES_PER_WORD-1)) / HDR.BYTES_PER_WORD);
        if ((instanceSize & 0xFFFF) != ((offset + (HDR.BYTES_PER_WORD-1)) / HDR.BYTES_PER_WORD)) {
            throw new LinkageError("instance fields overflow");
        }

        /*
         * Create oop map
         */
        if (instanceSize > HDR.BITS_PER_WORD) {
            oopMap = new UWord[instanceSize];
            if (superType.instanceSize > HDR.BITS_PER_WORD) {
                UWord[] superOopMap = superType.oopMap;
                System.arraycopy(superOopMap, 0, oopMap, 0, superOopMap.length);
                for (int i = superOopMap.length; i < oopMap.length; ++i) {
                    oopMap[i] = UWord.zero();
                }
            } else {
                oopMap[0] = superType.oopMapWord;
                for (int i = 1; i < oopMap.length; ++i) {
                    oopMap[i] = UWord.zero();
                }
            }
        } else {
            oopMapWord = superType.oopMapWord;
        }

        /*
         * Set the bits in the map
         */
        for (int i = 0; i != fields.length; ++i) {
            ClassFileField field = fields[i];
            if (field.getType().isReferenceType()) {
                offset = field.getOffset();
                bit = UWord.fromPrimitive(1 << (offset % HDR.BITS_PER_WORD));
                if (instanceSize > HDR.BITS_PER_WORD) {
                    int index = offset / HDR.BITS_PER_WORD;
                    oopMap[index] = oopMap[index].or(bit);
                } else {
                    oopMapWord = oopMapWord.or(bit);
                }
            }
        }
    }

    /*---------------------------------------------------------------------------*\
     *            Method tables initialization and offset computation            *
    \*---------------------------------------------------------------------------*/

    /**
     * A zero-sized table of method bodies.
     */
    private static final Object[] NO_METHODS = {};

    /**
     * Creates a table for holding method bodies. This will return a shared
     * zero-sized table if <code>count</code> is zero.
     *
     * @param   count  the number of entries in the table
     * @return  the table for storing <code>count</code> method bodies
     */
    private static Object[] createMethodTable(int count) {
        return count == 0 ? NO_METHODS : new Object[count];
    }

    /**
     * Initializes the vtable for this class as well as setting the
     * vtable offset for each virtual method.
     *
     * @param methods  the virtual methods declared in the class file
     * @see   Member#getOffset()
     */
    private void initializeVTable(ClassFileMethod[] methods) {
        if (this == Klass.OBJECT || isInterface()) {
            virtualMethods = createMethodTable(methods.length);
            for (int i = 0 ; i < methods.length ; i++) {
                methods[i].setOffset(i);
                if (this == Klass.OBJECT && methods[i].isFinalize()) {
                    Assert.that(i == FINALIZE_SLOT); // Check that the offset is correct.
                }
            }
        } else if (methods.length == 0) {
            virtualMethods = superType.virtualMethods; // Inherit the super class's vtable
        } else {
            int offset = superType.virtualMethods.length;
            for (int i = 0 ; i < methods.length ; i++) {
                ClassFileMethod method = methods[i];

                /*
                 * Look for overridden method in the super class
                 */
                Method superMethod = superType.lookupMethod(
                                                             method.getName(),
                                                             method.getParameterTypes(),
                                                             method.getReturnType(),
                                                             null,
                                                             false
                                                           );
                if (superMethod != null) {
                    if (superMethod.isFinal()) {
                        throw new LinkageError("cannot override final method");
                    }

                    // This is a restriction imposed by the way Squawk treats native methods
                    if (superMethod.isNative()) {
                        throw new LinkageError("cannot override native method");
                    }

                    /*
                     * If the method can override the one in the super class then use the same vtable offset.
                     * Otherwise allocate a different one. This deals with the case where a sub-class that
                     * is in a different package overridea a package-private member.
                     */
                    if (superMethod.isAccessibleFrom(this)) {
                         method.setOffset(superMethod.getOffset());
                    } else {
                         method.setOffset(offset++);
                    }

                    /*
                     * Note if this is a finalize() method.
                     */
                    if (superMethod.getOffset() == FINALIZE_SLOT) {
                        modifiers |= Modifier.HASFINALIZER;
                    }
                } else {
                    method.setOffset(offset++);
                }
            }
            virtualMethods = createMethodTable(offset);
        }
    }

    /**
     * Initializes the table of static methods for this class as well as
     * setting the offset for each static method.
     *
     * @param methods  the static methods declared in the class file
     * @see   Member#getOffset()
     */
    private void initializeSTable(ClassFileMethod[] methods) {
        staticMethods = createMethodTable(methods.length);
        for (short i = 0; i != methods.length; ++i) {
            ClassFileMethod method = methods[i];
            method.setOffset(i);
            if (method.isInit()) {
                indexForInit = i;
                initModifiers = (byte)method.getModifiers();
            } else if (method.isClinit()) {
                indexForClinit = i;
                Assert.that(Global.needsGlobalVariables(getInternalName()) == false); // <clinit> found for class with global variables.
            } else if (method.isMain()) {
                indexForMain = i;
            }
        }
        mustClinit = setMustClinit();
    }

    /**
     * Installs the method body for a given method in this class.
     *
     * @param body     the method body
     * @param isStatic specifies whether the method is static or virtual
     */
    public void installMethodBody(MethodBody body, boolean isStatic) {
        int index = body.getIndex();
        Object[] methodTable = isStatic ? staticMethods : virtualMethods;
        Assert.that(index < methodTable.length);
        methodTable[index] = body;
        KlassMetadata klassmetadata = getMetadata();
        klassmetadata.setMethodMetadata(isStatic, index, body.getMetadata());
    }

    /**
     * Get the source file from which the class was compiled.
     *
     * @return the file name
     */
    public final String getSourceFileName() {
        return getMetadata().getSourceFileName();
    }

    /**
     * Get the source file path corresponding to the package path of this class.
     * For example, if this is class is <code>java.lang.Object</code>, and the
     * result of {@link #getSourceFileName()} is <code>"Object.java"</code> then
     * the value returned by this method is <code>"java/lang/Object.java"</code>.
     *
     * @return the source file path of this class or null if the source file is not known
     */
    final String getSourceFilePath() {
        String fileName = getSourceFileName();
        if (fileName != null) {
            int last = name.lastIndexOf('.');
            if (last >= 0) {
                fileName = name.substring(0, last+1).replace('.', '/') + fileName;
            }
        }
        return fileName;
    }

    /*---------------------------------------------------------------------------*\
     *                     Interface closure computation                         *
    \*---------------------------------------------------------------------------*/

    /**
     * A zero-length table of interface method to vtable offset mappings.
     */
    private static short[][] NO_INTERFACE_VTABLE_MAPS = {};

    /**
     * Adds the elements of <code>interfaces</code> to <code>closure</code>
     * that are not already in it. For each interface added, this method
     * recurses on the interfaces implmented by the added interface.
     *
     * @param closure     a collection of interfaces
     * @param interfaces  the array of interfaces to add to <code>closure</code>
     */
    private static void addToInterfaceClosure(Vector closure, Klass[] interfaces) {
        for (int i = 0; i != interfaces.length; ++i) {
            Klass iface = interfaces[i];
            if (!closure.contains(iface)) {
                closure.addElement(iface);
                if (iface.interfaces.length != 0) {
                    addToInterfaceClosure(closure, iface.interfaces);
                }
            }
        }
    }

    /**
     * Computes the closure of interfaces that are implemented by this class
     * excluding those that are implemented by the super class(es). The
     * {@link #interfaces} and {@link #interfaceVTableMaps} are initialized as a
     * result of this computation.
     *
     * @param   cfInterfaces  the interfaces specified in the class file
     */
    private void setInterfaces(Klass[] cfInterfaces) {
        if (isInterface() || isAbstract()) {
            interfaces = cfInterfaces;
            interfaceVTableMaps = NO_INTERFACE_VTABLE_MAPS;
            return;
        }

        /*
         * Compute the closure of interfaces implied by the class file interfaces
         */
        Vector closure = new Vector(cfInterfaces.length);
        addToInterfaceClosure(closure, cfInterfaces);

        /*
         * Add all the interfaces implemented by the abstract class(es) in
         * the super class hierarchy up until the first non-abstract class
         * in the hierarchy. This is required so that the 'interfaceSlots'
         * table for this class also includes the methods implemented by
         * abstract super classes (which have no such table).
         */
        Klass superClass = getSuperclass();
        while (superClass != null && superClass.isAbstract()) {
            addToInterfaceClosure(closure, superClass.interfaces);
            superClass = superClass.getSuperclass();
        }

        /*
         * Remove interfaces implemented by the non-abstract super class(es)
         */
        while (superClass != null) {
            if (!superClass.isAbstract()) {
                Klass[] superInterfaces = superClass.interfaces;
                for (int i = 0 ; i < superInterfaces.length ; i++) {
                    closure.removeElement(superInterfaces[i]);
                }
            }
            superClass = superClass.getSuperclass();
        }

        if (closure.isEmpty()) {
            interfaces = Klass.NO_CLASSES;
            interfaceVTableMaps = NO_INTERFACE_VTABLE_MAPS;
        } else {
            interfaces = new Klass[closure.size()];
            closure.copyInto(interfaces);
            interfaceVTableMaps = new short[closure.size()][];
            for (int i = 0 ; i < interfaces.length ; i++) {
                Klass iface = interfaces[i];
                int count = iface.getMethodCount(false);
                short[] vtableMap = interfaceVTableMaps[i] = new short[count];
                for (int index = 0 ; index < count ; index++) {
                    Method ifaceMethod = iface.getMethod(index, false);
                    Method implMethod = lookupMethod(
                                                      ifaceMethod.getName(),
                                                      ifaceMethod.getParameterTypes(),
                                                      ifaceMethod.getReturnType(),
                                                      null,
                                                      false
                                                    );
                    if (implMethod == null) {
                        throw new LinkageError("AbstractMethodError");
                    }
                    int offset = implMethod.getOffset();
                    Assert.that((offset & 0xFFFF) == offset);
                    vtableMap[index] = (short)offset;
                }
            }
        }
    }


    /*---------------------------------------------------------------------------*\
     *                        Method and field lookup                            *
    \*---------------------------------------------------------------------------*/

    /**
     * Finds the <code>Method</code> object representing a method in
     * this class's hierarchy. This method returns null if the method does
     * not exist.
     *
     * @param   name           the name of the method
     * @param   parameterTypes the parameter types of the method
     * @param   returnType     the return type of the method
     * @param   currentClass   the class context of this lookup or null if
     *                         there is no current class context
     * @param   isStatic       specifies a search for a static or virtual method
     * @return  the method that matches the given signature or null if there
     *                         is no match
     */
    public Method lookupMethod(
                                String  name,
                                Klass[] parameterTypes,
                                Klass   returnType,
                                Klass   currentClass,
                                boolean isStatic
                              ) {
        KlassMetadata metadata = SuiteManager.getMetadata(classID);
        if (metadata == null) {
            return null;
        }

        SymbolParser parser = metadata.getSymbolParser();
        int category = isStatic ? SymbolParser.STATIC_METHODS : SymbolParser.VIRTUAL_METHODS;
        int id = parser.lookupMember(category, name, parameterTypes, returnType);
        if (id != -1) {
            Method method = new Method(metadata, id);
            if (
                  currentClass == null ||
                  currentClass == this ||
                  method.isPublic()    ||
                  method.isProtected() ||
                (!method.isPrivate() && this.isInSamePackageAs(currentClass))
               ) {
                return method;
            }
        }

        /*
         * Recurse to superclass. This is done even for static method lookup
         * except when looking for <clinit> or <init>
         */
        Klass superClass = getSuperclass();
        if (superClass != null && !name.equals("<init>") && !name.equals("<clinit>")) {
            Method method = superClass.lookupMethod(name, parameterTypes, returnType, currentClass, isStatic);
            if (method != null) {
                return method;
            }
        }

        /*
         * Check implemented interfaces if this is an interface class
         */
        if (!isStatic && interfaces != null) {
            for (int i = 0; i != interfaces.length; i++) {
                Method method = interfaces[i].lookupMethod(name, parameterTypes, returnType, currentClass, false);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * Finds the <code>Field</code> object representing a field in
     * this class's hierarchy. This method returns null if the field does
     * not exist.
     *
     * @param   name      the name of the field
     * @param   type      the type of the field
     * @param   isStatic  specifies a search for a static or instance field
     * @return  the field that matches the given signature or null if there
     *                    is no match
     */
    public Field lookupField(String name, Klass type, boolean isStatic) {
        KlassMetadata metadata = SuiteManager.getMetadata(classID);
        if (metadata == null) {
            return null;
        }
        SymbolParser parser = metadata.getSymbolParser();
        final int category = isStatic ? SymbolParser.STATIC_FIELDS : SymbolParser.INSTANCE_FIELDS;
        int id = parser.lookupMember(category, name, Klass.NO_CLASSES, type);
        if (id != -1) {
            return new Field(metadata, id);
        }

        /*
         * Recurse to superclass. This is done even for static field lookup.
         */
        Klass superClass = getSuperclass();
        if (superClass != null) {
            return superClass.lookupField(name, type, isStatic);
        }
        return null;

    }

    /**
     * Gets the metadata for this class that contains the symbolic information
     * for its fields and methods. This can only be called on a non-synthetic
     * class that has been loaded.
     *
     * @return the metadata for this class
     */
   /* private*/ KlassMetadata getMetadata() {
        if (isSynthetic() || isArray()) {
            return null;
        }
        Assert.that(!State.isBefore(state, State.LOADED));
        KlassMetadata metadata = SuiteManager.getMetadata(classID);
        return metadata;
    }

    /**
     * Gets the number of fields declared by this class.
     *
     * @param   isStatic  specifies whether to count static or instance fields
     * @return  the number of static or instance fields (as determined by
     *                    <code>isStatic</code>) declared by this class
     */
    public int getFieldCount(boolean isStatic) {
        int category = isStatic ? SymbolParser.STATIC_FIELDS : SymbolParser.INSTANCE_FIELDS;
        KlassMetadata metadata = getMetadata();
        if (metadata == null) {
            return 0;
        }
        return metadata.getSymbolParser().getMemberCount(category);
    }

    /**
     * Gets a field declared by this class based on a given field table index.
     *
     * @param  index    the index of the desired field
     * @param  isStatic specifies whether or not the desired field is static
     * @return the field at <code>index</code> in the table of static or
     *                  instance fields (as determined by <code>isStatic</code>)
     *                  declared by this class
     */
    public Field getField(int index, boolean isStatic) {
        int category = isStatic ? SymbolParser.STATIC_FIELDS : SymbolParser.INSTANCE_FIELDS;
        KlassMetadata metadata = getMetadata();
        if (metadata == null) {
            return null;
        }
        int id = metadata.getSymbolParser().getMemberID(category, index);
        return new Field(metadata, id);
    }

    /**
     * Gets the number of methods declared by this class.
     *
     * @param   isStatic  specifies whether to count static or virtual methods
     * @return  the number of static or virtual methods (as determined by
     *                    <code>isStatic</code>) declared by this class
     */
    public int getMethodCount(boolean isStatic) {
        int category = isStatic ? SymbolParser.STATIC_METHODS : SymbolParser.VIRTUAL_METHODS;
        KlassMetadata metadata = getMetadata();
        if (metadata == null) {
            return 0;
        }
        return metadata.getSymbolParser().getMemberCount(category);
    }

    /**
     * Gets a method declared by this class based on a given method table index.
     *
     * @param  index    the index of the desired method
     * @param  isStatic specifies whether or not the desired method is static
     * @return the method at <code>index</code> in the table of static or
     *                  virtual methods (as determined by <code>isStatic</code>)
     *                  declared by this class
     */
    public Method getMethod(int index, boolean isStatic) {
        int category = isStatic ? SymbolParser.STATIC_METHODS : SymbolParser.VIRTUAL_METHODS;
        KlassMetadata metadata = getMetadata();
        if (metadata == null) {
            return null;
        }
        int id = metadata.getSymbolParser().getMemberID(category, index);
        return new Method(metadata, id);
    }

    /**
     * Searches for the symbolic method declaration corresponding to a given method body
     * that was defined by this class.
     *
     * @param body   the method body for which the symbolic info is requested
     * @return the symbolic info for <code>body</code> or null if it is not available
     */
    Method findMethod(Object body) {
        if (body instanceof MethodBody) {
            MethodBody mbody = (MethodBody)body;
            if (mbody.getDefiningClass() == this) {
                return mbody.getDefiningMethod();
            } else {
                return null;
            }
        }
        KlassMetadata metadata = getMetadata();
        if (metadata == null) {
            return null;
        }

        if (VM.asKlass(Unsafe.getObject(body, HDR.methodDefiningClass)) != this) {
            return null;
        }

        int methodID = -1;
        SymbolParser parser = metadata.getSymbolParser();
        for (int i = 0; i != virtualMethods.length; i++) {
            if (virtualMethods[i] == body) {
                methodID = parser.lookupMember(SymbolParser.VIRTUAL_METHODS, i, -1);
                break;
            }
        }
        if (methodID == -1) {
            for (int i = 0; i != staticMethods.length; i++) {
                if (staticMethods[i] == body) {
                    methodID = parser.lookupMember(SymbolParser.STATIC_METHODS, i, -1);
                    break;
                }
            }
        }

        if (methodID != -1) {
            return new Method(metadata, methodID);
        }
        return null;
    }

    /**
     * Test an instance oop map bit.
     *
     * @param wordIndex the word index into the instance
     * @return whether the instance word at the given index represents a reference
     */
    boolean isInstanceWordReference(int wordIndex) {
        return isInstanceWordReference(this, wordIndex);
    }

    static boolean isInstanceWordReference(Klass klass, int wordIndex) {
        Assert.that(wordIndex < getInstanceSize(klass));
        UWord word;
        if (klass.instanceSize > HDR.BITS_PER_WORD) {
            word = klass.oopMap[wordIndex / HDR.BITS_PER_WORD];
        } else {
            Assert.that(klass.oopMap == null);
            word = klass.oopMapWord;
        }
        UWord bit = UWord.fromPrimitive(1 << (wordIndex % HDR.BITS_PER_WORD));
        return word.and(bit).ne(UWord.zero());
    }


    /*---------------------------------------------------------------------------*\
     *                        Object table manipulation                          *
    \*---------------------------------------------------------------------------*/

    /**
     * Set the object table.
     *
     * @param objects the object array
     */
    public void setObjectTable(Object[] objects) {
        this.objects = objects;
    }

    /**
     * Get an object from the object table.
     *
     * @param index the index into the table
     * @return the result
     */
    public Object getObject(int index) {
        return objects[index];
    }


    /*---------------------------------------------------------------------------*\
     *                               hashcode                                    *
    \*---------------------------------------------------------------------------*/

    /**
     * Returns a hashcode for this class which is its {@link #getClassID()
     * unique identifier}.
     *
     * @return  this class's unique identifier
     */
    public final int hashCode() {
        return classID;
    }


    /*---------------------------------------------------------------------------*\
     *                          Application startup                              *
    \*---------------------------------------------------------------------------*/

    /**
     * Call this class's <code>public static void main(String[])</code> method
     * if it is defined.
     *
     * @param  args  the arguments to be passed to the invocation
     */
    void main(String[] args) {
        int index = indexForMain & 0xFF;
        if (index != 0xFF) {
            Assert.that(GC.getKlass(staticMethods[index]) == Klass.BYTECODE_ARRAY);
            VM.callStaticOneParm(this, index, args);
        } else {
            throw new Error("Class "+getName()+" has no main() method");
        }
    }


    /*---------------------------------------------------------------------------*\
     *                           Class initialization                            *
    \*---------------------------------------------------------------------------*/

    /**
     * The queue of classes currently being initialized.
     */
    static KlassInitializationState initializationQueue;

    /**
     * A constant denoting that a class is not initialized.
     */
    private final static int STATE_NOTINITIALIZED = 0;

    /**
     * A constant denoting that class initialization is in progress.
     */
    private final static int STATE_INITIALIZING = 1;

    /**
     * A constant denoting that class initialization completed successfully.
     */
    private final static int STATE_INITIALIZED = 2;

    /**
     * A constant denoting that class initialization failed.
     */
    private final static int STATE_FAILED = 3;

    /**
     * Gets the initialzation state. This will be one of the
     * <code>STATE_*</code> values.
     *
     * @return  the initialzation state of this class
     */
    private int getInitializationState() {
        if (getClassState() != null) {
            return STATE_INITIALIZED;
        }
        KlassInitializationState state = initializationQueue;
        while (state != null && state.klass != this) {
            state = state.next;
        }
        if (state == null) {
            return STATE_NOTINITIALIZED;
        }
        if (state.thread == null) {
            return STATE_FAILED;
        }
        return STATE_INITIALIZING;
    }

    /**
     * Sets the class initialization thread for this class, creating the
     * initialization state first if necessary.
     *
     * @param thread  the thread being used to initialize this class
     */
    private void setInitializationState(Thread thread) {
        KlassInitializationState first = initializationQueue;
        KlassInitializationState state = first;
        while (state != null && state.klass != this) {
            state = state.next;
        }
        if (state == null) {
            state = new KlassInitializationState();
            state.next = first;
            state.thread = thread;
            state.klass = this;
            state.classState = GC.newClassState(this);
            initializationQueue = state;
        } else {
            state.thread = thread;
        }
    }

    /**
     * Gets the thread being used to initialize this class.
     *
     * @return the thread being used to initialize this class
     */
    private Thread getInitializationThread() {
        KlassInitializationState state = initializationQueue;
        KlassInitializationState prev = null;
        while (state.klass != this) {
            prev = state;
            state = state.next;
            Assert.that(state != null);
        }
        return state.thread;
    }

    /**
     * Gets the initialzation state object for this class.
     *
     * @return the initialzation state object for this class
     */
    private Object getInitializationClassState() {
        KlassInitializationState state = initializationQueue;
        KlassInitializationState prev = null;
        while (state.klass != this) {
            prev = state;
            state = state.next;
            Assert.that(state != null);
        }
        return state.classState;
    }

    /**
     * Remove the initialization state object for this class.
     */
    private void removeInitializationState() {
        KlassInitializationState state = initializationQueue;
        KlassInitializationState prev = null;
        while (state.klass != this) {
            prev = state;
            state = state.next;
            Assert.that(state != null);
        }
        if (prev == null) {
            initializationQueue = state.next;
        } else {
            prev.next = state.next;
        }
    }

    /**
     * Convert any entries in a given method table that are
     * instances of <code>MethodBody</code> into the byte arrays with special
     * headers that are the executable form for methods.
     *
     * @param  methods  the table of methods to fixup
     */
    private void fixupMethodTable(Object[] methods) {
        for (int i = 0; i != methods.length; ++i) {
            if (methods[i] instanceof MethodBody) {
                MethodBody body = (MethodBody)methods[i];
                Assert.that(body.getDefiningClass() == this);
                methods[i] = GC.newMethod(body.getDefiningClass(), body);
            }
/*
            boolean isStatic = methods == staticMethods;
            VM.print(name);
            VM.print(isStatic ? ".smethod[" : ".vmethod[");
            VM.print(i);
            VM.print("] = ");
            VM.printAddress(Address.asAddress(methods[i]));

            Object methodBody = methods[i];
            Klass definingClass = VM.asKlass(Unsafe.getObject(methodBody, HDR.methodDefiningClass));
            Method method = definingClass.findMethod(methodBody);
            VM.print("  ");
            VM.print(method);

            VM.println("");
*/
        }
    }

    /**
     * Convert any entries in the method tables of this class that are
     * instances of <code>MethodBody</code> into the byte arrays with special
     * headers that are the executable form for methods.
     */
    final void fixupMethodTables() {
        fixupMethodTable(staticMethods);
        fixupMethodTable(virtualMethods);
    }

    /**
     * Get the class state for this class.
     *
     * @return the class state object or null if non exists
     */
    private Object getClassState() {
        return VM.getCurrentIsolate().getClassState(this);
    }

    /**
     * Initialize the class such that a new or newInstance() could be performed.
     */
    final void initialiseClass() {
        if (getState() == State.ERROR) {
            throw new NoClassDefFoundError(getName());
        }

        if (mustClinit() && getClassState() == null) {
            initializeInternal();
        }
    }

    /**
     * Initializes this class. This method implements the detailed class
     * initialization procedure described in section 2.17.5 (page 53) of
     * "The Java Virtual Machine Specification - Second Edition".
     *
     * @return the class state object
     * @see   <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/Concepts.doc.html#24237">
     *         The Java Virtual Machine Specification - Second Edition</a>
     */
    final Object initializeInternal() {
        /*
         * Test to see if there was a linkage error.
         */
        if (state == State.ERROR) {
            throw new NoClassDefFoundError(name);
        }

        /*
         * Step 1
         */
        synchronized(this) {
            /*
             * Step 2
             */
            if (getInitializationState() == STATE_INITIALIZING) {
                if (getInitializationThread() != Thread.currentThread()) {
                    do {
                        try {
                            wait();
                        } catch (InterruptedException e) {}
                    } while (getInitializationState() == STATE_INITIALIZING);
                } else {
                    /*
                     * Step 3
                     */
                    return getInitializationClassState();
                }
            }
            /*
             * Step 4
             */
            if (getInitializationState() == STATE_INITIALIZED) {
                return getClassState();
            }
            /*
             * Step 5
             */
            if (getInitializationState() == STATE_FAILED) {
                throw new NoClassDefFoundError();
            }
            /*
             * Step 6
             */
            setInitializationState(Thread.currentThread()); // state = INITIALIZING);
        }
        /*
         * Step 7
         */
        if (!isInterface()) {
            if (superType != null && superType.mustClinit() && superType.getInitializationState() != STATE_INITIALIZED) {
                try {
                    superType.initializeInternal();
                } catch(Error ex) {
                    synchronized(this) {
                        setInitializationState(null); // state = FAILED;
                        notifyAll();
                    }
                    throw ex;
                } catch(Throwable ex) {
                    VM.fatalVMError();
                }
            }
        }
        /*
         * Step 8
         */
        try {
            clinit();
            /*
             * Step 9
             */
            synchronized(this) {
                Object cs = getInitializationClassState();
                VM.getCurrentIsolate().addClassState(cs);
                removeInitializationState(); // state = INITIALIZED;
                notifyAll();
                return cs;
            }
        } catch (Throwable ex) {
            /*
             * Step 10
             */
            if (!(ex instanceof Error)) {
                ex = new ExceptionInInitializerError(ex);
            }
            /*
             * Step 11
             */
            synchronized(this) {
                setInitializationState(null); // state = FAILED;
                notifyAll();
            }
            throw (Error)ex;
        }
    }

    /**
     * Determines if class initialization must be performed
     * for this class. Class initialization is required for a class
     * if it or any of it's super classes has a <code>&lt;clinit&gt;</code>
     * method.
     *
     * @return   true if class initialization must be performed
     *           for this class; false otherwise
     */
    public boolean mustClinit() {
        return mustClinit;
    }

    /**
     * Used to set up the mustClinit field.
     */
    public boolean setMustClinit() {
        if (indexForClinit >= 0) {
            return true;
        } else if (superType == null) {
            return false;
        } else {
            return superType.setMustClinit();
        }
    }

    /**
     * Call this class's <code>&lt;clinit&gt;</code> method if it is defined.
     */
    void clinit() {
        int index = indexForClinit;
        if (index != -1) {
            // Verbose trace.
            if (VM.isVeryVerbose()) {
                  VM.print("[initializing ");
                  VM.print(isInterface() ? "interface " :  "class ");
                  VM.print(name);
                  VM.println("]");
            }
            Assert.that(GC.getKlass(staticMethods[index]).classID == CID.BYTECODE_ARRAY);
            VM.callStaticNoParm(this, index);
        }
    }


    /*---------------------------------------------------------------------------*\
     *                           Bootstrap classes                               *
    \*---------------------------------------------------------------------------*/

    /**
     * The root of the verification type hierarchy.
     *
     * @see  Klass
     */
    public final static Klass TOP;

    /**
     * The root of all single word types.
     */
    public final static Klass ONE_WORD;

    /**
     * The root of all two word types.
     */
    public final static Klass TWO_WORD;

    /**
     * The type for <code>boolean</code>.
     */
    public final static Klass BOOLEAN;

    /**
     * The type for <code>byte</code>.
     */
    public final static Klass BYTE;

    /**
     * The type for <code>char</code>.
     */
    public final static Klass CHAR;

    /**
     * The type for <code>short</code>.
     */
    public final static Klass SHORT;

    /**
     * The type for <code>int</code>.
     */
    public final static Klass INT;

    /**
     * The type for <code>float</code>.
     */
    public final static Klass FLOAT;

    /**
     * The type for <code>long</code>.
     */
    public final static Klass LONG;

    /**
     * The type for the second word of a <code>long</code> value.
     */
    public final static Klass LONG2;

    /**
     * The type for <code>double</code>.
     */
    public final static Klass DOUBLE;

    /**
     * The type for the second word of a <code>double</code> value.
     */
    public final static Klass DOUBLE2;

    /**
     * The type for <code>void</code>.
     */
    public final static Klass VOID;

    /**
     * The root type for all reference types.
     */
    public final static Klass REFERENCE;

    /**
     * The root type for all uninitialized reference types.
     */
    public final static Klass UNINITIALIZED;

    /**
     * The type for <code>this</code> in a constructor before the call to
     * the super constructor.
     */
    public final static Klass UNINITIALIZED_THIS;

    /**
     * The root of the types representing the result of the <i>new</i>
     * bytecode before it has been passed to a constructor.
     */
    public final static Klass UNINITIALIZED_NEW;

    /**
     * The type for <code>null</code>.
     */
    public final static Klass NULL;

    /**
     * The type for <code>java.lang.Object</code>.
     */
    public final static Klass OBJECT;

    /**
     * The type for <code>java.lang.String</code>.
     */
    public final static Klass STRING;

    /**
     * The type for <code>java.lang.Class</code>.
     */
    public final static Klass THROWABLE;

    /**
     * The type for <code>java.lang.Class</code>.
     */
    public final static Klass CLASS;

    /**
     * The type for <code>java.lang.Object[]</code>.
     */
    public final static Klass OBJECT_ARRAY;

    /**
     * The type for <code>java.lang.String[]</code>.
     */
    public final static Klass STRING_ARRAY;

    /**
     * The type for <code>boolean[]</code>.
     */
    public final static Klass BOOLEAN_ARRAY;

    /**
     * The type for <code>byte[]</code>.
     */
    public final static Klass BYTE_ARRAY;

    /**
     * The type for <code>char[]</code>.
     */
    public final static Klass CHAR_ARRAY;

    /**
     * The type for <code>short[]</code>.
     */
    public final static Klass SHORT_ARRAY;

    /**
     * The type for <code>int[]</code>.
     */
    public final static Klass INT_ARRAY;

    /**
     * The type for <code>float[]</code>.
     */
    public final static Klass FLOAT_ARRAY;

    /**
     * The type for <code>long[]</code>.
     */
    public final static Klass LONG_ARRAY;

    /**
     * The type for <code>double[]</code>.
     */
    public final static Klass DOUBLE_ARRAY;

    /**
     * The type for <code>java.lang.StringOfBytes</code>.
     */
    public final static Klass STRING_OF_BYTES;

    /**
     * The type for a slot in a stack chunk.
     */
    public final static Klass LOCAL;

    /**
     * The type for a stack chunk.
     */
    public final static Klass LOCAL_ARRAY;

    /**
     * The type for a class state word.
     */
    public final static Klass GLOBAL;

    /**
     * The type for a class state structure.
     */
    public final static Klass GLOBAL_ARRAY;

    /**
     * The type for a table of class state structures.
     */
    public final static Klass GLOBAL_ARRAYARRAY;

    /**
     * The type for an element of a method.
     */
    public final static Klass BYTECODE;

    /**
     * The type for an array of bytes that is a method.
     */
    public final static Klass BYTECODE_ARRAY;

    /**
     * The type for representing machine addresses.
     */
    public final static Klass ADDRESS;

    /**
     * The type for representing an array of machine addresses.
     */
    public final static Klass ADDRESS_ARRAY;

    /**
     * The type for representing unsigned machine words.
     */
    public final static Klass UWORD;

    /**
     * The type for representing an array of unsigned word addresses.
     */
    public final static Klass UWORD_ARRAY;

    /**
     * The type for representing the directed distance between two machine addresses.
     */
    public final static Klass OFFSET;

    /**
     * Container of methods for peeking and poking memory.
     */
    public final static Klass UNSAFE;

    /**
     * Finds one of the bootstrap classes, creating it if necessary.
     *
     * @param   superType  the super type of the bootstrap class
     * @param   name       the name of the class
     * @param   classID    the predefined class identifier for the class or -1 if
     *                     this class does not have a predefined class number
     * @param   modifiers  the modifiers of the class
     * @return             the created class
     */
    private static Klass boot(Klass superType, String name, int classID, int modifiers) {
        Isolate isolate = VM.getCurrentIsolate();
        Suite bootstrapSuite = isolate.getBootstrapSuite();
        Klass klass = bootstrapSuite.lookup(name);
        if (klass != null) {
            Assert.that(klass.getSuperType() == superType);
            Assert.that(classID == -1 || klass.getClassID() == classID);
            Assert.that((klass.getModifiers() & modifiers) == modifiers);
            return klass;
        }
        Assert.that(VM.isHosted());
        klass = isolate.getTranslator().findClass(name, classID, false);
        Assert.that(classID == -1 || bootstrapSuite.getKlass(classID) == klass);
        klass.setSuperType(superType);
        klass.updateModifiers(modifiers | klass.getModifiers());
        return klass;
    }

    /**
     * Initializes the constants for the bootstrap classes.
     */
    static {
        final int none            = 0;
        final int publik          = Modifier.PUBLIC;
        final int synthetic       = publik    | Modifier.SYNTHETIC;
        final int synthetic2      = synthetic | Modifier.DOUBLEWORD;
        final int primitive       = synthetic | Modifier.PRIMITIVE;
        final int primitive2      = primitive | Modifier.DOUBLEWORD;
        final int squawkarray     = publik    | Modifier.SQUAWKARRAY;
        final int squawknative    = Modifier.SQUAWKNATIVE;
        final int squawkprimitive = squawknative | Modifier.SQUAWKPRIMITIVE;

        TOP                = boot(null,          "-T-",                     -1,                    synthetic);
        ONE_WORD           = boot(TOP,           "-1-",                     -1,                    synthetic);
        TWO_WORD           = boot(TOP,           "-2-",                     -1,                    synthetic2);

        INT                = boot(ONE_WORD,      "int",                     CID.INT,               primitive);
        BOOLEAN            = boot(INT,           "boolean",                 CID.BOOLEAN,           primitive);
        BYTE               = boot(INT,           "byte",                    CID.BYTE,              primitive);
        CHAR               = boot(INT,           "char",                    CID.CHAR,              primitive);
        SHORT              = boot(INT,           "short",                   CID.SHORT,             primitive);
        FLOAT              = boot(ONE_WORD,      "float",                   CID.FLOAT,             primitive);
        LONG               = boot(TWO_WORD,      "long",                    CID.LONG,              primitive2);
        LONG2              = boot(ONE_WORD,      "-long2-",                 CID.LONG2,             primitive2);
        DOUBLE             = boot(TWO_WORD,      "double",                  CID.DOUBLE,            primitive2);
        DOUBLE2            = boot(ONE_WORD,      "-double2-",               CID.DOUBLE2,           primitive2);
        VOID               = boot(TOP,           "void",                    CID.VOID,              synthetic);

        REFERENCE          = boot(ONE_WORD,      "-ref-",                   -1,                    synthetic);
        UNINITIALIZED      = boot(REFERENCE,     "-uninit-",                -1,                    synthetic);
        UNINITIALIZED_THIS = boot(UNINITIALIZED, "-uninit_this-",           -1,                    synthetic);
        UNINITIALIZED_NEW  = boot(UNINITIALIZED, "-uninit_new-",            -1,                    synthetic);

        OBJECT             = boot(REFERENCE,     "java.lang.Object",        CID.OBJECT,            none);
        STRING             = boot(OBJECT,        "java.lang.String",        CID.STRING,            squawkarray);
        THROWABLE          = boot(OBJECT,        "java.lang.Throwable",     CID.THROWABLE,         none);
        CLASS              = boot(OBJECT,        "java.lang.Class",         CID.CLASS,             none);
        NULL               = boot(OBJECT,        "-null-",                  CID.NULL,              synthetic);

        OBJECT_ARRAY       = boot(OBJECT,        "[java.lang.Object",       CID.OBJECT_ARRAY,      synthetic);
        STRING_ARRAY       = boot(OBJECT,        "[java.lang.String",       CID.STRING_ARRAY,      synthetic);
        BOOLEAN_ARRAY      = boot(OBJECT,        "[boolean",                CID.BOOLEAN_ARRAY,     synthetic);
        BYTE_ARRAY         = boot(OBJECT,        "[byte",                   CID.BYTE_ARRAY,        synthetic);
        CHAR_ARRAY         = boot(OBJECT,        "[char",                   CID.CHAR_ARRAY,        synthetic);
        SHORT_ARRAY        = boot(OBJECT,        "[short",                  CID.SHORT_ARRAY,       synthetic);
        INT_ARRAY          = boot(OBJECT,        "[int",                    CID.INT_ARRAY,         synthetic);
        LONG_ARRAY         = boot(OBJECT,        "[long",                   CID.LONG_ARRAY,        synthetic);
        FLOAT_ARRAY        = boot(OBJECT,        "[float",                  CID.FLOAT_ARRAY,       synthetic);
        DOUBLE_ARRAY       = boot(OBJECT,        "[double",                 CID.DOUBLE_ARRAY,      synthetic);

        /*
         * Special implementation types.
         */
        STRING_OF_BYTES    = boot(STRING,        "java.lang.StringOfBytes", CID.STRING_OF_BYTES,   squawkarray);
        LOCAL              = boot(ONE_WORD,      "-local-",                 CID.LOCAL,             synthetic);
        LOCAL_ARRAY        = boot(OBJECT,        "[-local-",                CID.LOCAL_ARRAY,       synthetic);
        GLOBAL             = boot(ONE_WORD,      "-global-",                CID.GLOBAL,            synthetic);
        GLOBAL_ARRAY       = boot(OBJECT,        "[-global-",               CID.GLOBAL_ARRAY,      synthetic);
        GLOBAL_ARRAYARRAY  = boot(OBJECT,        "[[-global-",              CID.GLOBAL_ARRAYARRAY, synthetic);
        ADDRESS            = boot(OBJECT,        "java.lang.Address",       CID.ADDRESS,           squawkprimitive);
        ADDRESS_ARRAY      = boot(OBJECT,        "[java.lang.Address",      CID.ADDRESS_ARRAY,     none);
        UWORD              = boot(OBJECT,        "java.lang.UWord",         CID.UWORD,             squawkprimitive);
        UWORD_ARRAY        = boot(OBJECT,        "[java.lang.UWord",        CID.UWORD_ARRAY,       none);
        OFFSET             = boot(OBJECT,        "java.lang.Offset",        CID.OFFSET,            squawkprimitive);
        UNSAFE             = boot(OBJECT,        "java.lang.Unsafe",        CID.UNSAFE,            squawknative);

        /*
         * Methods.
         */
        BYTECODE           = boot(INT,           "-bytecode-",              CID.BYTECODE,          synthetic);
        BYTECODE_ARRAY     = boot(OBJECT,        "[-bytecode-",             CID.BYTECODE_ARRAY,    synthetic);

        if (State.isBefore(OBJECT.getState(), State.LOADED)) {
            Isolate isolate = VM.getCurrentIsolate();
            Suite bootstrapSuite = isolate.getBootstrapSuite();
            TranslatorInterface translator = isolate.getTranslator();
            for (int cno = 0 ; cno <= CID.LAST_CLASS_ID ; cno++) {
                Klass klass = bootstrapSuite.getKlass(cno);
                if (!klass.isArray() && !klass.isSynthetic()) {
                    translator.load(klass);
                }
                if (klass.isArray() && klass.virtualMethods == null) { // Doug: is this right?
                    klass.virtualMethods = klass.superType.virtualMethods;
                }
            }
        }
    }


    /*---------------------------------------------------------------------------*\
     *                           Double word types                               *
    \*---------------------------------------------------------------------------*/

    /**
     * Gets the type representing the second word of a double word type.
     *
     * @param   type  a double word type
     * @return  the type of the second word type of <code>type</code>
     */
    public static Klass getSecondWordType(Klass type) {
        if (type == DOUBLE) {
            return DOUBLE2;
        } else {
            Assert.that(type == LONG); // Is not double word type.
            return LONG2;
        }
    }

    /*---------------------------------------------------------------------------*\
     *                          KlassInitializationState                         *
    \*---------------------------------------------------------------------------*/

    private static class KlassInitializationState {
        KlassInitializationState next;
        Thread thread;
        Klass klass;
        Object classState;
    }

}
