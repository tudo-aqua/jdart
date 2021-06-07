package gov.nasa.jpf.jdart.constraints;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;

public class Assumption extends Instruction {
	@Override
	public int getByteCode() {
		return 0;
	}

	@Override
	public Instruction execute(ThreadInfo ti) {
		return null;
	}
}
