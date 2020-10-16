package gov.nasa.jpf.jdart.peers;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.objects.SymbolicSMTString;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;
public class JPF_org_sosy_lab_sv_benchmarks_Verifier extends NativePeer {
@MJI
public int nondetString____Ljava_lang_String_2(MJIEnv env, int objRef) {
ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
ConcolicUtil.Pair<String> symbolicString = ca.getOrCreateSymbolicString();
System.out.println("Created nondetString: "+ symbolicString.conc + " with value: " +symbolicString.symb);
int res = env.newString(symbolicString.conc);
SymbolicSMTString smtString = new SymbolicSMTString((Variable<String>) symbolicString.symb, symbolicString.symb);
env.addObjectAttr(res, smtString);
return res;
}
}
