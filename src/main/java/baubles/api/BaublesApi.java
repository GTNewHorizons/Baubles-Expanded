package baubles.api;

import baubles.api.expanded.IBaubleSlotInventory;
import baubles.api.expanded.IBaubleRenderInventory;
import baubles.api.expanded.IBaubleCosmeticInventory;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

/**
 * @author Azanor
 */
public class BaublesApi
{
	static Method getBaubles;

    /**
     * Returns dynamic slot support from the player's current baubles inventory.
     */
    public static IBaubleSlotInventory getBaubleSlots(EntityPlayer player) {
        IInventory inventory = getBaubles(player);
        return inventory instanceof IBaubleSlotInventory ? (IBaubleSlotInventory) inventory : null;
    }

    /**
     * Returns optional model visibility support from the player's current inventory.
     */
    public static IBaubleRenderInventory getBaubleRenders(EntityPlayer player) {
        IInventory inventory = getBaubles(player);
        return inventory instanceof IBaubleRenderInventory ? (IBaubleRenderInventory) inventory : null;
    }

    /**
     * Checks model visibility using an already resolved inventory. Legacy inventories remain visible.
     */
    public static boolean isBaubleRendered(IInventory inventory, int slot) {
        return !(inventory instanceof IBaubleRenderInventory renders) || renders.isSlotRendered(slot);
    }

    /**
     * Returns optional cosmetic storage without replacing the functional inventory.
     */
    public static IInventory getBaubleCosmetics(EntityPlayer player) {
        IInventory inventory = getBaubles(player);
        return inventory instanceof IBaubleCosmeticInventory cosmetics ? cosmetics.getCosmeticInventory() : null;
    }

    /**
     * Selects the visible model: a cosmetic replacement, or the functional item when none is stored.
     * The visibility toggle also hides cosmetic replacements. This is only a rendering query.
     */
    public static ItemStack getRenderedBauble(IInventory inventory, int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.getSizeInventory() || !isBaubleRendered(inventory, slot)) return null;
        if (inventory instanceof IBaubleCosmeticInventory cosmetics) {
            ItemStack replacement = cosmetics.getCosmeticInventory().getStackInSlot(slot);
            if (replacement != null) return replacement;
        }
        return inventory.getStackInSlot(slot);
    }

	/**
	 * Retrieves the baubles inventory for the supplied player
	 */
	public static IInventory getBaubles(EntityPlayer player)
	{
		IInventory ot = null;

	    try
	    {
	        if(getBaubles == null)
	        {
	            Class<?> fake = Class.forName("baubles.common.lib.PlayerHandler");
	            getBaubles = fake.getMethod("getPlayerBaubles", EntityPlayer.class);
	        }

	        ot = (IInventory) getBaubles.invoke(null, player);
	    }
	    catch(Exception ex)
	    {
	    	FMLLog.warning("[Baubles API] Could not invoke baubles.common.lib.PlayerHandler method getPlayerBaubles");
	    }

		return ot;
	}

}
