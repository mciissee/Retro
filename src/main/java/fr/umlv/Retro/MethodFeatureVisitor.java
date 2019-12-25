package fr.umlv.Retro;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;

/**
 * Visit method code to detect and rewrite a feature.
 */
public interface MethodFeatureVisitor {
	
	/**
	 * Visit the method code.
	 * @param ci informations about the method's class.
	 * @param mi informations about the method.
	 * @param options transform options.
	 * @return new MethodVisitor
	 */
	MethodVisitor visit(ClassInfo ci, MethodInfo mi, TransformOptions options);
}
