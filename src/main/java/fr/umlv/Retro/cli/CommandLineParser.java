package fr.umlv.retro.cli;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Command line arguments parser.
 */
public class CommandLineParser {

	
	/**
	 * Parses the given arguments following the given pattern.
	 * 
	 * The pattern is a space separated list of the authorized options.
	 * Required options must be prefixed with ":". 
	 * Parameterized options must be prefixed with "*".
	 *
	 * Example:
	 * 
	 * :opt1 opt2 *opt3 :*opt4 means that an argument is required for opt1 and opt4
	 * and opt3 and opt4 are must be specified.
	 * opt2 is optional. 
	 *
	 * @param args the arguments.
	 * @param pattern the pattern.
	 * @return new CommandLine object.
	 * @throws IllegalArgumentException if args is null.
	 * @throws AssertionError if a required option|argument is missing or an unknown option is found.
	 */
	public static CommandLine parse(String args[], String pattern) throws AssertionError, IllegalArgumentException {
		if (args == null) {
			throw new IllegalArgumentException("args");
		}
		var patterns = Arrays.stream(pattern.split(" ")).filter(e -> !e.isEmpty()).map(String::trim).toArray(String[]::new);
		var i = 0;
		var n = args.length;
		var options = new ArrayList<CommandLineOption>();
		var defaults = new ArrayList<String>();
		while (i < n) {
			if (args[i].startsWith("-")) {
				var opt = args[i].replace("-", "");
				if (isTakeAnArgument(opt, patterns)) {
					if (i + 1 >= n || args[i + 1].startsWith("-")) {
						throw new AssertionError("An argument is required for option " + opt);
					}
					options.add(new CommandLineOption(opt, new String[] {args[i + 1]}));
					i++;
				} else {
					if (!existPattern(opt, patterns)) {
						throw new AssertionError("unkown option " + opt);
					}
					options.add(new CommandLineOption(opt, new String[0]));
				}
			} else {
				defaults.add(args[i]);
			}
			i++;
		}
		if (!defaults.isEmpty()) {
			options.add(new CommandLineOption("^", defaults.toArray(new String[defaults.size()])));			
		}
		checkRequirements(patterns, options);
		return new CommandLine(options);
	}

	private static void checkRequirements(String[] patterns, ArrayList<CommandLineOption> options)
			throws AssertionError {
		for (var p : patterns) {
			if (p.contains("*")) {
				var name = p.replace("*", "").replace(":", "");
				var found = false;
				for (var o : options) {
					if (o.name().equals(name)) {
						found = true;
						break;
					}
				}
				if (!found) {
					throw new AssertionError("option '" + name + "' is required");
				}
			}
		}
	}

	private static boolean isTakeAnArgument(String opt, String[] patterns) {
		for (var p : patterns) {
			if (p.endsWith(opt)) {
				return p.contains(":");
			}
		}
		return false;
	}
	
	private static boolean existPattern(String opt, String[] patterns) {
		for (var p : patterns) {
			if (p.endsWith(opt)) {
				return true;
			}
		}
		return false;
	}
}
