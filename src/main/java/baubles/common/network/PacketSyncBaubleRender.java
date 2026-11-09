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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class PacketSyncBaubleRender implements IMessage, IMessageHandler<PacketSyncBaubleRender, IMessage> {

    private int playerId;
    private int dimension;
    private int slot;
    private long revision;
    private boolean rendered;

    public PacketSyncBaubleRender() {}

    public PacketSyncBaubleRender(EntityPlayer player, int slot) {
        InventoryBaubles inventory = PlayerHandler.getPlayerBaubles(player);
        playerId = player.getEntityId();
        dimension = player.dimension;
        this.slot = slot;
        revision = inventory.getLayoutRevision();
        rendered = inventory.isSlotRendered(slot);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(playerId);
        buffer.writeInt(dimension);
        ByteBufUtils.writeVarInt(buffer, slot, 5);
        buffer.writeLong(revision);
        buffer.writeBoolean(rendered);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        playerId = buffer.readInt();
        dimension = buffer.readInt();
        slot = BaubleSlotCodec.readSize(buffer, BaubleSlotState.MAX_SLOTS - 1);
        revision = buffer.readLong();
        rendered = buffer.readBoolean();
    }

    @Override
    public IMessage onMessage(PacketSyncBaubleRender message, MessageContext ctx) {
        World world = Baubles.proxy.getClientWorld();
        if (world == null || world.provider.dimensionId != message.dimension) return null;
        Entity entity = world.getEntityByID(message.playerId);
        if (entity instanceof EntityPlayer player) {
            PlayerHandler.getPlayerBaubles(player).applyRenderState(message.slot, message.revision, message.rendered);
        }
        return null;
    }
}
