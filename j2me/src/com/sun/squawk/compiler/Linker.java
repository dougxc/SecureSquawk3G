/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.compiler;

import com.sun.squawk.util.Hashtable; // Version without synchronization
import java.util.Enumeration;
import com.sun.squawk.util.Assert;
import com.sun.squawk.os.*;

/**
 * Class to link the results of a compilation.
 *
 * @author   Nik Shaylor
 */
public class Linker implements Types {

    /**
     * Global symbol table of external references.
     */
    private static Hashtable symbolTable = new Hashtable();

    /**
     * The compiler context to relocate.
     */
    private Compiler c;

    /**
     * The relocated code address.
     */
    private int codeAddress;

    /**
     * Add a symbol to the symbol table.
     *
     * @param symbol the name of the symbol
     * @param address the address of the symbol
     */
    public static void addSymbol(String symbol, int address) {
        symbolTable.put(symbol, new Integer(address));
    }

    /**
     * Initialization.
     */
    static {
        addSymbol("printf", CSystem.lookup("printf"));
    }

    /**
     * Constructor.
     *
     * @param c the compiler context to relocate.
     */
    public Linker(Compiler c) {
        this.c = c;
    }

    /**
     * Relocate the compiled code.
     *
     * @param name the given symbolic name for the code (or null)
     * @return the entry point to the relocated code
     */
    public Linker relocate(String name) {
        Assert.that(codeAddress == 0);

        /*
         * Test that the relocation information is correct
         * by allocating a code buffer, copying the code there
         * relocating the buffer and then calling the code.
         */
        int csize   = c.getCodeSize();
        byte[] code = c.getCode();
        codeAddress = CSystem.malloc(csize);
        CSystem.copy(codeAddress, code, csize);

        int[] rinfo = c.getRelocationInfo();
        for (int i = 0 ; i < rinfo.length ; i++) {
            int type = rinfo[i] >> 24;
            int offset = rinfo[i] & 0x00FFFFFF;
            switch (type) {
            case RELOC_ABSOLUTE_INT: {
                int val = CSystem.getInt(codeAddress+offset);
                CSystem.setInt(codeAddress+offset, val+codeAddress);
                break;
            }
            case RELOC_RELATIVE_INT: {
                int val = CSystem.getInt(codeAddress+offset);
                CSystem.setInt(codeAddress+offset, val-codeAddress);
                break;
            }
            default: Assert.shouldNotReachHere();
            }
        }

        /*
         * If a symbol was defined then add it to the symbol table.
         */
        if (name != null) {
            addSymbol(name, codeAddress);
        }

        return this;
    }

    /**
     * Relocate the compiled code.
     *
     * @return the entry point to the relocated code
     */
    public Linker relocate() {
        return relocate(null);
    }

    /**
     * Resolve all unresolved symbols.
     */
    public int link() {
        /*
         * Relocate the code if it has not yet been done.
         */
        if (codeAddress == 0) {
            relocate();
        }

        /*
         * Iterate throught the fixup information fixing up the code.
         */
        Hashtable table = c.getFixupInfo();
        for (Enumeration e = table.keys() ; e.hasMoreElements() ;) {
            Integer key = (Integer)e.nextElement();
            String name = (String)table.get(key);
            int rinfo   = key.intValue();
            int type    = rinfo >> 24;
            int offset  = rinfo & 0x00FFFFFF;
            switch (type) {
                case RELOC_ABSOLUTE_INT: {
                    CSystem.setInt(codeAddress+offset, lookup(name));
                    break;
                }
                case RELOC_RELATIVE_INT: {
                    int val = lookup(name) + CSystem.getInt(codeAddress+offset);
                    CSystem.setInt(codeAddress+offset, val);
                    break;
                }
                default: Assert.shouldNotReachHere();
            }
        }
        print();
        return codeAddress;
    }

    /**
     * Print the relocation and fixup information.
     */
    public void print() {

        /**
         * Print the relocation information.
         */
        int[] rinfo = c.getRelocationInfo();
        System.out.println("Relocation info");
        for (int i = 0 ; i < rinfo.length ; i++) {
            int type = rinfo[i] >> 24;
            int offset = rinfo[i] & 0x00FFFFFF;
            switch (type) {
                case RELOC_ABSOLUTE_INT: System.out.print("  ABS   "); break;
                case RELOC_RELATIVE_INT: System.out.print("  REL   "); break;
                default: Assert.shouldNotReachHere();
            }
            System.out.println(Integer.toHexString(offset));
        }

        /**
         * Print the fixuo information.
         */
        System.out.println("Fixup info");
        Hashtable table = c.getFixupInfo();
        for (Enumeration e = table.keys() ; e.hasMoreElements() ;) {
            Integer key = (Integer)e.nextElement();
            String name = (String)table.get(key);
            int rinfo2  = key.intValue();
            int type    = rinfo2 >> 24;
            int offset  = rinfo2 & 0x00FFFFFF;
            switch (type) {
                case RELOC_ABSOLUTE_INT: System.out.print("  ABS   "); break;
                case RELOC_RELATIVE_INT: System.out.print("  REL   "); break;
                default: Assert.shouldNotReachHere();
            }
            System.out.print(Integer.toHexString(offset));
            System.out.println("\t " + name);
        }

    }

    /**
     * Lookup a symbol.
     *
     * @param symbol the symbol name
     * @return the value of the symbol
     */
    private int lookup(String symbol) {
        Integer res = (Integer)symbolTable.get(symbol);
        return res.intValue();
    }

}
