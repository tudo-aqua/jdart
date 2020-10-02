package gov.nasa.jpf.jdart.objects;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;

public class SymbolicSMTString extends SymbolicString {

	public Expression symbolicValue;
	public Variable<String> strVar;


	public SymbolicSMTString(Variable<String> strVar, Expression string) {
		this.symbolicValue = string;
		this.strVar = strVar;
	}

}
