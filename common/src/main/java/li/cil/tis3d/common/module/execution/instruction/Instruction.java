package li.cil.tis3d.common.module.execution.instruction;

import li.cil.tis3d.api.machine.Port;
import li.cil.tis3d.common.module.execution.Machine;
import li.cil.tis3d.common.module.execution.MachineImpl;

/**
 * A single instruction that can be executed by the execution module.
 * <p>
 * Note that instructions <em>must not</em> have any mutable state! They
 * always operate on the machine given to them, and the pipes connected
 * to that machine. Their only state is their configuration.
 */
public interface Instruction {
    /**
     * Update the instruction, when it is done it must increment the machine's
     * program counter itself.
     *
     * @param machine the machine to run the instruction on.
     */
    void step(final Machine machine);

    /**
     * Take action based on pending write operation completion, e.g. to abort other
     * writes when a value should only be readable once.
     *
     * @param machine the machine the operation will finish on.
     * @param port    the port the operation will finish on.
     * @see li.cil.tis3d.api.module.Module#onBeforeWriteComplete(Port)
     */
    default void onBeforeWriteComplete(final MachineImpl machine, final Port port) {
    }

    /**
     * Finish a write operation started by the instruction, usually by
     * advancing the program counter.
     * <p>
     * Instructions must <em>always</em> await or cancel a write operation they
     * started.
     *
     * @param machine the machine the operation finished on.
     * @param port    the port the operation finished on.
     */
    default void onWriteCompleted(final Machine machine, final Port port) {
    }
}
