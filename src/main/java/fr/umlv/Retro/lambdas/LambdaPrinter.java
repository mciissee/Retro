package fr.umlv.Retro.lambdas;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.FeaturePrinter;
import fr.umlv.Retro.models.MethodInfo;

/**
 * Displays lambda method feature detection message.
 */
class LambdaPrinter implements FeaturePrinter {

	private final int lineNumber;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final Type[] captures;
	private final String descriptor;
	private final Handle calling;
	
	public LambdaPrinter(ClassInfo ci, MethodInfo mi, int lineNumber, Type[] captures, String descriptor, Handle calling) {
		this.ci = Objects.requireNonNull(ci);
		this.mi = Objects.requireNonNull(mi);
		this.captures = Objects.requireNonNull(Arrays.copyOf(captures, captures.length));
		this.descriptor = Objects.requireNonNull(descriptor);
		this.calling = Objects.requireNonNull(calling);
		this.lineNumber = lineNumber;
	}
	
	@Override
	public String toString() {
		var rType = Type.getReturnType(descriptor).getInternalName();
		var cOwner = calling.getOwner();
		var cName = calling.getName();
		var cDesc = calling.getDesc();
		var captures = Arrays.stream(this.captures).map(e -> {
			return e.toString();
		}).collect(Collectors.joining(",", "[", "]"));
		return String.format(
			"LAMBDA at %s.%s%s (%s:%d): lambda %s capture %s calling %s.%s%s",
			ci.className(),
			mi.methodName(),
			mi.descriptor(),
			ci.fileName(),
			lineNumber,
			rType,
			captures,
			cOwner,
			cName,
			cDesc	
		);
	}
}
