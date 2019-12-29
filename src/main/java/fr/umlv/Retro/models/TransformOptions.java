package fr.umlv.Retro.models;

import java.util.EnumSet;

/**
 * 
 * Byte code transformation options.
 */
public class TransformOptions {

	private final EnumSet<Features> features;
	private final int target;
	private final boolean info;
	private final boolean help;
	private final boolean force;

	private TransformOptions(int target, boolean force, boolean info, boolean help, EnumSet<Features> features) {
		this.target = target;
		this.force = force;
		this.info = info;
		this.help = help;
		this.features = features;
	}
	
	/**
	 * Creates options from command line arguments.
	 * @param args The arguments of the command line.
	 * @return new Instance of TransformOptions
	 */
	public static TransformOptions fromCommandLine(String[] args) {
		// TODO parse args 
		return new TransformOptions(
			7,
			true,
			true,
			false,
			Features.ALL
		);
	}
	
	/**
	 * The JDK version to which the byte codes should be transformed.
	 */
	public int target() {
		return target;
	}

	/**
	 * Gets a value indicating whether a message should be
	 * displayed when a feature is detected.
	 */
	public boolean info() {
		return info;
	}

	/**
	 * Gets a value indicating whether help message should be displayed. 
	 */
	public boolean help() {
		return help;
	}
	
	/**
	 * Is the program launched with force option.
	 */
	public boolean force() {
		return force;
	}
	
	/**
	 * Is the given feature present in the list of features to transform.
	 * @param the feature
	 */
	public boolean hasFeature(Features feature) {
		return features.contains(feature);
	}

}
