package fr.umlv.retro.records;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.MethodInfo;
import fr.umlv.retro.utils.TypeUtils;


class RecordRewriter implements Opcodes {

	private final ClassInfo ci;
	private final MethodInfo mi;

	private final Map<String, Consumer<Handle[]>> rewritters = Map.of(
			"toString", this::rewriteToString,
			"hashCode", this::rewriteHashCode,
			"equals", this::rewriteEquals
	
	);

	public RecordRewriter(ClassInfo ci, MethodInfo mi) {
		this.ci = Objects.requireNonNull(ci);
		this.mi = Objects.requireNonNull(mi);
	}
	
	public void rewrite(String method, Handle[] fields) {
		Objects.requireNonNull(method);
		Objects.requireNonNull(fields);
		var consumer = rewritters.get(method);
		if (consumer == null) {
			throw new IllegalArgumentException("arg method should be one of [toString, hashCode, equals]");
		}
		consumer.accept(fields);
	}
	
	private void rewriteToString(Handle[] fields) {
		var mv = mi.visitor();
		mv.visitInsn(POP);
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
		mv.visitVarInsn(ASTORE, 1);
		Label label1 = new Label();
		mv.visitLabel(label1);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitLdcInsn(ci.className() + "[");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitInsn(POP);
		Label label2 = new Label();
		mv.visitLabel(label2);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitLdcInsn("a=");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitInsn(POP);
		
		Label label3 = new Label();
		mv.visitLabel(label3);
		mv.visitLineNumber(31, label3);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, ci.className(), "a", "I");
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
		mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitInsn(POP);
		
		
		Label label4 = new Label();
		mv.visitLabel(label4);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitLdcInsn(", ");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitInsn(POP);

		Label label5 = new Label();
		mv.visitLabel(label5);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitLdcInsn("b=");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitInsn(POP);

		Label label6 = new Label();
		mv.visitLabel(label6);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, ci.className(), "b", "Ljava/lang/String;");
		mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitInsn(POP);

		Label label7 = new Label();
		mv.visitLabel(label7);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitLdcInsn("]");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitInsn(POP);
		Label label8 = new Label();
		mv.visitLabel(label8);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
	}
	
	private void rewriteHashCode(Handle[] fields) {
		var mv = mi.visitor();
        mv.visitIntInsn(SIPUSH, fields.length);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
		var i = 0;
		for (var e : fields) {
			var owner = e.getOwner();
			var name = e.getName();
			var desc = e.getDesc();
			var type = Type.getType(desc);
			mv.visitInsn(DUP);
	        mv.visitIntInsn(SIPUSH, i);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, owner, name, "()" + desc, false);
			if (TypeUtils.isPrimitive(type)) {
				var wrapper = TypeUtils.wrapper(type);
				mv.visitMethodInsn(
					INVOKESTATIC,
					wrapper.getInternalName(),
					"valueOf",
					String.format("(%s)%s", desc, wrapper.getDescriptor()),
					false
				);
			}
			mv.visitInsn(AASTORE);			
			i++;
		}
		mv.visitMethodInsn(
			INVOKESTATIC,
			ci.className(),
			"hashCode",
			"([Ljava/lang/Object;)I",
			false
		);
	}

	private void rewriteEquals(Handle[] fields) {
		// TODO visitFrame throw exception
		/*
		var mv = mi.visitor();
		mv.visitTypeInsn(INSTANCEOF, ci.className());
		Label label1 = new Label();
		mv.visitJumpInsn(IFNE, label1);
		Label label2 = new Label();
		mv.visitLabel(label2);
		mv.visitInsn(ICONST_0);
		mv.visitInsn(IRETURN);
		mv.visitLabel(label1);
		mv.visitLineNumber(19, label1);
		// mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, ci.className());
		mv.visitVarInsn(ASTORE, 2);
		Label label3 = new Label();
		mv.visitLabel(label3);

		Label label4 = new Label();

		for (var e : fields) {
			var owner = e.getOwner();
			var name = e.getName();
			var desc = e.getDesc();
			var type = Type.getType(desc);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, owner, name, "()" + desc, false);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEVIRTUAL, owner, name, "()" + desc, false);
			if (TypeUtils.isPrimitive(type)) {
				mv.visitJumpInsn(IF_ICMPNE, label4);
			} else {
				mv.visitMethodInsn(INVOKEVIRTUAL, type.getInternalName(), "equals", "(Ljava/lang/Object;)Z", false);
				mv.visitJumpInsn(IFEQ, label4);
			}
		}
		
		mv.visitInsn(ICONST_1);
		Label label5 = new Label();
		mv.visitJumpInsn(GOTO, label5);
		mv.visitLabel(label4);
		// mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {ci.className()}, 0, null);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(label5);
		// mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER});
		mv.visitInsn(IRETURN);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();
		*/
	}

}
