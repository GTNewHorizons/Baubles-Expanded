package baubles.api.expanded;

import com.github.bsideup.jabel.Desugar;

import net.minecraft.item.ItemStack;

/**
 * A matching functional item and its slot context. The stack belongs to the inventory.
 */
@Desugar
public record BaubleSlotResult(BaubleSlotContext context, ItemStack stack) {
}
