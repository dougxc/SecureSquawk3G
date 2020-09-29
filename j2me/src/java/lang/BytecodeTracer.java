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

/**
 * A tracer for method bodies.
 *
 * @author  Nik Shaylor
 */
public abstract class BytecodeTracer {

    /**
     * Constuctor.
     */
    public BytecodeTracer() {
    }

    /**
     * Get the current bytecode offset.
     *
     * @return the value
     */
    protected abstract int getCurrentPosition();

    /**
     * Print a string.
     *
     * @param str the string
     */
    protected abstract void print(String str);

    /**
     * Print an opcode and a string.
     *
     * @param opcode the opcode
     * @param str the string
     */
    private void print(int opcode, String str) {
        print(Mnemonics.OPCODES[opcode]+" "+str);
    }

    /**
     * Print an opcode.
     *
     * @param opcode the opcode
     */
    private void print(int opcode) {
        print(opcode, "");
    }

    /**
     * Get the next signed byte from the method.
     *
     * @return the value
     */
    protected abstract int getByte();

    /**
     * Get the next unsigned byte from the method.
     *
     * @return the value
     */
    int getUnsignedByte() {
        return getByte() & 0xFF;
    }

    /**
     * Get the next char from the method.
     *
     * @return the value
     */
    int getChar() {
        int ch1 = getUnsignedByte();
        int ch2 = getUnsignedByte();
        if (VM.isBigEndian()) {
            return ((ch1 << 8) + (ch2 << 0));
        } else {
            return ((ch2 << 8) + (ch1 << 0));
        }
    }

    /**
     * Get the next short from the method.
     *
     * @return the value
     */
    int getShort() {
        return (short)getChar();
    }

    /**
     * Get the next int from the method.
     *
     * @return the value
     */
    int getInt() {
        int ch1 = getUnsignedByte();
        int ch2 = getUnsignedByte();
        int ch3 = getUnsignedByte();
        int ch4 = getUnsignedByte();
        if (VM.isBigEndian()) {
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        } else {
            return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
        }
    }

    /**
     * Get the next long from the method.
     *
     * @return the value
     */
    long getLong() {
        if (VM.isBigEndian()) {
            return ((long)(getInt()) << 32) + (getInt() & 0xFFFFFFFFL);
        } else {
            return ((long)(getInt() & 0xFFFFFFFFL) + (getInt()) << 32);
        }
    }

/*if[FLOATS]*/

    /**
     * Get the next float from the method.
     *
     * @return the value
     */
    float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    /**
     * Get the next double from the method.
     *
     * @return the value
     */
    double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

/*end[FLOATS]*/

    /**
     * Trace the next bytecode.
     */
    protected void traceByteCode() {
        int opcode = getUnsignedByte();
        switch(opcode) {
            default:  {
                do_normal(opcode);
                return;
            }
            case OPC.ESCAPE: {
                print(opcode);
                do_normal(getUnsignedByte() + 256);
                return;
            }
            case OPC.WIDE_M1:
            case OPC.WIDE_0:
            case OPC.WIDE_1:
            case OPC.WIDE_SHORT:
            case OPC.WIDE_INT: {
                print(opcode);
                do_wide(opcode, getUnsignedByte());
                return;
            }
            case OPC.ESCAPE_WIDE_M1:
            case OPC.ESCAPE_WIDE_0:
            case OPC.ESCAPE_WIDE_1:
            case OPC.ESCAPE_WIDE_SHORT:
            case OPC.ESCAPE_WIDE_INT: {
                print(opcode);
                do_wide(opcode, getUnsignedByte() + 256);
                return;
            }
            case OPC.TABLESWITCH_I: {
                do_switch(opcode, 4);
                return;
            }
            case OPC.TABLESWITCH_S: {
                do_switch(opcode, 2);
                return;
            }
        }
    }

    /**
     * Process a constant bytecode
     *
     * @param opcode the  regular opcode
     * @return true if the bytecode was for a constant
     */
    private boolean do_const(int opcode) {
        switch(opcode) {
            case OPC.CONST_M1:      print(opcode);                 break;
            case OPC.CONST_BYTE:    print(opcode, ""+getByte());   break;
            case OPC.CONST_SHORT:   print(opcode, ""+getShort());  break;
            case OPC.CONST_CHAR:    print(opcode, ""+getChar());   break;
            case OPC.CONST_INT:     print(opcode, ""+getInt());    break;
            case OPC.CONST_LONG:    print(opcode, ""+getLong());   break;
/*if[FLOATS]*/
            case OPC.CONST_FLOAT:   print(opcode, ""+getFloat());  break;
            case OPC.CONST_DOUBLE:  print(opcode, ""+getDouble()); break;
/*end[FLOATS]*/
            default: return false;
        }
        return true;
    }

    /**
     * Process a normal bytecode
     *
     * @param opcode the  regular opcode
     */
    private void do_normal(int opcode) {
        if (do_const(opcode)) {
            return;
        }
        if (isBranch(opcode)) {
            print(opcode, "("+(getByte()+getCurrentPosition())+")");
        } else if (hasParm(opcode)) {
            print(opcode, ""+getUnsignedByte());
        } else {
            print(opcode);
        }
    }

    /**
     * Process a wide bytecode
     *
     * @param widecode the wide opcode
     * @param opcode the  regular opcode
     */
    private void do_wide(int widecode, int opcode) {
        int val = 0;
        switch (widecode) {
            case OPC.WIDE_M1:
            case OPC.ESCAPE_WIDE_M1: {
                val = 0xFFFFFF00 + getUnsignedByte();
                break;
            }
            case OPC.WIDE_0:
            case OPC.ESCAPE_WIDE_0: {
                val = getUnsignedByte();
                break;
            }
            case OPC.WIDE_1:
            case OPC.ESCAPE_WIDE_1: {
                val = 0x00000100 + getUnsignedByte();
                break;
            }
            case OPC.WIDE_SHORT:
            case OPC.ESCAPE_WIDE_SHORT: {
                val = getShort();
                break;
            }
            case OPC.WIDE_INT:
            case OPC.ESCAPE_WIDE_INT: {
                val = getInt();
                break;
            }
        }
        if (isBranch(opcode)) {
            print(opcode, "("+(val+getCurrentPosition())+")");
        } else {
            print(opcode, ""+val);
        }
    }


    /**
     * Get a tableswitch entry
     *
     * @param size the table entry size
     */
    private int getSwitchEntry(int size) {
        if (size == 2) {
            return getShort();
        } else {
            return getInt();
        }
    }

    /**
     * Process a tableswitch
     *
     * @param opcode the  regular opcode
     * @param size the table entry size
     */
    private void do_switch(int opcode, int size) {
        print(opcode);
        while ((getCurrentPosition() % size) != 0) {
            print("    pad  = "+getUnsignedByte());
        }
        int low  = getSwitchEntry(size);
        print("    low  = "+low);
        int high = getSwitchEntry(size);
        print("    high = "+high);
        int loc  = getSwitchEntry(size);
        int pos  = getCurrentPosition();
        print("    def  = ("+(loc+pos)+")");
        for (int i = low ; i <= high ; i++) {
            loc = getSwitchEntry(size);
            print("    ["+i+"] = ("+(loc+pos)+")");
        }
    }

    /**
     * Test to see if an opcode has a parameter
     *
     * @param opcode the opcode
     * @return true if it has
     */
    boolean hasParm(int opcode) {
        if (opcode >= OPC.FIRST_PARM_BYTECODE && opcode < OPC.FIRST_PARM_BYTECODE+OPC.PARM_BYTECODE_COUNT) {
            return true;
        }
        if (opcode >= OPC.FIRST_ESCAPE_PARM_BYTECODE && opcode < OPC.FIRST_ESCAPE_PARM_BYTECODE+OPC.ESCAPE_PARM_BYTECODE_COUNT) {
            return true;
        }
        return false;
    }

    /**
     * Test to see if an opcode is a branch
     *
     * @param opcode the opcode
     * @return true if it is
     */
    boolean isBranch(int opcode) {
        switch (opcode) {
            case OPC.GOTO:
            case OPC.IF_EQ_O:
            case OPC.IF_NE_O:
            case OPC.IF_CMPEQ_O:
            case OPC.IF_CMPNE_O:
            case OPC.IF_EQ_I:
            case OPC.IF_NE_I:
            case OPC.IF_LT_I:
            case OPC.IF_LE_I:
            case OPC.IF_GT_I:
            case OPC.IF_GE_I:
            case OPC.IF_CMPEQ_I:
            case OPC.IF_CMPNE_I:
            case OPC.IF_CMPLT_I:
            case OPC.IF_CMPLE_I:
            case OPC.IF_CMPGT_I:
            case OPC.IF_CMPGE_I:
            case OPC.IF_EQ_L:
            case OPC.IF_NE_L:
            case OPC.IF_LT_L:
            case OPC.IF_LE_L:
            case OPC.IF_GT_L:
            case OPC.IF_GE_L:
            case OPC.IF_CMPEQ_L:
            case OPC.IF_CMPNE_L:
            case OPC.IF_CMPLT_L:
            case OPC.IF_CMPLE_L:
            case OPC.IF_CMPGT_L:
            case OPC.IF_CMPGE_L:
/*if[FLOATS]*/
            case OPC.IF_EQ_F:
            case OPC.IF_NE_F:
            case OPC.IF_LT_F:
            case OPC.IF_LE_F:
            case OPC.IF_GT_F:
            case OPC.IF_GE_F:
            case OPC.IF_CMPEQ_F:
            case OPC.IF_CMPNE_F:
            case OPC.IF_CMPLT_F:
            case OPC.IF_CMPLE_F:
            case OPC.IF_CMPGT_F:
            case OPC.IF_CMPGE_F:
            case OPC.IF_EQ_D:
            case OPC.IF_NE_D:
            case OPC.IF_LT_D:
            case OPC.IF_LE_D:
            case OPC.IF_GT_D:
            case OPC.IF_GE_D:
            case OPC.IF_CMPEQ_D:
            case OPC.IF_CMPNE_D:
            case OPC.IF_CMPLT_D:
            case OPC.IF_CMPLE_D:
            case OPC.IF_CMPGT_D:
            case OPC.IF_CMPGE_D: {
                return true;
            }
/*end[FLOATS]*/
        }
        return false;
    }

}
