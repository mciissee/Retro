package fr.umlv.retro.models;

import org.objectweb.asm.ClassVisitor;

import fr.umlv.retro.ClassTransformer;


public interface FeatureVisitor {
	ClassVisitor visit(ClassInfo ci, ClassVisitor cv, ClassTransformer tr);
}
