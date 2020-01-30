package fr.umlv.Retro.lambdas;

import java.util.HashMap;
import java.util.Objects;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import fr.umlv.Retro.ClassTransformer;
import fr.umlv.Retro.Retro;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.FeatureVisitor;
import fr.umlv.Retro.models.MethodInfo;

public class LambdaVisitor extends ClassVisitor implements FeatureVisitor {
	
	private final Retro app;
	private final HashMap<String, Integer> lambdas = new HashMap<String, Integer>();
	private ClassInfo ci;
	
	public LambdaVisitor(Retro app) {
		super(Objects.requireNonNull(app).api());
		this.app = app;
	}

	public ClassVisitor visit(ClassInfo ci, ClassVisitor cv,  ClassTransformer tr) {
		this.ci = Objects.requireNonNull(ci);
		this.cv = Objects.requireNonNull(cv);
		return this;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		var mi = new MethodInfo(access, name, descriptor, exceptions, mv);
		return new LambdaDetector(app, ci, mi, this::nextName);
	}

	private String nextName() {
		var k = ci.path().toString();
		var e = lambdas.get(k);
		if (e == null) {
			e = 0;
		}
		var name =  ci.className() + "$Lambda$" + ++e;
		lambdas.put(k, e);
		return name;
	}
}
