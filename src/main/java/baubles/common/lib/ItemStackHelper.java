package baubles.common.lib;

import baubles.common.BaublesConfig;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

public class ItemStackHelper {

    public static boolean returnToPlayer(EntityPlayer player, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return true;
        InventoryPlayer inventory = player.inventory;
        for (int pass = 0; pass < 2 && stack.stackSize > 0; pass++) {
            for (int slot = 0; slot < inventory.mainInventory.length && stack.stackSize > 0; slot++) {
                ItemStack target = inventory.mainInventory[slot];
                if ((pass == 0) != (target != null) || !inventory.isItemValidForSlot(slot, stack)) continue;
                int limit = Math.min(stack.getMaxStackSize(), inventory.getInventoryStackLimit());
                if (target != null) {
                    if (!stack.isStackable() || !stack.isItemEqual(target)
                            || !ItemStack.areItemStackTagsEqual(stack, target)) continue;
                    int moved = Math.min(stack.stackSize, limit - target.stackSize);
                    if (moved <= 0) continue;
                    target.stackSize += moved;
                    target.animationsToGo = 5;
                    stack.stackSize -= moved;
                } else {
                    int moved = Math.min(stack.stackSize, limit);
                    if (moved <= 0) continue;
                    ItemStack inserted = stack.splitStack(moved);
                    inserted.animationsToGo = 5;
                    inventory.setInventorySlotContents(slot, inserted);
                }
                inventory.markDirty();
            }
        }
        if (stack.stackSize <= 0) return true;
        ItemStack attempted = stack.copy();
        int before = countMatching(inventory, attempted);
        EntityItem dropped = player.dropPlayerItemWithRandomChoice(stack, false);
        if (dropped != null && (dropped.addedToChunk || dropped.isDead || player.capturedDrops.contains(dropped))) return true;
        int restored = countMatching(inventory, attempted) - before;
        if (restored >= attempted.stackSize || stack.stackSize <= 0) return true;
        int alreadyConsumed = attempted.stackSize - stack.stackSize;
        stack.stackSize -= Math.min(stack.stackSize, Math.max(0, restored - alreadyConsumed));
        return stack.stackSize <= 0;
    }

    private static int countMatching(InventoryPlayer inventory, ItemStack expected) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && expected.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(expected, stack)) {
                count += stack.stackSize;
            }
        }
        ItemStack cursor = inventory.getItemStack();
        if (cursor != null && expected.isItemEqual(cursor) && ItemStack.areItemStackTagsEqual(expected, cursor)) {
            count += cursor.stackSize;
        }
        return count;
    }

    public static boolean isSoulBound(ItemStack itemStack) {
        if(BaublesConfig.soulBoundEnchantments.length >= 0) {
            NBTTagList enchantments = itemStack.getEnchantmentTagList();
            if(enchantments != null) {
                for(int i = 0; i < enchantments.tagCount(); i++) {
                    int itemEnchantID = enchantments.getCompoundTagAt(i).getInteger("id");
                    for(int soulBoundID : BaublesConfig.soulBoundEnchantments) {
                        if(itemEnchantID == soulBoundID) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
