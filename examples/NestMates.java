import java.util.Arrays;

class NestMates {
    private String name;

    class Inner {
        String sayHi() {
            name = "mamadou";
            return "Hi, " + name + "!" + concat("a", "b");
        }
    }

    interface Consumer {
        void consume();
    }
    

    private String concat(String... args) {
      return Arrays.toString(args);
   }

    public static void main(String[] args) {
        NestMates.Inner inner = new NestMates().new Inner();
        System.out.println(inner.sayHi());
    }
}