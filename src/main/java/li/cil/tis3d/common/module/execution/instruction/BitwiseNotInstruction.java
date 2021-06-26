package li.cil.tis3d.common.module.execution.instruction;

import li.cil.tis3d.common.module.execution.Machine;
import li.cil.tis3d.common.module.execution.MachineState;

public final class BitwiseNotInstruction implements Instruction {
    public static final String NAME = "NOT";
    public static final Instruction INSTANCE = new BitwiseNotInstruction();

    @Override
    public void step(final Machine machine) {
        final MachineState state = machine.getState();
        state.acc = (short) ~state.acc;
        state.pc++;
    }

    @Override
    public String toString() {
        return NAME;
    }
}
