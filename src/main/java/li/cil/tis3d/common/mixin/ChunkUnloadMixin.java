package li.cil.tis3d.common.mixin;

import li.cil.tis3d.common.block.entity.AbstractComputerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(World.class)
public abstract class ChunkUnloadMixin {
    @Shadow
    @Final
    protected List<BlockEntity> unloadedBlockEntities;

    @Inject(method = "tickBlockEntities", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = {"ldc=blockEntities"}))
    private void onBlockEntityChunkUnload(final CallbackInfo ci) {
        if (!unloadedBlockEntities.isEmpty()) {
            for (final BlockEntity blockEntity : unloadedBlockEntities) {
                if (blockEntity instanceof AbstractComputerBlockEntity) {
                    ((AbstractComputerBlockEntity)blockEntity).onChunkUnload();
                }
            }
        }
    }
}
