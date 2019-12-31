package fr.umlv.Retro.models;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.Retro;

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
	
	/**
	 * Gets a value indicating whether the visitor is for the given feature.
	 * @param feature the feature.
	 * @return true if the visitor is for the feature false otherwise.
	 */
	boolean canDetect(Features feature);	

	/**
	 * Gets a value indicating whether the visitor can rewrite the feature depending on application state.
	 * @param app application instance.
	 * @return true if can rewrite false otherwise.
	 */
	boolean canRewrite(Retro app);

	/**
	 * Handles class visit end event.
	 * @param app instance of the application facade.
	 * @param ci informations about the method's class.
	 */
	default void visitEnd(Retro app, ClassInfo ci) {
	}
}
