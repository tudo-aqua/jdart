/*
 * Copyright (C) 2015, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Modifications Copyright 2019 TU Dortmund, Falk Howar (@fhowar)
 *
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package gov.nasa.jpf.jdart.constraints.tree.internal;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.jdart.config.AnalysisConfig;
import gov.nasa.jpf.jdart.config.ConcolicConfig;
import gov.nasa.jpf.jdart.config.ConcolicValues;
import gov.nasa.jpf.jdart.constraints.paths.PathResult;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Instruction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static gov.nasa.jpf.jdart.config.ConcolicConfig.ExplorationStrategy.DFS;
import static gov.nasa.jpf.jdart.config.ConcolicConfig.ExplorationStrategy.IN_ORDER;

public class InternalConstraintsTree {

	static boolean DEBUG = true;

	public enum BranchEffect {
		NORMAL, UNEXPECTED, BUGGY
	}

	private final JPFLogger logger = JPF.getLogger("jdart");

	/**
	 * root of the tree
	 */
	private Node root = LeafNode.open(null, -1);

	/**
	 * This is the current node in our EXPLORATION
	 */
	private Node current = root;

	/**
	 * required for setting initial valuation after execution
	 */
	private LeafNode initialTarget = null;

	/**
	 * This is the node the valuation computed by the constraint solver SHOULD reach
	 */
	private LeafNode currentTarget = (LeafNode) root;

	/**
	 * last explored valuation
	 */
	private Valuation currentValues = null;

	/**
	 * analysis config
	 */
	private final AnalysisConfig anaConf;

	/**
	 * expected path of current execution
	 */
	private List<Integer> expectedPath = new ArrayList<>();

	/**
	 * status flag: execution diverged from expected path
	 */
	private boolean diverged = false;

	/**
	 * constraint solver context for incremental solving
	 */
	private final SolverContext solverCtx;

	/**
	 * internal mode switch: explore open nodes
	 */
	private boolean exploreMode;

	/**
	 * valuations for replay
	 */
	private final ConcolicValues replayValues;

	/**
	 * internal mode switch: replay given valuations
	 */
	private boolean replayMode = true;

	/**
	 * use incremental solving
	 */
	private boolean incremental = false;

	/**
	 * exploration strategy
	 */
	private ExplorationStrategy strategy = new DFSExplorationStrategy();

	InternalConstraintsTree(SolverContext solverCtx, AnalysisConfig anaConf) {
		this(solverCtx, anaConf, null, DFS, false);
	}

	public InternalConstraintsTree(SolverContext solverCtx, AnalysisConfig anaConf, ConcolicValues replayValues,
								   ConcolicConfig.ExplorationStrategy strategy, boolean incremental) {
		this.solverCtx = solverCtx;
		this.anaConf = anaConf;
		this.exploreMode = anaConf.isExploreInitially();
		this.replayValues = replayValues;

		this.incremental = incremental;

		System.out.println("Exploration Strategy: " + strategy);
		switch (strategy) {
			case BFS:
				this.strategy = new BFSExplorationStrategy();
			case IN_ORDER:
				this.strategy = new InOrderExplorationStrategy();
			case DFS:
			default:
				// already defaults to DFS
		}

		solverCtx.push();
	}

	public void setExplore(boolean explore) {
		this.exploreMode = explore;
	}

	public boolean needsDecision() {
		return DEBUG;
	}

	/**
	 * Retrieves the node in the constraints tree that should be reached using the given valuation.
	 *
	 * @param values the valuation
	 * @return the node in the tree that would be reached by the given valuation
	 */
	public LeafNode simulate(Valuation values) {
		Node curr = root;
		while (curr.isDecisionNode()) {

			DecisionNode dd = (DecisionNode) curr;
			int branchIdx = -1;
			try {
				branchIdx = dd.evaluate(values);
			}
			catch (RuntimeException ex) {
				// e.g. due to function with undefined semantics
				return null;
			}

			if (branchIdx < 0) {
				throw new IllegalStateException("Non-complete set of constraints at constraints tree node!");
			}

			curr = dd.getChild(branchIdx);
		}
		return (LeafNode) curr;
	}

	/**
	 *
	 * Once we diverge, we fail our current target and continue until
	 * we hit a leaf that we do not want to explore
	 * (anything other than on open, diverged, or d/k
	 *
	 * @param insn
	 * @param branchIdx
	 * @param decisions
	 * @return
	 */
	public BranchEffect decision(Instruction insn, int branchIdx, Expression<Boolean>[] decisions) {

		System.out.println("dec: " + Arrays.toString(decisions) + " : " + branchIdx);

		// 1. at decision node => move down in the tree, check that decision is
		//    expected, and check that decision matches bc decision
		if (current.isDecisionNode()) {

			// move down
			DecisionNode dn = (DecisionNode) current;
			current = dn.getChild(branchIdx);

			// if still on track
			if (!replayMode && !diverged) {
				// diverging now?
				// check if still in expected path
				int depth = dn.depth();
				int expectedBranch = expectedPath.get(depth).intValue();
				if (expectedBranch != branchIdx) {
					diverged = true;
					// returning unexpected will fail the current target
					// i.e., mark current target as dont_know
					return BranchEffect.UNEXPECTED;
				}

				// check decision
				String error = dn.validateDecision(insn, decisions);
				if (error != null) {
					// FIXME: maybe we should terminate jdart in this case?
					logger.severe("LIKELY A SEVERE BUG IN JDART:" + error);
					// returning inconclusively will fail the current
					// target and stop executing this path
					return BranchEffect.BUGGY;
				}

			}
		}
		// 2. at a leaf => expand tree by decision if node is not exhausted
		else {
			LeafNode leaf = (LeafNode) current;
			if (leaf.isExhausted()) {
				// FIXME: maybe we should terminate jdart in this case?
				logger.severe("LIKELY A SEVERE BUG IN JDART: decision at exhausted leaf: + " + leaf);
				// returning inconclusively will fail the current
				// target and stop executing this path
				return BranchEffect.BUGGY;
			}

			// expand tree ...
			if (anaConf.maxDepthExceeded(current.depth())) {
				System.out.println("max depth exceeded ...");
				leaf.setComplete(false);
			}
			else {
				current = expand(leaf, insn, branchIdx, decisions);
			}
		}

		// already diverged or everything fine or at leaf: return normally
		return BranchEffect.NORMAL;
	}

	private Node expand(LeafNode leaf, Instruction insn, int branchIdx, Expression<Boolean>[] decisions) {
		DecisionNode parent = leaf.parent();
		DecisionNode newInner = new DecisionNode(parent, insn, decisions, leaf.childId(),
				exploreMode, strategy);
		if (parent == null) {
			root = newInner;
		} else {
			parent.expand(leaf, newInner);
		}
		return newInner.getChild(branchIdx);
	}

	private void replaceLeaf(LeafNode oldLeaf, LeafNode newLeaf) {
		if (oldLeaf.parent() != null) {
			oldLeaf.parent().replace(oldLeaf, newLeaf);
		}
		else {
			root = newLeaf;
		}
	}

	/**
	 *
	 * @param result
	 */
	public void finish(PathResult result) {
		LeafNode updatedLeaf = null;

		// explored to here before ...
		if ( ((LeafNode)current).isFinal() ) {
			return;
		}

		switch (result.getState()) {
			case OK:
				updatedLeaf = new LeafOK(current.parent(), current.childId(), currentValues,
						((PathResult.OkResult) result).getPostCondition() );
				break;
			case ERROR:
				updatedLeaf = new LeafError(current.parent(), current.childId(), currentValues,
						((PathResult.ErrorResult) result).getExceptionClass(),
						((PathResult.ErrorResult) result).getStackTrace());
				break;
		}

		updatedLeaf.setComplete( ((LeafNode)current).complete() );

		System.out.println("MAX DEPTH: " + anaConf.getTreeMaxDepth());

		current.parent().replace( (LeafNode) current, updatedLeaf);
		current = updatedLeaf;
		if (initialTarget == null) {
			initialTarget = updatedLeaf;
		}
	}

	/**
	 *
	 */
	public void failCurrentTargetDontKnow() {
		LeafNode dk = LeafNode.dontKnow(currentTarget.parent(), currentTarget.childId());
		currentTarget.parent().replace(currentTarget, dk);
		currentTarget = dk;
	}

	public void failCurrentTargetDiverged() {
		LeafNode div = new LeafWithValuation(currentTarget.parent(), LeafNode.NodeType.DIVERGED,
				currentTarget.childId(), currentValues);
		currentTarget.parent().replace(currentTarget, div);
		currentTarget = div;
	}

	public void failCurrentTargetUnsat() {
		LeafNode unsat = LeafNode.unsat(currentTarget.parent(), currentTarget.childId());
		currentTarget.parent().replace(currentTarget, unsat);
		currentTarget = unsat;
	}

	public void failCurrentTargetBuggy(String cause) {
		LeafNode buggy = new LeafBuggy(currentTarget.parent(), currentTarget.childId(), currentValues, cause);
		currentTarget.parent().replace(currentTarget, buggy);
		currentTarget = buggy;
	}

	private void updateContext(Node from, Node to) {
		if (incremental) {
			Node lca = leastCommonAncestor(from, to);
			System.out.println(lca + " " + from.depth() + " " + to.depth());
			int popDepth = from.depth() - lca.depth();
			if (popDepth > 0) {
				solverCtx.pop(popDepth);
			}

			List<Expression<Boolean>> path = pathConstraint(to, lca);
			for (Expression<Boolean> clause : path) {
				assertExpression(clause);
				solverCtx.push();
			}
		}
		else {
			solverCtx.pop();
			List<Expression<Boolean>> path = pathConstraint(to, root);
			System.out.println("solving: " + Arrays.toString( path.toArray() ));
			solverCtx.add(path);
			solverCtx.push();
		}

	}

	private void assertExpression(Expression<Boolean> ... expr) {
		try {
			solverCtx.add(expr);
		}
		catch (RuntimeException ex) {
			// The only consequence of not adding a constraint to the context
			// should be that we may fail to generate models for some paths
			// later on ...
			logger.warning("Failed to add constrtaint " + expr + " due to " + ex.getMessage());
			//ex.printStackTrace();
		}
	}

	private List<Expression<Boolean>> pathConstraint(Node to, Node subTreeRoot) {
		LinkedList<Expression<Boolean>> path = new LinkedList<>();
		Node cur = to;
		while (cur != subTreeRoot) {
			path.addFirst( ((DecisionNode) cur.parent()).getConstraint( cur.childId()) );
			cur = cur.parent();
		}
		return path;
	}

	private List<Integer> expectedPathTo(Node to) {
		LinkedList<Integer> path = new LinkedList<>();
		Node cur = to;
		while (cur != null) {
			if (cur.parent() != null) {
				path.addFirst( cur.childId());
			}
			cur = cur.parent();
		}
		return path;
	}

	private Node leastCommonAncestor(Node n1, Node n2) {
		Node a1 = null;
		Node a2 = null;
		if (n1.depth() > n2.depth()) {
			a1 = n1;
			a2 = n2;
		}
		else {
			a1 = n2;
			a2 = n1;
		}
		while (a1.depth() > a2.depth()) {
			a1 = a1.parent();
		}
		while (a1 != a2) {
			a1 = a1.parent();
			a2 = a2.parent();
		}
		return a1;
	}

	/**
	 * next concrete valuation
	 *
	 * @return
	 */
	public Valuation findNext() {
		// mark root for re-execution
		current = root;

		// if we have preset values => use those ...
		if (replayMode) {
			if (replayValues == null || !replayValues.hasNext()) {
				replayMode = false;
			} else {
				currentTarget = null;
				assert this.expectedPath.isEmpty();
				return replayValues.next();
			}
		}

		// else: find next open node to explore
		while (strategy.hasMoreNodes()) {

			LeafNode nextOpen = null;
			while (nextOpen == null) {
				// no more nodes to explore ...
				if (!strategy.hasMoreNodes()) {
					return null;
				}
				nextOpen = strategy.nextOpenNode();

				// check if node is still valid
				if ((nextOpen.parent() == null && root != nextOpen) ||
						(nextOpen.parent() != null && nextOpen.parent().getChild(nextOpen.childId()) != nextOpen)) {
					nextOpen = null;
					continue;
				}
			}

			// update context and current target
			updateContext((currentTarget == null || currentTarget.parent() == null) ? root : currentTarget, nextOpen);
			currentTarget = nextOpen;

			// find model
			Valuation val = new Valuation();
			logger.finer("Finding new valuation");
			Result res = solverCtx.solve(val);
			currentValues = val;
			logger.finer("Found: " + res + " : " + val);

			// if node is unsat or dont/know -> next
			// if node is satisfiable -> simulate and execute!
			switch (res) {
				case UNSAT:
					failCurrentTargetUnsat();
					break;
				case DONT_KNOW:
					failCurrentTargetDontKnow();
					break;
				case SAT:
					LeafNode predictedTarget = simulate(val);
					if (predictedTarget != null && predictedTarget != currentTarget) {
						boolean inconclusive = predictedTarget.isExhausted();
						logger.info("Predicted ", inconclusive ? "inconclusive " : "", "divergence");
						if (inconclusive) {
							logger.finer("NOT attempting execution");
							failCurrentTargetBuggy("Failed to simulate");
							break;
						}
					}
					expectedPath = expectedPathTo(currentTarget);
					return val;
			}
		}

		// no more nodes
		return null;
	}

	public void setInitialValuation(Valuation initValuation) {
		((LeafWithValuation) initialTarget).updateValues(initValuation);
	}

	/**
	 * does constraints tree explore nodes?
	 *
	 * @return
	 */
	public boolean isExploreMode() {
		return exploreMode;
	}

	public void setExploreMode(boolean b) {
		this.exploreMode = b;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		root.print(sb, 0);
		return sb.toString();
	}

}
