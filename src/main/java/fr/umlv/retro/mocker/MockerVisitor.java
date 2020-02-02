package fr.umlv.retro.mocker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import fr.umlv.retro.ClassTransformer;
import fr.umlv.retro.Retro;
import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.FeatureVisitor;
import fr.umlv.retro.models.MethodInfo;

/**
 * 
 * Mock unsupported JDK apis if not supported in target version.
 * 
 * Example. java.util.function.* is not supported for a target JDK < 8
 *
 */
public class MockerVisitor extends ClassVisitor implements FeatureVisitor {
	
	private ClassTransformer tr;
	private ClassInfo ci;
	private final Retro app;
	private final HashSet<String> unmocked = new HashSet<>();
	private final HashSet<Path> mocked = new HashSet<>();

	private final Map<String, Integer> releases = Map.of(
		"java.util.Objects", 7,
		"java.util.function", 8,
		"java.util.stream", 8
	);
		
 	public MockerVisitor(Retro app) {
		super(Objects.requireNonNull(app).api());
		this.app = app;
	}

	public ClassVisitor visit(ClassInfo ci, ClassVisitor cv,  ClassTransformer tr) {
		this.cv = Objects.requireNonNull(cv);
		this.ci = Objects.requireNonNull(ci);
		this.tr = Objects.requireNonNull(tr);
		return this;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		var path = Paths.get(app.root().toString(), "mock", name + ".class");
		if (mocked.contains(path)) {
			name = "mock/" + name;
		}
		name = mockInternal(name);
		superName = mockInternal(superName);
		for (var i = 0; i < interfaces.length; i++) {
			interfaces[i] = mockInternal(interfaces[i]);
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		super.visitInnerClass(
			mockInternal(name),
			mockInternal(outerName),
			mockInternal(innerName),
			access
		);
	}
		
	@Override
	public void visitOuterClass(String owner, String name, String descriptor) {
		super.visitOuterClass(
			mockInternal(owner),
			mockInternal(name),
			mock(descriptor)
		);
	}
	
	@Override
	public void visitNestMember(String nestMember) {
		super.visitNestMember(mockInternal(nestMember));
	}
	
	@Override
	public void visitNestHost(String nestHost) {
		super.visitNestHost(mockInternal(nestHost));
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		descriptor = mock(descriptor);
		return super.visitField(access, name, descriptor, signature, value);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		descriptor = mock(descriptor);
		var mv =  super.visitMethod(access, name, descriptor, signature, exceptions);
		var mi = new MethodInfo(access, name, descriptor, exceptions, mv);
		return new MockerMethodDetector(app, mi, this);
	}
	
	@Override
	public void visitEnd() {
		for (var className : new HashSet<String>(unmocked)) { // copy to avoid concurrent modification
			var path = app.root().resolve("mock/" + className.replace(".", "/") + ".class");
			if (!mocked.contains(path)) {
				mocked.add(path);
				try {
					var cr = new ClassReader(className);
					var cw = new ClassWriter(0);
					cr.accept(cw, 0);	
					ClassTransformer.transform(app, path, cw.toByteArray(), tr);
					app.logInfo(
						String.format(
							"%s.class: info: as -force option is specified, %s is replaced by mock/%s since the class does not exists on %d",
							ci.className(), className, className, app.target()
						)
					);
				} catch (IOException e) {
					app.logError(
						String.format(
							"%s.class: error: an error occured while trying to mock %s: %s",
							ci.className(), className, e.getMessage()
						)
					);
				}
			}
		}
		super.visitEnd();
	}

	public Type mock(Type type) {
		var sort = type.getSort();
		if (sort == Type.METHOD) {
			return Type.getMethodType(mock(type.getDescriptor()));
		}
		return Type.getType(mockInternal(type.getDescriptor()));
	}
		
	public String mock(String descriptor) {
		if (descriptor == null) {
			return descriptor;
		}

		if (descriptor.length() == 1) { // primitive
			return descriptor;
		}

		if (!descriptor.startsWith("(")) { // not is method
			return mockInternal(descriptor);
		}

		var args = Type.getArgumentTypes(descriptor);
		for (var i = 0; i < args.length; i++) {
			var sort = args[i].getSort();
			if (sort != Type.ARRAY && sort != Type.OBJECT) {
				continue;
			}
			args[i] = Type.getObjectType(mockInternal(args[i].getInternalName()));
		}

		var ret = Type.getReturnType(descriptor);
		var sort = ret.getSort();
		if (sort == Type.ARRAY || sort == Type.OBJECT) {
			ret = Type.getObjectType(mockInternal(ret.getInternalName()));
		}
		descriptor = Type.getMethodDescriptor(ret, args);
		return descriptor;
	}

	private String mockInternal(String internalName) {
		if (internalName == null) {
			return internalName;
		}

		if (internalName.length() == 1) { // primitive
			return internalName;
		}

		var isObjArr = internalName.startsWith("[L");
		var name = internalName.replace("/", ".");
		if (isObjArr) {
			name = name.replace("[L", "");
		}
		for (var entry : releases.entrySet()) {
			var k = entry.getKey();
			if (name.startsWith(k)) {
				var release = entry.getValue();
				if (app.target() < release) {
					if (!app.force()) {
						app.logWarning(
							String.format(
								"%s.class: warn: %s class does not exists in %d (use -target %d or higher or -force to mock the class)",
								ci.className(), name, app.target(), releases.get(k)
							)
						);
						return internalName;
					}
					unmocked.add(name);
					var mock = "mock/" + internalName;
					if (isObjArr) {
						mock = "[Lmock/" + name.replace(".", "/");
					}
					return mock;
				}
			}
		}
		return internalName;
	}

}
