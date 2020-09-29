/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.listclass;

import org.apache.bcel.classfile.Visitor;
import org.apache.bcel.classfile.Deprecated;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import java.io.*;
import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.util.ClassPath;

/**
 * Disassemble Java class object into the <A HREF="http://www.cat.nyu.edu/meyer/jasmin">
 * JASMIN</A> format.
 *
 * @version $Id: JasminVisitor.java,v 1.3 2004/08/26 02:43:36 dougxc Exp $
 * @author  <A HREF="http://www.berlin.de/~markus.dahm/">M. Dahm</A>
 */
public class JasminVisitor extends org.apache.bcel.classfile.EmptyVisitor {
    private JavaClass clazz;
    private PrintWriter out;
    private String class_name;
    private ConstantPoolGen cp;

    public JasminVisitor(JavaClass clazz, OutputStream out) {
        this.clazz = clazz;
        this.out = new PrintWriter(out);
        class_name = clazz.getClassName();
        cp = new ConstantPoolGen(clazz.getConstantPool());
    }

    /**
     * Start traversal using DefaultVisitor pattern.
     */
    public void disassemble() {
        new org.apache.bcel.classfile.DescendingVisitor(clazz, this).visit();
        out.close();
    }

    public void visitJavaClass(JavaClass clazz) {
        out.println(";; Produced by JasminVisitor (BCEL)");
        out.println(";; http://bcel.sourceforge.net/");
        out.println(";; " + new Date());

        out.println(".source " + clazz.getSourceFileName());
        out.println("." + Utility.classOrInterface(clazz.getAccessFlags()) +
                    " " +
                    Utility.accessToString(clazz.getAccessFlags(), true) +
                    " " + clazz.getClassName().replace('.', '/'));
        out.println(".super " + clazz.getSuperclassName().replace('.', '/'));

        String[] interfaces = clazz.getInterfaceNames();

        for (int i = 0; i < interfaces.length; i++)
            out.println(".implements " + interfaces[i].replace('.', '/'));

        out.println();
    }

    public void visitField(org.apache.bcel.classfile.Field field) {
        out.print(".field " + Utility.accessToString(field.getAccessFlags()) +
                  " " + field.getName() + " " + field.getSignature());
        if (field.getAttributes().length == 0)
            out.println();
    }

    public void visitConstantValue(ConstantValue cv) {
        out.println(" = " + cv);
    }

    private org.apache.bcel.classfile.Method method;

    /**
     * Unfortunately Jasmin expects ".end method" after each method. Thus we've to check
     * for every of the method's attributes if it's the last one and print ".end method"
     * then.
     */
    private final void printEndMethod(Attribute attr) {
        Attribute[] attributes = method.getAttributes();

        if (attr == attributes[attributes.length - 1])
            out.println(".end method");
    }

    public void visitDeprecated(Deprecated attribute) {
        printEndMethod(attribute); }

    public void visitSynthetic(Synthetic attribute) {
        printEndMethod(attribute); }

    public void visitMethod(org.apache.bcel.classfile.Method method) {
        out.println();
        out.println(".method " + Utility.accessToString(method.getAccessFlags()) +
                    " " + method.getName() + method.getSignature());

        this.method = method; // Remember for use in subsequent visitXXX calls

        Attribute[] attributes = method.getAttributes();
        if ((attributes == null) || (attributes.length == 0))
            out.println(".end method");
    }

    public void visitExceptionTable(ExceptionTable e) {
        String[] names = e.getExceptionNames();
        for (int i = 0; i < names.length; i++)
            out.println(".throws " + names[i].replace('.', '/'));

        printEndMethod(e);
    }

    private Hashtable map;

    public void visitCode(Code code) {
        int label_counter = 0;

        out.println("    .limit stack " + code.getMaxStack());
        out.println("    .limit locals " + code.getMaxLocals());
        out.println();

        MethodGen mg = new MethodGen(method, class_name, cp);
        InstructionList il = mg.getInstructionList();
        InstructionHandle[] ihs = il.getInstructionHandles();

        /* Pass 1: Give all referenced instruction handles a symbolic name, i.e. a
         * label.
         */
        map = new Hashtable();

        for (int i = 0; i < ihs.length; i++) {
            if (ihs[i] instanceof BranchHandle) {
                BranchInstruction bi = (BranchInstruction)ihs[i].getInstruction();

                if (bi instanceof Select) { // Special cases LOOKUPSWITCH and TABLESWITCH
                    InstructionHandle[] targets = ((Select)bi).getTargets();

                    for (int j = 0; j < targets.length; j++) {
                        put(targets[j], "label_" + label_counter++ +":");
                    }
                }

                InstructionHandle ih = bi.getTarget();
                put(ih, "label_" + label_counter++ +":");
            }
        }

        LocalVariableGen[] lvs = mg.getLocalVariables();
        for (int i = 0; i < lvs.length; i++) {
            InstructionHandle ih = lvs[i].getStart();
            put(ih, "label_" + label_counter++ +":");
            ih = lvs[i].getEnd();
            put(ih, "label_" + label_counter++ +":");
        }

        CodeExceptionGen[] ehs = mg.getExceptionHandlers();

        for (int i = 0; i < ehs.length; i++) {
            CodeExceptionGen c = ehs[i];
            InstructionHandle ih = c.getStartPC();

            put(ih, "label_" + label_counter++ +":");
            ih = c.getEndPC();
            put(ih, "label_" + label_counter++ +":");
            ih = c.getHandlerPC();
            put(ih, "label_" + label_counter++ +":");
        }

        LineNumberGen[] lns = mg.getLineNumbers();
        for (int i = 0; i < lns.length; i++) {
            InstructionHandle ih = lns[i].getInstruction();
            put(ih, ".line " + lns[i].getSourceLine());
        }

        Attribute[] code_attrs = code.getAttributes();
        StackMapEntry[] stackMap = null;
        int stackMapIndex = 0;
        for (int i = 0; i != code_attrs.length; i++) {
            Attribute code_attr = code_attrs[i];
            String attrName = ((ConstantUtf8)cp.getConstant(code_attr.getNameIndex())).getBytes();
            if(attrName.equals("StackMap"))
                stackMap = ((StackMap)code_attr).getStackMap();
        }


        /* Pass 2: Output code.
         */
        if (lvs.length != 0) {
            for (int i = 0; i < lvs.length; i++) {
                LocalVariableGen l = lvs[i];
                out.println("    .var " + l.getIndex() + " is " + l.getName() + " " +
                            l.getType().getSignature() +
                            " from " + get(l.getStart()) +
                            " to " + get(l.getEnd()));
            }
        }

        out.println();

        for (int i = 0; i < ihs.length; i++) {
            InstructionHandle ih = ihs[i];
            Instruction inst = ih.getInstruction();

            String str = (String)map.get(ih);
            if (str != null) {
                out.println(str);
            }

            if (stackMap != null && stackMapIndex < stackMap.length) {
                int ip = ih.getPosition();
                StackMapEntry entry = stackMap[stackMapIndex];
                if (entry.getByteCodeOffset() == ip) {
                    out.println("; stack_map " + entry);
                    stackMapIndex++;
                }
            }

            if (inst instanceof BranchInstruction) {
                if (inst instanceof Select) { // Special cases LOOKUPSWITCH and TABLESWITCH
                    Select s = (Select)inst;
                    int[] matchs = s.getMatchs();
                    InstructionHandle[] targets = s.getTargets();

                    if (s instanceof TABLESWITCH) {
                        out.println("    tableswitch " + matchs[0] + " " +
                                    matchs[matchs.length - 1]);

                        for (int j = 0; j < targets.length; j++)
                            out.println("        " + get(targets[j]));

                    } else { // LOOKUPSWITCH
                        out.println("    lookupswitch ");

                        for (int j = 0; j < targets.length; j++)
                            out.println("        " + matchs[j] + " : " +
                                        get(targets[j]));
                    }

                    out.println("        default: " + get(s.getTarget())); // Applies for both
                } else {
                    BranchInstruction bi = (BranchInstruction)inst;
                    ih = bi.getTarget();
                    str = get(ih);
                    out.println("    " + Constants.OPCODE_NAMES[bi.getOpcode()] +
                                " " + str);
                }
            } else
                out.println("    " + inst.toString(cp.getConstantPool()));
        }

        out.println();

        for (int i = 0; i < ehs.length; i++) {
            CodeExceptionGen c = ehs[i];
            ObjectType caught = c.getCatchType();
            String class_name = (caught == null) ? // catch any exception, used when compiling finally
                "all" : caught.getClassName().replace('.', '/');

            out.println(".catch " + class_name + " from " +
                        get(c.getStartPC()) + " to " + get(c.getEndPC()) +
                        " using " + get(c.getHandlerPC()));
        }

        printEndMethod(code);
    }

    private final String get(InstructionHandle ih) {
        String str = new StringTokenizer((String)map.get(ih), "\n").nextToken();
        return str.substring(0, str.length() - 1);
    }

    private final void put(InstructionHandle ih, String line) {
        String str = (String)map.get(ih);

        if (str == null)
            map.put(ih, line);
        else {
            if (line.startsWith("label_1") || str.endsWith(line)) // Already have a label in the map
                return;

            map.put(ih, str + "\n" + line); // append
        }
    }
}

