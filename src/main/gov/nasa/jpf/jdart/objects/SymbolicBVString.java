package gov.nasa.jpf.jdart.objects;

import gov.nasa.jpf.constraints.api.Expression;

public class SymbolicBVString extends SymbolicString {

	private final Expression<Integer> symbolicLength;


	private final Expression[] symbolicChars;

	public SymbolicBVString(Expression symbolicLength, Expression[] symbolicChars) {
		this.symbolicLength = symbolicLength;
		this.symbolicChars = symbolicChars;
	}

	public Expression getSymbolicLength() {
		return symbolicLength;
	}

	public Expression[] getSymbolicChars() {
		return symbolicChars;
	}


}
