package fr.umlv.retro.trywithresources;

import java.util.Objects;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import fr.umlv.retro.Retro;
import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.MethodInfo;

/**
 * Backports lambda expressions to anonymous inner classes.
 */
class TryWithResourceRewriter implements Opcodes {

	private final Retro app;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private final TryWithResourceDetector mv;
	private final ClassWriter cw;

	public TryWithResourceRewriter(TryWithResourceDetector mv,  Retro app, ClassInfo ci, MethodInfo mi) {
		this.mv = Objects.requireNonNull(mv);
		this.ci = Objects.requireNonNull(ci);
		this.mi = Objects.requireNonNull(mi);
		this.app = Objects.requireNonNull(app);
		this.cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
	}
}