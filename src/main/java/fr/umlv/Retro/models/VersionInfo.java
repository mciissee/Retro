package fr.umlv.Retro.models;

/**
 * Provides informations about byte code versions.
 */
public class VersionInfo {
	
	/**
	 * Transforms byte code major version to JDK version using the following
	 * associations:
	 * 
	 *  49 -> 5,
	 *  50 -> 6,
	 *  51 -> 7,
	 *  52 -> 8,
	 *  53 -> 9,
	 *  54 -> 10,
	 *  55 -> 11,
	 *  56 -> 12,
	 *  57 -> 13,
	 *  58 -> 14
	 * @param A byte code major version
	 */
	public static int fromMajor(int version) {
		return version - 44;
	}
}
