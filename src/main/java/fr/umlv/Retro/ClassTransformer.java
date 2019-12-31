package fr.umlv.Retro;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


import fr.umlv.Retro.concats.ConcatMethodVisitor;
import fr.umlv.Retro.lambdas.LambdaMethodVisitor;
import fr.umlv.Retro.models.ClassInfo;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.MethodFeatureVisitor;
import fr.umlv.Retro.models.MethodInfo;
import fr.umlv.Retro.nestmates.NestMateMemberDecriber;
import fr.umlv.Retro.nestmates.NestMateMethodVisitor;
import fr.umlv.Retro.utils.VersionUtils;

/**
 * Transforms byte code from a version to another version.
 */
public class ClassTransformer extends ClassVisitor implements Opcodes {

	private final MethodFeatureVisitor[] visitors;
	private final Retro app;
	private final Path path;
	private final String className;
	private final String fileName;

	private int version;
	private String nestHost;

	private final ArrayList<String> nestMembers = new ArrayList<>();

	private ClassTransformer(ClassVisitor cv, Retro app, Path path, MethodFeatureVisitor[] methodVisitors) {
		super(ASM7, Objects.requireNonNull(cv));
		this.app = Objects.requireNonNull(app);
		this.path = Objects.requireNonNull(path);
		this.visitors = Objects.requireNonNull(methodVisitors);
		this.fileName = path.getFileName().toString();
		this.className = this.fileName.replace(".class", "");
	}
	
	public static void transform(Retro app, Path path, byte[] bytes) {
		if (app == null) {
			throw new IllegalArgumentException("app");
		}
		if (path == null) {
			throw new IllegalArgumentException("path");
		}
		if (bytes == null) {
			throw new IllegalArgumentException("bytes");
		}

		transform(app, path, bytes, new MethodFeatureVisitor[] {
    		new LambdaMethodVisitor(),
    		new ConcatMethodVisitor(),
    		new NestMateMethodVisitor()
        });
	}

	private static void transform(Retro app, Path path, byte[] bytes, MethodFeatureVisitor[] visitors) {
		var cr = new ClassReader(bytes);
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ClassTransformer(cw, app, path, visitors), ClassReader.EXPAND_FRAMES);
        app.writeClass(path, cw.toByteArray());
	}


	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.version = VersionUtils.toJDK(version);
	    super.visit(VersionUtils.toBytecode(app.target()), access, name, signature, superName, interfaces);
	}
	
	@Override
	public void visitNestMember(String nestMember) {
		nestMembers.add(nestMember);
		app.onFeatureDetected(Features.NestMates, new NestMateMemberDecriber(className, nestMember));
		if (!canRewrite(Features.NestMates)) {
			super.visitNestMember(nestMember);			
		}
	}
		
	@Override
	public void visitNestHost(String nestHost) {
		this.nestHost = nestHost;
		if (!canRewrite(Features.NestMates)) {
			super.visitNestHost(nestHost);
		}
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (canRewrite(Features.NestMates)) {
			if (nestMembers.contains(name)) {
				try {
					app.openClassRelativeTo(path, name, (path, bytes) -> {
						transform(app, path, bytes, visitors);
					});
				} catch (FileNotFoundException e) {
					throw new AssertionError(e);
				} catch (IOException e) {
					throw new AssertionError(e);
				}
			}			
		}
		super.visitInnerClass(name, outerName, innerName, access);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {		
		var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		for (var visitor : visitors) {
			var ci = new ClassInfo(
					api,
					version,
					path,
					fileName,
					className,
					nestHost,
					cv
			);
			var mi = new MethodInfo(
					access,
					name,
					descriptor,
					exceptions == null ? new String[0] : exceptions,
					mv
			);
			mv = visitor.visit(app, ci, mi);
		}
		return mv;
	}
	
	@Override
	public void visitEnd() {
		for (var visitor : visitors) {
			visitor.visitEnd(app, new ClassInfo(api, version, path, fileName, className, nestHost, cv));
		}
		super.visitEnd();
	}

	private boolean canRewrite(Features feature) {
		for (var visitor : visitors) {
			if (visitor.isFor(feature)) {
				return visitor.canRewrite(app);
			}
		}
		return false;
	}

}