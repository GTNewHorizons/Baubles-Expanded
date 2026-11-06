package baubles.common.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.BaubleSlotType;
import baubles.common.BaubleSlotConfiguration;

public class BaubleSlotState {

    public static final int MAX_SLOTS = Short.MAX_VALUE - 45;

    private final String[] assignments;
    private final Map<String, Map<String, Integer>> modifiers;
    private final Map<String, BaubleSlotType> definitions;
    private final BitSet inactive;
    private final int[] typeIndices;

    public BaubleSlotState(String[] assignments, Map<String, Map<String, Integer>> modifiers) {
        this(assignments, modifiers, BaubleSlotConfiguration.getDefinitions(), new BitSet());
    }

    public BaubleSlotState(String[] assignments, Map<String, Map<String, Integer>> modifiers,
            Map<String, BaubleSlotType> definitions, BitSet inactive) {
        if (assignments.length > MAX_SLOTS) {
            throw new IllegalArgumentException("Too many bauble slots: " + assignments.length);
        }
        this.assignments = assignments.clone();
        typeIndices = new int[assignments.length];
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int slot = 0; slot < assignments.length; slot++) {
            typeIndices[slot] = counts.getOrDefault(assignments[slot], 0);
            counts.put(assignments[slot], typeIndices[slot] + 1);
        }
        this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        this.inactive = (BitSet) inactive.clone();
        this.inactive.clear(assignments.length, Math.max(assignments.length, this.inactive.length()));
        this.modifiers = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : modifiers.entrySet()) {
            this.modifiers.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
    }

    public String[] getAssignments() {
        return assignments.clone();
    }

    public String getType(int slot) {
        return slot >= 0 && slot < assignments.length ? assignments[slot] : BaubleExpandedSlots.unknownType;
    }

    public int getCount(String type) {
        int count = 0;
        for (String assignment : assignments) {
            if (assignment.equals(type)) count++;
        }
        return count;
    }

    public Map<String, Map<String, Integer>> getModifiers() {
        return Collections.unmodifiableMap(modifiers);
    }

    public Map<String, Integer> getModifiers(String type) {
        return modifiers.getOrDefault(type, Collections.emptyMap());
    }

    public Map<String, BaubleSlotType> getDefinitions() {
        return definitions;
    }

    public BaubleSlotType getDefinition(String type) {
        BaubleSlotType definition = definitions.get(type);
        return definition == null ? BaubleSlotType.defaults(type) : definition;
    }

    public boolean isActive(int slot) {
        return slot >= 0 && slot < assignments.length && !BaubleExpandedSlots.unknownType.equals(getType(slot))
                && getDefinition(getType(slot)).enabled() && !inactive.get(slot);
    }

    public BitSet getInactiveSlots() {
        return (BitSet) inactive.clone();
    }

    public int getTypeIndex(int slot) {
        return slot >= 0 && slot < typeIndices.length ? typeIndices[slot] : -1;
    }

    public BaubleSlotState withSlotActive(int slot, boolean active) {
        if (slot < 0 || slot >= assignments.length || BaubleExpandedSlots.unknownType.equals(getType(slot))) {
            throw new IllegalArgumentException("Invalid bauble slot: " + slot);
        }
        if (active && !getDefinition(getType(slot)).enabled()) {
            throw new IllegalArgumentException("Slot type is disabled by configuration: " + getType(slot));
        }
        BitSet changed = getInactiveSlots();
        changed.set(slot, !active);
        return new BaubleSlotState(assignments, modifiers, definitions, changed);
    }

    public BaubleSlotState rebase(String[] base, Map<String, BaubleSlotType> types) {
        BaubleSlotState updated = resolve(base, assignments, modifiers, types);
        BitSet retained = getInactiveSlots();
        for (int slot = retained.nextSetBit(0); slot >= 0; slot = retained.nextSetBit(slot + 1)) {
            if (!getType(slot).equals(updated.getType(slot))) retained.clear(slot);
        }
        return new BaubleSlotState(updated.assignments, modifiers, types, retained);
    }

    public BaubleSlotState withSlotsActive(String type, boolean active) {
        if (active && !getDefinition(type).enabled()) throw new IllegalArgumentException("Slot type is disabled: " + type);
        BitSet changed = getInactiveSlots();
        for (int slot = 0; slot < assignments.length; slot++) {
            if (type.equals(assignments[slot])) changed.set(slot, !active);
        }
        return new BaubleSlotState(assignments, modifiers, definitions, changed);
    }

    public BaubleSlotState withModifier(String type, String source, int amount) {
        validateType(type);
        validateSource(source);
        Map<String, Map<String, Integer>> updated = new LinkedHashMap<>(modifiers);
        Map<String, Integer> values = new LinkedHashMap<>(getModifiers(type));
        if (amount == 0) values.remove(source);
        else values.put(source, amount);
        if (values.isEmpty()) updated.remove(type);
        else updated.put(type, values);
        return new BaubleSlotState(assignments, updated, definitions, inactive)
                .rebase(BaubleExpandedSlots.getCurrentSlotAssignments(), definitions);
    }

    public BaubleSlotState withoutSource(String source) {
        validateSource(source);
        Map<String, Map<String, Integer>> updated = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : modifiers.entrySet()) {
            Map<String, Integer> values = new LinkedHashMap<>(entry.getValue());
            values.remove(source);
            if (!values.isEmpty()) updated.put(entry.getKey(), values);
        }
        return new BaubleSlotState(assignments, updated, definitions, inactive)
                .rebase(BaubleExpandedSlots.getCurrentSlotAssignments(), definitions);
    }

    public static void validateType(String type) {
        if (!BaubleExpandedSlots.isTypeRegistered(type) || BaubleExpandedSlots.unknownType.equals(type)) {
            throw new IllegalArgumentException("Unknown bauble slot type: " + type);
        }
    }

    public static void validateSource(String source) {
        if (source == null || source.trim().isEmpty() || source.length() > 256) {
            throw new IllegalArgumentException("A modifier source must contain 1 to 256 characters");
        }
    }

    public static BaubleSlotState resolve(String[] base, String[] previous,
            Map<String, Map<String, Integer>> modifiers) {
        return resolve(base, previous, modifiers, BaubleSlotConfiguration.getDefinitions());
    }

    private static BaubleSlotState resolve(String[] base, String[] previous,
            Map<String, Map<String, Integer>> modifiers, Map<String, BaubleSlotType> definitions) {
        Map<String, Long> remaining = new LinkedHashMap<>();
        for (String type : base) {
            if (!BaubleExpandedSlots.unknownType.equals(type)) {
                remaining.put(type, remaining.getOrDefault(type, 0L) + 1);
            }
        }
        for (Map.Entry<String, Map<String, Integer>> entry : modifiers.entrySet()) {
            if (!definitions.containsKey(entry.getKey())
                    || BaubleExpandedSlots.unknownType.equals(entry.getKey())) continue;
            long count = remaining.getOrDefault(entry.getKey(), 0L);
            for (int amount : entry.getValue().values()) count += amount;
            remaining.put(entry.getKey(), Math.max(0L, count));
        }
        long total = 0;
        for (long count : remaining.values()) total += count;
        if (total > MAX_SLOTS || base.length > MAX_SLOTS) {
            throw new IllegalArgumentException("Bauble slot capacity exceeds " + MAX_SLOTS);
        }

        List<String> layout = new ArrayList<>(Arrays.asList(base));
        for (int slot = 0; slot < base.length; slot++) {
            if (!consume(remaining, base[slot])) layout.set(slot, BaubleExpandedSlots.unknownType);
        }
        for (int slot = base.length; slot < previous.length; slot++) {
            String type = previous[slot];
            layout.add(consume(remaining, type) ? type : BaubleExpandedSlots.unknownType);
        }
        int free = base.length;
        for (Map.Entry<String, Long> entry : remaining.entrySet()) {
            for (long count = entry.getValue(); count > 0; count--) {
                while (free < layout.size() && !BaubleExpandedSlots.unknownType.equals(layout.get(free))) free++;
                if (free < layout.size()) layout.set(free, entry.getKey());
                else layout.add(entry.getKey());
                free++;
            }
        }
        while (layout.size() > base.length
                && BaubleExpandedSlots.unknownType.equals(layout.get(layout.size() - 1))) {
            layout.remove(layout.size() - 1);
        }
        return new BaubleSlotState(layout.toArray(new String[0]), modifiers, definitions, new BitSet());
    }

    private static boolean consume(Map<String, Long> counts, String type) {
        long count = counts.getOrDefault(type, 0L);
        if (count <= 0) return false;
        counts.put(type, count - 1);
        return true;
    }
}
