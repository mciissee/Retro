package fr.umlv.retro.records;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import fr.umlv.retro.Retro;
import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.MethodInfo;

/**
 * Detects lambda method calls and rewrite them if needed (target < JDK 8).
 */
class RecordDetector extends LocalVariablesSorter implements Opcodes {
	
	private final Retro app;
	private final ClassInfo ci;
	private final MethodInfo mi;
	private int lineNumber;
	
	public RecordDetector(Retro app, ClassInfo ci, MethodInfo mi) {
		super(app.api(), mi.access(), mi.descriptor(), mi.visitor());
		this.ci = ci;
		this.mi = mi;
		this.app = app;
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		this.lineNumber = line;
		super.visitLineNumber(line, start);
	}
}
