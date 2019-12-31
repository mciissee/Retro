package fr.umlv.Retro;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.BiConsumer;

import fr.umlv.Retro.cli.CommandLine;
import fr.umlv.Retro.models.FeatureDescriber;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.Options;
import fr.umlv.Retro.utils.Contracts;


/**
 * Core class of the program.
 */
public class Retro {

	private final Options options;
	private final FileSystem fs;
	private final HashSet<Features> unsupported = new HashSet<>();

	private Retro(FileSystem fs, Options options) {
		this.fs = Objects.requireNonNull(fs);
		this.options = Objects.requireNonNull(options);
	}

	/**
	 * Creates new instance from command line arguments.
	 * @param commandLine the command line arguments.
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	public static void fromCommandLine(CommandLine commandLine) throws IOException, URISyntaxException {
		Contracts.requires(commandLine, "commandLine");	   
        if (commandLine.hasOption("help") || commandLine.isEmpty()) {
    		try(var stream = ClassLoader.getSystemClassLoader().getResourceAsStream("help.txt")) {
    			System.out.println(new String(stream.readAllBytes()));
    		}
        	return;
        }
       
    	var error = new StringBuilder();
    	error.append("\nError: require speficication of a path pointing to a jar file, a class file or directory.\n");
    	error.append("use -help for a list of possible options");
    	
        var files = commandLine.args();
        if (files.isEmpty()) {
        	throw new AssertionError(error.toString());
        }
     
        try {
        	for (var path : files.get()) {
        		if (!path.isBlank() && !path.isEmpty()) {
        			var fs = FileSystem.create(Paths.get(path));
        			var options = Options.fromCommandLine(commandLine);
        			var retro = new Retro(fs, options); 
        			if (!retro.run()) {
        				return;
        			}        			
        		}
        	}
        } catch (IOException e) {
        	throw new IOException(error.toString(), e.getCause());
        }
	}

	
	/**
	 * Runs the program.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @return false if a problem is not occurred true otherwise.
	 */
	private boolean run() throws FileNotFoundException, IOException, URISyntaxException {
		fs.clear();
		unsupported.clear();
		fs.iterate((path, bytes) -> {
			ClassTransformer.transform(this, path, bytes);
		});
		
		if (unsupported.size() > 0) {
			System.out.println("\nUnsupported features: the bytecodes contains the following unsupported features : " + unsupported);
			System.out.println("Run the program with --force or -features options\n");
			return false;
		} else {
			fs.save();			
		}
		return true;
	}
		
	/**
	 * Adds the given class file to the file system.
	 * @param path path where to write the class
	 * @param clazz the class name
	 * @param bytes the class bytecode
	 * @throws IllegalArgumentException
	 */
	public void writeClass(Path path, String clazz, byte[] bytes) {
		fs.writeClass(path, clazz, bytes);
	}
	
	/**
	 * Adds the given class file to the file system.
	 * @param path path where to write the class
	 * @param bytes the class bytecode
	 * @throws IllegalArgumentException
	 */
	public void writeClass(Path path, byte[] bytes) {
		fs.writeClass(path, bytes);
	}
	
	/**
	 * Gets the bytecode of a class file from the disk relative to the given path.
	 * @param path path where to search the class.
	 * @param clazz class name
	 * @param consumer consumer.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void openClassRelativeTo(Path path, String clazz, BiConsumer<Path, byte[]> consumer) throws FileNotFoundException, IOException {
		fs.openClassRelativeTo(path, clazz, consumer);
	}

	/**
	 * Handles feature detection.
	 * @param feature the feature
	 * @param describer feature describer.
	 * @throws IllegalArgumentException
	 */
	public void onFeatureDetected(Features feature, FeatureDescriber describer) {
		Contracts.requires(feature, "feature");
		Contracts.requires(describer, "describer");

		if (options.info()) {
			System.out.println(describer.describe());
		}

		if (!hasFeature(feature)) {
			unsupported.add(feature);

		}
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
	
	/**
	 * The JDK version to which the byte codes should be transformed.
	 */
	public int target() {
		return options.target();
	}
	

}
