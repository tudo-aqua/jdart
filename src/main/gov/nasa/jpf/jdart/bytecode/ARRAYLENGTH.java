package gov.nasa.jpf.jdart.bytecode;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class ARRAYLENGTH extends gov.nasa.jpf.jvm.bytecode.ARRAYLENGTH {

	@Override
	public Instruction execute(ThreadInfo ti) {
		ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		if (analysis == null) {
			return super.execute(ti);
		}

		StackFrame sf = ti.getTopFrame();
		ElementInfo ei = ti.getHeap().get(sf.peek());
		if (ei.getObjectAttr() != null) {
			sf.pop();
			Expression length = ei.getObjectAttr(Expression.class);
			int concreteLength = ei.arrayLength();
			ConcolicUtil.pushInt(new ConcolicUtil.Pair<>(concreteLength, length), sf);
		} else {
			return super.execute(ti);
		}

		return getNext(ti);
	}
}
