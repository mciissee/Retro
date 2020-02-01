package fr.umlv.retro.models;

import java.util.ArrayList;
import java.util.Objects;
import java.util.StringJoiner;

public class Logger {

	// make it public to allow serialization
	public static class Log {

		public final LogTypes type;
		public final String message;

		Log(LogTypes type, String message) {
			this.type = type;
			this.message = Objects.requireNonNull(message);
		}
	}

	private final ArrayList<Log> logs = new ArrayList<>();

	public ArrayList<Log> get() {
		return new ArrayList<>(logs);
	}

	public void info(String... messages) {
		Objects.requireNonNull(messages);
		var message = join(messages);
		logs.add(new Log(LogTypes.Info, message));
		System.out.println(message);
	}

	public void warn(String... messages) {
		Objects.requireNonNull(messages);
		var message = join(messages);
		logs.add(new Log(LogTypes.Warn, message));
		System.out.println(message);
	}

	public void error(String... messages) {
		Objects.requireNonNull(messages);
		var message = join(messages);
		logs.add(new Log(LogTypes.Error, message));
		System.out.println(message);
	}
	
	private String join(String... messages) {
		var joiner = new StringJoiner("\n");
		for (var e : messages) {
			joiner.add(e);
		}
		return joiner.toString();
	}
}
