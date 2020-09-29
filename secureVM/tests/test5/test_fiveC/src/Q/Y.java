//5c

package Q;

//UNTRUSTED CLASS
public class Y {
  public static void main(String[] args) {
    try {
      P.X.Throw();
    } catch (Exception e) {
        System.out.println(e.getMessage());
    }
    return;
  }
}
