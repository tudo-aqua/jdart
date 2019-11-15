package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicString;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;

public class JPF_java_util_Scanner extends NativePeer {

	@MJI
	@SymbolicPeer
	public void $init__Ljava_lang_String_2__V(MJIEnv env, int objref, int stringRef) {
		ElementInfo eiString = env.getElementInfo(stringRef);
		SymbolicString symbolicString = eiString.getObjectAttr(SymbolicString.class);
		if (symbolicString != null) {
			env.addObjectAttr(objref, symbolicString);
		}
	}

	@MJI
	@SymbolicPeer
	public int nextInt____I(MJIEnv env, int objref) {
		SymbolicString symbolic = env.getObjectAttr(objref, SymbolicString.class);
		if (symbolic != null) {
			ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
			ConcolicUtil.Pair<Integer> res = ca.getOrCreateSymbolicInt();
			env.setReturnAttribute(res.symb);
			return res.conc;
		}
		System.out.println("Scanner is not symbolic");
		env.throwException("errors.Assume");
		return MJIEnv.NULL;
	}
}
