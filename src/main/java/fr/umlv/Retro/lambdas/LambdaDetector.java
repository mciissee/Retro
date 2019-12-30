package fr.umlv.Retro.lambdas;

import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import fr.umlv.Retro.Retro;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodInfo;

/**
 * Detects lambda method call and rewrite them if needed (target < JDK 8).
 */
class LambdaDetector extends LocalVariablesSorter implements Opcodes {
	
	private final Retro app;
	private final ClassInfo ci;
	private final MethodInfo mi;

	private int lineNumber;
	
	public LambdaDetector(Retro app, ClassInfo ci, MethodInfo mi) {
		super(ci.api(), mi.access(), mi.descriptor(), mi.visitor());
		this.ci = ci;
		this.mi = mi;
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
			app.onFeatureDetected(Features.Lambda, new LambdaDescriber(ci, mi, lineNumber, captures, descriptor, (Handle) bsmArgs[1]));
			if (app.target() < 8 && app.hasFeature(Features.Lambda)) {
				var rewriter = new LambdaRewriter(this, app, ci, mi);
				rewriter.rewrite(name, descriptor, bsm, bsmArgs);
				rewritten = true;
			}
		}
		if (!rewritten) {
			super.visitInvokeDynamicInsn(name, descriptor, bsm, bsmArgs);
		}
	}
}
