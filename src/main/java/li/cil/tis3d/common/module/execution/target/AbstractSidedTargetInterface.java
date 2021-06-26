package li.cil.tis3d.common.module.execution.target;

import li.cil.tis3d.api.machine.Casing;
import li.cil.tis3d.api.machine.Face;
import li.cil.tis3d.api.machine.Port;
import li.cil.tis3d.common.module.ExecutionModule;
import li.cil.tis3d.common.module.execution.Machine;

abstract class AbstractSidedTargetInterface extends AbstractTargetInterface {
    private final ExecutionModule module;
    private final Face face;

    protected AbstractSidedTargetInterface(final Machine machine, final ExecutionModule module, final Face face) {
        super(machine);
        this.module = module;
        this.face = face;
    }

    // --------------------------------------------------------------------- //

    protected final void beginWrite(final Port port, final short value) {
        getCasing().getSendingPipe(face, port).beginWrite(value);
    }

    protected final void cancelWrite(final Port port) {
        getCasing().getSendingPipe(face, port).cancelWrite();
    }

    protected final boolean isWriting(final Port port) {
        return getCasing().getSendingPipe(face, port).isWriting();
    }

    protected final void beginRead(final Port port) {
        getCasing().getReceivingPipe(face, port).beginRead();
    }

    protected final void cancelRead(final Port port) {
        getCasing().getReceivingPipe(face, port).cancelRead();
    }

    protected final boolean isReading(final Port port) {
        return getCasing().getReceivingPipe(face, port).isReading();
    }

    protected final boolean canTransfer(final Port port) {
        return getCasing().getReceivingPipe(face, port).canTransfer();
    }

    protected final short read(final Port port) {
        return getCasing().getReceivingPipe(face, port).read();
    }

    // --------------------------------------------------------------------- //

    private Casing getCasing() {
        return module.getCasing();
    }
}
