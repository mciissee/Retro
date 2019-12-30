import java.net.URI;
import java.net.URISyntaxException;

class TestConcat {
	private static void testConcat() throws URISyntaxException {
		var uri = new URI("http://www.u-pem.fr");
		var value = 5;
		String s = "uri " + uri + " value " + value + ".";
		System.out.println(s);
	}

	private static void testConcat2() {
		var val1 = 5L;
		var val2 = 4.0;
		var val3 = 3;
		String s = val1 + " " + val2 + " " + val3;
		System.out.println(s);
	}

	private static void testConcat3() {
		var text = "hello";
		String s = text + "" + text;
		System.out.println(s);
	}

	private static void testConcat4() {
		Object[] array = { "Hello", 5 };
		String s = array[0] + "" + array[1] + ":" + F();
		System.out.println(s);
	}

	static String F() {
		return "World";
	}

	public static void main(String[] args) throws URISyntaxException {
		testConcat();
		testConcat2();
		testConcat3();
		testConcat4();
	}
}