package fr.umlv.Retro.models;

/**
 * Informations about a feature detection
 */
public interface FeatureDescriber {
	/**
	 * Describes the detection.
	 * @return A String describing the detected feature.
	 */
	String describe();
}
