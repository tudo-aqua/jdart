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

import gov.nasa.jpf.Config;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class JPF_java_util_Random extends gov.nasa.jpf.vm.JPF_java_util_Random {

	public JPF_java_util_Random(Config conf) {
		super(conf);
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean nextBoolean____Z(MJIEnv env, int objRef) {
		ThreadInfo ti = env.getThreadInfo();
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		if (ca == null) {
			return super.nextBoolean____Z(env, objRef);
		}

		ConcolicUtil.Pair<Boolean> result = ca.getOrCreateSymbolicBoolean();
		env.setReturnAttribute(result.symb);
		return result.conc;
	}

	@MJI
	@SymbolicPeer
	@Override
	public void nextBytes___3B__V(MJIEnv env, int objRef, int dataRef) {
		ThreadInfo ti = env.getThreadInfo();
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		if (ca == null) {
			super.nextBytes___3B__V(env, objRef, dataRef);
		}

		ElementInfo ei = env.getElementInfo(dataRef);
		for (int i = 0; i < ei.arrayLength(); i++) {
			ConcolicUtil.Pair<Byte> newByte = ca.getOrCreateSymbolicByte();
			ei.setByteElement(i, newByte.conc);
			ei.addElementAttr(i, newByte.symb);
		}
	}

	@MJI
	@SymbolicPeer
	@Override
	public int nextInt____I(MJIEnv env, int objRef) {
		ThreadInfo ti = env.getThreadInfo();
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(ti);
		if (ca == null) {
			return super.nextInt____I(env, objRef);
		}

		StackFrame sf = env.getThreadInfo().getTopFrame();
		MethodInfo mi = sf.getPrevious().getMethodInfo();
		ConcolicUtil.Pair<Integer> result = ca.getOrCreateSymbolicInt();
		if (mi.getName().startsWith("nondetString") &&
			mi.getClassInfo().getName().startsWith("org.sosy_lab.sv_benchmarks.Verifier")) {
			result = ca.getOrCreateSymbolicInt8();
		}

		env.setReturnAttribute(result.symb);
		return result.conc;
	}
}
