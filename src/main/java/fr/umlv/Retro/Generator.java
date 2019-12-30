package fr.umlv.Retro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class Generator {
	private final HashMap<String, byte[]> bytes = new HashMap<>();
	
	public static void fromPath(Path path) {
		if (path == null) {
			throw new IllegalArgumentException("path");
		}
        var name = path.getFileName().toString();
        var isDirectory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
        var isClass = !isDirectory && name.endsWith(".class");
        var isJar = !isDirectory && name.endsWith(".jar");
       
	}
}
