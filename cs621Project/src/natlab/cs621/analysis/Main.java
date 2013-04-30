package natlab.cs621.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import natlab.cs621.analysis.ReachingDefs;

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
    
    // Run the analysis here.
    // Here we run the example reaching defs analysis.
    // Note that the analysis is intraprocedural. We're going to run it on each function/script
    // separately. It would also be possible for the analysis itself to take care of this
    // by defining appropriate caseFunction and caseScript methods.
    
    // Map functions and scripts to their analysis results
    Map<ASTNode<?>, ReachingDefs> analyses = new HashMap<ASTNode<?>, ReachingDefs>();
    for (Program unit : program.getPrograms()) {
      if (unit instanceof Script) {
        analyses.put(unit, ReachingDefs.of(unit));
      } else if (unit instanceof FunctionList) {
        for (Function f : ((FunctionList) unit).getFunctions()) {
          analyses.put(f, ReachingDefs.of(f));
        }
      }
    }
    
    // Report the analysis results.
    for (ASTNode<?> node : analyses.keySet()) {
      System.out.println("Reaching defs for " + getName(node));
      analyses.get(node).prettyPrint();
    }
  }
}
