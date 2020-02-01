package fr.umlv.retro.concats;

import java.util.Objects;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.FeatureVisitor;
import fr.umlv.retro.models.MethodInfo;
import fr.umlv.retro.ClassTransformer;
import fr.umlv.retro.Retro;

/**
 * Transforms string concatenation feature from Java 9 (implemented using
 * InvokeDynamic) by an implementation using StringBuilder in lower versions.
 */
public class ConcatVisitor extends ClassVisitor implements FeatureVisitor {

	private final Retro app;
	private ClassInfo ci;

	public ConcatVisitor(Retro app) {
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
		return new ConcatDetector(app, ci, new MethodInfo(access, name, descriptor, exceptions, mv));
	}
	
}
