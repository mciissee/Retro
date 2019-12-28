package fr.umlv.Retro.concats;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.MethodFeatureVisitor;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;

/**
 * Transforms string concatenation feature from Java 9 (implemented using
 * InvokeDynamic) by an implementation using StringBuilder in lower versions.
 */
public class ConcatMethodVisitor implements MethodFeatureVisitor {

	@Override
	public MethodVisitor visit(ClassInfo ci, MethodInfo mi, TransformOptions options) {
		return new ConcatDetector(ci, mi, options);
	}

}
