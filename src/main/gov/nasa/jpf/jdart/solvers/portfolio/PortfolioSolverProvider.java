package gov.nasa.jpf.jdart.solvers.portfolio;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverProvider;
import java.util.Properties;

public class PortfolioSolverProvider implements
    ConstraintSolverProvider {

  @Override
  public String[] getNames() {
    return new String[]{"portfolio", "Portfolio", "parallel", "multi"};
  }

  @Override
  public ConstraintSolver createSolver(Properties properties) {
    return new PortfolioSolver(properties);
  }
}
