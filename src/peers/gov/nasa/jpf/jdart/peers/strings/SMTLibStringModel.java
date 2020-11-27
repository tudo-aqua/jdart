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
import gov.nasa.jpf.constraints.expressions.CastExpression;
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
import java.math.BigInteger;


public class SMTLibStringModel {
	private final JPF_java_lang_String peer;

	public SMTLibStringModel(JPF_java_lang_String peer) {
		this.peer = peer;
	}

	public static boolean startsWith__Ljava_lang_String_2__Z(MJIEnv env, int objRef, int prefixRef) {
		String concreteSelf = env.getStringObject(objRef);
		String concretePrefix = env.getStringObject(prefixRef);

		ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		SymbolicSMTString symbolicSelf = env.getObjectAttr(objRef, SymbolicSMTString.class);
		SymbolicSMTString symbolicPrefix = env.getObjectAttr(prefixRef, SymbolicSMTString.class);
		boolean concreteRes = concreteSelf.startsWith(concretePrefix);
		if (analysis != null) {
			if (symbolicSelf == null) {
				ConcolicUtil.Pair<String> newSelf = analysis.getOrCreateSymbolicString();
				symbolicSelf =
						new SymbolicSMTString((Variable<String>) newSelf.symb,
								Constant.create(BuiltinTypes.STRING, concreteSelf));
			}
			if (symbolicPrefix == null) {
				ConcolicUtil.Pair<String> newPrefix = analysis.getOrCreateSymbolicString();
				symbolicPrefix = new SymbolicSMTString((Variable<String>) newPrefix.symb,
						Constant.create(BuiltinTypes.STRING, concretePrefix));
			}
			Expression startWith =
					StringBooleanExpression
							.createPrefixOf(symbolicPrefix.symbolicValue, symbolicSelf.symbolicValue);
			analysis.decision(env.getThreadInfo(), null, concreteRes ? 0 : 1, startWith,
					Negation.create(startWith));

			env.setReturnAttribute(startWith);
		}
		return concreteRes;

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
			env.setReturnAttribute(CastExpression.create(strLength, BuiltinTypes.SINT32));
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

		if (env.getArgAttributes() != null && env.getArgAttributes()[1] != null) {
			Object[] attrs = env.getArgAttributes();
			symbIndex = (Expression) attrs[1];
			if (!symbIndex.getType().equals(BuiltinTypes.SINT32)) {
				symbIndex = CastExpression.create(symbIndex, BuiltinTypes.SINT32);
			}
		}
		if (analysis == null || (symb == null && symbIndex == null)) {
			return peer.super_charAt__I__C(env, objRef, index);
		}

		boolean isSymbIndex = symbIndex != null;
		if (!isSymbIndex) {
			symbIndex = new Constant(BuiltinTypes.SINT32, index);
		}

		Expression length = (symb != null) ? CastExpression
				.create(StringIntegerExpression.createLength(symb.symbolicValue), BuiltinTypes.SINT32) :
				null;
		if (length == null) {
			length = new Constant(BuiltinTypes.SINT32, eiThis.length());
		}

		Expression constraint1 = new NumericBooleanExpression(
				new Constant<>(BuiltinTypes.SINT32, 0),
				NumericComparator.LE,
				symbIndex);
		Expression constraint2 = new NumericBooleanExpression(symbIndex, NumericComparator.LT, length);

		boolean sat1 = (0 <= index);
		boolean sat2 = (index < eiThis.length());
		if (!sat1 && !isSymbIndex) {
			env.throwException("java.lang.StringIndexOutOfBoundsException",
					"String index out of range: " + index);
			return MJIEnv.NULL;
		} else if (isSymbIndex) {
			analysis.decision(env.getThreadInfo(), null, sat1 ? 0 : 1, constraint1,
					new Negation(constraint1));
		}
		analysis.decision(ti, null, sat2 ? 0 : 1, constraint2, new Negation(constraint2));

		char[] data = env.getStringChars(objRef);
		if (index >= 0 && index < data.length) {
			if (symb == null) {
				throw new UnsupportedOperationException("We need to compute a valueSet in this case");
			}

			StringCompoundExpression charAt = StringCompoundExpression
					.createAt(symb.symbolicValue, symbIndex);
			Variable<String> newStrVar = (Variable<String>) analysis.getOrCreateSymbolicString().symb;
			env.setReturnAttribute(new SymbolicSMTString(newStrVar, charAt));
			return data[index];
		}
		env.throwException("java.lang.StringIndexOutOfBoundsException",
				"String index out of range: " + index);
		return MJIEnv.NULL;
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
		Object[] argAttrs = env.getArgAttributes();
		//DISCLAIMER: We do not support symbolic toffset, ooffset, len, ignoreCase at the moment.
		//ignore
		if (argAttrs != null) {
			//Return dont know in this case.
			env.throwException("error.Assume");
			return false;
		}

		String self = env.getStringObject(objref);
		String other = env.getStringObject(otherString);

		SymbolicSMTString symbolicSelf = env.getObjectAttr(objref, SymbolicSMTString.class);
		SymbolicSMTString symbolicOther = env.getObjectAttr(otherString, SymbolicSMTString.class);
		boolean concreteResult = self.regionMatches(ignoreCase, toffset, other, ooffset, len);
		if (symbolicSelf == null && symbolicOther == null) {
			//No symbolic parts, so we do not need to update anything in the symbolic model.
			return concreteResult;
		}
		if (symbolicSelf == null) {
			symbolicSelf = new SymbolicSMTString(null, Constant.create(BuiltinTypes.STRING, self));
		}
		if (symbolicOther == null) {
			symbolicOther = new SymbolicSMTString(null, Constant.create(BuiltinTypes.STRING, other));
		}

		boolean boundsCheck = evaluateBoundsRegionMatches(ooffset,
																											toffset,
																											len,
																											other.length(),
																											self.length(),
																											StringIntegerExpression.createLength(symbolicOther.symbolicValue),
																											StringIntegerExpression.createLength((symbolicSelf.symbolicValue)),
																											env.getThreadInfo());
		if (!boundsCheck) {
			return false;
		}
		if (symbolicSelf != null && symbolicOther == null) {
			return regionMatchConcreteSymbolicMix(symbolicSelf,
																						other,
																						toffset,
																						ooffset,
																						len,
																						ignoreCase,
																						concreteResult,
																						env.getThreadInfo());
		}
		if (symbolicOther != null && symbolicSelf == null) {
			return regionMatchConcreteSymbolicMix(symbolicOther,
																						self,
																						toffset,
																						ooffset,
																						len,
																						ignoreCase,
																						concreteResult,
																						env.getThreadInfo());
		}
//		if (toffset + len >= symbolicSelf.getSymbolicChars().length ||
//				ooffset + len >= symbolicOther.getSymbolicChars().length) {
//			return false;
//		}
		return regionMatchBotchSymbolic(symbolicSelf,
																		symbolicOther,
																		toffset,
																		ooffset,
																		len,
																		ignoreCase,
																		concreteResult,
																		env.getThreadInfo());

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

	public static int local_valueOf__C__Ljava_lang_String_2(MJIEnv env, int objRef, char value) {
		Object[] attrs = env.getArgAttributes();
		String res = String.valueOf(value);
		int resId = env.newString(res);
		if (attrs != null) {
			for (Object o : attrs) {
				if (o instanceof SymbolicNumber) {
					ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
					ConcolicUtil.Pair<String> expr = ca.getOrCreateSymbolicString();
					StringCompoundExpression fromInt =
							StringCompoundExpression.createToString(((SymbolicNumber) o).symbolicNumber);
					SymbolicSMTString symbolicRes = new SymbolicSMTString((Variable<String>) expr.symb, fromInt);
					symbolicRes.setIsFromNumber(true);
					symbolicRes.setSymbolicNumber((SymbolicNumber) o);
					env.setObjectAttr(resId, symbolicRes);
				} else if (o instanceof SymbolicSMTString) {
					env.setObjectAttr(resId, o);
				}
			}
		}
		return resId;
	}


	@MJI
	public int substring__I__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex) {
		SymbolicSMTString symbolicString = env.getObjectAttr(objRef, SymbolicSMTString.class);
		if (env.getArgAttributes() != null && (env.getArgAttributes()[1] != null)) {
			System.out.println("JPF_java_lang_String.substring__I__Ljava_lang_String_2");
			System.out.println("Cannot deal with symbolic values in args");
			env.throwException("errors.Assume");
			return -1;
		}
		int res = -1;
		if (symbolicString != null) {
			String concreteString = env.getStringObject(objRef);
			ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());

			Constant cBegin = Constant.create(BuiltinTypes.INTEGER, BigInteger.valueOf(beginIndex));

			Expression boundCheck = NumericBooleanExpression.create(cBegin,
																															NumericComparator.LT,
																															StringIntegerExpression.createLength(symbolicString.symbolicValue));
			ca.decision(env.getThreadInfo(),
									null,
									beginIndex < concreteString.length() ? 0 : 1,
									boundCheck,
									Negation.create(boundCheck));
			if (!(beginIndex < concreteString.length())) {
				env.throwException("java.lang.IndexOutOfBoundsException");
				return -1;
			}

			NumericCompound upperBound =
					NumericCompound.create(StringIntegerExpression.createLength(symbolicString.symbolicValue),
																 NumericOperator.MINUS,
																 cBegin);
			StringCompoundExpression expr =
					StringCompoundExpression.createSubstring(symbolicString.symbolicValue, cBegin, upperBound);


			ConcolicUtil.Pair<String> varName = ca.getOrCreateSymbolicString();
			SymbolicSMTString symbolicValue = new SymbolicSMTString((Variable<String>) varName.symb, expr);

			res = env.newString(concreteString.substring(beginIndex));
			env.addObjectAttr(res, symbolicValue);
		} else {
			res = peer.super_substring__I__Ljava_lang_String_2(env, objRef, beginIndex);
		}
		return res;
	}

	@MJI
	@SymbolicPeer
	public int substring__II__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex, int endIndex) {
		SymbolicSMTString symbolicString = env.getObjectAttr(objRef, SymbolicSMTString.class);

		String concreteString = env.getStringObject(objRef);

		if (env.getArgAttributes() != null && (env.getArgAttributes()[1] != null || env.getArgAttributes()[2] != null)) {
			System.out.println("JPF_java_lang_String.substring__II__Ljava_lang_String_2");
			System.out.println("Cannot deal with symbolic values in args");
			env.throwException("error.Assume");
			return -1;
		}

		if (beginIndex < 0) {
			env.throwException("java.lang.IndexOutOfBoundsException");
			return -1;
		}
		int res = -1;
		if (symbolicString != null) {
			ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());

			Constant cBegin = Constant.create(BuiltinTypes.SINT32, beginIndex);
			Constant cEnd = Constant.create(BuiltinTypes.INTEGER, BigInteger.valueOf(endIndex));

			Expression inBound = NumericBooleanExpression.create(cEnd,
																													 NumericComparator.GE,
																													 StringIntegerExpression.createLength(symbolicString.symbolicValue));


			if (endIndex >= concreteString.length()) {
				ca.decision(env.getThreadInfo(), null, 0, inBound, Negation.create(inBound));
				env.throwException("java.lang.IndexOutOfBoundsException");
				return -1;
			}
			ca.decision(env.getThreadInfo(), null, 1, inBound, Negation.create(inBound));
			res = peer.super_substring__II__Ljava_lang_String_2(env, objRef, beginIndex, endIndex);

			StringCompoundExpression expr =
					StringCompoundExpression.createSubstring(symbolicString.symbolicValue, cBegin, cEnd);
			ConcolicUtil.Pair<String> varName = ca.getOrCreateSymbolicString();
			SymbolicSMTString symbolicValue = new SymbolicSMTString((Variable<String>) varName.symb, expr);
			env.addObjectAttr(res, symbolicValue);
		} else {
			res = peer.super_substring__II__Ljava_lang_String_2(env, objRef, beginIndex, endIndex);
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
			throw new UnsupportedOperationException("Cannot deal with symbolic array split");
//			int[] fields = env.getReferenceArrayObject(concreteRes);
//			ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
//			if (fields.length != 0) {
//				SymbolicSMTString[] symbolicFields = new SymbolicSMTString[fields.length];
//
//				String param = env.getStringObject(rString0);
//				Constant cParam = Constant.create(BuiltinTypes.STRING, param);
//
//				int count = 0;
//				for (int ref : fields) {
//					ConcolicUtil.Pair<String> newStringVar = ca.getOrCreateSymbolicString();
//					Expression value = Negation.create(StringBooleanExpression.createContains(newStringVar.symb, cParam));
//					ca.decision(env.getThreadInfo(), null, 0, value);
//					SymbolicSMTString field = new SymbolicSMTString((Variable<String>) newStringVar.symb, newStringVar.symb);
//					env.addObjectAttr(ref, field);
//					symbolicFields[count] = field;
//					++count;
//				}
//				Expression concat = symbolicFields[0].symbolicValue;
//				for (int i = 1; i < symbolicFields.length; i++) {
//					concat = StringCompoundExpression.createConcat(concat, cParam);
//					concat = StringCompoundExpression.createConcat(concat, symbolicFields[i].symbolicValue);
//				}
//				concat = StringBooleanExpression.createEquals(symbolicString.symbolicValue, concat);
//				ca.decision(env.getThreadInfo(), null, 0, concat);
//			}
//			ConcolicUtil.Pair<Integer> newArrayLength = ca.getOrCreateSymbolicInt();
//			ca.decision(env.getThreadInfo(),
//									null,
//									0,
//									(NumericBooleanExpression.create(newArrayLength.symb,
//																									 NumericComparator.LT,
//																									 CastExpression.create(StringIntegerExpression.createLength(
//																											 symbolicString.symbolicValue), BuiltinTypes.SINT32))));
//
//
//			env.setObjectAttr(concreteRes, newArrayLength.symb);
		}
		return concreteRes;
	}

	private static boolean evaluateBoundsRegionMatches(int ooffset,
																										 int toffset,
																										 int len,
																										 int olen,
																										 int tlen,
																										 Expression oSymLen,
																										 Expression tSymLen,
																										 ThreadInfo ti) {
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		boolean upperOBound = (ooffset + len) > olen;
		Expression upperOBoundE = new NumericBooleanExpression(oSymLen,
				NumericComparator.LT,
				new Constant(BuiltinTypes.INTEGER, BigInteger.valueOf(ooffset + len)));

		boolean lowerOBound = (ooffset + len) < 0;

		boolean lowerTBound = (toffset + len) < 0;

		boolean upperTBound = (toffset + len) > tlen;
		Expression upperTBoundE = new NumericBooleanExpression(tSymLen,
				NumericComparator.LT,
				new Constant(BuiltinTypes.INTEGER, BigInteger.valueOf(toffset + len)));
		Expression check0 = ExpressionUtil.and(upperOBoundE, upperTBoundE);
		Expression check1 = ExpressionUtil.and(upperOBoundE, new Negation(upperTBoundE));
		Expression check2 = ExpressionUtil.and(new Negation(upperOBoundE), upperTBoundE);
		Expression check3 = ExpressionUtil.and(new Negation(upperOBoundE), new Negation(upperTBoundE));
		int branchIdx = -1;
		if (upperOBound) {
			if (upperTBound) {
				branchIdx = 0;

			} else {
				branchIdx = 1;
			}
		} else {
			if (upperTBound) {
				branchIdx = 2;
			} else {
				branchIdx = 3;
			}
		}
		ca.decision(ti, null, branchIdx, check0, check1, check2, check3);
		return !(upperOBound || lowerOBound || lowerTBound || upperTBound);
	}

	private static boolean regionMatchBotchSymbolic(SymbolicSMTString self,
																									SymbolicSMTString other,
																									int soffset,
																									int ooffset,
																									int len,
																									boolean ignoreCase,
																									boolean concreteResult,
																									ThreadInfo ti) {

		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		Expression cSOff = Constant.create(BuiltinTypes.INTEGER, BigInteger.valueOf(soffset));
		Expression cOOff = Constant.create(BuiltinTypes.INTEGER, BigInteger.valueOf(ooffset));
		Expression cLen = Constant.create(BuiltinTypes.INTEGER, BigInteger.valueOf(len));
		//FIXME: We don't integrate ignoreCase currently into the analysis. Think about the possible effects.
//		if (ignoreCase) {
//			ti.getMJIEnv().throwException("errors.Assume", "Cannot Model ignore Case yet");
//		}
		Expression symbolicSubstring = StringCompoundExpression.createSubstring(self.symbolicValue, cSOff, cLen);
		Expression otherSubstring = StringCompoundExpression.createSubstring(other.symbolicValue, cSOff, cLen);
		StringBooleanExpression equalStrings = StringBooleanExpression.createEquals(symbolicSubstring, otherSubstring);
		int branchIndex = concreteResult ? 0 : 1;
		ca.decision(ti, null, branchIndex, equalStrings, new Negation(equalStrings));
		return concreteResult;
	}

	private static boolean regionMatchConcreteSymbolicMix(SymbolicSMTString symbolic,
																												String other,
																												int soffset,
																												int ooffset,
																												int len,
																												boolean ignoreCase,
																												boolean concreteResult,
																												ThreadInfo ti) {
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		Expression cSOff = Constant.create(BuiltinTypes.INTEGER, BigInteger.valueOf(soffset));
		Expression cOOff = Constant.create(BuiltinTypes.INTEGER, BigInteger.valueOf(ooffset));
		Expression cLen = Constant.create(BuiltinTypes.INTEGER, BigInteger.valueOf(len));
		if (ignoreCase) {
			ti.getMJIEnv().throwException("errors.Assume", "Cannot Model ignore Case yet");
		}
		Expression symbolicSubstring = StringCompoundExpression.createSubstring(symbolic.symbolicValue, cSOff, cLen);
		Constant otherSubstring = Constant.create(BuiltinTypes.STRING, other.substring(ooffset, len));
		StringBooleanExpression equalStrings = StringBooleanExpression.createEquals(symbolicSubstring, otherSubstring);
		int branchIndex = concreteResult ? 0 : 1;
		ca.decision(ti, null, branchIndex, equalStrings, new Negation(equalStrings));
		return concreteResult;
	}
}
