package baubles.common.container;

import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import baubles.common.BaublesConfig;
import baubles.common.lib.PlayerHandler;
import baubles.common.network.PacketHandler;
import baubles.common.network.client.PacketScrollBaubleClient;
import baubles.common.network.server.PacketScrollBaubleServer;
import codechicken.nei.api.IInfiniteItemHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.TreeMap;

public class ContainerPlayerExpanded extends Container {

    public SortedMap<Integer, String> baubleSlotMap = new TreeMap<>();
    SortedMap<Integer, String> shownFilteredSlots = new TreeMap<>();
    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 2, 2);
    public IInventory craftResult = new InventoryCraftResult();
    public InventoryBaubles baubles;

    private int lastScrollIndex;

    public boolean isLocalWorld;
    private final EntityPlayer thePlayer;

    public ContainerPlayerExpanded(InventoryPlayer playerInv, boolean par2, EntityPlayer player) {
        this.isLocalWorld = player.worldObj.isRemote;
        this.thePlayer = player;
        baubles = new InventoryBaubles(player);
        baubles.setEventHandler(this);
        if (!player.worldObj.isRemote) {
            baubles.stackList = PlayerHandler.getPlayerBaubles(player).stackList;
        }

        this.addSlotToContainer(new SlotCrafting(playerInv.player, this.craftMatrix, this.craftResult, 0, 144, 36));

        int i;
        int j;

        for (i = 0; i < 2; ++i) {
            for (j = 0; j < 2; ++j) {
                this.addSlotToContainer(new Slot(this.craftMatrix, j + i * 2, 88 + j * 18, 26 + i * 18));
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
        final int slotStartY = 8;

        //inventory slots
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                addSlotToContainer(new Slot(playerInv, j + (i + 1) * 9, slotStartY + j * slotOffset, 84 + i * 18));
            }
        }

        //hotbar slots
        for (i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(playerInv, i, slotStartY + i * slotOffset, 142));
        }

        this.onCraftMatrixChanged(this.craftMatrix);
        this.scrollToIndex(0);
    }

    @Override
    public void onCraftMatrixChanged(IInventory par1IInventory) {
        this.craftResult.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(this.craftMatrix, this.thePlayer.worldObj));
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        for (int i = 0; i < 4; ++i) {
            ItemStack itemstack = this.craftMatrix.getStackInSlotOnClosing(i);

            if (itemstack != null) {
                player.dropPlayerItemWithRandomChoice(itemstack, false);
            }
        }

        this.craftResult.setInventorySlotContents(0, (ItemStack)null);
        if (!player.worldObj.isRemote) {
            PlayerHandler.setPlayerBaubles(player, baubles);
        }
    }

    public void scrollToIndex(int indexIn) {
        String[] numberOfActiveSlots = BaubleExpandedSlots.getCurrentSlotAssignments();
        final int visibleBaubleSlots = BaublesConfig.showUnusedSlots ? BaubleExpandedSlots.slotLimit : BaubleExpandedSlots.slotsCurrentlyUsed();
        //System.out.print(Arrays.toString(numberOfActiveSlots));

        int slots = 0;
        int yOffset = 12;
        int index = 0;
        this.inventorySlots.subList(45, this.inventorySlots.size()).clear();
        this.inventoryItemStacks.subList(45, this.inventoryItemStacks.size()).clear();

        for (int i = 0; i < numberOfActiveSlots.length && i < BaubleExpandedSlots.slotLimit; i++) {
            if (!numberOfActiveSlots[i].equals(BaubleExpandedSlots.unknownType)) {
                baubleSlotMap.put(i, numberOfActiveSlots[i]);
                //System.out.println(baubleSlotMap);
            }
        }
        //System.out.println(baubleSlotMap);
        for (int identifier : baubleSlotMap.keySet()) {
            //System.out.println(identifier + " " + baubleSlotMap.get(identifier));

            String shownSlot = baubleSlotMap.get(identifier);

            //System.out.println(identifier + " " + baubleSlotMap.get(identifier));
        for (int i = 0; i + 1 < shownSlot.length() && slots < 8; i++) {
            String slotType = BaubleExpandedSlots.getSlotType(i);
            //System.out.println(slotType + i);
            if (index >= indexIn) {
                addSlotToContainer(new SlotBauble(baubles, slotType, i, -18, yOffset));
                yOffset += 18;
                slots++;
                //System.out.println(shownSlot + " " + identifier);
            }
            index++;
        }
        }
        if (!this.isLocalWorld) {
            PacketHandler.INSTANCE.sendTo(new PacketScrollBaubleServer(this.windowId, indexIn), (EntityPlayerMP) this.thePlayer);
        }
        lastScrollIndex = indexIn;
    }

    public void scrollTo(float offset) {

        if (!canScroll()) return;

        if (offset < 0) offset = 0;
        if (offset > 1) offset = 1;


        int k = Math.min (baubleSlotMap.size(), 8);

        int j = (int)((double)(offset * (float) k) + 0.5D);

        if (j < 0) {
            j = 0;
        }

        if (j == this.lastScrollIndex) {
            return;
        }

        shownFilteredSlots = baubleSlotMap.subMap(this.lastScrollIndex * (int) offset, (int)(offset * (float) k));
        System.out.println(lastScrollIndex + " " + offset + " " + k);
        System.out.println(shownFilteredSlots);

        if (this.isLocalWorld) {
            PacketHandler.INSTANCE.sendToServer(new PacketScrollBaubleClient(this.windowId, j));
        }
        //baubleSlotMap.subMap( , (int)(offset * (float) k));
        /*
        for ()
         this.baubleSlotMap.subMap();
        int k = (baubleSlotMap.get());
        int j = (int)((double)(pos * (float) k) + 0.5D);

        if (j < 0) {
            j = 0;
        }

        if (j == this.lastScrollIndex) {
            return;
        }
         */


    }

    public boolean canScroll() {
        if (BaubleExpandedSlots.slotsCurrentlyUsed() > 8) {
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

        if(slot != null && slot.getHasStack()) {
            ItemStack originalStack = slot.getStack();
            returnStack = originalStack.copy();
            Item item = returnStack.getItem();

            if (slotIndex == 0) {
                if (!mergeItemStack(originalStack, 9 + visibleBaubleSlots, 45 + visibleBaubleSlots, true)) {
                    return null;
                }
                slot.onSlotChange(originalStack, returnStack);
            }
            else if (slotIndex >= 1 && slotIndex < 5) {
                if (!mergeItemStack(originalStack, 9 + visibleBaubleSlots, 45 + visibleBaubleSlots, false)) {
                    return null;
                }
            }
            else if (slotIndex >= 5 && slotIndex < 9) {
                if (!mergeItemStack(originalStack, 9 + visibleBaubleSlots, 45 + visibleBaubleSlots, false)) {
                    return null;
                }
            }
            else if (item instanceof ItemArmor && !((Slot) inventorySlots.get(5 + ((ItemArmor) item).armorType)).getHasStack()) {
                int armorSlot = 5 + ((ItemArmor) item).armorType;
                if(!mergeItemStack(originalStack, armorSlot, armorSlot + 1, false)) {
                    returnStack = null;
                }
            } else if(slotIndex >= 9 + visibleBaubleSlots && item instanceof IBauble && ((IBauble) item).canEquip(returnStack, thePlayer)) {
                for(int baubleSlot = 9; baubleSlot < 4 + visibleBaubleSlots; baubleSlot++) {
                    if(returnStack == null) {
                        break;
                    }
                	if(!((Slot) inventorySlots.get(baubleSlot)).getHasStack()) {
                		String[] types;
                		if(item instanceof IBaubleExpanded) {
                			types = ((IBaubleExpanded)item).getBaubleTypes(returnStack);
                		} else {
                			types = new String[] {BaubleExpandedSlots.getTypeFromBaubleType(((IBauble)item).getBaubleType(returnStack))};
                		}
                		for(String type : types) {
                			if((type.equals(BaubleExpandedSlots.universalType) || type.equals(BaubleExpandedSlots.getSlotType(baubleSlot - 4))) && !mergeItemStack(originalStack, baubleSlot, baubleSlot + 1, false)) {
                                returnStack = null;
                			}
                		}
                	}
                }
            } else if(slotIndex >= 9 + visibleBaubleSlots && slotIndex < 36 + visibleBaubleSlots) {
                if(!mergeItemStack(originalStack, 36 + visibleBaubleSlots, 45 + visibleBaubleSlots, false)) {
                    returnStack = null;
                }
            } else if(slotIndex >= 36 + visibleBaubleSlots && slotIndex < 45 + visibleBaubleSlots) {
                if(!mergeItemStack(originalStack, 9 + visibleBaubleSlots, 36 + visibleBaubleSlots, false)) {
                    returnStack = null;
                }
            } else if(!mergeItemStack(originalStack, 9 + visibleBaubleSlots, 45 + visibleBaubleSlots, false, slot)) {
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
