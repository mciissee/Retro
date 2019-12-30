package fr.umlv.Retro.models;

import java.util.EnumSet;
import java.util.Objects;

import fr.umlv.Retro.cli.CommandLine;

/**
 * Bytecode transformation options.
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
		this.features = Objects.requireNonNull(features);
	}
	
	/**
	 * Creates new TransformOptions object from command line arguments.
	 * @param cli The command line.
	 * @return new Instance of TransformOptions
	 * @throws IllegalArgumentException if cl is null.
	 */
	public static TransformOptions fromCommandLine(CommandLine cl) {
		if (cl == null) {
			throw new IllegalArgumentException("cli");
		}
		var force = cl.hasOption("force");
		var info = cl.hasOption("info");
		var help = cl.hasOption("help");
		var target = Integer.parseInt(cl.args("target")[0]);
		var features = EnumSet.noneOf(Features.class);
		for (var feature : cl.args("features")[0].split(",")) {
			try {
				features.add(Enum.valueOf(Features.class, feature));				
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("unknown feature " + feature);
			}
		}
		return new TransformOptions(
			target,
			force,
			info,
			help,
			features
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
