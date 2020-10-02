/*
 * Copyright 2020 TU Dortmund, Malte Mues (@mmuesly)
 *
 *
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */


package gov.nasa.jpf.jdart.peers.strings;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.StringBooleanExpression;
import gov.nasa.jpf.constraints.expressions.StringCompoundExpression;
import gov.nasa.jpf.constraints.expressions.StringIntegerExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicNumber;
import gov.nasa.jpf.jdart.objects.SymbolicSMTString;
import gov.nasa.jpf.jdart.peers.JPF_java_lang_String;
import gov.nasa.jpf.vm.DynamicElementInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.ThreadInfo;


public class SMTLibStringModel {
	private final JPF_java_lang_String peer;

	public SMTLibStringModel(JPF_java_lang_String peer) {
		this.peer = peer;
	}

	@MJI
	@SymbolicPeer
	public int init___3BII__Ljava_lang_String_2(MJIEnv env, int objRef, int bytesRef, int offset, int length) {
		ElementInfo ei = env.getElementInfo(bytesRef);
		Expression symbolicLength = ei.getObjectAttr(Expression.class);
		ConcolicUtil.Pair<String> aNewString =
				ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getOrCreateSymbolicString();
		StringIntegerExpression newLength = StringIntegerExpression.createLength(aNewString.symb);
		NumericBooleanExpression constrainedLength =
				NumericBooleanExpression.create(symbolicLength, NumericComparator.EQ, newLength);

		for (int i = 0; i < ei.arrayLength(); i++) {
			Expression charExpr = ei.getElementAttr(i, Expression.class);
			if (charExpr != null && !(charExpr instanceof Variable)) {
				throw new UnsupportedOperationException("It seems like this char is constrained");
			}
		}
		env.setObjectAttr(objRef, new SymbolicSMTString((Variable<String>) aNewString.symb, constrainedLength));
		return peer.super_init___3BII(env, objRef, bytesRef, offset, length);
	}

	@MJI
	@SymbolicPeer
	public int length____I(MJIEnv env, int objRef) {
		ElementInfo ei = env.getElementInfo(objRef);
		SymbolicSMTString symb = ei.getObjectAttr(SymbolicSMTString.class);
		if (symb != null) {
			StringIntegerExpression strLength = StringIntegerExpression.createLength(symb.strVar);
			env.setReturnAttribute(strLength);
		}
		String concreteString = ei.asString();
		return concreteString.length();
	}

	@MJI
	@SymbolicPeer
	public char charAt__I__C(MJIEnv env, int objRef, int index) {
		ThreadInfo ti = env.getThreadInfo();
		ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);

		String eiThis = env.getElementInfo(objRef).asString();
		SymbolicSMTString symb = env.getObjectAttr(objRef, SymbolicSMTString.class);
		Expression symbIndex = null;

		if (env.getArgAttributes() != null) {
			symbIndex = (Expression) env.getArgAttributes()[1];
		}
		if (analysis == null || (symb == null && symbIndex == null)) {
			return peer.super_charAt__I__C(env, objRef, index);
		}
		Expression length = (symb != null) ? StringIntegerExpression.createLength(symb.strVar) : null;
		if (length == null) {
			length = new Constant(BuiltinTypes.SINT32, eiThis.length());
		}
		if (symbIndex == null) {
			symbIndex = new Constant(BuiltinTypes.SINT32, index);
		}
		Expression constraint = ExpressionUtil.and(new NumericBooleanExpression(new Constant<>(BuiltinTypes.SINT32, 0),
																																						NumericComparator.LE,
																																						symbIndex),
																							 new NumericBooleanExpression(symbIndex, NumericComparator.LT, length));

		boolean sat = (0 <= index) && (index < eiThis.length());
		int branchIdx = sat ? 0 : 1;
		analysis.decision(ti, null, branchIdx, constraint, new Negation(constraint));

		try {
			if (symb == null) {
				throw new UnsupportedOperationException("We need to compute a valueSet in this case");
			}

			StringCompoundExpression charAt = StringCompoundExpression.createAt(symb.strVar, symbIndex);
			if (symb.symbolicValue instanceof StringCompoundExpression) {
				charAt = StringCompoundExpression.createAt(symb.symbolicValue, symbIndex);
			}
			Variable<String> newStrVar = (Variable<String>) analysis.getOrCreateSymbolicString().symb;
			StringBooleanExpression assignment = StringBooleanExpression.createEquals(newStrVar, charAt);
			env.setReturnAttribute(new SymbolicSMTString(newStrVar, assignment));
			return peer.charAt__I__C(env, objRef, index);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			env.throwException(ArrayIndexOutOfBoundsException.class.getName(), e.getMessage());
			return MJIEnv.NULL;
		}
	}

	@MJI
	@SymbolicPeer
	public boolean equals__Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int argRef) {
		if (argRef == env.NULL) {
			return false;
		}

		ThreadInfo ti = env.getThreadInfo();
		ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		System.out.println("----- Symbolic String equals");
		SymbolicSMTString symbThis = env.getObjectAttr(objRef, SymbolicSMTString.class);
		SymbolicSMTString symbOther = env.getObjectAttr(argRef, SymbolicSMTString.class);

		if (analysis == null || (symbThis == null && symbOther == null)) {
			System.out.println("----- concrete, " + (analysis == null));
			return peer.super_equals__Ljava_lang_Object_2__Z(env, objRef, argRef);
		}
		System.out.println("----- symbolic");

		if (symbThis != null && symbOther != null) {
			System.out.println("----- both symbolic");
			String selfConcrete = env.getStringObject(objRef);
			String otherConcrete = env.getStringObject(argRef);

			Expression thisSymbValue =
					symbThis.symbolicValue instanceof StringCompoundExpression ? symbThis.symbolicValue : symbThis.strVar;
			Expression otherSymbValue =
					symbOther.symbolicValue instanceof StringCompoundExpression ? symbOther.symbolicValue : symbOther.strVar;
			Expression<Boolean> decisionExpr = StringBooleanExpression.createEquals(thisSymbValue, otherSymbValue);
			boolean concreteEquals = selfConcrete.equals(otherConcrete);
			analysis.decision(ti, null, concreteEquals ? 0 : 1, decisionExpr, Negation.create(decisionExpr));
			return concreteEquals;
		}

		System.out.println("----- one symbolic!");

		SymbolicSMTString symb = (symbThis != null) ? symbThis : symbOther;
		if (symb.isFromNumber()) {
			return peer.equals_numbers(env, (symbThis != null) ? objRef : argRef, (symbThis != null) ? argRef : objRef);
		}

		int concRef = (symbThis != null) ? argRef : objRef;
		int symbRef = (symbThis != null) ? objRef : argRef;

		ElementInfo eiConc = ti.getElementInfo(concRef);
		System.out.println("The concret String is: " + eiConc.asString());

		ElementInfo eiSymb = ti.getElementInfo(symbRef);
		String concString = eiConc.asString();
		String symbString = eiSymb.asString();

		Expression symbolicValue =
				symb.symbolicValue instanceof StringCompoundExpression ? symb.symbolicValue : symb.strVar;

		Expression equals =
				StringBooleanExpression.createEquals(symbolicValue, Constant.create(BuiltinTypes.STRING, concString));
		boolean sat = peer.super_equals__Ljava_lang_Object_2__Z(env, objRef, argRef);
		System.out.println("----- equals: " + sat);
		int branchIdx = sat ? 0 : 1;
		analysis.decision(ti, null, branchIdx, equals, Negation.create(equals));
		return sat;
	}

	@MJI
	@SymbolicPeer
	public int concat__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int paramRef) {
		ElementInfo ei1 = env.getElementInfo(objRef);
		ElementInfo ei2 = env.getElementInfo(paramRef);
		String concreteS1 = ei1.asString();
		String concreteS2 = ei2.asString();

		int newRes = env.newString(concreteS1.concat(concreteS2));
		SymbolicSMTString s1 = ei1.getObjectAttr(SymbolicSMTString.class);
		SymbolicSMTString s2 = ei2.getObjectAttr(SymbolicSMTString.class);


		Expression smtValueS1 = Variable.create(BuiltinTypes.STRING, concreteS1);
		Expression smtValueS2 = Variable.create(BuiltinTypes.STRING, concreteS2);

		if (s1 != null) {
			smtValueS1 = s1.symbolicValue instanceof StringCompoundExpression ? s1.symbolicValue : s1.strVar;
		}
		if (s2 != null) {
			smtValueS2 = s2.symbolicValue instanceof StringCompoundExpression ? s2.symbolicValue : s2.strVar;
		}
		if (s1 != null || s2 != null) {
			ThreadInfo ti = env.getThreadInfo();
			ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);
			ConcolicUtil.Pair<String> newStringVar = analysis.getOrCreateSymbolicString();
			SymbolicSMTString symbolicSMTString = new SymbolicSMTString((Variable<String>) newStringVar.symb,
																																	StringCompoundExpression.createConcat(smtValueS1,
																																																				smtValueS2));
			ElementInfo newEI = env.getElementInfo(newRes);
			newEI.addObjectAttr(symbolicSMTString);
		}
		return newRes;
	}

	@MJI
	@SymbolicPeer
	public boolean regionMatches__ZILjava_lang_String_2II__Z(MJIEnv env,
																													 int objref,
																													 boolean ignoreCase,
																													 int toffset,
																													 int otherString,
																													 int ooffset,
																													 int len) {
		throw new UnsupportedOperationException();
	}

	@MJI
	@SymbolicPeer
	public boolean contains__Ljava_lang_CharSequence_2__Z(MJIEnv env, int objRef, int charSequenceRef) {
		ThreadInfo ti = env.getThreadInfo();

		String self = env.getStringObject(objRef);
		String other = "";
		if (((DynamicElementInfo) env.getHeap().get(charSequenceRef)).getClassInfo()
																																 .getName()
																																 .startsWith("java.lang.String")) {
			other = env.getStringObject(charSequenceRef);
		} else {
			ConcolicMethodExplorer.getCurrentAnalysis(ti).completePathDontKnow(ti);
			return false;
		}
		ElementInfo eiSelf = env.getElementInfo(objRef);
		ElementInfo eiOther = env.getElementInfo(charSequenceRef);
		SymbolicSMTString symbolicSelf = eiSelf.getObjectAttr(SymbolicSMTString.class);
		SymbolicSMTString symbolicOther = eiOther.getObjectAttr(SymbolicSMTString.class);
		ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		if (analysis == null || (symbolicOther == null && symbolicSelf == null)) {
			return self.contains(other);
		} else if (symbolicOther != null && symbolicSelf == null) {
			return evaluateContainsConcreteSelfSymbolicOther(self, other, symbolicOther, analysis, ti);
		} else if (symbolicOther == null && symbolicSelf != null) {
			return evaluateContainsSymbolicSelfConcreteOther(self, symbolicSelf, other, analysis, ti);
		} else {
			return evaluateContainsSymbolicSelfSymbolicOther(self, symbolicSelf, other, symbolicOther, analysis, ti);
		}
	}

	private boolean evaluateContainsConcreteSelfSymbolicOther(String self,
																														String other,
																														SymbolicSMTString symbolicOther,
																														ConcolicMethodExplorer analysis,
																														ThreadInfo ti) {
		boolean ret = self.contains(other);
		Constant selfStr = Constant.create(BuiltinTypes.STRING, self);
		StringBooleanExpression containsExpr = StringBooleanExpression.createContains(selfStr, symbolicOther.strVar);
		analysis.decision(ti, null, ret ? 0 : 1, containsExpr, Negation.create(containsExpr));
		return ret;
	}

	private boolean evaluateContainsSymbolicSelfConcreteOther(String self,
																														SymbolicSMTString symbolicSelf,
																														String other,
																														ConcolicMethodExplorer analysis,
																														ThreadInfo ti) {
		boolean ret = self.contains(other);
		Constant otherStr = Constant.create(BuiltinTypes.STRING, other);
		StringBooleanExpression containsExpr = StringBooleanExpression.createContains(symbolicSelf.strVar, otherStr);
		analysis.decision(ti, null, ret ? 0 : 1, containsExpr, Negation.create(containsExpr));
		return ret;
	}

	private boolean evaluateContainsSymbolicSelfSymbolicOther(String self,
																														SymbolicSMTString symbolicSelf,
																														String other,
																														SymbolicSMTString symbolicOther,
																														ConcolicMethodExplorer analysis,
																														ThreadInfo ti) {
		boolean ret = self.contains(other);
		StringBooleanExpression containsExpr =
				StringBooleanExpression.createContains(symbolicSelf.strVar, symbolicOther.strVar);
		analysis.decision(ti, null, ret ? 0 : 1, containsExpr, Negation.create(containsExpr));
		return ret;
	}


	@MJI
	public static int local_valueOf__D__Ljava_lang_String_2(MJIEnv env, int objRef, double value) {
		Object[] attrs = env.getArgAttributes();
		String res = String.valueOf(value);
		if (attrs != null) {
			for (Object o : attrs) {
				if (o instanceof SymbolicNumber) {
					ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
					ConcolicUtil.Pair<String> expr = ca.getOrCreateSymbolicString();
					Constant symbolicValue = Constant.create(BuiltinTypes.STRING, res);
					SymbolicSMTString symbolicRes = new SymbolicSMTString((Variable<String>) expr.symb, symbolicValue);
					symbolicRes.setIsFromNumber(true);
					symbolicRes.setSymbolicNumber((SymbolicNumber) o);
					env.setReturnAttribute(symbolicRes);
				}
			}
		}
		return env.newString(res);
	}


	@MJI
	public int substring__I__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex) {
		SymbolicSMTString symbolicString = env.getObjectAttr(objRef, SymbolicSMTString.class);
		int res = peer.super_substring__I__Ljava_lang_String_2(env, objRef, beginIndex);
		if (env.getArgAttributes() != null && (env.getArgAttributes()[1] != null || env.getArgAttributes()[2] != null)) {
			System.out.println("JPF_java_lang_String.substring__I__Ljava_lang_String_2");
			System.out.println("Cannot deal with symbolic values in args");
			env.throwException("error.Assume");
			return -1;
		}
		if (symbolicString != null) {
			Constant cBegin = Constant.create(BuiltinTypes.SINT32, beginIndex);
			NumericCompound upperBound =
					NumericCompound.create(StringIntegerExpression.createLength(symbolicString.symbolicValue),
																 NumericOperator.MINUS,
																 cBegin);
			StringCompoundExpression expr =
					StringCompoundExpression.createSubstring(symbolicString.symbolicValue, cBegin, upperBound);

			ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
			ConcolicUtil.Pair<String> varName = ca.getOrCreateSymbolicString();
			SymbolicSMTString symbolicValue = new SymbolicSMTString((Variable<String>) varName.symb, expr);
			env.addObjectAttr(res, symbolicValue);
		}
		return res;
	}

	@MJI
	@SymbolicPeer
	public int substring__II__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex, int endIndex) {
		SymbolicSMTString symbolicString = env.getObjectAttr(objRef, SymbolicSMTString.class);
		int res = peer.super_substring__II__Ljava_lang_String_2(env, objRef, beginIndex, endIndex);
		if (env.getArgAttributes() != null && (env.getArgAttributes()[1] != null || env.getArgAttributes()[2] != null)) {
			System.out.println("JPF_java_lang_String.substring__II__Ljava_lang_String_2");
			System.out.println("Cannot deal with symbolic values in args");
			env.throwException("error.Assume");
			return -1;
		}
		if (symbolicString != null) {
			Constant cBegin = Constant.create(BuiltinTypes.SINT32, beginIndex);
			Constant cEnd = Constant.create(BuiltinTypes.SINT32, endIndex);
			StringCompoundExpression expr =
					StringCompoundExpression.createSubstring(symbolicString.symbolicValue, cBegin, cEnd);

			ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
			ConcolicUtil.Pair<String> varName = ca.getOrCreateSymbolicString();
			SymbolicSMTString symbolicValue = new SymbolicSMTString((Variable<String>) varName.symb, expr);
			env.addObjectAttr(res, symbolicValue);
		}
		return res;
	}

	@MJI
	@SymbolicPeer
	public int split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env, int objRef, int rString0) {
		int concreteRes = peer.super_split__Ljava_lang_String_2___3Ljava_lang_String_2(env, objRef, rString0);
		SymbolicSMTString symbolicString = env.getObjectAttr(objRef, SymbolicSMTString.class);
		SymbolicSMTString symbolicParameter = env.getObjectAttr(rString0, SymbolicSMTString.class);
		if (symbolicParameter != null) {
			throw new UnsupportedOperationException("Cannot deal with symbolic parameter in String.split");
		}
		if (symbolicString != null) {
			int[] fields = env.getReferenceArrayObject(concreteRes);
			if (fields.length != 0) {
				SymbolicSMTString[] symbolicFields = new SymbolicSMTString[fields.length];

				String param = env.getStringObject(rString0);
				Constant cParam = Constant.create(BuiltinTypes.STRING, param);
				ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
				int count = 0;
				for (int ref : fields) {
					ConcolicUtil.Pair<String> newStringVar = ca.getOrCreateSymbolicString();
					Expression value = Negation.create(StringBooleanExpression.createContains(newStringVar.symb, cParam));
					ca.decision(env.getThreadInfo(), null, 0, value);
					SymbolicSMTString field = new SymbolicSMTString((Variable<String>) newStringVar.symb, newStringVar.symb);
					env.addObjectAttr(ref, field);
					symbolicFields[count] = field;
					++count;
				}
				Expression concat = symbolicFields[0].symbolicValue;
				for (int i = 1; i < symbolicFields.length; i++) {
					concat = StringCompoundExpression.createConcat(concat, cParam);
					concat = StringCompoundExpression.createConcat(concat, symbolicFields[i].symbolicValue);
				}
				concat = StringBooleanExpression.createEquals(symbolicString.symbolicValue, concat);
				ca.decision(env.getThreadInfo(), null, 0, concat);
			}
		}
		return concreteRes;
	}
}
