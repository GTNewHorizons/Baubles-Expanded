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

import java.util.ArrayList;
import java.util.List;

import static baubles.common.BaublesConfig.maxColumns;

public class ContainerPlayerExpanded extends Container {

    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 2, 2);
    public IInventory craftResult = new InventoryCraftResult();
    public InventoryBaubles baubles;

    private final EntityPlayer thePlayer;
    private final boolean useOldGuiRendering;
    private final long layoutRevision;
    private final List<SlotBauble> visibleBaubleSlots = new ArrayList<>();

    private int slotsAdded = 0;

    private int baubleFirstSlotIndex = -1;
    private int baubleSlotCount = 0;

    public ContainerPlayerExpanded(InventoryPlayer playerInv, boolean isClient, EntityPlayer player) {
        this.thePlayer = player;
        baubles = PlayerHandler.getPlayerBaubles(player);
        useOldGuiRendering = baubles.usesOldGuiRendering();
        layoutRevision = baubles.getLayoutRevision();
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
        String[] currentSlotAssignments = baubles.getSlotAssignments();
        for (String type : currentSlotAssignments) {
            if (BaublesConfig.showUnusedSlots || !BaubleExpandedSlots.unknownType.equals(type)) baubleSlotCount++;
        }
        int visibleIndex = 0;
        for (int index = 0; index < currentSlotAssignments.length; index++) {
            String type = currentSlotAssignments[index];
            boolean visible = BaublesConfig.showUnusedSlots || !BaubleExpandedSlots.unknownType.equals(type);
            int x = useOldGuiRendering ? slotStartX + slotOffset * (visibleIndex / 4) : -18 - 18 * (visibleIndex % getColumns());
            int y = useOldGuiRendering ? slotStartY + slotOffset * (visibleIndex % 4) : 12 + slotOffset * (visibleIndex / getColumns());
            SlotBauble slot = new SlotBauble(baubles, type, index, x, y);
            addSlotToContainer(slot);
            if (visible) {
                visibleBaubleSlots.add(slot);
                visibleIndex++;
            } else slot.yDisplayPosition = -2000;
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
        ItemStack cursor = player.inventory.getItemStack();
        player.inventory.setItemStack(null);
        super.onContainerClosed(player);
        if (cursor != null && !player.worldObj.isRemote) PlayerHandler.getPlayerBaubles(player).returnItem(cursor);

        if (baubles != null && baubles.getEventHandler() == this) {
            baubles.setEventHandler(null);
        }

        if (useOldGuiRendering) {
            return;
        }
        for (int i = 0; i < 4; ++i) {
            ItemStack itemstack = this.craftMatrix.getStackInSlotOnClosing(i);

            if (itemstack != null && !player.worldObj.isRemote) PlayerHandler.getPlayerBaubles(player).returnItem(itemstack);
        }

        this.craftResult.setInventorySlotContents(0, null);
    }

    public int getBaubleSlotCount() {
        return this.baubleSlotCount;
    }

    public SlotBauble getBaubleSlot(int slotIndex) {
        if(slotIndex < 0 || slotIndex >= baubleSlotCount) return null;
        return visibleBaubleSlots.get(slotIndex);
    }

    public boolean usesOldGuiRendering() {
        return useOldGuiRendering;
    }

    public void scrollTo(float offset) {
        if (!canScroll()) return;
        final int activeBaubleSlots = baubleSlotCount;
        final int columns = getColumns();

        offset = Math.max(0, Math.min(1, offset));

        int shownRows = 8;
        int totalRows = (activeBaubleSlots + columns - 1) / columns;
        int slotRowOffset = (int) (offset * (totalRows - shownRows) + 0.5F);

        if (slotRowOffset < 0) {
            slotRowOffset = 0;
        }

        for (int i = 0; i < activeBaubleSlots; i++) {
            Slot slot = visibleBaubleSlots.get(i);
            int row = i / columns;
            int scrolledRow = row - slotRowOffset;
            slot.yDisplayPosition = 12 + scrolledRow * 18;
            if (slot.yDisplayPosition < 12 || slot.yDisplayPosition > 8 * 18) {
                slot.yDisplayPosition = -2000;
            }
        }
    }

    public int getColumns() {
        int activeBaubleSlots = baubleSlotCount;
        for (int cols = 1; cols < maxColumns; cols++) {
            if ((activeBaubleSlots + cols - 1) / cols <= 8) {
                return cols;
            }
        }
        return maxColumns;
    }

    public boolean canScroll() {
        return baubleSlotCount > (long) maxColumns * 8 && !useOldGuiRendering;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player == thePlayer && player.isEntityAlive() && baubles == PlayerHandler.getPlayerBaubles(player)
            && layoutRevision == baubles.getLayoutRevision();
    }

    @Override
    public ItemStack slotClick(int slot, int button, int mode, EntityPlayer player) {
        if (!canInteractWith(player) || slot >= inventorySlots.size() || slot < -1 && slot != -999
                || mode < 0 || mode > 6 || button < 0) return null;
        if (mode == 2 ? button > 8 : mode == 3 ? button != 2
                : mode == 5 ? button > 10 || (button & 3) > 2 : button > 1) return null;
        return super.slotClick(slot, button, mode, player);
    }

    /**
     * Called when a player shift-clicks on a slot. You must override this, or you will crash when someone does that.
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        if (!canInteractWith(player) || slotIndex < 0 || slotIndex >= inventorySlots.size()) return null;
        ItemStack returnStack = null;
        Slot slot = (Slot) inventorySlots.get(slotIndex);
        final int visibleBaubleSlots = baubles.getSizeInventory();
        int craftingActive = 5;
        if (useOldGuiRendering) {
            craftingActive = 0;
        }

        if (slot == null || !slot.getHasStack() || !slot.canTakeStack(player)) {
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
            ItemStack remaining = originalStack.copy();
            if (!mergeItemStack(remaining, baubleEnd, hotbarEnd, false, slot)) return null;
            ItemStack removed = slot.decrStackSize(originalStack.stackSize - remaining.stackSize);
            slot.onPickupFromSlot(player, removed);
            return returnStack;
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
            if (types == null) return null;

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

    @Override
    public void putStacksInSlots(ItemStack[] p_75131_1_) {
        boolean previous = baubles.blockEvents;
        baubles.blockEvents = true;
        try {
            super.putStacksInSlots(p_75131_1_);
        } finally {
            baubles.blockEvents = previous;
        }
    }

    private void tryMergeIntoBaubleSlotsByPriority(ItemStack sourceStack, int baubleStart, int baubleEnd, String[] itemTypes, int priority) {
        // First pass: merge into occupied, compatible bauble slots.
        for (int baubleSlot = baubleStart; baubleSlot < baubleEnd && sourceStack.stackSize > 0; baubleSlot++) {
            Slot targetInventorySlot = (Slot) inventorySlots.get(baubleSlot);
            if (!targetInventorySlot.getHasStack()) {
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

        // Second pass: place remaining items into empty, compatible bauble slots.
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

    @Override
    protected boolean mergeItemStack(ItemStack sourceStack, int startIndex, int endIndex, boolean reverse) {
        boolean merged = false;
        for (int pass = 0; pass < 2 && sourceStack.stackSize > 0; pass++) {
            for (int index = reverse ? endIndex - 1 : startIndex;
                    sourceStack.stackSize > 0 && index >= startIndex && index < endIndex;
                    index += reverse ? -1 : 1) {
                Slot targetSlot = (Slot) inventorySlots.get(index);
                ItemStack targetStack = targetSlot.getStack();
                if ((pass == 0) != (targetStack != null) || !targetSlot.isItemValid(sourceStack)) continue;
                int limit = Math.min(sourceStack.getMaxStackSize(), targetSlot.getSlotStackLimit());
                if (targetStack != null) {
                    if (!sourceStack.isStackable() || !sourceStack.isItemEqual(targetStack)
                            || !ItemStack.areItemStackTagsEqual(sourceStack, targetStack)) continue;
                    int moved = Math.min(sourceStack.stackSize, limit - targetStack.stackSize);
                    if (moved <= 0) continue;
                    sourceStack.stackSize -= moved;
                    targetStack.stackSize += moved;
                    targetSlot.onSlotChanged();
                } else {
                    int moved = Math.min(sourceStack.stackSize, limit);
                    if (moved <= 0) continue;
                    ItemStack inserted = sourceStack.splitStack(moved);
                    targetSlot.putStack(inserted);
                }
                merged = true;
            }
        }
        return merged;
    }

    protected boolean mergeItemStack(ItemStack sourceStack, int startIndex, int endIndex, boolean reverse, Slot sourceSlot) {
        return mergeItemStack(sourceStack, startIndex, endIndex, reverse);
    }
}
