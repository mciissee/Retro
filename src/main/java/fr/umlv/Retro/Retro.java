package fr.umlv.Retro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import fr.umlv.Retro.cli.CommandLine;
import fr.umlv.Retro.models.FeaturePrinter;
import fr.umlv.Retro.models.Features;
import fr.umlv.Retro.models.TransformOptions;


public class Retro {

	private final HashMap<String, byte[]> entries = new HashMap<>();

	private final Path path;
	private final boolean isJar;
	private final boolean isClass;
	private final boolean isDirectory;
	private final TransformOptions options;

	private Retro(Path path, TransformOptions options, boolean isDirectory, boolean isClass, boolean isJar) {
		this.path = Objects.requireNonNull(path);
		this.options = Objects.requireNonNull(options);
		this.isDirectory = isDirectory;
		this.isClass = isClass;
		this.isJar = isJar;
	}

	public static Retro fromCommandLine(CommandLine commandLine) {
		if (commandLine == null) {
			throw new IllegalArgumentException("commandLine");
		}

        var args = commandLine.args();
        if (args.length == 0) {
        	var message = new StringBuilder();
        	message.append("\nError: require speficication of a path pointing to a jar file, a class file or directory.\n");
        	message.append("use --help for a list of possible options");
        	throw new AssertionError(message);
        }

        var path = Paths.get(args[0]);
		var options = TransformOptions.fromCommandLine(commandLine);

        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
        	throw new AssertionError("use --help for a list of possible options");
        }
        
        var name = path.getFileName().toString();
        var isDirectory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
        var isClass = !isDirectory && name.endsWith(".class");
        var isJar = !isDirectory && name.endsWith(".jar");
        
        if (!isDirectory && (!isJar && !isClass)) {
        	throw new IllegalArgumentException("path must point to a directory or a jar file or a class file.");
        }
 
        return new Retro(path, options, isDirectory, isClass, isJar);
	}
	
	public void run() throws FileNotFoundException, IOException {
		iterate((path, bytes) -> {
	        var cr = new ClassReader(bytes);
	        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
	        cr.accept(new ClassTransformer(cw, this, path), ClassReader.EXPAND_FRAMES);
	        write(path, cw.toByteArray());
		});
		
		//// System.out.println(entries.size());
	}
	
	public void write(String path, byte[] bytes) {
		entries.put(path, bytes);
	}
	
	public void write(String path, String clazz, byte[] bytes) {
		System.out.println(this.path + " : " + path + " : " + clazz); ;
		entries.put(path, bytes);
	}
	
	public void detectFeature(Features feature, FeaturePrinter printer) {
		if (options.info()) {
			printer.print();
		}
	}

	public int target() {
		return options.target();
	}
	
	public boolean hasFeature(Features feature) {
		return options.hasFeature(feature) || options.force();
	}

	private void open(Path path, BiConsumer<String, byte[]> consumer) throws FileNotFoundException, IOException {
		try(var in = new FileInputStream(path.toFile())) {
			consumer.accept(path.toString(), in.readAllBytes());
		}
	}

	private void openJar(Path path, BiConsumer<String, byte[]> consumer) throws IOException {
		try(var zip = new ZipFile(path.toString())) {
			var entries = zip.stream()
					.filter(e -> !e.isDirectory() && e.getName().endsWith(".class"))
					.collect(Collectors.toList());
			for (var entry : entries) {
				try(var in = zip.getInputStream(entry)) {
					consumer.accept(entry.toString(), in.readAllBytes());
				}
			}
		}
	}

	private void iterate(BiConsumer<String, byte[]> consumer) throws FileNotFoundException, IOException {
		if (this.isClass) {
			open(path, consumer);
		} else if (isDirectory) {
			var entries = Files.list(path).filter(e -> {
				return  e.toString().endsWith(".class");
			}).collect(Collectors.toList());
			for (var entry : entries) {
				open(entry, consumer);
			}
		} else if (isJar) {
			openJar(path, consumer);
		}
	}
	
}
