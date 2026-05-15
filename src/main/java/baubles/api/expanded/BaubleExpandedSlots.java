package baubles.api.expanded;

import java.util.ArrayList;
import java.util.Arrays;

import baubles.api.BaubleType;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.network.ByteBufUtils;

public class BaubleExpandedSlots {

	//The total number of slots that can be added.
	//Please keep in mind that increases to this limit are possible with minimal changes.
	public static final int slotLimit = 20;
	public static final int maxSlotIdBytes = ByteBufUtils.varIntByteCount(slotLimit);
    //The type used in place of null.
    public static final String invalidType = "";

    //The type used for unassigned slots.
    public static final String unknownType = "unknown";

    //Pre-registered types.
    public static final String ringType = "ring";
    public static final String amuletType = "amulet";
    public static final String beltType = "belt";
    public static final String universalType = "universal";
    public static final String headType = "head";
    public static final String bodyType = "body";
    public static final String charmType = "charm";
    public static final String capeType = "cape";
    public static final String shieldType = "shield";
    public static final String quiverType = "quiver";
    public static final String gauntletType = "gauntlet";
    public static final String earringType = "earring";
    public static final String wingsType = "wings";

    /**
     * Registers a type and returns true if the type could be or has been registered.
     * Types can only be registered while the loader state is pre-initialization.
     *
     * @param type The type to register.
     * @return If a type was registered or had been registered previously.
     */
    public static boolean tryRegisterType(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        if (isTypeRegistered(type)) {
            return true;
        }
        if (Loader.instance().getLoaderState() == LoaderState.PREINITIALIZATION) {
            registeredTypes.add(type);
            return true;
        }
        return false;
    }

    /**
     * Returns false if the type is unregistered or unknown, the minimum number of
     * assigned slots cannot be met, or the loader state is not pre-initialization.
     *
     * @param type The type of the slot to evaluate and possibly assign.
     * @param minimumOfType The minimum slots of type to be assigned.
     *
     * @return If the total assigned slots of the specified type equals or is more than the minimum.
     */
    public static boolean tryAssignSlotsUpToMinimum(String type, int minimumOfType) {
        if (minimumOfType < 1 || !isTypeRegistered(type) || type.equals(unknownType) || Loader.instance().getLoaderState() != LoaderState.PREINITIALIZATION) {
            return false;
        }
        int total = 0;
        for (String assignedSlot : assignedSlots) {
            if (assignedSlot.equals(type)) {
                total++;
            }
        }
        if (total >= minimumOfType) {
            return true;
        }
        total = minimumOfType - total;
        for (int i = 0; i < total; i++) {
            tryAssignSlotOfType(type);
        }
        return true;
    }

    /**
     * Returns false if the type is unregistered or unknown, or the
     * loader state is not pre-initialization.
     *
     * @param type The type of the slot to evaluate and possibly unassign.
     * @param maximumOfType The maximum slots of type to be left assigned.
     *
     * @return If the total assigned slots of the specified type equals or is less than the maximum.
     */
    public static boolean tryUnassignSlotsDownToMaximum(String type, int maximumOfType) {
        if (maximumOfType < 0) maximumOfType = 0;
        if (!isTypeRegistered(type) || type.equals(unknownType) || Loader.instance().getLoaderState() != LoaderState.PREINITIALIZATION) {
            return false;
        }
        int total = 0;
        for (String assignedSlot : assignedSlots) {
            if (assignedSlot.equals(type)) {
                total++;
            }
        }
        if (total <= maximumOfType) {
            return true;
        }
        total -= maximumOfType;
        for (int i = 0; i < total; i++) {
            tryUnassignSlotOfType(type);
        }
        return true;
    }

    /**
     * Returns if a slot was assigned successfully.
     * Does not assign a type to a slot if the type is unregistered, no slots
     * are free, or the loader state is not pre-initialization.
     *
     * @param type The type to attempt assigning to a slot.
     *
     * @return If assigning a type to a slot was successful or not.
     */
    public static boolean tryAssignSlotOfType(String type) {
        if (!isTypeRegistered(type) || type.equals(unknownType) || Loader.instance().getLoaderState() != LoaderState.PREINITIALIZATION) {
            return false;
        }
        assignedSlots.add(type);
        return true;
    }

    /**
     * Unassigns the last slot of the specified type if one can be found
     * and the loader state is pre-initialization.
     *
     * @param type The type of the slot to be unassigned.
     *
     * @return If unassigning a slot was successful or not.
     */
    public static boolean tryUnassignSlotOfType(String type) {
        if (assignedSlots.isEmpty() || type == null || type.equals(unknownType) || Loader.instance().getLoaderState() != LoaderState.PREINITIALIZATION) {
            return false;
        }
        for (int i = assignedSlots.size() - 1; i >= 0; i--) {
            if (type.equals(assignedSlots.get(i))) {
                assignedSlots.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the current number of slots with a matching type.
     *
     * @param type The type of slot being counted.
     *
     * @return The current total slots with a matching type assigned.
     */
    public static int totalCurrentlyAssignedSlotsOfType(String type) {
        if (!isTypeRegistered(type)) {
            return 0;
        }
        int total = 0;
        for (String assignedSlot : assignedSlots) {
            if (assignedSlot.equals(type)) {
                total++;
            }
        }
        return total;
    }

    /**
     * Returns an array of slot indexes that have the specified type assigned.
     * Do not treat results from this as final until postinit or later.
     *
     * @param type The type of slot being checked.
     *
     * @return an array of slot indexes with the specified type.
     */
    public static int[] getIndexesOfAssignedSlotsOfType(String type) {
        int[] slotIndexes = new int[0];
        if (!isTypeRegistered(type)) {
            return slotIndexes;
        }
        for (int slotToCheck = 0; slotToCheck < assignedSlots.size(); slotToCheck++) {
            if (assignedSlots.get(slotToCheck).equals(type)) {
                slotIndexes = Arrays.copyOf(slotIndexes, slotIndexes.length + 1);
                slotIndexes[slotIndexes.length - 1] = slotToCheck;
            }
        }
        return slotIndexes;
    }

    /**
     * Returns if the type is registered or not.
     *
     * @param type The type to check the registration of.
     *
     * @return If the type is registered or not.
     */
    public static boolean isTypeRegistered(String type) {
        return registeredTypes.contains(type);
    }

    /**
     * Returns the number of bauble slots that are currently used.
     *
     * @return The number of bauble slots currently used.
     */
    public static int slotsCurrentlyUsed() {
        return assignedSlots.size();
    }

    /**
     * We now have infinite slots, so this method has been deprecated.
     *
     * @return The number of bauble slots currently unused.
     */
    @Deprecated
    public static int slotsCurrentlyUnused() {
        return 0;
    }

    /**
     * Returns the type of the specified slot, "unknown" if out of range.
     *
     * @param slot The slot to check
     * @return The type of the specified slot or unknown.
     */
    public static String getSlotType(int slot) {
        if (slot >= 0 && slot < assignedSlots.size()) {
            return assignedSlots.get(slot);
        } else {
            return unknownType;
        }
    }

    /**
     * Returns the index of a certain type in the registeredType array
     * or -1 if it cannot be found.
     *
     * @param type The type to get the index of
     * @return Index of a type or negative one
     */
    public static int getIndexOfTypeInRegisteredTypes(String type) {
        return registeredTypes.indexOf(type);
    }

    /**
     * Returns the currently registered bauble types.
     * If pre-initialization is done this list is effectively final.
     *
     * @return The currently registered bauble types.
     */
    public static ArrayList<String> getCurrentlyRegisteredTypes() {
        return registeredTypes;
    }

    /**
     * Returns the current slot assignments as a string array.
     * If initialization is done this list is effectively final.
     *
     * @return The current contents of the slots array.
     */
    public static String[] getCurrentSlotAssignments() {
        return assignedSlots.toArray(new String[]{});
    }

    /**
     * Returns a type based on the specified BaubleType.
     *
     * @param type The BaubleType to get a matching type from.
     *
     * @return The type matching the BaubleType or unknown.
     */
    public static String getTypeFromBaubleType(BaubleType type) {
        if (type == null) {
            return invalidType;
        }
        return switch (type) {
            case RING -> ringType;
            case AMULET -> amuletType;
            case BELT -> beltType;
            case UNIVERSAL -> universalType;
        };
    }

    /**
     * This is only intended to be used by the Baubles manualSlotSelection config option, if it is enabled.
     * Overrides assigned slots with the contents of a string array.
     * Any entry that would go over slotLimit is ignored.
     * If an array is shorter than slotLimit all remaining slots are set to unknown.
     * Invalid slot types are set to unknown.
     *
     * @param overrideSlots The array to override assigned slots with.
     */
    public static void overrideSlots(String[] overrideSlots) {
        if (Loader.instance().getLoaderState() != LoaderState.INITIALIZATION) {
            return;
        }
        assignedSlots.clear();
        for (String overrideSlot : overrideSlots) {
            if (isTypeRegistered(overrideSlot) && !overrideSlot.equals(unknownType)) {
                assignedSlots.add(overrideSlot);
            } else {
                assignedSlots.add(unknownType);
            }
        }
    }

    private static final ArrayList<String> assignedSlots = new ArrayList<>();
    private static final ArrayList<String> registeredTypes = new ArrayList<>();
    static {
        registeredTypes.add(unknownType);
        registeredTypes.add(ringType);
        registeredTypes.add(amuletType);
        registeredTypes.add(beltType);
        registeredTypes.add(universalType);
        registeredTypes.add(headType);
        registeredTypes.add(bodyType);
        registeredTypes.add(charmType);
        registeredTypes.add(capeType);
        registeredTypes.add(shieldType);
        registeredTypes.add(quiverType);
        registeredTypes.add(gauntletType);
        registeredTypes.add(earringType);
        registeredTypes.add(wingsType);

        assignedSlots.add(amuletType);
        assignedSlots.add(ringType);
        assignedSlots.add(ringType);
        assignedSlots.add(beltType);
    }

}
