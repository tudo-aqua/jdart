package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;

public class JPF_java_net_URLDecoder extends NativePeer {

  @MJI
  @SymbolicPeer
  public int decode__Ljava_lang_String_2Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env,
      int objRef, int toDecodeRef, int codecRef) {
    return toDecodeRef;
  }
}
