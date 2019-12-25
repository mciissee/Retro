package fr.umlv.Retro;

import java.util.Objects;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;
import fr.umlv.Retro.models.VersionInfo;

/**
 * Transforms byte code from a version to another version.
 */
public class ClassTransformer extends ClassVisitor implements Opcodes {

	private final MethodFeatureVisitor[] methodVisitors =  {
	};
	
	private final TransformOptions options;

	private int version;
	private String className;
	private String fileName;

	public ClassTransformer(ClassVisitor cv, TransformOptions options) {
		super(ASM7, Objects.requireNonNull(cv));

		this.options = Objects.requireNonNull(options);
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.version = VersionInfo.fromMajor(version);
		this.className = name;
	}
	
	@Override
	public void visitSource(String source, String debug) {
		this.fileName = source;
		super.visitSource(source, debug);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {		
		var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		for (var visitor : methodVisitors) {
			var ci = new ClassInfo(
					api,
					version,
					fileName,
					className,
					cv
			);
			var mi = new MethodInfo(
					name,
					descriptor,
					exceptions == null ? new String[0] : exceptions,
					mv
			);
			mv = visitor.visit(ci, mi, options);
		}
		return mv;
	}

}