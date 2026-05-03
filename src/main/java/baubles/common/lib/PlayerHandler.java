package baubles.common.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import com.google.common.io.Files;

import baubles.common.Baubles;
import baubles.common.container.InventoryBaubles;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

public class PlayerHandler {

	private static final HashMap<String, InventoryBaubles> playerBaublesServer = new HashMap<>();
	private static final HashMap<String, InventoryBaubles> playerBaublesClient = new HashMap<>();

	public static void clearPlayerBaubles(EntityPlayer player) {
        clearPlayerBaubles(player.getCommandSenderName(), player.worldObj.isRemote);
	}

	public static void clearClientPlayerBaubles() {
		playerBaublesClient.clear();
	}

    public static void pruneServerSide() {
        baublesPrune(playerBaublesServer, false);
    }

    public static void pruneClientSide() {
        baublesPrune(playerBaublesClient, true);
    }

    private static void baublesPrune(HashMap<String, InventoryBaubles> map, boolean isClient) {
        for (var iter = map.entrySet().iterator(); iter.hasNext(); ) {
            var e = iter.next();
            String playerName = e.getKey();
            InventoryBaubles baubles = e.getValue();
            EntityPlayer player = baubles.player.get();
            if (player != null && !player.isDead) continue;
            EntityPlayer newPlayer = null;
            if (isClient) {
                for (Object o : Baubles.proxy.getClientWorld().playerEntities) {
                    EntityPlayer playerEntity = (EntityPlayer) o;
                    if (playerEntity.getCommandSenderName().equals(playerName)) {
                        newPlayer = playerEntity;
                        break;
                    }
                }
            } else {
                newPlayer = MinecraftServer.getServer().getConfigurationManager().func_152612_a(playerName);
            }
            if (newPlayer != null) {
                baubles.player = new WeakReference<>(newPlayer);
                continue;
            }
            iter.remove();
        }
    }

    public static void clearPlayerBaubles(String playerName, boolean isClient) {
        HashMap<String, InventoryBaubles> map = isClient ? playerBaublesClient : playerBaublesServer;
        map.remove(playerName);
    }

	public static InventoryBaubles getPlayerBaubles(EntityPlayer player) {
		if (player.worldObj.isRemote) {
			return playerBaublesClient.computeIfAbsent(player.getCommandSenderName(), username -> new InventoryBaubles(player));
		} else {
			return playerBaublesServer.computeIfAbsent(player.getCommandSenderName(), username -> new InventoryBaubles(player));
		}
	}

	public static void setPlayerBaubles(EntityPlayer player, InventoryBaubles inventory) {
		if (player.worldObj.isRemote) {
			playerBaublesClient.put(player.getCommandSenderName(), inventory);
		} else {
			playerBaublesServer.put(player.getCommandSenderName(), inventory);
		}
	}

	public static void loadPlayerBaubles(EntityPlayer player, File mainFile, File backupFile) {
		if(player != null && !player.worldObj.isRemote) {
			try {
				NBTTagCompound data = null;
				boolean save = false;
				if(mainFile != null && mainFile.exists()) {
					try {
						FileInputStream fileinputstream = new FileInputStream(mainFile);
						data = CompressedStreamTools.readCompressed(fileinputstream);
						fileinputstream.close();
					} catch (Exception loadMainException) {
						loadMainException.printStackTrace();
					}
				}

				if(mainFile == null || !mainFile.exists() || data == null || data.hasNoTags()) {
					Baubles.log.warn("Data not found for "
							+ player.getCommandSenderName()
							+ ". Trying to load backup data."
					);
					if(backupFile != null && backupFile.exists()) {
						try {
							FileInputStream fileinputstream = new FileInputStream(backupFile);
							data = CompressedStreamTools.readCompressed(fileinputstream);
							fileinputstream.close();
							save = true;
						} catch(Exception loadBackupException) {
							loadBackupException.printStackTrace();
						}
					}
				}

				if(data != null) {
					InventoryBaubles inventory = new InventoryBaubles(player);
					inventory.readNBT(data);
					playerBaublesServer.put(player.getCommandSenderName(), inventory);
					if(save) {
						savePlayerBaubles(player, mainFile, backupFile);
					}
				}
			} catch(Exception loadException) {
				Baubles.log.fatal("Error loading baubles inventory");
				loadException.printStackTrace();
			}
		}
	}

	public static void savePlayerBaubles(EntityPlayer player, File mainFile, File backupFile) {
		if(player != null && !player.worldObj.isRemote) {
			try {
				if (mainFile != null && mainFile.exists()) {
					try {
						Files.copy(mainFile, backupFile);
					} catch (Exception saveBackupException) {
						Baubles.log.error("Could not backup old baubles file for player " + player.getCommandSenderName());
					}
				}

				try {
					if(mainFile != null) {
						InventoryBaubles inventory = getPlayerBaubles(player);
						NBTTagCompound data = new NBTTagCompound();
						inventory.saveNBT(data);

						FileOutputStream fileoutputstream = new FileOutputStream(mainFile);
						CompressedStreamTools.writeCompressed(data, fileoutputstream);
						fileoutputstream.close();
					}
				} catch(Exception saveMainException) {
					Baubles.log.error("Could not save baubles file for player " + player.getCommandSenderName());
					saveMainException.printStackTrace();
					if (mainFile.exists()) {
						try {
							mainFile.delete();
						} catch (Exception ignored) {
						}
					}
				}
			} catch(Exception saveException) {
				Baubles.log.fatal("Error saving baubles inventory");
				saveException.printStackTrace();
			}
		}
	}

}
