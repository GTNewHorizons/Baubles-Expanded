package baubles.common.network;

import baubles.common.Baubles;
import baubles.common.container.BaubleSlotState;
import baubles.common.container.InventoryBaubles;
import baubles.common.lib.PlayerHandler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PacketSyncAllBauble implements IMessage, IMessageHandler<PacketSyncAllBauble, IMessage> {

    private int playerId;
    private int dimension;
    private long revision;
    private boolean oldGui;
    private BaubleSlotState state;
    private ItemStack[] inventory;

    public PacketSyncAllBauble() {}

    public PacketSyncAllBauble(EntityPlayer player) {
        playerId = player.getEntityId();
        dimension = player.dimension;
        InventoryBaubles inv = PlayerHandler.getPlayerBaubles(player);
        revision = inv.getLayoutRevision();
        state = inv.getSlotState();
        oldGui = inv.usesOldGuiRendering();
        inventory = new ItemStack[inv.getSizeInventory()];
        for (int i = 0; i < inventory.length; i++) inventory[i] = ItemStack.copyItemStack(inv.getStackInSlot(i));
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(playerId);
        buffer.writeInt(dimension);
        buffer.writeLong(revision);
        buffer.writeBoolean(oldGui);
        BaubleSlotCodec.writeState(buffer, state);
        writeStacks(buffer, inventory);
    }

    private static void writeStacks(ByteBuf buffer, ItemStack[] stacks) {
        int occupied = 0;
        for (ItemStack stack : stacks) if (stack != null) occupied++;
        ByteBufUtils.writeVarInt(buffer, occupied, 5);
        for (int slot = 0; slot < stacks.length; slot++) {
            if (stacks[slot] == null) continue;
            ByteBufUtils.writeVarInt(buffer, slot, 5);
            ByteBufUtils.writeItemStack(buffer, stacks[slot]);
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        playerId = buffer.readInt();
        dimension = buffer.readInt();
        revision = buffer.readLong();
        oldGui = buffer.readBoolean();
        state = BaubleSlotCodec.readState(buffer);
        inventory = new ItemStack[state.getAssignments().length];
        readStacks(buffer, inventory);
    }

    private static void readStacks(ByteBuf buffer, ItemStack[] stacks) {
        int occupied = BaubleSlotCodec.readSize(buffer, stacks.length);
        int previous = -1;
        for (int i = 0; i < occupied; i++) {
            int slot = BaubleSlotCodec.readSize(buffer, stacks.length - 1);
            if (slot <= previous) throw new DecoderException("Unordered bauble item indices");
            stacks[slot] = ByteBufUtils.readItemStack(buffer);
            previous = slot;
        }
    }

    @Override
    public IMessage onMessage(PacketSyncAllBauble message, MessageContext ctx) {
        World world = Baubles.proxy.getClientWorld();
        if (world == null || world.provider.dimensionId != message.dimension) return null;
        Entity entity = world.getEntityByID(message.playerId);
        if (entity instanceof EntityPlayer player) {
            PlayerHandler.getPlayerBaubles(player).applySnapshot(
                    message.state, message.revision, message.inventory, message.oldGui);
        }
        return null;
    }
}
