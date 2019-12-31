package fr.umlv.Retro.nestmates;

import java.util.Objects;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import fr.umlv.Retro.Retro;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;

/**
 * Replaces private members access of an outer class from it's inner classes.
 */
public class NestMateDetector extends MethodVisitor implements Opcodes {
	private final Retro app;
	private final ClassInfo ci;
	private final NestMateMethodVisitor vi;

	public NestMateDetector(Retro app, ClassInfo ci, MethodInfo mi, NestMateMethodVisitor vi) {
		super(ci.api(), mi.visitor());
		this.ci = ci;
		this.vi = Objects.requireNonNull(vi);
		this.app = Objects.requireNonNull(app);
	}
	
	// TODO skip non private fields
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		var rewritten = false;
		if (owner.equals(ci.nestHost())) {
			var rewriter = vi.findRewriter(owner, name, descriptor);
			if (vi.canRewrite(app)) {
				rewriter.rewrite(this, opcode);
				rewritten = true;
			}	
		}
		if (!rewritten) {
			super.visitFieldInsn(opcode, owner, name, descriptor);				
		}
	}
	
	// TODO skip non private methods
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		var rewritten = false;
		if (owner.equals(ci.nestHost()) && !name.contains("$")) {
			var rewriter = vi.findRewriter(owner, name, descriptor);
			if (vi.canRewrite(app)) {
				rewriter.rewrite(this, opcode);
				rewritten = true;
			}	
		}
		if (!rewritten) {
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}

}