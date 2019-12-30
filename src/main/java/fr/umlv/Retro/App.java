package fr.umlv.Retro;

import java.io.IOException;

import fr.umlv.Retro.cli.CommandLineParser;;

public class App {

	// -target 7 --force -info -help -features Lambda,Concat,NestMates,TryWithResources ../Drafts/

    public static void main(String[] args) throws IOException {
        var commandLine = CommandLineParser.parse(args, ":*target :*features force info help");
        Retro.fromCommandLine(commandLine).run();
    }
}
