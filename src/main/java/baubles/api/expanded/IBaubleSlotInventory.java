package baubles.api.expanded;

import java.util.Map;

import net.minecraft.inventory.IInventory;

/**
 * Optional dynamic slot support. Obtain it through BaublesApi.getBaubleSlots(player).
 * Mutations must run on the server game thread. Modifiers persist with the player.
 * Use a unique source such as "examplemod:equipment" to keep contributions independent.
 * Requests made inside equipment callbacks are applied on the next player tick, after item movement completes.
 */
public interface IBaubleSlotInventory extends IInventory {

    String getSlotType(int slot);

    /**
     * Returns a copy of the effective layout, including inactive reserved indices.
     */
    String[] getSlotAssignments();

    int getSlotCount(String type);

    /**
     * Returns an immutable snapshot of source identifiers and their additive amounts.
     */
    Map<String, Integer> getSlotModifiers(String type);

    /**
     * Replaces one source's contribution; zero removes that contribution.
     */
    void setSlotModifier(String type, String source, int amount);

    /**
     * Adjusts one source so that the effective count equals amount at the time of the call.
     */
    void setSlotCount(String type, String source, int amount);

    /**
     * Removes only the named source, across all types.
     */
    void resetSlotModifiers(String source);
}
