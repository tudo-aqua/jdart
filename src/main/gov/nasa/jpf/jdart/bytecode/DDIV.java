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

import static gov.nasa.jpf.constraints.expressions.NumericComparator.EQ;

import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.jdart.ConcolicInstructionFactory;
import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


/**
 * Divide 2 doubles
 * ..., value1, value2 => ..., value2/value1
 */
public class DDIV extends gov.nasa.jpf.jvm.bytecode.DDIV  {

  @Override
	public Instruction execute (ThreadInfo ti) {
    StackFrame sf = ti.getTopFrame();

    if (sf.getOperandAttr(1) == null && sf.getOperandAttr(3) == null) {
      return super.execute(ti);
    }

    ConcolicUtil.Pair<Double> right = ConcolicUtil.popDouble(sf);
    ConcolicUtil.Pair<Double> left = ConcolicUtil.popDouble(sf);

    ConcolicMethodExplorer analysis = ConcolicMethodExplorer.getCurrentAnalysis(ti);
    NumericBooleanExpression expr = NumericBooleanExpression.create(right.symb, EQ, Constant.create(
        BuiltinTypes.DOUBLE, 0.0));
    analysis.decision(ti, null, right.conc == 0.0 ? 0 : 1, expr, Negation.create(expr));

    NumericCompound<Double> symb = new NumericCompound<Double>(
        left.symb, NumericOperator.DIV, right.symb);

    double conc = left.conc / right.conc;

    ConcolicUtil.Pair<Double> result = new ConcolicUtil.Pair<Double>(conc, symb);
    ConcolicUtil.pushDouble(result, sf);

    if (ConcolicInstructionFactory.DEBUG) {
      ConcolicInstructionFactory.logger.finest("Execute DDIV: " + result);
    }
    return getNext(ti);
  }
}

