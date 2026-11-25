package baubles.common.lib;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public class PlayerBaublesProperties implements IExtendedEntityProperties {

    public static final String IDENTIFIER = "Baubles:inventory";
    public static final String NBT_KEY = "Baubles";

    private EntityPlayer player;
    private NBTTagCompound loadedData;

    @Override
    public void init(Entity entity, World world) {
        player = (EntityPlayer) entity;
    }

    @Override
    public void saveNBTData(NBTTagCompound tags) {
        if (player == null || player.worldObj.isRemote) return;
        NBTTagCompound inventory = new NBTTagCompound();
        PlayerHandler.getPlayerBaubles(player).saveNBT(inventory);
        tags.setTag(NBT_KEY, inventory);
    }

    @Override
    public void loadNBTData(NBTTagCompound tags) {
        loadedData = tags.hasKey(NBT_KEY, 10) ? (NBTTagCompound) tags.getCompoundTag(NBT_KEY).copy() : null;
    }

    /**
     * Consumes the snapshot read with the player, before considering older standalone files.
     */
    public static NBTTagCompound takeLoadedData(EntityPlayer player) {
        if (player.getExtendedProperties(IDENTIFIER) instanceof PlayerBaublesProperties properties) {
            NBTTagCompound data = properties.loadedData;
            properties.loadedData = null;
            return data;
        }
        return null;
    }
}
