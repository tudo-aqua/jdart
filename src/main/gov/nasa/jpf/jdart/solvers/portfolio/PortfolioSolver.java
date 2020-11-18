package gov.nasa.jpf.jdart.solvers.portfolio;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
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

  private List<ConstraintSolver> solvers;

  public PortfolioSolver(Properties properties) {
    solvers = resolveSolvers(properties);
  }

  @Override
  public Result solve(Expression<Boolean> expression, Valuation valuation) {
    List<Runnable> calls = new LinkedList<>();
    ExecutorService exec = Executors.newFixedThreadPool(solvers.size());
    ExecutorCompletionService ecs = new ExecutorCompletionService(exec);
    for(ConstraintSolver solver: solvers){
      ecs.submit(() -> {
        Valuation val = new Valuation();
        Result res = solver.solve(expression, val);
        return new SolvingResult(res, val);
      });
    }
    return processResult(solvers.size(), ecs, valuation, exec);
  }

  @Override
  public SolverContext createContext() {
    Map<String, String> env = System.getenv();
    List<SolverContext> ctxs = new LinkedList<>();
    for(ConstraintSolver s: solvers){
      ctxs.add(s.createContext());
    }
    return new PortfolioSolverContext(ctxs);
  }

  private List<ConstraintSolver> resolveSolvers(Properties properties){
    String specifiedSolvers = properties.getProperty("jdart.portfolio.solvers", "");
    LinkedList<ConstraintSolver> solvers = new LinkedList();
    ConstraintSolverFactory csf = ConstraintSolverFactory.getRootFactory();
    String[] solverNames = specifiedSolvers.split(",");
    for(String solverName: solverNames){
      solvers.push(new ProcessWrapperSolver(solverName));
    }
    return solvers;
  }

  public static Result processResult(int maxSolvers, ExecutorCompletionService ecs, Valuation valuation, ExecutorService exec){
    for(int i = 0; i < maxSolvers; i++){
      try {
        Future<SolvingResult> solverRes = ecs.take();
        if(solverRes.isDone() && !solverRes.get().getResult().equals(Result.DONT_KNOW)){
          if(solverRes.get().getResult().equals(Result.SAT)){
            for(ValuationEntry e: solverRes.get().getVal().entries()){
              valuation.addEntry(e);
            }
          }
          exec.shutdownNow();
          return solverRes.get().getResult();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
    exec.shutdown();
    return Result.DONT_KNOW;
  }
}
