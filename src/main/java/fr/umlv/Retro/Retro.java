package fr.umlv.Retro;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;

import fr.umlv.Retro.cli.CommandLine;
import fr.umlv.Retro.models.FeatureDescriber;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.Options;
import fr.umlv.Retro.utils.Contracts;


/**
 * Facade of the application.
 */
public class Retro {

	private final Options options;
	private final FileSystem fs;
	private final HashMap<Features, ArrayList<String>> unsupported = new HashMap<>();

	private Retro(FileSystem fs, Options options) {
		this.fs = Objects.requireNonNull(fs);
		this.options = Objects.requireNonNull(options);
	}

	/**
	 * Creates new instance from command line arguments.
	 * @param commandLine the command line arguments.
	 * @throws IOException
	 * @throws URISyntaxException 
	 * @throws IllegalArgumentException
	 */
	public static void create(CommandLine commandLine) throws IOException, URISyntaxException {
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
			System.out.println(describer.describe());
		}
		if (!hasFeature(feature)) {
			var files = unsupported.get(feature);
			if (files == null) {
				files = new ArrayList<>();
				unsupported.put(feature, files);
			}
			files.add(path.toString());
		}
	}

	private boolean run() throws FileNotFoundException, IOException, URISyntaxException {
		fs.clear();
		unsupported.clear();
		fs.iterate((path, bytes) -> {
			ClassTransformer.transform(this, path, bytes);
		});
		if (unsupported.size() > 0) {
			System.out.println("\nCancelled because of the following reasons :\n");
			System.out.println("warning: unsupported features on JDK: " + target());
			unsupported.forEach((k, v) -> {
				v.forEach(e -> System.out.println("<" + k + "> on " + e));
			});
			System.out.print("\nRun the program with --force option or add the features using -features options ");
			System.out.println("or use -help for more options");
			return false;
		} else {
			fs.save();			
		}
		return true;
	}
}
