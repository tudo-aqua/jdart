/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.jpf.jdart.peers;

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.jdart.annotations.SymbolicPeer;
import gov.nasa.jpf.jdart.objects.SymbolicString;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 *
 * @author falk
 */
public class JPF_java_lang_String extends gov.nasa.jpf.vm.JPF_java_lang_String {

  @MJI
  @SymbolicPeer
  @Override
  public int init___3BII__Ljava_lang_String_2(MJIEnv env, int objRef, int bytesRef, int offset, int length) {
    ElementInfo eiSource = env.getElementInfo(bytesRef);
    Expression symbLength = eiSource.getObjectAttr(Expression.class);
    Expression[] symbChars = new Expression[eiSource.arrayLength()];
    for (int i = 0; i < eiSource.arrayLength(); i++) {
      symbChars[i] = eiSource.getElementAttr(i, Expression.class);
    }
    env.setObjectAttr(objRef, new SymbolicString(symbLength, symbChars));
    return super.init___3BII__Ljava_lang_String_2(env, objRef, bytesRef, offset, length);
  }

  @MJI
  @SymbolicPeer
  @Override
  public char charAt__I__C(MJIEnv env, int objRef, int index) {
    ThreadInfo ti = env.getThreadInfo();
    ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);  
    ElementInfo eiThis = env.getElementInfo(objRef);
    SymbolicString symb = env.getObjectAttr(objRef, SymbolicString.class);
    Expression symbIndex = (Expression) env.getArgAttributes()[1];
    
    if (analysis == null || (symb == null && symbIndex == null)) {
      return super.charAt__I__C(env, objRef, index);
    }

    Expression length = (symb != null) ? symb.getSymbolicLength() : null;
    if (length == null) {
      length = new Constant(BuiltinTypes.SINT32, eiThis.asString().length());
    }
    if (symbIndex == null) {
      symbIndex = new Constant(BuiltinTypes.SINT32, index);
    }

    Expression constraint = ExpressionUtil.and(
            new NumericBooleanExpression(
                    new Constant<>(BuiltinTypes.SINT32, 0),
                    NumericComparator.LE,
                    symbIndex),
            new NumericBooleanExpression(
                    symbIndex,
                    NumericComparator.LT,
                    length
            )
    );

    boolean sat = (0 <= index) && (index < eiThis.asString().length());
    int branchIdx = sat ? 0 : 1;
    analysis.decision(ti, null, branchIdx, constraint, new Negation(constraint));
    
    try {
      env.setReturnAttribute(symb.getSymbolicChars()[index]);
      return super.charAt__I__C(env, objRef, index);
    } catch (ArrayIndexOutOfBoundsException e) {
      env.throwException(ArrayIndexOutOfBoundsException.class.getName(), e.getMessage());
      return MJIEnv.NULL;
    }
  }

}
