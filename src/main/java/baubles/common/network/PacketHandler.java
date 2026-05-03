package baubles.common.network;

import baubles.common.Baubles;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.world.WorldServer;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Baubles.MODID.toLowerCase());

    public static void init() {
        INSTANCE.registerMessage(PacketOpenBaublesInventory.class, PacketOpenBaublesInventory.class, 0, Side.SERVER);
        INSTANCE.registerMessage(PacketOpenNormalInventory.class, PacketOpenNormalInventory.class, 1, Side.SERVER);
        INSTANCE.registerMessage(PacketSyncBauble.class, PacketSyncBauble.class, 2, Side.CLIENT);
        INSTANCE.registerMessage(PacketSyncAllBauble.class, PacketSyncAllBauble.class, 3, Side.CLIENT);
    }

    public static void sendToTracking(IMessage message, EntityPlayer target, boolean sendToTarget) {
        if (target.worldObj.isRemote) {
            // won't work
            Baubles.log.error("Sending message {} from wrong side: cannot find tracking players on client side.", message.getClass().getName());
            return;
        }
        Packet packet = INSTANCE.getPacketFrom(message);
        EntityTracker tracker = ((WorldServer) target.worldObj).getEntityTracker();
        if (sendToTarget) {
            tracker.func_151248_b(target, packet);
        } else {
            tracker.func_151247_a(target, packet);
        }
    }
}
