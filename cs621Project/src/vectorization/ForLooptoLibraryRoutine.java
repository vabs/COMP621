package vectorization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import natlab.toolkits.analysis.varorfun.VFPreorderAnalysis;
import nodecases.AbstractNodeCaseHandler;
import ast.ASTNode;
import ast.AssignStmt;
import ast.ForStmt;
import ast.Function;
import ast.IntLiteralExpr;
import ast.List;
import ast.MTimesExpr;
import ast.NameExpr;
import ast.PlusExpr;
import ast.RangeExpr;
import ast.ReturnStmt;
import ast.Script;
import ast.Stmt;

/**
 * This class is an example of instrumentation. It rewrites an input program to
 * keep track of the number of assignments made at runtime.
 */
public class ForLooptoLibraryRoutine extends AbstractNodeCaseHandler {
	public static void instrument(ASTNode<?> node) {
		node.analyze(new ForLooptoLibraryRoutine());
	}

	private String currentScope;

	private Set<Stmt> skip = new HashSet<Stmt>();

	private HashMap<String, ASTNode> lvalstack = new HashMap<>();
	private HashMap<String, ASTNode> rvalstack = new HashMap<>();
	private HashMap<ASTNode, ASTNode> optstack = new HashMap<>();
	private HashMap<ASTNode, ASTNode> rangestack = new HashMap<>();
	private HashMap<ASTNode<IntLiteralExpr>, ASTNode> intstack = new HashMap<>();
	private Set<ForStmt> forstack = new HashSet<>();
	

	private Stmt skip(Stmt node) {
		skip.add(node);
		return node;
	}

	private Stmt init() {
		return skip(Asts.init(currentScope));
	}

	private Stmt increment() {
		return(Asts.increment(currentScope));
	}

	private Stmt display() {
		return skip(Asts.display(currentScope));
	}

	@Override
	public void caseASTNode(ASTNode node) {
		for (int i = 0; i < node.getNumChild(); ++i) {
			node.getChild(i).analyze(this);
		}
	}

	private void instrumentStmtList(List<Stmt> stmts) {

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

	@Override
	public void caseForStmt(ForStmt node) {
		forstack.add(node);

		caseASTNode(node);
	}

	@Override
	public void caseAssignStmt(AssignStmt node) {

		Iterator<String> lvals = node.getLValues().iterator();
		while (lvals.hasNext()) {

			lvalstack.put(lvals.next(), node.getParent());

		}

		VFPreorderAnalysis kind = new VFPreorderAnalysis(node);
		int length = forstack.size();
		if (length > 0) {
			if (node.getChild(1) instanceof PlusExpr) {
				optstack.put(node.getChild(1), node.getParent());
			} else if (node.getChild(1) instanceof MTimesExpr) {
				optstack.put(node.getChild(1), node.getParent());
			} else if (node.getChild(1) instanceof RangeExpr) {
				rangestack.put(node.getChild(1), node.getParent());
			} else if (node.getChild(1) instanceof IntLiteralExpr) {
				intstack.put(node.getChild(1), node.getParent());
			}
		}
		
	}

	@Override
	public void caseReturnStmt(ReturnStmt node) {

	}

	private ASTNode<?> insertStmt(int i, IntLiteralExpr range1,
			IntLiteralExpr range2, NameExpr varname) {
		return Asts.display(i, range1, range2, varname);
	}

}