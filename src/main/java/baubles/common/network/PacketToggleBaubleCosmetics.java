package baubles.common.network;

import baubles.common.Baubles;
import baubles.common.container.ContainerPlayerExpanded;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public class PacketToggleBaubleCosmetics implements IMessage, IMessageHandler<PacketToggleBaubleCosmetics, IMessage> {

    private int windowId;
    private long revision;

    public PacketToggleBaubleCosmetics() {}

    public PacketToggleBaubleCosmetics(int windowId, long revision) {
        this.windowId = windowId;
        this.revision = revision;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(windowId);
        buffer.writeLong(revision);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        windowId = buffer.readInt();
        revision = buffer.readLong();
    }

    @Override
    public IMessage onMessage(PacketToggleBaubleCosmetics message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (!(player.openContainer instanceof ContainerPlayerExpanded container)
                || container.windowId != message.windowId || !container.canInteractWith(player)
                || container.baubles.getLayoutRevision() != message.revision) return null;
        int gui = container.isCosmeticView() ? Baubles.GUI : Baubles.GUI_COSMETICS;
        PacketHandler.INSTANCE.sendTo(new PacketSyncAllBauble(player), player);
        ItemStack cursor = player.inventory.getItemStack();
        player.inventory.setItemStack(null);
        ItemStack[] crafting = new ItemStack[container.craftMatrix.getSizeInventory()];
        for (int slot = 0; slot < crafting.length; slot++) crafting[slot] = container.craftMatrix.getStackInSlotOnClosing(slot);
        try {
            player.openGui(Baubles.instance, gui, player.worldObj, (int) player.posX, (int) player.posY, (int) player.posZ);
        } finally {
            ContainerPlayerExpanded target = player.openContainer instanceof ContainerPlayerExpanded expanded ? expanded : container;
            for (int slot = 0; slot < crafting.length; slot++) target.craftMatrix.setInventorySlotContents(slot, crafting[slot]);
            player.inventory.setItemStack(cursor);
        }
        player.sendContainerToPlayer(player.openContainer);
        return null;
    }
}
