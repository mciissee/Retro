package fr.umlv.Retro;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Arrays;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import fr.umlv.Retro.cli.CommandLineParser;
import fr.umlv.Retro.models.TransformOptions;;

public class App {

	// -target 7 --force -info -help -features Lambda,Concat ../Drafts/TestConcat.class

    public static void main(String[] args) throws IOException {
        var cli = CommandLineParser.parse(args, ":*target :*features force info help");
        var defaults = cli.args();
        if (defaults.length == 0) {
        	throw new AssertionError("A path must be specified. use --help for a list of possible options");
        }
        var path = Paths.get(defaults[0]);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
        	throw new AssertionError("A valid path must be specified. use --help for a list of possible options");
        }

    	// var cr = new ClassReader(new FileInputStream("../Drafts/TestConcatID.class"));
    	var cr = new ClassReader("TestLambda");
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        var options = TransformOptions.fromCommandLine(args);
        cr.accept(new ClassTransformer(cw, options), ClassReader.EXPAND_FRAMES);

        /*
        if (Files.isDirectory(Paths.get("../Drafts/"))) {
        	var fos = new FileOutputStream("../Drafts/TestLambda.class");
        	fos.write(cw.toByteArray());
        	fos.close();
        }
        */
    }
}
