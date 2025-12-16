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
		
		// Create a custom ClassWriter that handles missing classes gracefully
		// when computing common super class for frame generation
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            /**
             * Overrides the default implementation to prevent {@link TypeNotPresentException}
             * when ASM tries to load classes that are not in the classpath (e.g., ASM internal
             * classes like CheckMethodAdapter$MethodWriterWrapper, or application classes).
             * 
             * <p>When the class hierarchy cannot be determined, we fall back to returning
             * {@code java/lang/Object} as the common superclass, which is always safe and
             * allows the transformation to proceed without errors.
             * 
             * @param type1 the internal name of a class
             * @param type2 the internal name of another class
             * @return the internal name of the common super class of the two given classes,
             *         or {@code java/lang/Object} if the classes cannot be loaded
             */
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (TypeNotPresentException e) {
                    // If we can't load the class (e.g., ASM internal classes or missing dependencies),
                    // fall back to Object which is always a valid common superclass
                    return "java/lang/Object";
                }
            }
        };
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