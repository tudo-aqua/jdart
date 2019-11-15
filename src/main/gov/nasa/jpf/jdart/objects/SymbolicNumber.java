package gov.nasa.jpf.jdart.objects;

import gov.nasa.jpf.constraints.api.Expression;

public class SymbolicNumber {
	public boolean isFromString = false;
	public boolean hasBeenConstrained = false;
	public Expression symbolicNumber;

	public SymbolicNumber(Expression sn) {
		symbolicNumber = sn;
	}
}
