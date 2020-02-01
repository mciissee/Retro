package fr.umlv.retro;

import java.io.IOException;
import java.net.URISyntaxException;

import fr.umlv.retro.cli.CommandLineParser;;

public class App {
	public static void main(String[] args) throws IOException, URISyntaxException {
		var commandLine = CommandLineParser.parse(args, ":target :features force info help");
		Retro.exec(commandLine);
	}
}
