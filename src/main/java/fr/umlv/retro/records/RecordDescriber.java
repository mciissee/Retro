package fr.umlv.retro.records;

import java.util.Objects;

import fr.umlv.retro.models.ClassInfo;
import fr.umlv.retro.models.FeatureDescriber;

class RecordDescriber implements FeatureDescriber {
	private final ClassInfo ci;

	public RecordDescriber(ClassInfo ci) {
		this.ci = Objects.requireNonNull(ci);
	}
	
	@Override
	public String describe() {
		return String.format(
			"RECORD AT %s (%s.java)",
			ci.className(), ci.className()
		);
	}

	@Override
	public String toString() {
		return this.describe();
	}

}
