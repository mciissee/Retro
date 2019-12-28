package fr.umlv.Retro.lambdas;

import java.util.HashSet;
import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;

class LambdaDetector extends MethodVisitor implements Opcodes {

	private int lineNumber;

	private final ClassInfo ci;
	private final MethodInfo mi;
	private final TransformOptions options;
	private final HashSet<String> captures = new HashSet<String>();

	public LambdaDetector(ClassInfo ci, MethodInfo mi, TransformOptions options) {
		super(ci.api(), mi.visitor());
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
	public void visitVarInsn(int opcode, int var) {
		if (opcode >= ISTORE && opcode <= ASTORE) {
			captures.add(opcode + "-" + var);
		} else if (opcode >= ILOAD && opcode <= ILOAD) {
			captures.remove(opcode + "-" + var);
		}
		super.visitVarInsn(opcode, var);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
		var rewritten = false;
		if (handle.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
			var captures = Type.getArgumentTypes(descriptor);
			var printer = new LambdaPrinter(ci, mi, lineNumber, captures, descriptor, (Handle) args[1]);
			if (options.info()) {
				System.out.println(printer);
			}

			if (options.target() < 8 && (options.hasFeature(Features.Lambda) || options.force())) {
				// TODO rm super.visitInvokeDynamicInsn and rewrite to anonymous func
				super.visitInvokeDynamicInsn(name, descriptor, handle, args);
			}
		}

		if (!rewritten) {
			super.visitInvokeDynamicInsn(name, descriptor, handle, args);			
		}
	}
}
