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

import gov.nasa.jpf.constraints.casts.NumericCastOperation;
import gov.nasa.jpf.constraints.expressions.BitvectorExpression;
import gov.nasa.jpf.constraints.expressions.BitvectorOperator;
import gov.nasa.jpf.constraints.expressions.CastExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.jdart.ConcolicInstructionFactory;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;


/**
 * Shift left
 * ..., value1, value2  =>..., result
 */
public class LSHL extends gov.nasa.jpf.jvm.bytecode.LSHL {

  @Override
  public Instruction execute (ThreadInfo ti) {
		StackFrame sf = ti.getTopFrame();

    if (sf.getOperandAttr(0) == null && sf.getOperandAttr(2) == null) {
      return super.execute(ti);
    }
    
	  ConcolicUtil.Pair<Integer> right = ConcolicUtil.popInt(sf);
	  ConcolicUtil.Pair<Long> left = ConcolicUtil.popLong(sf);
    
          /*
           * Java Language Specification paragraph 15.19
           * Shift Operators
           *
           * If the promoted type of the left-hand operand is int, 
           * only the five lowest-order bits of the right-hand operand 
           * are used as the shift distance. It is as if the right-hand 
           * operand were subjected to a bitwise logical AND operator & 
           * (p15.22.1) with the mask value 0x1f (0b11111). The shift
           * distance actually used is therefore always in the range 0 
           * to 31, inclusive.
           *
           * If the promoted type of the left-hand operand is long, then 
           * only the six lowest-order bits of the right-hand operand are 
           * used as the shift distance. It is as if the right-hand operand 
           * were subjected to a bitwise logical AND operator & (p15.22.1)
           * with the mask value 0x3f (0b111111). The shift distance actually 
           * used is therefore always in the range 0 to 63, inclusive.
           */

    BitvectorExpression<Long> symb = BitvectorExpression.create(
            left.symb, BitvectorOperator.SHIFTL, BitvectorExpression.create(
              new CastExpression<>(right.symb, BuiltinTypes.SINT64, NumericCastOperation.TO_SINT64),
                  BitvectorOperator.AND, Constant.create(BuiltinTypes.SINT64, (long) 0x3f)));

    int rh = right.conc & 0x3f; 
    
    long conc = left.conc << rh;      
    
    ConcolicUtil.Pair<Long> result = new ConcolicUtil.Pair<Long>(conc, symb);
    ConcolicUtil.pushLong(result, sf);

    if (ConcolicInstructionFactory.DEBUG) ConcolicInstructionFactory.logger.finest("Execute LSHL: " + result);		
    return getNext(ti);
	}
}
