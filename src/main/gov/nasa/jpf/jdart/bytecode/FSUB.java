/*
 * Copyright (C) 2015, United States Government, as represented by the 
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 * 
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment 
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. You may obtain a 
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 
 * 
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package gov.nasa.jpf.jdart.bytecode;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.jdart.ConcolicInstructionFactory;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.jdart.objects.SymbolicNumber;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


/**
 * Subtract float
 * ..., value1, value2 => ..., result
 */
public class FSUB extends gov.nasa.jpf.jvm.bytecode.FSUB {


  @Override
  public Instruction execute (ThreadInfo ti) {
    StackFrame sf = ti.getTopFrame();

    Object rightO = sf.getOperandAttr(0);
    Object leftO = sf.getOperandAttr(1);
    if (rightO == null && leftO == null) {
      return super.execute(ti);
    }

    ConcolicUtil.Pair<Float> right = ConcolicUtil.popFloat(sf);
    ConcolicUtil.Pair<Float> left = ConcolicUtil.popFloat(sf);

    Expression leftSymb = left.symb;
    Expression rightSymb = right.symb;

    if (leftO instanceof SymbolicNumber) {
      SymbolicNumber sn = (SymbolicNumber) leftO;
      leftSymb = sn.symbolicNumber;
    }
    if (rightO instanceof SymbolicNumber) {
      SymbolicNumber sn = (SymbolicNumber) rightO;
      rightSymb = sn.symbolicNumber;
    }

    NumericCompound<Float> symb = new NumericCompound<Float>(
        leftSymb, NumericOperator.MINUS, rightSymb);

    float conc = left.conc - right.conc;

    ConcolicUtil.Pair<Float> result = new ConcolicUtil.Pair<Float>(conc, symb);
    ConcolicUtil.pushFloat(result, sf);

    if (ConcolicInstructionFactory.DEBUG) {
      ConcolicInstructionFactory.logger.finest("Execute FSUB: " + result);
    }
    return getNext(ti);
  }
}
