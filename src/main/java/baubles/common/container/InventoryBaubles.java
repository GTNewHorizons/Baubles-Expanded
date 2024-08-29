package baubles.common.container;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import baubles.common.Baubles;
import baubles.common.lib.ItemStackHelper;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketSyncBauble;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;

public class InventoryBaubles implements IInventory {
	public ItemStack[] stackList;
	private Container eventHandler;
	public WeakReference<EntityPlayer> player;
	public boolean blockEvents=false;

	public InventoryBaubles(EntityPlayer player) {
		stackList = new ItemStack[BaubleExpandedSlots.slotLimit];
		this.player = new WeakReference<>(player);
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
		return slot >= getSizeInventory() ? null : stackList[slot];
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
		if(stackList[slot] != null) {
			ItemStack itemstack = this.stackList[slot];
			stackList[slot] = null;
			return itemstack;
		} else {
			return null;
		}
	}

	/**
	 * Removes from an inventory slot (first arg) up to a specified number
	 * (second arg) of items and returns them in a new stack.
	 */
	@Override
	public ItemStack decrStackSize(int slot, int decrementBy) {
		if(stackList[slot] != null) {
			ItemStack itemstack;

			if(stackList[slot].stackSize <= decrementBy) {
				itemstack = stackList[slot];

				if(itemstack != null && itemstack.getItem() instanceof IBauble) {
					((IBauble) itemstack.getItem()).onUnequipped(itemstack, player.get());
				}

				stackList[slot] = null;
			} else {
				itemstack = stackList[slot].splitStack(decrementBy);

				if(itemstack != null && itemstack.getItem() instanceof IBauble) {
					((IBauble)itemstack.getItem()).onUnequipped(itemstack, player.get());
				}

				if(stackList[slot].stackSize == 0) {
					stackList[slot] = null;
				}
			}

			if(eventHandler != null)
				eventHandler.onCraftMatrixChanged(this);
			syncSlotToClients(slot);
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
		if(!blockEvents && stackList[slot] != null) {
        	((IBauble)stackList[slot].getItem()).onUnequipped(stackList[slot], player.get());
		}
		stackList[slot] = stack;
		if(!blockEvents && stack != null && stack.getItem() instanceof IBauble) {
			if(player.get()!=null) {
				((IBauble) stack.getItem()).onEquipped(stack, player.get());
			}
		}
		if(eventHandler != null) {
			eventHandler.onCraftMatrixChanged(this);
		}
		syncSlotToClients(slot);
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
		String slotType = BaubleExpandedSlots.getSlotType(slot);
		if (stack == null || slotType == null) {
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

		for(String type : types) {
			if(type.equals(BaubleExpandedSlots.universalType) || type.equals(slotType)) {
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
		NBTTagList tagList = new NBTTagList();
		NBTTagCompound invSlot;
		for (int slot = 0; slot < stackList.length; ++slot) {
			if (stackList[slot] != null) {
				invSlot = new NBTTagCompound();
				invSlot.setByte("Slot", (byte) slot);
				stackList[slot].writeToNBT(invSlot);
				tagList.appendTag(invSlot);
			}
		}
		tags.setTag("Baubles.Inventory", tagList);
	}

	public void readNBT(EntityPlayer player) {
		NBTTagCompound tags = player.getEntityData();
		readNBT(tags);
	}

	public void readNBT(NBTTagCompound tags) {
		NBTTagList tagList = tags.getTagList("Baubles.Inventory", 10);
		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound nbttagcompound = (NBTTagCompound) tagList.getCompoundTagAt(i);
			int slot = nbttagcompound.getByte("Slot") & 255;
			ItemStack itemstack = ItemStack.loadItemStackFromNBT(nbttagcompound);
			if (itemstack != null) {
				stackList[slot] = itemstack;
			}
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
		try {
			if (Baubles.proxy.getClientWorld() == null) {
				PacketHandler.INSTANCE.sendToAll(new PacketSyncBauble(player.get(), slot));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
