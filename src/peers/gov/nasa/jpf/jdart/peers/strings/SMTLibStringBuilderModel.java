package gov.nasa.jpf.jdart.peers.strings;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.StringBooleanExpression;
import gov.nasa.jpf.constraints.expressions.StringCompoundExpression;
import gov.nasa.jpf.constraints.expressions.StringIntegerExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicSMTString;
import gov.nasa.jpf.jdart.peers.JPF_java_lang_StringBuilder;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import java.math.BigInteger;
import java.util.ArrayList;

public class SMTLibStringBuilderModel {

	private final JPF_java_lang_StringBuilder peer;

	public SMTLibStringBuilderModel(JPF_java_lang_StringBuilder peer) {
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

		SymbolicSMTString symbParam = ei.getObjectAttr(SymbolicSMTString.class);

		ElementInfo eiSB = env.getElementInfo(objref);
		SymbolicSMTString symbSB = eiSB.getObjectAttr(SymbolicSMTString.class);

		if (symbSB == null && symbParam == null) {
			return peer.super_append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);
		}

		if (symbSB == null && symbParam != null) {

			int field = env.getReferenceField(objref, "value");
			char[] values = env.getCharArrayObject(field);
			ArrayList<Expression> symbolicChars = new ArrayList<>();
			int count = env.getIntField(objref, "count");

			ConcolicUtil.Pair<String> newSymbolicString = ca.getOrCreateSymbolicString();
			String value = new String(values, 0, count);
			symbSB =
					new SymbolicSMTString((Variable<String>) newSymbolicString.symb,
							Constant.create(BuiltinTypes.STRING,
									value));
		}
		if (symbParam == null && symbSB != null) {
			symbParam = new SymbolicSMTString(null, Constant.create(BuiltinTypes.STRING, ei.asString()));
		}

		int concreteResultRef = peer
				.super_append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);

		//test for symbolic string
		if (symbParam != null) {

			Expression<String> concat = StringCompoundExpression
					.createConcat(symbSB.symbolicValue, symbParam.symbolicValue);
			ConcolicUtil.Pair<String> newSymbolicString = ca.getOrCreateSymbolicString();
			symbSB = new SymbolicSMTString((Variable<String>) newSymbolicString.symb, concat);
			env.addObjectAttr(objref, symbSB);
		}
		return concreteResultRef;
	}

	@MJI
	@SymbolicPeer
	public char charAt__I__C(MJIEnv env, int objref, int index) {
		SymbolicSMTString symbolic = env.getObjectAttr(objref, SymbolicSMTString.class);

		int field = env.getReferenceField(objref, "value");
		char[] values = env.getCharArrayObject(field);
		Object[] attrs = env.getArgAttributes();
		if (attrs != null && attrs[1] != null) {
			//Expect index to be concrete for now.
			env.throwException("errors.Assume");
			return (char) -1;
		}
		if (symbolic != null) {
			ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
			Expression strLen = StringIntegerExpression.createLength(symbolic.symbolicValue);
			Expression indexWithinBounds =
					new PropositionalCompound(
							new NumericBooleanExpression(new Constant<BigInteger>(BuiltinTypes.INTEGER,
									BigInteger.valueOf(index)),
									NumericComparator.LT,
									strLen),
							LogicalOperator.AND,
							new NumericBooleanExpression(
									new Constant<BigInteger>(BuiltinTypes.INTEGER, BigInteger.valueOf(0)),
									NumericComparator.LT,
									strLen));
			if (!(index >= 0 && index < values.length)) {
				ca.decision(env.getThreadInfo(), null, 1, indexWithinBounds, new Negation(indexWithinBounds));

				env.throwException("java.lang.IndexOutOfBoundsException");
			} else {
				ca.decision(env.getThreadInfo(), null, 0, indexWithinBounds, new Negation(indexWithinBounds));
				ConcolicUtil.Pair<String> newStrVar = ca.getOrCreateSymbolicString();
				Constant cIndex = Constant.create(BuiltinTypes.SINT32, index);
				SymbolicSMTString newStr = new SymbolicSMTString((Variable<String>) newStrVar.symb,
																												 StringCompoundExpression.createAt(symbolic.symbolicValue,
																																													 cIndex));
				env.setReturnAttribute(newStr);
				return (char) values[index];
			}
		} else {
			if (index >= 0 && index < values.length) {
				return (char) values[index];
			} else {
				env.throwException("java.lang.IndexOutOfBoundsException");
			}
		}
		//This is never executed, but the compiler doe nos recognise the env.throwException as an throw-Statement.
		//Therefore the if is not exhaustive without this statement.
		return (char) 0;
	}

	@MJI
	@SymbolicPeer
	public void setCharAt__IC__V(MJIEnv env, int objref, int index, char character) {
		ElementInfo eiSB = env.getElementInfo(objref);
		SymbolicSMTString symbolic = eiSB.getObjectAttr(SymbolicSMTString.class);

		Object[] attrs = env.getArgAttributes();
		if (attrs != null && attrs[1] != null) {
			//Expect index to be concrete for now.
			env.throwException("errors.Assume");
			return;
		}

		int field = env.getReferenceField(objref, "value");
		char[] values = env.getCharArrayObject(field);
		int length = env.getIntField(objref, "count");

		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());

		if (index < 0) {
			env.throwException("java.lang.StringIndexOutOfBoundsException");
			return;
		}

		if (symbolic != null) {
			int sat = index >= length ? 0 : 1;
			Expression outOfRange =
					NumericBooleanExpression.create(Constant.create(BuiltinTypes.INTEGER, BigInteger.valueOf(index)),
																					NumericComparator.GE,
																					StringIntegerExpression.createLength(symbolic.symbolicValue));
			ca.decision(env.getThreadInfo(), null, sat, outOfRange, Negation.create(outOfRange));
		}

		if (index >= length) {
			env.throwException("java.lang.StringIndexOutOfBoundsException");
			return;
		}
		values[index] = character;

		if (symbolic != null) {
			Constant cChar = Constant.create(BuiltinTypes.STRING, new String(new char[]{character}));
			Constant cIndex = Constant.create(BuiltinTypes.SINT32, index);
			ca.decision(env.getThreadInfo(),
									null,
									0,
									StringBooleanExpression.createEquals(cChar,
																											 StringCompoundExpression.createAt(symbolic.symbolicValue,
																																												 cIndex)));

		}


	}

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
		SymbolicSMTString symbolic = eiSB.getObjectAttr(SymbolicSMTString.class);
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		if (symbolic != null) {
			SymbolicSMTString[] symbolicChars = new SymbolicSMTString[dest_start + end - start];
			for (int i = 0; i + start < end; i++) {
				ConcolicUtil.Pair<String> newChar = ca.getOrCreateSymbolicString();
				symbolicChars[dest_start + i] = new SymbolicSMTString((Variable<String>) newChar.symb,
						StringCompoundExpression.createAt(symbolic.symbolicValue,
								Constant.create(
										BuiltinTypes.SINT32,
										i + start)));
				eiCA.setElementAttr(dest_start + i, symbolicChars[dest_start + i]);
			}
			eiCA.addObjectAttr(symbolicChars);
		}
	}
}
