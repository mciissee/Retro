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
import fr.umlv.Retro.utils.InstUtils;
import fr.umlv.Retro.utils.VersionUtils;

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
		var method = (Handle) args[1];
		var clazz = String.format("%s_%s", method.getOwner(), method.getName());
		if (!method.getName().contains("$")) { // method reference
			clazz = String.format("%s_%s$%d", ci.className(), method.getName(), ++methodRefs);
		}
		clazz = clazz.replace("/", "_");
		
		var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		var captures = Type.getArgumentTypes(descriptor);
		var constructor = Arrays.stream(captures).map(e -> e.toString()).collect(Collectors.joining("", "(", ")V"));

		writeHeader(cw, clazz, descriptor);
		writeFields(cw, captures);
		writeConstructor(cw, clazz, constructor, captures);
		writeMethods(cw, clazz, captures, name, method, args);
		/*
		try (var fos = new FileOutputStream("../Drafts/" + clazz + ".class")) {
			fos.write(cw.toByteArray());
		} catch (Exception e) {
			System.out.println(e);
		}
		*/
		instantiate(clazz, constructor, captures);
	}

	private void writeHeader(ClassWriter cw, String clazz, String descriptor) {
		var version = VersionUtils.toBytecode(ci.version());
		var itf = Type.getReturnType(descriptor).getInternalName();
		cw.visit(version, ACC_SUPER, clazz, null, "java/lang/Object", new String[] { itf });
		cw.visitSource(ci.className() + ".java", null);
		cw.visitOuterClass(ci.className(), mi.methodName(), mi.descriptor());
		cw.visitInnerClass(clazz, ci.className(), clazz, 0);
	}

	private void writeFields(ClassWriter cw, Type[] captures) {
		var i = 0;
		for (var capture : captures) {
			var fv = cw.visitField(ACC_FINAL | ACC_SYNTHETIC, "f$" + i, capture.toString(), null, null);
			fv.visitEnd();
			i++;
		}
	}

	private void writeConstructor(ClassWriter cw, String clazz, String descriptor, Type[] arguments) {
		var mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
		mv.visitCode();

		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

		for (int i = 0; i < arguments.length; i++) {
			mv.visitVarInsn(ALOAD, 0);
			InstUtils.load(mv, arguments[i], i + 1);
			mv.visitFieldInsn(PUTFIELD, clazz, "f$" + i, arguments[i].toString());
		}

		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private void writeMethods(ClassWriter cw, String clazz, Type[] captures, String name, Handle bsm, Object... bsmArgs) {
		var descriptor = bsmArgs[0].toString();
		var signature = bsmArgs[2].toString();

		var mv = cw.visitMethod(ACC_PUBLIC, name, signature, null, null);
		mv.visitCode();

		for (int i = 0; i < captures.length; i++) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, clazz, "f$" + i, captures[i].toString());
		}
	
		var args = Type.getArgumentTypes(signature);
		var params = parameters(bsm, captures);
		for (int i = 0; i < args.length; i++) {
			InstUtils.load(mv, args[i], i + 1);
			if (!args[i].equals(params[i])) {
				InstUtils.cast(mv, args[i], params[i]);
			}
		}

		mv.visitMethodInsn(bsm.getTag() == H_INVOKESTATIC ? INVOKESTATIC : INVOKEVIRTUAL, bsm.getOwner(), accessor(bsm), bsm.getDesc(), false);
		
		var r1 = Type.getReturnType(bsm.getDesc());
		var r2 = Type.getReturnType(signature);
		if (!r1.equals(r2)) {
			InstUtils.cast(mv, r1, r2);	
		}

		InstUtils.ret(mv, r2);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		if (!descriptor.equals(signature)) {
			mv = cw.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, name, descriptor, null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			
			params = Type.getArgumentTypes(descriptor);
			for (int i = 0; i < params.length; i++) {
				InstUtils.load(mv, params[i], i + 1);
				mv.visitTypeInsn(CHECKCAST, args[i].getInternalName());
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, clazz, name, signature, false);

			InstUtils.ret(mv, Type.getReturnType(descriptor));
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
	}

	private String accessor(Handle bsm) {
		var owner = bsm.getOwner();
		if (!owner.equals(ci.className())) {
			return bsm.getName();
		}

		var cv = ci.visitor();
		var desc = bsm.getDesc();
		var name = String.format("access$%s", bsm.getName());
		var isStatic = bsm.getTag() == H_INVOKESTATIC;
	
		var mv = cv.visitMethod((isStatic ? ACC_STATIC : 0) + ACC_SYNTHETIC, name, desc, null, null);
		mv.visitCode();	
	
		var types = Type.getArgumentTypes(desc);
		var i = 0;
		if (!isStatic) {
			mv.visitVarInsn(ALOAD, 0);
			i++;
		}

		for (var type : types) {
			InstUtils.load(mv, type, i);
			i += type.getSize();
		}

		mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, bsm.getOwner(), bsm.getName(), desc, false);

		InstUtils.ret(mv, Type.getReturnType(desc));
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	
		return name;
	}

	private Type[] parameters(Handle method, Type[] captures) {
		var params = Type.getArgumentTypes(method.getDesc());
		var skip = params.length == captures.length ? 0 : captures.length;
		return Arrays
				.stream(params)
				.skip(skip)
				.toArray(Type[]::new);
	}

	
	private void instantiate(String className, String constructor, Type[] arguments) {
		var args = new Integer[arguments.length];
		for (var i = arguments.length - 1; i >= 0; i--) {
			var arg = arguments[i];
			args[i] = mv.newLocal(arg);
			InstUtils.store(mv, arg, args[i]);
		}
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		for (var i = 0; i < arguments.length; i++) {
			InstUtils.load(mv, arguments[i], args[i]);
		}
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", constructor, false);
	}
}
