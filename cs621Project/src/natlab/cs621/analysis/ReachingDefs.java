package natlab.cs621.analysis;

import java.util.HashSet;
import java.util.Set;

import natlab.toolkits.analysis.HashMapFlowMap;
import natlab.toolkits.analysis.Merger;
import natlab.toolkits.analysis.Mergers;
import nodecases.AbstractNodeCaseHandler;
import analysis.AbstractSimpleStructuralForwardAnalysis;
import ast.ASTNode;
import ast.AssignStmt;
import ast.EmptyStmt;
import ast.Stmt;

/**
 * This is a simple reaching defs analysis. It doesn't handle function parameters,
 * global variables, or persistent variables. (It just maps variable names to assignment
 * statements). It also doesn't handle nested functions.
 */
public class ReachingDefs 
  extends AbstractSimpleStructuralForwardAnalysis<HashMapFlowMap<String, Set<AssignStmt>>> {
  // Factory method, instantiates and runs the analysis
  public static ReachingDefs of(ASTNode<?> tree) {
    ReachingDefs analysis = new ReachingDefs(tree);
    analysis.analyze();
    return analysis;
  }
  
  public void prettyPrint() {
    getTree().analyze(this.new Printer());
  }

  private ReachingDefs(ASTNode tree) {
    super(tree);
  }

  // The initial flow is an empty map.
  @Override
  public HashMapFlowMap<String, Set<AssignStmt>> newInitialFlow() {
    return new HashMapFlowMap<String, Set<AssignStmt>>();
  }
  
  @Override
  public void caseStmt(Stmt node) {
    inFlowSets.put(node, currentInSet.copy());
    currentInSet.copy(currentOutSet);
    outFlowSets.put(node, currentOutSet.copy());
  }

  @Override
  public void caseAssignStmt(AssignStmt node) {
    inFlowSets.put(node, currentInSet.copy());

    // kill. We kill all previous definitions of variables defined by this statement.
    // (We don't need the actual defs, just the variables, since we can just remove by key
    // in the map). Gathering up the variables can be fairly complicated if we're just
    // working with the AST without simplifications; you can have e.g.
    // * multiple assignments: [x, y] = ...
    // * complicated lvalues: a(i).b = ...
    // The getLValues() method takes care of all the cases.
    Set<String> kill = node.getLValues();

    // gen just maps every lvalue to a set containing this statement.
    HashMapFlowMap<String, Set<AssignStmt>> gen = newInitialFlow();
    for (String s : node.getLValues()) {
      Set<AssignStmt> defs = new HashSet<AssignStmt>();
      defs.add(node);
      gen.put(s, defs);
    }

    // compute (in - kill) + gen
    currentOutSet = newInitialFlow();
    currentInSet.copy(currentOutSet);
    currentOutSet.removeKeys(kill);
    currentOutSet.union(gen);

    outFlowSets.put(node, currentOutSet.copy());
  }

  // Copy is straightforward.
  @Override
  public void copy(HashMapFlowMap<String, Set<AssignStmt>> src,
      HashMapFlowMap<String, Set<AssignStmt>> dest) {
    src.copy(dest);
  }
  
  // We just want to create this merger once. It's used in merge() below.
  private static final Merger<Set<AssignStmt>> UNION = Mergers.union();

  // Here we define the merge operation. There are two "levels" of set union here:
  // union on the maps by key (the union method)
  // if a key is in both maps, union the two sets (the UNION merger passed to the method)
  @Override
  public void merge(HashMapFlowMap<String, Set<AssignStmt>> in1,
      HashMapFlowMap<String, Set<AssignStmt>> in2,
      HashMapFlowMap<String, Set<AssignStmt>> out) {
    in1.union(UNION, in2, out);
  }
  
  // This class pretty prints the program annotated with analysis results.
  class Printer extends AbstractNodeCaseHandler {

    private int getLine(ASTNode<?> node) {
      return beaver.Symbol.getLine(node.getStart());
    }
    
    private int getColumn(ASTNode<?> node) {
      return beaver.Symbol.getColumn(node.getStart());
    }
    
    @Override
    public void caseASTNode(ASTNode node){
      for(int i = 0; i < node.getNumChild(); i++) {
        node.getChild(i).analyze(this);
      }
    }
    
    @Override
    public void caseStmt(Stmt node){
      System.out.println("in {");
      printMap(inFlowSets.get(node));
      System.out.println("}");
      System.out.println(node.getPrettyPrinted());
      System.out.println("out {");
      printMap(outFlowSets.get(node));
      System.out.println("}");
      System.out.println();
      
      caseASTNode(node);
    }

    @Override
    public void caseEmptyStmt(EmptyStmt node) {
      return;
    }

    private void printMap(HashMapFlowMap<String, Set<AssignStmt>> map) {
      for (String var : map.keySet()) {
        System.out.print(var + ": ");
        boolean first = true;
        for (AssignStmt def : map.get(var)) {
          if (!first) {
            System.out.print(", ");
          }
          first = false;
          System.out.print(String.format("[%s at [%d, %d]]", 
              def.getPrettyPrintedLessComments().trim(), getLine(def), getColumn(def)));
        }
        System.out.println();
      }
    }
  }
}

