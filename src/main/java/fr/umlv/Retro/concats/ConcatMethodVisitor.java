package fr.umlv.Retro.concats;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.MethodFeatureVisitor;
import fr.umlv.Retro.Retro;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;

/**
 * Transforms string concatenation feature from Java 9 (implemented using
 * InvokeDynamic) by an implementation using StringBuilder in lower versions.
 */
public class ConcatMethodVisitor implements MethodFeatureVisitor {

	@Override
	public MethodVisitor visit(Retro app, ClassInfo ci, MethodInfo mi) {
		return new ConcatDetector(app, ci, mi);
	}

}
