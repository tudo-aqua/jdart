/*
 * Copyright 2020 TU Dortmund, Falk Howar (@fhowar), Malte Mues (@mmuesly)
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
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.exceptions.SymbolicModellingError;
import gov.nasa.jpf.jdart.objects.SymbolicBVString;
import gov.nasa.jpf.jdart.objects.SymbolicNumber;
import gov.nasa.jpf.jdart.peers.JPF_java_lang_String;
import gov.nasa.jpf.vm.DynamicElementInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class BitVectorStringModel {
	private final JPF_java_lang_String peer;

	public BitVectorStringModel(JPF_java_lang_String peer) {
		this.peer = peer;
	}

	public static boolean startsWith__Ljava_lang_String_2__Z(MJIEnv env, int objRef, int prefixRef) {
		return false;

	}


	@MJI
	@SymbolicPeer
	public int init___3BII__Ljava_lang_String_2(MJIEnv env, int objRef, int bytesRef, int offset, int length) {
		ElementInfo eiSource = env.getElementInfo(bytesRef);
		Expression symbLength = eiSource.getObjectAttr(Expression.class);
		Expression[] symbChars = new Expression[eiSource.arrayLength()];
		for (int i = 0; i < eiSource.arrayLength(); i++) {
			symbChars[i] = CastExpression.create(eiSource.getElementAttr(i, Expression.class), BuiltinTypes.UINT16);
		}
		env.setObjectAttr(objRef, new SymbolicBVString(symbLength, symbChars));
		return peer.super_init___3BII(env, objRef, bytesRef, offset, length);
	}

	@MJI
	@SymbolicPeer
	public int length____I(MJIEnv env, int objRef) {
		ElementInfo ei = env.getElementInfo(objRef);
		String concrete = ei.asString();
		int result = concrete.length();

		if (ei.hasObjectAttr(SymbolicBVString.class)) {
			SymbolicBVString symbolic = ei.getObjectAttr(SymbolicBVString.class);
			Expression length = symbolic.getSymbolicLength();
			env.setReturnAttribute(length);

		}
		return result;
	}

	@MJI
	@SymbolicPeer
	public char charAt__I__C(MJIEnv env, int objRef, int index) {
		ThreadInfo ti = env.getThreadInfo();

		ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		ElementInfo eiThis = env.getElementInfo(objRef);
		SymbolicBVString symb = env.getObjectAttr(objRef, SymbolicBVString.class);
		Expression symbIndex = null;

		if (env.getArgAttributes() != null) {
			symbIndex = (Expression) env.getArgAttributes()[1];
		}
		if (analysis == null || (symb == null && symbIndex == null)) {
			return peer.super_charAt__I__C(env, objRef, index);
		}
		Expression length = (symb != null) ? symb.getSymbolicLength() : null;
		if (length == null) {
			length = new Constant(BuiltinTypes.SINT32, eiThis.asString().length());
		}
		if (symbIndex == null) {
			symbIndex = new Constant(BuiltinTypes.SINT32, index);
		}
		Expression constraint = ExpressionUtil.and(new NumericBooleanExpression(new Constant<>(BuiltinTypes.SINT32, 0),
																																						NumericComparator.LE,
																																						symbIndex),
																							 new NumericBooleanExpression(symbIndex, NumericComparator.LT, length));

		boolean sat = (0 <= index) && (index < eiThis.asString().length());
		int branchIdx = sat ? 0 : 1;
		analysis.decision(ti, null, branchIdx, constraint, new Negation(constraint));

		try {
			env.setReturnAttribute(symb.getSymbolicChars()[index]);
			return peer.super_charAt__I__C(env, objRef, index);
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
		SymbolicBVString symbThis = env.getObjectAttr(objRef, SymbolicBVString.class);
		SymbolicBVString symbOther = env.getObjectAttr(argRef, SymbolicBVString.class);

		if (analysis == null || (symbThis == null && symbOther == null)) {
			System.out.println("----- concrete, " + (analysis == null));
			return peer.super_equals__Ljava_lang_Object_2__Z(env, objRef, argRef);
		}
		System.out.println("----- symbolic");

		if (symbThis != null && symbOther != null) {
			System.out.println("----- both symbolic");
			String selfConcrete = env.getStringObject(objRef);
			String otherConcrete = env.getStringObject(argRef);
			boolean lengthSat = (selfConcrete.length() == otherConcrete.length());

			Expression left = symbThis.getSymbolicLength().getType().equals(BuiltinTypes.SINT32) &&
												symbThis.getSymbolicLength() instanceof CastExpression ?
					((CastExpression) symbThis.getSymbolicLength()).getCasted() :
					symbThis.getSymbolicLength();
			Expression right = symbOther.getSymbolicLength().getType().equals(BuiltinTypes.SINT32) &&
												 symbOther.getSymbolicLength() instanceof CastExpression ?
					((CastExpression) symbOther.getSymbolicLength()).getCasted() :
					symbOther.getSymbolicLength();

			Expression equalLength = new NumericBooleanExpression(left, NumericComparator.EQ, right);
			analysis.decision(ti, null, lengthSat ? 0 : 1, equalLength, new Negation(equalLength));

			if (lengthSat) {
				boolean contentSat = selfConcrete.equals(otherConcrete);
				Expression check = ExpressionUtil.TRUE;
				for (int i = 0; i < selfConcrete.length(); i++) {
					check = ExpressionUtil.and(check,
																		 new NumericBooleanExpression(symbThis.getSymbolicChars()[i],
																																	NumericComparator.EQ,
																																	symbOther.getSymbolicChars()[i]));
				}
				analysis.decision(ti, null, contentSat ? 0 : 1, check, new Negation(check));
				return contentSat;
			} else {
				return false;
			}
		}

		System.out.println("----- one symbolic!");

		SymbolicBVString symb = (symbThis != null) ? symbThis : symbOther;
		if (symb.isFromNumber()) {
			return peer.equals_numbers(env, (symbThis != null) ? objRef : argRef, (symbThis != null) ? argRef : objRef);
		}

		int symbRef = (symbThis != null) ? objRef : argRef;
		int concRef = (symbThis != null) ? argRef : objRef;

		ElementInfo eiConc = ti.getElementInfo(concRef);
		System.out.println("The concret String is: " + eiConc.asString());

		ElementInfo eiSymb = ti.getElementInfo(symbRef);
		String concString = eiConc.asString();
		String symbString = eiSymb.asString();

		Expression length = symb.getSymbolicLength();
		Expression sameLength = new NumericBooleanExpression(length,
																												 NumericComparator.EQ,
																												 new Constant<>(BuiltinTypes.SINT32, concString.length()));

		boolean sat = (concString.length() == symbString.length());
		System.out.println("----- same length: " + sat);
		System.out.println("----- symbString length: " + length);
		System.out.println("----- sat: " + concString.length() + "==" + symbString.length() + "sat: " + sat);
		int branchIdx = sat ? 0 : 1;
		analysis.decision(ti, null, branchIdx, sameLength, new Negation(sameLength));
		if (!sat) {
			return false;
		}
		System.out.println("----- equal length: " + concString.length());

		if (concString.length() == 0) {
			return sat;
		}

		Expression[] chars = symb.getSymbolicChars();
		int idx0 = extractBaseIndexFromExpression(chars[0]);

		Expression[] chareqs = new Expression[concString.length()];
		for (int i = 0; i < concString.length(); i++) {
			Constant<Character> concChar = new Constant<Character>(BuiltinTypes.UINT16, concString.charAt(i));
			chareqs[i] = new NumericBooleanExpression(chars[i], NumericComparator.EQ, concChar);
		}

		Expression equals = ExpressionUtil.and(chareqs);
		System.out.println("--- " + equals);
		sat = peer.super_equals__Ljava_lang_Object_2__Z(env, objRef, argRef);
		branchIdx = sat ? 0 : 1;
		analysis.decision(ti, null, branchIdx, equals, new Negation(equals));
		return sat;
	}

	@MJI
	@SymbolicPeer
	public int concat__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int paramRef) {
		ElementInfo ei1 = env.getElementInfo(objRef);
		ElementInfo ei2 = env.getElementInfo(paramRef);
		int newRes = env.newString(ei1.asString().concat(ei2.asString()));
		SymbolicBVString s1 = ei1.getObjectAttr(SymbolicBVString.class);
		SymbolicBVString s2 = ei2.getObjectAttr(SymbolicBVString.class);
		Expression[] symbolicChars = null;
		Expression length = null;
		if (s1 != null) {
			symbolicChars = s1.getSymbolicChars();
			length = s1.getSymbolicLength();
		}
		if (s2 != null) {
			Expression[] symbolicChars2 = s2.getSymbolicChars();
			Expression[] resulting = new Expression[symbolicChars.length + symbolicChars2.length];
			for (int i = 0; i < symbolicChars.length; i++) {
				resulting[i] = symbolicChars[i];
			}
			int offset = symbolicChars.length;
			for (int i = offset; i < symbolicChars2.length + offset; i++) {
				resulting[i] = symbolicChars2[i - offset];
			}
			if (length == null) {
				length = s2.getSymbolicLength();
			} else {
				length = length.getType().equals(BuiltinTypes.SINT32) && length instanceof CastExpression ?
						((CastExpression) length).getCasted() :
						length;
				Expression right = s2.getSymbolicLength();
				right = right.getType().equals(BuiltinTypes.SINT32) && right instanceof CastExpression ?
						((CastExpression) right).getCasted() :
						length;
				length = new NumericCompound(length, NumericOperator.PLUS, right);
			}
			symbolicChars = resulting;
		}
		if (s1 != null || s2 != null) {
			SymbolicBVString newS = new SymbolicBVString(length, symbolicChars);
			ElementInfo newEI = env.getElementInfo(newRes);
			newEI.addObjectAttr(newS);
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

		SymbolicBVString symbolicSelf = env.getObjectAttr(objref, SymbolicBVString.class);
		SymbolicBVString symbolicOther = env.getObjectAttr(otherString, SymbolicBVString.class);
		boolean concreteResult = self.regionMatches(ignoreCase, toffset, other, ooffset, len);
		if (symbolicSelf == null && symbolicOther == null) {
			//No symbolic parts, so we do not need to update anything in the symbolic model.
			return concreteResult;
		}
		boolean boundsCheck = evaluateBoundsRegionMatches(ooffset,
																											toffset,
																											len,
																											other.length(),
																											self.length(),
																											symbolicOther.getSymbolicLength(),
																											symbolicSelf.getSymbolicLength(),
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
		if (toffset + len >= symbolicSelf.getSymbolicChars().length ||
				ooffset + len >= symbolicOther.getSymbolicChars().length) {
			return false;
		}
		String selfSub = self.substring(toffset, toffset + len);
		String otherSub = self.substring(ooffset, ooffset + len);
		ElementInfo ei = env.getElementInfo(objref);
		MethodInfo equals = ei.getClassInfo().getMethod("equals", "Ljava_lang_String_2", true);
		int branchIdx = concreteResult ? 0 : 1;
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
		SymbolicBVString symbolicSelf = eiSelf.getObjectAttr(SymbolicBVString.class);
		SymbolicBVString symbolicOther = eiOther.getObjectAttr(SymbolicBVString.class);
		boolean concreteResult = self.contains(other);
		if (symbolicOther == null && symbolicSelf == null) {
			return concreteResult;
		} else if (symbolicOther != null && symbolicSelf == null) {
			return evaluateContainsConcreteSelfSymbolicOther(self, other, symbolicOther, ti);
		} else if (symbolicOther == null && symbolicSelf != null) {
			return evaluateContainsSymbolicSelfConcreteOther(self, symbolicSelf, other, ti);
		} else {
			return evaluateContainsSymbolicSelfSymbolicOther(self, symbolicSelf, other, symbolicOther, ti);
		}
	}

	private static boolean computeContainsCheckOneSymbolicOneConcrete(String self,
																																		String other,
																																		Expression[] symbolicChars,
																																		boolean selfSymbolic,
																																		String concreteChars,
																																		boolean lengthCheck,
																																		Expression lengthCheckE,
																																		ThreadInfo ti) {
		ConcolicMethodExplorer ce = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		ce.decision(ti, null, lengthCheck ? 0 : 1, lengthCheckE, new Negation(lengthCheckE));
		if (lengthCheck) {
			Expression all = ExpressionUtil.FALSE;
			for (int i = 0; i < self.length() - other.length(); i++) {
				Expression check = ExpressionUtil.TRUE;
				for (int j = 0; j < other.length(); j++) {
					check = ExpressionUtil.and(check,
																		 new NumericBooleanExpression(symbolicChars[selfSymbolic ? i + j : j],
																																	NumericComparator.EQ,
																																	new Constant<Character>(BuiltinTypes.UINT16,
																																													concreteChars.charAt(
																																															selfSymbolic ?
																																																	i + j :
																																																	j))));
				}
				all = ExpressionUtil.or(all, check);
			}
			boolean concreteContains = self.contains(other);
			ce.decision(ti, null, concreteContains ? 0 : 1, all, new Negation(all));
			return concreteContains;
		} else {
			return false;
		}
	}

	private static boolean evaluateContainsConcreteSelfSymbolicOther(String self,
																																	 String other,
																																	 SymbolicBVString symbolicOther,
																																	 ThreadInfo ti) {
		boolean lengthCheck = self.length() >= other.length();
		Expression lengthCheckE = new NumericBooleanExpression(symbolicOther.getSymbolicLength(),
																													 NumericComparator.LE,
																													 new Constant<Integer>(BuiltinTypes.SINT32, self.length()));
		return computeContainsCheckOneSymbolicOneConcrete(self,
																											other,
																											symbolicOther.getSymbolicChars(),
																											false,
																											self,
																											lengthCheck,
																											lengthCheckE,
																											ti);
	}

	private static boolean evaluateContainsSymbolicSelfConcreteOther(String self,
																																	 SymbolicBVString symbolicSelf,
																																	 String other,
																																	 ThreadInfo ti) {
		boolean lengthCheck = self.length() >= other.length();
		Expression lengthCheckE = new NumericBooleanExpression(symbolicSelf.getSymbolicLength(),
																													 NumericComparator.GE,
																													 new Constant<Integer>(BuiltinTypes.SINT32, other.length()));
		return computeContainsCheckOneSymbolicOneConcrete(self,
																											other,
																											symbolicSelf.getSymbolicChars(),
																											true,
																											other,
																											lengthCheck,
																											lengthCheckE,
																											ti);
	}

	private static boolean evaluateContainsSymbolicSelfSymbolicOther(String self,
																																	 SymbolicBVString symbolicSelf,
																																	 String other,
																																	 SymbolicBVString symbolicOther,
																																	 ThreadInfo ti) {
		ConcolicMethodExplorer ce = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		boolean lengthCheck = self.length() >= other.length();
		Expression lengthCheckE = new NumericBooleanExpression(symbolicSelf.getSymbolicLength(),
																													 NumericComparator.GE,
																													 symbolicOther.getSymbolicLength());
		ce.decision(ti, null, lengthCheck ? 0 : 1, lengthCheckE, new Negation(lengthCheckE));
		boolean lengthZero = self.length() == 0 && other.length() == 0;
		Expression lengthZeroE = new PropositionalCompound(new NumericBooleanExpression(symbolicSelf.getSymbolicLength(),
																																										NumericComparator.EQ,
																																										new Constant<Integer>(BuiltinTypes.SINT32,
																																																					0)),
																											 LogicalOperator.AND,
																											 new NumericBooleanExpression(symbolicOther.getSymbolicLength(),
																																										NumericComparator.EQ,
																																										new Constant<Integer>(BuiltinTypes.SINT32,
																																																					0)));
		ce.decision(ti, null, lengthZero ? 0 : 1, lengthZeroE, new Negation(lengthZeroE));
		if (lengthCheck && !lengthZero) {
			Expression all = ExpressionUtil.FALSE;
			for (int i = 0; i < self.length() - other.length(); i++) {
				Expression check = ExpressionUtil.TRUE;
				for (int j = 0; j < other.length(); j++) {
					check = ExpressionUtil.and(check,
																		 new NumericBooleanExpression(symbolicSelf.getSymbolicChars()[i + j],
																																	NumericComparator.EQ,
																																	symbolicOther.getSymbolicChars()[j]));
				}
				all = ExpressionUtil.or(all, check);
			}
			boolean concreteContains = self.contains(other);
			ce.decision(ti, null, concreteContains ? 0 : 1, all, new Negation(all));
			return concreteContains;
		} else {
			return lengthZero;
		}
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
																													 new Constant<Byte>(BuiltinTypes.SINT8,
																																							(byte) (ooffset + len)));

		boolean lowerOBound = (ooffset + len) < 0;

		boolean lowerTBound = (toffset + len) < 0;

		boolean upperTBound = (toffset + len) > tlen;
		Expression upperTBoundE = new NumericBooleanExpression(tSymLen,
																													 NumericComparator.LT,
																													 new Constant<Byte>(BuiltinTypes.SINT8,
																																							(byte) (toffset + len)));
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

	private static boolean regionMatchBotchSymbolic(SymbolicBVString self,
																									SymbolicBVString other,
																									int soffset,
																									int ooffset,
																									int len,
																									boolean ignoreCase,
																									boolean concreteResult,
																									ThreadInfo ti) {
		//FIXME: We don't integrate ignoreCase currently into the analysis. Think about the possible effects.
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		Expression completeString = ExpressionUtil.TRUE;
		for (int i = 0; i < len; i++) {
			Expression symbolicSelfChar = self.getSymbolicChars()[i + soffset];
			Expression symbolicOtherChar = self.getSymbolicChars()[i + ooffset];
			Expression check = new NumericBooleanExpression(symbolicSelfChar, NumericComparator.EQ, symbolicOtherChar);
			ExpressionUtil.and(completeString, check);
		}
		int branchIndex = concreteResult ? 0 : 1;
		ca.decision(ti, null, branchIndex, completeString, new Negation(completeString));
		return concreteResult;
	}

	private static boolean regionMatchConcreteSymbolicMix(SymbolicBVString symbolic,
																												String other,
																												int soffset,
																												int ooffset,
																												int len,
																												boolean ignoreCase,
																												boolean concreteResult,
																												ThreadInfo ti) {
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		Expression completeString = ExpressionUtil.TRUE;
		for (int i = 0; i < len; i++) {
			Expression symbolicChar = symbolic.getSymbolicChars()[i + soffset];
			Expression check = ExpressionUtil.TRUE;
			if (ignoreCase) {
				char original = other.charAt(ooffset + i);
				char lower = Character.toLowerCase(original);
				char upper = Character.toUpperCase(original);
				Expression lowerCaseConcreteChar = new Constant(BuiltinTypes.UINT16, lower);
				Expression upperCaseConcreteChar = new Constant(BuiltinTypes.UINT16, upper);
				check =
						ExpressionUtil.or(new NumericBooleanExpression(symbolicChar, NumericComparator.EQ, lowerCaseConcreteChar),
															new NumericBooleanExpression(symbolicChar, NumericComparator.EQ, upperCaseConcreteChar));
			} else {
				Expression concreteChar = new Constant(BuiltinTypes.UINT16, other.charAt(ooffset + i));
				check = new NumericBooleanExpression(symbolicChar, NumericComparator.EQ, concreteChar);
			}
			ExpressionUtil.and(completeString, check);
		}
		int branchIndex = concreteResult ? 0 : 1;
		ca.decision(ti, null, branchIndex, completeString, new Negation(completeString));
		return concreteResult;
	}

	@MJI
	public static int local_valueOf__D__Ljava_lang_String_2(MJIEnv env, int objRef, double value) {
		Object[] attrs = env.getArgAttributes();
		String res = String.valueOf(value);
		if (attrs != null) {
			for (Object o : attrs) {
				if (o instanceof SymbolicNumber) {
					SymbolicNumber sn = (SymbolicNumber) o;
					ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
					ConcolicUtil.Pair<Integer> length = ca.getOrCreateSymbolicInt8();
					Expression[] symbolicChars = new Expression[res.length()];
					for (int i = 0; i < res.length(); i++) {
						symbolicChars[i] = ca.getOrCreateSymbolicByte().symb;
					}
					SymbolicBVString symbolicRes = new SymbolicBVString(length.symb, symbolicChars);
					symbolicRes.setIsFromNumber(true);
					symbolicRes.setSymbolicNumber(sn);
					env.setReturnAttribute(symbolicRes);
				}
			}
		}
		return env.newString(res);
	}

	public static int local_valueOf__C__Ljava_lang_String_2(MJIEnv env, int objRef, char value) {
		Object[] attrs = env.getArgAttributes();
		String res = String.valueOf(value);
		if (attrs != null) {
			for (Object o : attrs) {
				if (o instanceof SymbolicNumber) {
					SymbolicNumber sn = (SymbolicNumber) o;
					ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
					ConcolicUtil.Pair<Integer> length = ca.getOrCreateSymbolicInt8();
					Expression[] symbolicChars = new Expression[res.length()];
					for (int i = 0; i < res.length(); i++) {
						symbolicChars[i] = ca.getOrCreateSymbolicByte().symb;
					}
					SymbolicBVString symbolicRes = new SymbolicBVString(length.symb, symbolicChars);
					symbolicRes.setIsFromNumber(true);
					symbolicRes.setSymbolicNumber(sn);
					env.setReturnAttribute(symbolicRes);
				}
			}
		}
		return env.newString(res);
	}

	@MJI
	public int substring__I__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex) {
		SymbolicBVString symbolicValue = env.getObjectAttr(objRef, SymbolicBVString.class);
		if (symbolicValue == null) {
			return peer.super_substring__I__Ljava_lang_String_2(env, objRef, beginIndex);
		}
		if (env.getArgAttributes() != null && env.getArgAttributes()[1] != null) {
			System.out.println("JPF_java_lang_String.substring__I__Ljava_lang_String_2");
			System.out.println("Cannot deal with symbolic values in args");
			env.throwException("error.Assume");
			return -1;
		}
		if (beginIndex < 0) {
			env.throwException("java.lang.IndexOutOfBoundsException");
			return -1;
		}
		String concreteValue = env.getStringObject(objRef);
		Expression upperBound = new NumericBooleanExpression(new Constant<>(BuiltinTypes.SINT32, beginIndex),
																												 NumericComparator.GT,
																												 symbolicValue.getSymbolicLength());
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		ca.decision(env.getThreadInfo(),
								null,
								beginIndex > concreteValue.length() ? 0 : 1,
								upperBound,
								new Negation(upperBound));
		if (beginIndex > concreteValue.length()) {
			env.throwException("java.lang.IndexOutOfBoundsException");
			return -1;
		}
		int resValue = env.newString(concreteValue.substring(beginIndex));
		ArrayList<Expression> symbolicChars = new ArrayList<>();
		for (int i = beginIndex; i < concreteValue.length(); i++) {
			symbolicChars.add(symbolicValue.getSymbolicChars()[i]);
		}
		ConcolicUtil.Pair<Integer> symbolicLength = ca.getOrCreateSymbolicInt8();
		env.addObjectAttr(resValue, new SymbolicBVString(symbolicLength.symb, symbolicChars.toArray(new Expression[0])));
		return resValue;
	}

	@MJI
	@SymbolicPeer
	public int substring__II__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex, int endIndex) {
		SymbolicBVString symbolicValue = env.getObjectAttr(objRef, SymbolicBVString.class);
		if (symbolicValue == null) {
			return peer.super_substring__II__Ljava_lang_String_2(env, objRef, beginIndex, endIndex);
		}
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
		String concreteValue = env.getStringObject(objRef);
		Expression upperBound = new NumericBooleanExpression(new Constant<>(BuiltinTypes.SINT32, endIndex),
																												 NumericComparator.GT,
																												 symbolicValue.getSymbolicLength());
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		ca.decision(env.getThreadInfo(),
								null,
								beginIndex > concreteValue.length() ? 0 : 1,
								upperBound,
								new Negation(upperBound));
		if (endIndex > concreteValue.length() || beginIndex > endIndex) {
			env.throwException("java.lang.IndexOutOfBoundsException");
			return -1;
		}
		int resValue = env.newString(concreteValue.substring(beginIndex, endIndex));
		ArrayList<Expression> symbolicChars = new ArrayList<>();
		for (int i = beginIndex; i < concreteValue.length(); i++) {
			symbolicChars.add(symbolicValue.getSymbolicChars()[i]);
		}
		ConcolicUtil.Pair<Integer> symbolicLength = ca.getOrCreateSymbolicInt8();
		env.addObjectAttr(resValue, new SymbolicBVString(symbolicLength.symb, symbolicChars.toArray(new Expression[0])));
		return resValue;
	}

	@MJI
	@SymbolicPeer
	public int split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env, int objRef, int rString0) {
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		ElementInfo ei = env.getElementInfo(objRef);
		SymbolicBVString symbolic = ei.getObjectAttr(SymbolicBVString.class);
		if (symbolic != null) {
			String concreteValue = ei.asString();
			ConcolicUtil.Pair<Integer> arrayLength = ca.getOrCreateSymbolicInt8();
			Expression lengthConstraint =
					new PropositionalCompound(new NumericBooleanExpression(symbolic.getSymbolicLength(),
																																													 NumericComparator.GT,
																																													 arrayLength.symb),
																															LogicalOperator.AND,
																															new NumericBooleanExpression(arrayLength.symb,
																																													 NumericComparator.GE,
																																													 new Constant(BuiltinTypes.SINT8,
																																																				0)));

			ca.decision(env.getThreadInfo(),
									null,
									concreteValue.length() > arrayLength.conc ? 0 : 1,
									lengthConstraint,
									new Negation(lengthConstraint));
			if (concreteValue.length() > arrayLength.conc && arrayLength.conc > 0) {
				int arrayRef = env.newObjectArray("java.lang.String", arrayLength.conc);
				int[] fields = env.getReferenceArrayObject(arrayRef);
				int remaining = concreteValue.length();
				for (int j = 0; j < fields.length; j++) {
					Random r = new Random();
					int upperBound = remaining - (fields.length - j - 1);
					int length = r.nextInt(upperBound);
					remaining -= length;
					byte[] content = new byte[length];
					r.nextBytes(content);
					String subString = new String(content);
					ArrayList<Expression> symbolicChars = new ArrayList<>();
					for (int i = 0; i < subString.length(); i++) {
						symbolicChars.add(ca.getOrCreateSymbolicChar().symb);
					}
					ConcolicUtil.Pair<Integer> newLength = ca.getOrCreateSymbolicInt8();
					int ref = env.newString(subString);
					fields[j] = ref;
					env.addObjectAttr(ref, new SymbolicBVString(newLength.symb, symbolicChars.toArray(new Expression[0])));
				}

				env.setObjectAttr(arrayRef, arrayLength.symb);
				return arrayRef;
			} else {
				int arrayRef = env.newObjectArray("java.lang.String", 1);
				int[] fields = env.getReferenceArrayObject(arrayRef);
				fields[0] = objRef;
				return arrayRef;
			}
		} else {
			return peer.super_split__Ljava_lang_String_2___3Ljava_lang_String_2(env, objRef, rString0);
		}
	}

	private static int extractBaseIndexFromExpression(Expression symbolicChar) {
		Set<Variable<?>> vars = ExpressionUtil.freeVariables(symbolicChar);
		if (vars.size() != 1) {
			throw new SymbolicModellingError(
					"Cannot determine base index for char if not exactly one variable in the expression");
		}
		String value = "-1";
		for (Variable v : vars) {
			String name = v.getName();
			if (name.contains("_byte")) {
				value = name.replace("_byte", "");
			} else if (name.contains("_char")) {
				value = name.replace("_char", "");
			} else {
				throw new SymbolicModellingError("No _byte or _char in variable name.");
			}
		}
		return Integer.parseInt(value);
	}
}
