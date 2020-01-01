package fr.umlv.Retro.lambdas;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.Retro;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodFeatureVisitor;
import fr.umlv.Retro.models.MethodInfo;

public class LambdaMethodVisitor implements MethodFeatureVisitor {
	
	@Override
	public MethodVisitor visit(Retro app, ClassInfo ci, MethodInfo mi) {
		return new LambdaDetector(app, ci, mi, this);
	}

	@Override
	public boolean canDetect(Features feature) {
		if (feature == null) {
			throw new IllegalArgumentException("app");
		}
		return Features.Lambda.equals(feature);
	}
	
	@Override
	public boolean canRewrite(Retro app) {
		if (app == null) {
			throw new IllegalArgumentException("app");
		}
		return app.target() < 8 && app.hasFeature(Features.Lambda);
	}
	
}
