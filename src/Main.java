import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.types.Path;

import wybs.util.AbstractCompilationUnit.Tuple;
import wyc.lang.WhileyFile;
import wyc.lang.WhileyFile.Decl;
import wyc.lang.WhileyFile.Expr;
import wyc.lang.WhileyFile.Stmt;
import wyc.util.AbstractVisitor;

/**
 * <p>
 * This provides an example project for reading in a Whiley file, compiling it
 * and traversing the Abstract Syntax Tree. The project illustrates a minimal
 * Maven configuration file (pom.xml) for automatically downloading the compiler
 * components from Maven Central.
 * </p>
 *
 * <p>
 * Note, this does not implement a compiler plugin, however, as that is much
 * more involved. The code prints out the name of all functions and methods
 * declared in the compiled WyIL file.
 * </p>
 *
 * @author David J. Pearce
 *
 */
public class Main {
	/**
	 * Identify location of Whiley Standard Library
	 */
	private static String WYSTD_LIB = "lib/wystd-v0.2.3.jar".replaceAll("/", File.separator);

	/**
	 * This is a very simple client which takes a Whiley file, and compiles it. The
	 * point is only to illustrate how to write a visitor for the Whiley
	 * Intermediate Language.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if(args.length == 0) {
			// If no command-line argument given, just run over all files in the examples
			// directory.
			for(String file : new File("examples").list()) {
				if(file.endsWith("whiley")) {
					run(file);
				}
			}
		} else {
			// Otherwise, run over a specific file
			run(args[0]);
		}
	}

	public static void run(String filename) throws Exception {
		try {
			WhileyFile wf = Util.compile(filename, "examples", false, WYSTD_LIB);
			System.out.println("=========================================================");
			System.out.println("File: " + filename);
			System.out.println("=========================================================");
			// Run over the file and print everything out
			new WhileyFilePrinter(System.out).apply(wf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
