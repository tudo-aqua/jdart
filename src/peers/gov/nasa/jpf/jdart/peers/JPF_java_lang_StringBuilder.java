package gov.nasa.jpf.jdart.peers;

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
import gov.nasa.jpf.jdart.objects.SymbolicString;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;

import java.util.ArrayList;
import java.util.Arrays;

public class JPF_java_lang_StringBuilder extends gov.nasa.jpf.vm.JPF_java_lang_StringBuilder {

	@MJI
	@SymbolicPeer
	@Override
	public void $init__Ljava_lang_String_2__V(MJIEnv env, int objref, int sRef) {
		super.$init__Ljava_lang_String_2__V(env, objref, sRef);

		SymbolicString symbolicString = env.getObjectAttr(sRef, SymbolicString.class);
		if (symbolicString != null) {
			env.addObjectAttr(objref, symbolicString);
		}
	}

	@MJI
	@SymbolicPeer
	public int length____I(MJIEnv env, int objref) {
		ElementInfo ei = env.getElementInfo(objref);
		SymbolicString symbolic = ei.getObjectAttr(SymbolicString.class);
		int length = env.getIntField(objref, "count");
		if (symbolic != null) {
			env.setReturnAttribute(symbolic.getSymbolicLength());
		}
		return length;
	}

	@MJI
	@SymbolicPeer
	@Override
	/*
	 * gets always implicitly called by string concatenation via + operator
	 */ public int append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(MJIEnv env, int objref, int sref) {
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		ElementInfo ei = env.getElementInfo(sref);
		if (ei == null || ei.isNull()) {
			return super.append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);
		}

		SymbolicString symbParam = ei.getObjectAttr(SymbolicString.class);

		ElementInfo eiSB = env.getElementInfo(objref);
		SymbolicString symbSB = eiSB.getObjectAttr(SymbolicString.class);

		if (symbSB == null) {
			int field = env.getReferenceField(objref, "value");
			char[] values = env.getCharArrayObject(field);
			ArrayList<Expression> symbolicChars = new ArrayList<>();
			for (char c : values) {
				ConcolicUtil.Pair newSymbolicChar = ca.getOrCreateSymbolicByte();
				Expression valueAssignment = new NumericBooleanExpression(newSymbolicChar.symb,
																		  NumericComparator.EQ,
																		  new Constant<Byte>(BuiltinTypes.SINT8,
																							 (byte) c));
				ca.decision(env.getThreadInfo(), null, 0, valueAssignment);
				symbolicChars.add(newSymbolicChar.symb);
			}
			ConcolicUtil.Pair<Integer> symbolicLength = ca.getOrCreateSymbolicInt8();
			Expression valueAssignment = new NumericBooleanExpression(symbolicLength.symb,
																	  NumericComparator.EQ,
																	  new Constant<Byte>(BuiltinTypes.SINT8,
																						 (byte) env.getIntField(objref,
																												"count"
																						 )));
			ca.decision(env.getThreadInfo(), null, 0, valueAssignment);
			symbSB = new SymbolicString(symbolicLength.symb, symbolicChars.toArray(new Expression[0]));
		}

		int concreteResultRef = super.append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);

		//test for symbolic string
		if (symbParam != null) {
			ArrayList<Expression> newChars = new ArrayList(Arrays.asList(symbSB.getSymbolicChars()));
			for (Expression symbolicChar : symbParam.getSymbolicChars()) {
				newChars.add(symbolicChar);
			}
			ConcolicUtil.Pair<Integer> newSymbolicLength = ca.getOrCreateSymbolicInt8();
			Expression left = symbSB.getSymbolicLength().getType().equals(BuiltinTypes.SINT8) ?
					CastExpression.create(symbSB.getSymbolicLength(), BuiltinTypes.SINT32) :
					symbSB.getSymbolicLength();

			Expression newSymbolicLengthConstraint =
					new NumericCompound<Integer>(left, NumericOperator.PLUS, symbParam.getSymbolicLength());
			//ca.decision(env.getThreadInfo(), null, 0, newSymbolicLength.symb);
			symbSB = new SymbolicString(newSymbolicLengthConstraint, newChars.toArray(new Expression[0]));
			env.addObjectAttr(concreteResultRef, symbSB);
		}
		return concreteResultRef;
	}

	@MJI
	@SymbolicPeer
	@Override
	public int toString____Ljava_lang_String_2(MJIEnv env, int objref) {
		ElementInfo eiThis = env.getElementInfo(objref);
		SymbolicString symb = eiThis.getObjectAttr(SymbolicString.class);

		int toString = super.toString____Ljava_lang_String_2(env, objref);
		ElementInfo eiString = env.getElementInfo(toString);
		eiString.setObjectAttr(symb);

		if (symb != null) {
			return toString;
		} else {
			return super.toString____Ljava_lang_String_2(env, objref);
		}
	}

	@MJI
	@SymbolicPeer
	public int charAt__I__C(MJIEnv env, int objref, int index) {
		SymbolicString symbolic = env.getObjectAttr(objref, SymbolicString.class);
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
					new PropositionalCompound(new NumericBooleanExpression(new Constant<Integer>(
					BuiltinTypes.SINT32,
					index), NumericComparator.LT, symbolic.getSymbolicLength()),
																	 LogicalOperator.AND,
																	 new NumericBooleanExpression(new Constant<Integer>(
																			 BuiltinTypes.SINT32,
																			 0),
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
		if (end > maxSize || start < 0 || dest_start < 0 || start > end ||
			(dest_start + end - start) > destChar.length) {
			env.throwException("java.lang.IndexOutOfBoundsException");
		} else {
			for (int i = 0; i + start < end; i++) {
				destChar[dest_start + i] = values[i + start];
			}
		}
		SymbolicString symbolic = eiSB.getObjectAttr(SymbolicString.class);
		if (symbolic != null) {
			Expression[] symbolicChars = new Expression[dest_start + end - start];
			for (int i = 0; i + start < end; i++) {
				symbolicChars[dest_start + i] = symbolic.getSymbolicChars()[i + start];
			}
			eiCA.addObjectAttr(new SymbolicString(symbolic.getSymbolicLength(), symbolicChars));
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
		SymbolicString symbolicString = ei.getObjectAttr(SymbolicString.class);

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
																	 new Constant<Byte>(BuiltinTypes.SINT8,
																						(byte) character));
			ca.decision(env.getThreadInfo(), null, 0, charConstraint);
			env.setObjectAttr(objref, symbolicString);
		}
	}
}
