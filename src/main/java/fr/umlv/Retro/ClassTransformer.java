package fr.umlv.Retro;

import java.util.Objects;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import fr.umlv.Retro.concats.ConcatMethodVisitor;
import fr.umlv.Retro.lambdas.LambdaMethodVisitor;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.models.TransformOptions;
import fr.umlv.Retro.models.VersionInfo;

/**
 * Transforms byte code from a version to another version.
 */
public class ClassTransformer extends ClassVisitor implements Opcodes {

	private final MethodFeatureVisitor[] methodVisitors =  {
			new LambdaMethodVisitor(),
			new ConcatMethodVisitor(),
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
		this.version = VersionInfo.fromMajor(version);
		this.className = name;
	    super.visit(VersionInfo.toMajor(options.target()), access, name, signature, superName, interfaces);

	}
	
	@Override
	public void visitNestMember(String nestMember) {
		System.out.println(
				String.format(
					"NESTMATES AT %s (%s.java) nestmate of %s",
					nestMember, className, className
		));
		super.visitNestMember(nestMember);
	}
	

	@Override
	public void visitNestHost(String nestHost) {
		System.out.println("nest host" + nestHost);
		super.visitNestHost(nestHost);
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
					access,
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