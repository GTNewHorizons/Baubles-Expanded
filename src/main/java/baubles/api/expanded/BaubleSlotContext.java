package baubles.api.expanded;

import com.github.bsideup.jabel.Desugar;

import net.minecraft.entity.player.EntityPlayer;

/**
 * A slot's context at the time of a callback. Slot is the stable inventory index;
 * index is the position among slots of the same type. Do not retain the player.
 */
@Desugar
public record BaubleSlotContext(EntityPlayer player, String type, int slot, int index,
        boolean cosmetic, boolean rendered, boolean active) {
}
