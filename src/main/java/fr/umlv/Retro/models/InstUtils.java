package fr.umlv.Retro.models;

import java.util.Map;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A helper class for bytecode generation with ASM.
 */
public class InstUtils implements Opcodes {

	public static void load(MethodVisitor mv, Type type, int var) {
		var e = type.toString();
		if (e.startsWith("L")) {
			mv.visitVarInsn(ALOAD, var);
		} else {
			var map = Map.of("I", ILOAD, "C", ILOAD, "S", ILOAD, "B", ILOAD, "Z", ILOAD, "J", LLOAD, "D", DLOAD, "F",
					FLOAD);
			mv.visitVarInsn(map.get(type.toString()), var);
		}
	}

	public static void store(MethodVisitor mv, Type type, int var) {
		var e = type.toString();
		if (e.startsWith("L")) {
			mv.visitVarInsn(ASTORE, var);
		} else {
			var map = Map.of("I", ISTORE, "C", ISTORE, "S", ISTORE, "B", ISTORE, "Z", ISTORE, "J", LSTORE, "D", DSTORE,
					"F", FSTORE);

			mv.visitVarInsn(map.get(type.toString()), var);
		}
	}

	public static void ret(MethodVisitor mv, Type type) {
		var e = type.toString();
		if (e.endsWith("V")) {
			mv.visitInsn(RETURN);
		} else if (e.startsWith("L")) {
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
	 */
	public static void box(MethodVisitor mv, Type from, Type to) {
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
	 */
	public static void unbox(MethodVisitor mv, Type from, Type to) {
		if (TypeUtils.isPrimitive(from)) {
			return;
		}
		var wrapper = from.getInternalName();
		var primitve = TypeUtils.primitive(to);
		mv.visitMethodInsn(INVOKEVIRTUAL, wrapper, primitve.getClassName() + "Value", "()" + primitve, false);
	}

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

	public static void push(MethodVisitor mv, int value) {
		switch (value) {
		case 0:
			mv.visitInsn(ICONST_0);
			break;
		case 1:
			mv.visitInsn(ICONST_1);
			break;
		case 2:
			mv.visitInsn(ICONST_2);
			break;
		case 3:
			mv.visitInsn(ICONST_3);
			break;
		case 4:
			mv.visitInsn(ICONST_4);
			break;
		case 5:
			mv.visitInsn(ICONST_5);
			break;
		default:
			if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
				mv.visitIntInsn(BIPUSH, value);
			} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
				mv.visitIntInsn(SIPUSH, value);
			} else {
				mv.visitLdcInsn(Integer.valueOf(value));
			}
		}
	}

}
