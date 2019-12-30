package fr.umlv.Retro.models;

import java.util.EnumSet;
import java.util.Objects;

import fr.umlv.Retro.cli.CommandLine;

/**
 * Bytecode transformation options.
 */
public class Options {

	private final EnumSet<Features> features;
	private final int target;
	private final boolean info;
	private final boolean help;
	private final boolean force;

	private Options(int target, boolean force, boolean info, boolean help, EnumSet<Features> features) {
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
	 * @throws IllegalArgumentException if commandLine is null.
	 */
	public static Options fromCommandLine(CommandLine commandLine) {
		if (commandLine == null) {
			throw new IllegalArgumentException("cli");
		}
		var force = commandLine.hasOption("force");
		var info = commandLine.hasOption("info");
		var help = commandLine.hasOption("help");
		var target = Integer.parseInt(commandLine.args("target")[0]);
		var features = EnumSet.noneOf(Features.class);
		var args = commandLine.args("features");
		if (args.length > 0) {
			args = args[0].split(",");
		}
		for (var feature : args) {
			try {
				features.add(Enum.valueOf(Features.class, feature));				
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("unknown feature " + feature);
			}
		}
		if (features.size() == 0) {
			features = Features.ALL;
		}

		return new Options(
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
