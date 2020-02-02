package fr.umlv.retro.records;

import java.util.Objects;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import fr.umlv.retro.ClassTransformer;
import fr.umlv.retro.Retro;
import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.FeatureVisitor;
import fr.umlv.retro.models.Features;
import fr.umlv.retro.models.MethodInfo;

public class RecordVisitor extends ClassVisitor implements FeatureVisitor, Opcodes {
	
	private final Retro app;
	private ClassInfo ci;
	private boolean isRecord;

	public RecordVisitor(Retro app) {
		super(Objects.requireNonNull(app).api());
		this.app = app;
	}

	public ClassVisitor visit(ClassInfo ci, ClassVisitor cv,  ClassTransformer tr) {
		this.ci = Objects.requireNonNull(ci);
		this.cv = Objects.requireNonNull(cv);
		return this;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (superName.equals("java/lang/Record")) {
			isRecord = true;
			superName = "java/lang/Object";
			app.detectFeature(ci.path(), Features.Record, new RecordDescriber(ci));
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		if (!isRecord) {
			return mv;
		}
		return new RecordDetector(app, ci, new MethodInfo(access, name, descriptor, exceptions, mv));
	}

	@Override
	public void visitEnd() {
		if (isRecord) {
			var cv = ci.visitor();
			var mv = cv.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_VARARGS, "hashCode", "([Ljava/lang/Object;)I", null, null);
			mv.visitCode();
			Label label0 = new Label();
			mv.visitLabel(label0);
			mv.visitVarInsn(ALOAD, 0);
			Label label1 = new Label();
			mv.visitJumpInsn(IFNONNULL, label1);
			Label label2 = new Label();
			mv.visitLabel(label2);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(IRETURN);
			mv.visitLabel(label1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ICONST_1);
			mv.visitVarInsn(ISTORE, 1);
			Label label3 = new Label();
			mv.visitLabel(label3);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitInsn(ARRAYLENGTH);
			mv.visitVarInsn(ISTORE, 3);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, 4);
			Label label4 = new Label();
			mv.visitLabel(label4);
			mv.visitFrame(Opcodes.F_FULL, 5, new Object[] {"[Ljava/lang/Object;", Opcodes.INTEGER, "[Ljava/lang/Object;", Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[] {});
			mv.visitVarInsn(ILOAD, 4);
			mv.visitVarInsn(ILOAD, 3);
			Label label5 = new Label();
			mv.visitJumpInsn(IF_ICMPGE, label5);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ILOAD, 4);
			mv.visitInsn(AALOAD);
			mv.visitVarInsn(ASTORE, 5);
			Label label6 = new Label();
			mv.visitLabel(label6);
			mv.visitIntInsn(BIPUSH, 31);
			mv.visitVarInsn(ILOAD, 1);
			mv.visitInsn(IMUL);
			mv.visitVarInsn(ALOAD, 5);
			Label label7 = new Label();
			mv.visitJumpInsn(IFNONNULL, label7);
			mv.visitInsn(ICONST_0);
			Label label8 = new Label();
			mv.visitJumpInsn(GOTO, label8);
			mv.visitLabel(label7);
			mv.visitFrame(Opcodes.F_FULL, 6, new Object[] {"[Ljava/lang/Object;", Opcodes.INTEGER, "[Ljava/lang/Object;", Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/Object"}, 1, new Object[] {Opcodes.INTEGER});
			mv.visitVarInsn(ALOAD, 5);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false);
			mv.visitLabel(label8);
			mv.visitFrame(Opcodes.F_FULL, 6, new Object[] {"[Ljava/lang/Object;", Opcodes.INTEGER, "[Ljava/lang/Object;", Opcodes.INTEGER, Opcodes.INTEGER, "java/lang/Object"}, 2, new Object[] {Opcodes.INTEGER, Opcodes.INTEGER});
			mv.visitInsn(IADD);
			mv.visitVarInsn(ISTORE, 1);
			Label label9 = new Label();
			mv.visitLabel(label9);
			mv.visitIincInsn(4, 1);
			mv.visitJumpInsn(GOTO, label4);
			mv.visitLabel(label5);
			mv.visitFrame(Opcodes.F_FULL, 2, new Object[] {"[Ljava/lang/Object;", Opcodes.INTEGER}, 0, new Object[] {});
			mv.visitVarInsn(ILOAD, 1);
			mv.visitInsn(IRETURN);
			mv.visitMaxs(2, 6);
			mv.visitEnd();
		}
		super.visitEnd();
	}
}
