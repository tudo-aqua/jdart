package gov.nasa.jpf.jdart.peers.strings;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.peers.JPF_java_lang_StringBuilder;
import gov.nasa.jpf.vm.MJIEnv;

public class SMTLibStringBuilderModel {

	private final JPF_java_lang_StringBuilder peer;

	public SMTLibStringBuilderModel(JPF_java_lang_StringBuilder peer) {
		this.peer = peer;
	}

	@MJI
	@SymbolicPeer
	/*
	 * gets always implicitly called by string concatenation via + operator
	 */ public int append__Ljava_lang_String_2__Ljava_lang_StringBuilder_2(MJIEnv env, int objref, int sref) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	@MJI
	@SymbolicPeer
	public char charAt__I__C(MJIEnv env, int objref, int index) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	@MJI
	@SymbolicPeer
	public void setCharAt__IC__V(MJIEnv env, int objref, int index, char character) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}

	public void getChars__II_3CI__V(MJIEnv env, int objref, int start, int end, int dest, int dest_start) {
		throw new UnsupportedOperationException("SMTLib String Model not yet supported");
	}
}
