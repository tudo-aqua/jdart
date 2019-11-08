/*
 * Copyright 2019 TU Dortmund, Falk Howar (@fhowar)
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
package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicString;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;

public class JPF_java_lang_String extends gov.nasa.jpf.vm.JPF_java_lang_String {

	@MJI
	@SymbolicPeer
	@Override
	public int init___3BII__Ljava_lang_String_2(MJIEnv env, int objRef, int bytesRef, int offset, int length) {
		ElementInfo eiSource = env.getElementInfo(bytesRef);
		Expression symbLength = eiSource.getObjectAttr(Expression.class);
		Expression[] symbChars = new Expression[eiSource.arrayLength()];
		for (int i = 0; i < eiSource.arrayLength(); i++) {
			symbChars[i] = eiSource.getElementAttr(i, Expression.class);
		}
		env.setObjectAttr(objRef, new SymbolicString(symbLength, symbChars));
		return super.init___3BII__Ljava_lang_String_2(env, objRef, bytesRef, offset, length);
	}

	@MJI
	@SymbolicPeer
	public int length____I(MJIEnv env, int objRef) {
		ElementInfo ei = env.getElementInfo(objRef);
		String concrete = ei.asString();
		int result = concrete.length();

		if (ei.hasObjectAttr(SymbolicString.class)) {
			SymbolicString symbolic = ei.getObjectAttr(SymbolicString.class);
			Expression length = symbolic.getSymbolicLength();
			env.setReturnAttribute(length);

		}
		return result;
	}

	@MJI
	@SymbolicPeer
	@Override
	public char charAt__I__C(MJIEnv env, int objRef, int index) {
		ThreadInfo ti = env.getThreadInfo();

		ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		ElementInfo eiThis = env.getElementInfo(objRef);
		SymbolicString symb = env.getObjectAttr(objRef, SymbolicString.class);
		Expression symbIndex = null;

		if (env.getArgAttributes() != null) {
			symbIndex = (Expression) env.getArgAttributes()[1];
		}
		if (analysis == null || (symb == null && symbIndex == null)) {
			return super.charAt__I__C(env, objRef, index);
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
												   new NumericBooleanExpression(symbIndex,
																				NumericComparator.LT,
																				length));

		boolean sat = (0 <= index) && (index < eiThis.asString().length());
		int branchIdx = sat ? 0 : 1;
		analysis.decision(ti, null, branchIdx, constraint, new Negation(constraint));

		try {
			env.setReturnAttribute(symb.getSymbolicChars()[index]);
			return super.charAt__I__C(env, objRef, index);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			env.throwException(ArrayIndexOutOfBoundsException.class.getName(), e.getMessage());
			return MJIEnv.NULL;
		}
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean equals__Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int argRef) {

		ThreadInfo ti = env.getThreadInfo();
		ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		System.out.println("----- Symbolic String equals");
		SymbolicString symbThis = env.getObjectAttr(objRef, SymbolicString.class);
		SymbolicString symbOther = env.getObjectAttr(argRef, SymbolicString.class);

		if (analysis == null || (symbThis == null && symbOther == null)) {
			System.out.println("----- concrete, " + (analysis == null));
			return super.equals__Ljava_lang_Object_2__Z(env, objRef, argRef);
		}
		System.out.println("----- symbolic");

		if (symbThis != null && symbOther != null) {
			System.out.println("----- both symbolic");
			//TODO: Build a real model to deal with symbolic strings.

			// can't deal with two symbolic strings currently
			//env.throwException("errors.NoModel");
			return true;
		}

		System.out.println("----- one symbolic!");

		SymbolicString symb = (symbThis != null) ? symbThis : symbOther;
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
		Variable v = (Variable) chars[0];
		int idx0 = Integer.parseInt(v.getName().replace("_byte", ""));

		Expression[] chareqs = new Expression[concString.length()];
		for (int i = 0; i < concString.length(); i++) {

			chareqs[i] = new NumericBooleanExpression(new Variable<>(BuiltinTypes.SINT8, "_byte" + (idx0 + i)),
													  NumericComparator.EQ,
													  new Constant<>(BuiltinTypes.SINT8, (byte) concString.charAt(i)));
		}

		Expression equals = ExpressionUtil.and(chareqs);
		System.out.println("--- " + equals);
		sat = super.equals__Ljava_lang_Object_2__Z(env, objRef, argRef);
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
		SymbolicString s1 = ei1.getObjectAttr(SymbolicString.class);
		SymbolicString s2 = ei2.getObjectAttr(SymbolicString.class);
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
				length = new NumericCompound(length, NumericOperator.PLUS, s2.getSymbolicLength());
			}
			symbolicChars = resulting;
		}
		if (s1 != null || s2 != null) {
			SymbolicString newS = new SymbolicString(length, symbolicChars);
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
			ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).completePathDontKnow(env.getThreadInfo());
			return false;
		}

		String self = env.getStringObject(objref);
		String other = env.getStringObject(otherString);

		SymbolicString symbolicSelf = env.getObjectAttr(objref, SymbolicString.class);
		SymbolicString symbolicOther = env.getObjectAttr(otherString, SymbolicString.class);
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

	private static boolean regionMatchBotchSymbolic(SymbolicString self,
													SymbolicString other,
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

	private static boolean regionMatchConcreteSymbolicMix(SymbolicString symbolic,
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
				check = ExpressionUtil.or(new NumericBooleanExpression(symbolicChar,
																	   NumericComparator.EQ,
																	   lowerCaseConcreteChar),
										  new NumericBooleanExpression(symbolicChar,
																	   NumericComparator.EQ,
																	   upperCaseConcreteChar));
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
}
