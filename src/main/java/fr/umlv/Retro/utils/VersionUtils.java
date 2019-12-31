package fr.umlv.Retro.utils;

/**
 * Provides informations about bytecode and JDK versions.
 */
public class VersionUtils {
	
	/**
	 * Transforms bytecode major version to JDK version using the following
	 * associations:
	 * 
	 * <pre>
	 *  49  5
	 *  50  6
	 *  51  7
	 *  52  8
	 *  53  9
	 *  54  10
	 *  55  11
	 *  56  12
	 *  57  13
	 *  58  14s
	 *  </pre>
	 * @param version A bytecode major version.
	 * @return a JDK version.
	 */
	public static int toJDK(int version) {
		return version - 44;
	}
	
	/**
	 * Opposite of {@link #toJDK(int) } method.
	 * @param version A JDK version.
	 * @return A bytecode major version.
	 */
	public static int toBytecode(int version) {
		return version + 44;
	}
}
