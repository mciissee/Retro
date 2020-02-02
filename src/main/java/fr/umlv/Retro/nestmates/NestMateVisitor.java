package fr.umlv.retro.nestmates;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.Features;
import fr.umlv.retro.models.FeatureVisitor;
import fr.umlv.retro.models.MethodInfo;
import fr.umlv.retro.ClassTransformer;
import fr.umlv.retro.Retro;

public class NestMateVisitor extends ClassVisitor implements FeatureVisitor {

	private final HashMap<String, HashSet<String>> nestMembers = new HashMap<>();
	private final HashMap<String, List<NestMateRewriter>> rewriters = new HashMap<>();
	private final Retro app;

	private ClassInfo ci;
	private ClassTransformer tr;
	
	public NestMateVisitor(Retro app) {
		super(Objects.requireNonNull(app).api());
		this.app = app;
	}
	
	@Override
	public ClassVisitor visit(ClassInfo ci, ClassVisitor cv, ClassTransformer tr) {
		this.ci = Objects.requireNonNull(ci);
		this.cv = Objects.requireNonNull(cv);
		this.tr  = Objects.requireNonNull(tr);
		return this;
	}

	@Override
	public void visitNestMember(String nestMember) {
		addNestMember(ci.className(), nestMember);
		app.detectFeature(ci.path(), Features.NestMates, new NestMateMemberDecriber(ci.className(), nestMember));
		if (!canRewrite()) {
			super.visitNestMember(nestMember);			
		}
	}

	@Override
	public void visitNestHost(String nestHost) {
		this.ci = new ClassInfo(ci.version(), ci.path(), ci.fileName(), ci.className(), nestHost, ci.visitor());
		if (!canRewrite()) {
			super.visitNestHost(nestHost);
		}
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (canRewrite() && findNestMembers(ci.className()).contains(name)) {
			try {
				var tokens = name.split("/");
				app.bytecode(ci.path(), tokens[tokens.length - 1], (path, bytes) -> {
					ClassTransformer.transform(app, path, bytes, tr);
				});
			} catch (FileNotFoundException e) {
				throw new AssertionError(
					"FileNotFound: missing inner class file '" + name + ".class'",
					e.getCause()
				);
			} catch (IOException e) {
				throw new AssertionError(
					"IOException: cannot open inner class file '" + name + ".class'",
					e.getCause()
				);
			}		
		}
		super.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (ci.nestHost() != null) {
			var members = nestMembers.get(ci.nestHost());
			if (members == null) {
				members = new HashSet<String>();
				nestMembers.put(ci.nestHost(), members);
			}
			members.add(ci.className());
		}
		var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new NestMateDetector(
			app,
			ci,
			new MethodInfo(access, name, descriptor, exceptions, mv),
			this
		);
	}
	
	@Override
	public void visitEnd() {
		if (canRewrite()) {
			var rewriters = this.rewriters.get(ci.className());
			if (rewriters != null) {
				rewriters.forEach(e -> e.rewrite(ci.visitor()));
				this.rewriters.remove(ci.className());
			}
			var members = nestMembers.get(ci.className());
			if (members != null && !members.isEmpty()) {
				app.detectFeature(ci.path(), Features.NestMates, new NestMateHostDescriber(ci.className(), members));
				this.nestMembers.remove(ci.className());
			}
		}
		super.visitEnd();
	}
	
	
	private boolean canRewrite() {
		return app.target() < 11 && app.hasFeature(Features.NestMates);
	}

	private HashSet<String> findNestMembers(String path) {
		var members = nestMembers.get(path);
		if (members == null) {
			members = new HashSet<>();
			nestMembers.put(path, members);
		}
		return members;
	}

	private void addNestMember(String className, String nestMember) {
		findNestMembers(className).add(nestMember);
	}
	
	boolean detectNestMate(String className, String fieldName, String descriptor, Consumer<NestMateRewriter> rw) {
		NestMateRewriter rewriter = null;
		var rewriters = this.rewriters.get(className);
		if (rewriters == null) {
			rewriters = new ArrayList<>();
			this.rewriters.put(className, rewriters);
		}
		for (var e : rewriters) {
			if (e.equals(new NestMateRewriter(e, className, fieldName, descriptor))) {
				rewriter = e;
				break;
			}
		}
		if (rewriter == null) {
			rewriter = new NestMateRewriter(rewriters.size() + 1, className, fieldName, descriptor);
			rewriters.add(Objects.requireNonNull(rewriter));			
		}
		if (canRewrite()) {
			rw.accept(rewriter);
			return true;
		}
		return false;
	}

}
