package gov.nasa.jpf.jdart.solvers.portfolio;

import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.solvers.encapsulation.ProcessWrapperContext;
import gov.nasa.jpf.constraints.solvers.encapsulation.SolvingResult;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PortfolioSolverContext extends SolverContext {

  private List<ProcessWrapperContext> solvers;
  private List<SolverContext> direct;
  private ExecutorService exec;

  public PortfolioSolverContext(List<ProcessWrapperContext> ctxs, List<SolverContext> direct) {
    this.solvers = ctxs;
    this.direct = direct;
    this.exec = Executors.newFixedThreadPool(ctxs.size());
  }

  @Override
  public void push() {
    for (SolverContext ctx : solvers) {
      ctx.push();
    }
    for (SolverContext ctx : direct) {
      ctx.push();
    }
  }

  @Override
  public void pop(int i) {
    for (SolverContext ctx : solvers) {
      ctx.pop(i);
    }
    for (SolverContext ctx : direct) {
      ctx.pop(i);
    }
  }

  @Override
  public Result solve(Valuation valuation) {
    ProcessWrapperContext ctx = solvers.get(0);
    Expression expression = ctx.getCurrentExpression();
    StringOrFloatExpressionVisitor visitor = new StringOrFloatExpressionVisitor();
    boolean isStringOrFloatExpression = (Boolean) expression.accept(visitor, null);
    if (isStringOrFloatExpression) {
      List<SolverContext> wrappedCtxs = new LinkedList<>();
      for (ProcessWrapperContext solver : solvers) {
        if (solver.getName().equalsIgnoreCase("cvc4")) {
          wrappedCtxs.add(solver);
        }
      }
      return dispatcheProcessWrappedSolvers(expression, valuation, wrappedCtxs);
    } else {
      if (direct.size() == 1) {
        return direct.get(0).solve(valuation);
      }
    }
    throw new IllegalArgumentException("Cannot run the problem with the provided solvers");
  }

  public Result dispatcheProcessWrappedSolvers(Expression<Boolean> expression,
      Valuation valuation,
      List<SolverContext> solvers) {
    List<Runnable> calls = new LinkedList<>();
    ExecutorCompletionService ecs = new ExecutorCompletionService(exec);
    for (SolverContext solver : solvers) {
      ecs.submit(() -> {
        Valuation val = new Valuation();
        Result res = solver.solve(val);
        return new SolvingResult(res, val);
      });
    }
    return PortfolioSolver.processResult(solvers.size(), ecs, valuation);
  }

  @Override
  public void add(List<Expression<Boolean>> list) {
    for (SolverContext ctx : solvers) {
      ctx.add(list);
    }
    Expression expr = solvers.get(0).getCurrentExpression();
    StringOrFloatExpressionVisitor visitor = new StringOrFloatExpressionVisitor();
    if (!(Boolean) expr.accept(visitor, null)) {
      for (SolverContext ctx : direct) {
        ctx.add(list);
      }
    }
  }

  @Override
  public void dispose() {
    for (SolverContext ctx : solvers) {
      ctx.dispose();
    }
    for (SolverContext ctx : direct) {
      ctx.dispose();
    }
  }
}
