package baubles.common.container;

import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import baubles.common.BaublesConfig;
import baubles.common.lib.PlayerHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.IIcon;

import static baubles.common.BaublesConfig.maxColumns;
import static baubles.common.BaublesConfig.useOldGuiRendering;

public class ContainerPlayerExpanded extends Container {

    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 2, 2);
    public IInventory craftResult = new InventoryCraftResult();
    public InventoryBaubles baubles;

    private final EntityPlayer thePlayer;

    private int slotsAdded = 0;

    private int baubleFirstSlotIndex = -1;
    private int baubleSlotCount = 0;

    public ContainerPlayerExpanded(InventoryPlayer playerInv, boolean isClient, EntityPlayer player) {
        this.thePlayer = player;
        baubles = PlayerHandler.getPlayerBaubles(player);
        baubles.setEventHandler(this);

        int i;
        int j;

        // Crafting slots
        if (!useOldGuiRendering) {
            this.addSlotToContainer(new SlotCrafting(playerInv.player, this.craftMatrix, this.craftResult, 0, 144, 36));

            for (i = 0; i < 2; ++i) {
                for (j = 0; j < 2; ++j) {
                    this.addSlotToContainer(new Slot(this.craftMatrix, j + i * 2, 88 + j * 18, 26 + i * 18));
                }
            }
        }

        // Armor slots
        for (i = 0; i < 4; ++i) {
            final int k = i;
            this.addSlotToContainer(new Slot(playerInv, playerInv.getSizeInventory() - 1 - i, 8, 8 + i * 18) {
                @Override
                public int getSlotStackLimit() { return 1; }

                @Override
                public boolean isItemValid(ItemStack itemStack) {
                    if (itemStack == null || itemStack.getItem() == null) return false;
                    return itemStack.getItem().isValidArmor(itemStack, k, thePlayer);
                }

                @Override
                @SideOnly(Side.CLIENT)
                public IIcon getBackgroundIconIndex() {
                    return ItemArmor.func_94602_b(k);
                }
            });
        }

        final int slotOffset = 18;
        final int slotStartX = 80;
        final int slotStartY = 8;

        // Bauble slots
        baubleFirstSlotIndex = slotsAdded;
        String[] currentSlotAssignments = BaubleExpandedSlots.getCurrentSlotAssignments();
        for (String slotType: currentSlotAssignments) {
            if (BaublesConfig.showUnusedSlots || !slotType.equals(BaubleExpandedSlots.unknownType)) {
                Slot slot = useOldGuiRendering
                    ? new SlotBauble(baubles, slotType, baubleSlotCount, slotStartX + (slotOffset * (baubleSlotCount / 4)), slotStartY + (slotOffset * (baubleSlotCount % 4)))
                    : new SlotBauble(baubles, slotType, baubleSlotCount, -18 - (18 * (baubleSlotCount % getColumns())), 12 + (slotOffset * (baubleSlotCount / getColumns())));
                addSlotToContainer(slot);
                baubleSlotCount++;
            }
        }

        // Inventory slots
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + (i + 1) * 9, slotStartY + j * slotOffset, 84 + i * 18));
            }
        }

        // Hotbar slots
        for (i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(playerInv, i, slotStartY + i * slotOffset, 142));
        }

        if (!useOldGuiRendering) {
            this.onCraftMatrixChanged(this.craftMatrix);
            this.scrollTo(0);
        }
    }

    @Override
    protected Slot addSlotToContainer(Slot slot) {
        // Count slots as we add them so we can determine what the first Bauble slot index will be
        slotsAdded++;
        return super.addSlotToContainer(slot);
    }

    @Override
    public void onCraftMatrixChanged(IInventory par1IInventory) {
        if (!useOldGuiRendering) {
            this.craftResult.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(this.craftMatrix, this.thePlayer.worldObj));
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (useOldGuiRendering) {
            return;
        }
        for (int i = 0; i < 4; ++i) {
            ItemStack itemstack = this.craftMatrix.getStackInSlotOnClosing(i);

            if (itemstack != null) {
                if (!player.inventory.addItemStackToInventory(itemstack)) {
                    player.dropPlayerItemWithRandomChoice(itemstack, false);
                }
            }
        }

        this.craftResult.setInventorySlotContents(0, null);
        if (!player.worldObj.isRemote) {
            PlayerHandler.setPlayerBaubles(player, baubles);
        }
    }

    public int getBaubleSlotCount() {
        return this.baubleSlotCount;
    }

    public SlotBauble getBaubleSlot(int slotIndex) {
        if(slotIndex < 0 || slotIndex >= baubleSlotCount) return null;
        return (SlotBauble) inventorySlots.get(baubleFirstSlotIndex + slotIndex);
    }

    public void scrollTo(float offset) {
        if (!canScroll()) return;
        final int activeBaubleSlots = BaubleExpandedSlots.slotsCurrentlyUsed();
        final int columns = getColumns();

        offset = Math.max(0, Math.min(1, offset));

        int shownRows = 8;
        int totalRows = (activeBaubleSlots + columns - 1) / columns;
        int slotRowOffset = (int) (offset * (totalRows - shownRows) + 0.5F);

        if (slotRowOffset < 0) {
            slotRowOffset = 0;
        }

        for (int i = 0; i < activeBaubleSlots && i < BaubleExpandedSlots.slotsCurrentlyUsed(); i++) {
            Slot slot = (Slot) this.inventorySlots.get(baubleFirstSlotIndex + i);
            int row = i / columns;
            int scrolledRow = row - slotRowOffset;
            slot.yDisplayPosition = 12 + scrolledRow * 18;
            if (slot.yDisplayPosition < 12 || slot.yDisplayPosition > 8 * 18) {
                slot.yDisplayPosition = -2000;
            }
        }
    }

    public int getColumns() {
        int activeBaubleSlots = BaubleExpandedSlots.slotsCurrentlyUsed();
        for (int cols = 1; cols < maxColumns; cols++) {
            if ((activeBaubleSlots + cols - 1) / cols <= 8) {
                return cols;
            }
        }
        return maxColumns;
    }

    public boolean canScroll() {
        return BaubleExpandedSlots.slotsCurrentlyUsed() > maxColumns * 8 && !useOldGuiRendering;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    /**
     * Called when a player shift-clicks on a slot. You must override this, or you will crash when someone does that.
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack returnStack = null;
        Slot slot = (Slot) inventorySlots.get(slotIndex);
        final int visibleBaubleSlots = BaubleExpandedSlots.slotsCurrentlyUsed();
        int craftingActive = 5;
        if (useOldGuiRendering) {
            craftingActive = 0;
        }

        if (slot == null || !slot.getHasStack()) {
            return returnStack;
        }
        ItemStack originalStack = slot.getStack();
        returnStack = originalStack.copy();
        Item item = returnStack.getItem();

        final int baubleStart = 4 + craftingActive;
        final int baubleEnd   = baubleStart + visibleBaubleSlots;
        final int invEnd      = baubleEnd + 27;
        final int hotbarEnd   = invEnd + 9;

        if (!useOldGuiRendering && slotIndex == 0) {
            if (!mergeItemStack(originalStack, baubleEnd, hotbarEnd, true)) {
                return null;
            }
            slot.onSlotChange(originalStack, returnStack);
        } else if (!useOldGuiRendering && slotIndex < 5) {
            if (!mergeItemStack(originalStack, baubleEnd, hotbarEnd, false)) {
                return null;
            }
        } else if (slotIndex >= baubleStart && slotIndex < baubleEnd) {
            if (!mergeItemStack(originalStack, baubleEnd, hotbarEnd, false, slot)) {
                returnStack = null;
            }
        } else if (item instanceof ItemArmor armor
                && !((Slot) inventorySlots.get(craftingActive + armor.armorType)).getHasStack()) {
            int armorSlot = craftingActive + armor.armorType;
            if (!mergeItemStack(originalStack, armorSlot, armorSlot + 1, false)) {
                returnStack = null;
            }
        } else if (item instanceof IBauble bauble && bauble.canEquip(returnStack, thePlayer)) {
            String[] types = item instanceof IBaubleExpanded
                ? ((IBaubleExpanded) item).getBaubleTypes(returnStack)
                : new String[] {BaubleExpandedSlots.getTypeFromBaubleType(bauble.getBaubleType(returnStack))};

            // Priority 2: direct slot matches (or universal items)
            tryMergeIntoBaubleSlotsByPriority(originalStack, baubleStart, baubleEnd, types, 2);

            // Priority 1: override matches (e.g. amulet/ring/belt into universal)
            if (originalStack.stackSize > 0) {
                tryMergeIntoBaubleSlotsByPriority(originalStack, baubleStart, baubleEnd, types, 1);
            }
        } else if (slotIndex >= baubleEnd && slotIndex < invEnd) {
            if (!mergeItemStack(originalStack, invEnd, hotbarEnd, false)) {
                returnStack = null;
            }
        } else if (slotIndex >= invEnd && slotIndex < hotbarEnd) {
            if (!mergeItemStack(originalStack, baubleEnd, invEnd, false)) {
                returnStack = null;
            }
        } else if (!mergeItemStack(originalStack, baubleEnd, hotbarEnd, false, slot)) {
            returnStack = null;
        }

        if (originalStack.stackSize <= 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }

        if (returnStack != null && originalStack.stackSize == returnStack.stackSize) {
            returnStack = null;
        }

        slot.onPickupFromSlot(player, originalStack);

        return returnStack;
    }

    private void unequipBauble(ItemStack stack) {
        //if (stack.getItem() instanceof IBauble) {
        //    ((IBauble)stack.getItem()).onUnequipped(stack, thePlayer);
        //}
    }

    @Override
    public void putStacksInSlots(ItemStack[] p_75131_1_) {
        baubles.blockEvents=true;
        super.putStacksInSlots(p_75131_1_);
    }

    private void tryMergeIntoBaubleSlotsByPriority(ItemStack sourceStack, int baubleStart, int baubleEnd, String[] itemTypes, int priority) {
        for (int baubleSlot = baubleStart; baubleSlot < baubleEnd && sourceStack.stackSize > 0; baubleSlot++) {
            Slot targetInventorySlot = (Slot) inventorySlots.get(baubleSlot);
            if (targetInventorySlot.getHasStack()) {
                continue;
            }
            String targetSlotType = targetInventorySlot instanceof SlotBauble
                ? ((SlotBauble) targetInventorySlot).getSlotType()
                : BaubleExpandedSlots.unknownType;
            if (getBestMatchPriority(itemTypes, targetSlotType) != priority) {
                continue;
            }
            mergeItemStack(sourceStack, baubleSlot, baubleSlot + 1, false);
        }
    }

    private int getBestMatchPriority(String[] itemTypes, String slotType) {
        int best = 0;
        for (String type : itemTypes) {
            if (type == null) {
                continue;
            }
            if (type.equals(BaubleExpandedSlots.universalType) || type.equals(slotType)) {
                return 2;
            }
            if (BaublesConfig.canTypeFitSlot(type, slotType)) {
                best = 1;
            }
        }
        return best;
    }

    protected boolean mergeItemStack(ItemStack sourceStack, int startIndex, int endIndex, boolean reverse, Slot sourceSlot) {
        boolean merged = false;
        int index = reverse ? endIndex - 1 : startIndex;

        Slot targetSlot;
        ItemStack targetStack;

        // First pass: try stacking into existing stacks
        if (sourceStack.isStackable()) {
            while (sourceStack.stackSize > 0 && (!reverse && index < endIndex || reverse && index >= startIndex)) {
                targetSlot = (Slot) inventorySlots.get(index);
                targetStack = targetSlot.getStack();

                if (targetStack != null && targetStack.getItem() == sourceStack.getItem() &&
                    (!sourceStack.getHasSubtypes() || sourceStack.getItemDamage() == targetStack.getItemDamage()) &&
                    ItemStack.areItemStackTagsEqual(sourceStack, targetStack)) {

                    int combinedSize = targetStack.stackSize + sourceStack.stackSize;

                    if (combinedSize <= sourceStack.getMaxStackSize()) {
                        if (sourceSlot instanceof SlotBauble) {
                            unequipBauble(sourceStack);
                        }
                        sourceStack.stackSize = 0;
                        targetStack.stackSize = combinedSize;
                        targetSlot.onSlotChanged();
                        return true;
                    } else if (targetStack.stackSize < sourceStack.getMaxStackSize()) {
                        if (sourceSlot instanceof SlotBauble) {
                            unequipBauble(sourceStack);
                        }
                        sourceStack.stackSize -= sourceStack.getMaxStackSize() - targetStack.stackSize;
                        targetStack.stackSize = sourceStack.getMaxStackSize();
                        targetSlot.onSlotChanged();
                        merged = true;
                    }
                }

                index = reverse ? index - 1 : index + 1;
            }
        }

        // Second pass: put into empty slots
        index = reverse ? endIndex - 1 : startIndex;
        while (!reverse && index < endIndex || reverse && index >= startIndex) {
            targetSlot = (Slot) inventorySlots.get(index);
            targetStack = targetSlot.getStack();

            if (targetStack == null) {
                if (sourceSlot instanceof SlotBauble) {
                    unequipBauble(sourceStack);
                }
                targetSlot.putStack(sourceStack.copy());
                targetSlot.onSlotChanged();
                sourceStack.stackSize = 0;
                return true;
            }

            index = reverse ? index - 1 : index + 1;
        }

        return merged;
    }
}
