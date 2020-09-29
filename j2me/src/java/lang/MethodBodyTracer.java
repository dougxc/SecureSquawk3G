//if[J2ME.DEBUG]
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package java.lang;

import java.io.*;

import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;

/**
 * A tracer for method bodies.
 *
 * @author  Nik Shaylor
 */
public class MethodBodyTracer extends BytecodeTracer {

    /**
     * An alias for Tracer.getPrintStream().
     */
    private PrintStream ps = Tracer.getPrintStream();

    /**
     * Internal string buffer.
     */
    private StringBuffer out;

    /**
     * The method body being traced.
     */
    private MethodBody body;

    /**
     * The bytecode array.
     */
    private byte[] code;

/*if[TYPEMAP]*/
    /**
     * The type map describing the type of the value (if any) written to memory by each instruction in 'code'.
     */
    private byte[] typeMap;
/*end[TYPEMAP]*/

    /**
     * The offset of the last instuction.
     */
    private int lastPosition = 0;

    /**
     * The current decoding offset.
     */
    private int currentPosition = 0;

    /**
     * Constuctor.
     *
     * @param body the method body being traced
     */
    public MethodBodyTracer(MethodBody body) {
        this.body = body;
        code = body.getCode();
/*if[TYPEMAP]*/
        typeMap = body.getTypeMap();
/*end[TYPEMAP]*/
    }

    /**
     * Traces the instructions in the method.
     */
    public void traceAll() {
        if (Klass.DEBUG) {
            traceHeader();
            traceBody();
        }
    }

    /**
     * Traces the method header.
     *
     * @param types the type array
     * @param from the start element
     * @param to the end element
     */
    private void traceTypes(Klass[] types, int from, int to) {
        for (int i = from ; i <= to ; i++) {
            ps.print(" "+types[i].getName());
        }
    }

    /**
     * Traces the method header.
     *
     * @param types the type array
     * @param from the start element
     * @param to the end element
     */
    private void traceOops(Klass[] types, int from, int to) {
        for (int i = from ; i <= to ; i++) {
            ps.print(" "+(types[i].isReferenceType() ? "1" : "0"));
        }
    }

    /**
     * Traces the method header.
     */
    public void traceHeader() {

        /*
         * Trace the basic parameters.
         */
        Klass[] types = body.getTypes();
        int parameterCount = body.getParametersCount();
        int localCount = types.length - parameterCount;

        ps.println("Stack    = "+body.getMaxStack());

        if (parameterCount > 0) {
            ps.print("Parms    = {");
            traceTypes(types, 0, parameterCount-1);
            ps.println(" }");
        }

        if (localCount > 0) {
            ps.print("Locals   = {");
            traceTypes(types, parameterCount, types.length-1);
            ps.println(" }");
        }

        /*
         * Trace the oopmap.
         */
        if (localCount > 0) {
            ps.print("Oops     = {");
            traceOops(types, parameterCount, types.length-1);
            ps.println(" }");
        }

        /*
         * Trace the exception table.
         */
        ExceptionHandler[] exceptionTable = body.getExceptionTable();
        if (exceptionTable != null && exceptionTable.length > 0) {
            ps.print("Handlers = { ");
            for (int i = 0 ; i < exceptionTable.length ; i++) {
                if (i > 0) {
                    ps.print("\n             ");
                }
                ExceptionHandler handler = exceptionTable[i];
                ps.print(""+handler.getStart()+","+handler.getEnd()+"->"+handler.getHandler()+" "+handler.getKlass().getName());
            }
            ps.println(" }");
        }

    }

    /**
     * Traces the bytecodes.
     */
    private void traceBody() {
        out = new StringBuffer(1024);
        while (currentPosition < code.length) {
            traceByteCode();
        }
        ps.println(out);
    }

    /**
     * Traces the bytecodes.
     *
     * @param pos the position to stop before
     * @return a string of lines where all but the last line has a '\n' at the end and
     *         where the last line is exactly 30 characters wide.
     */
    public String traceUntil(int pos) {
        out = new StringBuffer(32);
        if (pos == -1) {
            pos = code.length;
        }
        while (currentPosition < pos) {
            traceByteCode();
        }
        if (out.length() > 0) {
            out.deleteCharAt(out.length()-1); // remove the final '\n'
        }
        return out.toString();
    }

    /**
     * Print a string.
     *
     * @param str the string
     */
    protected void print(String str) {
        String s = ""+lastPosition+":";
        while (s.length() < 5) {
            s = s + " ";
        }
/*if[TYPEMAP]*/
        if (typeMap != null) {
            String type = AddressType.getMnemonic((byte)((typeMap[lastPosition] >> AddressType.MUTATION_TYPE_SHIFT) & AddressType.TYPE_MASK));
            s = s + type + " ";
        }
/*end[TYPEMAP]*/

        s = s+" "+str;
        while (s.length() < 30) {
            s = s + " ";
        }
        out.append(s);
        out.append('\n');
        lastPosition = currentPosition;
    }

    /**
     * Get the next signed byte from the method.
     *
     * @return the value
     */
    protected int getByte() {
        return code[currentPosition++];
    }

    /**
     * Get the current bytecode offset.
     *
     * @return the value
     */
    protected int getCurrentPosition() {
        return currentPosition;
    }

}
