package fr.umlv.Retro.models;

import java.nio.file.Path;
import java.util.Objects;

import org.objectweb.asm.ClassVisitor;

/**
 * Provides informations about a class bytecode structure (Immutable).
 */
public class ClassInfo {

	private final int version;
	private final Path path;
	private final String fileName;
	private final String className;
	private final String nestHost;
	private final ClassVisitor cv;

	/**
	 * Creates new instance of ClassInfo
	 * @param version JDK version of the bytecode
	 * @param path path to the source file.
	 * @param fileName source file name
	 * @param className The name of the parsed class.
	 * @param cv The class visitor to which method calls should be delegated.
	 */
	public ClassInfo(int version, Path path, String fileName, String className, ClassVisitor cv) {
		this(version, path, fileName, className, null, cv);
	}
	
	/**
	 * Creates new instance of ClassInfo
	 * @param version JDK version of the bytecode
	 * @param path path to the source file.
	 * @param fileName source file name
	 * @param className The name of the parsed class.
	 * @param nestHost The nestHost of the class (nullable).
	 * @param cv The class visitor to which method calls should be delegated.
	 */
	public ClassInfo(int version, Path path, String fileName, String className, String nestHost, ClassVisitor cv) {
		this.version = version;
		this.path = Objects.requireNonNull(path);
		this.fileName = Objects.requireNonNull(fileName);
		this.className = Objects.requireNonNull(className);
		this.nestHost = nestHost;
		this.cv = Objects.requireNonNull(cv);
	}
	
	/**
	 * JDK version of the bytecode. [5, 14]
	 * @return
	 */
	public int version() {
		return this.version;
	}
	
	/**
	 * Path of the file containing the bytecode.
	 */
	public Path path() {
		return this.path;
	}

	/**
	 * Name of the file containing the bytecode.
	 */
	public String fileName() {
		return this.fileName;
	}
	
	/**
	 * Name of the class.
	 */
	public String className() {
		return this.className;
	}
	
	/**
	 * NestHost attribute of the class (nullable).
	 */
	public String nestHost() {
		return this.nestHost;
	}

	/**
	 * The class visitor to which method calls should be delegated.
	 */
	public ClassVisitor visitor() {
		return this.cv;
	}
	
}
