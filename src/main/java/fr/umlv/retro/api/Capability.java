package fr.umlv.retro.api;

import java.util.Map;

import fr.umlv.retro.models.Features;

public class Capability {
	public final int minSupportedJDK = 5;
	public final int maxSupportedJDK = 13;
	public final Map<Features, Integer> features = Features.supported();
}
