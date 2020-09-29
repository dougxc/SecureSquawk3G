package com.sun.squawk.traces;

import java.io.*;
import java.util.*;

// *STACKTRACESTART*:21162:*PROFILE TRACE*:invokenative_v

public class OpCodeCounter {
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("trace"), 1000000);
        String line = reader.readLine();
        Hashtable table = new Hashtable();
        int opcodes = 0;
        int keys = 0;
        while (line != null) {
            if (line.startsWith("*STACKTRACESTART*") && line.indexOf("*PROFILE TRACE*") != -1) {
                int index = line.lastIndexOf(':');
                String opcode = line.substring(index+1);
                for (int i = 0 ; i < 16 ; i++) {
                    String end = "_"+i;
                    if (opcode.endsWith(end)) {
                        opcode = opcode.substring(0, opcode.length() - end.length()) + "_n";
                    }
                }
                if (!opcode.startsWith("invokenative_")) { // ignore invokenatives
                    opcodes++;
                    Integer i = (Integer)table.get(opcode);
                    if (i == null) {
                        table.put(opcode, new Integer(1));
                        keys++;
                    } else {
                        table.put(opcode, new Integer(i.intValue()+1));
                    }
                }
            }
            line = reader.readLine();
        }

        String[] array = new String[keys];
        int pos = 0;
        for (Enumeration e = table.keys() ; e.hasMoreElements() ;) {
            String key = (String)e.nextElement();
            Integer i = (Integer)table.get(key);
            float pc = (float)(100 * i.intValue()) / opcodes;
            String out = "";
            if (pc < 10.0) {
                out += "0";
            }
            out += pc;
            while (out.length() < 15) {
                out += " ";
            }
            array[pos++] = out+key;
        }
        Arrays.sort(array);
        for (int i = 0 ; i < keys ; i++) {
            System.out.println(array[i]);
        }

    }

}