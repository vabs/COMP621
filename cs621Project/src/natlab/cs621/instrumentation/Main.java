package natlab.cs621.instrumentation;

import java.io.File;
import java.io.IOException;

import mclint.util.Parsing;
import ast.CompilationUnits;
import ast.Program;

import com.google.common.base.Charsets;
import com.google.common.io.Files;


public class Main {
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
  
  private static File createFreshDirectory(String name) {
    File dir = new File(name);
    deleteRecursively(dir);
    dir.mkdir();
    return dir;
  }

  public static void main(String[] args) throws IOException {
    // Parse the input files into an AST.
    CompilationUnits program = Parsing.files(args);
    
    // Modify the input program here.
    // Here we call the example profiling:
    ProfileAssignments.instrument(program);
    
    // Pretty-print the modified program.
    File outputDirectory = createFreshDirectory("instrumented");
    for (Program unit : program.getPrograms()) {
      File outputFile = new File(outputDirectory, unit.getName() + ".m");
      Files.write(unit.getPrettyPrinted(), outputFile, Charsets.UTF_8);
    }
  }
}
