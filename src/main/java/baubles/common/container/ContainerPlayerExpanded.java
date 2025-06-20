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

import javax.annotation.Nonnull;

import static baubles.common.BaublesConfig.useOldGuiRendering;

public class ContainerPlayerExpanded extends Container {

    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 2, 2);
    public IInventory craftResult = new InventoryCraftResult();
    public InventoryBaubles baubles;

    private final EntityPlayer thePlayer;

    public ContainerPlayerExpanded(InventoryPlayer playerInv, boolean par2, EntityPlayer player) {
        this.thePlayer = player;
        baubles = new InventoryBaubles(player);
        baubles.setEventHandler(this);
        if (!player.worldObj.isRemote) {
            baubles.stackList = PlayerHandler.getPlayerBaubles(player).stackList;
        }

        int i;
        int j;

        if (!useOldGuiRendering) {
            this.addSlotToContainer(new SlotCrafting(playerInv.player, this.craftMatrix, this.craftResult, 0, 144, 36));

            for (i = 0; i < 2; ++i) {
                for (j = 0; j < 2; ++j) {
                    this.addSlotToContainer(new Slot(this.craftMatrix, j + i * 2, 88 + j * 18, 26 + i * 18));
                }
            }
        }

        //armor
        for (i = 0; i < 4; ++i) {
            final int k = i;
            this.addSlotToContainer(new Slot(playerInv, playerInv.getSizeInventory() - 1 - i, 8, 8 + i * 18) {
                @Override
                public int getSlotStackLimit() { return 1; }

                @Override
                public boolean isItemValid(ItemStack itemStack) {
                    if (itemStack == null) return false;
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

        //Bauble Slots
        for(i = 0; i < BaubleExpandedSlots.slotLimit; i++) {
            String slotType = BaubleExpandedSlots.getSlotType(i);
            if(BaublesConfig.showUnusedSlots || !slotType.equals(BaubleExpandedSlots.unknownType)) {
                if (useOldGuiRendering) {
                    addSlotToContainer(new SlotBauble(baubles, slotType, i, slotStartX + (slotOffset * (i / 4)), slotStartY + (slotOffset * (i % 4))));
                } else {
                    addSlotToContainer(new SlotBauble(baubles, slotType, i, -18, 12 + (slotOffset * i)));
                }
            }
        }

        //inventory slots
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInv, j + (i + 1) * 9, slotStartY + j * slotOffset, 84 + i * 18));
            }
        }

        //hotbar slots
        for (i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(playerInv, i, slotStartY + i * slotOffset, 142));
        }

        if (!useOldGuiRendering) {
            this.onCraftMatrixChanged(this.craftMatrix);
            this.scrollTo(0);
        }
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
        if (!useOldGuiRendering) {
            for (int i = 0; i < 4; ++i) {
                ItemStack itemstack = this.craftMatrix.getStackInSlotOnClosing(i);

                if (itemstack != null) {
                    player.dropPlayerItemWithRandomChoice(itemstack, false);
                }
            }

            this.craftResult.setInventorySlotContents(0, (ItemStack) null);
            if (!player.worldObj.isRemote) {
                PlayerHandler.setPlayerBaubles(player, baubles);
            }
        }
    }

    public void scrollTo(float offset) {

        if (!canScroll()) return;
        final int activeBaubleSlots = BaublesConfig.showUnusedSlots ? BaubleExpandedSlots.slotLimit : BaubleExpandedSlots.slotsCurrentlyUsed();

        if (offset < 0) offset = 0;
        if (offset > 1) offset = 1;

        int shownslots = 8;
        int slotOffset = (int) ((double) (offset * (float) (activeBaubleSlots - shownslots)) + 0.5F);


        if (slotOffset < 0) {
            slotOffset = 0;
        }

        for (int i = 0; i < activeBaubleSlots && i < BaubleExpandedSlots.slotLimit; i++) {
            //TODO: Find a way to not use a magic number to get to the correct ID range for the Slot editing.
            Slot slot = (Slot) this.inventorySlots.get(9 + i);
            int displayIndex = i;
            if (displayIndex >= 0 && displayIndex < activeBaubleSlots) {
                slot.yDisplayPosition = (12 - (slotOffset * 18) + (displayIndex) * 18);
                if (slot.yDisplayPosition < 12 || slot.yDisplayPosition > 8 * 18) {
                    //Hide the rest of the slots!
                    slot.yDisplayPosition = -2000;
                }
            }
        }
    }

    public boolean canScroll() {
        if (BaubleExpandedSlots.slotsCurrentlyUsed() > 8 && !useOldGuiRendering) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    /**
     * Called when a player shift-clicks on a slot. You must override this or you will crash when someone does that.
     */
    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack returnStack = null;
        Slot slot = (Slot) inventorySlots.get(slotIndex);
        final int visibleBaubleSlots = BaublesConfig.showUnusedSlots ? BaubleExpandedSlots.slotLimit : BaubleExpandedSlots.slotsCurrentlyUsed();
        int craftingActive = 5;
        if (useOldGuiRendering) {
            craftingActive = 0;
        }

        if(slot != null && slot.getHasStack()) {
            ItemStack originalStack = slot.getStack();
            returnStack = originalStack.copy();
            Item item = returnStack.getItem();

            if (!useOldGuiRendering) {
                if (slotIndex == 0) {
                    if (!mergeItemStack(originalStack, 4 + craftingActive + visibleBaubleSlots, 40 + craftingActive + visibleBaubleSlots, true)) {
                        return null;
                    }
                    slot.onSlotChange(originalStack, returnStack);
                } else if (slotIndex >= 1 && slotIndex < 5) {
                    if (!mergeItemStack(originalStack, 4 + craftingActive + visibleBaubleSlots, 40 + craftingActive + visibleBaubleSlots, false)) {
                        return null;
                    }
                }
            }
            if (item instanceof ItemArmor && !((Slot) inventorySlots.get(craftingActive + ((ItemArmor) item).armorType)).getHasStack()) {
                int armorSlot = craftingActive + ((ItemArmor) item).armorType;
                if(!mergeItemStack(originalStack, armorSlot, armorSlot + 1, false)) {
                    returnStack = null;
                }
            } else if(slotIndex >= 4 + craftingActive + visibleBaubleSlots && item instanceof IBauble && ((IBauble) item).canEquip(returnStack, thePlayer)) {
                for(int baubleSlot = 4 + craftingActive; baubleSlot < 4 + craftingActive + visibleBaubleSlots; baubleSlot++) {
                    if(returnStack == null) {
                        break;
                    }
                	if(!((Slot) inventorySlots.get(baubleSlot)).getHasStack()) {
                		String[] types;
                		if(item instanceof IBaubleExpanded) {
                			types = ((IBaubleExpanded) item).getBaubleTypes(returnStack);
                		} else {
                			types = new String[] {BaubleExpandedSlots.getTypeFromBaubleType(((IBauble)item).getBaubleType(returnStack))};
                		}
                		for(String type : types) {
                			if((type.equals(BaubleExpandedSlots.universalType)
                                || type.equals(BaubleExpandedSlots.getSlotType(baubleSlot - 4 - craftingActive)))
                                && !mergeItemStack(originalStack, baubleSlot, baubleSlot + 1, false)) {
                                returnStack = null;
                			}
                		}
                	}
                }
            } else if(slotIndex >= 4 + craftingActive + visibleBaubleSlots && slotIndex < 31 + craftingActive + visibleBaubleSlots) {
                if(!mergeItemStack(originalStack, 31 + craftingActive + visibleBaubleSlots, 40 + craftingActive + visibleBaubleSlots, false)) {
                    returnStack = null;
                }
            } else if(slotIndex >= 31 + craftingActive + visibleBaubleSlots && slotIndex < 40 + craftingActive + visibleBaubleSlots) {
                if(!mergeItemStack(originalStack, 4 + craftingActive + visibleBaubleSlots, 31 + craftingActive + visibleBaubleSlots, false)) {
                    returnStack = null;
                }
            } else if(!mergeItemStack(originalStack, 4 + craftingActive + visibleBaubleSlots, 40 + craftingActive + visibleBaubleSlots, false, slot)) {
                returnStack = null;
            }

            if(originalStack.stackSize <= 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }

            if(returnStack != null && originalStack.stackSize == returnStack.stackSize) {
                returnStack = null;
            }

            slot.onPickupFromSlot(player, originalStack);
        }

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

    protected boolean mergeItemStack(ItemStack itemStack, int par2, int par3, boolean par4, Slot ss) {
        boolean flag1 = false;
        int k = par2;

        if (par4) {
            k = par3 - 1;
        }

        Slot slot;
        ItemStack itemstack1;

        if (itemStack.isStackable()) {
            while (itemStack.stackSize > 0 && (!par4 && k < par3 || par4 && k >= par2)) {
                slot = (Slot)inventorySlots.get(k);
                itemstack1 = slot.getStack();

                if (itemstack1 != null && itemstack1.getItem() == itemStack.getItem() && (!itemStack.getHasSubtypes() || itemStack.getItemDamage() == itemstack1.getItemDamage()) && ItemStack.areItemStackTagsEqual(itemStack, itemstack1)) {
                    int l = itemstack1.stackSize + itemStack.stackSize;
                    if (l <= itemStack.getMaxStackSize()) {
                        if (ss instanceof SlotBauble) unequipBauble(itemStack);
                        itemStack.stackSize = 0;
                        itemstack1.stackSize = l;
                        slot.onSlotChanged();
                        flag1 = true;
                    } else if (itemstack1.stackSize < itemStack.getMaxStackSize()) {
                        if (ss instanceof SlotBauble) unequipBauble(itemStack);
                        itemStack.stackSize -= itemStack.getMaxStackSize() - itemstack1.stackSize;
                        itemstack1.stackSize = itemStack.getMaxStackSize();
                        slot.onSlotChanged();
                        flag1 = true;
                    }
                }

                if (par4) {
                    --k;
                } else {
                    ++k;
                }
            }
        }

        if (itemStack.stackSize > 0) {
            if (par4) {
                k = par3 - 1;
            } else {
                k = par2;
            }

            while (!par4 && k < par3 || par4 && k >= par2) {
                slot = (Slot)inventorySlots.get(k);
                itemstack1 = slot.getStack();

                if (itemstack1 == null) {
                    if (ss instanceof SlotBauble) unequipBauble(itemStack);
                    slot.putStack(itemStack.copy());
                    slot.onSlotChanged();
                    itemStack.stackSize = 0;
                    flag1 = true;
                    break;
                }

                if (par4) {
                    --k;
                } else {
                    ++k;
                }
            }
        }
        return flag1;
    }

}
