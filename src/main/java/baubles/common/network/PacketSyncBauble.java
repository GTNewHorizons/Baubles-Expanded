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
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PacketSyncBauble implements IMessage, IMessageHandler<PacketSyncBauble, IMessage> {

    private int slot;
    private int playerId;
    private int dimension;
    private long revision;
    private ItemStack bauble;

    public PacketSyncBauble() {}

    public PacketSyncBauble(EntityPlayer player, int slot) {
        InventoryBaubles inventory = PlayerHandler.getPlayerBaubles(player);
        this.slot = slot;
        bauble = ItemStack.copyItemStack(inventory.getStackInSlot(slot));
        playerId = player.getEntityId();
        dimension = player.dimension;
        revision = inventory.getLayoutRevision();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeVarInt(buffer, slot, 5);
        buffer.writeInt(playerId);
        buffer.writeInt(dimension);
        buffer.writeLong(revision);
        ByteBufUtils.writeItemStack(buffer, bauble);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        slot = BaubleSlotCodec.readSize(buffer, BaubleSlotState.MAX_SLOTS - 1);
        playerId = buffer.readInt();
        dimension = buffer.readInt();
        revision = buffer.readLong();
        bauble = ByteBufUtils.readItemStack(buffer);
    }

    @Override
    public IMessage onMessage(PacketSyncBauble message, MessageContext ctx) {
        World world = Baubles.proxy.getClientWorld();
        if (world == null || world.provider.dimensionId != message.dimension) return null;
        Entity entity = world.getEntityByID(message.playerId);
        if (entity instanceof EntityPlayer player) {
            InventoryBaubles inventory = PlayerHandler.getPlayerBaubles(player);
            if (message.revision == inventory.getLayoutRevision() && message.slot < inventory.getSizeInventory()) {
                inventory.setInventorySlotContents(message.slot, message.bauble);
            }
        }
        return null;
    }
}
