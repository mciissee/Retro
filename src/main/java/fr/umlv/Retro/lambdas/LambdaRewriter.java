package fr.umlv.Retro.lambdas;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.VersionInfo;
import fr.umlv.Retro.models.InstUtils;

class LambdaRewriter implements Opcodes {

	private static int methodRefs;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final LambdaDetector mv;

	public LambdaRewriter(LambdaDetector ld, ClassInfo ci, MethodInfo mi) {
		this.mv = Objects.requireNonNull(ld);
		this.ci = Objects.requireNonNull(ci);
		this.mi = Objects.requireNonNull(mi);
	}

	public void rewrite(String name, String descriptor, Handle bootstrap, Object... args) {
		var lambdaReturn = Type.getReturnType(descriptor).getInternalName();
		var lambdaCaptures = Type.getArgumentTypes(descriptor);
		var lambdaCtor = Arrays.stream(lambdaCaptures).map(e -> e.toString()).collect(Collectors.joining("", "(", ")V"));

		var lambda = (Handle) args[1];

		var lambdaMethodName = lambda.getName();
		var lambdaMethodOwner = lambda.getOwner();
		var isMethodRef = !lambdaMethodName.contains("$");

		var lambdaClassName = String.format("%s_%s", lambdaMethodOwner, lambdaMethodName);
		if (isMethodRef) {
			lambdaClassName = String.format("%s_%s$%d", ci.className(), lambdaMethodName, ++methodRefs);
		}
		lambdaClassName = lambdaClassName.replace("/", "_");
		

		replaceDynamicInvoke(lambdaClassName, lambdaCtor, lambdaCaptures);

		var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		writeHeader(lambdaReturn, lambdaClassName, cw);
		writeFields(lambdaCaptures, cw);
		writeConstructor(cw, lambdaClassName, lambdaCaptures, lambdaCtor);
		writeMethod(cw, name, lambda, lambdaClassName, lambdaCaptures, args);

	
		try (var fos = new FileOutputStream("../Drafts/" + lambdaClassName + ".class")) {
			fos.write(cw.toByteArray());
		} catch (Exception e) {
			System.out.println(e);
		}
		
	}

	private void writeHeader(String interfaces, String className, ClassWriter cw) {
		cw.visit(VersionInfo.toMajor(ci.version()), ACC_SUPER, className, null, "java/lang/Object",
				new String[] { interfaces });
		cw.visitSource(ci.className() + ".java", null);
		cw.visitNestHost(ci.className());
		cw.visitOuterClass(ci.className(), mi.methodName(), mi.descriptor());
		cw.visitInnerClass(className, null, null, 0);
	}

	private void writeFields(Type[] captures, ClassWriter cw) {
		var i = 0;
		for (var capture : captures) {
			var fv = cw.visitField(ACC_FINAL | ACC_SYNTHETIC, "f$" + i, capture.toString(), null, null);
			fv.visitEnd();
			i++;
		}
	}

	private void writeConstructor(ClassWriter cw, String className, Type[] captures, String ctor) {
		var mv = cw.visitMethod(ACC_PUBLIC, "<init>", ctor, null, null);
		mv.visitCode();

		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

		for (int i = 0; i < captures.length; i++) {
			mv.visitVarInsn(ALOAD, 0);
			InstUtils.load(mv, captures[i], i + 1);
			mv.visitFieldInsn(PUTFIELD, className, "f$" + i, captures[i].toString());
		}

		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private void writeMethod(ClassWriter cw, String name, Handle lambda, String clazz, Type[] captures, Object... args) {
		var description = args[2].toString();
		var mv = cw.visitMethod(ACC_PUBLIC, name, description, null, null);
		mv.visitCode();

		for (int i = 0; i < captures.length; i++) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, clazz, "f$" + i, captures[i].toString());
		}
	
		var loadedTypes = Type.getArgumentTypes(description);
		var expectTypes = Arrays.stream(Type.getArgumentTypes(lambda.getDesc())).skip(captures.length).toArray(Type[]::new);
		for (int i = 0; i < loadedTypes.length; i++) {
			InstUtils.load(mv, loadedTypes[i], i + 1);
			if (!expectTypes[i].equals(loadedTypes[i])) {
				InstUtils.cast(mv, loadedTypes[i], expectTypes[i]);
			}
		}
		
		var opcode = lambda.getTag() == H_INVOKESTATIC ? INVOKESTATIC : INVOKESPECIAL;
		mv.visitMethodInsn(opcode, lambda.getOwner(), lambda.getName(), lambda.getDesc(), false);
		
		var r1 = Type.getReturnType(lambda.getDesc());
		var r2 = Type.getReturnType(description);
		if (!r1.equals(r2)) {
			InstUtils.cast(mv, r1, r2);	
		}
		InstUtils.ret(mv, r2);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		var arg0 = args[0].toString();
		if (!arg0.equals(description)) {
			var types = Type.getArgumentTypes(arg0);
			mv = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, name, arg0, null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			for (int i = 0; i < types.length; i++) {
				InstUtils.load(mv, types[i], i + 1);
				mv.visitTypeInsn(CHECKCAST, loadedTypes[i].getInternalName());
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, clazz, name, args[2].toString(), false);
			mv.visitInsn(Type.getReturnType(arg0).toString().equals("V") ? RETURN : ARETURN);		
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
	}

	private void replaceDynamicInvoke(String name, String ctor, Type[] ctorArgTypes) {
		var cv = ci.visitor();
		cv.visitNestMember(name);
		cv.visitInnerClass(name, null, null, 0);
		var args = new Integer[ctorArgTypes.length];
		for (var i = ctorArgTypes.length - 1; i >= 0; i--) {
			var arg = ctorArgTypes[i];
			args[i] = mv.newLocal(arg);
			InstUtils.store(mv, arg, args[i]);
		}
		mv.visitTypeInsn(NEW, name);
		mv.visitInsn(DUP);
		for (var i = 0; i < ctorArgTypes.length; i++) {
			InstUtils.load(mv, ctorArgTypes[i], args[i]);
		}
		mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", ctor, false);
	}

}
