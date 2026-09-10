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
	 * Called whenever the contents of the slot holding this item changed, including on equip alongside onEquipped.
	 * The case no other hook covers is a stack that changes size without leaving the slot, which happens when a
	 * player merges items into an occupied slot or takes part of a stack out of one, so anything derived from the
	 * stack size has to be recalculated here. Compare against your own state, this does not tell you what changed.
	 */
	default void onSlotContentsChanged (ItemStack itemstack, EntityLivingBase player) {}

}
