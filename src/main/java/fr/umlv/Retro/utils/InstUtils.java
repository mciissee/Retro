package fr.umlv.Retro.utils;

import java.util.Map;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Provides static methods for bytecode generation with ASM.
 */
public class InstUtils implements Opcodes {

	/**
	 * Generates the bytecode to push the local variable `var` to the stack.
	 * @param mv method visitor
	 * @param type type of the variable
	 * @param var index of the variable in local variables tables.
	 * @throws IllegalArgumentException if any argument is null.
	 * @throws IndexOutOfBoundsException if var < 0
	 */
	public static void load(MethodVisitor mv, Type type, int var) {
		Contracts.requires(mv, "mv");
		Contracts.requires(type, "type");

		if (var < 0) {
			throw new IndexOutOfBoundsException("local variable index must be >= 0");
		}

		var e = type.toString();
		if (e.startsWith("L") || e.startsWith("[")) {
			mv.visitVarInsn(ALOAD, var);
		} else {
			var map = Map.of("I", ILOAD, "C", ILOAD, "S", ILOAD, "B", ILOAD, "Z", ILOAD, "J", LLOAD, "D", DLOAD, "F",
					FLOAD);
			mv.visitVarInsn(map.get(type.toString()), var);
		}
	}

	/**
	 * Generates the bytecode to pop the current value from the top of the stack into the local variable `var`.
	 * @param mv method visitor
	 * @param type type of the variable
	 * @param var index of the variable in local variables tables.
	 * @throws IllegalArgumentException if any argument is null.
	 * @throws IndexOutOfBoundsException if var < 0
	 */
	public static void store(MethodVisitor mv, Type type, int var) {
		Contracts.requires(mv, "mv");
		Contracts.requires(type, "type");
		if (var < 0) {
			throw new IndexOutOfBoundsException("local variable index must be >= 0");
		}
		var e = type.toString();
		if (e.startsWith("L") || e.startsWith("[")) {
			mv.visitVarInsn(ASTORE, var);
		} else {
			var map = Map.of("I", ISTORE, "C", ISTORE, "S", ISTORE, "B", ISTORE, "Z", ISTORE, "J", LSTORE, "D", DSTORE,
					"F", FSTORE);

			mv.visitVarInsn(map.get(type.toString()), var);
		}
	}

	/**
	 * Generates the bytecode to return from the current method.
	 * @param mv method visitor.
	 * @param type return type
	 * @throws IllegalArgumentException if any argument is null.
	 */
	public static void ret(MethodVisitor mv, Type type) {
		Contracts.requires(mv, "mv");
		Contracts.requires(type, "type");

		var e = type.toString();
		if (e.endsWith("V")) {
			mv.visitInsn(RETURN);
		} else if (e.startsWith("L") || e.startsWith("[")) {
			mv.visitInsn(ARETURN);
		} else {
			var map = Map.of("I", IRETURN, "C", IRETURN, "S", IRETURN, "B", IRETURN, "Z", IRETURN, "J", LRETURN, "D",
					DRETURN, "F", FRETURN);
			mv.visitInsn(map.get(type.toString()));
		}
	}

	/**
	 * Generates the bytecode to convert the current primitive (int, long, double)
	 * value on the stack to it's wrapper type (Integer, Long, Double).
	 * 
	 * @param mv method visitor
	 * @param from current type on the stack (primitive).
	 * @param to the target type (wrapper).
	 * @throws IllegalArgumentException if any argument is null.
	 */
	public static void box(MethodVisitor mv, Type from, Type to) {
		Contracts.requires(mv, "mv");
		Contracts.requires(from, "from");
		Contracts.requires(to, "to");

		if (TypeUtils.isWrapper(from)) {
			return;
		}

		var wrapper = TypeUtils.wrapper(to);
		var primitive = TypeUtils.primitive(to);
		var desc = String.format("(%s)%s", primitive.toString(), wrapper.toString());
		mv.visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf", desc, false);
	}

	/**
	 * Generates the bytecode to convert the current wrapper (Integer, Long, Double)
	 * value on the stack to the given primitive type `to`.
	 * 
	 * @param mv   method visitor
	 * @param from current type on the stack (wrapper).
	 * @param to the target type (primitive).
	 * @throws IllegalArgumentException if any argument is null.
	 */
	public static void unbox(MethodVisitor mv, Type from, Type to) {
		Contracts.requires(mv, "mv");
		Contracts.requires(from, "from");
		Contracts.requires(to, "to");

		if (TypeUtils.isPrimitive(from)) {
			return;
		}
		var wrapper = from.getInternalName();
		var primitve = TypeUtils.primitive(to);
		mv.visitMethodInsn(INVOKEVIRTUAL, wrapper, primitve.getClassName() + "Value", "()" + primitve, false);
	}

	/**
	 * Generates the bytecode to do a cast. 
	 * @param mv method visitor
	 * @param from source type
	 * @param to target type
	 */
	public static void cast(MethodVisitor mv, Type from, Type to) {
		if (TypeUtils.isPrimitive(from) && TypeUtils.isWrapper(to)) {
			InstUtils.box(mv, from, to);
		} else if (TypeUtils.isWrapper(from) && TypeUtils.isPrimitive(to)) {
			InstUtils.unbox(mv, from, to);
		} else if (TypeUtils.isPrimitive(from) && TypeUtils.isPrimitive(to)) {
			// TODO use the right operand depending on the types.
			switch (to.toString()) {
			case "D":
				mv.visitInsn(I2D);
				break;
			}
		} else {
			mv.visitTypeInsn(CHECKCAST, to.getInternalName());
		}
	}

}
