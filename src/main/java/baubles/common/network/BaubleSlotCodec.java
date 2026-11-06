package baubles.common.network;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.BitSet;

import baubles.api.expanded.BaubleSlotType;
import baubles.common.container.BaubleSlotState;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public class BaubleSlotCodec {

    public static void writeState(ByteBuf buffer, BaubleSlotState state) {
        String[] assignments = state.getAssignments();
        ByteBufUtils.writeVarInt(buffer, assignments.length, 5);
        for (String type : assignments) ByteBufUtils.writeUTF8String(buffer, type);
        ByteBufUtils.writeVarInt(buffer, state.getModifiers().size(), 5);
        for (Map.Entry<String, Map<String, Integer>> type : state.getModifiers().entrySet()) {
            ByteBufUtils.writeUTF8String(buffer, type.getKey());
            ByteBufUtils.writeVarInt(buffer, type.getValue().size(), 5);
            for (Map.Entry<String, Integer> source : type.getValue().entrySet()) {
                ByteBufUtils.writeUTF8String(buffer, source.getKey());
                buffer.writeInt(source.getValue());
            }
        }
        writePolicies(buffer, state);
    }

    private static void writePolicies(ByteBuf buffer, BaubleSlotState state) {
        ByteBufUtils.writeVarInt(buffer, state.getDefinitions().size(), 5);
        for (BaubleSlotType type : state.getDefinitions().values()) {
            ByteBufUtils.writeUTF8String(buffer, type.id());
            buffer.writeInt(type.order());
            ByteBufUtils.writeUTF8String(buffer, type.icon());
            buffer.writeByte((type.cosmetic() ? 1 : 0) | (type.renderToggle() ? 2 : 0) | (type.nativeGui() ? 4 : 0)
                    | (type.enabled() ? 8 : 0) | (type.fitsUniversal() ? 16 : 0));
            buffer.writeInt(type.stackLimit());
        }
        int[] inactive = state.getInactiveSlots().stream().toArray();
        ByteBufUtils.writeVarInt(buffer, inactive.length, 5);
        for (int slot : inactive) ByteBufUtils.writeVarInt(buffer, slot, 5);
    }

    public static BaubleSlotState readState(ByteBuf buffer) {
        int size = readSize(buffer, BaubleSlotState.MAX_SLOTS);
        String[] assignments = new String[size];
        for (int i = 0; i < size; i++) assignments[i] = ByteBufUtils.readUTF8String(buffer);
        Map<String, Map<String, Integer>> modifiers = new LinkedHashMap<>();
        int types = readSize(buffer, buffer.readableBytes());
        for (int i = 0; i < types; i++) {
            String type = ByteBufUtils.readUTF8String(buffer);
            int sources = readSize(buffer, buffer.readableBytes());
            Map<String, Integer> values = new LinkedHashMap<>();
            for (int j = 0; j < sources; j++) {
                String source = ByteBufUtils.readUTF8String(buffer);
                BaubleSlotState.validateSource(source);
                if (values.put(source, buffer.readInt()) != null) throw new DecoderException("Duplicate slot source");
            }
            if (modifiers.put(type, values) != null) throw new DecoderException("Duplicate slot type");
        }
        Map<String, BaubleSlotType> definitions = new LinkedHashMap<>();
        int definitionCount = readSize(buffer, buffer.readableBytes());
        for (int i = 0; i < definitionCount; i++) {
            String id = ByteBufUtils.readUTF8String(buffer);
            int order = buffer.readInt();
            String icon = ByteBufUtils.readUTF8String(buffer);
            int flags = buffer.readUnsignedByte();
            if ((flags & ~31) != 0) throw new DecoderException("Invalid slot policy flags");
            BaubleSlotType type = new BaubleSlotType(id, order, icon, (flags & 1) != 0, (flags & 2) != 0,
                    (flags & 4) != 0, (flags & 8) != 0, buffer.readInt(), (flags & 16) != 0);
            if (definitions.put(id, type) != null) throw new DecoderException("Duplicate slot definition");
        }
        for (String type : assignments) {
            if (!definitions.containsKey(type)) throw new DecoderException("Missing slot definition: " + type);
        }
        BitSet inactive = new BitSet();
        int inactiveCount = readSize(buffer, size);
        int previous = -1;
        for (int i = 0; i < inactiveCount; i++) {
            int slot = readSize(buffer, size - 1);
            if (slot <= previous) throw new DecoderException("Unordered inactive slots");
            inactive.set(slot);
            previous = slot;
        }
        return new BaubleSlotState(assignments, modifiers, definitions, inactive);
    }

    public static int readSize(ByteBuf buffer, int maximum) {
        int size = ByteBufUtils.readVarInt(buffer, 5);
        if (size < 0 || size > maximum) throw new DecoderException("Invalid bauble size: " + size);
        return size;
    }
}
