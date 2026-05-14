package baubles.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import baubles.api.expanded.BaubleExpandedSlots;
import net.minecraftforge.common.config.Configuration;

public class BaublesConfig {

	public static boolean hideDebugItem = true;

    public static int[] soulBoundEnchantments = new int[] {};
    public static int maxColumns = 4;

    public static boolean useOldGuiButton = false;
    public static boolean useOldGuiRendering = false;
    public static boolean showUnusedSlots = false;
    public static boolean manualSlotSelection = false;
    public static boolean displayTooltipOnHover = true;
    public static String[] slotStackLimitOverrides = new String[] {};
    private static final Map<String, Integer> stackLimitsByType = new HashMap<>();

    public static String[] overrideSlotTypes = new String[] {
        BaubleExpandedSlots.amuletType,
        BaubleExpandedSlots.ringType,
        BaubleExpandedSlots.ringType,
        BaubleExpandedSlots.beltType
    };

    static final String categoryDebug = "debug";
    static final String categoryGeneral = "general";
    static final String categoryMenu = "menu";
    static final String categoryClient = "client";
    static final String categoryOverride = "override";

    public static void loadConfig(Configuration config) {

        ArrayList<String> currentlyRegisteredTypes = BaubleExpandedSlots.getCurrentlyRegisteredTypes();
        String[] currentSlotAssignments = BaubleExpandedSlots.getCurrentSlotAssignments();

        //categoryDebug
        hideDebugItem = config.getBoolean("hideDebugItem", categoryDebug, hideDebugItem, "Hides the Bauble debug item from the creative menu.\n");

        //categoryGeneral
        soulBoundEnchantments = config.get(categoryGeneral, "soulBoundEnchantments", soulBoundEnchantments,
            "IDs of enchantments that should be treated as soul bound when on items in a bauble slot."
        ).getIntList();

        //categoryClient
        useOldGuiButton = config.getBoolean("useOldGuiButton", categoryClient, useOldGuiButton, "Use the old Baubles Button texture and location instead.\n");
        useOldGuiRendering = config.getBoolean("useOldRendering", categoryClient, useOldGuiRendering, "Display the old Bauble GUI instead of the new sidebar.\nUsing old rendering with more than 20 slots works, but results in visual oddities and is not supported.\n");
        maxColumns = config.getInt("maxColumns", categoryClient, 4, 1, Integer.MAX_VALUE, "Maximum number of columns shown in the baubles inventory.\n"
        );

        //categoryMenu
        showUnusedSlots = config.getBoolean("showUnusedSlots", categoryMenu, showUnusedSlots, "Display unused Bauble slots.\n");
        manualSlotSelection = config.getBoolean("manualSlotSelection", categoryMenu, manualSlotSelection,
            "Manually override slot assignments.\n!Bauble slot types must be configured manually with this option enabled!\n"
        );
        displayTooltipOnHover = config.getBoolean("displayTooltipOnHover", categoryMenu, displayTooltipOnHover,
            "When hovering the mouse over a bauble slot, display a tooltip with the bauble type and if a held item can be equipped in that slot.\n"
        );

        //categoryOverride
        config.getStringList("defualtSlotTypes", categoryOverride, new String[] {},
            "Baubles and its addons assigned the folowing types to the bauble slots.\n!This config option automatically changes to reflect what Baubles and its addons assigned each time the game is launched!"
        );
        config.getCategory(categoryOverride).get("defualtSlotTypes").set(currentSlotAssignments);

        overrideSlotTypes = config.getStringList("slotTypeOverrides", categoryOverride, overrideSlotTypes,
            "Slot assignments to use if manualSlotSelection is enabled.\n!Adding, moving, or removing slots of the "
                + BaubleExpandedSlots.amuletType + ", " + BaubleExpandedSlots.ringType + ", or " + BaubleExpandedSlots.beltType +
                " types will reduce compatibility with mods made for original Baubles versions!\n",
            currentlyRegisteredTypes.toArray(new String[0])
        );
        slotStackLimitOverrides = config.getStringList("slotStackLimitOverrides", categoryOverride, slotStackLimitOverrides,
            "Per-slot-type stack limits in the format \"type=limit\" (example: heartcanister_red=10).\n"
                + "Unspecified types default to a stack limit of 1.\n",
            currentlyRegisteredTypes.stream().map(type -> type + "=1").toArray(String[]::new)
        );

        stackLimitsByType.clear();
        for (String entry : slotStackLimitOverrides) {
            if (entry == null) continue;
            String[] split = entry.split("=", 2);
            if (split.length != 2) {
                Baubles.log.warn("Ignoring malformed slotStackLimitOverrides entry '{}'. Expected format: type=limit", entry);
                continue;
            }
            String type = split[0].trim();
            String limitText = split[1].trim();
            if (!BaubleExpandedSlots.isTypeRegistered(type)) continue;
            try {
                int limit = Integer.parseInt(limitText);
                if (limit > 0) {
                    stackLimitsByType.put(type, limit);
                }
            } catch (NumberFormatException ignored) {}
        }

        if(manualSlotSelection) {
            BaubleExpandedSlots.overrideSlots(overrideSlotTypes);
        }

        if(config.hasChanged()) {
            config.save();
        }
    }

    public static int getStackLimitForSlotType(String type) {
        if (type == null) return 1;
        return stackLimitsByType.getOrDefault(type, 1);
    }

}
