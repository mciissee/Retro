package fr.umlv.retro.api;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import fr.umlv.retro.FileSystem;
import fr.umlv.retro.Retro;
import fr.umlv.retro.models.Features;
import fr.umlv.retro.models.Logger;
import fr.umlv.retro.models.Options;
import fr.umlv.retro.utils.Contracts;

@ApplicationScoped
public class EnvService {

	private static class MultipartReader {
		private final Options options;
		private final HashMap<String, byte[]> files;
		private final int ttl;

		private MultipartReader(int ttl, Options options, HashMap<String, byte[]> files) {
			this.ttl = ttl;
			this.options = Objects.requireNonNull(options);
			this.files = Objects.requireNonNull(files);
		}

		private static MultipartReader read(MultipartFormDataInput input) throws IOException {
			Contracts.requires(input, "input");
			var target = input.getFormDataPart("target", new GenericType<Integer>() {
			});
			if (target == null) {
				throw new IllegalArgumentException("missing parameter 'target'");
			}
			var map = input.getFormDataMap();
			var files = map.get("files[]");
			if (files == null) {
				throw new IllegalArgumentException("missing parameter 'files'");
			}
			var ttl = input.getFormDataPart("ttl", new GenericType<Integer>() {
			});
			if (ttl == null) {
				ttl = 60 * 1000;
			}
			var force = input.getFormDataPart("force", new GenericType<Boolean>() {
			});
			if (force == null) {
				force = false;
			}
			EnumSet<Features> enumSet;
			var feature = input.getFormDataPart("features", new GenericType<String>() {
			});
			if (feature == null || feature.isEmpty()) {
				enumSet = Features.ALL;
			} else {
				enumSet = EnumSet.copyOf(Arrays.stream(feature.split(",")).map(e -> {
					return Features.valueOf(e);
				}).collect(Collectors.toList()));
			}
			var fileMap = new HashMap<String, byte[]>();
			var i = 0;
			for (var file : files) {
				var name = fileName(file, i);
				if (!name.endsWith(".class") && !name.endsWith(".jar")) {
					throw new AssertionError("unsupported file '" + name + "' expected a .class or a .jar");
				}
				try (var stream = file.getBody(InputStream.class, null)) {
					fileMap.put(name, stream.readAllBytes());
				}
				i++;
			}
			return new MultipartReader(ttl, new Options(target, force, enumSet), fileMap);
		}

		private static String fileName(InputPart part, int index) {
			var headers = part.getHeaders();
			var disposition = headers.getFirst("Content-Disposition").split(";");
			for (var filename : disposition) {
				if ((filename.trim().startsWith("filename"))) {
					var name = filename.split("=");
					var finalFileName = name[1].trim().replaceAll("\"", "");
					return finalFileName;
				}
			}
			throw new IllegalArgumentException("unknown file name at index " + index);
		}

	}

	public EnvService() {
	}

	/**
	 * Compress the environment identified by {@code envid} in a zip and returns and
	 * response forcing the browser to download the zip file.
	 * 
	 * @param envid the environment's identifier.
	 * @return An Response Object
	 */
	public Response retrieve(String envid) {
		var source = Paths.get("env", envid);
		var target = Paths.get("env", envid + ".zip");
		try {
			purge();
			if (!Files.isDirectory(source)) {
				return Response.status(Status.NOT_FOUND).entity("Error: env '" + envid + "' is not found").build();
			}
			if (!Files.exists(target)) {
				FileSystem.makeZip(source, target);
			}
			var response = Response.ok(target.toFile());
			response.header("Content-Disposition", "attachment;filename=retro.zip");
			response.header("Content-Type", "application/zip");
			return response.build();
		} catch (IOException e) {
			return Response.status(400).entity(e.getMessage()).build();
		}
	}

	/**
	 * Transforms the class files inside the given {@code input} into a new
	 * environment.
	 * 
	 * @param envid the environment's identifier.
	 * @return An Response Object
	 */
	public Response create(MultipartFormDataInput input) {
		var envid = UUID.randomUUID().toString();
		var envdir = Paths.get("env", envid);
		try {
			purge();
			var i = 0;
			var reader = MultipartReader.read(input);
			var filePaths = new Path[reader.files.size()];
			envdir = Files.createDirectories(Paths.get(envdir.toString() + "~" + reader.ttl));
			envid += "~" + reader.ttl;
			for (var entry : reader.files.entrySet()) {
				var name = entry.getKey();
				filePaths[i] = Paths.get(envdir.toString(), name);
				try (var out = new FileOutputStream(filePaths[i].toFile())) {
					out.write(entry.getValue());
				}
				i++;
			}
			var logger = new Logger();
			var success = Retro.exec(filePaths, reader.options, logger);
			return Response.ok(Map.of("success", success, "envid", envid, "logs", logger.get(), "ttl", reader.ttl))
					.build();
		} catch (AssertionError | IOException | URISyntaxException e) {
			try {
				FileUtils.deleteDirectory(envdir.toFile());
			} catch (IOException e1) {
				return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
			}
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	private void purge() {
		var dir = Paths.get("env");
		if (!Files.isDirectory(dir)) {
			return;
		}
		try {
			var entries = Files.list(dir)
					.filter(e -> !e.toString().endsWith(".zip"))
					.toArray(Path[]::new);
			for (var entry : entries) {
				var tokens = entry.getFileName().toString().split("~");
				if (tokens.length == 2) {
					var file = entry.toFile();
					var deleteTime = System.currentTimeMillis() - Integer.parseInt(tokens[1]);
					if (file.lastModified() < deleteTime) {
						System.out.println("marked for delete: " + entry);
						FileUtils.deleteDirectory(file);
						var zip = Paths.get(entry.toString() + ".zip");
						if (Files.exists(zip)) {
							Files.delete(zip);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
