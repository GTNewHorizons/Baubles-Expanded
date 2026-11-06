package baubles.common.network;

import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.container.BaubleSlotState;
import baubles.common.container.ContainerPlayerExpanded;
import baubles.common.container.InventoryBaubles;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketToggleBaubleRender implements IMessage, IMessageHandler<PacketToggleBaubleRender, IMessage> {

    private int windowId;
    private int slot;
    private long revision;

    public PacketToggleBaubleRender() {}

    public PacketToggleBaubleRender(int windowId, int slot, long revision) {
        this.windowId = windowId;
        this.slot = slot;
        this.revision = revision;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(windowId);
        ByteBufUtils.writeVarInt(buffer, slot, 5);
        buffer.writeLong(revision);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        windowId = buffer.readInt();
        slot = BaubleSlotCodec.readSize(buffer, BaubleSlotState.MAX_SLOTS - 1);
        revision = buffer.readLong();
    }

    @Override
    public IMessage onMessage(PacketToggleBaubleRender message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (!(player.openContainer instanceof ContainerPlayerExpanded container)
                || container.windowId != message.windowId || !container.canInteractWith(player)) return null;
        InventoryBaubles inventory = container.baubles;
        if (inventory.getLayoutRevision() == message.revision && message.slot < inventory.getSizeInventory()
                && !BaubleExpandedSlots.unknownType.equals(inventory.getSlotType(message.slot))) {
            inventory.setSlotRendered(message.slot, !inventory.isSlotRendered(message.slot));
        }
        return null;
    }
}
