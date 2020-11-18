package gov.nasa.jpf.jdart.solvers.portfolio;

import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.solvers.encapsulation.SolvingResult;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PortfolioSolverContext extends SolverContext {
  private List<SolverContext> solvers;

  public PortfolioSolverContext(List<SolverContext> ctxs){
    this.solvers = ctxs;
  }

  @Override
  public void push() {
    for(SolverContext ctx: solvers){
      ctx.push();
    }
  }

  @Override
  public void pop(int i) {
    for(SolverContext ctx: solvers){
      ctx.pop(i);
    }
  }

  @Override
  public Result solve(Valuation valuation) {
    List<Runnable> calls = new LinkedList<>();
    ExecutorService exec = Executors.newFixedThreadPool(solvers.size());
    ExecutorCompletionService ecs = new ExecutorCompletionService(exec);
    for(SolverContext solver: solvers){
      ecs.submit(() -> {
        Valuation val = new Valuation();
        Result res = solver.solve(val);
        return new SolvingResult(res, val);
      });
    }
   return PortfolioSolver.processResult(solvers.size(), ecs, valuation, exec);
  }

  @Override
  public void add(List<Expression<Boolean>> list) {
    for(SolverContext ctx: solvers){
      ctx.add(list);
    }
  }

  @Override
  public void dispose() {
    for(SolverContext ctx: solvers){
      ctx.dispose();
    }
  }
}
