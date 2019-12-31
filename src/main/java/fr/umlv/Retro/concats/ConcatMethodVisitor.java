package fr.umlv.Retro.concats;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.Retro;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodFeatureVisitor;
import fr.umlv.Retro.models.MethodInfo;

/**
 * Transforms string concatenation feature from Java 9 (implemented using
 * InvokeDynamic) by an implementation using StringBuilder in lower versions.
 */
public class ConcatMethodVisitor implements MethodFeatureVisitor {

	@Override
	public MethodVisitor visit(Retro app, ClassInfo ci, MethodInfo mi) {
		return new ConcatDetector(app, ci, mi, this);
	}

	@Override
	public boolean canDetect(Features feature) {
		if (feature == null) {
			throw new IllegalArgumentException("app");
		}
		return Features.Concat.equals(feature);
	}
	
	@Override
	public boolean canRewrite(Retro app) {
		if (app == null) {
			throw new IllegalArgumentException("app");
		}
		return app.target() < 9 && app.hasFeature(Features.Concat);
	}

}
