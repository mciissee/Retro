package fr.umlv.retro.lambdas;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.FeatureDescriber;
import fr.umlv.retro.models.MethodInfo;

class LambdaDescriber implements FeatureDescriber {

	private final int lineNumber;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final Type[] captures;
	private final String descriptor;
	private final Handle method;
	
	public LambdaDescriber(ClassInfo ci, MethodInfo mi, int lineNumber, Type[] captures, String descriptor, Handle method) {
		this.ci = Objects.requireNonNull(ci);
		this.mi = Objects.requireNonNull(mi);
		this.captures = Objects.requireNonNull(Arrays.copyOf(captures, captures.length));
		this.descriptor = Objects.requireNonNull(descriptor);
		this.method = Objects.requireNonNull(method);
		this.lineNumber = lineNumber;
	}
	
	@Override
	public String describe() {
		var rType = Type.getReturnType(descriptor).getInternalName();
		var cOwner = method.getOwner();
		var cName = method.getName();
		var cDesc = method.getDesc();
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

	@Override
	public String toString() {
		return this.describe();
	}

}
