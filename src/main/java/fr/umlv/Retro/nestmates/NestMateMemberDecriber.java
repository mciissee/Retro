package fr.umlv.Retro.nestmates;

import java.util.Objects;

import fr.umlv.Retro.models.FeatureDescriber;

public class NestMateMemberDecriber implements FeatureDescriber {
	private final String className;
	private final String nestMember;

	public NestMateMemberDecriber(String className, String nestMember) {
		this.className = Objects.requireNonNull(className);
		this.nestMember = Objects.requireNonNull(nestMember);
	}

	@Override
	public String describe() {
		return String.format(
			"NESTMATES AT %s (%s.java) nestmate of %s",
			nestMember, className, className
		);
	}
	
	@Override
	public String toString() {
		return describe();
	}

}
