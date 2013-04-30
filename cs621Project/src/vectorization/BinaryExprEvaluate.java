package vectorization;


import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;

import natlab.FPNumericLiteralValue;
import natlab.toolkits.analysis.HashMapFlowMap;
import ast.BinaryExpr;
import ast.Expr;
import ast.FPLiteralExpr;
import ast.IntLiteralExpr;
import ast.LiteralExpr;
import ast.MDivExpr;
import ast.MTimesExpr;
import ast.MinusExpr;
import ast.NameExpr;
import ast.PlusExpr;

public class BinaryExprEvaluate {

	public Expr evaluate(Expr be,
			HashMapFlowMap<String, Set<LiteralExpr>> currentInSet) {
		
		
		if (be instanceof BinaryExpr) {
			Expr left = evaluate((Expr)be.getChild(0), currentInSet);
			
			Expr right = evaluate((Expr)be.getChild(1), currentInSet);
			
			if (left instanceof LiteralExpr && right instanceof LiteralExpr) {

				BigDecimal lval = new BigDecimal(0);

				LiteralExpr l = (LiteralExpr) left;
				if (left instanceof FPLiteralExpr) {
					FPLiteralExpr l1 = (FPLiteralExpr) l;
					lval = l1.getValue().getValue();

				} else if (left instanceof IntLiteralExpr) {
					IntLiteralExpr l1 = (IntLiteralExpr) l;
					lval = new BigDecimal(l1.getValue().getValue());

				}

				BigDecimal rval = new BigDecimal(0);

				LiteralExpr r = (LiteralExpr) right;
				if (right instanceof FPLiteralExpr) {
					FPLiteralExpr r1 = (FPLiteralExpr) r;
					rval = r1.getValue().getValue();

				} else if (right instanceof IntLiteralExpr) {
					IntLiteralExpr r1 = (IntLiteralExpr) r;
					rval = new BigDecimal(r1.getValue().getValue());

				}
				if (be instanceof MTimesExpr) {
					BigDecimal b = lval.multiply(rval);
					Expr a = new FPLiteralExpr(
							new FPNumericLiteralValue(String.valueOf(b
									.toString())));
					return a;
				} else if (be instanceof MDivExpr) {
					BigDecimal b = lval.divide(rval);
					Expr a = new FPLiteralExpr(
							new FPNumericLiteralValue(String.valueOf(b
									.toString())));
					return a;
				} else if (be instanceof MinusExpr) {
					BigDecimal b = lval.subtract(rval);
					Expr a = new FPLiteralExpr(
							new FPNumericLiteralValue(String.valueOf(b
									.toString())));
					return a;
				} else if (be instanceof PlusExpr) {
					BigDecimal b = lval.add(rval);
					Expr a = new FPLiteralExpr(
							new FPNumericLiteralValue(String.valueOf(b
									.toString())));
					return a;
				}
			} else {
				if (be instanceof MTimesExpr) {
					return new MTimesExpr((Expr) left, (Expr) right);
				} else if (be instanceof PlusExpr) {
					return new PlusExpr((Expr) left, (Expr) right);
				} else if (be instanceof MinusExpr) {
					return new MinusExpr((Expr) left, (Expr) right);
				} else if (be instanceof MDivExpr) {
					return new MDivExpr((Expr) left, (Expr) right);
				}
			}
		} else if (be instanceof LiteralExpr) {
			return be;
		} else if (be instanceof NameExpr) {
			// do checking
			System.out.println(be.getVarName());
			if (currentInSet.containsKey(be.getVarName())) {
				
				Set<LiteralExpr> val = currentInSet.get(be.getVarName());
				Iterator<LiteralExpr> itr = val.iterator();
				LiteralExpr l=itr.next();
				if(l==null)
					return be;
				else
				return l;
			}
		}
		return be;
	}
}
