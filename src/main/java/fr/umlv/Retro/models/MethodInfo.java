package fr.umlv.Retro.models;

import java.util.Arrays;
import java.util.Objects;
import org.objectweb.asm.MethodVisitor;

/**
 * Provides informations about a class byte code method structure.
 */
public class MethodInfo {

	private final String methodName;
	private final String descriptor;
	private final String[] exceptions;
	private final MethodVisitor mv;

	
	/**
	 * Creates new instance of MethodInfo
	 * @param name The name of the method.
	 * @param descriptor The descriptor of the method.
	 * @param exceptions A list of exceptions of thrown by the method.
	 * @param mv The method visitor to which method calls should be delegated.
	 */
	public MethodInfo(String name, String descriptor, String[] exceptions, MethodVisitor mv) {
		this.methodName = Objects.requireNonNull(name);
		this.descriptor = Objects.requireNonNull(descriptor);
		this.exceptions = Objects.requireNonNull(exceptions);
		this.mv = Objects.requireNonNull(mv);
	}

	/** 
	 * The name of the method.
	 */
	public String methodName() {
		return methodName;
	}
	
	/** 
	 * The descriptor of the method.
	 */
	public String descriptor() {
		return descriptor;
	}
	
	/** 
	 * A list of exceptions thrown by the method.
	 */
	public String[] exceptions() {
		return Arrays.copyOf(exceptions, exceptions.length);
	}

	/**
	 * The method visitor to which method calls should be delegated.
	 */
	public MethodVisitor visitor() {
		return mv;
	}

}
