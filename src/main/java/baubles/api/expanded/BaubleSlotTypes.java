package baubles.api.expanded;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;

/**
 * Programmatic defaults registered during pre-initialization. Configuration can override them.
 * Read a player's effective definitions through IBaubleSlotSettings instead of this registry.
 */
public class BaubleSlotTypes {

    private static final Map<String, BaubleSlotType> defaults = new LinkedHashMap<>();

    public static boolean register(BaubleSlotType type) {
        if (Loader.instance().getLoaderState() != LoaderState.PREINITIALIZATION) return false;
        if (!BaubleExpandedSlots.tryRegisterType(type.id())) return false;
        defaults.put(type.id(), type);
        return true;
    }

    public static Map<String, BaubleSlotType> getDefaults() {
        Map<String, BaubleSlotType> result = new LinkedHashMap<>();
        for (String id : BaubleExpandedSlots.getCurrentlyRegisteredTypes()) {
            result.put(id, defaults.getOrDefault(id, BaubleSlotType.defaults(id)));
        }
        return Collections.unmodifiableMap(result);
    }
}
