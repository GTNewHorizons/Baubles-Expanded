package baubles.common.network;

import java.io.IOException;

import baubles.api.IBauble;
import baubles.common.Baubles;
import baubles.common.container.InventoryBaubles;
import baubles.common.lib.PlayerHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

public class PacketSyncBauble implements IMessage, IMessageHandler<PacketSyncBauble, IMessage> {

	int slot;
	int playerId;
	ItemStack bauble = null;
	boolean initial;

	public PacketSyncBauble() {}

	public PacketSyncBauble(EntityPlayer player, int slot) {
		this(player, slot, false);
	}

	public PacketSyncBauble(EntityPlayer player, int slot, boolean reset) {
		this.slot = slot;
		this.bauble = PlayerHandler.getPlayerBaubles(player).getStackInSlot(slot);
		this.playerId = player.getEntityId();
		this.initial = reset;
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeByte(slot);
		buffer.writeInt(playerId);
		buffer.writeBoolean(initial);
		PacketBuffer pb = new PacketBuffer(buffer);
		try { pb.writeItemStackToBuffer(bauble); } catch (IOException ignored) {}
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		slot = buffer.readByte();
		playerId = buffer.readInt();
		initial = buffer.readBoolean();
		PacketBuffer pb = new PacketBuffer(buffer);
		try { bauble = pb.readItemStackFromBuffer(); } catch (IOException ignored) {}
	}

	@Override
	public IMessage onMessage(PacketSyncBauble message, MessageContext ctx) {
		World world = Baubles.proxy.getClientWorld();
		if (world == null) return null;
		Entity e = world.getEntityByID(message.playerId);
		if (e instanceof EntityPlayer player) {
			InventoryBaubles baubles = PlayerHandler.getPlayerBaubles(player);
			if (message.initial) {
				if (message.slot == 0) {
					PlayerHandler.clearClientPlayerBaubles();
					baubles = PlayerHandler.getPlayerBaubles(player);
				}
				baubles.stackList[message.slot] = message.bauble;
				if (message.bauble != null && message.bauble.getItem() instanceof IBauble itemBauble) {
					itemBauble.onPlayerLoad(message.bauble, player);
				}
			}
			else {
				baubles.setInventorySlotContents(message.slot, message.bauble);
			}
		}
		return null;
	}
}
