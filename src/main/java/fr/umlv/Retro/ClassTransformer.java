package fr.umlv.retro;

import java.nio.file.Path;
import java.util.Objects;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.utils.Contracts;
import fr.umlv.retro.utils.VersionUtils;

/**
 * Visit a bytecode to backport bytecode it to a specific version of Java.
 */
public class ClassTransformer extends ClassVisitor implements Opcodes {

	private final Path path;
	private final Retro app;
	private final ClassWriter writer;
	private final ClassTransformer parent;
	private int version;

	private ClassTransformer(Retro app, Path path, ClassWriter writer, ClassTransformer parent) {
		super(ASM7);
		this.app = Objects.requireNonNull(app);
		this.path = Objects.requireNonNull(path);
		this.writer = Objects.requireNonNull(writer);
		this.parent = parent;
	}
	
	/**
	 * Transforms the given {@code bytecode} according to {@code app} configuration.
	 * @param app application instance
	 * @param path path to the bytecode
	 * @param bytecode the bytecode
	 */
	public static void transform(Retro app, Path path, byte[] bytecode) {
		transform(app, path, bytecode, null);
	}

	/**
	 * Transforms the given {@code bytecode} according to {@code app} configuration.
	 * @param app application instance
	 * @param path path to the bytecode
	 * @param bytecode the bytecode
	 * @param parent reference to parent transformer in case of inner class
	 * (suspend the parent visitor and resume once the new visitor has finished)
	 */
	public static void transform(Retro app, Path path, byte[] bytecode, ClassTransformer parent) {
		Contracts.requires(app, "app");
		Contracts.requires(path, "path");
		Contracts.requires(bytecode, "bytecode");
		var cr = new ClassReader(bytecode);
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cr.accept(new ClassTransformer(app, path, cw, parent), ClassReader.EXPAND_FRAMES);
        app.write(path, cw.toByteArray());
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		visit(version);
		super.visit(VersionUtils.toBytecode(app.target()), access, name, signature, superName, interfaces);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		if (parent != null) {
			parent.visit(parent.version);
		}		
	}

	private void visit(int version) {
		this.version = version;
		app.visitFeatures(visitors -> {			
			var n = visitors.length - 1;
			var fileName = path.getFileName().toString();
			var className = fileName.replace(".class", "");
			var ci = new ClassInfo(version, path, fileName, className, writer);
			var next = visitors[n].visit(ci, writer, this);
			for (var i = n - 1; i >= 0; i--) {
				next = visitors[i].visit(ci, next, this);
			}
			this.cv = next;
		});
	}
	
}