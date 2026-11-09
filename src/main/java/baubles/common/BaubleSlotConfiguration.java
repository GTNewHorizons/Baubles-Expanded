package baubles.common;

import com.github.bsideup.jabel.Desugar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.BaubleSlotType;
import baubles.api.expanded.BaubleSlotTypes;
import baubles.common.container.BaubleSlotState;
import baubles.common.container.InventoryBaubles;
import baubles.common.lib.PlayerHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;

/**
 * Parses and preflights complete slot configurations before publishing them to online players.
 * Player modifiers and activation remain independent of the configured baseline.
 */
public class BaubleSlotConfiguration {

    private static Map<String, BaubleSlotType> definitions = Collections.emptyMap();
    private static String[] baseline;
    private static String[] publishedLayout;
    private static File configFile;

    public static Map<String, BaubleSlotType> getDefinitions() {
        return definitions.isEmpty() ? BaubleSlotTypes.getDefaults() : definitions;
    }

    public static void initialize(Configuration config) {
        configFile = config.getConfigFile();
        if (baseline == null) baseline = BaubleExpandedSlots.getCurrentSlotAssignments();
        publish(parse(config));
    }

    public static void reload() {
        if (configFile == null) throw new IllegalStateException("Slot configuration has not been initialized");
        Configuration config = new Configuration(configFile);
        config.load();
        Snapshot snapshot = parse(config);
        Map<InventoryBaubles, BaubleSlotState> updates = new LinkedHashMap<>();
        for (Object object : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) object;
            InventoryBaubles inventory = PlayerHandler.getPlayerBaubles(player);
            updates.put(inventory, inventory.prepareConfiguration(snapshot.assignments(), snapshot.definitions()));
        }
        publish(snapshot);
        for (Map.Entry<InventoryBaubles, BaubleSlotState> update : updates.entrySet()) {
            update.getKey().applyConfiguration(update.getValue());
        }
    }

    /**
     * Retains initialization-time layout overrides made by downstream mods after Baubles initialized.
     */
    public static void finishInitialization() {
        String[] finalLayout = BaubleExpandedSlots.getCurrentSlotAssignments();
        if (!Arrays.equals(finalLayout, publishedLayout)) baseline = finalLayout;
    }

    private static Snapshot parse(Configuration config) {
        Map<String, BaubleSlotType> types = new LinkedHashMap<>(BaubleSlotTypes.getDefaults());
        boolean manual = config.get("menu", "manualSlotSelection", false).getBoolean(false);
        String[] override = config.get("override", "slotTypeOverrides", baseline).getStringList();
        List<String> layout = new ArrayList<>(Arrays.asList(manual ? override : baseline));
        for (String id : layout) {
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Empty slot type in layout");
            types.putIfAbsent(id, BaubleSlotType.defaults(id));
        }
        String[] stackOverrides = config.get("override", "slotStackLimitOverrides", new String[0]).getStringList();
        String[] universalOverrides = config.get("override", "slotUniversalOverrides", new String[0]).getStringList();
        Map<String, Integer> limits = new HashMap<>();
        for (String entry : stackOverrides) {
            String[] pair = entry.split("=", 2);
            try {
                if (pair.length != 2 || pair[0].trim().isEmpty()) throw new NumberFormatException();
                int limit = Integer.parseInt(pair[1].trim());
                if (limit < 1) throw new NumberFormatException();
                limits.put(pair[0].trim(), limit);
            } catch (NumberFormatException ignored) {
                // Preserve the legacy handling of malformed override entries.
                Baubles.log.warn("Ignoring invalid slotStackLimitOverrides entry '{}'", entry);
            }
        }
        Set<String> universal = new HashSet<>();
        for (String entry : universalOverrides) {
            String type = entry.trim();
            if (!type.isEmpty() && !BaubleExpandedSlots.unknownType.equals(type)
                    && !BaubleExpandedSlots.universalType.equals(type)) universal.add(type);
        }
        for (Map.Entry<String, BaubleSlotType> entry : types.entrySet()) {
            BaubleSlotType type = entry.getValue();
            entry.setValue(new BaubleSlotType(type.id(), type.order(), type.icon(), type.cosmetic(), type.renderToggle(),
                    type.nativeGui(), type.enabled(), limits.getOrDefault(type.id(), type.stackLimit()),
                    type.fitsUniversal() || universal.contains(type.id())));
        }
        String[] rows = config.getStringList("slotDefinitions", "slots", new String[0],
                "Type policies, one row per type: head;add_cosmetic=false;render_toggle=false\n"
                        + "Optional keys: order, icon, use_native_gui, enabled, size, stack_limit, fits_universal.\n"
                        + "Unspecified policies preserve existing behavior. size changes the configured baseline only.\n"
                        + "Icons are standalone textures such as example:textures/gui/head.png.\n"
                        + "Use /baubles reload after editing. Invalid definitions leave the running configuration unchanged.");
        Map<String, Boolean> seen = new HashMap<>();
        for (String row : rows) {
            String[] fields = row.split(";", -1);
            String id = fields[0].trim();
            if (id.isEmpty() || BaubleExpandedSlots.unknownType.equals(id) || seen.put(id, true) != null) {
                throw new IllegalArgumentException("Invalid or duplicate slot definition: " + id);
            }
            Map<String, String> values = new HashMap<>();
            for (int i = 1; i < fields.length; i++) {
                String[] pair = fields[i].split("=", 2);
                if (pair.length != 2 || values.put(pair[0].trim(), pair[1].trim()) != null) {
                    throw new IllegalArgumentException("Invalid slot property: " + fields[i]);
                }
            }
            BaubleSlotType fallback = types.getOrDefault(id, BaubleSlotType.defaults(id));
            BaubleSlotType type = new BaubleSlotType(id, number(values, "order", fallback.order()),
                    string(values, "icon", fallback.icon()), bool(values, "add_cosmetic", fallback.cosmetic()),
                    bool(values, "render_toggle", fallback.renderToggle()), bool(values, "use_native_gui", fallback.nativeGui()),
                    bool(values, "enabled", fallback.enabled()), number(values, "stack_limit", limits.getOrDefault(id, fallback.stackLimit())),
                    bool(values, "fits_universal", fallback.fitsUniversal() || universal.contains(id)));
            types.put(id, type);
            if (values.containsKey("size")) resize(layout, id, number(values, "size", 0));
            if (!values.isEmpty()) throw new IllegalArgumentException("Unknown properties for " + id + ": " + values.keySet());
        }
        if (layout.size() > BaubleSlotState.MAX_SLOTS) throw new IllegalArgumentException("Too many configured slots");
        return new Snapshot(layout.toArray(new String[0]), Collections.unmodifiableMap(types), manual, override,
                stackOverrides, universalOverrides);
    }

    private static void resize(List<String> layout, String id, int size) {
        if (size < 0 || size > BaubleSlotState.MAX_SLOTS) throw new IllegalArgumentException("Invalid slot count: " + size);
        int count = Collections.frequency(layout, id);
        for (int slot = layout.size() - 1; slot >= 0 && count > size; slot--) {
            if (id.equals(layout.get(slot))) {
                layout.set(slot, BaubleExpandedSlots.unknownType);
                count--;
            }
        }
        while (count++ < size) layout.add(id);
    }

    private static String string(Map<String, String> values, String key, String fallback) {
        return values.containsKey(key) ? values.remove(key) : fallback;
    }

    private static int number(Map<String, String> values, String key, int fallback) {
        return values.containsKey(key) ? Integer.parseInt(values.remove(key)) : fallback;
    }

    private static boolean bool(Map<String, String> values, String key, boolean fallback) {
        if (!values.containsKey(key)) return fallback;
        String value = values.remove(key);
        if (!"true".equals(value) && !"false".equals(value)) throw new IllegalArgumentException("Invalid " + key + ": " + value);
        return Boolean.parseBoolean(value);
    }

    private static void publish(Snapshot snapshot) {
        definitions = snapshot.definitions();
        publishedLayout = snapshot.assignments().clone();
        BaublesConfig.manualSlotSelection = snapshot.manual();
        BaublesConfig.overrideSlotTypes = snapshot.override().clone();
        BaublesConfig.slotStackLimitOverrides = snapshot.stackOverrides().clone();
        BaublesConfig.slotUniversalOverrides = snapshot.universalOverrides().clone();
        BaubleExpandedSlots.applyConfiguration(snapshot.assignments(), definitions.keySet());
    }

    @Desugar
    private record Snapshot(String[] assignments, Map<String, BaubleSlotType> definitions, boolean manual, String[] override,
            String[] stackOverrides, String[] universalOverrides) {
    }
}
