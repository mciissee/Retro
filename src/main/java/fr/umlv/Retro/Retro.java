package fr.umlv.retro;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.objectweb.asm.Opcodes;

import fr.umlv.retro.cli.CommandLine;
import fr.umlv.retro.concats.ConcatVisitor;
import fr.umlv.retro.lambdas.LambdaVisitor;
import fr.umlv.retro.models.FeatureDescriber;
import fr.umlv.retro.models.FeatureVisitor;
import fr.umlv.retro.models.Features;
import fr.umlv.retro.models.Logger;
import fr.umlv.retro.models.Options;
import fr.umlv.retro.nestmates.NestMateVisitor;
import fr.umlv.retro.utils.Contracts;


/**
 * Facade of the application.
 */
public class Retro {

	private final Options options;
	private final FileSystem fs;
	private final Logger logger;
	private boolean success = true;

	private final FeatureVisitor[] visitors = new FeatureVisitor[] {
		new NestMateVisitor(this),
		new ConcatVisitor(this),
		new LambdaVisitor(this),
    };
	
	private Retro(FileSystem fs, Options options, Logger logger) {
		this.fs = Objects.requireNonNull(fs);
		this.options = Objects.requireNonNull(options);
		this.logger = Objects.requireNonNull(logger);
	}


	/**
	 * Creates new instance and run the program.
	 * For each specified path, the program will output the transformed
	 * classes and jars in a directory 'retro-output' relative to the path.
	 * @param paths the paths where to run the program.
	 * @param options the program options.
	 * @param logger all call to sysout will be forwarded to this logger.
	 * @return true if the class files are transformed false otherwise.
	 * @throws IOException
	 * @throws URISyntaxException 
	 * @throws IllegalArgumentException
	 */
	public static boolean exec(Path[] paths, Options options, Logger logger) throws IOException, URISyntaxException {
		Contracts.requires(paths, "paths");
		Contracts.requires(options, "options");
		Contracts.requires(logger, "logger");
        var savers = new ArrayList<Runnable>();
        var success = true;
		try {
			if (options.help()) {
	    		try(var stream = ClassLoader.getSystemClassLoader().getResourceAsStream("help.txt")) {
	    			logger.info(new String(stream.readAllBytes()));
	    		}
	        	return false;
	        }
	        if (paths.length == 0) {
	        	var error = new StringBuilder();
	        	error.append("\nError: require speficication of a path pointing to a jar file, a class file or directory.\n");
	        	error.append("use -help for a list of possible options");
	        	throw new AssertionError(error.toString());
	        }
        	Retro retro;
        	for (var path : paths) {
        		var fs = FileSystem.create(path);
        		retro = new Retro(fs, options, logger);        			
        		if (!retro.run(savers)) {
        			success = false;
        		}
        	}
        } finally {
        	if (success) {
        		for (var save : savers) {
        			save.run();
        		}
        	} else if (!savers.isEmpty()) {
            	logger.error(
        			"\nRun the program with --force option or add the features using -features options",
        			"or use -help for more options"
        		);
        	}
        }
		return success;
	}

	/**
	 * Creates new instance from command line arguments and run the program
	 * from the paths specified in the arguments. <br> <br>
	 * 
	 * For each specified path, the program will output the transformed
	 * classes and jars in a directory 'retro-output' relative to the path.
	 * @param commandLine the command line arguments.
	 * @return true if the class files are transformed false otherwise.
	 * @throws IOException
	 * @throws URISyntaxException 
	 * @throws IllegalArgumentException
	 */
	public static boolean exec(CommandLine commandLine) throws IOException, URISyntaxException {
		Contracts.requires(commandLine, "commandLine");	
		var options = Options.fromCommandLine(commandLine);
		var paths = Arrays.stream(commandLine.args().get())
				.map(e -> Paths.get(e))
				.toArray(Path[]::new);
		return exec(paths, options, new Logger());
	}
	
	/**
	 * Creates new instance and run the program.
	 * For each specified path, the program will output the transformed
	 * classes and jars in a directory 'retro-output' relative to the path.
	 * @param path the path where to run the program.
	 * @param options the program options.
	 * @param logger all call to sysout will be forwarded to this logger.
	 * @return true if the class files are transformed false otherwise.
	 * @throws IOException
	 * @throws URISyntaxException 
	 * @throws IllegalArgumentException
	 */
	public static boolean exec(Path path, Options options, Logger logger) throws IOException, URISyntaxException {
		Contracts.requires(path, "path");
		Contracts.requires(options, "options");
		Contracts.requires(logger, "logger");
        return exec(new Path[] { path }, options, logger);
	}
	
	/**
	 * ASM API version
	 */
	public int api() {
		return Opcodes.ASM7;
	}

	/**
	 * The JDK version to which the byte codes should be transformed.
	 */
	public int target() {
		return options.target();
	}
	
	/**
	 * Checks whether the given feature is in the list of features to transform.
	 * @param feature the feature
	 * @return true if the feature is present of if force option is enabled false otherwise.
	 * @throws IllegalArgumentException
	 */
	public boolean hasFeature(Features feature) {
		Contracts.requires(feature, "feature");
		return options.hasFeature(feature) || options.force();
	}
	
	public FeatureVisitor[] visitors() {
		return Arrays.copyOf(visitors, visitors.length);
	}

	/**
	 * Writes the given {@code bytecode} in the class file {@code className} 
	 * on the file system relative to {@code path}. <br/> <br/>
	 * 
	 * Note: the bytecode will not be written on the disk until {@link #save()} method is called.
	 * @param path path where to write the class.
	 * @param className class name (without an extension)..
	 * @param bytecode bytecode to write.
	 * @throws IllegalArgumentException if any of the arguments is null.
	 */
	public void write(Path path, String className, byte[] bytes) {
		fs.write(path, className, bytes);
	}
	
	/**
	 * Writes the given {@code bytecode} at {@code path} on the file system. <br/> <br/>
	 * 
	 * Note: the bytecode will not be written on the disk until {@link #save()} method is called.
	 * @param path path where to write the bytecode.
	 * @param bytecode bytecode to write.
	 * @throws IllegalArgumentException if any of the arguments is null.
	 */
	public void write(Path path, byte[] bytecode) {
		fs.write(path, bytecode);
	}
	
	/**
	 * Gets the bytecode of the class {@code className} relative to the given {@code path}. <br><br>
	 * 
	 * The method will always return the last modified version of the bytecode since program the class is created.
	 * @param path path where to search the class.
	 * @param className class name
	 * @param consumer bytecode consumer.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IllegalArgumentException if any of the arguments is null.
	 */
	public void bytecode(Path path, String clazz, BiConsumer<Path, byte[]> consumer) throws FileNotFoundException, IOException {
		fs.bytecode(path, clazz, consumer);
	}

	/**
	 * Handles feature detection.
	 * @param path path where the feature is detected.
	 * @param feature the feature.
	 * @param describer feature describer.
	 * @throws IllegalArgumentException if any of the arguments is null.
	 */
	public void detectFeature(Path path, Features feature, FeatureDescriber describer) {
		Contracts.requires(feature, "feature");
		Contracts.requires(describer, "describer");
		if (options.info()) {
			logger.info(describer.describe());
		}
		if (!hasFeature(feature)) {
			logger.warn("<" + feature + "> used in " + path + " is not supported in JDK " + target());
			success = false;
		}
	}

	private boolean run(ArrayList<Runnable> savers) throws FileNotFoundException, IOException, URISyntaxException {
		fs.iterate((path, bytes) -> {
			ClassTransformer.transform(this, path, bytes);
		});
		if (success) {
			savers.add(() -> {
				try {
					fs.save();
				} catch (IOException e) {
					throw new AssertionError(e.getMessage(), e.getCause());
				}
			});
		}
		return success;
	}

}
