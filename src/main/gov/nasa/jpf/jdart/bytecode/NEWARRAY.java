/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.jpf.jdart.bytecode;

import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 *
 * @author falk
 */
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
