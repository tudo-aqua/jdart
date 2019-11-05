package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicString;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;

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
	@Override
	/*
	 * gets always implicitly called by string concatenation via + operator
	 */ public int append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(MJIEnv env, int objref, int sref) {
		ElementInfo ei = env.getElementInfo(sref);
		if (ei == null || ei.isNull()) {
			return super.append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);
		}
		SymbolicString symb = ei.getObjectAttr(SymbolicString.class);
		//test for symbolic string
		if (symb != null) {
			env.addObjectAttr(objref, symb);
		}

		ElementInfo debug = env.getElementInfo(objref);
		return super.append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);
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

}
