package gov.nasa.jpf.jdart.solvers.portfolio;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.solvers.encapsulation.ProcessWrapperContext;
import gov.nasa.jpf.constraints.solvers.encapsulation.ProcessWrapperSolver;
import gov.nasa.jpf.constraints.solvers.encapsulation.SolvingResult;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PortfolioSolver extends ConstraintSolver {

  private List<ConstraintSolver> processWrappedSolvers;
  private List<ConstraintSolver> directSolvers;
  private ExecutorService exec;


  public PortfolioSolver(Properties properties) {
    processWrappedSolvers = new LinkedList();
    directSolvers = new LinkedList<>();
    resolveSolvers(properties);
    exec = Executors.newFixedThreadPool(processWrappedSolvers.size());
  }

  @Override
  public Result solve(Expression<Boolean> expression, Valuation valuation) {
    StringOrFloatExpressionVisitor visitor = new StringOrFloatExpressionVisitor();
    boolean isStringOrFloatExpression = expression.accept(visitor, null);
    if (isStringOrFloatExpression) {
      List<ConstraintSolver> solvers = new LinkedList<>();
      for (ConstraintSolver solver : processWrappedSolvers) {
        if (solver.getName().equalsIgnoreCase("cvc4")) {
          solvers.add(solver);
        }
      }
      return dispatcheProcessWrappedSolvers(expression, valuation, solvers);
    } else {
      for (ConstraintSolver solver : directSolvers) {
        if (solver.getName().equalsIgnoreCase("z3")) {
          return solver.solve(expression, valuation);
        }
      }
    }
    throw new IllegalArgumentException("Cannot run the problem with the provided solvers");
  }


  @Override
  public SolverContext createContext() {
    Map<String, String> env = System.getenv();
    List<ProcessWrapperContext> ctxs = new LinkedList<>();
    for (ConstraintSolver s : processWrappedSolvers) {
      ctxs.add((ProcessWrapperContext) s.createContext());
    }
    List<SolverContext> direct = new LinkedList<>();
    for (ConstraintSolver s : directSolvers) {
      direct.add(s.createContext());
    }
    return new PortfolioSolverContext(ctxs, direct);
  }

  private Result dispatcheProcessWrappedSolvers(Expression<Boolean> expression,
      Valuation valuation,
      List<ConstraintSolver> solvers) {
    List<Runnable> calls = new LinkedList<>();
    ExecutorCompletionService ecs = new ExecutorCompletionService(exec);
    for (ConstraintSolver solver : solvers) {
      ecs.submit(() -> {
        Valuation val = new Valuation();
        Result res = solver.solve(expression, val);
        return new SolvingResult(res, val);
      });
    }
    return processResult(solvers.size(), ecs, valuation);
  }

  private void resolveSolvers(Properties properties) {
    String specifiedSolvers = properties.getProperty("jdart.portfolio.solvers", "");
    ConstraintSolverFactory csf = ConstraintSolverFactory.getRootFactory();
    String[] solverNames = specifiedSolvers.split(",");
    for (String solverName : solverNames) {
      processWrappedSolvers.add(new ProcessWrapperSolver(solverName));
      if (!solverName.equalsIgnoreCase("cvc4")) {
        directSolvers.add(csf.createSolver(solverName));
      }
    }
  }

  public static Result processResult(int maxSolvers, ExecutorCompletionService ecs,
      Valuation valuation) {
    for (int i = 0; i < maxSolvers; i++) {
      try {
        Future<SolvingResult> solverRes = ecs.take();
        if (solverRes.isDone() && !solverRes.get().getResult().equals(Result.DONT_KNOW)) {
          if (solverRes.get().getResult().equals(Result.SAT)) {
            for (ValuationEntry e : solverRes.get().getVal().entries()) {
              valuation.addEntry(e);
            }
          }
          return solverRes.get().getResult();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
    return Result.DONT_KNOW;
  }
}
