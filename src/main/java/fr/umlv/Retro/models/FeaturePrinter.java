package fr.umlv.Retro.models;

public interface FeaturePrinter {
	default void print() {
		System.out.println(this);
	}
}
