package fr.umlv.Retro.concats;

import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;

class ConcatDetector extends LocalVariablesSorter implements Opcodes {
	private int lineNumber;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final TransformOptions options;
	private final ConcatRewriter rewriter;

	public ConcatDetector(ClassInfo ci, MethodInfo mi, TransformOptions options) {
		super(ci.api(), mi.access(), mi.descriptor(), mi.visitor());
		this.ci = ci;
		this.mi = mi;
		this.options = Objects.requireNonNull(options);
		this.rewriter = new ConcatRewriter(this);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		this.lineNumber = line;
		super.visitLineNumber(line, start);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
		var rewrited = false;
		if (handle.getOwner().equals("java/lang/invoke/StringConcatFactory")) {
			var tokens = ((String) args[0]).split("((?<=\u0001)|(?=\u0001))");
			var printer = new ConcatPrinter(ci, mi, lineNumber, tokens);
			if (options.info()) {
				System.out.println(printer);
			}
			if (options.target() < 9 && (options.hasFeature(Features.Concat) || options.force())) {
				rewriter.rewrite(descriptor, tokens);
				rewrited = true;
			}
		}
		if (!rewrited) {
			super.visitInvokeDynamicInsn(name, descriptor, handle, args);
		}
	}

}
