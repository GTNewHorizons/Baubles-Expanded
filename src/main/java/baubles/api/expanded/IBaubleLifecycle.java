package baubles.api.expanded;

import baubles.api.IBauble;
import net.minecraft.item.ItemStack;

/**
 * Optional contextual callbacks. Defaults delegate to the original IBauble methods exactly once.
 * Cosmetic contents never receive equipment callbacks or worn ticks.
 */
public interface IBaubleLifecycle extends IBauble {

    default void onBaubleTick(ItemStack stack, BaubleSlotContext context) {
        onWornTick(stack, context.player());
    }

    /**
     * Receives the live equipped stack and a detached copy of the previous contents, or null.
     */
    default void onBaubleEquipped(ItemStack stack, BaubleSlotContext context, ItemStack previous) {
        onEquipped(stack, context.player());
    }

    /**
     * Receives the unequipped stack and a detached copy of its replacement, or null.
     */
    default void onBaubleUnequipped(ItemStack stack, BaubleSlotContext context, ItemStack replacement) {
        onUnequipped(stack, context.player());
    }

    default void onBaubleLoaded(ItemStack stack, BaubleSlotContext context) {
        onPlayerLoad(stack, context.player());
    }

    /**
     * Observes an in-place item change. Previous is a detached copy; this is not a new equip.
     */
    default void onBaubleChanged(ItemStack stack, BaubleSlotContext context, ItemStack previous) {}

    default void onBaubleContentsChanged(ItemStack stack, BaubleSlotContext context) {
        if (this instanceof IBaubleExpanded expanded) expanded.onSlotContentsChanged(stack, context.player());
    }

    default boolean canEquipBauble(ItemStack stack, BaubleSlotContext context) {
        return context.cosmetic() || canEquip(stack, context.player());
    }

    default boolean canUnequipBauble(ItemStack stack, BaubleSlotContext context) {
        return context.cosmetic() || canUnequip(stack, context.player());
    }
}
