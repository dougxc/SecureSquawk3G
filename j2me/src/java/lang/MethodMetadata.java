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
 * A <code>MethodMetadata</code> instance represents all the information
 * about a method body that is not absolutely required for execution. This
 * includes the information found in the JVM LineNumberTable and
 * LocalVariableTable class file attributes.
 *
 * @author  Doug Simon
 */
public final class MethodMetadata {

    /**
     * The member ID for the method within the KlassMetadata.
     */
    private final int id;

/*if[SCOPEDLOCALVARIABLES]*/
    /**
     * The local variable table.
     *
     * @see  #getLocalVariableTable()
     */
    private final ScopedLocalVariable[] lvt;
/*end[SCOPEDLOCALVARIABLES]*/

    /**
     * The line number table.
     *
     * @see  #getLineNumberTable()
     */
    private final int [] lnt;

    /**
     * Creates a new <code>MethodMetadata</code> instance.
     *
     * @param methodID the Method the metadata is for
     * @param lnt      the table mapping instruction addresses to the
     *                 source line numbers that start at the addresses.
     *                 The table is encoded as an int array where the high
     *                 16-bits of each element is an instruction address and
     *                 the low 16-bits is the corresponding source line
     * @param lvt      the table describing the symbolic information for
     *                 the local variables in the method
     */
    public MethodMetadata(
                           int methodID,
/*if[SCOPEDLOCALVARIABLES]*/
                           ScopedLocalVariable[] lvt,
/*end[SCOPEDLOCALVARIABLES]*/
                           int[] lnt
                         ) {
        this.id  = methodID;
/*if[SCOPEDLOCALVARIABLES]*/
        this.lvt = lvt;
/*end[SCOPEDLOCALVARIABLES]*/
        this.lnt = lnt;
    }

    /**
     * Get the member ID for the method.
     *
     * @return the id
     */
    int getID() {
        return id;
    }

    /**
     * Gets the table mapping instruction addresses to the source line numbers
     * that start at the addresses. The table is encoded as an int array where
     * the high 16-bits of each element is an instruction address and the low
     * 16-bits is the corresponding source line.
     *
     * @return the line number table or null if there is no line number
     *         information for the method
     */
    public int[] getLineNumberTable() {
        return lnt;
    }

/*if[SCOPEDLOCALVARIABLES]*/
    /**
     * Gets a table describing the scope, name and type of each local variable
     * in the method.
     *
     * @return the local variable table or null if there is no local variable
     *         information for the method
     */
    public ScopedLocalVariable[] getLocalVariableTable() {
        return lvt;
    }
/*end[SCOPEDLOCALVARIABLES]*/

}
