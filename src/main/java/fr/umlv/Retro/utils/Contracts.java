package fr.umlv.Retro.utils;

public class Contracts {
	/**
	 * Throws IllegalArgumentException if o is null
	 * @param o an object
	 * @param message detail message
	 * @throws IllegalArgumentException
	 */
	public static void requires(Object o, String message) throws IllegalArgumentException  {
		if (o == null) {
			throw new IllegalArgumentException(message);
		}
	}
}
