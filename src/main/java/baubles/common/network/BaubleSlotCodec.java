package baubles.common.network;

import java.util.LinkedHashMap;
import java.util.Map;

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
        return new BaubleSlotState(assignments, modifiers);
    }

    public static int readSize(ByteBuf buffer, int maximum) {
        int size = ByteBufUtils.readVarInt(buffer, 5);
        if (size < 0 || size > maximum) throw new DecoderException("Invalid bauble size: " + size);
        return size;
    }
}
