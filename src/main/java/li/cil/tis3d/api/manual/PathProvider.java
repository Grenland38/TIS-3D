package li.cil.tis3d.api.manual;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;

/**
 * Allows providing paths for item stacks and blocks in the world.
 * <p>
 * This is used for generating NEI usage pages with a button opening the manual
 * on the page at the specified path, or for opening the manual when held in
 * hand and sneak-activating a block in the world.
 * <p>
 * This way you can easily make entries in your documentation available the
 * same way OpenComputers does it itself.
 * <p>
 * Note that you can use the special variable <tt>%LANGUAGE%</tt> in your
 * paths, for language agnostic paths. These will be resolved to the currently
 * set language, falling back to <tt>en_US</tt>, during actual content lookup.
 */
@OnlyIn(Dist.CLIENT)
public interface PathProvider extends IForgeRegistryEntry<PathProvider> {
    /**
     * Get the path to the documentation page for the provided item stack.
     * <p>
     * Return <tt>null</tt> if there is no known page for this item, allowing
     * other providers to be queried.
     *
     * @param stack the stack to get the documentation path to.
     * @return the path to the page, <tt>null</tt> if none is known.
     */
    @Nullable
    String pathFor(final ItemStack stack);

    /**
     * Get the path to the documentation page for the provided block.
     * <p>
     * Return <tt>null</tt> if there is no known page for this item, allowing
     * other providers to be queried.
     *
     * @param world the world containing the block.
     * @param pos   the position coordinate of the block.
     * @param side  the face of the block.
     * @return the path to the page, <tt>null</tt> if none is known.
     */
    @Nullable
    String pathFor(final World world, final BlockPos pos, final Direction side);
}
