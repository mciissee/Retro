package fr.umlv.Retro.models;

import java.util.EnumSet;


/**
 * List of features supported by the API.
 */
public enum Features {

	/**
	 * Java 7 (AutoCloseable(owner), Throwable(desc), addSuppressed (name))
	 */
	TryWithResources,

	/**
	 * Java 8 (InvokeDynamic)
	 */
	Lambda,

	/**
	 * Java 9 (InvokeDynamic)
	 */
	Concat,
	
	/**
	 * Java 11 (NestHost or NestMembers attritutes)
	 */
	NestMates,

	/**
	 * Java 14 (java.lang.Record)
	 */
	Record;
	
    public static final EnumSet<Features> ALL = EnumSet.allOf(Features.class);
}
