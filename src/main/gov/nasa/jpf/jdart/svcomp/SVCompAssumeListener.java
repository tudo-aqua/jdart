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
package gov.nasa.jpf.jdart.svcomp;

import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jvm.ClassFile;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.VMListener;

public class SVCompAssumeListener implements VMListener {

  @Override
  public void vmInitialized(VM vm) {

  }

  @Override
  public void executeInstruction(VM vm, ThreadInfo ti, Instruction i) {

  }

  @Override
  public void instructionExecuted(VM vm, ThreadInfo ti, Instruction i, Instruction i1) {

  }

  @Override
  public void threadStarted(VM vm, ThreadInfo ti) {

  }

  @Override
  public void threadBlocked(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void threadWaiting(VM vm, ThreadInfo ti) {

  }

  @Override
  public void threadNotified(VM vm, ThreadInfo ti) {

  }

  @Override
  public void threadInterrupted(VM vm, ThreadInfo ti) {

  }

  @Override
  public void threadTerminated(VM vm, ThreadInfo ti) {

  }

  @Override
  public void threadScheduled(VM vm, ThreadInfo ti) {

  }

  @Override
  public void loadClass(VM vm, ClassFile cf) {

  }

  @Override
  public void classLoaded(VM vm, ClassInfo ci) {

  }

  @Override
  public void objectCreated(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void objectReleased(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void objectLocked(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void objectUnlocked(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void objectWait(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void objectNotify(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void objectNotifyAll(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void objectExposed(VM vm, ThreadInfo ti, ElementInfo ei, ElementInfo ei1) {

  }

  @Override
  public void objectShared(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void gcBegin(VM vm) {

  }

  @Override
  public void gcEnd(VM vm) {

  }

  @Override
  public void exceptionThrown(VM vm, ThreadInfo ti, ElementInfo ei) {

  }

  @Override
  public void exceptionBailout(VM vm, ThreadInfo ti) {

  }

  @Override
  public void exceptionHandled(VM vm, ThreadInfo ti) {

  }

  @Override
  public void choiceGeneratorRegistered(VM vm, ChoiceGenerator<?> cg, ThreadInfo ti, Instruction i) {

  }

  @Override
  public void choiceGeneratorSet(VM vm, ChoiceGenerator<?> cg) {

  }

  @Override
  public void choiceGeneratorAdvanced(VM vm, ChoiceGenerator<?> cg) {

  }

  @Override
  public void choiceGeneratorProcessed(VM vm, ChoiceGenerator<?> cg) {

  }

  @Override
  public void methodEntered(VM vm, ThreadInfo ti, MethodInfo mi) {
    if ("java.lang.Runtime".equals(mi.getClassName())
            && "halt".equals(mi.getName())) {
      ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(ti);
      //Assumption violation is okay
      ca.completePathOk(ti);
    }
  }

  @Override
  public void methodExited(VM vm, ThreadInfo ti, MethodInfo mi) {

  }
  
}
