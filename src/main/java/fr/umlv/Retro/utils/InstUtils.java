package fr.umlv.retro.utils;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Provides static methods for bytecode generation with ASM.
 */
public class InstUtils implements Opcodes {

	/**
	 * Generates the bytecode to push the local variable {@code var} to the stack.
	 * 
	 * @param mv   method visitor
	 * @param type type of the variable
	 * @param var  index of the variable in local variables tables.
	 * @throws IllegalArgumentException  if any argument is null.
	 * @throws IndexOutOfBoundsException if var < 0
	 */
	public static void load(MethodVisitor mv, Type type, int var) {
		Contracts.requires(mv, "mv");
		Contracts.requires(type, "type");

		if (var < 0) {
			throw new IndexOutOfBoundsException("local variable index must be >= 0");
		}
		mv.visitVarInsn(type.getOpcode(ILOAD), var);
	}

	/**
	 * Generates the bytecode to pop the current value from the top of the stack
	 * into the local variable {@code var}.
	 * 
	 * @param mv   method visitor
	 * @param type type of the variable
	 * @param var  index of the variable in local variables tables.
	 * @throws IllegalArgumentException  if any argument is null.
	 * @throws IndexOutOfBoundsException if {@code var} < 0
	 */
	public static void store(MethodVisitor mv, Type type, int var) {
		Contracts.requires(mv, "mv");
		Contracts.requires(type, "type");
		if (var < 0) {
			throw new IndexOutOfBoundsException("local variable index must be >= 0");
		}
		mv.visitVarInsn(type.getOpcode(ISTORE), var);
	}

	/**
	 * Generates the bytecode to return from the current method.
	 * 
	 * @param mv   method visitor.
	 * @param type return type
	 * @throws IllegalArgumentException if any argument is null.
	 */
	public static void ret(MethodVisitor mv, Type type) {
		Contracts.requires(mv, "mv");
		Contracts.requires(type, "type");
		mv.visitInsn(type.getOpcode(IRETURN));
	}

	/**
	 * Generates the bytecode to convert the current primitive (int, long, double)
	 * value on the stack to it's wrapper type (Integer, Long, Double).
	 * 
	 * @param mv   method visitor
	 * @param from current type on the stack (primitive).
	 * @param to   the target type (wrapper).
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
	 * @param to   the target type (primitive).
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
	 * 
	 * @param mv   method visitor
	 * @param from source type
	 * @param to   target type
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
