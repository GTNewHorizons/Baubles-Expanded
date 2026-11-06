package baubles.common.event;

import baubles.api.IBauble;
import baubles.common.Baubles;
import baubles.common.container.InventoryBaubles;
import baubles.common.lib.PlayerHandler;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketSyncAllBauble;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public class EventHandlerNetwork {

	@SubscribeEvent
	public void playerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event)    {
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.SERVER) {
			// Apply all baubles
            InventoryBaubles baubles = PlayerHandler.getPlayerBaubles(event.player);
            baubles.returnOverflowItems();
            PacketHandler.INSTANCE.sendTo(new PacketSyncAllBauble(event.player), ((EntityPlayerMP) event.player));
			for (int i = 0; i < baubles.getSizeInventory(); i++) {
				ItemStack stack = baubles.getStackInSlot(i);
				if (stack != null && stack.getItem() instanceof IBauble itemBauble) {
					itemBauble.onPlayerLoad(stack, event.player);
				}
			}

		}
	}

    @SubscribeEvent
    public void playerChangedDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP playerMP) {
            PacketHandler.INSTANCE.sendTo(new PacketSyncAllBauble(playerMP), playerMP);
        }
    }

    @SubscribeEvent
    public void onConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        // clear old cache
        PlayerHandler.clearClientPlayerBaubles();
    }

    @SubscribeEvent
    public void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP playerMP) {
            InventoryBaubles baubles = PlayerHandler.getPlayerBaubles(playerMP);
            baubles.returnOverflowItems();
            for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
                ItemStack stack = baubles.getStackInSlot(slot);
                if (stack != null && stack.getItem() instanceof IBauble item) {
                    try {
                        item.onPlayerLoad(stack, playerMP);
                    } catch (Exception e) {
                        Baubles.log.error("Error restoring bauble after respawn {}", stack, e);
                    }
                }
            }
            PacketHandler.sendToTracking(new PacketSyncAllBauble(playerMP), playerMP, true);
        }
    }

	public static void syncBaubles(EntityPlayer player) {
        if (player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
            PacketHandler.sendToTracking(new PacketSyncAllBauble(player), player, true);
        }
    }

}
