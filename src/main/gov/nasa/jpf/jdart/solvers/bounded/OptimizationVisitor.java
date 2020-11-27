package gov.nasa.jpf.jdart.solvers.bounded;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.BitvectorExpression;
import gov.nasa.jpf.constraints.expressions.BitvectorOperator;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class OptimizationVisitor extends DuplicatingVisitor<Boolean> {

  @Override
  public <E> Expression<?> visit(NumericCompound<E> n, Boolean replace) {
    if (replace && n.getOperator().equals(NumericOperator.REM)) {
      Expression r = n.getRight();
      Expression l = n.getLeft();
      if (r instanceof Constant && l instanceof Variable) {
        Variable x = (Variable) l;
        Constant check = (Constant) r;
        if (r.getType().equals(BuiltinTypes.SINT32) && (((Constant<Integer>) r).getValue() == 2)) {
          return BitvectorExpression.create(
              x, BitvectorOperator.AND, Constant.create(BuiltinTypes.SINT32, 1));
        }
      }
    }
    return n;
  }

  @Override
  public Expression<?> visit(NumericBooleanExpression n, Boolean data) {
    Expression duplicate = n;
    if (n.getComparator().equals(NumericComparator.EQ)
        || n.getComparator().equals(NumericComparator.NE)) {
      Expression right = n.getRight();
      Expression left = n.getLeft();
      if (right instanceof Constant) {
        Constant cR = (Constant) right;
        if (cR.getType().equals(BuiltinTypes.SINT32)
            && ((int) cR.getValue() == 0 || (int) cR.getValue() == 1)) {
          left = visit(left, true);
          return NumericBooleanExpression.create(left, n.getComparator(), right);
        }
      }
    }
    return duplicate;
  }
}
