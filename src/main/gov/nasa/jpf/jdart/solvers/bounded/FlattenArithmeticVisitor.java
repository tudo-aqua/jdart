package gov.nasa.jpf.jdart.solvers.bounded;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class FlattenArithmeticVisitor extends DuplicatingVisitor<Void> {

  @Override
  public Expression visit(NumericCompound n, Void data) {
    Expression left = visit(n.getLeft(), data);
    Expression right = visit(n.getRight(), data);
    try {
      if (left instanceof NumericCompound
          && right instanceof Constant
          && left.getType().equals(BuiltinTypes.SINT32)) {
        NumericCompound leftChild = (NumericCompound) left;
        if (leftChild.getLeft() instanceof Variable && leftChild.getRight() instanceof Constant) {
          Variable v = (Variable) leftChild.getLeft();
          Constant c = (Constant) leftChild.getRight();
          int cNew = apply(0, (int) c.getValue(), leftChild.getOperator());
          cNew = apply(cNew, ((Constant<Integer>) right).getValue(), n.getOperator());
          Constant newC = Constant.create(BuiltinTypes.SINT32, Math.abs(cNew));
          if(cNew > 0){
            return NumericCompound.create(v, NumericOperator.PLUS, newC);
          }else if(cNew < 0){
            return NumericCompound.create(v, NumericOperator.MINUS, newC);
          }else{
            return v;
          }
        }
      }
    } catch (IllegalArgumentException e) {
    }
    return n;
  }

  public int apply(int newRes, int constant, NumericOperator cmp) {
    if (cmp.equals(NumericOperator.PLUS)) {
      return newRes + constant;
    } else if (cmp.equals(NumericOperator.MINUS)) {
      return newRes - constant;
    }
    throw new IllegalArgumentException("Cannot be applied for Operator: " + cmp);
  }

  protected <E> Expression<?> defaultVisit(Expression<E> expression, Void data) {
    Expression<?>[] children = expression.getChildren();
    if(children != null){
      boolean changed = false;

      for(int i = 0; i < children.length; ++i) {
        Expression<?> c = children[i];
        if (c != null) {
          Expression<?> r = (Expression) this.visit(c, data);
          if (c != r) {
            changed = true;
          }

          children[i] = r;
        }
      }

      if (!changed) {
        return expression;
      } else {
        return expression.duplicate(children);
      }
    }
    return expression;
  }

}
