package dev.souppvp;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class StoredPlayerData {
    final ItemStack[] inventoryContents;
    final ItemStack[] armorContents;
    final int level;
    final float exp;
    final int totalExperience;
    final double health;
    final int foodLevel;
    final float saturation;
    final GameMode gameMode;
    final String minigameWorldName;

    StoredPlayerData(ItemStack[] inventoryContents, ItemStack[] armorContents, int level, float exp,
            int totalExperience, double health, int foodLevel, float saturation, GameMode gameMode,
            String minigameWorldName) {
        this.inventoryContents = inventoryContents;
        this.armorContents = armorContents;
        this.level = level;
        this.exp = exp;
        this.totalExperience = totalExperience;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.gameMode = gameMode;
        this.minigameWorldName = minigameWorldName;
    }

    static StoredPlayerData from(Player player, String minigameWorldName) {
        return new StoredPlayerData(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getLevel(),
                player.getExp(),
                player.getTotalExperience(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getGameMode(),
                minigameWorldName);
    }
}
