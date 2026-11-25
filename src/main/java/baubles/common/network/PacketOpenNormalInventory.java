package baubles.common.network;

import baubles.common.container.ContainerPlayerExpanded;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketOpenNormalInventory implements IMessage, IMessageHandler<PacketOpenNormalInventory, IMessage> {

    private int windowId = -1;

	public PacketOpenNormalInventory() {}

	public PacketOpenNormalInventory(EntityPlayer player) {
        windowId = player.openContainer.windowId;
    }

	@Override
	public void toBytes(ByteBuf buffer) {
        buffer.writeInt(windowId);
    }

	@Override
	public void fromBytes(ByteBuf buffer) {
        windowId = buffer.isReadable() ? buffer.readInt() : -1;
    }

	@Override
	public IMessage onMessage(PacketOpenNormalInventory message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (!(player.openContainer instanceof ContainerPlayerExpanded container) || !container.canInteractWith(player)
                || message.windowId >= 0 && container.windowId != message.windowId) return null;
        player.closeContainer();
        player.sendContainerToPlayer(player.inventoryContainer);
		return null;
	}
}
