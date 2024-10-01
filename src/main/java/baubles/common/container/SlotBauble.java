package baubles.common.container;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import baubles.common.Baubles;
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

    /**
     * Check if the stack is a valid item for this slot. Always true beside for the armor slots.
     */
    @Override
    public boolean isItemValid(ItemStack stack) {
    	if (stack == null) {
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

        for(String type : types) {
            if(type.equals(BaubleExpandedSlots.universalType) || type.equals(slotType)) {
                return true;
            }
        }

		return false;
    }

	@Override
	public boolean canTakeStack(EntityPlayer player) {
		ItemStack itemStack = getStack();
		return itemStack != null && ((IBauble)itemStack.getItem()).canUnequip(itemStack, player);
	}

	@Override
    public int getSlotStackLimit() {
        return 1;
    }

	@Override
	@SideOnly(Side.CLIENT)
    public IIcon getBackgroundIconIndex() {
        return ((ItemDebugger) Baubles.itemDebugger).getBackgroundIconForSlotType(slotType);
    }

}
