package baubles.common.container;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import baubles.api.expanded.IBaubleSlotInventory;
import baubles.api.expanded.IBaubleRenderInventory;
import baubles.common.Baubles;
import baubles.common.BaublesConfig;
import baubles.common.lib.ItemStackHelper;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketSyncAllBauble;
import baubles.common.network.PacketSyncBauble;
import baubles.common.network.PacketSyncBaubleRender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;

public class InventoryBaubles implements IBaubleSlotInventory, IBaubleRenderInventory {
    public ItemStack[] stackList;
    private Container eventHandler;
    public WeakReference<EntityPlayer> player;
    public boolean blockEvents = false;
    private BaubleSlotState slotState;
    private long layoutRevision;
    private boolean changingSlots;
    private int callbackDepth;
    private boolean returningItems;
    private BaubleSlotState pendingSlotState;
    private boolean oldGuiRendering = BaublesConfig.useOldGuiRendering;
    private WeakReference<EntityPlayer> loadedPlayer = new WeakReference<>(null);
    private final ArrayDeque<ItemStack> pendingReturns = new ArrayDeque<>();
    private final BitSet hiddenSlots = new BitSet();

    public InventoryBaubles(EntityPlayer player) {
        slotState = new BaubleSlotState(BaubleExpandedSlots.getCurrentSlotAssignments(), Collections.emptyMap());
        stackList = new ItemStack[slotState.getAssignments().length];
        this.player = new WeakReference<>(player);
    }

    @Override
    public String getSlotType(int slot) {
        return slotState.getType(slot);
    }

    @Override
    public String[] getSlotAssignments() {
        return slotState.getAssignments();
    }

    @Override
    public int getSlotCount(String type) {
        return slotState.getCount(type);
    }

    @Override
    public Map<String, Integer> getSlotModifiers(String type) {
        return slotState.getModifiers(type);
    }

    public BaubleSlotState getSlotState() {
        return slotState;
    }

    public long getLayoutRevision() {
        return layoutRevision;
    }

    public boolean usesOldGuiRendering() {
        return oldGuiRendering;
    }

    @Override
    public boolean isSlotRendered(int slot) {
        return isActiveSlot(slot) && !hiddenSlots.get(slot);
    }

    @Override
    public void setSlotRendered(int slot, boolean rendered) {
        EntityPlayer owner = requireServerPlayer();
        if (!isActiveSlot(slot)) throw new IllegalArgumentException("Cannot change visibility of an inactive slot");
        if (isSlotRendered(slot) == rendered) return;
        hiddenSlots.set(slot, !rendered);
        markDirty();
        if (owner instanceof EntityPlayerMP) {
            PacketHandler.sendToTracking(new PacketSyncBaubleRender(owner, slot), owner, true);
        }
    }

    private boolean isActiveSlot(int slot) {
        return slot >= 0 && slot < getSizeInventory()
                && !BaubleExpandedSlots.unknownType.equals(getSlotType(slot));
    }

    public int[] getHiddenSlots() {
        return hiddenSlots.stream().toArray();
    }

    public void applyRenderState(int slot, long revision, boolean rendered) {
        EntityPlayer owner = player.get();
        if (owner != null && owner.worldObj.isRemote && revision == layoutRevision && isActiveSlot(slot)) {
            hiddenSlots.set(slot, !rendered);
        }
    }

    @Override
    public void setSlotModifier(String type, String source, int amount) {
        requireServerPlayer();
        updateSlots(getPendingSlotState().withModifier(type, source, amount));
    }

    @Override
    public void setSlotCount(String type, String source, int amount) {
        requireServerPlayer();
        if (amount < 0 || amount > BaubleSlotState.MAX_SLOTS) {
            throw new IllegalArgumentException("Slot count must be between 0 and " + BaubleSlotState.MAX_SLOTS);
        }
        long contribution = amount;
        contribution -= BaubleExpandedSlots.totalCurrentlyAssignedSlotsOfType(type);
        for (Map.Entry<String, Integer> modifier : getPendingSlotState().getModifiers(type).entrySet()) {
            if (!modifier.getKey().equals(source)) contribution -= modifier.getValue();
        }
        if (contribution < Integer.MIN_VALUE || contribution > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Slot modifier overflow");
        }
        setSlotModifier(type, source, (int) contribution);
    }

    @Override
    public void resetSlotModifiers(String source) {
        requireServerPlayer();
        updateSlots(getPendingSlotState().withoutSource(source));
    }

    private BaubleSlotState getPendingSlotState() {
        return pendingSlotState == null ? slotState : pendingSlotState;
    }

    private EntityPlayer requireServerPlayer() {
        EntityPlayer owner = player.get();
        if (owner == null || owner.worldObj.isRemote) {
            throw new IllegalStateException("Slot mutations require a server player");
        }
        return owner;
    }

    private void updateSlots(BaubleSlotState updated) {
        EntityPlayer owner = requireServerPlayer();
        if (callbackDepth > 0 || changingSlots || returningItems) {
            pendingSlotState = updated;
            return;
        }
        pendingSlotState = null;
        String[] assignments = updated.getAssignments();
        if (Arrays.equals(assignments, getSlotAssignments())
                && updated.getModifiers().equals(slotState.getModifiers())) return;
        changingSlots = true;
        try {
            boolean closedBaubles = owner.openContainer instanceof ContainerPlayerExpanded;
            if (closedBaubles) owner.closeScreen();
            ItemStack[] previous = stackList;
            BaubleSlotState previousState = slotState;
            slotState = updated;
            stackList = Arrays.copyOf(previous, assignments.length);
            layoutRevision++;
            for (int slot = 0; slot < previous.length; slot++) {
                if (previousState.getType(slot).equals(updated.getType(slot)) && slot < stackList.length) continue;
                hiddenSlots.clear(slot);
                ItemStack stack = previous[slot];
                if (slot < stackList.length) stackList[slot] = null;
                if (stack != null) {
                    notifyUnequipped(stack, owner);
                    pendingReturns.add(stack);
                }
            }
            returnOverflowItems();
            markDirty();
            if (owner instanceof EntityPlayerMP serverPlayer) {
                // Correct client predictions made before the close packet arrived.
                if (closedBaubles) serverPlayer.sendContainerToPlayer(serverPlayer.inventoryContainer);
                PacketHandler.sendToTracking(new PacketSyncAllBauble(owner), owner, true);
            }
        } finally {
            changingSlots = false;
        }
    }

    private void notifyUnequipped(ItemStack stack, EntityPlayer owner) {
        if (stack.getItem() instanceof IBauble bauble) {
            callbackDepth++;
            try {
                bauble.onUnequipped(stack, owner);
            } catch (Exception e) {
                Baubles.log.error("Error unequipping bauble {}", stack, e);
            } finally {
                callbackDepth--;
            }
        }
    }

    private void notifyEquipped(ItemStack stack, EntityPlayer owner) {
        if (owner != null && stack.getItem() instanceof IBauble bauble) {
            callbackDepth++;
            try {
                bauble.onEquipped(stack, owner);
            } catch (Exception e) {
                Baubles.log.error("Error equipping bauble {}", stack, e);
            } finally {
                callbackDepth--;
            }
        }
    }

    public void tick() {
        EntityPlayer owner = player.get();
        if (owner == null) return;
        if (!owner.worldObj.isRemote) {
            if (pendingSlotState != null && callbackDepth == 0 && !changingSlots && !returningItems) {
                updateSlots(pendingSlotState);
            }
            if (owner.ticksExisted % 20 == 0) returnOverflowItems();
        }
        for (int slot = 0; slot < getSizeInventory(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (stack == null || !(stack.getItem() instanceof IBauble bauble)) continue;
            callbackDepth++;
            try {
                bauble.onWornTick(stack, owner);
            } finally {
                callbackDepth--;
            }
        }
    }

    public void onSlotContentsChanged(int slot) {
        ItemStack stack = getStackInSlot(slot);
        EntityPlayer owner = player.get();
        if (!blockEvents && owner != null && stack != null && stack.getItem() instanceof IBaubleExpanded bauble) {
            callbackDepth++;
            try {
                bauble.onSlotContentsChanged(stack, owner);
            } catch (Exception e) {
                Baubles.log.error("Error updating bauble {}", stack, e);
            } finally {
                callbackDepth--;
            }
        }
        syncSlotToClients(slot);
    }

    public void applySnapshot(BaubleSlotState state, long revision, ItemStack[] stacks, boolean oldGui) {
        applySnapshot(state, revision, stacks, oldGui, new int[0]);
    }

    public void applySnapshot(BaubleSlotState state, long revision, ItemStack[] stacks, boolean oldGui, int[] hidden) {
        EntityPlayer owner = player.get();
        if (owner == null || !owner.worldObj.isRemote) return;
        if (loadedPlayer.get() == owner && revision < layoutRevision) return;
        if (stacks.length != state.getAssignments().length) {
            throw new IllegalArgumentException("Snapshot items and layout have different sizes");
        }
        ItemStack[] previous = stackList;
        boolean loadAll = loadedPlayer.get() != owner;
        loadedPlayer = new WeakReference<>(owner);
        slotState = state;
        stackList = stacks.clone();
        layoutRevision = revision;
        oldGuiRendering = oldGui;
        hiddenSlots.clear();
        for (int slot : hidden) {
            if (isActiveSlot(slot)) hiddenSlots.set(slot);
        }
        for (int slot = 0; slot < Math.max(previous.length, stacks.length); slot++) {
            ItemStack before = slot < previous.length ? previous[slot] : null;
            ItemStack after = getStackInSlot(slot);
            if (!loadAll && ItemStack.areItemStacksEqual(before, after)) continue;
            if (before != null && !loadAll) notifyUnequipped(before, owner);
            if (after != null && after.getItem() instanceof IBauble bauble) {
                try {
                    bauble.onPlayerLoad(after, owner);
                } catch (Exception e) {
                    Baubles.log.error("Error loading bauble {}", after, e);
                }
            }
        }
    }

    public void returnOverflowItems() {
        EntityPlayer owner = player.get();
        if (owner == null || owner.worldObj.isRemote || returningItems || pendingReturns.isEmpty()) return;
        returningItems = true;
        try {
            int count = pendingReturns.size();
            while (count-- > 0) {
                if (ItemStackHelper.returnToPlayer(owner, pendingReturns.peekFirst())) pendingReturns.removeFirst();
                else pendingReturns.addLast(pendingReturns.removeFirst());
            }
        } finally {
            returningItems = false;
        }
    }

    /**
     * Returns a detached stack, retaining it for later delivery if a drop is cancelled.
     */
    public void returnItem(ItemStack stack) {
        requireServerPlayer();
        if (stack == null || stack.stackSize <= 0) return;
        pendingReturns.add(stack);
        returnOverflowItems();
    }

    public Container getEventHandler() {
        return eventHandler;
    }

    public void setEventHandler(Container eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * Returns the number of slots in the inventory.
     */
    @Override
    public int getSizeInventory() {
        return stackList.length;
    }

    /**
     * Returns the stack in slot i
     */
    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot < 0 || slot >= getSizeInventory() ? null : stackList[slot];
    }

    /**
     * Returns the name of the inventory
     */
    @Override
    public String getInventoryName() {
        return "";
    }

    /**
     * Returns if the inventory is named
     */
    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    /**
     * When some containers are closed they call this on each slot, then drop
     * whatever it returns as an EntityItem - like when you close a workbench
     * GUI.
     */
    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return decrStackSize(slot, Integer.MAX_VALUE);
    }

    /**
     * Removes from an inventory slot (first arg) up to a specified number
     * (second arg) of items and returns them in a new stack.
     */
    @Override
    public ItemStack decrStackSize(int slot, int decrementBy) {
        if (slot < 0 || slot >= stackList.length || decrementBy <= 0) return null;
        if(stackList[slot] != null) {
            ItemStack itemstack;

            if(stackList[slot].stackSize <= decrementBy) {
                itemstack = stackList[slot];
                stackList[slot] = null;

            } else {
                itemstack = stackList[slot].splitStack(decrementBy);

                if(stackList[slot].stackSize == 0) {
                    stackList[slot] = null;
                }
            }
            if (!blockEvents) notifyUnequipped(itemstack, player.get());

            if(eventHandler != null)
                eventHandler.onCraftMatrixChanged(this);
            syncSlotToClients(slot);
            markDirty();
            return itemstack;
        } else {
            return null;
        }
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be
     * crafting or armor sections).
     */
    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stackList.length) return;
        ItemStack previous = stackList[slot];
        boolean changed = !ItemStack.areItemStacksEqual(previous, stack);
        stackList[slot] = stack;
        if (!blockEvents && changed) {
            if (previous != null) notifyUnequipped(previous, player.get());
            if (stack != null) notifyEquipped(stack, player.get());
        }
        if (eventHandler != null) {
            eventHandler.onCraftMatrixChanged(this);
        }
        syncSlotToClients(slot);
        markDirty();
    }

    /**
     * Returns the maximum stack size for an inventory slot.
     */
    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    /**
     * For tile entities, ensures the chunk containing the tile entity is saved
     * to disk later - the game won't think it hasn't changed and skip it.
     */
    @Override
    public void markDirty() {
        try {
            player.get().inventory.markDirty();
        } catch (Exception ignored) {
        }
    }

    /**
     * Do not make give this method the name canInteractWith because it clashes
     * with Container
     */
    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    /**
     * Returns true if automation is allowed to insert the given stack (ignoring
     * stack size) into the given slot.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        String slotType = getSlotType(slot);
        if (stack == null || slot < 0 || slot >= getSizeInventory() || BaubleExpandedSlots.unknownType.equals(slotType)) {
            return false;
        }

        Item item = stack.getItem();
        if(!(item instanceof IBauble) || !((IBauble) item).canEquip(stack, player.get())) {
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

    public void saveNBT(EntityPlayer player) {
        NBTTagCompound tags = player.getEntityData();
        saveNBT(tags);
    }

    public void saveNBT(NBTTagCompound tags) {
        BaubleSlotStorage.write(tags, new BaubleSlotState(slotState.getAssignments(), getPendingSlotState().getModifiers()));
        tags.setIntArray("Baubles.HiddenSlots", getHiddenSlots());
        NBTTagList tagList = new NBTTagList();
        NBTTagCompound invSlot;
        for (int slot = 0; slot < stackList.length; ++slot) {
            if (stackList[slot] != null) {
                invSlot = new NBTTagCompound();
                invSlot.setByte("Slot", (byte) slot);
                invSlot.setInteger("SlotIndex", slot);
                stackList[slot].writeToNBT(invSlot);
                tagList.appendTag(invSlot);
            }
        }
        tags.setTag("Baubles.Inventory", tagList);
        NBTTagList overflow = new NBTTagList();
        for (ItemStack stack : pendingReturns) overflow.appendTag(stack.writeToNBT(new NBTTagCompound()));
        tags.setTag("Baubles.Overflow", overflow);
    }

    public void readNBT(EntityPlayer player) {
        NBTTagCompound tags = player.getEntityData();
        readNBT(tags);
    }

    public void readNBT(NBTTagCompound tags) {
        pendingSlotState = null;
        BaubleSlotState saved = BaubleSlotStorage.read(tags);
        slotState = BaubleSlotState.resolve(BaubleExpandedSlots.getCurrentSlotAssignments(),
                saved.getAssignments(), saved.getModifiers());
        stackList = new ItemStack[slotState.getAssignments().length];
        pendingReturns.clear();
        hiddenSlots.clear();
        for (int slot : tags.getIntArray("Baubles.HiddenSlots")) {
            if (isActiveSlot(slot) && saved.getType(slot).equals(getSlotType(slot))) hiddenSlots.set(slot);
        }
        NBTTagList tagList = tags.getTagList("Baubles.Inventory", 10);
        for (int i = 0; i < tagList.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = tagList.getCompoundTagAt(i);
            int slot = nbttagcompound.hasKey("SlotIndex", 3)
                    ? nbttagcompound.getInteger("SlotIndex") : nbttagcompound.getByte("Slot") & 255;
            ItemStack itemstack = ItemStack.loadItemStackFromNBT(nbttagcompound);
            if (itemstack != null) {
                if (slot >= 0 && slot < stackList.length && stackList[slot] == null
                        && !BaubleExpandedSlots.unknownType.equals(getSlotType(slot))
                        && saved.getType(slot).equals(getSlotType(slot))) {
                    stackList[slot] = itemstack;
                } else {
                    pendingReturns.add(itemstack);
                }
            }
        }
        NBTTagList overflow = tags.getTagList("Baubles.Overflow", 10);
        for (int i = 0; i < overflow.tagCount(); i++) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(overflow.getCompoundTagAt(i));
            if (stack != null) pendingReturns.add(stack);
        }
    }

    public void dropItems(ArrayList<EntityItem> drops) {
        for (int slot = 0; slot < stackList.length; ++slot) {
            if (stackList[slot] != null) {
                EntityItem item = new EntityItem(player.get().worldObj,
                    player.get().posX,
                    player.get().posY + player.get().eyeHeight, player.get().posZ,
                    stackList[slot].copy());
                item.delayBeforeCanPickup = 40;
                float f1 = player.get().worldObj.rand.nextFloat() * 0.5F;
                float f2 = player.get().worldObj.rand.nextFloat() * (float) Math.PI * 2.0F;
                item.motionX = (-MathHelper.sin(f2) * f1);
                item.motionZ = (MathHelper.cos(f2) * f1);
                item.motionY = 0.20000000298023224D;
                drops.add(item);
                stackList[slot] = null;
                syncSlotToClients(slot);
            }
        }
    }

    public void dropItemsAt(ArrayList<EntityItem> drops, Entity entity) {
        for (int slot = 0; slot < stackList.length; ++slot) {
            if (stackList[slot] != null && !ItemStackHelper.isSoulBound(stackList[slot])) {
                EntityItem item = new EntityItem(entity.worldObj,
                    entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
                    stackList[slot].copy());
                item.delayBeforeCanPickup = 40;
                float f1 = entity.worldObj.rand.nextFloat() * 0.5F;
                float f2 = entity.worldObj.rand.nextFloat() * (float) Math.PI * 2.0F;
                item.motionX = (-MathHelper.sin(f2) * f1);
                item.motionZ = (MathHelper.cos(f2) * f1);
                item.motionY = 0.20000000298023224D;
                drops.add(item);
                stackList[slot] = null;
                syncSlotToClients(slot);
            }
        }
    }

    public void syncSlotToClients(int slot) {
        if (changingSlots) return;
        try {
            final EntityPlayer entityPlayer = this.player.get();
            if (entityPlayer instanceof EntityPlayerMP playerMP && !entityPlayer.worldObj.isRemote) {
                PacketHandler.sendToTracking(new PacketSyncBauble(playerMP, slot), playerMP, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
