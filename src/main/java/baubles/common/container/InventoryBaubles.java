package baubles.common.container;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import baubles.api.expanded.IBaubleSlotInventory;
import baubles.api.expanded.IBaubleSlotSettings;
import baubles.api.expanded.IBaubleLifecycle;
import baubles.api.expanded.BaubleSlotContext;
import baubles.api.expanded.BaubleSlotType;
import baubles.common.Baubles;
import baubles.common.BaublesConfig;
import baubles.common.BaubleSlotConfiguration;
import baubles.common.lib.ItemStackHelper;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketSyncAllBauble;
import baubles.common.network.PacketSyncBauble;
import baubles.common.network.BaubleSlotCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import java.util.Collections;
import java.util.Map;

public class InventoryBaubles implements IBaubleSlotInventory, IBaubleSlotSettings {
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
    private ItemStack[] previousStacks = new ItemStack[0];

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

    /**
     * Exports effective rules for a detached container without exporting equipment or invoking callbacks.
     */
    public NBTTagCompound writeContainerData() {
        NBTTagCompound data = new NBTTagCompound();
        ByteBuf buffer = Unpooled.buffer();
        try {
            BaubleSlotCodec.writeState(buffer, slotState);
            byte[] state = new byte[buffer.readableBytes()];
            buffer.readBytes(state);
            data.setByteArray("SlotState", state);
        } finally {
            buffer.release();
        }
        data.setBoolean("OldGui", oldGuiRendering);
        return data;
    }

    /**
     * Initializes a detached client inventory with server rules and no equipment side effects.
     * Vanilla container packets supply the contents separately.
     */
    public void readContainerData(NBTTagCompound data) {
        EntityPlayer owner = player.get();
        if (owner == null || !owner.worldObj.isRemote || !blockEvents) {
            throw new IllegalStateException("Container data requires a detached client inventory with events blocked");
        }
        ByteBuf buffer = Unpooled.wrappedBuffer(data.getByteArray("SlotState"));
        BaubleSlotState state;
        try {
            state = BaubleSlotCodec.readState(buffer);
            if (buffer.isReadable()) throw new IllegalArgumentException("Trailing container slot data");
        } finally {
            buffer.release();
        }
        if (state.getAssignments().length != stackList.length) throw new IllegalArgumentException("Container slot count differs");
        slotState = state;
        oldGuiRendering = data.getBoolean("OldGui");
        resetStackSnapshots();
    }

    public long getLayoutRevision() {
        return layoutRevision;
    }

    public boolean usesOldGuiRendering() {
        return oldGuiRendering;
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < getSizeInventory()
                && !BaubleExpandedSlots.unknownType.equals(getSlotType(slot));
    }

    @Override
    public boolean isSlotActive(int slot) {
        return slotState.isActive(slot);
    }

    @Override
    public void setSlotActive(int slot, boolean active) {
        requireServerPlayer();
        updateSlots(getPendingSlotState().withSlotActive(slot, active));
    }

    @Override
    public void setSlotsActive(String type, boolean active) {
        requireServerPlayer();
        BaubleSlotState.validateType(type);
        updateSlots(getPendingSlotState().withSlotsActive(type, active));
    }

    @Override
    public Map<String, BaubleSlotType> getSlotDefinitions() {
        return slotState.getDefinitions();
    }

    @Override
    public BaubleSlotType getSlotDefinition(String type) {
        return slotState.getDefinition(type);
    }

    @Override
    public BaubleSlotContext getSlotContext(int slot, boolean cosmetic) {
        return context(slotState, slot, cosmetic);
    }

    private BaubleSlotContext context(BaubleSlotState state, int slot, boolean cosmetic) {
        boolean active = state.isActive(slot);
        boolean rendered = active;
        return new BaubleSlotContext(player.get(), state.getType(slot), slot, state.getTypeIndex(slot), cosmetic, rendered, active);
    }

    public boolean canTypeFitSlot(String itemType, String slotType) {
        if (itemType == null || slotType == null) return false;
        return itemType.equals(slotType) || itemType.equals(BaubleExpandedSlots.universalType)
                || slotType.equals(BaubleExpandedSlots.universalType) && getSlotDefinition(itemType).fitsUniversal();
    }

    public BaubleSlotState prepareConfiguration(String[] baseline, Map<String, BaubleSlotType> definitions) {
        requireServerPlayer();
        if (callbackDepth > 0 || changingSlots || returningItems) throw new IllegalStateException("Inventory is busy");
        return getPendingSlotState().rebase(baseline, definitions);
    }

    public void applyConfiguration(BaubleSlotState state) {
        updateSlots(state);
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
                && updated.getModifiers().equals(slotState.getModifiers())
                && updated.getDefinitions().equals(slotState.getDefinitions())
                && updated.getInactiveSlots().equals(slotState.getInactiveSlots())) return;
        changingSlots = true;
        try {
            boolean closedBaubles = owner.openContainer instanceof ContainerPlayerExpanded;
            if (closedBaubles) owner.closeScreen();
            ItemStack[] previous = stackList;
            BaubleSlotState previousState = slotState;
            BaubleSlotContext[] oldContexts = new BaubleSlotContext[previous.length];
            for (int slot = 0; slot < previous.length; slot++) oldContexts[slot] = context(previousState, slot, false);
            slotState = updated;
            stackList = Arrays.copyOf(previous, assignments.length);
            layoutRevision++;
            for (int slot = 0; slot < previous.length; slot++) {
                boolean retained = previousState.getType(slot).equals(updated.getType(slot)) && slot < stackList.length;
                if (retained) {
                    if (previous[slot] != null && !matchesSlotType(slot, previous[slot])) {
                        stackList[slot] = null;
                        if (previousState.isActive(slot)) notifyUnequipped(previous[slot], oldContexts[slot], null);
                        pendingReturns.add(previous[slot]);
                        continue;
                    }
                    if (previous[slot] != null && previous[slot].stackSize > getSlotDefinition(getSlotType(slot)).stackLimit()) {
                        ItemStack before = previous[slot].copy();
                        pendingReturns.add(previous[slot].splitStack(previous[slot].stackSize - getSlotDefinition(getSlotType(slot)).stackLimit()));
                        if (updated.isActive(slot)) notifyStackChanged(slot, previous[slot], before);
                    }
                    if (previous[slot] != null && previousState.isActive(slot) != updated.isActive(slot)) {
                        if (previousState.isActive(slot)) notifyUnequipped(previous[slot], oldContexts[slot], previous[slot]);
                        else notifyEquipped(previous[slot], getSlotContext(slot, false), previous[slot]);
                    }
                    continue;
                }
                ItemStack stack = previous[slot];
                if (slot < stackList.length) stackList[slot] = null;
                if (stack != null) {
                    if (previousState.isActive(slot)) notifyUnequipped(stack, oldContexts[slot], null);
                    pendingReturns.add(stack);
                }
            }
            resetStackSnapshots();
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

    private void notifyUnequipped(ItemStack stack, BaubleSlotContext context, ItemStack replacement) {
        if (stack.getItem() instanceof IBauble bauble) {
            callbackDepth++;
            try {
                BaubleLifecycle.unequipped(stack, context, replacement);
            } catch (Exception e) {
                Baubles.log.error("Error unequipping bauble {}", stack, e);
            } finally {
                callbackDepth--;
            }
        }
    }

    private void notifyEquipped(ItemStack stack, BaubleSlotContext context, ItemStack previous) {
        if (context.player() != null && stack.getItem() instanceof IBauble) {
            callbackDepth++;
            try {
                BaubleLifecycle.equipped(stack, context, previous);
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
            if (stack == null || !isSlotActive(slot) || !(stack.getItem() instanceof IBauble)) continue;
            callbackDepth++;
            try {
                BaubleLifecycle.tick(stack, getSlotContext(slot, false));
            } finally {
                callbackDepth--;
            }
        }
        if (!owner.worldObj.isRemote) {
            detectStackChanges();
        }
    }

    private void resetStackSnapshots() {
        previousStacks = new ItemStack[stackList.length];
        for (int slot = 0; slot < stackList.length; slot++) previousStacks[slot] = ItemStack.copyItemStack(stackList[slot]);
    }

    private void detectStackChanges() {
        if (previousStacks.length != stackList.length) previousStacks = Arrays.copyOf(previousStacks, stackList.length);
        for (int slot = 0; slot < stackList.length; slot++) {
            ItemStack current = stackList[slot];
            ItemStack previous = previousStacks[slot];
            if (ItemStack.areItemStacksEqual(current, previous)) continue;
            if (current != null && current.stackSize <= 0) current = stackList[slot] = null;
            if (!blockEvents && isSlotActive(slot)) {
                if (current != null && previous != null && current.getItem() == previous.getItem()) {
                    notifyStackChanged(slot, current, previous);
                } else {
                    if (previous != null) notifyUnequipped(previous, getSlotContext(slot, false), current);
                    if (current != null) notifyEquipped(current, getSlotContext(slot, false), previous);
                }
            }
            syncSlotToClients(slot);
        }
    }

    private void notifyStackChanged(int slot, ItemStack current, ItemStack previous) {
        callbackDepth++;
        try {
            BaubleLifecycle.changed(current, getSlotContext(slot, false), ItemStack.copyItemStack(previous));
        } catch (Exception e) {
            Baubles.log.error("Error updating bauble state {}", current, e);
        } finally {
            callbackDepth--;
        }
    }

    public void onPlayerLoaded() {
        for (int slot = 0; slot < stackList.length; slot++) {
            if (stackList[slot] == null || !isSlotActive(slot)) continue;
            callbackDepth++;
            try {
                BaubleLifecycle.loaded(stackList[slot], getSlotContext(slot, false));
            } catch (Exception e) {
                Baubles.log.error("Error loading bauble {}", stackList[slot], e);
            } finally {
                callbackDepth--;
            }
        }
        resetStackSnapshots();
    }

    public void applyStackSync(int slot, ItemStack stack) {
        if (player.get() == null || !player.get().worldObj.isRemote) return;
        ItemStack previous = getStackInSlot(slot);
        if (previous != null && stack != null && previous.getItem() == stack.getItem()) {
            stackList[slot] = stack;
            if (!blockEvents && isSlotActive(slot) && !ItemStack.areItemStacksEqual(previous, stack)) notifyStackChanged(slot, stack, previous);
        } else setInventorySlotContents(slot, stack);
    }

    public void onSlotContentsChanged(int slot) {
        ItemStack stack = getStackInSlot(slot);
        EntityPlayer owner = player.get();
        if (!blockEvents && isSlotActive(slot) && owner != null && stack != null) {
            callbackDepth++;
            try {
                if (stack.getItem() instanceof IBaubleLifecycle item) item.onBaubleContentsChanged(stack, getSlotContext(slot, false));
                else if (stack.getItem() instanceof IBaubleExpanded item) item.onSlotContentsChanged(stack, owner);
            } catch (Exception e) {
                Baubles.log.error("Error updating bauble {}", stack, e);
            } finally {
                callbackDepth--;
            }
        }
        syncSlotToClients(slot);
    }

    public void applySnapshot(BaubleSlotState state, long revision, ItemStack[] stacks, boolean oldGui) {
        EntityPlayer owner = player.get();
        if (owner == null || !owner.worldObj.isRemote) return;
        if (loadedPlayer.get() == owner && revision < layoutRevision) return;
        if (stacks.length != state.getAssignments().length) {
            throw new IllegalArgumentException("Snapshot items and layout have different sizes");
        }
        ItemStack[] previous = stackList;
        BaubleSlotState previousState = slotState;
        BaubleSlotContext[] previousContexts = new BaubleSlotContext[previous.length];
        for (int slot = 0; slot < previous.length; slot++) previousContexts[slot] = context(previousState, slot, false);
        boolean loadAll = loadedPlayer.get() != owner;
        loadedPlayer = new WeakReference<>(owner);
        slotState = state;
        stackList = stacks.clone();
        layoutRevision = revision;
        oldGuiRendering = oldGui;
        for (int slot = 0; slot < Math.max(previous.length, stacks.length); slot++) {
            ItemStack before = slot < previous.length ? previous[slot] : null;
            ItemStack after = getStackInSlot(slot);
            boolean wasActive = previousState.isActive(slot);
            boolean active = isSlotActive(slot);
            if (!loadAll && wasActive == active && ItemStack.areItemStacksEqual(before, after)) continue;
            if (!loadAll && wasActive && active && before != null && after != null && before.getItem() == after.getItem()) {
                notifyStackChanged(slot, after, before);
                continue;
            }
            if (before != null && wasActive && !loadAll) notifyUnequipped(before, previousContexts[slot], after);
            if (after != null && active) {
                if (!loadAll && !wasActive) {
                    notifyEquipped(after, getSlotContext(slot, false), before);
                    continue;
                }
                callbackDepth++;
                try {
                    BaubleLifecycle.loaded(after, getSlotContext(slot, false));
                } catch (Exception e) {
                    Baubles.log.error("Error loading bauble {}", after, e);
                } finally {
                    callbackDepth--;
                }
            }
        }
        resetStackSnapshots();
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
            if (!blockEvents && isSlotActive(slot)) notifyUnequipped(itemstack, getSlotContext(slot, false), stackList[slot]);

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
        if (!blockEvents && changed && isSlotActive(slot)) {
            EntityPlayer owner = player.get();
            if (owner != null && owner.worldObj.isRemote && previous != null && stack != null
                    && previous.getItem() == stack.getItem()) {
                // Vanilla container packets use the inventory setter for synchronized item data.
                notifyStackChanged(slot, stack, previous);
            } else {
                if (previous != null) notifyUnequipped(previous, getSlotContext(slot, false), stack);
                if (stack != null) notifyEquipped(stack, getSlotContext(slot, false), previous);
            }
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
        return stack != null && isSlotActive(slot) && matchesSlotType(slot, stack)
                && BaubleLifecycle.canEquip(stack, getSlotContext(slot, false));
    }

    public boolean matchesSlotType(int slot, ItemStack stack) {
        String slotType = getSlotType(slot);
        if (stack == null || !isValidSlot(slot)) {
            return false;
        }

        Item item = stack.getItem();
        if(!(item instanceof IBauble)) {
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
            if (canTypeFitSlot(type, slotType)) {
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
        BaubleSlotStorage.write(tags, new BaubleSlotState(slotState.getAssignments(), getPendingSlotState().getModifiers(),
                slotState.getDefinitions(), getPendingSlotState().getInactiveSlots()));
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
        slotState = saved.rebase(BaubleExpandedSlots.getCurrentSlotAssignments(),
                BaubleSlotConfiguration.getDefinitions());
        stackList = new ItemStack[slotState.getAssignments().length];
        pendingReturns.clear();
        NBTTagList tagList = tags.getTagList("Baubles.Inventory", 10);
        for (int i = 0; i < tagList.tagCount(); ++i) {
            NBTTagCompound nbttagcompound = tagList.getCompoundTagAt(i);
            int slot = nbttagcompound.hasKey("SlotIndex", 3)
                    ? nbttagcompound.getInteger("SlotIndex") : nbttagcompound.getByte("Slot") & 255;
            ItemStack itemstack = ItemStack.loadItemStackFromNBT(nbttagcompound);
            if (itemstack != null) {
                if (slot >= 0 && slot < stackList.length && stackList[slot] == null
                        && !BaubleExpandedSlots.unknownType.equals(getSlotType(slot))
                        && saved.getType(slot).equals(getSlotType(slot)) && matchesSlotType(slot, itemstack)) {
                    stackList[slot] = itemstack;
                    int limit = getSlotDefinition(getSlotType(slot)).stackLimit();
                    if (itemstack.stackSize > limit) pendingReturns.add(itemstack.splitStack(itemstack.stackSize - limit));
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
        resetStackSnapshots();
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
        if (previousStacks.length != stackList.length) previousStacks = Arrays.copyOf(previousStacks, stackList.length);
        if (slot >= 0 && slot < previousStacks.length) previousStacks[slot] = ItemStack.copyItemStack(stackList[slot]);
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
