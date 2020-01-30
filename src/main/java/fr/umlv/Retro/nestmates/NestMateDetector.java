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
	private final ClassInfo ci;
	private final NestMateVisitor vi;

	public NestMateDetector(Retro app, ClassInfo ci, MethodInfo mi, NestMateVisitor vi) {
		super(app.api(), mi.visitor());
		this.ci = ci;
		this.vi = Objects.requireNonNull(vi);
	}
	
	// TODO skip non private fields
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		var rewritten = false;
		if (owner.equals(ci.nestHost())) {
			rewritten = vi.detectNestMate(owner, name,  descriptor, rw -> rw.rewrite(this, opcode));	
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
			rewritten = vi.detectNestMate(owner, name,  descriptor, rw -> rw.rewrite(this, opcode));
		}
		if (!rewritten) {
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}

}