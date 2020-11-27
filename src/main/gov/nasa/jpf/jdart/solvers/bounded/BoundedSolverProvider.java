package gov.nasa.jpf.jdart.solvers.bounded;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverProvider;
import java.util.Properties;

public class BoundedSolverProvider implements ConstraintSolverProvider {

	@Override
	public String[] getNames() {
		return new String[]{"bounded"};
	}

	@Override
	public ConstraintSolver createSolver(Properties config) {
		String dp = "dontknow";
		if (config.containsKey("bounded.dp")) {
			dp = config.getProperty("bounded.dp");
		}

		int bound = 200;
		int iter = 1;

		BoundedSolver.BoundType type = BoundedSolver.BoundType.linear;

		if (config.containsKey("bounded.bound")) {
			bound = Integer.parseInt(config.getProperty("bounded.bound"));
		}
		if (config.containsKey("bounded.iter")) {
			iter = Integer.parseInt(config.getProperty("bounded.iter"));
		}
		if (config.containsKey("bounded.type") && config.getProperty("bounded.type")
				.equals("fibonacci")) {
			type = BoundedSolver.BoundType.fibonacci;
		}
		if (config.containsKey("bounded.type") && config.getProperty("bounded.type")
				.equals("skipped")) {
			type = BoundedSolver.BoundType.fibonacci;
		}

		ConstraintSolver solver = ConstraintSolverFactory.getRootFactory().createSolver(dp, config);
		return new BoundedSolver(solver, bound, iter, type);
	}
}
