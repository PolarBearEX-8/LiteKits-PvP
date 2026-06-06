package dev.souppvp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

final class PlayerStorageService {
    private final SoupPvPPlugin plugin;

    PlayerStorageService(SoupPvPPlugin plugin) {
        this.plugin = plugin;
    }

    File getStoredPlayerFile(Player player) {
        return new File(new File(plugin.getDataFolder(), "players"), player.getUniqueId().toString() + ".dat");
    }

    boolean save(Player player, StoredPlayerData data) {
        File storedPlayerFile = getStoredPlayerFile(player);
        if (!storedPlayerFile.getParentFile().exists() && !storedPlayerFile.getParentFile().mkdirs()) {
            plugin.getLogger().warning("Could not create player data folder for " + player.getName());
            return false;
        }

        BukkitObjectOutputStream output = null;
        try {
            output = new BukkitObjectOutputStream(new FileOutputStream(storedPlayerFile));
            output.writeObject(data.inventoryContents);
            output.writeObject(data.armorContents);
            output.writeInt(data.level);
            output.writeFloat(data.exp);
            output.writeInt(data.totalExperience);
            output.writeUTF(data.gameMode.name());
            output.writeUTF(data.minigameWorldName);
            output.writeDouble(data.health);
            output.writeInt(data.foodLevel);
            output.writeFloat(data.saturation);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save player data for " + player.getName() + ": " + exception.getMessage());
            return false;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    StoredPlayerData load(Player player) {
        File storedPlayerFile = getStoredPlayerFile(player);
        if (!storedPlayerFile.isFile()) {
            return null;
        }

        BukkitObjectInputStream input = null;
        try {
            input = new BukkitObjectInputStream(new FileInputStream(storedPlayerFile));
            ItemStack[] inventoryContents = (ItemStack[]) input.readObject();
            ItemStack[] armorContents = (ItemStack[]) input.readObject();
            int level = input.readInt();
            float exp = input.readFloat();
            int totalExperience = input.readInt();
            double health = player.getMaxHealth();
            int foodLevel = 20;
            float saturation = 5.0F;
            GameMode gameMode = GameMode.SURVIVAL;
            String minigameWorldName;
            String nextValue = input.readUTF();
            try {
                gameMode = GameMode.valueOf(nextValue);
                minigameWorldName = input.readUTF();
            } catch (IllegalArgumentException exception) {
                minigameWorldName = nextValue;
            }

            try {
                health = input.readDouble();
                foodLevel = input.readInt();
                saturation = input.readFloat();
            } catch (IOException ignored) {
            }
            return new StoredPlayerData(inventoryContents, armorContents, level, exp, totalExperience, health,
                    foodLevel, saturation, gameMode, minigameWorldName);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not load player data for " + player.getName() + ": " + exception.getMessage());
        } catch (ClassNotFoundException exception) {
            plugin.getLogger().warning("Could not load player data for " + player.getName() + ": " + exception.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }

        return null;
    }

    void delete(Player player) {
        File storedPlayerFile = getStoredPlayerFile(player);
        if (storedPlayerFile.isFile() && !storedPlayerFile.delete()) {
            plugin.getLogger().warning("Could not delete restored player data for " + player.getName());
        }
    }
}
