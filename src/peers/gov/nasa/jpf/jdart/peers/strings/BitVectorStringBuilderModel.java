package gov.nasa.jpf.jdart.peers.strings;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.CastExpression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicBVString;
import gov.nasa.jpf.jdart.peers.JPF_java_lang_StringBuilder;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;

import java.util.ArrayList;
import java.util.Arrays;

public class BitVectorStringBuilderModel {

	private final JPF_java_lang_StringBuilder peer;

	public BitVectorStringBuilderModel(JPF_java_lang_StringBuilder peer) {
		this.peer = peer;
	}

	@MJI
	@SymbolicPeer
	/*
	 * gets always implicitly called by string concatenation via + operator
	 */ public int append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(MJIEnv env, int objref, int sref) {
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		ElementInfo ei = env.getElementInfo(sref);
		if (ei == null || ei.isNull()) {
			return peer.super_append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);
		}

		SymbolicBVString symbParam = ei.getObjectAttr(SymbolicBVString.class);

		ElementInfo eiSB = env.getElementInfo(objref);
		SymbolicBVString symbSB = eiSB.getObjectAttr(SymbolicBVString.class);

		if (symbSB == null && symbParam == null) {
			return peer.super_append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);
		}

		if (symbSB == null && symbParam != null) {

			int field = env.getReferenceField(objref, "value");
			char[] values = env.getCharArrayObject(field);
			ArrayList<Expression> symbolicChars = new ArrayList<>();
			int count = env.getIntField(objref, "count");
			for (int i = 0; i < count; i++) {
				ConcolicUtil.Pair newSymbolicChar = ca.getOrCreateSymbolicChar();
				Expression valueAssignment = new NumericBooleanExpression(newSymbolicChar.symb,
																																	NumericComparator.EQ,
																																	new Constant<Character>(BuiltinTypes.UINT16,
																																													values[i]));
				ca.decision(env.getThreadInfo(), null, 0, valueAssignment);
				symbolicChars.add(newSymbolicChar.symb);
			}
			Expression constantSize = new Constant(BuiltinTypes.SINT8, count);
			symbSB = new SymbolicBVString(constantSize, symbolicChars.toArray(new Expression[0]));
		}

		int concreteResultRef = peer.super_append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);

		//test for symbolic string
		if (symbParam != null) {
			ArrayList<Expression> newChars = new ArrayList(Arrays.asList(symbSB.getSymbolicChars()));
			for (Expression symbolicChar : symbParam.getSymbolicChars()) {
				newChars.add(symbolicChar);
			}
			ConcolicUtil.Pair<Integer> newSymbolicLength = ca.getOrCreateSymbolicInt8();
			Expression left = symbSB.getSymbolicLength().getType().equals(BuiltinTypes.SINT32) &&
												symbSB.getSymbolicLength() instanceof CastExpression ?
					((CastExpression) symbSB.getSymbolicLength()).getCasted() :
					symbSB.getSymbolicLength();
			Expression right = symbParam.getSymbolicLength().getType().equals(BuiltinTypes.SINT32) &&
												 symbParam.getSymbolicLength() instanceof CastExpression ?
					((CastExpression) symbParam.getSymbolicLength()).getCasted() :
					symbParam.getSymbolicLength();
			Expression newSymbolicLengthConstraint = new NumericCompound<Integer>(left, NumericOperator.PLUS, right);
			symbSB = new SymbolicBVString(newSymbolicLengthConstraint, newChars.toArray(new Expression[0]));
			env.addObjectAttr(concreteResultRef, symbSB);
		}
		return concreteResultRef;
	}

	@MJI
	@SymbolicPeer
	public char charAt__I__C(MJIEnv env, int objref, int index) {
		SymbolicBVString symbolic = env.getObjectAttr(objref, SymbolicBVString.class);
		int field = env.getReferenceField(objref, "value");
		char[] values = env.getCharArrayObject(field);
		Object[] attrs = env.getArgAttributes();
		if (attrs != null && attrs[1] != null) {
			//Expect index to be concrete for now.
			env.throwException("errors.Assume");
		}
		if (symbolic != null) {
			ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
			Expression indexWithinBounds =
					new PropositionalCompound(new NumericBooleanExpression(new Constant<Integer>(BuiltinTypes.SINT32, index),
																																 NumericComparator.LT,
																																 symbolic.getSymbolicLength()),
																		LogicalOperator.AND,
																		new NumericBooleanExpression(new Constant<Integer>(BuiltinTypes.SINT32, 0),
																																 NumericComparator.LT,
																																 symbolic.getSymbolicLength()));
			if (!(index >= 0 && index < symbolic.getSymbolicChars().length)) {
				ca.decision(env.getThreadInfo(), null, 1, indexWithinBounds, new Negation(indexWithinBounds));

				env.throwException("java.lang.IndexOutOfBoundsException");
			} else {
				ca.decision(env.getThreadInfo(), null, 0, indexWithinBounds, new Negation(indexWithinBounds));
				env.setReturnAttribute(symbolic.getSymbolicChars()[index]);
				return (char) values[index];
			}
		} else {
			if (index >= 0 && index < values.length) {
				return (char) values[index];
			} else {
				env.throwException("java.lang.IndexOutOfBoundsException");
			}
		}
		return (char) 0;
	}

	@MJI
	@SymbolicPeer
	public void getChars__II_3CI__V(MJIEnv env, int objref, int start, int end, int dest, int dest_start) {
		ElementInfo eiSB = env.getElementInfo(objref);
		ElementInfo eiCA = env.getElementInfo(dest);
		int maxSize = env.getIntField(objref, "count");
		int charArrayRef = env.getReferenceField(objref, "value");
		char[] values = env.getCharArrayObject(charArrayRef);
		char[] destChar = eiCA.asCharArray();
		if (end > maxSize || start < 0 || dest_start < 0 || start > end || (dest_start + end - start) > destChar.length) {
			env.throwException("java.lang.IndexOutOfBoundsException");
		} else {
			for (int i = 0; i + start < end; i++) {
				destChar[dest_start + i] = values[i + start];
			}
		}
		SymbolicBVString symbolic = eiSB.getObjectAttr(SymbolicBVString.class);
		if (symbolic != null) {
			Expression[] symbolicChars = new Expression[dest_start + end - start];
			for (int i = 0; i + start < end; i++) {
				symbolicChars[dest_start + i] = symbolic.getSymbolicChars()[i + start];
			}
			eiCA.addObjectAttr(new SymbolicBVString(symbolic.getSymbolicLength(), symbolicChars));
		}
	}

	@MJI
	@SymbolicPeer
	public void setCharAt__IC__V(MJIEnv env, int objref, int index, char character) {
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		Object[] attrs = env.getArgAttributes();
		if (attrs != null && attrs[1] != null && attrs[2] != null) {
			env.throwException("errors.Assume");
		}

		ElementInfo ei = env.getElementInfo(objref);
		SymbolicBVString symbolicString = ei.getObjectAttr(SymbolicBVString.class);

		int field = env.getReferenceField(objref, "value");
		char[] values = env.getCharArrayObject(field);
		int length = env.getIntField(objref, "count");

		if (symbolicString != null) {
			Expression stringLength = new NumericBooleanExpression(new Constant<Integer>(BuiltinTypes.SINT32, index),
																														 NumericComparator.GE,
																														 symbolicString.getSymbolicLength());
			ca.decision(env.getThreadInfo(), null, index >= length ? 0 : 1, stringLength, new Negation(stringLength));
		}
		if (index >= length || index < 0) {
			env.throwException("java.lang.StringIndexOutOfBoundsException");
			return;
		}
		values[index] = character;
		if (symbolicString != null) {
			Expression[] symbolicChars = symbolicString.getSymbolicChars();
			Expression charConstraint = new NumericBooleanExpression(symbolicChars[index],
																															 NumericComparator.EQ,
																															 new Constant<Character>(BuiltinTypes.UINT16,
																																											 character));
			ca.decision(env.getThreadInfo(), null, 0, charConstraint);
			env.setObjectAttr(objref, symbolicString);
		}
	}

}
