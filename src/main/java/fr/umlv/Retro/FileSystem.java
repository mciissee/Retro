package fr.umlv.Retro;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import fr.umlv.Retro.utils.Contracts;

/**
 * Representation of a virtual file system.
 */
public final class FileSystem {
	
	private final HashMap<String, byte[]> entries = new HashMap<>();
	private final Path root;
	private final boolean isJar;
	private final boolean isClass;
	private final boolean isDirectory;
	
	private FileSystem(Path root, boolean isDirectory, boolean isClass, boolean isJar) {
		this.root = Objects.requireNonNull(root);
		this.isDirectory = isDirectory;
		this.isClass = isClass;
		this.isJar = isJar;
	}

	/**
	 * Creates new FileSystem.
	 * @param root root path of the file system.
	 * @return new FileSystem object.
	 * @throws IOException
	 * @throws IllegalArgumentException if root is not a valid path.
	 */
	public static FileSystem create(Path root) throws IOException {
		if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
        	throw new IOException("path must point to a directory or a jar file or a class file.");
        }
	    var name = root.getFileName().toString();
        var isDirectory = Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS);
        var isClass = !isDirectory && name.endsWith(".class");
        var isJar = !isDirectory && name.endsWith(".jar");
        
        if (!isDirectory && (!isJar && !isClass)) {
        	throw new IllegalArgumentException("path must point to a directory or a jar file or a class file.");
        }
        return new FileSystem(root, isDirectory, isClass, isJar);
	}

	/**
	 * Iterates over the files and call the given consumer.
	 * @param consumer function to call for each file.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public void iterate(BiConsumer<Path, byte[]> consumer) throws FileNotFoundException, IOException {
		Contracts.requires(consumer, "consumer");

		// parse nest members only once.
		BiConsumer<Path, byte[]> action = (path, bytes) -> {
			if (!entries.containsKey(path.toString())) {
				consumer.accept(path, bytes);				
			}
		};

		if (this.isClass) {
			openClass(root, action);
		} else if (isDirectory) {
			var entries = Files.list(root).filter(e -> {
				return  e.toString().endsWith(".class");
			}).collect(Collectors.toList());
			for (var entry : entries) {
				openClass(entry, action);
			}
		} else if (isJar) {
			openJar(root, e -> !e.isDirectory() && e.getName().endsWith(".class"), action);
		}
	}
	
	/*
	 * Adds the given class file to the file system.
	 * @param path path to the class file.
	 * @param bytes the class bytecode
	 * @throws IllegalArgumentException
	 */
	public void writeClass(Path path, byte[] bytes) {
		Contracts.requires(path, "path");
		Contracts.requires(bytes, "bytes");
	
		entries.put(path.toString(), bytes);
	}

	/**
	 * Adds the given class file to the file system.
	 * @param path path where to write the class
	 * @param clazz the class name
	 * @param bytes the class bytecode
	 * @throws IllegalArgumentException
	 */
	public void writeClass(Path path, String clazz, byte[] bytes) {
		Contracts.requires(path, "path");
		Contracts.requires(clazz, "clazz");
		Contracts.requires(bytes, "bytes");
	
		Path parent = Paths.get("./");
		if (isClass || isJar) {
			parent = path.getParent();
			if (parent == null) {
				parent = Paths.get("./");
			}
		} else {
			path = Paths.get(root.toString(), clazz + ".class");
		}
		var p = Paths.get(parent.toString(), clazz + ".class").toString().replace("./", "");
		entries.put(p, bytes);
	}
	
	/**
	 * Saves the files to the disk.
	 * @throws IOException
	 */
	public void save() throws IOException {
		if (isJar) {
			saveAsJar();
		} else {
			saveAsFiles();
		}
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
		Contracts.requires(path, "path");
		Contracts.requires(clazz, "clazz");
		Contracts.requires(consumer, "consumer");
	
		if (!clazz.endsWith(".class")) {
			clazz += ".class";
		}
		
		path = path.getParent();
		if (path == null) {
			path = Paths.get(clazz);
		} else {
			path = Paths.get(path.toString(), clazz);				
		}

		if (isJar) {
			try(var zip = new ZipFile(path.toString())) {
				var entries = zip.stream().collect(Collectors.toList());
				for (var entry : entries) {
					if (entry.getName().equals(path.toString())) {						
						try(var in = zip.getInputStream(entry)) {
							consumer.accept(Paths.get(entry.getName()), in.readAllBytes());
						}
						return;
					}
				}
			}
			throw new FileNotFoundException(path.toString());
		}

		try(var in = new FileInputStream(path.toFile())) {
			consumer.accept(path, in.readAllBytes());
		}
	}

	/**
	 * Removes files added with a call to write() method.
	 */
	public void clear() {
		this.entries.clear();
	}



	private void saveAsJar() throws IOException  {
		openJar(root, e -> !e.isDirectory() && !e.getName().endsWith(".class"), (p, b) -> {
			entries.put(p.toString(), b);
		});
		var name = this.root.getFileName().toString();
		name = name.substring(0, name.lastIndexOf('.')) + "-retro.jar";
		var path = Paths.get(outputdir().toString(), name);	
		try(var out = new JarOutputStream(new FileOutputStream(path.toFile()))) {
			for (var entry : entries.entrySet()) {
				out.putNextEntry(new ZipEntry(entry.getKey()));
				out.write(entry.getValue());
			}
		}
	}
	
	private void saveAsFiles() throws FileNotFoundException, IOException {
		var path = outputdir();
		for (var entry : entries.entrySet()) {
			var name = Paths.get(entry.getKey()).getFileName().toString();
			try(var out = new FileOutputStream(Paths.get(path.toString(), name).toFile())) {
				out.write(entry.getValue());
			}
		}
	}

	private Path outputdir() throws IOException {
		var path = this.root;
		if (!isDirectory) {
			path = path.getParent();
			if (path == null) {
				path = Paths.get("./");
			}
		}

		path = Paths.get(path.toString(), "retro-output");
		Files.createDirectories(path);
		return path;
	}

	private void openClass(Path path, BiConsumer<Path, byte[]> consumer) throws FileNotFoundException, IOException {
		try(var in = new FileInputStream(path.toFile())) {
			consumer.accept(path, in.readAllBytes());
		}
	}
	
	private void openJar(Path path, Predicate<? super ZipEntry> predicate, BiConsumer<Path, byte[]> consumer) throws IOException {
		try(var zip = new ZipFile(path.toString())) {
			var entries = zip.stream()
					.filter(predicate)
					.collect(Collectors.toList());
			for (var entry : entries) {
				try(var in = zip.getInputStream(entry)) {
					consumer.accept(Paths.get(entry.getName()), in.readAllBytes());
				}
			}
		}
	}
	
}
