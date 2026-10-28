package xyz.iwolfking.woldsvaults.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import xyz.iwolfking.woldsvaults.WoldsVaults;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Calls Modded Item Borders' {@code ModdedItemBorders.render} without a compile dependency. Only reached from the
 * storage-screen mixin, which is restricted to packs that ship the mod.
 */
public final class ModdedItemBordersBridge {
    private static MethodHandle render;
    private static boolean resolved;

    private ModdedItemBordersBridge() {
    }

    /** Draws the item's rarity border at (x, y), exactly as Modded Item Borders does in vanilla container screens. */
    public static void render(PoseStack poseStack, ItemStack stack, int x, int y) {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> owner = Class.forName("com.radimous.moddeditemborders.ModdedItemBorders");
                render = MethodHandles.publicLookup().findStatic(owner, "render",
                        MethodType.methodType(void.class, PoseStack.class, ItemStack.class, int.class, int.class));
            } catch (ReflectiveOperationException e) {
                WoldsVaults.LOGGER.error("Modded Item Borders render hook not found; storage screens will show items without rarity borders", e);
            }
        }
        if (render == null) {
            return;
        }
        try {
            render.invokeExact(poseStack, stack, x, y);
        } catch (Throwable e) {
            render = null;
            WoldsVaults.LOGGER.error("Modded Item Borders render failed; disabling rarity borders in storage screens", e);
        }
    }
}
