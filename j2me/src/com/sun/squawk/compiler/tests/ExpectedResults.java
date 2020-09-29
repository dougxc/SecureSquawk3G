/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.compiler.tests;
import java.io.PrintStream;


public class ExpectedResults {

    public static void main(String[] args) {
        System.out.println("*** Tests for integer type ***");
        System.out.println("Addition tests");
        System.out.println("5+2 = " + (5+2));
        System.out.println("2+5 = " + (2+5));
        System.out.println("0+(2^31-1) = " + (0+2147483647));
        System.out.println("(2^31-1)+1 = " + (1+2147483647));
        System.out.println("(2^31-1)+(2^31-1) = " + (2147483647+2147483647));
        System.out.println("(2^31-1)+(2^31-1)+2 = " + ((2+2147483647)+2147483647));
        System.out.println();

        System.out.println("Subtraction tests");
        System.out.println("5-2 = " + (5-2));
        System.out.println("2-5 = " + (2-5));
        System.out.println("(2^31-1)-(2^31-1) = " + (2147483647-2147483647));
        System.out.println("0-(2^31-1) = " + (0-2147483647));
        System.out.println("(-2^31)-(-2^31) = " + (-2147483648-(-2147483648)));
        System.out.println("10-3 = " + (10-3));
        System.out.println("3-10 = " + (3-10));
        System.out.println();

        System.out.println("Multiplication tests");
        System.out.println("5*2 = " + (5*2));
        System.out.println("0*5 = " + (0*5));
        System.out.println("(2^31-1)*1 = " + (2147483647*1));
        System.out.println("2*(2^31-1) = " + (2147483647*2));
        System.out.println("(-2^31)*(-2^31) = " + (-2147483648*-2147483648));
        System.out.println();

        System.out.println("Division tests");
        System.out.println("5/2 = " + 5/2);
        System.out.println("2/5 = " + 2/5);
        System.out.println("0/(2^31-1) = " + 0/2147483647);
        System.out.println("(-2^31)/(-2^31) = " + -2147483648/-2147483648);
        System.out.println();

        System.out.println("Remainder tests");
        System.out.println("5%2 = " + 5%2);
        System.out.println("2%5 = " + 2%5);
        System.out.println("0%(2^31-1) = " + 0%2147483647);
        System.out.println("(-2^31)%(-2^31) = " + (-2147483648%-2147483648));
        System.out.println();

        System.out.println("Expression tests");
        System.out.println("(5/3)+2 = " + ((5/3)+2));
        System.out.println("1+(3*4)-8 = " + (1+(3*4)-8));
        System.out.println("(5/3)+2 = " + ((5/3)+2));
        System.out.println("((20-6)*3+1)/2 = " + (((20-6)*3+1)/2));
        System.out.println();

        System.out.println("And tests");
        System.out.println("7&2 = " + (7&2));
        System.out.println("2&7 = " + (2&7));
        System.out.println("0&(2^31-1) = " + (0&2147483647));
        System.out.println("(-2^31)&(-2^31) = " + (-2147483648&-2147483648));
        System.out.println();

        System.out.println("Or tests");
        System.out.println("7|2 = " + (7|2));
        System.out.println("2|7 = " + (2|7));
        System.out.println("0|(2^31-1) = " + (0|2147483647));
        System.out.println("(-2^31)|(-2^31) = " + (-2147483648|-2147483648));
        System.out.println();

        System.out.println("Xor tests");
        System.out.println("7^2 = " + (7^2));
        System.out.println("2^7 = " + (2^7));
        System.out.println("0^(2^31-1) = " + (0^2147483647));
        System.out.println("(-2^31)^(-2^31) = " + (-2147483648^-2147483648));
        System.out.println();

        System.out.println("Comparison < tests");
        System.out.println("1<2 = " + (1<2));
        System.out.println("2<1 = " + (2<1));
        System.out.println("9999<9999 = " + (9999<9999));
        System.out.println("5<2 = " + (5<2));
        System.out.println("5<10 = " + (5<10));
        System.out.println("2<5 = " + (2<5));
        System.out.println("10<5 = " + (10<5));
        System.out.println();

        System.out.println("Comparison <= tests");
        System.out.println("1<=2 = " + (1<=2));
        System.out.println("2<=1 = " + (2<=1));
        System.out.println("9999<=9999 = " + (9999<=9999));
        System.out.println("5<=2 = " + (5<=2));
        System.out.println("5<=10 = " + (5<=10));
        System.out.println("2<=5 = " + (2<=5));
        System.out.println("10<=5 = " + (10<=5));
        System.out.println();

        System.out.println("Comparison == tests");
        System.out.println("1==2 = " + (1==2));
        System.out.println("2==1 = " + (2==1));
        System.out.println("9999==9999 = " + (9999==9999));
        System.out.println("5==2 = " + (5==2));
        System.out.println("5==10 = " + (5==10));
        System.out.println("2==5 = " + (2==5));
        System.out.println("10==5 = " + (10==5));
        System.out.println();

        System.out.println("Comparison != tests");
        System.out.println("1!=2 = " + (1!=2));
        System.out.println("2!=1 = " + (2!=1));
        System.out.println("9999!=9999 = " + (9999!=9999));
        System.out.println("5!=2 = " + (5!=2));
        System.out.println("5!=10 = " + (5!=10));
        System.out.println("2!=5 = " + (2!=5));
        System.out.println("10!=5 = " + (10!=5));
        System.out.println();

        System.out.println("Comparison >= tests");
        System.out.println("1>=2 = " + (1>=2));
        System.out.println("2>=1 = " + (2>=1));
        System.out.println("9999>=9999 = " + (9999>=9999));
        System.out.println("5>=2 = " + (5>=2));
        System.out.println("5>=10 = " + (5>=10));
        System.out.println("2>=5 = " + (2>=5));
        System.out.println("10>=5 = " + (10>=5));
        System.out.println();

        System.out.println("Comparison > tests");
        System.out.println("1>2 = " + (1>2));
        System.out.println("2>1 = " + (2>1));
        System.out.println("9999>9999 = " + (9999>9999));
        System.out.println("5>2 = " + (5>2));
        System.out.println("5>10 = " + (5>10));
        System.out.println("2>5 = " + (2>5));
        System.out.println("10>5 = " + (10>5));
        System.out.println();

        System.out.println("Shift left tests");
        System.out.println("5<<2 = " + (5<<2));
        System.out.println("2<<5 = " + (2<<5));
        System.out.println("0<<(2^31-1) = " + (0<<(2147483647&31)));
        System.out.println("-16<<3 = " + (-16<<3));
        System.out.println("16<<-3 = " + (16<<(-3&31)));
        System.out.println("10<<3 = " + (10<<3));
        System.out.println("3<<10 = " + (3<<10));
        System.out.println("10<<1 = " + (10<<1));
        System.out.println();

        System.out.println("Shift right unsigned tests");
        System.out.println("5>>>2 = " + (5>>>2));
        System.out.println("2>>>5 = " + (2>>>5));
        System.out.println("0>>>(2^31-1) = " + (0>>>(2147483647&31)));
        System.out.println("-16>>>3 = " + (-16>>>3));
        System.out.println("16>>>-3 = " + (16>>>(-3&31)));
        System.out.println("10>>>3 = " + (10>>>3));
        System.out.println("3>>>10 = " + (3>>>10));
        System.out.println("10>>>1 = " + (10>>>1));
        System.out.println();

        System.out.println("Shift right signed tests");
        System.out.println("5>>2 = " + (5>>2));
        System.out.println("2>>5 = " + (2>>5));
        System.out.println("0>>(2^31-1) = " + (0>>(2147483647&31)));
        System.out.println("-16>>3 = " + (-16>>3));
        System.out.println("16>>-3 = " + (16>>(-3&31)));
        System.out.println("10>>3 = " + (10>>3));
        System.out.println("3>>10 = " + (3>>10));
        System.out.println("10>>1 = " + (10>>1));
        System.out.println();

        System.out.println("Unary tests");
        System.out.println("-(8) = " + (-(8)));
        System.out.println("-(-8) = " + (-(-8)));
        System.out.println("~(8) = " + (~(8)));
        System.out.println("~(-8) = " + (~(-8)));
        System.out.println();

        System.out.println("*** Tests for long type ***");
        System.out.println("Addition tests");
        System.out.println("5+2 = " + (5L+2L));
        System.out.println("2+5 = " + (2L+5L));
        System.out.println("8000000+2 = " + (8000000+2L));
        System.out.println("5+987654321 = " + (5L+987654321));
        System.out.println("0+(2^31-1) = " + (0L+2147483647L));
        System.out.println("(2^31-1)+1 = " + (1L+2147483647L));
        System.out.println("(2^31-1)+(2^31-1) = " + (2147483647L+2147483647L));
        System.out.println("(2^31-1)+(2^31-1)+2 = " + ((2L+2147483647L)+2147483647L));
        System.out.println();

        System.out.println("Subtraction tests");
        System.out.println("5-2 = " + (5L-2L));
        System.out.println("2-5 = " + (2L-5L));
        System.out.println("8000000-2 = " + (8000000-2L));
        System.out.println("5-987654321 = " + (5L-987654321));
        System.out.println("(2^31-1)-(2^31-1) = " + (2147483647L-2147483647L));
        System.out.println("0-(2^31-1) = " + (0-2147483647L));
        System.out.println("(-2^31)-(-2^31) = " + (-2147483648L-(-2147483648L)));
        System.out.println("10-3 = " + (10L-3L));
        System.out.println("3-10 = " + (3L-10L));
        System.out.println();

        System.out.println("Multiplication tests");
        System.out.println("5*2 = " + (5L*2L));
        System.out.println("0*5 = " + (0L*5L));
        System.out.println("8000000*2 = " + (8000000*2L));
        System.out.println("5*987654321 = " + (5L*987654321));
        System.out.println("(2^31-1)*1 = " + (2147483647L*1L));
        System.out.println("2*(2^31-1) = " + (2147483647L*2L));
        System.out.println("(-2^31)*(-2^31) = " + (-2147483648L*-2147483648L));
        System.out.println("(5+5)*(2+2) = " + (10L*4L));
        System.out.println("((2^31-1)+5)*(2+2) = " + ((2147483647L+5L)*4L));
        System.out.println();

        System.out.println("Division tests");
        System.out.println("5/2 = " + 5L/2L);
        System.out.println("2/5 = " + 2L/5L);
        System.out.println("8000000/2 = " + (8000000/2L));
        System.out.println("5/987654321 = " + (5L/987654321));
        System.out.println("0/(2^31-1) = " + 0L/2147483647L);
        System.out.println("(-2^31)/(-2^31) = " + -2147483648L/-2147483648L);
        System.out.println();

        System.out.println("Remainder tests");
        System.out.println("5%2 = " + 5L%2L);
        System.out.println("2%5 = " + 2L%5L);
        System.out.println("8000000%2 = " + (8000000%2L));
        System.out.println("5%987654321 = " + (5L%987654321));
        System.out.println("0%(2^31-1) = " + 0L%2147483647L);
        System.out.println("(-2^31)%(-2^31) = " + (-2147483648L%-2147483648L));
        System.out.println();

        System.out.println("Expression tests");
        System.out.println("(5/3)+2 = " + ((5L/3L)+2L));
        System.out.println("1+(3*4)-8 = " + (1L+(3L*4L)-8L));
        System.out.println("(5/3)+2 = " + ((5L/3L)+2L));
        System.out.println("((20-6)*3+1)/2 = " + (((20L-6L)*3L+1L)/2L));
        System.out.println();

        System.out.println("And tests");
        System.out.println("7&2 = " + (7L&2L));
        System.out.println("2&7 = " + (2L&7L));
        System.out.println("8000000&2 = " + (8000000&2L));
        System.out.println("5&987654321 = " + (5L&987654321));
        System.out.println("0&(2^31-1) = " + (0L&2147483647L));
        System.out.println("(-2^31)&(-2^31) = " + (-2147483648L&-2147483648L));
        System.out.println();

        System.out.println("Or tests");
        System.out.println("7|2 = " + (7L|2L));
        System.out.println("2|7 = " + (2L|7L));
        System.out.println("8000000|2 = " + (8000000|2L));
        System.out.println("5|987654321 = " + (5L|987654321));
        System.out.println("0|(2^31-1) = " + (0L|2147483647L));
        System.out.println("(-2^31)|(-2^31) = " + (-2147483648L|-2147483648L));
        System.out.println();

        System.out.println("Xor tests");
        System.out.println("7^2 = " + (7L^2L));
        System.out.println("2^7 = " + (2L^7L));
        System.out.println("8000000^2 = " + (8000000^2L));
        System.out.println("5^987654321 = " + (5L^987654321));
        System.out.println("0^(2^31-1) = " + (0L^2147483647L));
        System.out.println("(-2^31)^(-2^31) = " + (-2147483648L^-2147483648L));
        System.out.println();

        System.out.println("Comparison < tests");
        System.out.println("1<2 = " + (1L<2L));
        System.out.println("2<1 = " + (2L<1L));
        System.out.println("8000000<2 = " + (8000000<2L));
        System.out.println("5<987654321 = " + (5L<987654321));
        System.out.println("123456789<123456789 = " + (123456789<123456789));
        System.out.println("9999<9999 = " + (9999L<9999L));
        System.out.println("5<2 = " + (5L<2L));
        System.out.println("5<10 = " + (5L<10L));
        System.out.println("2<5 = " + (2L<5L));
        System.out.println("10<5 = " + (10L<5L));
        System.out.println();

        System.out.println("Comparison <= tests");
        System.out.println("1<=2 = " + (1L<=2L));
        System.out.println("2<=1 = " + (2L<=1L));
        System.out.println("8000000<=2 = " + (8000000<=2L));
        System.out.println("5<=987654321 = " + (5L<=987654321));
        System.out.println("123456789<=123456789 = " + (123456789<=123456789));
        System.out.println("9999<=9999 = " + (9999L<=9999L));
        System.out.println("5<=2 = " + (5L<=2L));
        System.out.println("5<=10 = " + (5L<=10L));
        System.out.println("2<=5 = " + (2L<=5L));
        System.out.println("10<=5 = " + (10L<=5L));
        System.out.println();

        System.out.println("Comparison == tests");
        System.out.println("1==2 = " + (1L==2L));
        System.out.println("2==1 = " + (2L==1L));
        System.out.println("8000000==2 = " + (8000000==2L));
        System.out.println("5==987654321 = " + (5L==987654321));
        System.out.println("123456789==123456789 = " + (123456789==123456789));
        System.out.println("9999==9999 = " + (9999L==9999L));
        System.out.println("5==2 = " + (5L==2L));
        System.out.println("5==10 = " + (5L==10L));
        System.out.println("2==5 = " + (2L==5L));
        System.out.println("10==5 = " + (10L==5L));
        System.out.println();

        System.out.println("Comparison != tests");
        System.out.println("1!=2 = " + (1L!=2L));
        System.out.println("2!=1 = " + (2L!=1L));
        System.out.println("8000000!=2 = " + (8000000!=2L));
        System.out.println("5!=987654321 = " + (5L!=987654321));
        System.out.println("123456789!=123456789 = " + (123456789!=123456789));
        System.out.println("9999!=9999 = " + (9999L!=9999L));
        System.out.println("5!=2 = " + (5L!=2L));
        System.out.println("5!=10 = " + (5L!=10L));
        System.out.println("2!=5 = " + (2L!=5L));
        System.out.println("10!=5 = " + (10L!=5L));
        System.out.println();

        System.out.println("Comparison >= tests");
        System.out.println("1>=2 = " + (1L>=2L));
        System.out.println("2>=1 = " + (2L>=1L));
        System.out.println("8000000>=2 = " + (8000000>=2L));
        System.out.println("5>=987654321 = " + (5L>=987654321));
        System.out.println("123456789>=123456789 = " + (123456789>=123456789));
        System.out.println("9999>=9999 = " + (9999L>=9999L));
        System.out.println("5>=2 = " + (5L>=2L));
        System.out.println("5>=10 = " + (5L>=10L));
        System.out.println("2>=5 = " + (2L>=5L));
        System.out.println("10>=5 = " + (10L>=5L));
        System.out.println();

        System.out.println("Comparison > tests");
        System.out.println("1>2 = " + (1L>2L));
        System.out.println("2>1 = " + (2L>1L));
        System.out.println("8000000>2 = " + (8000000>2L));
        System.out.println("5>987654321 = " + (5L>987654321));
        System.out.println("123456789>123456789 = " + (123456789>123456789));
        System.out.println("9999>9999 = " + (9999L>9999L));
        System.out.println("5>2 = " + (5L>2L));
        System.out.println("5>10 = " + (5L>10L));
        System.out.println("2>5 = " + (2L>5L));
        System.out.println("10>5 = " + (10L>5L));
        System.out.println();

        System.out.println("Shift left tests");
        System.out.println("5<<2 = " + (5L<<2));
        System.out.println("2<<5 = " + (2L<<5));
        System.out.println("8000000<<2 = " + (8000000<<2));
        System.out.println("5<<987654321 = " + (5L<<(987654321&31)));
        System.out.println("0<<(2^31-1) = " + (0L<<(2147483647L&31)));
        System.out.println("-16<<3 = " + (-16L<<3));
        System.out.println("16<<-3 = " + (16L<<(-3&31)));
        System.out.println("10<<3 = " + (10L<<3));
        System.out.println("3<<10 = " + (3L<<10));
        System.out.println("10<<1 = " + (10L<<1));
        System.out.println();

        System.out.println("Shift right unsigned tests");
        System.out.println("5>>>2 = " + (5L>>>2));
        System.out.println("2>>>5 = " + (2L>>>5));
        System.out.println("8000000>>>2 = " + (8000000>>>2));
        System.out.println("5>>>987654321 = " + (5L>>>(987654321&31)));
        System.out.println("0>>>(2^31-1) = " + (0L>>>(2147483647&31)));
        System.out.println("-16>>>3 = " + (-16L>>>3));
        System.out.println("16>>>-3 = " + (16L>>>(-3&31)));
        System.out.println("10>>>3 = " + (10L>>>3));
        System.out.println("3>>>10 = " + (3L>>>10));
        System.out.println("10>>>1 = " + (10L>>>1));
        System.out.println();

        System.out.println("Shift right signed tests");
        System.out.println("5>>2 = " + (5L>>2));
        System.out.println("2>>5 = " + (2L>>5));
        System.out.println("8000000>>2 = " + (8000000>>2));
        System.out.println("5>>987654321 = " + (5L>>(987654321&31)));
        System.out.println("0>>(2^31-1) = " + (0L>>(2147483647&31)));
        System.out.println("-16>>3 = " + (-16L>>3));
        System.out.println("16>>-3 = " + (16L>>(-3&31)));
        System.out.println("10>>3 = " + (10L>>3));
        System.out.println("3>>10 = " + (3L>>10));
        System.out.println("10>>1 = " + (10L>>1));
        System.out.println();

        System.out.println("Unary tests");
        System.out.println("-(8) = " + (-(8L)));
        System.out.println("-(-8) = " + (-(-8L)));
        System.out.println("-(8000000) = " + (-(8000000)));
        System.out.println("-(-987654321) = " + (-(-987654321)));
        System.out.println("~(8) = " + (~(8L)));
        System.out.println("~(-8) = " + (~(-8L)));
        System.out.println("~(8000000) = " + (~(8000000)));
        System.out.println("~(-987654321) = " + (~(-987654321)));
        System.out.println();

     }
}
