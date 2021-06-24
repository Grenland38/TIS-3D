package li.cil.tis3d.common.container;

import li.cil.tis3d.api.API;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Containers {
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, API.MOD_ID);

    // --------------------------------------------------------------------- //

    public static final RegistryObject<ContainerType<ReadOnlyMemoryModuleContainer>> READ_ONLY_MEMORY_MODULE = CONTAINERS.register("read_only_memory_module", () -> IForgeContainerType.create(ReadOnlyMemoryModuleContainer::create));

    // --------------------------------------------------------------------- //

    public static void initialize() {
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
