package fr.umlv.Retro;

// import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
// import java.nio.file.Paths;

// import org.objectweb.asm.ClassReader;
// import org.objectweb.asm.ClassWriter;

import fr.umlv.Retro.cli.CommandLineParser;;

public class App {

	// -target 7 --force -info  examples

    public static void main(String[] args) throws IOException, URISyntaxException {
        var commandLine = CommandLineParser.parse(args, ":target :features force info help");
        Retro.create(commandLine);
        /*
        var r = new ClassReader("java.util.function.Consumer");
        var w = new ClassWriter(0);
        r.accept(w, 0);
        try(var out = new FileOutputStream(Paths.get("examples/JavaConsumer.class").toFile())) {
			out.write(w.toByteArray());
		}
        */
    }
}
