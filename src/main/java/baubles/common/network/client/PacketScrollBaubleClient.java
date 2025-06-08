package baubles.common.network.client;

import baubles.common.container.ContainerPlayerExpanded;
import baubles.common.network.server.PacketOpenNormalInventory;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

public class PacketScrollBaubleClient implements IMessage, IMessageHandler<PacketScrollBaubleClient, IMessage> {

    int windowId;
    int index;

    public PacketScrollBaubleClient() {};

    public PacketScrollBaubleClient(int windowId, int index) {
        this.windowId = windowId;
        this.index = index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        index = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        buf.writeInt(index);
    }


    @Override
    public IMessage onMessage(PacketScrollBaubleClient message, MessageContext ctx) {
        EntityPlayerMP serverPlayer = ctx.getServerHandler().playerEntity;
        Container container = serverPlayer.openContainer;
        if (container instanceof ContainerPlayerExpanded && container.windowId == message.windowId) {
            ((ContainerPlayerExpanded) container).scrollToIndex(message.index);
        }
        return null;
    }
}
