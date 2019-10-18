/*
 * Copyright 2019 TU Dortmund, Falk Howar (@fhowar)
 *
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

import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class NEWARRAY extends gov.nasa.jpf.jvm.bytecode.NEWARRAY {

  public NEWARRAY(int typeCode) {
    super(typeCode);
  }

  @Override
  public Instruction execute(ThreadInfo ti) {
    StackFrame sf = ti.getTopFrame();
    if (sf.getOperandAttr(0) != null) {
      ConcolicUtil.Pair<Integer> length = ConcolicUtil.peekInt(sf);
      Instruction i = super.execute(ti);
      ElementInfo ei =  ti.getHeap().get(sf.peek());
      ei.setObjectAttr(length.symb);
      return i;
    }

    return super.execute(ti);
  }
}
