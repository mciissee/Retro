package fr.umlv.retro.models;

import java.util.EnumSet;
import java.util.Map;


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
    
    public static Map<Features, Integer> supported() {
    	return Map.of(
    			Features.TryWithResources, 7,
    			Features.Lambda, 8,
    			Features.Concat, 9,
    			Features.NestMates, 11,
    			Features.Record, 14
    	);
    }
}
