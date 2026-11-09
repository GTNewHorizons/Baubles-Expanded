package baubles.api.expanded;

import net.minecraft.inventory.IInventory;

/**
 * Optional cosmetic storage with the same slot indices as the functional inventory.
 * Cosmetic items never provide equipment effects. Mutations belong on the server game thread.
 */
public interface IBaubleCosmeticInventory extends IInventory {

    /**
     * Returns the persistent cosmetic inventory. Its contents only replace rendered models.
     */
    IInventory getCosmeticInventory();
}
