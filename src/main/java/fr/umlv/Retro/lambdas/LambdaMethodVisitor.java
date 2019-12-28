package fr.umlv.Retro.lambdas;

import java.util.HashSet;
import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import fr.umlv.Retro.MethodFeatureVisitor;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;

/**
 * Transform lambda functions from the method to Anonymous functions
 * if target version is lower than JDK 8.
 */
public class LambdaMethodVisitor implements MethodFeatureVisitor {
	
	private static class Visitor extends MethodVisitor implements Opcodes {

		private int lineNumber;

		private final ClassInfo ci;
		private final MethodInfo mi;
		private final TransformOptions options;
		private final HashSet<String> captures = new HashSet<String>();

		public Visitor(ClassInfo ci, MethodInfo mi, TransformOptions options) {
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
				captures.add(opcode  + "-" + var);
			} else if (opcode >= ILOAD && opcode <= ILOAD) {
				captures.remove(opcode  + "-" + var);
			}
			super.visitVarInsn(opcode, var);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
			if (handle.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
				var printer = new LambdaPrinter(ci, mi, lineNumber, captures, descriptor, (Handle) args[1]);
				if (options.info()) {
					System.out.println(printer);
				}

				if (options.target() < 8 && (options.hasFeature(Features.Lambda) || options.force())) {
					// TODO rm super.visitInvokeDynamicInsn and rewrite to anonymous func
					super.visitInvokeDynamicInsn(name, descriptor, handle, args);
				}
			} else {
				super.visitInvokeDynamicInsn(name, descriptor, handle, args);				
			}
		}
	}

	@Override
	public MethodVisitor visit(ClassInfo ci, MethodInfo mi, TransformOptions options) {
		return new Visitor(ci, mi, options);
	}

}
