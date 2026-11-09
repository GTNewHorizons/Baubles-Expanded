package baubles.common.container;

import baubles.api.BaubleType;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.BaubleSlotType;
import baubles.api.expanded.BaubleSlotContext;
import baubles.common.BaubleSlotConfiguration;
import baubles.common.Baubles;
import baubles.common.BaublesConfig;
import baubles.common.ItemDebugger;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
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

    public InventoryBaubles getBaublesInventory() {
        if (inventory instanceof InventoryBaubleCosmetics cosmetics) return cosmetics.getOwner();
        return inventory instanceof InventoryBaubles baubles ? baubles : null;
    }

    public BaubleSlotType getSlotDefinition() {
        InventoryBaubles baubles = getBaublesInventory();
        return baubles != null ? baubles.getSlotDefinition(slotType)
                : BaubleSlotConfiguration.getDefinitions().getOrDefault(slotType, BaubleSlotType.defaults(slotType));
    }

    public boolean isCosmeticSlot() {
        return inventory instanceof InventoryBaubleCosmetics;
    }

    public boolean isSlotActive() {
        InventoryBaubles baubles = getBaublesInventory();
        return baubles == null || baubles.isSlotActive(getSlotIndex());
    }

    /**
     * Check if the stack is a valid item for this slot. Always true beside for the armor slots.
     */
    @Override
    public boolean isItemValid(ItemStack stack) {
        return inventory.isItemValidForSlot(getSlotIndex(), stack);
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
        InventoryBaubles baubles = getBaublesInventory();
        BaubleSlotContext context = baubles != null ? baubles.getSlotContext(getSlotIndex(), isCosmeticSlot())
                : new BaubleSlotContext(player, slotType, getSlotIndex(), getSlotIndex(), false, true, true);
        return itemStack != null && isSlotActive()
                && BaubleLifecycle.canUnequip(itemStack, context);
	}

	@Override
    public int getSlotStackLimit() {
        return getSlotDefinition().stackLimit();
    }

    @Override
	@SideOnly(Side.CLIENT)
    public IIcon getBackgroundIconIndex() {
        if (!getSlotDefinition().icon().isEmpty()) return null;
        if (!BaublesConfig.useOldGuiRendering && BaubleExpandedSlots.unknownType.equals(slotType)) {
            return null;
        }
        return ((ItemDebugger) Baubles.itemDebugger).getBackgroundIconForSlotType(slotType);
    }

}
