package fr.umlv.retro.mocker;

import java.util.Objects;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import fr.umlv.retro.Retro;
import fr.umlv.retro.models.MethodInfo;

public class MockerMethodDetector extends MethodVisitor {
	private final MockerVisitor mocker;

	public MockerMethodDetector(Retro app, MethodInfo mi, MockerVisitor mocker) {
		super(Objects.requireNonNull(app).api(), Objects.requireNonNull(mi).visitor());
		this.mocker = Objects.requireNonNull(mocker);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return super.visitAnnotation(mocker.mock(descriptor), visible);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		super.visitFieldInsn(opcode, mocker.mock(owner), name, mocker.mock(descriptor));
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		super.visitMethodInsn(opcode, mocker.mock(owner), name, mocker.mock(descriptor), isInterface);
	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... args) {
		descriptor = mocker.mock(descriptor);
		for (var i = 0; i < args.length; i++) {
			var arg = args[i];
			if (arg instanceof Type) {
				args[i] = mocker.mock((Type)arg);
			} else if(arg instanceof Handle) {
				var h = (Handle) arg;
				args[i] = new Handle(
					h.getTag(),
					mocker.mock(h.getOwner()),
					h.getName(),
					mocker.mock(h.getDesc()),
					h.isInterface()
				);			
			}
		}
		super.visitInvokeDynamicInsn(name, descriptor, handle, args);
	}

	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
		super.visitLocalVariable(name, mocker.mock(descriptor), signature, start, end, index);
	}
		
	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		super.visitMultiANewArrayInsn(mocker.mock(descriptor), numDimensions);
	}
}

