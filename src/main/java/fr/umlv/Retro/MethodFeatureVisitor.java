package fr.umlv.Retro;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;

/**
 * Visit method code to detect and rewrite a feature.
 */
public interface MethodFeatureVisitor {
	
	/**
	 * Visit the method code.
	 * @param app instance of the application facade.
	 * @param ci informations about the method's class.
	 * @param mi informations about the method.
	 * @return new MethodVisitor
	 */
	MethodVisitor visit(Retro app, ClassInfo ci, MethodInfo mi);
}
