package fr.umlv.Retro;

import java.io.IOException;
import java.net.URISyntaxException;

import fr.umlv.Retro.cli.CommandLineParser;;

public class App {

	// -target 7 --force -info  examples

    public static void main(String[] args) throws IOException, URISyntaxException {
        var commandLine = CommandLineParser.parse(args, ":*target :features force info help");
        Retro.fromCommandLine(commandLine);
    }
}
