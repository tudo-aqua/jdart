package gov.nasa.jpf.jdart.peers;

import dk.brics.automaton.RegExpConverter;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.expressions.AbstractRegExExpression;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicBVString;
import gov.nasa.jpf.jdart.objects.SymbolicMatcher;
import gov.nasa.jpf.jdart.objects.SymbolicSMTString;
import gov.nasa.jpf.jdart.objects.SymbolicString;
import gov.nasa.jpf.jdart.peers.strings.PatternAndMatcherStore;
import gov.nasa.jpf.vm.MJIEnv;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JPF_java_util_regex_Pattern extends gov.nasa.jpf.vm.JPF_java_util_regex_Pattern {

	@MJI
	@SymbolicPeer
	public static int compile__Ljava_lang_String_2__Ljava_util_regex_Pattern_2(MJIEnv env, int objref, int patternRef) {
		String pattern = env.getStringObject(patternRef);
		Pattern newP = Pattern.compile(pattern);
		int newPID = env.newObject("java.util.regex.Pattern");
		PatternAndMatcherStore.getStore().putPattern(newPID, newP);

		SymbolicString symbolicString = env.getObjectAttr(patternRef, SymbolicString.class);
		if (symbolicString != null) {
			env.throwException("errors.Assume", "Cannot deal with symbolic pattern");
			return -1;
		}
		AbstractRegExExpression reToString = RegExpConverter.toSMTExpression(pattern);
		env.setObjectAttr(newPID, reToString);
		return newPID;
	}

	@MJI
	@SymbolicPeer
	public int matcher__Ljava_lang_CharSequence_2__Ljava_util_regex_Matcher_2(MJIEnv env, int objRef, int paramString) {
		String cs = env.getStringObject(paramString);
		Pattern p = PatternAndMatcherStore.getStore().getPattern(objRef);
		Matcher m = p.matcher(cs);

		int newMatcherId = env.newObject("java.util.regex.Matcher");
		PatternAndMatcherStore.getStore().putMatcher(newMatcherId, m);

		AbstractRegExExpression symbolicPattern = env.getObjectAttr(objRef, AbstractRegExExpression.class);
		SymbolicString symbolicString = env.getObjectAttr(paramString, SymbolicString.class);

		if (symbolicPattern == null && symbolicString != null) {
			String pattern = p.pattern();
			symbolicPattern = RegExpConverter.toSMTExpression(pattern);
		}
		if (symbolicString != null) {
			if (symbolicString instanceof SymbolicBVString) {
				env.throwException("errors.Assume", "Cannot run symbolic matcher with the bv model");
				ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo()).completePathDontKnow(env.getThreadInfo());
				return -1;
			} else if (symbolicString instanceof SymbolicSMTString) {
				SymbolicSMTString symbolicSMT = (SymbolicSMTString) symbolicString;
				SymbolicMatcher sm = new SymbolicMatcher(symbolicSMT, symbolicPattern);
				env.setObjectAttr(newMatcherId, sm);
			} else {
				throw new UnsupportedOperationException("Don't know this string model yet");
			}
		}
		return newMatcherId;
	}
}
