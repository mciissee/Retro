package fr.umlv.Retro.lambdas;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.MethodFeatureVisitor;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;

/**
 * Transform lambda functions from the method to Anonymous functions
 * if target version is lower than JDK 8.
 */
public class LambdaMethodVisitor implements MethodFeatureVisitor {
	
	@Override
	public MethodVisitor visit(ClassInfo ci, MethodInfo mi, TransformOptions options) {
		return new LambdaDetector(ci, mi, options);
	}

}
