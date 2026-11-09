package baubles.common.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.lib.ItemStackHelper;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketSyncBaubleCosmetic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class InventoryBaubleCosmetics implements IInventory {

    private final InventoryBaubles owner;
    private ItemStack[] stacks;
    private ItemStack[] previousStacks = new ItemStack[0];

    public InventoryBaubleCosmetics(InventoryBaubles owner) {
        this.owner = owner;
        stacks = new ItemStack[owner.stackList.length];
    }

    @Override
    public int getSizeInventory() {
        return stacks.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < stacks.length ? stacks[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack current = getStackInSlot(slot);
        if (current == null || amount <= 0) return null;
        ItemStack taken = current.stackSize <= amount ? current : current.splitStack(amount);
        if (taken == current || current.stackSize == 0) stacks[slot] = null;
        markDirty();
        syncSlot(slot);
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (stack != null) setInventorySlotContents(slot, null);
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stacks.length) return;
        stacks[slot] = stack;
        markDirty();
        syncSlot(slot);
    }

    @Override
    public String getInventoryName() {
        return "container.baubles.cosmetics";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
        owner.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return owner.player.get() == player;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        String type = owner.getSlotType(slot);
        return owner.isSlotActive(slot) && owner.getSlotDefinition(type).cosmetic()
                && owner.matchesSlotType(slot, stack) && BaubleLifecycle.canEquip(stack, owner.getSlotContext(slot, true));
    }

    public void syncSlot(int slot) {
        if (previousStacks.length != stacks.length) previousStacks = Arrays.copyOf(previousStacks, stacks.length);
        if (slot >= 0 && slot < stacks.length) previousStacks[slot] = ItemStack.copyItemStack(stacks[slot]);
        EntityPlayer player = owner.player.get();
        if (player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
            PacketHandler.sendToTracking(new PacketSyncBaubleCosmetic(player, slot), player, true);
        }
    }

    public void detectChanges() {
        if (previousStacks.length != stacks.length) previousStacks = Arrays.copyOf(previousStacks, stacks.length);
        for (int slot = 0; slot < stacks.length; slot++) {
            if (!ItemStack.areItemStacksEqual(previousStacks[slot], stacks[slot])) syncSlot(slot);
        }
    }

    public InventoryBaubles getOwner() {
        return owner;
    }

    public void resize(BaubleSlotState previous, Consumer<ItemStack> removed) {
        ItemStack[] old = stacks;
        stacks = Arrays.copyOf(old, owner.getSizeInventory());
        for (int slot = 0; slot < old.length; slot++) {
            if (slot < stacks.length && previous.getType(slot).equals(owner.getSlotType(slot))
                    && owner.getSlotDefinition(owner.getSlotType(slot)).cosmetic()
                    && (old[slot] == null || owner.matchesSlotType(slot, old[slot]))) continue;
            if (slot < stacks.length) stacks[slot] = null;
            if (old[slot] != null) removed.accept(old[slot]);
        }
        previousStacks = copyStacks();
    }

    public ItemStack[] copyStacks() {
        ItemStack[] copy = new ItemStack[stacks.length];
        for (int slot = 0; slot < copy.length; slot++) copy[slot] = ItemStack.copyItemStack(stacks[slot]);
        return copy;
    }

    public void applySnapshot(ItemStack[] snapshot) {
        if (snapshot.length != owner.getSizeInventory()) throw new IllegalArgumentException("Cosmetic snapshot size differs");
        stacks = snapshot.clone();
        previousStacks = copyStacks();
    }

    public void writeNBT(NBTTagCompound tags) {
        NBTTagList entries = new NBTTagList();
        for (int slot = 0; slot < stacks.length; slot++) {
            if (stacks[slot] == null) continue;
            NBTTagCompound entry = stacks[slot].writeToNBT(new NBTTagCompound());
            entry.setInteger("SlotIndex", slot);
            entries.appendTag(entry);
        }
        tags.setTag("Baubles.Cosmetics", entries);
    }

    public void readNBT(NBTTagCompound tags, BaubleSlotState saved, Consumer<ItemStack> removed) {
        stacks = new ItemStack[owner.getSizeInventory()];
        NBTTagList entries = tags.getTagList("Baubles.Cosmetics", 10);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            int slot = entry.getInteger("SlotIndex");
            ItemStack stack = ItemStack.loadItemStackFromNBT(entry);
            if (stack == null) continue;
            if (slot >= 0 && slot < stacks.length && stacks[slot] == null
                    && !BaubleExpandedSlots.unknownType.equals(owner.getSlotType(slot))
                    && owner.getSlotDefinition(owner.getSlotType(slot)).cosmetic()
                    && owner.matchesSlotType(slot, stack)
                    && saved.getType(slot).equals(owner.getSlotType(slot))) stacks[slot] = stack;
            else removed.accept(stack);
        }
        previousStacks = copyStacks();
    }

    public void dropItems(ArrayList<EntityItem> drops, Entity entity, boolean keepSoulBound) {
        for (int slot = 0; slot < stacks.length; slot++) {
            ItemStack stack = stacks[slot];
            if (stack == null || keepSoulBound && ItemStackHelper.isSoulBound(stack)) continue;
            EntityItem drop = new EntityItem(entity.worldObj, entity.posX, entity.posY + entity.getEyeHeight(),
                    entity.posZ, stack.copy());
            drop.delayBeforeCanPickup = 40;
            drops.add(drop);
            stacks[slot] = null;
            syncSlot(slot);
        }
    }
}
