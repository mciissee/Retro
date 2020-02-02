package fr.umlv.retro.records;

import java.util.Arrays;
import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import fr.umlv.retro.Retro;
import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.Features;
import fr.umlv.retro.models.MethodInfo;

public class RecordDetector extends LocalVariablesSorter implements Opcodes {

	private final Retro app;
	private final ClassInfo ci;
	private final MethodInfo mi;

	public RecordDetector(Retro app, ClassInfo ci, MethodInfo mi) {
		super(
			Objects.requireNonNull(app).api(),
			Objects.requireNonNull(mi).access(),
			Objects.requireNonNull(mi).descriptor(),
			Objects.requireNonNull(mi).visitor()
		);
		this.app = app;
		this.ci = ci;
		this.mi = mi;
	}
	

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		if (owner.equals("java/lang/Record")) {
			owner = "java/lang/Object";
		}
		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
		var rewritten = false;
		if (handle.getOwner().equals("java/lang/runtime/ObjectMethods")) {
			if (app.target() < 14 && app.hasFeature(Features.Record)) {
				var rewriter = new RecordRewriter(ci, mi);
				rewriter.rewrite(name, Arrays.stream(args).skip(2).toArray(Handle[]::new));
				rewritten = true;
			}
		}	
		if (!rewritten) {
			super.visitInvokeDynamicInsn(name, descriptor, handle, args);
		}
	}

}