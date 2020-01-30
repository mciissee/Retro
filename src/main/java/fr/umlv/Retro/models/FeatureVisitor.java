package fr.umlv.Retro.models;

import org.objectweb.asm.ClassVisitor;

import fr.umlv.Retro.ClassTransformer;


public interface FeatureVisitor {
	ClassVisitor visit(ClassInfo ci, ClassVisitor cv, ClassTransformer tr);
}
