/*
 * Copyright (C) 2015, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Modifications Copyright 2019 TU Dortmund, Falk Howar (@fhowar) und Malte Mues (@mmuesly)
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
package gov.nasa.jpf.jdart;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.ValuationEntry;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.exceptions.ImpreciseRepresentationException;
import gov.nasa.jpf.constraints.java.ObjectConstraints;
import gov.nasa.jpf.constraints.parser.ParserUtil;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.jdart.ConcolicUtil.Pair;
import gov.nasa.jpf.jdart.config.AnalysisConfig;
import gov.nasa.jpf.jdart.config.ConcolicConfig;
import gov.nasa.jpf.jdart.config.ConcolicMethodConfig;
import gov.nasa.jpf.jdart.config.ConcolicValues;
import gov.nasa.jpf.jdart.config.ParamConfig;
import gov.nasa.jpf.jdart.constraints.tree.ConstraintsTree;
import gov.nasa.jpf.jdart.constraints.tree.ConstraintsTree.BranchEffect;
import gov.nasa.jpf.jdart.constraints.paths.PathResult;
import gov.nasa.jpf.jdart.constraints.paths.PostCondition;
import gov.nasa.jpf.jdart.objects.SymbolicObjectsContext;
import gov.nasa.jpf.jdart.termination.TerminateOnAssertionError;
import gov.nasa.jpf.jdart.termination.TerminationStrategy;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.UncaughtException;
import org.antlr.runtime.RecognitionException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ConcolicMethodExplorer {

	private static final class RestoreExploreState {
		public final boolean explore;

		public RestoreExploreState(boolean explore) {
			this.explore = explore;
		}

		public RestoreExploreState clone() {
			return this;
		}
	}

	public static ConcolicMethodExplorer getCurrentAnalysis(ThreadInfo ti) {
		return ti.getAttr(ConcolicMethodExplorer.class);
	}


	/**
	 * logger
	 */
	private final JPFLogger logger = JPF.getLogger("jdart");

	/**
	 * explored method
	 */
	private MethodInfo methodInfo;

	/**
	 * concolic config for method
	 */
	private final ConcolicMethodConfig methodConfig;
	private final AnalysisConfig anaConf;

	/**
	 * constraints tree from exploring method
	 */
	private ConstraintsTree constraintsTree;

	/**
	 * the original initial valuation for symbolic vars
	 */
	private Valuation initValuation;

	private Valuation currValuation;
	private Valuation nextValuation;

	/**
	 * The original values of the parameters
	 */
	private Object[] initParams;

	private SymbolicObjectsContext symContext;

	/**
	 * execution info about method
	 */

	private final SolverContext solverCtx;

	/**
	 * Storing eventualy configured TerminationStrategy
	 */
	private final TerminationStrategy ts;

	private final ConcolicConfig.StringModel sm;

	public ConcolicMethodExplorer(ConcolicConfig config, String id, MethodInfo mi) {
		// store method info and config
		this.methodInfo = mi;
		this.methodConfig = config.getMethodConfig(id);
		this.anaConf = methodConfig.getAnalysisConfig();

		// get preset values
		ConcolicValues vals = methodConfig.getConcolicValues();

		// create a constraints tree
		this.solverCtx = config.getSolver().createContext();
		this.constraintsTree = new ConstraintsTree(solverCtx, anaConf, vals,
				config.getExplorationStrategy(), config.getIncremental());
		this.ts = config.getTerminationStrategy();
		sm = config.getStringModel();
	}

	public void setExplore(boolean explore) {
		constraintsTree.setExploreMode(explore);
	}

	public String getId() {
		return methodConfig.getId();
	}

	public boolean isRootFrame(StackFrame sf) {
		return sf.getPrevious().hasFrameAttr(RootFrame.class);
	}

	private PostCondition collectPostCondition(ThreadInfo ti) {
		PostCondition pc = new PostCondition();
		for (SymbolicVariable<?> sv : symContext.getSymbolicVars()) {
			sv.addToPC(pc);
		}
		collectSymbolicReturn(pc, ti);

		return pc;
	}

	private void collectSymbolicReturn(PostCondition pc, ThreadInfo ti) {
		byte rtc = methodInfo.getReturnTypeCode();
		if (rtc == Types.T_VOID) {
			return;
		}


		StackFrame sf = ti.getTopFrame();
		if (rtc == Types.T_ARRAY || rtc == Types.T_REFERENCE) {
			int ref = sf.peek();
			ElementInfo ei = ti.getHeap().get(ref);
			if (ei == null) {
				return;
			}

		} else { // primitive
			Type<?> type = ConcolicUtil.forTypeCode(rtc);
			Pair<?> cr = ConcolicUtil.peek(sf, type);
			if (!cr.isConcrete()) {
				pc.setReturn(cr.symb);
			}
		}
	}

	public void completePathDontKnow(ThreadInfo ti) {
		PostCondition pc = collectPostCondition(ti);
		PathResult res = PathResult.dontKnow();
		constraintsTree.finish(res);
	}

	public void completePathOk(ThreadInfo ti) {
		PostCondition pc = collectPostCondition(ti);
		PathResult res = PathResult.ok(currValuation, pc);
		constraintsTree.finish(res);
	}

	public void completePathError(ThreadInfo ti) {
		ElementInfo exElem = ti.getPendingException().getException();
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			ti.printStackTrace(pw, exElem.getObjectRef());
		}
		String st = sw.toString();
		PathResult res = PathResult.error(currValuation, exElem.getClassInfo().getName(), st);
		constraintsTree.finish(res);
		if (ts != null && ts instanceof TerminateOnAssertionError) {
			TerminateOnAssertionError tofe = (TerminateOnAssertionError) ts;
			tofe.notifyObservedErrorWithName(exElem.getClassInfo().getName());
		}
	}

	public boolean hasMoreChoices() {
		// If the initValuation is null, then the initial
		// valuation has not been read; we are *before* the
		// first execution
		if (initValuation == null) {
			return true;
		}

		// If there is no next valuation, try to find one
		if (nextValuation == null) {
			nextValuation = constraintsTree.findNext();
		}
		return (nextValuation != null);
	}

	public boolean advanceValuation() {
		if (nextValuation == null) {
			nextValuation = constraintsTree.findNext();
		}

		if (nextValuation != null) {
			for (Variable v : currValuation.getVariables()) {
				if (!nextValuation.containsValueFor(v)) {
					nextValuation.addEntry(new ValuationEntry(v, nextValuation.getValue(v))); // returns the default
					// value for this type
				}
			}
		}
		currValuation = nextValuation;
		nextValuation = null;

		return (currValuation != null);
	}


	/**
	 * registers method for concolic execution. Puts symbolic input values onto the stack ...
	 *
	 * @param invokeInstruction
	 * @param systemState
	 * @param ti
	 */
	public void initializeMethod(ThreadInfo ti, StackFrame sf) {
		logger.finest("Initializing concolic execution of " + methodInfo.getFullName());

		// mark root frame
		sf.setFrameAttr(RootFrame.getInstance());

		symContext = new SymbolicObjectsContext(ti.getHeap(),
																						anaConf.getSymbolicFieldsExclude(),
																						anaConf.getSymbolicFieldsInclude(),
																						anaConf.getSpecialExclude());

		initializeSymbolicStatic(ti);
		initializeSymbolicParams(ti, sf);

		List<Variable<?>> vlist = new ArrayList<>();
		logger.finest("Symbolic variables:");
		logger.finest("===================");
		for (SymbolicVariable<?> var : symContext.getSymbolicVars()) {
			logger.finest(var.getVariable().getName());
			vlist.add(var.getVariable());
		}
		logger.finest();

		for (String constraintStr : anaConf.getConstraints()) {
			logger.finer("Adding constraint ", constraintStr);
			try {
				Expression<Boolean> constrExpr =
						ParserUtil.parseLogical(constraintStr, ObjectConstraints.getJavaTypes(), vlist);
				try {
					solverCtx.add(constrExpr);
				}
				catch (Exception ex) {
					logger.severe("Could not add constraint to solver: ", ex);
				}
			}
			catch (RecognitionException | ImpreciseRepresentationException ex) {
				logger.severe("Could not parse constraint: ", ex);
			}
		}

	}

	private void initializeSymbolicStatic(ThreadInfo ti) {
		List<String> symbStatics = anaConf.getSymbolicStatics();
		for (String clazz : symbStatics) {
			ClassInfo ci = ClassInfo.getInitializedClassInfo(clazz, ti);
			ElementInfo ei = ci.getStaticElementInfo();
			symContext.processObject(ei, ci.getName());
		}
	}

	private void initializeSymbolicParams(ThreadInfo ti, StackFrame sf) {
		List<ParamConfig> pconfig = methodConfig.getParams();
		int argSize = pconfig.size();
		this.initParams = new Object[argSize];
		int stackIdx = methodInfo.getArgumentsSize();

		Heap heap = ti.getHeap();

		if (!methodInfo.isStatic()) {
			stackIdx--;
			int thisRef = sf.peek(stackIdx);
			ElementInfo thisEi = heap.get(thisRef);
			symContext.processObject(thisEi, "this", true);
		}

		byte[] argTypes = methodInfo.getArgumentTypes();

		for (int i = 0; i < argSize; i++) {
			ParamConfig pc = pconfig.get(i);
			String name = pc.getName();

			byte tc = argTypes[i];
			stackIdx--;
			if (tc == Types.T_LONG || tc == Types.T_DOUBLE) {
				stackIdx--;
			}

			this.initParams[i] = getVal(sf, stackIdx, tc);

			if (name == null) {
				continue; // null name indicates non-symbolic param
			}

			if (tc == Types.T_REFERENCE || tc == Types.T_ARRAY) {
				int ref = sf.peek(stackIdx);
				ElementInfo ei = heap.get(ref);
				if (ei != null) {
					symContext.processObject(ei, name, true);
				}
			} else { // primitive type
				Type<?> t = ConcolicUtil.forTypeCode(tc);
				Variable<?> var = Variable.create(t, name);
				SymbolicParam<?> sp = new SymbolicParam<>(var, stackIdx);
				symContext.addStackVar(sp);
			}
		}
	}

	private static Object getVal(StackFrame sf, int offset, byte type) {
		switch (type) {
			case Types.T_LONG:
				return sf.peekLong(offset);
			case Types.T_DOUBLE:
				return sf.peekDouble(offset);
			case Types.T_FLOAT:
				return sf.peekFloat(offset);
			case Types.T_INT:
			default:
				return sf.peek(offset);
		}
	}

	public void prepareFirstExecution(StackFrame sf) {
		initValuation = new Valuation();
		for (SymbolicVariable<?> sv : symContext.getSymbolicVars()) {
			sv.readInitial(initValuation, sf);
		}
		currValuation = initValuation;
		count = 0;
		byteCount = 0;
	}

	public void prepareReexecution(StackFrame sf) {
		logger.finest("Reexecuting with valuation " + currValuation);
		for (SymbolicVariable<?> sv : symContext.getSymbolicVars()) {
			sv.apply(currValuation, sf);
		}
		count = 0;
		byteCount = 0;
	}


	public void uncaughtException(ThreadInfo ti, UncaughtException uex) {
		completePathError(ti);
		ti.clearPendingException();
		ti.breakTransition(true);
	}

	public boolean needsDecisions() {
		return constraintsTree.needsDecision();
	}

	@SafeVarargs
	public final void decision(ThreadInfo ti, Instruction branchInsn, int chosenIdx,
														 Expression<Boolean>... expressions) {
		BranchEffect eff = constraintsTree.decision(branchInsn, chosenIdx, expressions);
		switch (eff) {
			case BUGGY:
				logger.severe("Aborting current execution due to buggy divergence...");
				constraintsTree.failCurrentTargetBuggy("Aborting current execution due to buggy divergence...");
				ti.breakTransition(true);
				break;
			case UNEXPECTED:
				logger.warning("Unexpected divergence in execution of current valuation ...");
				// TODO: Here, we could make more effort (e.g., using a constraint solver again)...
				constraintsTree.failCurrentTargetDiverged();
				break;
			default:
		}
	}


	public ConcolicMethodConfig getMethodConfig() {
		return this.methodConfig;
	}

	public ConstraintsTree getInternalConstraintsTree() {
		return this.constraintsTree;
	}

	public CompletedAnalysis finish() {
		constraintsTree.setInitialValuation(initValuation);
		return new CompletedAnalysis(methodConfig, initValuation, initParams, constraintsTree);
	}

	public void newPath(StackFrame sf) {
		if (initValuation == null) { // first execution
			prepareFirstExecution(sf);
		} else { // reexecution
			advanceValuation();
			prepareReexecution(sf);
		}
	}


	public void makeCurrentAnalysis(ThreadInfo ti) {
		ti.setAttr(this);
	}

	public AnalysisConfig getAnalysisConfig() {
		return anaConf;
	}

	public void methodExited(ThreadInfo ti, MethodInfo mi) {
		RestoreExploreState r = ti.getTopFrame().getFrameAttr(RestoreExploreState.class);
		if (r != null) {
			constraintsTree.setExploreMode(r.explore);
			logger.finer("Restored exploration state after leaving method ", mi.getFullName());
		}
	}

	public void methodEntered(ThreadInfo ti, MethodInfo mi) {
		boolean explore = constraintsTree.isExploreMode();

		if (explore) {
			if (anaConf.suspendExploration(mi)) {
				ti.getTopFrame().setFrameAttr(new RestoreExploreState(explore));
				logger.finer("Suspending exploration in method " + mi.getFullName());
				constraintsTree.setExploreMode(false);
			}
		} else {
			if (anaConf.resumeExploration(mi)) {
				ti.getTopFrame().setFrameAttr(new RestoreExploreState(explore));
				logger.finer("Resuming exploration in method " + mi.getFullName());
				constraintsTree.setExploreMode(true);
			}
		}
	}

	private int count = 0;
	private int byteCount = 0;

	public Pair<Integer> getOrCreateSymbolicInt() {
		Variable var = new Variable(BuiltinTypes.SINT32, "_int" + count++);
		Integer val = (Integer) currValuation.getValue(var);
		if (val == null) {
			val = 0;
		}
		return new Pair<>(val, var);
	}

	public Pair<Integer> getOrCreateSymbolicInt8() {
		Variable var = new Variable(BuiltinTypes.SINT8, "_int" + count++);
		Integer val = ((Byte) currValuation.getValue(var)).intValue();
		if (val == null) {
			val = 0;
		}
		return new Pair<>(val, var);
	}

	public Pair<String> getOrCreateSymbolicString() {
		Variable var = new Variable(BuiltinTypes.STRING, "_string" + count++);
		String val = ((String) currValuation.getValue(var));
		if (val == null) {
			val = "";
		}
		return new Pair<>(val, var);
	}

	public Pair<Boolean> getOrCreateSymbolicBoolean() {
		Variable var = new Variable(BuiltinTypes.BOOL, "_bool" + count++);
		Boolean val = (Boolean) currValuation.getValue(var);
		if (val == null) {
			val = false;
		}
		return new Pair<>(val, var);
	}

	public Pair<Byte> getOrCreateSymbolicByte() {
		Variable var = new Variable(BuiltinTypes.SINT8, "_byte" + byteCount++);
		Byte val = (Byte) currValuation.getValue(var);
		if (val == null) {
			val = 0;
		}
		return new Pair<>(val, var);
	}

	public Pair<Character> getOrCreateSymbolicChar() {
		Variable var = new Variable(BuiltinTypes.UINT16, "_char" + byteCount++);
		Character val = (Character) currValuation.getValue(var);
		if (val == null) {
			val = 0;
		}
		return new Pair<>(val, var);
	}

	public Pair<Float> getOrCreateSymbolicFloat() {
		Variable var = new Variable(BuiltinTypes.FLOAT, "_float" + count++);
		Float val = (Float) currValuation.getValue(var);
		if (val == null) {
			val = 0f;
		}
		return new Pair<>(val, var);
	}

	public Pair<Double> getOrCreateSymbolicDouble() {
		Variable var = new Variable(BuiltinTypes.DOUBLE, "_double" + count++);
		Double val = (Double) currValuation.getValue(var);
		if (val == null) {
			val = 0.0;
		}
		return new Pair<>(val, var);
	}

	public Pair<Long> getOrCreateSymbolicLong() {
		Variable var = new Variable(BuiltinTypes.SINT64, "_long" + count++);
		Long val = (Long) currValuation.getValue(var);
		if (val == null) {
			val = 0l;
		}
		return new Pair<>(val, var);
	}

	public ConcolicConfig.StringModel getStringModel() {
		return sm;
	}

	// LEGACY API

	/*
	@Deprecated
	public ConstraintsTree getConstraintsTree() {
		return constraintsTree.toFinalCTree();
	}

	@Deprecated
	public Valuation getOriginalInitialValuation() {
		return initValuation;
	}

	 */
}
