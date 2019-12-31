package fr.umlv.Retro.utils;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

/**
 * Provides static methods for ASM Type manipulations.
 */
public class TypeUtils {

	/**
	 * Gets a value indicating whether the given type is a primitive type (except void)
	 * @param type the type
	 * @return true if type is a primitive.
	 */
	public static boolean isPrimitive(Type type) {
		return List.of("Z", "C", "B", "S", "I", "F", "D", "J").contains(type.toString());
	}
	
	/**
	 * Gets a value indicating whether the given type is a primitive wrapper type
	 * @param type the type
	 * @return true if type is a primitive.
	 */
	public static boolean isWrapper(Type type) {
		return List.of(
			"Ljava/lang/Boolean;", "Ljava/lang/Character;",
			"Ljava/lang/Byte;", "Ljava/lang/Short;",
			"Ljava/lang/Integer;", "Ljava/lang/Float;",
			"Ljava/lang/Double;", "Ljava/lang/Long;" 
		)
		.contains(type.toString());
	}
	
	/**
	 * Converts the primitive "type" to it's wrapper version if possible (int -> Integer, double -> Double...)
	 * @param type the type
	 * @return A wrapper type or the type if it's not a primitive.
	 */
	public static Type wrapper(Type type) {
		if (isWrapper(type)) {
			return type;
		}
		return Map.of(
			"Z", Type.getType(Boolean.class), "C", Type.getType(Character.class),
			"B", Type.getType(Byte.class), "S", Type.getType(Short.class),
			"I", Type.getType(Integer.class), "F", Type.getType(Float.class),
			"D", Type.getType(Double.class), "J", Type.getType(Long.class)
		)
		.get(type.toString());
	}
	
	/**
	 * Converts the wrapper "type" to it's wrapper version if possible (Integer -> int, Double -> double...)
	 * @param type the type
	 * @return A primitive type or the type if it's not a wrapper.
	 */
	public static Type primitive(Type type) {
		if (isPrimitive(type)) {
			return type;
		}
		return Map.of(
			"Ljava/lang/Boolean;", Type.getType(boolean.class), "Ljava/lang/Character;", Type.getType(char.class),
			"Ljava/lang/Byte;", Type.getType(byte.class), "Ljava/lang/Short;", Type.getType(short.class),
			"Ljava/lang/Integer;", Type.getType(int.class), "Ljava/lang/Float;", Type.getType(float.class),
			"Ljava/lang/Double;", Type.getType(double.class), "Ljava/lang/Long;", Type.getType(long.class)
		)
		.get(type.toString());
	}
}
