package natlab.cs621.instrumentation;

import ast.ASTNode;

// A couple of helpers to insert an AST node into a tree.
public class AstUtil {
  private static void insert(ASTNode<?> node, ASTNode<?> newNode, int offset) {
    ASTNode<?> parent = node.getParent();
    int index = parent.getIndexOfChild(node);
    parent.insertChild(newNode, index + offset);
  }
  
  public static void insertBefore(ASTNode<?> node, ASTNode<?> newNode) {
    insert(node, newNode, 0);
  }
  
  public static void insertAfter(ASTNode<?> node, ASTNode<?> newNode) {
    insert(node, newNode, 1);
  }
}
