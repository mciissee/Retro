package fr.umlv.Retro.concats;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.FeatureDescriber;
import fr.umlv.Retro.models.MethodInfo;


class ConcatDescriber implements FeatureDescriber {

	private final int lineNumber;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final String[] args;

	public ConcatDescriber(ClassInfo ci, MethodInfo mi, int lineNumber, String[] args) {
		this.ci = Objects.requireNonNull(ci);
		this.mi = Objects.requireNonNull(mi);
		Objects.requireNonNull(args);
		this.args = Arrays.copyOf(args, args.length);
		this.lineNumber = lineNumber;
	}
	
	@Override
	public String describe() {
		var pattern = Arrays.stream(args)
				.map(e -> e.equals("\u0001") ? "%1" : e)
				.collect(Collectors.joining());

		return String.format(
			"CONCATENATION at %s.%s%s (%s:%d): pattern %s",
			ci.className(),
			mi.methodName(),
			mi.descriptor(),
			ci.fileName(),
			lineNumber,
			pattern
		);
	}

	@Override
	public String toString() {
		return describe();
	}
}
