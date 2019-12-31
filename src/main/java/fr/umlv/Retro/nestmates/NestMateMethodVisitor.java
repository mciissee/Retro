package fr.umlv.Retro.nestmates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.Retro;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodFeatureVisitor;
import fr.umlv.Retro.models.MethodInfo;

public class NestMateMethodVisitor implements MethodFeatureVisitor {
	
	private final HashMap<String, HashSet<String>> nestMembers = new HashMap<>();
	private final HashMap<String, List<NestMateRewriter>> memberRewriters = new HashMap<>();

	@Override
	public MethodVisitor visit(Retro app, ClassInfo ci, MethodInfo mi) {
		var host = ci.nestHost();
		if (host != null) {
			var members = nestMembers.get(host);
			if (members == null) {
				members = new HashSet<String>();
				nestMembers.put(host, members);
			}
			members.add(ci.className());
		}
		return new NestMateDetector(app, ci, mi, this);
	}
	
	@Override
	public boolean canDetect(Features feature) {
		if (feature == null) {
			throw new IllegalArgumentException("app");
		}
		return Features.NestMates.equals(feature);
	}
	
	@Override
	public boolean canRewrite(Retro app) {
		if (app == null) {
			throw new IllegalArgumentException("app");
		}
		return app.target() < 11 && app.hasFeature(Features.NestMates);
	}

	@Override
	public void visitEnd(Retro app, ClassInfo ci) {
		if (canRewrite(app)) {
			var rewriters = memberRewriters.get(ci.className());
			if (rewriters != null) {
				rewriters.forEach(e -> e.rewrite(ci.visitor()));
				memberRewriters.remove(ci.className());
			}
			var members = nestMembers.get(ci.className());
			if (members != null) {
				app.detectFeature(ci.path(), Features.NestMates, new NestMateHostDescriber(ci.className(), members));
				nestMembers.remove(ci.className());
			}
		}
	}

	NestMateRewriter findRewriter(String className, String fieldName, String descriptor) {
		var rewriters = memberRewriters.get(className);
		if (rewriters == null) {
			rewriters = new ArrayList<>();
			memberRewriters.put(className, rewriters);
		}

		for (var e : rewriters) {
			if (e.equals(new NestMateRewriter(e, className, fieldName, descriptor))) {
				return e;
			}
		}
		
		var rewriter = new NestMateRewriter(rewriters.size() + 1, className, fieldName, descriptor);
		rewriters.add(Objects.requireNonNull(rewriter));
		return rewriter;
	}
	
}
