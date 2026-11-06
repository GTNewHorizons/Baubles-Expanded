package baubles.common.container;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.BitSet;

import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.BaubleSlotConfiguration;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public class BaubleSlotStorage {

    public static void write(NBTTagCompound tags, BaubleSlotState state) {
        NBTTagList layout = new NBTTagList();
        for (String type : state.getAssignments()) layout.appendTag(new NBTTagString(type));
        tags.setTag("Baubles.Slots", layout);
        NBTTagList modifiers = new NBTTagList();
        for (Map.Entry<String, Map<String, Integer>> type : state.getModifiers().entrySet()) {
            for (Map.Entry<String, Integer> source : type.getValue().entrySet()) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("Type", type.getKey());
                entry.setString("Source", source.getKey());
                entry.setInteger("Amount", source.getValue());
                modifiers.appendTag(entry);
            }
        }
        tags.setTag("Baubles.SlotModifiers", modifiers);
        tags.setIntArray("Baubles.InactiveSlots", state.getInactiveSlots().stream().toArray());
    }

    public static BaubleSlotState read(NBTTagCompound tags) {
        String[] assignments = BaubleExpandedSlots.getCurrentSlotAssignments();
        if (tags.hasKey("Baubles.Slots", 9)) {
            NBTTagList layout = tags.getTagList("Baubles.Slots", 8);
            if (layout.tagCount() > BaubleSlotState.MAX_SLOTS) {
                throw new IllegalArgumentException("Saved bauble layout is too large");
            }
            assignments = new String[layout.tagCount()];
            for (int i = 0; i < assignments.length; i++) assignments[i] = layout.getStringTagAt(i);
        }
        Map<String, Map<String, Integer>> modifiers = new LinkedHashMap<>();
        NBTTagList entries = tags.getTagList("Baubles.SlotModifiers", 10);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            String source = entry.getString("Source");
            BaubleSlotState.validateSource(source);
            modifiers.computeIfAbsent(entry.getString("Type"), key -> new LinkedHashMap<>())
                    .put(source, entry.getInteger("Amount"));
        }
        BitSet inactive = new BitSet();
        for (int slot : tags.getIntArray("Baubles.InactiveSlots")) {
            if (slot >= 0 && slot < assignments.length) inactive.set(slot);
        }
        return new BaubleSlotState(assignments, modifiers,
                BaubleSlotConfiguration.getDefinitions(), inactive);
    }
}
