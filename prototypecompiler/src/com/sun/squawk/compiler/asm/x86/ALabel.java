/*
 * @(#)ALabel.java                       1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.squawk.compiler.asm.x86;

/**
 * Represents known or yet unknown target destinations for jumps and calls.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ALabel {

    /**
     * The next label id
     */
    private static int nextid = 0;

    /**
     * Label id. Just used for printing a trace.
     */
    private int id = nextid++;

    /**
     * The assembler that allocated this label,
     */
    private Assembler asm;

    /**
     * Encodes both the binding state and the binding position of this label.
     */
    private int pos = -1;

    /**
     * List of unbound jcc instrutions;
     */
    private UnboundJcc jccs;

    /**
     * List of unbound relocators;
     */
    private UnboundReloc rels;

    /**
     * Constructs a new unused label.
     *
     * @param asm the assember the label is bound to
     */
    public ALabel(Assembler asm) {
        this.asm = asm;
        asm.unboundLabelCount(1);
    }

    /**
     * Returns the label id.
     *
     * @return  the id
     */
     public int getId() {
        return id;
    }

    /**
     * Returns the target position or the last displacement in the chain. The
     * meaning of the actual result depends on whether the label is bound or
     * unbound.
     *
     * @return  target position or last displacement
     */
     public int getPos() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(pos != -1, "label is unused");
        }
        return pos;
    }

    /**
     * Binds this label to the specified code position. The position is stored
     * in this label for future backward jumps.
     *
     * @param  pos  target code position
     */
    public void bindTo(int pos) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(this.pos == -1, "label bound twice");
            Assert.that(pos >= 0, "illegal position");
        }

        this.pos = pos;
        asm.unboundLabelCount(-1);

        /*
         * patch in any jcc instructions
         */
        CodeBuffer code = asm.getCode();
        int save = code.getCodePos();
        while(jccs != null) {
            UnboundJcc jcc = jccs;
            jccs = jccs.next;
            code.setCodePos(jcc.position);
            asm.jcc(jcc.cc, this);
        }
        code.setCodePos(save);

        /*
         * patch in any relocators
         */
        while(rels != null) {
            UnboundReloc rel = rels;
            rels = rels.next;
            rel.relocator.setValue(pos);
        }
    }

    /**
     * Returns whether or not this label is bound.
     *
     * @return  whether or not this label is bound
     */
    public boolean isBound() {
        return pos != -1;
    }

    /**
     * Returns whether or not this label is unbound.
     *
     * @return  whether or not this label is unbound
     */
    public boolean isUnbound() {
        return pos == -1;
    }

    /**
     * Add a record for a jcc that needs to be inserted when the label is bound.
     *
     * @param  position the positon of the jcc instruction
     * @param  cc the condition code for the branch
     */
    public void addJcc(int position, int cc) {
        jccs = new UnboundJcc(position, cc, jccs);
    }

    /**
     * Add a record for a relocator that needs to be updated when the label is bound.
     *
     * @param  reloc the relocator to add
     */
    public void addRelocator(Relocator reloc) {
        if (isUnbound()) {
            rels = new UnboundReloc(reloc, rels);
        } else {
            reloc.setValue(pos);
        }
    }

}

/**
 * Private class to hold the information for an unbound jcc instruction.
 */
class UnboundJcc {
    int position;
    int cc;
    UnboundJcc next;

    UnboundJcc(int position, int cc, UnboundJcc next) {
        this.position = position;
        this.cc       = cc;
        this.next     = next;
    }
}

/**
 * Private class to hold the information for an unbound relocation information.
 */
class UnboundReloc {
    Relocator relocator;
    UnboundReloc next;

    UnboundReloc(Relocator relocator, UnboundReloc next) {
        this.relocator = relocator;
        this.next      = next;
    }
}