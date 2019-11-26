package gov.nasa.jpf.jdart.solvers.bounded;

import gov.nasa.jpf.constraints.api.*;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

public class BoundedSolver extends ConstraintSolver {

    private final ConstraintSolver back;

    private final int itr;

    private final int bound;

    public BoundedSolver(ConstraintSolver back, int bound, int itr) {
        this.back = back;
        this.bound = bound;
        this.itr = itr;
    }

    @Override
    public Result solve(Expression<Boolean> exprsn, Valuation vltn) {

        Result res = null;
        for (int i=1; i<=itr; i++) {
            res = back.solve( ExpressionUtil.and(exprsn, getBound(exprsn, i * bound)), vltn);
            if (res == Result.SAT) {
                return res;
            }
        }
        return back.solve(exprsn, vltn);
    }

    @Override
    public BoundedSolverContext createContext() {
        SolverContext ctx = back.createContext();
        return new BoundedSolverContext(ctx, bound, itr);
    }


    static Expression<Boolean> getBound(Expression<Boolean> e, int bound) {

        Constant low = new Constant(BuiltinTypes.SINT32, -bound);
        Constant high = new Constant(BuiltinTypes.SINT32, bound);
        Expression<Boolean> ret = ExpressionUtil.TRUE;
        for (Variable v : ExpressionUtil.freeVariables(e)) {
            if (v.getType().equals(BuiltinTypes.SINT32)) {
                Expression<Boolean> lower = new NumericBooleanExpression(low, NumericComparator.LE, v);
                Expression<Boolean> upper = new NumericBooleanExpression(v, NumericComparator.LE, high);
                ret = ExpressionUtil.and(ret, lower, upper);
            }
         }
        return ret;
    }
}