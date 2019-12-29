package fr.umlv.Retro;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import fr.umlv.Retro.models.TransformOptions;;


public class App {
    public static void main(String[] args) throws IOException {

    	//var cr = new ClassReader(new FileInputStream("../Drafts/TestConcatID.class"));
    	var cr = new ClassReader("TestLambda");
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        var options = TransformOptions.fromCommandLine(args);
        cr.accept(new ClassTransformer(cw, options), 0);
 
        if (Files.isDirectory(Paths.get("../Drafts/"))) {
        	var fos = new FileOutputStream("../Drafts/TestLambda.class");
        	fos.write(cw.toByteArray());
        	fos.close();
        }
    }
}
