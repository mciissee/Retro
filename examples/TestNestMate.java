import java.util.Arrays;

class TestNestMate {
    private String name;

    class Inner {
        String sayHi() {
            name = "mamadou";
            return "Hi, " + name + "!" + concat("a", "b");
        }
    }

    private String concat(String... args) {
      return Arrays.toString(args);
   }

    public static void main(String[] args) {
        TestNestMate.Inner inner = new TestNestMate().new Inner();
        System.out.println(inner.sayHi());
    }
}