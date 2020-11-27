package gov.nasa.jpf.jdart.solvers.bounded;

import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class BoundedSolverContext extends SolverContext {

  private final SolverContext ctx;
  private final int bound;
  private final int itr;
  private final Stack<StackElement> dkStack = new Stack<>();
  private StackElement current;
  private BoundedSolver solver;
  private OptimizationVisitor optimizier;

  public BoundedSolverContext(SolverContext ctx, int bound, int itr, BoundedSolver solver) {
    this.ctx = ctx;
    this.bound = bound;
    this.itr = itr;
    this.solver = solver;
    this.optimizier = new OptimizationVisitor();
  }

  @Override
  public void push() {
    ctx.push();
    dkStack.push(current);
    current = new StackElement();
  }

  @Override
  public void pop(int n) {
    for (int i = 0; i < n; i++) {
      current = dkStack.pop();
    }
    ctx.pop(n);
  }

  @Override
  public Result solve(Valuation vltn) {
    return solveWithBound(vltn);
  }

  @Override
  public void add(List<Expression<Boolean>> list) {
    for (Expression<Boolean> e : list) {
      ctx.add(e);
      current.exprsn.add(e);
    }
  }

  @Override
  public void dispose() {
    ctx.dispose();
  }

  private Result solveWithBound(Valuation vals) {

    Expression all = ExpressionUtil.and(current.exprsn);
    for (StackElement s : dkStack) {
      if (s != null) {
        all = ExpressionUtil.and(all, ExpressionUtil.and(s.exprsn));
      }
    }
    all = (Expression) all.accept(optimizier, false);
    Result res = null;
    for (int i = 1; i <= itr; i++) {
      Expression<Boolean> boundExpr = solver.getBound(all, i);
      ArrayList<Expression<Boolean>> bounds = new ArrayList<>();
      bounds.add(boundExpr);
      push();
      add(bounds);
      // System.out.println("Try to solve: " + all + " with bounds: " + bounds);
      res = ctx.solve(vals);
      // System.out.println("Result: " + res);
      pop();
      if (res == Result.SAT) {
        return res;
      }
    }
    // FIXME: This should be logger.finer
    // System.out.println("Try to solve: " + all + " without bounds");
    push();
    add(solver.getBoundAnalysisLimit(all));
    res = ctx.solve(vals);
    // FIXME: This should be logger.finer
    // System.out.println("Result: " + res);
    if (res.equals(Result.SAT)) {
      assert (Boolean) all.evaluate(vals);
    }
    pop();
    return res;
  }

  private static class StackElement {

    final ArrayList<Expression<Boolean>> exprsn = new ArrayList<>();
  }
}
