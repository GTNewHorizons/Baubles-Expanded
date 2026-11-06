package baubles.common.container;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import baubles.common.Baubles;
import baubles.common.BaublesConfig;
import baubles.common.ItemDebugger;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

public class SlotBauble extends Slot {

	private final String slotType;

	@Deprecated
    public SlotBauble(IInventory inventory, BaubleType legacyType, int slot, int x, int y) {
        super(inventory, slot, x, y);
		slotType = BaubleExpandedSlots.getTypeFromBaubleType(legacyType);

    }

    public SlotBauble(IInventory inventory, String type, int slot, int x, int y) {
        super(inventory, slot, x, y);
        if(type == null) {
        	slotType = BaubleExpandedSlots.unknownType;
        } else {
        	slotType = type;
        }
    }

    public String getSlotType() {
        return slotType;
    }

    /**
     * Check if the stack is a valid item for this slot. Always true beside for the armor slots.
     */
    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack == null || BaubleExpandedSlots.unknownType.equals(slotType)) {
			return false;
		}

		Item item = stack.getItem();
		if(!(item instanceof IBauble) || !((IBauble)item).canEquip(stack, ((InventoryBaubles)inventory).player.get())) {
			return false;
		}

        String[] types;
        if(item instanceof IBaubleExpanded) {
            types = ((IBaubleExpanded)item).getBaubleTypes(stack);
        } else {
            BaubleType legacyType = ((IBauble)item).getBaubleType(stack);
            types = new String[] {BaubleExpandedSlots.getTypeFromBaubleType(legacyType)};
        }

        if (types == null) return false;
        for(String type : types) {
            if (BaublesConfig.canTypeFitSlot(type, slotType)) {
                return true;
            }
        }

		return false;
    }

	@Override
	public void onSlotChanged() {
		super.onSlotChanged();

        if (inventory instanceof InventoryBaubles baubleInventory) {
            baubleInventory.onSlotContentsChanged(getSlotIndex());
        }
	}

	@Override
	public boolean canTakeStack(EntityPlayer player) {
		ItemStack itemStack = getStack();
        if (itemStack == null) {
            return false;
        } else if (itemStack.getItem() instanceof IBauble bauble) {
            return bauble.canUnequip(itemStack, player);
        } else {
            // Consider the rare case of an item that used to be a Bauble, but has this property later removed (no
            // longer implements IBauble). The player should still be able to take it out of the slot.
            return true;
        }
	}

	@Override
    public int getSlotStackLimit() {
        return BaublesConfig.getStackLimitForSlotType(slotType);
    }

    @Override
	@SideOnly(Side.CLIENT)
    public IIcon getBackgroundIconIndex() {
        if (!BaublesConfig.useOldGuiRendering && BaubleExpandedSlots.unknownType.equals(slotType)) {
            return null;
        }
        return ((ItemDebugger) Baubles.itemDebugger).getBackgroundIconForSlotType(slotType);
    }

}
