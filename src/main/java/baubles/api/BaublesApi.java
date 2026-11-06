package baubles.api;

import baubles.api.expanded.IBaubleSlotInventory;
import baubles.api.expanded.IBaubleRenderInventory;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

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
