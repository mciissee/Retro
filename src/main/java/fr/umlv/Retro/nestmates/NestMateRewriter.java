package fr.umlv.retro.nestmates;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import fr.umlv.retro.utils.InstUtils;

public class NestMateRewriter implements Opcodes {

	private final Map<Integer, BiConsumer<MethodVisitor, Integer>> callers = Map.of(
			GETFIELD, this::callGetter,
			GETSTATIC, this::callGetter,
			
			PUTFIELD, this::callSetter,
			PUTSTATIC, this::callSetter,
			
			INVOKESTATIC, this::callMethod,
			INVOKEVIRTUAL, this::callMethod
	);
	
	private final Map<Integer, Consumer<ClassVisitor>> creators = Map.of(
			GETFIELD, this::createGetter,
			GETSTATIC, this::createGetter,
			
			PUTFIELD, this::createSetter,
			PUTSTATIC, this::createSetter,
			
			INVOKESTATIC, this::createMethod,
			INVOKEVIRTUAL, this::createMethod
	);
		
	private final int id;
	private final String className;
	private final String memberName;
	private final String descriptor;
	private final HashSet<Integer> calls = new HashSet<>();

	public NestMateRewriter(int id, String className, String fieldName, String descriptor) {
		this.id = id;
		this.className = Objects.requireNonNull(className);
		this.memberName = Objects.requireNonNull(fieldName);
		this.descriptor = Objects.requireNonNull(descriptor);
	}

	public NestMateRewriter(NestMateRewriter access, String className, String fieldName, String descriptor) {
		this(access.id, className, fieldName, descriptor);
	}

	/**
	 * Replaces outer class member access from an inner class.
	 * 
	 * This method is called while visiting a method inside of an inner class
	 * in order to replace access to members of outer class with a call to a dynamically
	 * generated static package-level method.
	 *
	 * @param mv method visitor
	 * @param opcode visitMethodInsn opcode argument.
	 * @return true if the rewrite is succeeded false otherwise.
	 */
	public boolean rewrite(MethodVisitor mv, int opcode) {
		if (mv == null) {
			throw new IllegalArgumentException("mv");
		}
		calls.add(opcode);
		var function  = this.callers.get(opcode);
		if (function != null) {
			function.accept(mv, opcode);
			return true;
		}
		return false;
	}

	/**
	 * Generates static package-level method into a outer class.
	 * 
	 *  This method is called while visiting an outer class.
	 *
	 * @param cv class visitor
	 * @return true if the rewrite is succeeded false otherwise.
	 */
	public boolean rewrite(ClassVisitor cv) {
		if (cv == null) {
			throw new IllegalArgumentException("cv");
		}
		var i = 0;
		for (var opcode : calls) {
			var function  = this.creators.get(opcode);
			if (function != null) {
				function.accept(cv);
				i++;
			}
		}
		return i > 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NestMateRewriter)) {
			return false;
		}
		var o = (NestMateRewriter) obj;
		return o.id == id && o.className.equals(className) && o.memberName.equals(memberName)
				&& o.descriptor.equals(descriptor);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id, className, memberName, descriptor);
	}
	
	private void callGetter(MethodVisitor mv, int opcode) {
		var desc = String.format("(L%s;)%s", className, descriptor);
		mv.visitMethodInsn(INVOKESTATIC, className, "access$" + id, desc, false);
	}
	
	private void callSetter(MethodVisitor mv, int opcode) {
		var desc = String.format("(L%s;%s)%s", className, descriptor, descriptor);
		mv.visitMethodInsn(INVOKESTATIC, className, "access$" + id, desc, false);
	}
		
	private void callMethod(MethodVisitor mv, int opcode) {
		var args = Type.getArgumentTypes(descriptor);
		var retType = Type.getReturnType(descriptor);
		var desc = createDescriptor(args, retType);
		mv.visitMethodInsn(INVOKESTATIC, className, "access$" + id, desc, false);
	}

	private void createGetter(ClassVisitor cv) {
		var desc = String.format("(L%s;)%s", className, descriptor);
		var mv = cv.visitMethod(ACC_SYNTHETIC | ACC_STATIC, "access$" + id, desc, null, null);
		mv.visitCode();
		InstUtils.load(mv, Type.getType("L" + className + ";"), 0);
		mv.visitFieldInsn(GETFIELD, className, memberName, descriptor);
		InstUtils.ret(mv, Type.getType(descriptor));
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private void createSetter(ClassVisitor cv) {
		var desc = String.format("(L%s;%s)%s", className, descriptor, descriptor);
		var mv = cv.visitMethod(ACC_SYNTHETIC | ACC_STATIC, "access$" + id, desc, null, null);
		mv.visitCode();
		InstUtils.load(mv, Type.getType("L" + className + ";"), 0);
		InstUtils.load(mv, Type.getType(descriptor), 1);
		mv.visitFieldInsn(PUTFIELD, className, memberName, descriptor);
		InstUtils.load(mv, Type.getType(descriptor), 1);
		InstUtils.ret(mv, Type.getType(descriptor));
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private void createMethod(ClassVisitor cv) {
		var args = Type.getArgumentTypes(descriptor);
		var retType = Type.getReturnType(descriptor);
		var desc = createDescriptor(args, retType);
		var mv = cv.visitMethod(ACC_SYNTHETIC | ACC_STATIC, "access$" + id, desc, null, null);
		mv.visitCode();
		InstUtils.load(mv, Type.getType("L" + className + ";"), 0);
		IntStream.range(0, args.length).forEach(i -> {
			InstUtils.load(mv, args[i], i + 1);
		});	
		mv.visitMethodInsn(calls.toArray(Integer[]::new)[0], className, memberName, descriptor, false);
		InstUtils.ret(mv, retType);
		mv.visitMaxs(1, 1);
		mv.visitEnd();	
	}

	private String createDescriptor(Type[] args, Type retType) {
		var desc = String.format("(L%s;%s)%s",
				className,
				Arrays.stream(args).map(e -> e.toString()).collect(Collectors.joining()),
				retType
		);
		return desc;
	}

}
