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
 * Representation of a virtual file system that maintain a reference to opened class files until they
 * are saved on the disk so they can processed multiple times.<br><br>
 * 
 * When {@link #save()} method is called, the class will save the the modified
 * class fields on a sub-directory of the root path. So original files are never modified.
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
	 * @throws IllegalArgumentException if any of the arguments is null.
	 */
	public static FileSystem create(Path root) throws IOException {
		Contracts.requires(root, "root");

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
	 * Removes written bytecodes from the memory.
	 */
	public void clear() {
		this.entries.clear();
	}

	/**
	 * Writes the given {@code bytecode} at {@code path} on the file system. <br> <br>
	 * 
	 * Note: the bytecode will not be written on the disk until {@link #save()} method is called.
	 * @param path path where to write the bytecode.
	 * @param bytecode bytecode to write.
	 * @throws IllegalArgumentException if any of the arguments is null.
	 */
	public void write(Path path, byte[] bytecode) {
		Contracts.requires(path, "path");
		Contracts.requires(bytecode, "bytes");
		entries.put(path.toString(), bytecode);
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
	public void write(Path path, String className, byte[] bytecode) {
		Contracts.requires(path, "path");
		Contracts.requires(className, "clazz");
		Contracts.requires(bytecode, "bytes");
		entries.put(resolve(path, className + ".class").toString(), bytecode);
	}
	
	/**
	 * Iterates over the file system to find bytecodes call the given consumer for each bytecode.<br><br>
	 * 
	 * Note: the method will always return last modified version of the bytecode. 
	 *
	 * @param consumer function to call for each file.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IllegalArgumentException if any of the arguments is null.
	 */
	public void iterate(BiConsumer<Path, byte[]> consumer) throws FileNotFoundException, IOException {
		Contracts.requires(consumer, "consumer");
		BiConsumer<Path, byte[]> action = (path, bytes) -> {
			var cached = entries.get(path.toString());
			if (cached != null) {
				consumer.accept(path, cached);				
			} else {
				consumer.accept(path, bytes);
			}
		};

		if (this.isClass) {
			bytecode(root, action);
		} else if (isDirectory) {
			var entries = Files.list(root).filter(e -> {
				return  e.toString().endsWith(".class");
			}).collect(Collectors.toList());
			for (var entry : entries) {
				bytecode(entry, action);
			}
		} else if (isJar) {
			openJar(root, e -> !e.isDirectory() && e.getName().endsWith(".class"), action);
		}
	}
	
	/**
	 * Gets the bytecode of the class {@code className} relative to the given {@code path}. <br><br>
	 * 
	 * The method will always return the last modified version of the bytecode since the file system is created.
	 * @param path path where to search the class.
	 * @param className class name
	 * @param consumer bytecode consumer.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IllegalArgumentException if any of the arguments is null.
	 */
	public void bytecode(Path path, String className, BiConsumer<Path, byte[]> consumer) throws FileNotFoundException, IOException {
		Contracts.requires(path, "path");
		Contracts.requires(className, "clazz");
		Contracts.requires(consumer, "consumer");
		path = resolve(path, className + ".class");
		if (isJar) {
			try(var zip = new ZipFile(root.toString())) {
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

	@Override
	public String toString() {
		return root.toString();
	}

	private Path output() throws IOException {
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

	private Path resolve(Path path, String name) {
		path = path.getParent();
		if (path == null) {
			path = Paths.get(name);
		} else {
			path = Paths.get(path.toString(), name);				
		}
		return path;
	}

	private void saveAsJar() throws IOException  {
		openJar(root, e -> !e.isDirectory() && !e.getName().endsWith(".class"), (p, b) -> {
			entries.put(p.toString(), b);
		});
		var name = this.root.getFileName().toString();
		name = name.substring(0, name.lastIndexOf('.')) + "-retro.jar";
		var path = Paths.get(output().toString(), name);	
		try(var out = new JarOutputStream(new FileOutputStream(path.toFile()))) {
			for (var entry : entries.entrySet()) {
				out.putNextEntry(new ZipEntry(entry.getKey()));
				out.write(entry.getValue());
			}
		}
	}
	
	private void saveAsFiles() throws FileNotFoundException, IOException {
		var path = output();
		for (var entry : entries.entrySet()) {
			var name = Paths.get(entry.getKey()).getFileName().toString();
			try(var out = new FileOutputStream(Paths.get(path.toString(), name).toFile())) {
				out.write(entry.getValue());
			}
		}
	}

	private void bytecode(Path path, BiConsumer<Path, byte[]> consumer) throws FileNotFoundException, IOException {
		var cache = entries.get(path.toString());
		if (cache != null) {
			consumer.accept(path, cache);
			return;
		}
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
