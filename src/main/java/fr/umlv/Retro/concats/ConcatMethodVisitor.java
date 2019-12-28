package fr.umlv.Retro.concats;

import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import fr.umlv.Retro.MethodFeatureVisitor;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;

/**
 * Transforms string concatenation feature from Java 9 (implemented using
 * InvokeDynamic) by an implementation using StringBuilder in lower versions.
 */
public class ConcatMethodVisitor implements MethodFeatureVisitor {

	private static class Visitor extends LocalVariablesSorter implements Opcodes {
		private int lineNumber;
		private final ClassInfo ci;
		private final MethodInfo mi;
		private final TransformOptions options;

		public Visitor(ClassInfo ci, MethodInfo mi, TransformOptions options) {
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
		public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
			var overrided = false;
			if (handle.getOwner().equals("java/lang/invoke/StringConcatFactory")) {
				var tokens = ((String) args[0]).split("((?<=\u0001)|(?=\u0001))");
				var printer = new ConcatPrinter(ci, mi, lineNumber, tokens);
				if (options.info()) {
					System.out.println(printer);
				}
				if (options.target() < 9 && (options.hasFeature(Features.Concat) || options.force())) {
					rewrite(descriptor, tokens);
					overrided = true;
				}
			}
			if (!overrided) {
				super.visitInvokeDynamicInsn(name, descriptor, handle, args);
			}
		}
		
		private void rewrite(String descriptor, String[] args) {
			var sb = "java/lang/StringBuilder";
			var ids = storeArgs(descriptor, args);
			mv.visitTypeInsn(NEW, sb);
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, sb, "<init>", "()V", false);
			for (var e : ids) {
				mv.visitVarInsn(ALOAD, e);
				mv.visitMethodInsn(INVOKEVIRTUAL, sb, "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, sb, "toString", "()Ljava/lang/String;", false);
		}

		private Integer[] storeArgs(String descriptor, String[] args) {
			var types = Type.getArgumentTypes(descriptor);
			var j = types.length - 1;
			var adresses = new Integer[args.length];
			for (var i = args.length - 1; i >= 0; i--) {
				var token = args[i];
				var id = newLocal(Type.getType(String.class));
				adresses[i] = id;
				if (token.equals("\u0001")) {
					var type = types[j--];
					var arg = type.toString();
					if (arg.startsWith("L")) {
						arg = Type.getDescriptor(Object.class);
					}
					var desc = String.format("(%s)Ljava/lang/String;", arg);
					mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", desc, false);
				} else {		
					mv.visitLdcInsn(token);
				}
				mv.visitVarInsn(ASTORE, id);
			}
			return adresses;
		}
	}

	@Override
	public MethodVisitor visit(ClassInfo ci, MethodInfo mi, TransformOptions options) {
		return new Visitor(ci, mi, options);
	}

}
