//test 2e
package Q;

//certificates: X.subclass
//keys: subclass, domain, cra 
//flags: not relevant

//TRUSTED CLASS
public class Y extends P.X {
  public static void main (String[] args) {
    P.X xobject = new P.X();
  }
}

