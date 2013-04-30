package vectorization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import nodecases.AbstractNodeCaseHandler;

import ast.ASTNode;
import ast.AssignStmt;
import ast.BinaryExpr;
import ast.ETimesExpr;
import ast.Expr;
import ast.ForStmt;
import ast.MDivExpr;
import ast.MTimesExpr;
import ast.MinusExpr;
import ast.NameExpr;
import ast.ParameterizedExpr;
import ast.PlusExpr;
import ast.RangeExpr;
import ast.Stmt;

/** This class checks for the self dependence of the statements **/

public class StatementSelfDependence extends AbstractNodeCaseHandler {
	
	//private Set<ForStmt> forStatments = new HashSet<>();
	private List<Integer> rangeLoop;
	
	public static void instrument(ASTNode<?> node) {
		node.analyze(new StatementSelfDependence());
	}

	@Override
		public void caseForStmt(ForStmt node) {
		  rangeLoop = null;
		  AssignStmt astm = node.getAssignStmt();
		  
		  // For getting the loop variable
		  String loopVar = astm.getVarName();
		  
		  ASTNode lChild = node.getChild(0);
		  RangeExpr range = (RangeExpr) lChild.getChild(1);
		  rangeLoop = rangeForLoop(range);

		  // getting all for-loop statements
		  ast.List<Stmt> stmts = node.getStmts();
		  
		  // For testing the statement self dependence
		  // The HashMap contains the uID of the statement along with a binary value
		  // 0 = The statement does not depend upon itself
		  // 1 = The statement in self-dependent.
		  HashMap<Long, Integer> dependenceHM = new HashMap<>();
		  dependenceHM = eachDependece(stmts, rangeLoop, loopVar);
		  // self-dependence test over
		  
		  // for getting information about statement output
		  // and values used in RHS
		  HashMap<Long, List<String>> stmtDHM = new HashMap<>();
		  stmtDHM = statementVariableAnalysis(stmts, loopVar);
		  
		  // considering only true dependence of the statements
		  // for ex:
		  // S1: X = ...
		  // S2: ... = X
		  // We say here that S2 depends on S1 as it used the value written in S1
		  // Also remembering that we should maintain the topological sort order of the statements
		  
		  // Creating a dependence graph of the statements
		  // Using HashMap for this
		  HashMap<Long, List<Long>> stmtDGHM = new LinkedHashMap<>();
		  stmtDGHM = statementDependence(stmtDHM);
		  // true dependence over
		  
		  // The true dependence information for each statement will be helpful in 
		  // deciding the order in which statements should be vectorized.
		  // Also need to maintain the topological order of the statement in the original program.
		  List<Long> executionOrder = new LinkedList();
		  
		  Iterator it = dependenceHM.keySet().iterator();
			while(it.hasNext()){
				Object o = it.next();
				if(stmtDGHM.containsKey(o)){
					if(!executionOrder.containsAll(stmtDGHM.get(o))){
						List<Long> ll = stmtDGHM.get(o);
						ll.removeAll(executionOrder);
						executionOrder.addAll(ll);
					}
					if(!executionOrder.contains(o))
						executionOrder.add((Long) o);
				}
				else{
					if(!executionOrder.contains(o))
						executionOrder.add((Long) o);
				}
			}
			//printNormalList(executionOrder);	
			
			vectorizingStatements(stmts, dependenceHM, range);
			
			//Since the execution order of the statement matters.
			//Re-ordering statements as the new execution order
			HashMap<Long, ASTNode> ll = reorderStatements(stmts);
			List<ASTNode> newOrder = new ArrayList();
			Iterator it1 = executionOrder.iterator();
			for(int i=executionOrder.size() -1; i>=0; i--){
				Long l = executionOrder.get(i);
				ASTNode s = ll.get(l);
				AstUtil.insertAfter(node, (ASTNode<?>) s);
			}
			
			//int nodeIndex = node.getParent().getIndexOfChild(node);
			//node.getParent().removeChild(nodeIndex);
		}
	
	/**
	 * 
	 * @param range
	 * @return The range of the for loop in a list format
	 */
	public List<Integer> rangeForLoop(RangeExpr range){
		List<Integer> r1 = new ArrayList<Integer>();
		
		try{
		String temp = range.getChild(0).getNodeString();
		r1.add(Integer.parseInt(temp));
		
		temp = range.getChild(2).getNodeString();
		r1.add(Integer.parseInt(temp));
		
		temp = range.getIncr().getNodeString();
		r1.add(Integer.parseInt(temp));
		}
		
		catch(Exception e){
			r1.add(1);
		}
		return r1;
	}
	
	/**
	 * 
	 * @param statements - takes the list of all the statements in for loop
	 * @param range - takes the range of the for loop
	 * @param loopVar - the iteration variable of loop
	 * @return - returns a list of integers whose size is equal to the number of statements in 
	 * for loop. The value 0 for a particular statement suggests it doesn't depend upon itself
	 * and the value 1 suggests the the statement is dependent on itself.
	 * 
	 */
	public HashMap<Long, Integer> eachDependece(ast.List<Stmt> statements, List<Integer> range, String loopVar){
		HashMap<Long, Integer> dependenceAnalysis = new LinkedHashMap<Long, Integer>();
		Iterator<Stmt> it = statements.iterator();
		
		while(it.hasNext()){
			Stmt statement = it.next();
			if(statement instanceof AssignStmt){
//				System.out.println(statement.getuID() + ": " + statement.getNodeString());
				if(singleDependence(statement, loopVar)){
					dependenceAnalysis.put(statement.getuID(), 1);
				}
				else{
					dependenceAnalysis.put(statement.getuID(), 0);
				}
			}
		}
			
		return dependenceAnalysis;
	}
	
	/**
	 * @param statement
	 * @param loopVar
	 * @return
	 */
	public Boolean singleDependence(Stmt statement, String loopVar){
			List<ListWrapper> stmtData = new ArrayList<ListWrapper>();
			
			// For saving the data on left side of the statement
			if(statement.getChild(0).getNumChild() > 1){
				ast.List ls = (ast.List) statement.getChild(0).getChild(1);
				Expr exp = (Expr) ls.getChild(0);

				//System.out.println(exp.getChild(0).getClass());
				if(exp.getNumChild()>1){
					if(exp.getChild(0).getClass().toString().endsWith("MTimesExpr")){
						ListWrapper lw = new ListWrapper(
									statement.getChild(0).getVarName(),
									exp.getChild(0).getChild(1).getVarName(), 
									Integer.parseInt(exp.getChild(1).getNodeString()),
									Integer.parseInt(exp.getChild(0).getChild(0).getNodeString())
								);
						stmtData.add(lw);
					}
					else{
						if(exp.getChild(0).getClass().toString().endsWith("IntLiteralExpr")){
							ListWrapper lw = new ListWrapper(
									statement.getChild(0).getVarName(),
									exp.getChild(1).getVarName(), 
									0,
									Integer.parseInt(exp.getChild(0).getNodeString())
								);
							stmtData.add(lw);
						}
						else{
							ListWrapper lw = new ListWrapper(
									statement.getChild(0).getVarName(),
									exp.getChild(0).getNodeString(), 
									Integer.parseInt(exp.getChild(1).getNodeString()),
									1
								);
							stmtData.add(lw);
						}
					}
				}
				else{
					if(exp.getChild(0).getClass().toString().endsWith("MTimesExpr")){
						ListWrapper lw = new ListWrapper(
								statement.getChild(0).getVarName(),
								exp.getChild(0).getNodeString(), 
								0,
								Integer.parseInt(exp.getChild(0).getChild(0).getNodeString())
							);
						stmtData.add(lw);
					}
					else{
							ListWrapper lw = new ListWrapper(
									statement.getChild(0).getVarName(),
									exp.getChild(0).getNodeString(), 
									0,
									1
								);
							stmtData.add(lw);
					}
				}
				//System.out.println(exp.getChild(0).getNodeString());
				//System.out.println(exp.getChild(1).getNodeString());
			}
			
			//printList(stmtData);			
			// For saving the data on right side of the statement
			// Here we only care about the expression which involve the variable 
			// present on the left hand side of the statement
			
			if(statement.getChild(1).getNumChild() > 1){
				//ast.List ls = (ast.List) statement.getChild(1).getChild(1);
				//Expr exp = (Expr) ls.getChild(0);	
				Integer noOfChild = statement.getChild(1).getNumChild();
				ASTNode rChild = statement.getChild(1);
				for(int i=0; i < noOfChild; i++){
					String temp1 = stmtData.get(0).getFirstStringValue();
					String temp2 = rChild.getChild(i).getVarName();
					if(temp1.equals(temp2)){
						if(rChild.getChild(i).getNumChild() > 1){
							ast.List ls = (ast.List) rChild.getChild(0).getChild(1);
							Expr exp = (Expr) ls.getChild(0);
							if(exp.getChild(0).getClass().toString().endsWith("MTimesExpr")){
								ListWrapper lw = new ListWrapper(
											statement.getChild(0).getVarName(),
											exp.getChild(0).getChild(1).getNodeString(), 
											Integer.parseInt(exp.getChild(1).getNodeString()),
											Integer.parseInt(exp.getChild(0).getChild(0).getNodeString())
										);
								stmtData.add(lw);
							}
							else{
								if(exp.getChild(0).getClass().toString().endsWith("IntLiteralExpr")){
									ListWrapper lw = new ListWrapper(
											statement.getChild(0).getVarName(),
											exp.getChild(1).getVarName(), 
											0,
											Integer.parseInt(exp.getChild(0).getNodeString())
										);
									stmtData.add(lw);
								}
								else{
									if(exp.getChild(0).getClass().toString().endsWith("NameExpr"))
									{
										ListWrapper lw = new ListWrapper(
												statement.getChild(0).getVarName(),
												exp.getChild(0).getNodeString(), 
												0,
												Integer.parseInt(exp.getChild(1).getNodeString())
											);
										stmtData.add(lw);										
									}
									else{
										
										ListWrapper lw = new ListWrapper(
												statement.getChild(0).getVarName(),
												exp.getChild(1).getNodeString(), 
												0,
												Integer.parseInt(exp.getChild(0).getNodeString())
											);
										stmtData.add(lw);
									}
								}
							}
						}
						else{
							System.out.println("Empty Condition");
						}
					}
				}
				//printList(stmtData);
			}
			
			
			// Finding GCD of the two number on which loop iteration is happening
			// a[i+11] = a[i+2] + 10;
			// gcd(11,2)
			if(stmtData.size() > 1){
				int gcdValue = findGCD(stmtData.get(0).getIntegerValue(), stmtData.get(1).getIntegerValue());
				int comparisonValue = Math.abs(stmtData.get(0).getLoopIndexValue() - stmtData.get(1).getLoopIndexValue());
				if(gcdValue!=0 && comparisonValue % gcdValue ==0)
					return Boolean.TRUE;
			}
			else
				return Boolean.FALSE;
			return Boolean.FALSE;
	}
	
	public HashMap<Long, List<String>> statementVariableAnalysis(ast.List<Stmt> statements, String loopVar){
		HashMap<Long, List<String>> stmtInfo = new HashMap<>();
		Iterator<Stmt> it = statements.iterator();
		
		while(it.hasNext()){
			Stmt statement = it.next();
			if(statement instanceof AssignStmt){
				List<String> ls = new ArrayList<>();
				ls.add(statement.getChild(0).getVarName());
				Expr exp = ((AssignStmt) statement).getRHS();
				if(exp.getNumChild() > 0){
					if(exp.getNumChild() == 1){
						ls.add(exp.getVarName());
					}
					else{
						List<String> varOuts = traverse(exp, loopVar);
						ls.addAll(varOuts);
					}
				}
				stmtInfo.put(statement.getuID(), ls);
			}
		}
		return stmtInfo;
	}
	
	private static List<String> traverse(ASTNode root, String loop){
		Set s = root.getAllNameExpressions();
		Iterator it = s.iterator();
		List<String> ls = new ArrayList<>();
		while(it.hasNext()){
			NameExpr nameExp = (NameExpr) it.next();
			ls.add(nameExp.getNodeString());
		}
		
		List<String> temp = new ArrayList<>();
		for (int i = 1; i < ls.size(); i++) {
			if(ls.get(i).equals(loop)) {
				temp.add(ls.get(i-1));
			}
		}
		return temp;
	}
	
	private static HashMap<Long, List<Long>> statementDependence(HashMap<Long, List<String>> varMap){
		HashMap<Long, List<Long>> stmtMap = new HashMap();
		Iterator it = varMap.keySet().iterator();
		List<String> tList = new ArrayList<>();
		while(it.hasNext()){
			Object o = it.next();
			//System.out.println(o.toString());
			tList = varMap.get(o);
			String inputVar = tList.get(0);
			stmtMap = stmtListDependence(varMap, o.toString(), inputVar, stmtMap);
		}
		
		/*it = stmtMap.keySet().iterator();
		while(it.hasNext()){
			Object o = it.next();
			List<Long> ls = stmtMap.get(o);
			System.out.println(o.toString() + " :: ");
			for (int i = 0; i < ls.size(); i++) {
				System.out.println(ls.get(i));
			}
			System.out.println("####");
		}*/
		
		return stmtMap;
	}

	private static HashMap<Long, List<Long>> stmtListDependence(
												HashMap<Long, List<String>> hm, 
												String currentKey, 
												String inputCom,
												HashMap<Long, List<Long>> outHM){	
		Iterator it = hm.keySet().iterator();
		List<String> tList = new ArrayList<>();
		while(it.hasNext()){
			Object o = it.next();
			if(!currentKey.equals(o.toString())) {
				tList = hm.get(o);
				if(tList.subList(1, tList.size()).contains(inputCom)){
					
					if(outHM.containsKey(o)){
						List<Long> ll = outHM.get(o);
						ll.add(Long.parseLong(currentKey));
						outHM.put((Long) o, ll);
					}
					else{
						List<Long> ll = new ArrayList<>();
						ll.add(Long.parseLong(currentKey));
						outHM.put((Long) o, ll);
					}
				}
			}
		}
		return outHM;
	} 
	
	public void vectorizingStatements(ast.List<Stmt> forStatements, 
									  HashMap<Long, Integer> dependenceHM,
									  RangeExpr r){
		
		Iterator<Stmt> it1 = forStatements.iterator();
		while(it1.hasNext()){
			Stmt statement = it1.next();
			if(statement instanceof AssignStmt){
				if(dependenceHM.get(statement.getuID()) != 1){// checking if statement is independent
					if(!statement.getChild(1).getClass().toString().endsWith("LiteralExpr")){
						ASTNode newNode = replaceExpression((Expr) statement.getChild(1), r);
						statement.removeChild(1);
						statement.insertChild(newNode, 1);
					}
					if(!statement.getChild(0).getClass().toString().endsWith("LiteralExpr")){
						ASTNode newNode = replaceExpression((Expr) statement.getChild(0), r);
						statement.removeChild(0);
						statement.insertChild(newNode, 0);
					}
				}
			}
		}
	}
	
	public HashMap<Long, ASTNode> reorderStatements(ast.List<Stmt> forStatements){
		HashMap<Long, ASTNode> execOrderHM = new HashMap<>();
		
		Iterator<Stmt> it1 = forStatements.iterator();
		while(it1.hasNext()){
			ASTNode statement = it1.next();
			execOrderHM.put(statement.getuID(), statement);
		}
		return execOrderHM;		
	}
	
	 /**
     * Function for replacing expression with removing indexes with range
     * 
     * @param child : RHS of the expression as the parameter
     * @param r : loop range
     * @return The modified tree node
     */
    public Expr replaceExpression(Expr child, RangeExpr r) {
            Expr astnode = child;

            if (child instanceof BinaryExpr) {
                    if (child instanceof PlusExpr) {
                            astnode = new PlusExpr(replaceExpression(
                                            (Expr) child.getChild(0), r), replaceExpression(
                                            (Expr) child.getChild(1), r));
                    } else if (child instanceof MTimesExpr) {
                            astnode = new MTimesExpr(replaceExpression(
                                            (Expr) child.getChild(0), r), replaceExpression(
                                            (Expr) child.getChild(1), r));
                    } else if (child instanceof MinusExpr) {
                            astnode = new MinusExpr(replaceExpression(
                                            (Expr) child.getChild(0), r), replaceExpression(
                                            (Expr) child.getChild(1), r));
                    } else if (child instanceof MDivExpr) {
                            astnode = new MDivExpr(replaceExpression(
                                            (Expr) child.getChild(0), r), replaceExpression(
                                            (Expr) child.getChild(1), r));
                    } else if (child instanceof ETimesExpr) {
                            astnode = new ETimesExpr(replaceExpression(
                                            (Expr) child.getChild(0), r), replaceExpression(
                                            (Expr) child.getChild(1), r));
                    }
            } else if (child instanceof ParameterizedExpr) {
                    NameExpr n = (NameExpr) child.getChild(0);
                    // do here

                    Expr indexVar = (Expr) ((ast.List) child.getChild(1)).getChild(0) ;
                    Expr newNode;
                    if(indexVar instanceof NameExpr){
                    	astnode = Asts.functionCall(n.getVarName(), r);
                    }
                    else if(indexVar instanceof BinaryExpr){
                    	indexVar.removeChild(0);
                    	indexVar.insertChild(r, 0);
                    	astnode = Asts.functionCall(n.getVarName(), indexVar);
                    }
            }
            return astnode;
    }
	
	
	private static int findGCD(int number1, int number2) {
        //base case
        if(number2 == 0){
            return number1;
        }
        return findGCD(number2, number1%number2);
    }
	

	@Override
	public void caseASTNode(ASTNode node) {
		for (int i = 0; i < node.getNumChild(); ++i) {
			node.getChild(i).analyze(this);
		}
	}
	
	
	public void printNormalList(List<?> ls){
		for (Iterator iterator = ls.iterator(); iterator.hasNext();) {
			Object o = (Object) iterator.next();
			System.out.println(o.toString());
		}
	}
	
	public void printList(List<ListWrapper> ls){
		System.out.println(ls.size());
		for (int i = 0; i < ls.size(); i++) {
			System.out.println(ls.get(i).getFirstStringValue() + "  " +
							 ls.get(i).getSecondStringValue()+ "  " +
							 ls.get(i).getIntegerValue()+ "  " +
							 ls.get(i).getLoopIndexValue());
		}
	}
	
	public void printHashMap(HashMap<Long, Integer> hm){
		System.out.println(hm.size());
		Iterator it = hm.keySet().iterator();
		while(it.hasNext()){
			Object o = it.next();
			System.out.print(o.toString() + " :  ");
			System.out.println(hm.get(o));
		}
	}
}
