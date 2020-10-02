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
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicSMTString;
import gov.nasa.jpf.jdart.peers.JPF_java_lang_String;
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
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	@MJI
	@SymbolicPeer
	public int length____I(MJIEnv env, int objRef) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	@MJI
	@SymbolicPeer
	public char charAt__I__C(MJIEnv env, int objRef, int index) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	@MJI
	@SymbolicPeer
	public boolean equals__Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int argRef) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	@MJI
	@SymbolicPeer
	public int concat__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int paramRef) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
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
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	private boolean evaluateContainsConcreteSelfSymbolicOther(String self,
																														String other,
																														SymbolicSMTString symbolicOther,
																														ConcolicMethodExplorer analysis,
																														ThreadInfo ti) {

		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	private boolean evaluateContainsSymbolicSelfConcreteOther(String self,
																														SymbolicSMTString symbolicSelf,
																														String other,
																														ConcolicMethodExplorer analysis,
																														ThreadInfo ti) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	private boolean evaluateContainsSymbolicSelfSymbolicOther(String self,
																														SymbolicSMTString symbolicSelf,
																														String other,
																														SymbolicSMTString symbolicOther,
																														ConcolicMethodExplorer analysis,
																														ThreadInfo ti) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}


	@MJI
	public static int local_valueOf__D__Ljava_lang_String_2(MJIEnv env, int objRef, double value) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}


	@MJI
	public int substring__I__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	@MJI
	@SymbolicPeer
	public int substring__II__Ljava_lang_String_2(MJIEnv env, int objRef, int beginIndex, int endIndex) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	@MJI
	@SymbolicPeer
	public int split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env, int objRef, int rString0) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}
}
