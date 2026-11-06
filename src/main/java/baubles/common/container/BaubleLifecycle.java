package baubles.common.container;

import baubles.api.IBauble;
import baubles.api.expanded.BaubleSlotContext;
import baubles.api.expanded.IBaubleLifecycle;
import net.minecraft.item.ItemStack;

/**
 * Selects contextual callbacks without changing the legacy interface or invoking it twice.
 */
public class BaubleLifecycle {

    public static void tick(ItemStack stack, BaubleSlotContext context) {
        if (stack.getItem() instanceof IBaubleLifecycle item) item.onBaubleTick(stack, context);
        else if (stack.getItem() instanceof IBauble item) item.onWornTick(stack, context.player());
    }

    public static void equipped(ItemStack stack, BaubleSlotContext context, ItemStack previous) {
        if (stack.getItem() instanceof IBaubleLifecycle item) item.onBaubleEquipped(stack, context, ItemStack.copyItemStack(previous));
        else if (stack.getItem() instanceof IBauble item) item.onEquipped(stack, context.player());
    }

    public static void unequipped(ItemStack stack, BaubleSlotContext context, ItemStack replacement) {
        if (stack.getItem() instanceof IBaubleLifecycle item) item.onBaubleUnequipped(stack, context, ItemStack.copyItemStack(replacement));
        else if (stack.getItem() instanceof IBauble item) item.onUnequipped(stack, context.player());
    }

    public static void loaded(ItemStack stack, BaubleSlotContext context) {
        if (stack.getItem() instanceof IBaubleLifecycle item) item.onBaubleLoaded(stack, context);
        else if (stack.getItem() instanceof IBauble item) item.onPlayerLoad(stack, context.player());
    }

    public static void changed(ItemStack stack, BaubleSlotContext context, ItemStack previous) {
        if (stack.getItem() instanceof IBaubleLifecycle item) item.onBaubleChanged(stack, context, previous);
    }

    public static boolean canEquip(ItemStack stack, BaubleSlotContext context) {
        if (stack.getItem() instanceof IBaubleLifecycle item) return item.canEquipBauble(stack, context);
        return stack.getItem() instanceof IBauble item && (context.cosmetic() || item.canEquip(stack, context.player()));
    }

    public static boolean canUnequip(ItemStack stack, BaubleSlotContext context) {
        if (stack.getItem() instanceof IBaubleLifecycle item) return item.canUnequipBauble(stack, context);
        return !(stack.getItem() instanceof IBauble item) || context.cosmetic() || item.canUnequip(stack, context.player());
    }
}
