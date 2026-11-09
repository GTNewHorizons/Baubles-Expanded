package baubles.api;

import baubles.api.expanded.IBaubleSlotInventory;
import baubles.api.expanded.IBaubleSlotSettings;
import baubles.api.expanded.BaubleSlotResult;
import baubles.api.expanded.BaubleSlotContext;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author Azanor
 */
public class BaublesApi
{
	static Method getBaubles;

    /**
     * Returns optional type policies, activation and contextual slot access.
     */
    public static IBaubleSlotSettings getBaubleSlotSettings(EntityPlayer player) {
        IInventory inventory = getBaubles(player);
        return inventory instanceof IBaubleSlotSettings settings ? settings : null;
    }

    /**
     * Finds active functional items. Results reference the inventory's real stacks.
     */
    public static List<BaubleSlotResult> findBaubles(EntityPlayer player, Predicate<ItemStack> filter) {
        List<BaubleSlotResult> results = new ArrayList<>();
        IInventory inventory = getBaubles(player);
        if (inventory == null) return results;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            BaubleSlotResult result = matchingBauble(player, inventory, slot, filter);
            if (result != null) results.add(result);
        }
        return results;
    }

    public static Optional<BaubleSlotResult> findFirstBauble(EntityPlayer player, Predicate<ItemStack> filter) {
        IInventory inventory = getBaubles(player);
        if (inventory != null) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                BaubleSlotResult result = matchingBauble(player, inventory, slot, filter);
                if (result != null) return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    private static BaubleSlotResult matchingBauble(EntityPlayer player, IInventory inventory, int slot, Predicate<ItemStack> filter) {
        if (inventory instanceof IBaubleSlotSettings settings && !settings.isSlotActive(slot)) return null;
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack == null || !filter.test(stack)) return null;
        BaubleSlotContext context = inventory instanceof IBaubleSlotSettings settings ? settings.getSlotContext(slot, false)
                : new BaubleSlotContext(player, "unknown", slot, slot, false, true, true);
        return new BaubleSlotResult(context, stack);
    }

    /**
     * Returns dynamic slot support from the player's current baubles inventory.
     */
    public static IBaubleSlotInventory getBaubleSlots(EntityPlayer player) {
        IInventory inventory = getBaubles(player);
        return inventory instanceof IBaubleSlotInventory ? (IBaubleSlotInventory) inventory : null;
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
