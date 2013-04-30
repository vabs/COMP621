package vectorization;

import natlab.DecIntNumericLiteralValue;
import ast.AssignStmt;
import ast.Expr;
import ast.ExprStmt;
import ast.IntLiteralExpr;
import ast.List;
import ast.MatrixExpr;
import ast.Name;
import ast.NameExpr;
import ast.ParameterizedExpr;
import ast.PlusExpr;
import ast.Row;
import ast.Stmt;
import ast.StringLiteralExpr;

// This class is used by ProfileAssignments to create some ASTs corresponding
// to MATLAB statements, which will be inserted into the program.
public class Asts {  
  // counter = 0;
  public static AssignStmt init(String scope) {
    return new AssignStmt(new NameExpr(new Name(scope + "_counter")), integer(0));
  }

  // counter = counter + 1;
  public static AssignStmt increment(String scope) {
    return new AssignStmt(
        new NameExpr(new Name(scope + "_counter")),
        new PlusExpr(new NameExpr(new Name(scope + "_counter")), integer(1)));
  }

  //  disp([Total # of runtime assignments made by scope: ', num2str(counter)]);
  public static ExprStmt display(String scope) {
    return new ExprStmt(functionCall("disp", row(
        string("Total # of runtime assignments made by " + scope + ": "),
        functionCall("num2str", new NameExpr(new Name(scope + "_counter"))))));
  }
  
  // Helpers, because manual AST construction can be very verbose
  
  // construct a function call with one argument
  static Expr functionCall(String name, Expr arg){
	  List<Expr> arguments = new List<Expr>();
	  arguments.add(arg);
	  return new ParameterizedExpr(new NameExpr(new Name(name)), arguments);
  }
  
  
  private static Expr functionCall(String name, List<Expr> args) {
    return new ParameterizedExpr(new NameExpr(new Name(name)), args);
  }
  
  // convert a Java string to an AST string literal node
  private static Expr string(String s) {
    return new StringLiteralExpr(s);
  }
  
  // convert a Java int to an AST integer literal node
  private static Expr integer(int i) {
    return new IntLiteralExpr(new DecIntNumericLiteralValue(String.valueOf(i)));
  }
  
  // construct a one-row matrix out of the given expressions
  private static Expr row(Expr... exprs) {
    MatrixExpr matrix = new MatrixExpr();
    Row row = new Row();
    for (Expr expr : exprs) {
      row.addElement(expr);
    }
    matrix.addRow(row);
    return matrix;
  }
  
  public static Stmt display(int i, IntLiteralExpr range1, IntLiteralExpr range2,NameExpr varname) {
		List<Expr> arguments = new List<Expr>();
	    arguments.add(range2);
	    arguments.add(range1);
	    if(i==0){
		return new AssignStmt(varname,
				functionCall("zeros",arguments));
	    }
	    else if(i==1){
	    	return new AssignStmt(varname,
	    			functionCall("ones",arguments));
	    }
		return null;
	}
  
  // Note: an alternative would be to parse the little snippets you need. Maybe useful
  // exercise: write a method ASTNode<?> parse(String code) that would let you write the
  // previous three methods more plainly, e.g.
  // init: return parse("counter = 0;")
  // increment: return parse("counter = counter + 1;")
  // What the advantages / disadvantages of parsing vs manual construction?
}
