package baubles.api.expanded;

import java.util.Map;

/**
 * Optional type policies and activation. Inactive slots retain their real inventory contents.
 * Mutations belong on the server game thread and are deferred during item callbacks.
 */
public interface IBaubleSlotSettings extends IBaubleSlotInventory {

    Map<String, BaubleSlotType> getSlotDefinitions();

    BaubleSlotType getSlotDefinition(String type);

    boolean isSlotActive(int slot);

    void setSlotActive(int slot, boolean active);

    void setSlotsActive(String type, boolean active);

    BaubleSlotContext getSlotContext(int slot, boolean cosmetic);
}
