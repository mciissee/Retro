package fr.umlv.Retro.nestmates;

import java.util.HashSet;
import java.util.Objects;

import fr.umlv.Retro.models.FeatureDescriber;

public class NestMateHostDescriber implements FeatureDescriber {
	private final String hostName;
	private final HashSet<String>  nestMembers;

	public NestMateHostDescriber(String hostName, HashSet<String> nestMembers) {
		this.hostName = Objects.requireNonNull(hostName);
		this.nestMembers = new HashSet<>(Objects.requireNonNull(nestMembers));
	}

	@Override
	public String describe() {
		return String.format(
			"NESTMATES AT %s (%s.java) nest host %s members %s",
			hostName, hostName, hostName, nestMembers
		);
	}
	
	@Override
	public String toString() {
		return describe();
	}

}
