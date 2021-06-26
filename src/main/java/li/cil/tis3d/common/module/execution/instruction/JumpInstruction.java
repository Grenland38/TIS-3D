package li.cil.tis3d.common.module.execution.instruction;

import li.cil.tis3d.common.module.execution.Machine;
import li.cil.tis3d.common.module.execution.MachineState;

public final class JumpInstruction implements Instruction {
    public static final String NAME = "JMP";

    private final String label;

    public JumpInstruction(final String label) {
        this.label = label;
    }

    @Override
    public void step(final Machine machine) {
        final MachineState state = machine.getState();
        state.pc = state.labels.get(label);
    }

    @Override
    public String toString() {
        return NAME + " " + label;
    }
}
