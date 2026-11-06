package baubles.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class SlotBaubleCosmetic extends SlotBauble {

    public SlotBaubleCosmetic(InventoryBaubleCosmetics inventory, String type, int slot, int x, int y) {
        super(inventory, type, slot, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return inventory.isItemValidForSlot(getSlotIndex(), stack);
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return super.canTakeStack(player);
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    @Override
    public void onSlotChanged() {
        inventory.markDirty();
        ((InventoryBaubleCosmetics) inventory).syncSlot(getSlotIndex());
    }
}
