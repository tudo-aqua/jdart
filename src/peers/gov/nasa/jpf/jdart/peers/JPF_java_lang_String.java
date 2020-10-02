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
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.config.ConcolicConfig;
import gov.nasa.jpf.jdart.objects.SymbolicBVString;
import gov.nasa.jpf.jdart.peers.strings.BitVectorStringModel;
import gov.nasa.jpf.jdart.peers.strings.SMTLibStringModel;
import gov.nasa.jpf.vm.MJIEnv;

public class JPF_java_lang_String extends gov.nasa.jpf.vm.JPF_java_lang_String {

	private final BitVectorStringModel bvStringPeer;
	private final SMTLibStringModel smtStringPeer;

	private static String errorMessage = "Cannot resolve the String Peer";

	public JPF_java_lang_String() {
		this.bvStringPeer = new BitVectorStringModel(this);
		this.smtStringPeer = new SMTLibStringModel(this);
	}

	@MJI
	@SymbolicPeer
	@Override
	public int init___3BII__Ljava_lang_String_2(MJIEnv env, int objRef, int bytesRef, int offset, int length) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.init___3BII__Ljava_lang_String_2(env, objRef, bytesRef, offset, length);
			case SMTLibModel:
				return smtStringPeer.init___3BII__Ljava_lang_String_2(env, objRef, bytesRef, offset, length);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
	}

	public int super_init___3BII(MJIEnv env, int objRef, int bytesRef, int offset, int length) {
		return super.init___3BII__Ljava_lang_String_2(env, objRef, bytesRef, offset, length);
	}

	@MJI
	@SymbolicPeer
	public int length____I(MJIEnv env, int objRef) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.length____I(env, objRef);
			case SMTLibModel:
				return smtStringPeer.length____I(env, objRef);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
	}

	@MJI
	@SymbolicPeer
	@Override
	public char charAt__I__C(MJIEnv env, int objRef, int index) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.charAt__I__C(env, objRef, index);
			case SMTLibModel:
				return smtStringPeer.charAt__I__C(env, objRef, index);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
	}

	public char super_charAt__I__C(MJIEnv env, int objRef, int index) {
		return super.charAt__I__C(env, objRef, index);
	}

	public boolean equals_numbers(MJIEnv env, int objRef, int argRef) {
		//argRef is expected to be a concrete String here.
		SymbolicBVString symbolic = env.getObjectAttr(objRef, SymbolicBVString.class);
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		if (symbolic.getSymbolicNumber().hasBeenConstrained) {
			//FIXME: Cannot deal with constrained double yet.
			ca.completePathDontKnow(env.getThreadInfo());
			return false;
		}
		String otherNumber = env.getStringObject(argRef);
		String self = env.getStringObject(objRef);
		if (self == null || otherNumber == null) {
			env.throwException("java.lang.NullPointerException");
		}
		try {
			double otherD = Double.parseDouble(otherNumber);
			double selfD = Double.parseDouble(self);
			Expression equal = new NumericBooleanExpression(new Constant<Double>(BuiltinTypes.DOUBLE, otherD),
																											NumericComparator.EQ,
																											symbolic.getSymbolicNumber().symbolicNumber);
			Expression notEqual = new NumericBooleanExpression(new Constant<Double>(BuiltinTypes.DOUBLE, otherD),
																												 NumericComparator.NE,
																												 symbolic.getSymbolicNumber().symbolicNumber);
			ca.decision(env.getThreadInfo(), null, otherD == selfD ? 0 : 1, equal, notEqual);
			return otherD == selfD;
		}
		catch (NumberFormatException e) {
			env.throwException("java.lang.NumberFormatException");
		}
		return false;
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean equals__Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int argRef) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.equals__Ljava_lang_Object_2__Z(env, objRef, argRef);
			case SMTLibModel:
				return smtStringPeer.equals__Ljava_lang_Object_2__Z(env, objRef, argRef);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
	}

	public boolean super_equals__Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int argRef) {
		return super.equals__Ljava_lang_Object_2__Z(env, objRef, argRef);
	}

	@MJI
	@SymbolicPeer
	public int concat__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int paramRef) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.concat__Ljava_lang_String_2__Ljava_lang_String_2(env, objRef, paramRef);
			case SMTLibModel:
				return smtStringPeer.concat__Ljava_lang_String_2__Ljava_lang_String_2(env, objRef, paramRef);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
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
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.regionMatches__ZILjava_lang_String_2II__Z(env,
																																			objref,
																																			ignoreCase,
																																			toffset,
																																			otherString,
																																			ooffset,
																																			len);
			case SMTLibModel:
				return smtStringPeer.regionMatches__ZILjava_lang_String_2II__Z(env,
																																			 objref,
																																			 ignoreCase,
																																			 toffset,
																																			 otherString,
																																			 ooffset,
																																			 len);
			default:
				throw new UnsupportedOperationException("Cannot resolve the String Peer");
		}
	}

	@MJI
	@SymbolicPeer
	public boolean contains__Ljava_lang_CharSequence_2__Z(MJIEnv env, int objRef, int charSequenceRef) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.contains__Ljava_lang_CharSequence_2__Z(env, objRef, charSequenceRef);
			case SMTLibModel:
				return smtStringPeer.contains__Ljava_lang_CharSequence_2__Z(env, objRef, charSequenceRef);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
	}


	@MJI
	@SymbolicPeer
	public int valueOf__D__Ljava_lang_String_2(MJIEnv env, int objRef, double value) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return BitVectorStringModel.local_valueOf__D__Ljava_lang_String_2(env, objRef, value);
			case SMTLibModel:
				return SMTLibStringModel.local_valueOf__D__Ljava_lang_String_2(env, objRef, value);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
	}

	@MJI
	@Override
	public int substring__I__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.substring__I__Ljava_lang_String_2(env, objRef, beginIndex);
			case SMTLibModel:
				return smtStringPeer.substring__I__Ljava_lang_String_2(env, objRef, beginIndex);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
	}

	public int super_substring__I__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex) {
		return super.substring__I__Ljava_lang_String_2(env, objRef, beginIndex);
	}

	@MJI
	@Override
	@SymbolicPeer
	public int substring__II__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex, int endIndex) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.substring__II__Ljava_lang_String_2(env, objRef, beginIndex, endIndex);
			case SMTLibModel:
				return smtStringPeer.substring__II__Ljava_lang_String_2(env, objRef, beginIndex, endIndex);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
	}

	public int super_substring__II__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex, int endIndex) {
		return super.substring__II__Ljava_lang_String_2(env, objRef, beginIndex, endIndex);
	}

	@MJI
	@Override
	@SymbolicPeer
	public int split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env, int objRef, int rString0) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.split__Ljava_lang_String_2___3Ljava_lang_String_2(env, objRef, rString0);
			case SMTLibModel:
				return smtStringPeer.split__Ljava_lang_String_2___3Ljava_lang_String_2(env, objRef, rString0);
			default:
				throw new UnsupportedOperationException(errorMessage);
		}
	}

	public int super_split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env, int objRef, int rString0) {
		return super.split__Ljava_lang_String_2___3Ljava_lang_String_2(env, objRef, rString0);
	}
}
