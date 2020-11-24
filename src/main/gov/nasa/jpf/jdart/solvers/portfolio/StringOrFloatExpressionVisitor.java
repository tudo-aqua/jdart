package gov.nasa.jpf.jdart.solvers.portfolio;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor;
import gov.nasa.jpf.constraints.expressions.CastExpression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.types.BuiltinTypes;

public class StringOrFloatExpressionVisitor extends AbstractExpressionVisitor<Boolean, Void> {

  @Override
  public <E> Boolean visit(Variable<E> v, Void data) {
    return v.getType().equals(BuiltinTypes.STRING) || v.getType().equals(BuiltinTypes.FLOAT) || v
        .getType().equals(BuiltinTypes.DOUBLE);
  }

  @Override
  public <E> Boolean visit(Constant<E> c, Void data) {
    return c.getType().equals(BuiltinTypes.STRING) || c.getType().equals(BuiltinTypes.FLOAT) || c
        .getType().equals(BuiltinTypes.DOUBLE);
  }

  @Override
  public <F, E> Boolean visit(CastExpression<F, E> cast, Void data) {
    return true;
  }

  @Override
  protected <E> Boolean defaultVisit(Expression<E> expression, Void data) {
    boolean res = false;
    for (Expression child : expression.getChildren()) {
      res = res || visit(child);
    }
    return res;
  }
}
