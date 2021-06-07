package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;

public class JPF_java_net_Socket extends NativePeer {

	@MJI
	public void $init__Ljava_lang_String_2I__V(MJIEnv env, int objRef, int hostRef, int port) {

	}

	@MJI
	public int getInputStream____Ljava_io_InputStream_2(MJIEnv env, int objRef) {
		int objectRef = env.newObject("mocks.java.net.InputStream_Mock");
		return objectRef;
	}

	@MJI
	public void close____V(MJIEnv env, int objRef) {
		
	}

}
