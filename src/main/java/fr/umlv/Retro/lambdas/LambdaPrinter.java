package fr.umlv.Retro.lambdas;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;


class LambdaPrinter {

	private final int lineNumber;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final HashSet<String> captures;
	private final String descriptor;
	private final Handle calling;
	
	public LambdaPrinter(ClassInfo ci, MethodInfo mi, int lineNumber, HashSet<String> captures, String descriptor, Handle calling) {
		this.ci = Objects.requireNonNull(ci);
		this.mi = Objects.requireNonNull(mi);
		this.captures = Objects.requireNonNull(captures);
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
		if (cName.contains("$")) {
			var index = cName.indexOf('$');
			var end = cName.substring(index, cName.length());
			cName = String.format("lambda$%s%s", mi.methodName(), end);
		}
		return String.format(
			"LAMBDA at %s.%s%s (%s:%d): lambda %s capture %s calling %s.%s%s",
			ci.className(),
			mi.methodName(),
			mi.descriptor(),
			ci.fileName(),
			lineNumber,
			rType,
			captures(),
			cOwner,
			cName,
			cDesc	
		);
	}
	
	private String captures() {
		var map = Map.of(
			Opcodes.ISTORE, "I",
			Opcodes.LSTORE, "L",
			Opcodes.FSTORE, "F",
			Opcodes.DSTORE, "D",
			Opcodes.ASTORE, "A"
		);
		return captures.stream().map(e -> {
			var i = e.indexOf('-');
			var v =  Integer.parseInt(e.substring(0, i));
			return map.get(v);
		}).collect(Collectors.joining(",", "[", "]"));
	}
}
