package fr.umlv.Retro.concats;

import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import fr.umlv.Retro.Retro;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodInfo;

class ConcatDetector extends LocalVariablesSorter implements Opcodes {

	private int lineNumber;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final Retro app;

	public ConcatDetector(Retro app, ClassInfo ci, MethodInfo mi) {
		super(ci.api(), mi.access(), mi.descriptor(), mi.visitor());
		this.app = Objects.requireNonNull(app);
		this.ci = ci;
		this.mi = mi;
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		this.lineNumber = line;
		super.visitLineNumber(line, start);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
		var rewritten = false;
		if (handle.getOwner().equals("java/lang/invoke/StringConcatFactory")) {
			var tokens = ((String) args[0]).split("((?<=\u0001)|(?=\u0001))");
			app.onFeatureDetected(Features.Concat, new ConcatDescriber(ci, mi, lineNumber, tokens));
			if (app.target() < 9 && app.hasFeature(Features.Concat)) {
				var rewriter = new ConcatRewriter(this);
				rewriter.rewrite(descriptor, tokens);
				rewritten = true;
			}
		}
		if (!rewritten) {
			super.visitInvokeDynamicInsn(name, descriptor, handle, args);
		}
	}

}
