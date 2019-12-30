package fr.umlv.Retro.cli;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a Command line option with it's arguments. (Immutable)
 */
public class CommandLineOption {

	private final String name;
	private final String[] args;
	
	/**
	 * Creates new instance of the class.
	 * @param name the name of the option.
	 * @param args the arguments of the options.
	 */
	CommandLineOption(String name, String[] args) {
		this.name = Objects.requireNonNull(name);
		this.args = Objects.requireNonNull(Arrays.copyOf(args, args.length));
	}

	/**
	 * Creates new instance of the class.
	 * @param name the name of the option.
	 */
	public CommandLineOption(String name) {
		this(name, new String[0]);
	}


	/**
	 * Gets the name of the option.
	 * @return the name of the option
	 */
	public String name() {
		return name;
	}

	/**
	 * Gets a copy of the arguments associated to the option.
	 * @return the arguments.
	 */
	public String[] args() {
		return Arrays.copyOf(args, args.length);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CommandLineOption)) {
			return false;
		}
		var o = (CommandLineOption) obj;
		return name.equals(o.name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "(" + name + " " + Arrays.toString(args) + ")";
	}
}
