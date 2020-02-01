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
	private final Supplier<String> nextName;
	private int lineNumber;
	
	public LambdaDetector(Retro app, ClassInfo ci, MethodInfo mi, Supplier<String> nextName) {
		super(app.api(), mi.access(), mi.descriptor(), mi.visitor());
		this.ci = ci;
		this.mi = mi;
		this.nextName = Objects.requireNonNull(nextName);
		this.app = Objects.requireNonNull(app);
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
				rewriter.rewrite(nextName.get(), name, descriptor, bsm, bsmArgs);
				rewritten = true;
			}
		}
		if (!rewritten) {
			super.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
		}
	}
}
