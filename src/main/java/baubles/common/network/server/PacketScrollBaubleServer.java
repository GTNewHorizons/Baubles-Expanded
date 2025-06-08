package baubles.common.network.server;

import baubles.common.container.ContainerPlayerExpanded;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.Container;

public class PacketScrollBaubleServer implements IMessage, IMessageHandler<PacketScrollBaubleServer, IMessage> {

    int windowId;
    int index;

    public PacketScrollBaubleServer(){}
    public PacketScrollBaubleServer(int windowId, int index) {
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
    public IMessage onMessage(PacketScrollBaubleServer message, MessageContext ctx) {
        EntityPlayerSP sp = Minecraft.getMinecraft().thePlayer;
        Container container = sp.openContainer;
        if (container instanceof ContainerPlayerExpanded && container.windowId == message.windowId) {
            ((ContainerPlayerExpanded) container).scrollToIndex(message.index);
        }
        return null;
    }
}
