package baubles.common.event;

import baubles.common.lib.PlayerHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class EventHandlerCleanup {
    private int serverTickCount;
    private int clientTickCount;

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && serverTickCount++ % 200 == 120) {
            PlayerHandler.pruneServerSide();
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.END && clientTickCount++ % 200 == 52) {
			PlayerHandler.pruneClientSide();
		}
	}
}
