package baubles.api.expanded;

import net.minecraft.inventory.IInventory;

/**
 * Optional per-slot model visibility, independent of equipment and gameplay effects.
 * Renderers must consult this capability before drawing an equipped bauble.
 */
public interface IBaubleRenderInventory extends IInventory {

    /**
     * Returns whether the active slot is visible. Empty active slots retain their preference.
     */
    boolean isSlotRendered(int slot);

    /**
     * Sets visibility on the server game thread and synchronizes tracking clients.
     * The slot must be active. This does not equip, unequip or modify its item.
     */
    void setSlotRendered(int slot, boolean rendered);
}
