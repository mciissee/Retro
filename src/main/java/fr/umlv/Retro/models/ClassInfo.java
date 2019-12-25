package fr.umlv.Retro.models;

import java.util.Objects;

import org.objectweb.asm.ClassVisitor;

/**
 * Provides informations about a class byte code structure.
 */
public class ClassInfo {

	private final int api;
	private final int version;
	private final String fileName;
	private final String className;
	private final ClassVisitor cv;
	
	/**
	 * Creates new instance of ClassInfo
	 * @param api ASM version
	 * @param version JDK version of the byte code
	 * @param fileName source file name
	 * @param className The name of the parsed class.
	 * @param cv The class visitor to which method calls should be delegated.
	 */
	public ClassInfo(int api, int version, String fileName, String className, ClassVisitor cv) {
		this.api = api;
		this.version = version;
		this.fileName = Objects.requireNonNull(fileName);
		this.className = Objects.requireNonNull(className);
		this.cv = Objects.requireNonNull(cv);
	}
	
	/**
	 * ASM version used to read the byte code.
	 */
	public int api() {
		return this.api;
	}

	/**
	 * JDK version of the byte code. [5, 14]
	 * @return
	 */
	public int version() {
		return this.version;
	}
	
	/**
	 * Name of the file containing the byte code.
	 */
	public String fileName() {
		return this.fileName;
	}
	
	/**
	 * Name of the class readed.
	 * @return
	 */
	public String className() {
		return this.className;
	}
	
	/**
	 * Reference to the class visitor to which method calls should be delegated.
	 */
	public ClassVisitor visitor() {
		return this.cv;
	}

}
