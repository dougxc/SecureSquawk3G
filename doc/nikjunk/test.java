/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

import java.io.*;
import java.util.*;
import javax.microedition.io.*;

public class test {
/*if[FLOATS]*/
      static double aDoubleStatic;
/*end[FLOATS]*/
      byte aByte;
      short aShort;
/*if[FLOATS]*/
      double aDouble;
/*end[FLOATS]*/
      static int initValue = 1000000;

      static long getLong() {
          return 123;
      }

      static void ignoreLong() {
          getLong();
      }

      static void oneLongLocal() {
          long aLong = 123;
      }

      static void oneLongParm(long aLong) {
      }

      static void oneLongParmAndOneLocal(long aLong) {
          long bLong = aLong;
      }

      Object getObj(String s) {
          String gg = null;
          String ff = gg;
          gg = "hello";
          return ff;
      }

      int tryCatch() {
          try {
              try {
                  return (Connector.openDataInputStream("kk")).readInt();
              } catch (IOException ioe) {
                  return -1;
              }
          } catch (Exception npe) {
              return -2;
          }
      }

      public void dupNull() {
          byte[] b1,b2;
          b1 = b2 = null;
          new String(b1);
          new String(b2);
      }

      public boolean stackOverBB(int i, String[] args) {
          i = (i == 0 ? args[0] : args[args.length - 1]).length();
          return (i % 2 == 0 ? true : false);
      }

      public static void main1(String[] args) {
          try{
              Class.forName(args[0]);
          } catch (NullPointerException npe) {
              npe = null;
          } catch (ArrayIndexOutOfBoundsException e) {
              e = null;
          } catch (ClassNotFoundException e) {
              e = null;
          }
          try{
              Class.forName(args[0]);
          } catch (ClassNotFoundException e) {
              e = null;
          } finally {
              System.out.println();
          }

      }

      public static void main(String[] args) {
          Object objArray = new Object[0];
          Object intArray = new int[0];
          if (intArray instanceof Object[]) {
              System.err.println("'intArray instanceof Object[]' returns true");
          }
          try {
              objArray = (Object[])intArray;
              System.err.println("'(Object[])intArray' succeeds");
          } catch (ClassCastException e) {
          }
      }

/*if[FLOATS]*/
      public double doDoubleAdd(double d1, double d2) {
          long l1 = (long)d1;
          long l2 = (long)d2;
          if (l1 < l2)
              d1 = d2;
          if (d1 > d2)
              d1 = d2;
          return d1 + d2 + aDouble;
      }
/*end[FLOATS]*/

      static int foo(int a, int b, int c, int d) {return 0;}
      static int bar(int a, int b) {return 0;}
      static int nestedStackUse(Vector v) {
          int var = 6;
          boolean b = v.size() > 8 || v.capacity() > 19;
          boolean b1 = v.size() > 99;
          return foo(v.size(), v.capacity(), bar(v.size(), v.hashCode()), bar(var, 6));
      }
}

class base {
    // length = 3
    // sizeInBytes = 9
    int word1;
    byte b1;
    byte b2;
    byte b3;
    byte b4;
    byte b5;

    int lookup(char ch, int i) {
        switch (ch){
        case '-':
        return 1;
        //FALLTHROUGH
        case '+':
        i++;
        }
      return i;
    }

}

class base2 extends base {
    // length = 3
    // sizeInBytes = 11
/*if[FLOATS]*/
    double d1;
/*end[FLOATS]*/
    byte b1;
    short s1;
}

class base3 extends base2 {
    Object ref1;
    byte b1;
}