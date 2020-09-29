/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import com.sun.squawk.compiler.*;

/**
 * Class that contains literal data that most be output after the next leave instruction.
 *
 * @author   Nik Shaylor
 */
public class LiteralData {

    /**
     * The label to be bound.
     */
    XLabel label;

    /**
     * The object to output.
     */
    Object value;

    /**
     * The next LiteralData in the queue.
     */
    LiteralData next;

    /**
     * Constructor.
     *
     * @param label the label to be bound
     * @param value object to output
     */
    public LiteralData(XLabel label, Object value) {
        this.label = label;
        this.value = value;
    }

    /**
     * Get the label.
     *
     * @return the label bound to the data
     */
    public XLabel getLabel() {
        return label;
    }

    /**
     * Get the object value.
     *
     * @return the object to output
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the next LiteralData.
     *
     * @param next the next LiteralData in the queue
     */
    public void setNext(LiteralData next) {
        this.next = next;
    }

    /**
     * Get the next LiteralData.
     *
     * @return the next LiteralData
     */
    public LiteralData getNext() {
        return next;
    }

    /**
     * Output the literal data.
     *
     * @param cgen the code generator
     */
    public void emit(CodeGenerator cgen) {
        cgen.emitLiteralData(this);
    }

}
