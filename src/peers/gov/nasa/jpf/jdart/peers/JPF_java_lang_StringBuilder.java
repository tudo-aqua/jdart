package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.api.Expression;
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
	public int length____I(MJIEnv env, int objref) {
		ElementInfo ei = env.getElementInfo(objref);
		SymbolicString symbolic = ei.getObjectAttr(SymbolicString.class);
		int length = env.getIntField(objref, "count");
		env.setReturnAttribute(symbolic.getSymbolicLength());
		return length;
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

	@MJI
	@SymbolicPeer
	public int charAt__I__C(MJIEnv env, int objref, int index) {
		SymbolicString symbolic = env.getObjectAttr(objref, SymbolicString.class);
		int field = env.getReferenceField(objref, "value");
		char[] values = env.getCharArrayObject(field);
		if (symbolic != null) {
			if (symbolic.getSymbolicChars().length == 0 ||
				!(index >= 0 && index < symbolic.getSymbolicChars().length)) {
				env.throwException("java.lang.IndexOutOfBoundsException");
			} else {
				env.setReturnAttribute(symbolic.getSymbolicChars()[index]);
				return (char) values[index];
			}
		} else {
			if (index > 0 && index < values.length) {
				return (char) values[index];
			} else {
				env.throwException("java.lang.IndexOutOfBoundsException");
			}
		}
		return (char) 0;
	}

	@MJI
	@SymbolicPeer
	public void getChars__II_3CI__V(MJIEnv env, int objref, int start, int end, int dest, int dest_start) {
		ElementInfo eiSB = env.getElementInfo(objref);
		ElementInfo eiCA = env.getElementInfo(dest);
		int maxSize = env.getIntField(objref, "count");
		int charArrayRef = env.getReferenceField(objref, "value");
		char[] values = env.getCharArrayObject(charArrayRef);
		char[] destChar = eiCA.asCharArray();
		if (end > maxSize || start < 0 || dest_start < 0 || start > end ||
			(dest_start + end - start) > destChar.length) {
			env.throwException("java.lang.IndexOutOfBoundsException");
		} else {
			for (int i = 0; i + start < end; i++) {
				destChar[dest_start + i] = values[i + start];
			}
		}
		SymbolicString symbolic = eiSB.getObjectAttr(SymbolicString.class);
		if (symbolic != null) {
			Expression[] symbolicChars = new Expression[dest_start + end - start];
			for (int i = 0; i + start < end; i++) {
				symbolicChars[dest_start + i] = symbolic.getSymbolicChars()[i + start];
			}
			eiCA.setObjectAttr(new SymbolicString(symbolic.getSymbolicLength(), symbolicChars));
		}
	}
}
