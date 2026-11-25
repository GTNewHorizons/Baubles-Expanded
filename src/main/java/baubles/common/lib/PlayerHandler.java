package baubles.common.lib;

import baubles.common.Baubles;
import baubles.common.container.InventoryBaubles;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

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

    public static void clearServerPlayerBaubles() {
        playerBaublesServer.clear();
    }

    public static void onWorldUnload(World world) {
        if (world.isRemote) {
            playerBaublesClient.values().removeIf(inv -> {
                final EntityPlayer player = inv.player.get();
                return player == null || player.worldObj == world;
            });
        }
    }

	public static InventoryBaubles getPlayerBaubles(EntityPlayer player) {
        HashMap<String, InventoryBaubles> inventories = player.worldObj.isRemote ? playerBaublesClient : playerBaublesServer;
        InventoryBaubles inventory = inventories.computeIfAbsent(player.getCommandSenderName(), name -> new InventoryBaubles(player));
        if (inventory.player.get() != player) inventory.player = new WeakReference<>(player);
        return inventory;
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
				NBTTagCompound data = PlayerBaublesProperties.takeLoadedData(player);
				if (data != null) {
                    InventoryBaubles inventory = new InventoryBaubles(player);
                    inventory.readNBT(data);
                    playerBaublesServer.put(player.getCommandSenderName(), inventory);
                    return;
                }
				boolean save = false;
				if(mainFile != null && mainFile.exists()) {
					try {
                        try (FileInputStream stream = new FileInputStream(mainFile)) {
                            data = CompressedStreamTools.readCompressed(stream);
                        }
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
                            try (FileInputStream stream = new FileInputStream(backupFile)) {
                                data = CompressedStreamTools.readCompressed(stream);
                            }
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
						savePlayerBaubles(player, mainFile, backupFile, false);
					}
				}
			} catch(Exception loadException) {
				Baubles.log.fatal("Error loading baubles inventory");
				loadException.printStackTrace();
			}
		}
	}

	public static void savePlayerBaubles(EntityPlayer player, File mainFile, File backupFile) {
        savePlayerBaubles(player, mainFile, backupFile, true);
	}

    private static void savePlayerBaubles(EntityPlayer player, File mainFile, File backupFile, boolean backupPrevious) {
        if (player == null || player.worldObj.isRemote || mainFile == null) return;
        Path target = mainFile.toPath();
        Path temporary = target.resolveSibling(mainFile.getName() + ".tmp");
        try {
            NBTTagCompound data = new NBTTagCompound();
            getPlayerBaubles(player).saveNBT(data);
            try (OutputStream stream = Files.newOutputStream(temporary)) {
                CompressedStreamTools.writeCompressed(data, stream);
            }
            if (backupPrevious && Files.exists(target) && backupFile != null) {
                Path backup = backupFile.toPath();
                Path backupTemporary = backup.resolveSibling(backupFile.getName() + ".tmp");
                try {
                    Files.copy(target, backupTemporary, StandardCopyOption.REPLACE_EXISTING);
                    replaceFile(backupTemporary, backup);
                } finally {
                    Files.deleteIfExists(backupTemporary);
                }
            }
            replaceFile(temporary, target);
        } catch (Exception e) {
            Baubles.log.error("Could not save baubles file for player " + player.getCommandSenderName(), e);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException e) {
                Baubles.log.warn("Could not remove temporary baubles file " + temporary, e);
            }
        }
    }

    private static void replaceFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
