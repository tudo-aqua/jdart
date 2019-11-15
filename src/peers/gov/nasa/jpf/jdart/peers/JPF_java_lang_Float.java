package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicNumber;
import gov.nasa.jpf.jdart.objects.SymbolicString;
import gov.nasa.jpf.vm.MJIEnv;

public class JPF_java_lang_Float extends gov.nasa.jpf.vm.JPF_java_lang_Float {
	@MJI
	@SymbolicPeer
	public static float parseFloat__Ljava_lang_String_2__F(MJIEnv env, int objref, int stringRef) {
		if (stringRef == MJIEnv.NULL) {
			env.getThreadInfo()
			   .createAndThrowException("java.lang.NullPointerException",
										"Cannot parse a double from Null string. Not even a symbolic one.");
		}
		String format = env.getStringObject(stringRef);
		SymbolicString symbolicFormat = env.getObjectAttr(stringRef, SymbolicString.class);
		if (symbolicFormat == null) {
			try {
				return Float.parseFloat(format);
			}
			catch (NumberFormatException e) {
				env.getThreadInfo().createAndThrowException("java.lang.NumberFormatException");
			}
		}
		ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(env.getThreadInfo());
		ConcolicUtil.Pair<Boolean> error = ca.getOrCreateSymbolicBoolean();
		Expression errorExp = new NumericBooleanExpression(error.symb,
														   NumericComparator.EQ,
														   new Constant<Boolean>(BuiltinTypes.BOOL, true));
		ca.decision(env.getThreadInfo(), null, error.conc ? 0 : 1, errorExp, new Negation(errorExp));
		if (error.conc) {
			env.getThreadInfo().createAndThrowException("java.lang.NumberFormatException");
		}

		ConcolicUtil.Pair<Float> symbolicFloat = ca.getOrCreateSymbolicFloat();
		SymbolicNumber sn = new SymbolicNumber(symbolicFloat.symb);
		sn.isFromString = true;
		env.setReturnAttribute(sn);
		return symbolicFloat.conc;
	}

}
