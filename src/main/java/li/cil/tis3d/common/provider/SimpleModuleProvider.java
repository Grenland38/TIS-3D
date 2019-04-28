package li.cil.tis3d.common.provider;

import li.cil.tis3d.api.machine.Casing;
import li.cil.tis3d.api.machine.Face;
import li.cil.tis3d.api.module.Module;
import li.cil.tis3d.api.module.ModuleProvider;
import li.cil.tis3d.common.init.Items;
import li.cil.tis3d.common.item.ItemModule;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class SimpleModuleProvider<T extends Module> implements ModuleProvider {
    private final String moduleName;
    private final BiFunction<Casing, Face, T> moduleConstructor;

    public SimpleModuleProvider(final String moduleName, final BiFunction<Casing, Face, T> moduleConstructor, @Nullable final Supplier<ItemModule> itemConstructor) {
        this.moduleName = moduleName;
        this.moduleConstructor = moduleConstructor;
        if (itemConstructor != null) {
            Items.registerModuleItemOverride(moduleName, itemConstructor);
        }
    }

    public SimpleModuleProvider(final String moduleName, final BiFunction<Casing, Face, T> moduleConstructor) {
        this(moduleName, moduleConstructor, null);
    }

    public boolean worksWith(final ItemStack stack, final Casing casing, final Face face) {
        return stack.getItem() == Items.getModules().get(moduleName);
    }

    @Nullable
    @Override
    public Module createModule(final ItemStack stack, final Casing casing, final Face face) {
        return moduleConstructor.apply(casing, face);
    }
}
