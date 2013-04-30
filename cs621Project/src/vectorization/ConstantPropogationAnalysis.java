package vectorization;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import natlab.toolkits.analysis.HashMapFlowMap;
import natlab.toolkits.analysis.Merger;
import natlab.toolkits.analysis.Mergers;
import natlab.utils.NodeFinder;
import nodecases.AbstractNodeCaseHandler;
import analysis.AbstractSimpleStructuralForwardAnalysis;
import ast.ASTNode;
import ast.AssignStmt;
import ast.LiteralExpr;
import ast.BinaryExpr;
import ast.EmptyStmt;
import ast.Expr;
import ast.FPLiteralExpr;
import ast.FloatType;
import ast.IntLiteralExpr;
import ast.LiteralExpr;
import ast.MDivExpr;
import ast.MTimesExpr;
import ast.MinusExpr;
import ast.NameExpr;
import ast.PlusExpr;
import ast.Stmt;

public class ConstantPropogationAnalysis
		extends
		AbstractSimpleStructuralForwardAnalysis<HashMapFlowMap<String, Set<LiteralExpr>>> {

	private HashMap<String, LiteralExpr> currentSet;

	public static ConstantPropogationAnalysis of(ASTNode<?> tree) {
		ConstantPropogationAnalysis analysis = new ConstantPropogationAnalysis(
				tree);
		analysis.analyze();
		return analysis;
	}

	public void prettyPrint() {
		getTree().analyze(this.new Intst());
	}

	private ConstantPropogationAnalysis(ASTNode tree) {
		super(tree);
		currentInSet = newInitialFlow();
		currentOutSet = newInitialFlow();
	}

	// The initial flow is an empty map.
	@Override
	public HashMapFlowMap<String, Set<LiteralExpr>> newInitialFlow() {
		return new HashMapFlowMap<String, Set<LiteralExpr>>();
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
		Set<String> kill = new HashSet<String>();
		HashMapFlowMap<String, Set<LiteralExpr>> gen = newInitialFlow();

		Iterator<NameExpr> itr3 = node.getLHS().getAllNameExpressions()
				.iterator();
		while (itr3.hasNext()) {
			String temp = itr3.next().getVarName();
			kill.add(temp);
		}

		ASTNode expr = node.getChild(1);

		if (expr instanceof BinaryExpr) {
			boolean flag = false;
			BinaryExpr bexpr = (BinaryExpr) expr;
			BinaryExprEvaluate bee = new BinaryExprEvaluate();
			Expr ev = bee.evaluate((Expr) expr, currentInSet);

			if (ev instanceof LiteralExpr) {

				Iterator<String> itr2 = node.getLValues().iterator();
				Set<LiteralExpr> defs = new HashSet<LiteralExpr>();
				defs.add((LiteralExpr) ev);
				gen.put(itr2.next(), defs);

			}
			node.setChild(ev, 1);
		} else {

			Iterable<LiteralExpr> t = NodeFinder.find(LiteralExpr.class, node);
			Iterator<LiteralExpr> itr = t.iterator();
			// x=10
			if (itr.hasNext()) {
				LiteralExpr lval = itr.next();

				Iterator<String> itr2 = node.getLValues().iterator();
				Set<LiteralExpr> defs = new HashSet<LiteralExpr>();
				defs.add(lval);
				gen.put(itr2.next(), defs);

			} else {
				// y=x
				Iterator<NameExpr> itr2 = node.getRHS().getAllNameExpressions()
						.iterator();

				String temp = itr2.next().getVarName();
				if (currentInSet.containsKey(temp)) {
					Set<LiteralExpr> getvalues = currentInSet.get(temp);
					Iterator<LiteralExpr> itr4 = getvalues.iterator();
					if (itr4.hasNext()) {
						LiteralExpr val = itr4.next();
						if(val!=null){
						node.setRHS(val);
						}
						Iterator<String> itr5 = node.getLValues().iterator();
						Set<LiteralExpr> defs = new HashSet<LiteralExpr>();
						defs.add(val);
						gen.put(itr5.next(), defs);
					}
				}
			}
		}

		currentInSet.copy(currentOutSet);
		currentOutSet.removeKeys(kill);

		Iterator<String> genval = gen.keySet().iterator();
		while (genval.hasNext()) {
			String t1 = genval.next();
			currentOutSet.put(t1, gen.get(t1));
		}

		outFlowSets.put(node, currentOutSet.copy());

	}

	@Override
	public void copy(HashMapFlowMap<String, Set<LiteralExpr>> src,
			HashMapFlowMap<String, Set<LiteralExpr>> dest) {
		src.copy(dest);
	}

	// We just want to create this merger once. It's used in merge() below.
	private static final Merger<Set<LiteralExpr>> UNION = Mergers.union();

	public void union(HashMapFlowMap<String, Set<LiteralExpr>> in1,
			HashMapFlowMap<String, Set<LiteralExpr>> in2,
			HashMapFlowMap<String, Set<LiteralExpr>> out) {

	}

	@Override
	public void merge(HashMapFlowMap<String, Set<LiteralExpr>> in1,
			HashMapFlowMap<String, Set<LiteralExpr>> in2,
			HashMapFlowMap<String, Set<LiteralExpr>> out) {
		in1.union(UNION, in2, out);
		System.out.println("-----");
		Iterator<String> rr = out.keySet().iterator();
		while (rr.hasNext()) {
			String r1 = rr.next();
			System.out.println(r1);
			Iterator it = out.get(r1).iterator();
			int count = 0;
			while (it.hasNext()) {
				count++;
				it.next();
			}
			if (count > 1) {
				Set<LiteralExpr> s1 = new HashSet<LiteralExpr>();
				s1.add(null);
				out.put(r1, s1);
			}
		}
		System.out.println("-----");
		rr = out.keySet().iterator();
		while (rr.hasNext()) {
			String r1 = rr.next();
			System.out.println(r1 + ":");
			Iterator it = out.get(r1).iterator();
			int count = 0;
			while (it.hasNext()) {

				System.out.println(it.next());
			}

		}
		System.out.println("-----");
	}

	class Intst extends AbstractNodeCaseHandler {
		public void instrument(ASTNode<?> node) {
			node.analyze(new Intst());
		}

		@Override
		public void caseASTNode(ASTNode node) {
			for (int i = 0; i < node.getNumChild(); ++i) {

				if (node.getChild(i) != null)
					node.getChild(i).analyze(this);
			}
		}
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
		public void caseASTNode(ASTNode node) {
			for (int i = 0; i < node.getNumChild(); i++) {
				node.getChild(i).analyze(this);
			}
		}

		@Override
		public void caseStmt(Stmt node) {
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

		private void printMap(HashMapFlowMap<String, Set<LiteralExpr>> map) {
			for (String var : map.keySet()) {
				System.out.print(var + ": ");
				boolean first = true;
				for (LiteralExpr def : map.get(var)) {
					if (def != null) {
						if (!first) {
							System.out.print(", ");
						}
						first = false;
						System.out.print(String.format("[%s at [%d, %d]]", def
								.getPrettyPrintedLessComments().trim(),
								getLine(def), getColumn(def)));
					}
				}
				System.out.println();
			}
		}
	}
}
