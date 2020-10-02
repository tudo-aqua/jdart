package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.expressions.StringIntegerExpression;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.config.ConcolicConfig;
import gov.nasa.jpf.jdart.objects.SymbolicBVString;
import gov.nasa.jpf.jdart.objects.SymbolicSMTString;
import gov.nasa.jpf.jdart.objects.SymbolicString;
import gov.nasa.jpf.jdart.peers.strings.BitVectorStringBuilderModel;
import gov.nasa.jpf.jdart.peers.strings.SMTLibStringBuilderModel;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;

public class JPF_java_lang_StringBuilder extends gov.nasa.jpf.vm.JPF_java_lang_StringBuilder {
	private final BitVectorStringBuilderModel bvStringPeer;
	private final SMTLibStringBuilderModel smtStringPeer;

	public JPF_java_lang_StringBuilder() {
		this.bvStringPeer = new BitVectorStringBuilderModel(this);
		this.smtStringPeer = new SMTLibStringBuilderModel(this);
	}

	private static String errorMessage = "Cannot resolve the StringBuilder Peer";

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
		if (symbolic != null) {
			if (symbolic instanceof SymbolicSMTString) {
				StringIntegerExpression symbolicLength =
						StringIntegerExpression.createLength(((SymbolicSMTString) symbolic).strVar);
				env.setReturnAttribute(symbolicLength);
			} else if (symbolic instanceof SymbolicBVString) {
				env.setReturnAttribute(((SymbolicBVString) symbolic).getSymbolicLength());
			} else {
				throw new UnsupportedOperationException("Don't know this Symbolic String type");
			}
		}
		return length;
	}

	@MJI
	@SymbolicPeer
	@Override
	/*
	 * gets always implicitly called by string concatenation via + operator
	 */ public int append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(MJIEnv env, int objref, int sref) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);
			case SMTLibModel:
				return smtStringPeer.append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(env, objref, sref);
			default:
				throw new IllegalArgumentException("Cannot resolve the String model used inside StringBuilder append" + ".\n" +
																					 "Got unsupported model class: " + sm.getClass());
		}
	}

	public int super_append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(MJIEnv env, int objref, int sref) {
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
	public char charAt__I__C(MJIEnv env, int objref, int index) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				return bvStringPeer.charAt__I__C(env, objref, index);
			case SMTLibModel:
				return smtStringPeer.charAt__I__C(env, objref, index);
			default:
				throw new IllegalArgumentException("Cannot resolve the String model used inside StringBuilder append" + ".\n" +
																					 "Got unsupported model class: " + sm.getClass());
		}
	}

	@MJI
	@SymbolicPeer
	public void getChars__II_3CI__V(MJIEnv env, int objref, int start, int end, int dest, int dest_start) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				bvStringPeer.getChars__II_3CI__V(env, objref, start, end, dest, dest_start);
				break;
			case SMTLibModel:
				smtStringPeer.getChars__II_3CI__V(env, objref, start, end, dest, dest_start);
				break;
			default:
				throw new IllegalArgumentException("Cannot resolve the String model used inside StringBuilder append" + ".\n" +
																					 "Got unsupported model class: " + sm.getClass());
		}
	}

	@MJI
	@SymbolicPeer
	public void setCharAt__IC__V(MJIEnv env, int objref, int index, char character) {
		ConcolicConfig.StringModel sm = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).getStringModel();
		switch (sm) {
			case BVModel:
				bvStringPeer.setCharAt__IC__V(env, objref, index, character);
				break;
			case SMTLibModel:
				smtStringPeer.setCharAt__IC__V(env, objref, index, character);
				break;
			default:
				throw new IllegalArgumentException("Cannot resolve the String model used inside StringBuilder append" + ".\n" +
																					 "Got unsupported model class: " + sm.getClass());
		}
	}
}
