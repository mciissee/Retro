import java.util.Objects;

public class Record {

   private int a;
   private String b;
   private Object c;

   public Record(int a, String b, Object c) {
      this.a = a;
      this.b = b;
      this.c = c;
   }

   public boolean equals(Object other) {
      if (!(other instanceof Record)) {
         return false;
      }
      Record o = (Record)other;
      return a == o.a && b.equals(o.b) && c.equals(o.c);
   }
 
   public int hashCode() {
      return Record.hashCode(a, b, c);
   }

   public String toString() {
      var sb = new StringBuilder();
      sb.append("Record[");

      sb.append("a=");
      sb.append(Objects.toString(a));
      sb.append(", ");

      sb.append("b=");
      sb.append(Objects.toString(b));
      sb.append(", ");
      
      sb.append("c=");
      sb.append(Objects.toString(b));
   
      sb.append("]");
      return sb.toString();
   }

   private static int hashCode(Object... a) {
        if (a == null)
            return 0;
        int result = 1;
        for (Object element : a)
            result = 31 * result + (element == null ? 0 : element.hashCode());
        return result;
    }

   public static void main(String[] args) {
      var record = new  Record(10, "...", true);
      System.out.println(record.toString());
      System.out.println(record.hashCode());
      System.out.println(record.equals(record));
      System.out.println(record.equals(new Record(10, "...", false)));
   }
}