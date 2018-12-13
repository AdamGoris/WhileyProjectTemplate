import java.io.IOException;
import java.util.ArrayList;

import wyal.lang.WyalFile;
import wybs.util.StdBuildRule;
import wybs.util.StdProject;
import wyc.lang.WhileyFile;
import wyc.task.CompileTask;
import wyc.task.Wyil2WyalBuilder;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.DirectoryRoot;
import wyfs.util.JarFileRoot;
import wyfs.util.Trie;
import wytp.provers.AutomatedTheoremProver;

/**
* <p>
* The code in this file illustrates how to read in a WyIL file specified as a
* command-line parameter and interact with it. Note, this does not implement a
* compiler plugin, however, as that is much more involved. The code prints out
* the name of all functions and methods declared in the given WyIL file.
* </p>
*/
public class Util {
	/**
	 * Identifies which whiley source files should be considered for compilation. By
	 * default, all files reachable from srcdir are considered.
	 */
	private static Content.Filter<WhileyFile> whileyIncludes = Content.filter("**", WhileyFile.ContentType);
	/**
	 * Identifies which WyIL source files should be considered for verification. By
	 * default, all files reachable from srcdir are considered.
	 */
	private static Content.Filter<WhileyFile> wyilIncludes = Content.filter("**", WhileyFile.BinaryContentType);
	/**
	 * Identifies which WyAL source files should be considered for verification. By
	 * default, all files reachable from srcdir are considered.
	 */
	private static Content.Filter<WyalFile> wyalIncludes = Content.filter("**", WyalFile.ContentType);
	/**
	 * A simple default registry which knows about whiley files and wyil files.
	 */
	private static final Content.Registry registry = new wyc.Activator.Registry();

	/**
	 * Compile a single Whiley file into the intermediate format. This requires
	 * setting up a temporary project in order to run the compiler within.
	 *
	 * @param filename     Whiley file to compile
	 * @param root         Root directory in which to compile Whiley file
	 * @param verify       Flag whether or not to apply verification
	 * @param dependencies List of jar files to include on build path
	 * @return
	 * @throws Exception
	 */
	public static WhileyFile compile(String filename, String root, boolean verify, String... dependencies)
			throws Exception {
		Path.ID id = extractPathID(filename);
		// The directory root specified where to look for Whiley / WyIL files.
		DirectoryRoot dir = new DirectoryRoot(root,registry);
		StdProject project = createWhileyProject(dir, dependencies);
		// Add build rules
		addCompilationRules(project,dir,verify);
		// Identify source file
		Path.Entry<WhileyFile> srcEntry = dir.get(id,WhileyFile.ContentType);
		if(srcEntry == null) {
			throw new IllegalArgumentException("unable to find file " + filename);
		}
		ArrayList<Path.Entry<WhileyFile>> srcFiles = new ArrayList<>();
		srcFiles.add(srcEntry);
		// Build the project
		project.build(srcFiles);
		// Flush any created resources (e.g. wyil files)
		dir.flush();
		// Binary form of WhileyFile is a WyIL File.
		Path.Entry<WhileyFile> entry = project.get(id, WhileyFile.BinaryContentType);
		// Finally, read and decode the WyIL file
		return entry.read();
	}

	/**
	 * Extract the path ID for the given filename. This is a relative path from the
	 * project root.
	 *
	 * @param filename
	 * @return
	 */
	public static Path.ID extractPathID(String filename) {
		// Strip the filename extension
		filename = filename.replace(".whiley", "");
		// Create ID from ROOT constant
		Path.ID id = Trie.ROOT.append(filename);
		// Done!
		return id;
	}

	/**
	 * Create a default Whiley project in the given directory. This can then be used
	 * to read WyIL files from that directory.
	 *
	 * @param dir
	 * @return
	 */
	public static StdProject createWhileyProject(DirectoryRoot root, String... dependencies) throws IOException {
		ArrayList<Path.Root> roots = new ArrayList<>();
		roots.add(root);
		for(String dependency : dependencies) {
			roots.add(new JarFileRoot(dependency, registry));
		}
		// Finally, create the project itself
		return new StdProject(roots);
	}

	/**
	 * Add compilation rules for compiling a Whiley file into a WyIL file and, where
	 * appropriate, for performing verification as well.
	 *
	 * @param project
	 * @param root
	 * @param verify
	 */
	private static void addCompilationRules(StdProject project, Path.Root root, boolean verify) {
		CompileTask task = new CompileTask(project);
		// Add compilation rule(s) (whiley => wyil)
		project.add(new StdBuildRule(task, root, whileyIncludes, null, root));
		// Rule for compiling WyIL to WyAL. This will force generation of WyAL files
		// regardless of whether verification is enabled or not.
		Wyil2WyalBuilder wyalBuilder = new Wyil2WyalBuilder(project);
		project.add(new StdBuildRule(wyalBuilder, root, wyilIncludes, null, root));
		//
		if(verify) {
			// Only configure verification if we're actually going to do it!
			wytp.types.TypeSystem typeSystem = new wytp.types.TypeSystem(project);
			AutomatedTheoremProver prover = new AutomatedTheoremProver(typeSystem);
			wyal.tasks.CompileTask wyalBuildTask = new wyal.tasks.CompileTask(project,typeSystem,prover);
			wyalBuildTask.setVerify(verify);
			project.add(new StdBuildRule(wyalBuildTask, root, wyalIncludes, null, root));
		}
	}

}
