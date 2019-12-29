package fr.umlv.Retro.lambdas;

import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;

class LambdaDetector extends LocalVariablesSorter implements Opcodes {

	private int lineNumber;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final TransformOptions options;

	public LambdaDetector(ClassInfo ci, MethodInfo mi, TransformOptions options) {
		super(ci.api(), mi.access(), mi.descriptor(), mi.visitor());

		this.ci = ci;
		this.mi = mi;
		this.options = Objects.requireNonNull(options);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		this.lineNumber = line;
		super.visitLineNumber(line, start);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... args) {
		var rewritten = false;
		if (bootstrap.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
			var captures = Type.getArgumentTypes(descriptor);
			var calling = (Handle) args[1];
			var printer = new LambdaPrinter(ci, mi, lineNumber, captures, descriptor, calling);
			if (options.info()) {
				System.out.println(printer);
			}
			if (options.target() < 8 && (options.hasFeature(Features.Lambda) || options.force())) {
				var rewriter = new LambdaRewriter(this, ci, mi);
				rewriter.rewrite(name, descriptor, bootstrap, args);
				rewritten = true;
			}
		}
		if (!rewritten) {
			super.visitInvokeDynamicInsn(name, descriptor, bootstrap, args);
		}
	}
}
