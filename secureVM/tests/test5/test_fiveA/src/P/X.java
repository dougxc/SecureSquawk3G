//5a

package P;

//TRUSTED CLASS
class X {
  public static void main(String[] args) {
    try {
      throw new TestException("Trusted Class X");
   } catch (TestException e) {
        System.out.println(e.getMessage());
    }
    return;

  }   
}
