package fr.umlv.retro.lambdas;

import java.util.Objects;
import java.util.function.Supplier;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.Features;
import fr.umlv.retro.models.MethodInfo;
import fr.umlv.retro.Retro;

/**
 * Detects lambda method calls and rewrite them if needed (target < JDK 8).
 */
class LambdaDetector extends LocalVariablesSorter implements Opcodes {
	
	private final Retro app;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final Supplier<String> lambdaNameFactory;
	private int lineNumber;
	
	public LambdaDetector(Retro app, ClassInfo ci, MethodInfo mi, Supplier<String> lambdaNameFactory) {
		super(
			Objects.requireNonNull(app).api(),
			Objects.requireNonNull(mi).access(),
			Objects.requireNonNull(mi).descriptor(),
			Objects.requireNonNull(mi).visitor()
		);
		this.app = app;
		this.ci = ci;
		this.mi = mi;
		this.lambdaNameFactory = Objects.requireNonNull(lambdaNameFactory);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		this.lineNumber = line;
		super.visitLineNumber(line, start);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... bsmArgs) {
		var rewritten = false;
		if (bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
			var captures = Type.getArgumentTypes(descriptor);
			var describer = new LambdaDescriber(ci, mi, lineNumber, captures, descriptor, (Handle) bsmArgs[1]);
			app.detectFeature(ci.path(), Features.Lambda, describer);
			if (app.target() < 8 && app.hasFeature(Features.Lambda)) {
				var rewriter = new LambdaRewriter(this, app, ci, mi);
				rewriter.rewrite(lambdaNameFactory.get(), name, descriptor, bsm, bsmArgs);
				rewritten = true;
			}
		}
		if (!rewritten) {
			super.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
		}
	}
}
