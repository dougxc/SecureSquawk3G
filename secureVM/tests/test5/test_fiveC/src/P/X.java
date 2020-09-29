//5c

package P;

//TRUSTED CLASS
public class X {
  public static void Throw() throws P.TestException {
      throw new P.TestException("Trusted X throws, untrusted Catch");
  }
} 
