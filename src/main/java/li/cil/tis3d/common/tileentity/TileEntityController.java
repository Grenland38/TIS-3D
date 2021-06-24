package li.cil.tis3d.common.tileentity;

import li.cil.tis3d.api.API;
import li.cil.tis3d.api.machine.HaltAndCatchFireException;
import li.cil.tis3d.common.CommonConfig;
import li.cil.tis3d.common.network.Network;
import li.cil.tis3d.common.network.message.HaltAndCatchFireMessage;
import li.cil.tis3d.util.WorldUtils;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;

/**
 * The controller tile entity.
 * <p>
 * Scans for multi-block structures if scheduled by either the controller
 * itself or by a connected casing. Manages a list of casings and updates
 * the modules in the casing (this is the only ticking part of a multi-block).
 * <p>
 * Controllers have no real state. They are active when powered by a redstone
 * signal, and can be reset by right-clicking them.
 */
public final class TileEntityController extends TileEntityComputer implements ITickableTileEntity {
    // --------------------------------------------------------------------- //
    // Computed data

    /**
     * Time in ticks to wait before restarting execution after an HCF event.
     */
    private static final int COOLDOWN_HCF = 60;

    /**
     * Possible states of a controller.
     */
    public enum ControllerState {
        /**
         * A scan has been scheduled and will be performed in the next tick.
         */
        SCANNING(false),

        /**
         * In the last scan another controller was found; only one is allowed per multi-block.
         */
        MULTIPLE_CONTROLLERS(true),

        /**
         * In the last scan more than {@link CommonConfig#maxCasingsPerController} casings were found.
         */
        TOO_COMPLEX(true),

        /**
         * In the last scan the border of the loaded area was hit; incomplete multi-blocks to nothing.
         */
        INCOMPLETE(true),

        /**
         * The controller is in operational state and can update connected casings each tick.
         */
        READY(false),

        /**
         * The controller is in operational state and powered, updating connected casings each tick.
         */
        RUNNING(false);

        // --------------------------------------------------------------------- //

        /**
         * Whether this states is an error state, i.e. whether it indicates the controller
         * not operation normally due it being configured incorrectly, for example.
         */
        public final boolean isError;

        /**
         * The message to display for this status.
         */
        public final ITextComponent message;

        ControllerState(final boolean isError) {
            this.isError = isError;
            this.message = new TranslationTextComponent(API.MOD_ID + ".controller.status." + name().toLowerCase(Locale.US));
        }

        // --------------------------------------------------------------------- //

        /**
         * All possible enum values for quick indexing.
         */
        public static final ControllerState[] VALUES = ControllerState.values();
    }

    /**
     * The list of casings managed by this controller.
     */
    private final List<TileEntityCasing> casings = new ArrayList<>(CommonConfig.maxCasingsPerController);

    /**
     * The current state of the controller.
     */
    private ControllerState state = ControllerState.SCANNING;

    /**
     * The last state we sent to clients, i.e. the state clients think the controller is in.
     */
    private ControllerState lastSentState = ControllerState.SCANNING;

    // NBT tag names.
    private static final String TAG_HCF_COOLDOWN = "hcfCooldown";
    private static final String TAG_STATE = "state";

    /**
     * User scheduled a forced step for the next tick.
     * <p>
     * This only matters if the machine is currently paused; in particular,
     * this is ignored if the machine is currently powered down. Therefore
     * there's not need to save the value to NBT, either.
     */
    private boolean forceStep;

    // --------------------------------------------------------------------- //
    // Persisted data

    /**
     * Time to keep waiting before resuming execution after an HCF event.
     */
    private int hcfCooldown = 0;

    // --------------------------------------------------------------------- //

    public TileEntityController() {
        super(TileEntities.CONTROLLER.get());
    }

    /**
     * Get the current state of the controller.
     *
     * @return the current state of the controller.
     */
    public ControllerState getState() {
        return state;
    }

    /**
     * Schedule a rescan for connected casings.
     * <p>
     * If we're currently scanning this does nothing.
     */
    public void scheduleScan() {
        state = ControllerState.SCANNING;
    }

    /**
     * If the controller is running, force at least one step in the next tick,
     * even if the controller is currently in the paused state. This will not
     * cause additional steps when not in the paused step!
     */
    public void forceStep() {
        final World world = getBlockEntityWorld();
        if (state == ControllerState.RUNNING) {
            forceStep = true;
            final Vector3d pos = Vector3d.atCenterOf(getBlockPos());
            world.playSound(null, pos.x(), pos.y(), pos.z(),
                SoundEvents.STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.2f, 0.8f + world.random.nextFloat() * 0.1f);
        }
    }

    /**
     * Reset the controller, pause for a moment and catch fire.
     */
    public void haltAndCatchFire() {
        final World world = getBlockEntityWorld();
        if (!world.isClientSide()) {
            state = ControllerState.READY;
            casings.forEach(TileEntityCasing::onDisabled);
            final HaltAndCatchFireMessage message = new HaltAndCatchFireMessage(getBlockPos());
            final PacketDistributor.PacketTarget target = Network.getTargetPoint(this, Network.RANGE_MEDIUM);
            Network.INSTANCE.send(target, message);
        }
        hcfCooldown = COOLDOWN_HCF;
    }

    // --------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (getBlockEntityWorld().isClientSide()) {
            return;
        }

        // If we were in an active state, deactivate all modules in connected cases.
        // Safe to always call this because the casings track their own enabled
        // state and just won't do anything if they're already disabled.
        // Also, this is guaranteed to be correct, because we were either the sole
        // controller, thus there is no controller anymore, or there were multiple
        // controllers, in which case they were disabled to start with.
        casings.forEach(TileEntityCasing::onDisabled);
        for (final TileEntityCasing casing : casings) {
            casing.setController(null);
        }
        casings.clear();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();

        // Just unset from our casings, do *not* disable them to keep their state.
        for (final TileEntityCasing casing : casings) {
            casing.setController(null);
        }
    }

    // --------------------------------------------------------------------- //
    // TileEntityComputer

    @Override
    protected void readFromNBTForServer(final CompoundNBT nbt) {
        super.readFromNBTForServer(nbt);

        hcfCooldown = nbt.getInt(TAG_HCF_COOLDOWN);
    }

    @Override
    protected void writeToNBTForServer(final CompoundNBT nbt) {
        super.writeToNBTForServer(nbt);

        nbt.putInt(TAG_HCF_COOLDOWN, hcfCooldown);
    }

    @Override
    protected void readFromNBTForClient(final CompoundNBT nbt) {
        super.readFromNBTForClient(nbt);

        state = ControllerState.VALUES[nbt.getByte(TAG_STATE) & 0xFF];
    }

    @Override
    protected void writeToNBTForClient(final CompoundNBT nbt) {
        super.writeToNBTForClient(nbt);

        nbt.putByte(TAG_STATE, (byte) state.ordinal());
    }

    // --------------------------------------------------------------------- //
    // ITickableTileEntity

    @Override
    public void tick() {
        final World world = getBlockEntityWorld();

        // Only update multi-block and casings on the server.
        if (world.isClientSide()) {
            if (hcfCooldown > 0) {
                --hcfCooldown;

                if (world instanceof ServerWorld) {
                    final ServerWorld serverWorld = (ServerWorld) world;
                    // Spawn some fire particles! No actual fire, that'd be... problematic.
                    for (final Direction facing : Direction.values()) {
                        final BlockPos neighborPos = getBlockPos().relative(facing);
                        final BlockState neighborState = world.getBlockState(neighborPos);
                        if (neighborState.canOcclude()) {
                            continue;
                        }
                        if (world.random.nextFloat() > 0.25f) {
                            continue;
                        }
                        final float ox = neighborPos.getX() + world.random.nextFloat();
                        final float oy = neighborPos.getY() + world.random.nextFloat();
                        final float oz = neighborPos.getZ() + world.random.nextFloat();
                        serverWorld.sendParticles(ParticleTypes.FLAME, ox, oy, oz, 5, 0, 0, 0, 0);
                    }
                }
            }

            return;
        }

        if (state != lastSentState) {
            final Chunk chunk = world.getChunkAt(getBlockPos());
            final BlockState blockState = world.getBlockState(getBlockPos());
            world.markAndNotifyBlock(getBlockPos(), chunk, blockState, blockState, 7, 512);
            lastSentState = state;
        }

        // Enforce cooldown after HCF event.
        if (hcfCooldown > 0) {
            --hcfCooldown;
            return;
        }

        // Check if we need to rescan our multi-block structure.
        if (state == ControllerState.SCANNING) {
            scan();
        }

        // Stop if we're in an invalid state.
        if (state != ControllerState.READY && state != ControllerState.RUNNING) {
            return;
        }

        // Get accumulated redstone power coming in.
        final int power = computePower();

        // If we're in an error state we do nothing.
        if (state == ControllerState.READY) {
            // Are we powered?
            if (power < 1) {
                // Nope, nothing to do then.
                return;
            } else {
                // Yes, switch to running state and enable modules.
                state = ControllerState.RUNNING;
                casings.forEach(TileEntityCasing::onEnabled);
            }
        }

        if (state == ControllerState.RUNNING) {
            // Ignore forceStep when not paused.
            forceStep = forceStep && power == 1;

            // Are we powered?
            if (!world.hasNeighborSignal(getBlockPos())) {
                // Nope, fall back to ready state, disable modules.
                state = ControllerState.READY;
                casings.forEach(TileEntityCasing::onDisabled);
            } else if (power > 1 || forceStep) {
                // Operating, step all casings redstone input info once.
                casings.forEach(TileEntityCasing::stepRedstone);

                try {
                    // 0 = off, we never have this or we'd be in the READY state.
                    // 1 = paused, i.e. we don't lose state, but don't step.
                    // [2-14] = step every 15-n-th step.
                    // 15 = step every tick.
                    // [16-75] = step n/15 times a tick.
                    // 75 = step 5 times a tick.
                    if (power < 15) {
                        // Stepping slower than 100%.
                        final int delay = 15 - power;
                        if (world.getGameTime() % delay == 0 || forceStep) {
                            step();
                        }
                    } else {
                        // Stepping faster than 100%.
                        final int steps = power / 15;
                        for (int step = 0; step < steps; step++) {
                            step();
                        }
                    }
                } catch (final HaltAndCatchFireException e) {
                    haltAndCatchFire();
                }
            }

            // Processed our forced step either way, reset flag.
            forceStep = false;
        }
    }

    // --------------------------------------------------------------------- //

    /**
     * Checks all six neighbors of the specified tile entity and adds them to the
     * queue if they're a controller or casing and haven't been checked yet (or
     * added to the queue yet).
     * <p>
     * This returns a boolean value indicating whether a world border has been
     * hit. In this case we abort the search and wait, to avoid potentially
     * partially loaded multi-blocks.
     * <p>
     * Note that this is also used in {@link TileEntityCasing} for the reverse
     * search when trying to notify a controller.
     * <p>
     * <em>Important</em>: we have to pass along a valid world object here
     * instead of relying on the passed tile entity's world, since we may
     * have caused tile entity creation in rare cases (e.g. broken saves
     * where tile entities were not restored during load), which will not have
     * their world set if this is called from the update loop (where newly
     * created tile entities are added to a separate list, and will be added
     * to their chunk and thus get their world set later on).
     *
     * @param world      the world we're scanning for tile entities in.
     * @param tileEntity the tile entity to get the neighbors for.
     * @param processed  the list of processed tile entities.
     * @param queue      the list of pending tile entities.
     * @return <tt>true</tt> if all neighbors could be checked, <tt>false</tt> otherwise.
     */
    static boolean addNeighbors(final World world, final TileEntity tileEntity, final Set<TileEntity> processed, final Queue<TileEntity> queue) {
        for (final Direction facing : Direction.values()) {
            final BlockPos neighborPos = tileEntity.getBlockPos().relative(facing);
            if (!WorldUtils.isBlockLoaded(world, neighborPos)) {
                return false;
            }

            final TileEntity neighborTileEntity = world.getBlockEntity(neighborPos);
            if (neighborTileEntity == null) {
                continue;
            }
            if (!processed.add(neighborTileEntity)) {
                continue;
            }
            if (neighborTileEntity instanceof TileEntityController || neighborTileEntity instanceof TileEntityCasing) {
                queue.add(neighborTileEntity);
            }
        }
        return true;
    }

    /**
     * Do a scan for connected casings starting from this controller.
     * <p>
     * Sets the state based on error or success and collects all found casings into
     * the {@link #casings} field on success.
     */
    private void scan() {
        final World world = getBlockEntityWorld();

        // List of processed tile entities to avoid loops.
        final Set<TileEntity> processed = new HashSet<>();
        // List of pending tile entities that still need to be scanned.
        final Queue<TileEntity> queue = new ArrayDeque<>();
        // List of new found casings.
        final List<TileEntityCasing> newCasings = new ArrayList<>(CommonConfig.maxCasingsPerController);

        // Start at our location, keep going until there's nothing left to do.
        processed.add(this);
        queue.add(this);
        while (!queue.isEmpty()) {
            final TileEntity tileEntity = queue.remove();

            // Check what we have. We only add controllers and casings to this list,
            // so we can skip the type check in the else branch.
            if (tileEntity instanceof TileEntityController) {
                if (tileEntity == this) {
                    // Special case: first iteration, add the neighbors.
                    if (!addNeighbors(world, tileEntity, processed, queue)) {
                        clear(ControllerState.INCOMPLETE);
                        return;
                    }
                } else {
                    // We require there to be exactly one controller per multi-block.
                    clear(ControllerState.MULTIPLE_CONTROLLERS);
                    return;
                }
            } else /* if (tileEntity instanceof TileEntityCasing) */ {
                // We only allow a certain number of casings per multi-block.
                if (newCasings.size() + 1 > CommonConfig.maxCasingsPerController) {
                    clear(ControllerState.TOO_COMPLEX);
                    return;
                }

                // Register as the controller with the casing and add neighbors.
                final TileEntityCasing casing = (TileEntityCasing) tileEntity;
                newCasings.add(casing);
                addNeighbors(world, casing, processed, queue);
            }
        }

        // Special handling in case we triggered tile entity creation while
        // scanning (see comment on addNeighbors), re-scan next tick when
        // they all have their world object set... but only exit after having
        // touched all of them, to make sure they've been created.
        if (newCasings.stream().anyMatch(c -> !c.hasLevel())) {
            return;
        }

        // Handle splits by first getting the set of casings we originally had
        // control over but no longer, setting their controller to null and
        // telling them to reschedule, just in case (onDisable *should* be fine
        // but better safe than sorry).
        casings.removeAll(newCasings);
        casings.forEach(c -> c.setController(null));
        casings.forEach(TileEntityCasing::scheduleScan);

        // Replace old list of casings with the new found ones, now that we're
        // sure we don't have to disable our old ones.
        casings.clear();
        casings.addAll(newCasings);
        casings.forEach(c -> c.setController(this));

        // Ensure our parts know their neighbors.
        casings.forEach(TileEntityCasing::checkNeighbors);
        checkNeighbors();
        casings.forEach(TileEntityComputer::rebuildOverrides);
        rebuildOverrides();

        // Sort casings for deterministic order of execution (important when modules
        // write / read from multiple ports but only want to make the data available
        // to the first [e.g. execution module's ANY target]).
        casings.sort(Comparator.comparing(TileEntityCasing::getPosition));

        // All done. Make sure this comes after the checkNeighbors or we get CMEs!
        state = ControllerState.READY;
    }

    /**
     * Compute the <em>accumulative</em> redstone power applied to the controller.
     *
     * @return the accumulative redstone signal.
     */
    private int computePower() {
        final World world = getBlockEntityWorld();

        int acc = 0;
        for (final Direction facing : Direction.values()) {
            acc += Math.max(0, Math.min(15, world.getSignal(getBlockPos().relative(facing), facing)));
        }
        return acc;
    }

    /**
     * Advance all computer parts by one step.
     */
    private void step() {
        casings.forEach(TileEntityCasing::stepModules);
        casings.forEach(TileEntityCasing::stepPipes);
        stepPipes();
    }

    /**
     * Clear the list of controlled casings (and clear their controller), then
     * enter the specified state.
     *
     * @param toState the state to enter after clearing.
     */
    private void clear(final ControllerState toState) {
        // Whatever we're clearing to, remove self from all casings first. If
        // we're clearing for a schedule that's fine because we'll find them
        // again (or won't, in which case we're unloading/partially unloaded
        // anyway), if we're disabling because of an error, that's also what
        // we want (because it could be the 'two controllers' error, in which
        // case we don't want to reference one of the controllers since that
        // could lead to confusion.
        for (final TileEntityCasing casing : casings) {
            casing.setController(null);
        }

        // Disable modules if we're in an errored state. If we're in an
        // incomplete state or rescanning, leave the state as is to avoid
        // unnecessarily resetting the computer.
        if (toState != ControllerState.INCOMPLETE) {
            casings.forEach(TileEntityCasing::onDisabled);
        }
        casings.clear();

        state = toState;
    }
}
