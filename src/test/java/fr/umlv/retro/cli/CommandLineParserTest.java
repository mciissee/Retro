package fr.umlv.retro.cli;


import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;

import junit.framework.TestCase;


public class CommandLineParserTest extends TestCase {

	public void testSouldEqualsReturnTrue() {
		var c1 = new CommandLineOption("opt1");
		var c2 = new CommandLineOption("opt1");
		assertTrue(c1.equals(c2));
	}

	public void testSouldEqualsReturnFalse() {
		var c1 = new CommandLineOption("opt1");
		var c2 = new CommandLineOption("opt");
		assertFalse(c1.equals(c2));
	}

	public void testShouldToStringWork() {
		assertEquals("(opt1 [])", new CommandLineOption("opt1").toString());
		assertEquals("(opt1 [a])", new CommandLineOption("opt1", new String[] {"a"}).toString());
	}

	public void testShouldHasOptionThrowsIfNullArg(){
		var array =  new ArrayList<CommandLineOption>();
		array.add(new CommandLineOption("opt"));
		CommandLine commandeLine = new CommandLine(array);
		assertThrows(IllegalArgumentException.class, () -> commandeLine.hasOption(null) );
	}
	
	public void testShouldHasOptionReturnTrue(){
		var array =  new ArrayList<CommandLineOption>();
		array.add(new CommandLineOption("opt"));
		var commandeLine = new CommandLine(array);
		assertTrue(commandeLine.hasOption("opt"));
	}

	public void testShouldHasOptionReturnFalse(){
		var array =  new ArrayList<CommandLineOption>();
		array.add(new CommandLineOption("opt"));
		var commandeLine = new CommandLine(array);
		assertFalse(commandeLine.hasOption("o"));
	}
	
	public void testShouldStoreArgs(){
		var array =  new ArrayList<CommandLineOption>();
		array.add(new CommandLineOption("opt"));
		array.add(new CommandLineOption("opt2"));
		var commandeLine = new CommandLine(array);
		assertTrue(commandeLine.args().isEmpty());

		array.add(new CommandLineOption("^", new String[] {"path"} ));
		commandeLine = new CommandLine(array);
		assertFalse(commandeLine.args().isEmpty());
	}
	
	
	public void testShouldParseWork(){
		var commandLine = CommandLineParser.parse(
			new String[] { "-target", "7", "-features", "A, B, C" , "../A.class"},
			":target :features force info help"
		);
		
		assertFalse(commandLine.isEmpty());
		assertFalse(commandLine.args().isEmpty());
		assertTrue(!commandLine.hasOption("info"));
		assertTrue(commandLine.hasOption("target"));
		assertTrue(commandLine.hasOption("features"));
		assertTrue(commandLine.hasOption("target"));

		assertEquals(1, commandLine.args().get().length);
		assertEquals("../A.class", commandLine.args().get()[0]);
		assertEquals("A, B, C", commandLine.args("features").get()[0]);
	}
	
	
	public void testShouldParseThrow(){
		assertThrows(AssertionError.class, () -> {
			CommandLineParser.parse(
				new String[] { "-features", "A, B, C" , "../A.class"},
				"*:target :features force info help"
			);
		});
		
		assertThrows(AssertionError.class, () -> {
			CommandLineParser.parse(
				new String[] { "-target" },
				":target"
			);
		});
		

		assertThrows(AssertionError.class, () -> {
			CommandLineParser.parse(
				new String[] { "-o" },
				":target"
			);
		});
	}
}