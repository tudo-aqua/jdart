package gov.nasa.jpf.jdart.analysis;

import gov.nasa.jpf.jdart.ConcolicMethodExplorer;
import gov.nasa.jpf.jdart.objects.SymbolicString;
import gov.nasa.jpf.jvm.ClassFile;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.VMListener;

public class AnalysisBorderListener implements VMListener {
	@Override
	public void vmInitialized(VM vm) {

	}

	@Override
	public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {

	}

	@Override
	public void instructionExecuted(VM vm,
									ThreadInfo currentThread,
									Instruction nextInstruction,
									Instruction executedInstruction) {

	}

	@Override
	public void threadStarted(VM vm, ThreadInfo startedThread) {

	}

	@Override
	public void threadBlocked(VM vm, ThreadInfo blockedThread, ElementInfo lock) {

	}

	@Override
	public void threadWaiting(VM vm, ThreadInfo waitingThread) {

	}

	@Override
	public void threadNotified(VM vm, ThreadInfo notifiedThread) {

	}

	@Override
	public void threadInterrupted(VM vm, ThreadInfo interruptedThread) {

	}

	@Override
	public void threadTerminated(VM vm, ThreadInfo terminatedThread) {

	}

	@Override
	public void threadScheduled(VM vm, ThreadInfo scheduledThread) {

	}

	@Override
	public void loadClass(VM vm, ClassFile cf) {

	}

	@Override
	public void classLoaded(VM vm, ClassInfo loadedClass) {

	}

	@Override
	public void objectCreated(VM vm, ThreadInfo currentThread, ElementInfo newObject) {

	}

	@Override
	public void objectReleased(VM vm, ThreadInfo currentThread, ElementInfo releasedObject) {

	}

	@Override
	public void objectLocked(VM vm, ThreadInfo currentThread, ElementInfo lockedObject) {

	}

	@Override
	public void objectUnlocked(VM vm, ThreadInfo currentThread, ElementInfo unlockedObject) {

	}

	@Override
	public void objectWait(VM vm, ThreadInfo currentThread, ElementInfo waitingObject) {

	}

	@Override
	public void objectNotify(VM vm, ThreadInfo currentThread, ElementInfo notifyingObject) {

	}

	@Override
	public void objectNotifyAll(VM vm, ThreadInfo currentThread, ElementInfo notifyingObject) {

	}

	@Override
	public void objectExposed(VM vm,
							  ThreadInfo currentThread,
							  ElementInfo fieldOwnerObject,
							  ElementInfo exposedObject) {

	}

	@Override
	public void objectShared(VM vm, ThreadInfo currentThread, ElementInfo sharedObject) {

	}

	@Override
	public void gcBegin(VM vm) {

	}

	@Override
	public void gcEnd(VM vm) {

	}

	@Override
	public void exceptionThrown(VM vm, ThreadInfo currentThread, ElementInfo thrownException) {

	}

	@Override
	public void exceptionBailout(VM vm, ThreadInfo currentThread) {

	}

	@Override
	public void exceptionHandled(VM vm, ThreadInfo currentThread) {

	}

	@Override
	public void choiceGeneratorRegistered(VM vm,
										  ChoiceGenerator<?> nextCG,
										  ThreadInfo currentThread,
										  Instruction executedInstruction) {

	}

	@Override
	public void choiceGeneratorSet(VM vm, ChoiceGenerator<?> newCG) {

	}

	@Override
	public void choiceGeneratorAdvanced(VM vm, ChoiceGenerator<?> currentCG) {

	}

	@Override
	public void choiceGeneratorProcessed(VM vm, ChoiceGenerator<?> processedCG) {

	}

	@Override
	public void methodEntered(VM vm, ThreadInfo currentThread, MethodInfo enteredMethod) {
		if (enteredMethod.getName().startsWith("matcher") &&
			enteredMethod.getClassInfo().getName().equals("java.util.regex.Pattern")) {
			int slotIndexInput = 1;
			LocalVarInfo[] lvi = enteredMethod.getArgumentLocalVars();
			StackFrame sf = currentThread.getTopFrame();

			Object[] attrs = sf.getSlotAttrs();

			ElementInfo ei = currentThread.getElementInfo(sf.getSlot(lvi[slotIndexInput].getSlotIndex()));
			SymbolicString symbolicString = ei.getObjectAttr(SymbolicString.class);
			Object attr = null;
			if (attrs != null) {
				attr = attrs[lvi[slotIndexInput].getSlotIndex()];
			}
			if ((attr != null && attr instanceof SymbolicString) || symbolicString != null) {
				ConcolicMethodExplorer ca = ConcolicMethodExplorer.getCurrentAnalysis(currentThread);
				ca.completePathDontKnow(currentThread);
			}
		}
	}

	@Override
	public void methodExited(VM vm, ThreadInfo currentThread, MethodInfo exitedMethod) {

	}
}
