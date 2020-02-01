package fr.umlv.retro.concats;

import java.util.Objects;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class ConcatRewriter implements Opcodes {
	private final ConcatDetector mv;

	public ConcatRewriter(ConcatDetector mv) {
		this.mv = Objects.requireNonNull(mv);
	}

	public void rewrite(String descriptor, String[] args) {
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
			var id = mv.newLocal(Type.getType(String.class));
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
