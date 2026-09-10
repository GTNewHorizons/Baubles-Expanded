package baubles.api.expanded;

import baubles.api.IBauble;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/**
 * This interface should be extended by items that can be worn in bauble slots.
 * Supports all original and several expanded slot types.
 *
 * @author jss2a98aj
 */
public interface IBaubleExpanded extends IBauble {

	/**
	 * This method returns the types of bauble slots this item can go into.
	 * getBaubleType is generally ignored when using IBaubleExpanded, so it can return null unless you want backwards compatibility with official bauble builds.
	 * Pre-registered slot types can be found in BaubleExpandedSlots, but it is possible to add more during pre-initialization.
	 */
	String[] getBaubleTypes (ItemStack itemstack);

	/**
	 * Called whenever the slot holding this item changes. Triggers when an item is equipped or when the player
	 * interacts with the slot changing its stack size, but not when the whole stack is unequipped.
	 * Interactions with InventoryBaubles from code never trigger this.
	 */
	default void onSlotContentsChanged (ItemStack itemstack, EntityLivingBase player) {}

}
