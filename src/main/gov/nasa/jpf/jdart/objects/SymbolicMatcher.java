package gov.nasa.jpf.jdart.objects;

import gov.nasa.jpf.constraints.expressions.AbstractRegExExpression;
import gov.nasa.jpf.constraints.expressions.RegExBooleanExpression;

import java.util.HashMap;

public class SymbolicMatcher {
	private AbstractRegExExpression pattern;
	private SymbolicSMTString target;
	private HashMap<Integer, SymbolicSMTString> symbolicGroups;
	private boolean firstMatch = false;

	public SymbolicMatcher(SymbolicSMTString target, AbstractRegExExpression pattern) {
		this.target = target;
		this.pattern = pattern;
		this.symbolicGroups = new HashMap<>();
	}

	public RegExBooleanExpression targetInPattern() {
		return RegExBooleanExpression.create(target.symbolicValue, pattern);
	}

	public AbstractRegExExpression getPattern() {
		return pattern;
	}

	public boolean hasSymbolicGroup(int i) {
		return symbolicGroups.containsKey(i);
	}

	public SymbolicSMTString getSymbolicGroup(int i) {
		return symbolicGroups.get(i);
	}

	public void storeSymbolicGroup(int i, SymbolicSMTString symbolic) {
		symbolicGroups.put(i, symbolic);
	}

	public SymbolicSMTString getTarget() {
		return target;
	}

	public void setMatched() {
		firstMatch = true;
	}

	public boolean getMatched() {
		return firstMatch;
	}
}
