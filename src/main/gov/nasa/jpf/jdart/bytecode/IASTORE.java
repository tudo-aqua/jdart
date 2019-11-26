package gov.nasa.jpf.jdart.bytecode;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class IASTORE extends gov.nasa.jpf.jvm.bytecode.IASTORE {

	//If this is the way to go, we need to find a way to inject this code somehow differently into the codebase.
	//Otherwise, it is required to implement the same snippet on every ASTORE or ALOAD bytecode.
	@Override
	public Instruction execute(ThreadInfo ti) {
		ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		if (analysis == null) {
			return super.execute(ti);
		}

		StackFrame sf = ti.getTopFrame();
		int index = peekIndex(ti);
		int array = peekArrayRef(ti);
		ElementInfo eiArray = ti.getElementInfo(array); //The ArrayRef offset is 1
		if (eiArray.getObjectAttr() != null || sf.getOperandAttr(1) != null) {

			ConcolicUtil.Pair<Integer> idx = ConcolicUtil.peekInt(sf, 1);
			Expression length = (Expression) eiArray.getObjectAttr();
			if (length == null) {
				length = new Constant(BuiltinTypes.SINT32, eiArray.arrayLength());
			}
			Expression constraint =
					ExpressionUtil.and(new NumericBooleanExpression(new Constant<>(BuiltinTypes.SINT32, 0),
																	NumericComparator.LE,
																	idx.symb),
									   new NumericBooleanExpression(idx.symb, NumericComparator.LT, length));

			boolean sat = (0 <= idx.conc) && (idx.conc < eiArray.arrayLength());
			int branchIdx = sat ? 0 : 1;
			analysis.decision(ti, this, branchIdx, constraint, new Negation(constraint));
		}
		return super.execute(ti);
	}
}
