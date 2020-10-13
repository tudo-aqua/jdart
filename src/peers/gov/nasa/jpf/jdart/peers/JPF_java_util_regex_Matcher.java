package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.StringBooleanExpression;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicMatcher;
import gov.nasa.jpf.jdart.objects.SymbolicSMTString;
import gov.nasa.jpf.jdart.peers.strings.PatternAndMatcherStore;
import gov.nasa.jpf.vm.MJIEnv;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JPF_java_util_regex_Matcher extends gov.nasa.jpf.vm.JPF_java_util_regex_Matcher {

	public JPF_java_util_regex_Matcher(Config conf) {
		super(conf);
	}

	Pattern getPatternFromMatcher(MJIEnv env, int objref) {
		int patRef = env.getReferenceField(objref, "pattern");

		int regexRef = env.getReferenceField(patRef, "regex");
		String regex = env.getStringObject(regexRef);
		int flags = env.getIntField(patRef, "flags");

		return Pattern.compile(regex, flags);
	}

	@MJI
	@SymbolicPeer
	@Override
	public void register____V(MJIEnv env, int objref) {
		Pattern pat = getPatternFromMatcher(env, objref);

		int inputRef = env.getReferenceField(objref, "input");
		String input = env.getStringObject(inputRef);

		Matcher matcher = pat.matcher(input);
		PatternAndMatcherStore.getStore().putInstance(env, objref, matcher);
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean matches____Z(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.matches();
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean find__I__Z(MJIEnv env, int objref, int i) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.find(i);
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean find____Z(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(objref);
		boolean res = matcher.find();

		SymbolicMatcher sm = env.getObjectAttr(objref, SymbolicMatcher.class);
		if (sm != null && !sm.getMatched()) {
			ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
			if (analysis != null) {


				int branchIdx = res ? 0 : 1;
				analysis.decision(env.getThreadInfo(),
													null,
													branchIdx,
													sm.targetInPattern(),
													new Negation(sm.targetInPattern()));
				int counter = res && matcher.groupCount() == 0 ? 1 : matcher.groupCount();

				for (int i = 0; i < counter; i++) {
					ConcolicUtil.Pair<String> newStrVar = analysis.getOrCreateSymbolicString();
					sm.storeSymbolicGroup(i, new SymbolicSMTString((Variable<String>) newStrVar.symb, newStrVar.symb));
					analysis.decision(env.getThreadInfo(),
														null,
														0,
														StringBooleanExpression.createContains(sm.getTarget().symbolicValue, newStrVar.symb));
				}
				sm.setMatched();
			}
		} else if (sm != null && sm.getMatched()) {
			//TODO: Modell continous matching.
			res = false;
		}
		return res;
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean lookingAt____Z(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.lookingAt();
	}

	@MJI
	@SymbolicPeer
	@Override
	public int start__I__I(MJIEnv env, int objref, int group) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.start(group);
	}

	@MJI
	@SymbolicPeer
	@Override
	public int end__I__I(MJIEnv env, int objref, int group) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.end(group);
	}

	@MJI
	@SymbolicPeer
	@Override
	public int regionStart____I(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.regionStart();
	}

	@MJI
	@SymbolicPeer
	@Override
	public int regionEnd____I(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.regionEnd();
	}

	@MJI
	@SymbolicPeer
	@Override
	public int region__II__Ljava_util_regex_Matcher_2(MJIEnv env, int objref, int start, int end) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		matcher = matcher.region(start, end);
		PatternAndMatcherStore.getStore().putInstance(env, objref, matcher);

		return objref;
	}

	@MJI
	@SymbolicPeer
	@Override
	public int reset____Ljava_util_regex_Matcher_2(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);

		int inputRef = env.getReferenceField(objref, "input");
		String input = env.getStringObject(inputRef);

		matcher = matcher.reset(input);
		PatternAndMatcherStore.getStore().putInstance(env, objref, matcher);

		return objref;
	}

	@MJI
	@SymbolicPeer
	@Override
	public int groupCount____I(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.groupCount();
	}

	@MJI
	@SymbolicPeer
	@Override
	public int group__I__Ljava_lang_String_2(MJIEnv env, int objref, int i) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		String grp = matcher.group(i);

		int newStringId = env.newString(grp);
		SymbolicMatcher sm = env.getObjectAttr(objref, SymbolicMatcher.class);
		if (sm != null) {
			if (!sm.hasSymbolicGroup(i)) {
				throw new UnsupportedOperationException("This mustnot happen");
			} else {
				env.setObjectAttr(newStringId, sm.getSymbolicGroup(i));
			}
		}
		return newStringId;
	}

	@MJI
	@SymbolicPeer
	@Override
	public int quoteReplacement__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int clsObjref, int string) {
		String parm = env.getStringObject(string);
		String result = Matcher.quoteReplacement(parm);
		return env.newString(result);
	}

	@MJI
	@SymbolicPeer
	@Override
	public int replaceAll__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objref, int string) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		String replacement = env.getStringObject(string);
		String result = matcher.replaceAll(replacement);

		int resultref = env.newString(result);
		return resultref;
	}

	@MJI
	@SymbolicPeer
	@Override
	public int replaceFirst__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objref, int string) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		String replacement = env.getStringObject(string);
		String result = matcher.replaceFirst(replacement);

		int resultref = env.newString(result);
		return resultref;
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean hasTransparentBounds____Z(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.hasTransparentBounds();
	}

	@MJI
	@SymbolicPeer
	@Override
	public int useTransparentBounds__Z__Ljava_util_regex_Matcher_2(MJIEnv env, int objref, boolean b) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		matcher = matcher.useTransparentBounds(b);
		PatternAndMatcherStore.getStore().putInstance(env, objref, matcher);

		return objref;
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean hasAnchoringBounds____Z(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.hasTransparentBounds();
	}

	@MJI
	@SymbolicPeer
	@Override
	public int useAnchoringBounds__Z__Ljava_util_regex_Matcher_2(MJIEnv env, int objref, boolean b) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		matcher = matcher.useAnchoringBounds(b);
		PatternAndMatcherStore.getStore().putInstance(env, objref, matcher);

		return objref;
	}

	@MJI
	@SymbolicPeer
	@Override
	public int updatePattern____Ljava_util_regex_Matcher_2(MJIEnv env, int objref) {
		//We get the newly updated pattern
		Pattern pat = getPatternFromMatcher(env, objref);

		//We update the matcher with the new pattern
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		matcher = matcher.usePattern(pat);
		PatternAndMatcherStore.getStore().putInstance(env, objref, matcher);

		return objref;
	}

	@MJI
	@SymbolicPeer
	@Override
	public int toString____Ljava_lang_String_2(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		String str = matcher.toString();

		return env.newString(str);
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean hitEnd____Z(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.hitEnd();
	}

	@MJI
	@SymbolicPeer
	@Override
	public boolean requireEnd____Z(MJIEnv env, int objref) {
		Matcher matcher = PatternAndMatcherStore.getStore().getMatcher(env, objref);
		return matcher.requireEnd();
	}
}
