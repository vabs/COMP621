package vectorization;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import natlab.cs621.analysis.ReachingDefs;
import natlab.cs621.instrumentation.ProfileAssignments;

import mclint.util.Parsing;
import ast.ASTNode;
import ast.CompilationUnits;
import ast.Function;
import ast.FunctionList;
import ast.Program;
import ast.Script;


public class Main {
  // extract the name from a function or script
  private static String getName(ASTNode<?> node) {
    if (node instanceof Script) {
      return ((Script) node).getName();
    } else if (node instanceof Function) {
      return ((Function) node).getName();
    }
    return null;
  }

  public static void main(String[] args) throws IOException {
	// Parse the input files into an AST.
	    CompilationUnits program = Parsing.files(args);
	    
	    // Modify the input program here.
	    // Here we call the example profiling:
	    StatementSelfDependence.instrument(program);
	    
	    // Pretty-print the modified program.
	    File outputDirectory = createFreshDirectory("instrumented");
	    for (Program unit : program.getPrograms()) {
	      File outputFile = new File(outputDirectory, unit.getName() + ".m");
	      Files.write(unit.getPrettyPrinted(), outputFile, Charsets.UTF_8);
	    }
  }
  
  private static File createFreshDirectory(String name) {
	    File dir = new File(name);
	    deleteRecursively(dir);
	    dir.mkdir();
	    return dir;
	  }
  
  private static void deleteRecursively(File file) {
	    if (!file.exists()) {
	      return;
	    }
	    if (file.isDirectory()) {
	      for (File child : file.listFiles()) {
	        deleteRecursively(child);
	      }
	    }
	    file.delete();
	  }
}
