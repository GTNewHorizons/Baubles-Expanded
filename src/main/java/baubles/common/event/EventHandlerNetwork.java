package baubles.common.event;

import baubles.api.IBauble;
import baubles.common.container.InventoryBaubles;
import baubles.common.lib.PlayerHandler;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketSyncBauble;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class EventHandlerNetwork {

    @SubscribeEvent
    public void playerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
        // Apply all baubles
        InventoryBaubles baubles = PlayerHandler.getPlayerBaubles(event.player);
        for (int i = 0; i < baubles.getSizeInventory(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            PacketHandler.INSTANCE.sendToDimension(new PacketSyncBauble(event.player, i, true), event.player.dimension);
            if (stack != null && stack.getItem() instanceof IBauble itemBauble) {
                itemBauble.onPlayerLoad(stack, event.player);
            }
        }
    }

    @SubscribeEvent
    public void playerChangedDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        InventoryBaubles baubles = PlayerHandler.getPlayerBaubles(event.player);
        for (int i = 0; i < baubles.getSizeInventory(); i++) {
            PacketHandler.INSTANCE.sendToDimension(new PacketSyncBauble(event.player, i, true), event.player.dimension);
        }
    }

    public static void syncBaubles(EntityPlayer player) {
        InventoryBaubles baubles = PlayerHandler.getPlayerBaubles(player);
        for (int i = 0; i < baubles.getSizeInventory(); i++) {
            baubles.syncSlotToClients(i);
        }
    }

}
