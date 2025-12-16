package fr.umlv.retro.models;

import org.objectweb.asm.ClassVisitor;

import fr.umlv.retro.ClassTransformer;


/**
 * Interface implemented by a ClassVisitor able to detect and rewrite a feature.
 */
public interface FeatureVisitor {

	/**
	 * 
	 * @param ci information about the visited class.
	 * @param cv the class visitor to which the calls of visitMethods should be delegated.
	 * @param tr current class transformer.
	 * @return The ClassVisitor itself.
	 */
	ClassVisitor visit(ClassInfo ci, ClassVisitor cv, ClassTransformer tr);
}
