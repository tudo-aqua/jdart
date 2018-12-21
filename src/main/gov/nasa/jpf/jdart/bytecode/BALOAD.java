/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.jpf.jdart.bytecode;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 *
 * @author falk
 */
public class BALOAD extends gov.nasa.jpf.jvm.bytecode.BALOAD {

  @Override
  public Instruction execute(ThreadInfo ti) {
    ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);    
    if(analysis == null) {
      return super.execute(ti);
    }
    
    StackFrame sf = ti.getTopFrame();
    ElementInfo ei = ti.getHeap().get(sf.peek(1));
    if (ei.getObjectAttr() != null || sf.getOperandAttr(0) != null) {

      ConcolicUtil.Pair<Integer> idx = ConcolicUtil.peekInt(sf, 0);
      Expression length = (Expression) ei.getObjectAttr();
      if (length == null) {
        length = new Constant(BuiltinTypes.SINT32, ei.arrayLength());
      }
      Expression constraint = ExpressionUtil.and(
              new NumericBooleanExpression(
                      new Constant<>(BuiltinTypes.SINT32,0), 
                      NumericComparator.LE,
                      idx.symb),
              new NumericBooleanExpression(
                      idx.symb,
                      NumericComparator.LT,
                      length
              )
      );
      
      boolean sat = (0 <= idx.conc) && (idx.conc <= ei.arrayLength());              
      int branchIdx = sat ? 0 : 1;
      analysis.decision(ti, this, branchIdx, constraint, new Negation(constraint));      
    }
    return super.execute(ti);
  }
}
