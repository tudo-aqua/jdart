package gov.nasa.jpf.jdart.peers.strings;

import gov.nasa.jpf.vm.MJIEnv;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternAndMatcherStore {

	private static PatternAndMatcherStore instance;

	private HashMap<Integer, Matcher> matcherStore = new HashMap<>();
	private HashMap<Integer, Pattern> patternStore = new HashMap<>();

	public synchronized static PatternAndMatcherStore getStore() {
		if (instance == null) {
			instance = new PatternAndMatcherStore();
		}
		return instance;
	}
	
	public synchronized void putMatcher(int objref, Matcher matcher) {
		matcherStore.put(objref, matcher);
	}

	public synchronized void putInstance(MJIEnv env, int objref, Matcher matcher) {
		putMatcher(objref, matcher);
	}

	public synchronized Matcher getMatcher(MJIEnv env, int objref) {
		return matcherStore.get(objref);
	}

	public synchronized Matcher getMatcher(int objref) {
		return matcherStore.get(objref);
	}

	public synchronized void putPattern(int objref, Pattern pattern) {
		patternStore.put(objref, pattern);
	}

	public synchronized Pattern getPattern(int objref) {
		return patternStore.get(objref);
	}
}
