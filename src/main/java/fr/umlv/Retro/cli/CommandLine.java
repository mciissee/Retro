package fr.umlv.Retro.cli;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents the list of option/arguments of the command line interface. (Immutable)
 */
public class CommandLine {

	private final ArrayList<CommandLineOption> options;;
	
	CommandLine(ArrayList<CommandLineOption> options) {
		this.options = new ArrayList<>(Objects.requireNonNull(options));
	}
		

	/**
	 * Gets a value indicating whether any option is specified.
	 * @return true if any option is specified false otherwise.
	 */
	public boolean isEmpty() {
		return options.isEmpty();
	}

	/**
	 * Checks whether an option has been set.
	 * @param opt the option.
	 * @return true if set false if not.
	 * @throws IllegalArgumentException if opt is null.
	 */
	public boolean hasOption(String opt) throws IllegalArgumentException {
		if (opt == null) {
			throw new IllegalArgumentException("opt");
		}
		return options.contains(new CommandLineOption(opt));
	}
	
	/**
	 * Gets the arguments associated with the given option.
	 * @param opt the option.
	 * @return a copy of the arguments.
	 * @throws IllegalArgumentException if opt is null.
	 */
	public Optional<String[]> args(String opt) throws AssertionError, IllegalArgumentException  {
		var option = new CommandLineOption(opt);
		for (var e : options) {
			if (e.equals(option)) {
				return Optional.of(e.args());
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Gets the arguments not associated with a specific option.
	 * @return a copy of the arguments.
	 * @throws IllegalArgumentException if opt is null.
	 */
	public Optional<String[]> args() {
		return args("^");
	}
	
	@Override
	public String toString() {
		return options.stream().map(String::valueOf).collect(Collectors.joining());
	}

}
