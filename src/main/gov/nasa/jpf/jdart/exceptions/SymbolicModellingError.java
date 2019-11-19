package gov.nasa.jpf.jdart.exceptions;

public class SymbolicModellingError extends RuntimeException {
	public SymbolicModellingError() {
		super();
	}

	public SymbolicModellingError(String msg) {
		super(msg);
	}
}
