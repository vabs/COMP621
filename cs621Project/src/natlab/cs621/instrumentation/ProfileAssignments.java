package natlab.cs621.instrumentation;

import java.util.HashSet;
import java.util.Set;

import nodecases.AbstractNodeCaseHandler;
import ast.ASTNode;
import ast.AssignStmt;
import ast.ForStmt;
import ast.Function;
import ast.List;
import ast.ReturnStmt;
import ast.Script;
import ast.Stmt;

/**
 * This class is an example of instrumentation. It rewrites an input program to keep track
 * of the number of assignments made at runtime.
 */
public class ProfileAssignments extends AbstractNodeCaseHandler {
  public static void instrument(ASTNode<?> node) {
    node.analyze(new ProfileAssignments());
  }
  
  // This remembers either the current function name, or "script" for scripts
  private String currentScope;

  // Statements in the skip set won't be analyzed / instrumented.
  // In general we only want to analyze the input program, so we'll
  // add the instrumentation statements we create to this set.
  // There may also be other cases where we just want to skip a node for whatever reason.
  private Set<Stmt> skip = new HashSet<Stmt>();

  // Little helper to add something to the skip set while still using it as an expression
  private Stmt skip(Stmt node) {
    skip.add(node);
    return node;
  }
  
  private Stmt init() {
    return skip(Asts.init(currentScope));
  }
  
  private Stmt increment() {
    return skip(Asts.increment(currentScope));
  }
  
  private Stmt display() {
    return skip(Asts.display(currentScope));
  }
  
  // This is the default node case. We recurse on the children from left to right,
  // so we're traversing the AST depth-first.
  @Override
  public void caseASTNode(ASTNode node) {
    for (int i = 0; i < node.getNumChild(); ++i) {
      node.getChild(i).analyze(this);
    }
  }

  // This is a helper used by both the Function and Script cases
  private void instrumentStmtList(List<Stmt> stmts) {
    // insert the counter initialization statement as the first statement,
    // and the counter display as the last statement
    stmts.insertChild(init(), 0);
    stmts.addChild(display());

    // recurse on children
    caseASTNode(stmts);

    skip.clear();
  }

  public void caseScript(Script node) {
    currentScope = "script";
    instrumentStmtList(node.getStmts());
  }

  public void caseFunction(Function node) {
    currentScope = node.getName();
    instrumentStmtList(node.getStmts());
    node.getNestedFunctions().analyze(this);
  }

  /**
   * We want to count assignments made in for loops, e.g. in
   * for i = 1:10
   *  a(i) = i;
   * end
   * 
   * We want to count the assignments to a(i), but also the implicit assignments to i.
   * We do this by adding a counter update as the first statement in the loop body.
   */ 
  @Override
  public void caseForStmt(ForStmt node) {
    node.getStmtList().insertChild(increment(), 0);

    // skip the assignment in the for loop header, as we're handling it ourselves
    skip.add(node.getAssignStmt());

    caseASTNode(node);
  }

  @Override
  public void caseAssignStmt(AssignStmt node) {
    if (skip.contains(node)) {
      return;
    }

    AstUtil.insertAfter(node, increment());
  }

  /**
    Functions can end early with a return statement; in that case we also want to
    display the counter, but the end of the function won't be reached, so we insert
    the display before the return statement also.
  */
  @Override
  public void caseReturnStmt(ReturnStmt node) {
    if (skip.contains(node)) {
      return;
    }

    AstUtil.insertBefore(node, display());

    // The insertion modified the tree being traversed, so the return statement will
    // be visited again. Add it to skip to avoid this
    skip.add(node);
  }
}