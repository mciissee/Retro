import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

class TestLambda {
	private int i;
	
	
	private static void testLambda() {
		var value = 40;
		var r = new TestLambda();
		r.i = 10;
		IntUnaryOperator op = (a) -> r.i + a + value;
		System.out.println(op.applyAsInt(2));
	}


	private static void testLambda1() {
		IntBinaryOperator op = (a, b) -> a + b;
		System.out.println(op.applyAsInt(2, 40));
	}

	private static void testLambda2() {
		var value = 40;
		IntUnaryOperator op = x -> x + value;
		System.out.println(op.applyAsInt(2));
	}

	private static void testLambda3() {
		double d = 40.0;
		Function<Integer, Double> fun = x -> x + d;
		System.out.println(fun.apply(2));
	}
	
	
	private static void testMethodRef() {
		IntBinaryOperator op = Integer::sum;
		System.out.println(op.applyAsInt(2, 40));
	}
	
	private static void testMethodRef2() {
		BiFunction<Integer, Integer, Integer> fun = Integer::sum;
		System.out.println(fun.apply(2, 40));
	}

	private static byte methodRef(double x) {
		return (byte) (40 + (int) x);
	}

	private static void testMethodRef3() {
		IntUnaryOperator op = TestLambda::methodRef;
		System.out.println(op.applyAsInt(2));
	}

	static void testMethodRef4() {
		var arr = new String[] { "A", "B" };
		Arrays.stream(arr).forEach(e -> {
			System.out.println(e);
		});
	}

	public static void main(String[] args) {
		testLambda();
		testLambda1();
		testLambda2();
		testLambda3();

		testMethodRef();
		testMethodRef2();
		testMethodRef3();
		testMethodRef4();
	}
}