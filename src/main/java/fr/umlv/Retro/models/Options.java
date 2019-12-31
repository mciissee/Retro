package fr.umlv.Retro.models;

import java.util.EnumSet;
import java.util.Objects;

import fr.umlv.Retro.cli.CommandLine;

/**
 * Bytecode transformation options (Immutable).
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
		var op = commandLine.args("target");
		var target = 0;
		if (!op.isEmpty()) {
			target = Integer.parseInt(op.get()[0]);	
			if (target < 5) {
				throw new AssertionError("Error: option target must be >= 5");
			}
		} else if (!commandLine.hasOption("help")) {
			throw new AssertionError("Error: option target is required");
		}
		op = commandLine.args("features");
		var features = EnumSet.noneOf(Features.class);
		for (var feature : op.isEmpty() ? new String[0] : op.get()[0].split(",")) {
			try {
				features.add(Enum.valueOf(Features.class, feature));				
			} catch (IllegalArgumentException e) {
				throw new AssertionError("unknown feature " + feature);
			}
		}
		if (features.size() == 0) {
			features = Features.ALL;
		}
		return new Options(
			target,
			commandLine.hasOption("force"),
			commandLine.hasOption("info"),
			commandLine.hasOption("help"),
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
