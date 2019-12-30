package fr.umlv.Retro;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import fr.umlv.Retro.cli.CommandLine;
import fr.umlv.Retro.models.FeatureDescriber;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.Options;


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
	 * @return new instance of Retro class.
	 * @throws IOException
	 */
	public static Retro fromCommandLine(CommandLine commandLine) throws IOException {
		if (commandLine == null) {
			throw new IllegalArgumentException("commandLine");
		}

    	var errormessage = new StringBuilder();
    	errormessage.append("\nError: require speficication of a path pointing to a jar file, a class file or directory.\n");
    	errormessage.append("use --help for a list of possible options");
    	
        var args = commandLine.args();
        if (args.length == 0) {
        	throw new AssertionError(errormessage.toString());
        }
        
        try {
        	var fs = FileSystem.create(Paths.get(args[0]));
        	var options = Options.fromCommandLine(commandLine);
        	return new Retro(fs, options);        	
        } catch (IOException e) {
        	throw new IOException(errormessage.toString(), e.getCause());
        }
	}
	
	/**
	 * Runs the program.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void run() throws FileNotFoundException, IOException {
		fs.clear();
		unsupported.clear();

		fs.iterate((path, bytes) -> {
	        var cr = new ClassReader(bytes);
	        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
	        cr.accept(new ClassTransformer(cw, this, path), ClassReader.EXPAND_FRAMES);
	        fs.write(path, cw.toByteArray());
		});
		
		if (unsupported.size() > 0) {
			System.out.println("\n\nUnsupported features: Some bytecodes contains the following unsupported features : " + unsupported);
			System.out.println("Run the program with --force or -features options");
		} else {
			fs.save();			
		}
	}
		
	/**
	 * Adds the given class file to the file system.
	 * @param path path where to write the class
	 * @param clazz the class name
	 * @param bytes the class bytecode
	 */
	public void write(Path path, String clazz, byte[] bytes) {
		fs.write(path, clazz, bytes);
	}
	
	/**
	 * Handles feature detection.
	 * @param feature the feature
	 * @param describer feature describer.
	 */
	public void onFeatureDetected(Features feature, FeatureDescriber describer) {
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
	 */
	public boolean hasFeature(Features feature) {
		return options.hasFeature(feature) || options.force();
	}
	
	/**
	 * The JDK version to which the byte codes should be transformed.
	 */
	public int target() {
		return options.target();
	}
	
}
