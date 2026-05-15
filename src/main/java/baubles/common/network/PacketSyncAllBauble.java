package baubles.common.network;

import baubles.api.IBauble;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.Baubles;
import baubles.common.container.InventoryBaubles;
import baubles.common.lib.PlayerHandler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PacketSyncAllBauble implements IMessage, IMessageHandler<PacketSyncAllBauble, IMessage> {

	private int playerId;
    private TIntObjectMap<ItemStack> inventory = new TIntObjectHashMap<>();

	public PacketSyncAllBauble() {}

	public PacketSyncAllBauble(EntityPlayer player) {
		this.playerId = player.getEntityId();
        InventoryBaubles inv = PlayerHandler.getPlayerBaubles(player);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack slotItem = inv.getStackInSlot(i);
            if (slotItem == null) continue;
            inventory.put(i, slotItem);
        }
    }

	@Override
	public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeVarInt(buffer, playerId, 4);
        ByteBufUtils.writeVarInt(buffer, inventory.size(), BaubleExpandedSlots.maxSlotIdBytes);
        inventory.forEachEntry((a, b) -> {
            ByteBufUtils.writeVarInt(buffer, a, BaubleExpandedSlots.maxSlotIdBytes);
            ByteBufUtils.writeItemStack(buffer, b);
            return true;
        });

	}

	@Override
	public void fromBytes(ByteBuf buffer) {
        playerId = ByteBufUtils.readVarInt(buffer, 4);
        int size = ByteBufUtils.readVarInt(buffer, BaubleExpandedSlots.maxSlotIdBytes);
        for (int i = 0; i < size; i++) {
            int slotId = ByteBufUtils.readVarInt(buffer, BaubleExpandedSlots.maxSlotIdBytes);
            ItemStack slotItem = ByteBufUtils.readItemStack(buffer);
            inventory.put(slotId, slotItem);
        }
	}

	@Override
	public IMessage onMessage(PacketSyncAllBauble message, MessageContext ctx) {
		World world = Baubles.proxy.getClientWorld();
		if (world == null) return null;
		Entity e = world.getEntityByID(message.playerId);
		if (e instanceof EntityPlayer player) {
            PlayerHandler.clearPlayerBaubles(player);
            InventoryBaubles baubles = PlayerHandler.getPlayerBaubles(player);
            message.inventory.forEachEntry((a, b) -> {
                baubles.stackList[a] = b;
                if (b.getItem() instanceof IBauble itemBauble) {
                    try {
                        itemBauble.onPlayerLoad(b, player);
                    } catch (Exception ex) {
                        Baubles.log.error("Error loading baubles {}", b, ex);
                    }
                }
                return true;
            });
		}
		return null;
	}
}
