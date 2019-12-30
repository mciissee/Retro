package fr.umlv.Retro.lambdas;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.MethodFeatureVisitor;
import fr.umlv.Retro.Retro;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;

/**
 * Transform lambda functions from the method to Anonymous functions
 * if target version is lower than JDK 8.
 */
public class LambdaMethodVisitor implements MethodFeatureVisitor {
	
	@Override
	public MethodVisitor visit(Retro app, ClassInfo ci, MethodInfo mi) {
		return new LambdaDetector(app, ci, mi);
	}

}
