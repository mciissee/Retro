

import java.io.Closeable;
import java.io.IOException;

class TestTryWithResource {
	private static void testTryWithResources() throws IOException {
		Closeable c = new Closeable() {
			@Override
			public void close() {
				System.out.println("close called !");
			}
		};
		try (c) {
			System.out.println("testTryWithResources");
		}
	}

	private static void testTryWithResources2() {
		var c = new Closeable() {
			@Override
			public void close() {
				System.out.println("close called !");
			}
		};
		try (c) {
			System.out.println("testTryWithResources2");
		}
	}

	private static void testNotATryWithResources() {
		var c = new Closeable() {
			@Override
			public void close() {
				System.out.println("close called !");
			}
		};
		try {
			System.out.println("testNotTryWithResources");
		} finally {
			c.close();
		}
	}

	private static void testNotATryWithResources2() {
		var c = new Closeable() {
			@Override
			public void close() {
				System.out.println("close called !");
			}
		};
		try {
			System.out.println("testNotTryWithResources2");
			c.close();
		} catch (Throwable t) {
			c.close();
			throw t;
		}
	}

	public static void main(String[] args) throws IOException {
		testTryWithResources();
		testTryWithResources2();

		testNotATryWithResources();
		testNotATryWithResources2();
	}
}