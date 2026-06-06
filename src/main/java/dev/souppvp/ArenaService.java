package dev.souppvp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class ArenaService {
    private final SoupPvPPlugin plugin;
    private final MessageService messages;
    private final PlayerStorageService storage;
    private final StatsService stats;
    private final CooldownService cooldowns;
    private final ScoreboardService scoreboard;
    private final Map<UUID, StoredPlayerData> storedPlayers = new HashMap<UUID, StoredPlayerData>();
    private final Map<UUID, Integer> selectedKits = new HashMap<UUID, Integer>();

    ArenaService(SoupPvPPlugin plugin, MessageService messages, PlayerStorageService storage, StatsService stats,
            CooldownService cooldowns, ScoreboardService scoreboard) {
        this.plugin = plugin;
        this.messages = messages;
        this.storage = storage;
        this.stats = stats;
        this.cooldowns = cooldowns;
        this.scoreboard = scoreboard;
    }

    void join(Player player) {
        if (isInGame(player)) {
            player.sendMessage(messages.get("already-in-minigame"));
            return;
        }

        Location spawn = plugin.getSpawn();
        if (spawn == null) {
            player.sendMessage(messages.get("spawn-not-set"));
            return;
        }

        StoredPlayerData data = StoredPlayerData.from(player, spawn.getWorld().getName());
        if (!storage.save(player, data)) {
            player.sendMessage(messages.get("join-failed"));
            return;
        }

        storedPlayers.put(player.getUniqueId(), data);
        stats.get(player);
        clearPlayer(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(spawn);
        scoreboard.update(player);
        player.sendMessage(messages.get("joined"));
    }

    void leave(Player player) {
        if (plugin.settings().isCombatLeaveBlocked() && cooldowns.isInCombat(player)) {
            player.sendMessage(messages.get("combat-leave-blocked"));
            return;
        }

        if (!leaveMinigame(player, true)) {
            player.sendMessage(messages.get("not-in-minigame"));
        }
    }

    boolean leaveMinigame(Player player, boolean sendMessage) {
        if (!isInGame(player)) {
            return false;
        }

        restorePlayer(player);
        if (sendMessage) {
            player.sendMessage(messages.get("left"));
        }
        return true;
    }

    void restorePlayer(Player player) {
        StoredPlayerData data = storedPlayers.remove(player.getUniqueId());
        if (data == null) {
            return;
        }

        player.getInventory().setContents(data.inventoryContents);
        player.getInventory().setArmorContents(data.armorContents);
        player.setLevel(data.level);
        player.setExp(data.exp);
        player.setTotalExperience(data.totalExperience);
        player.setHealth(Math.min(player.getMaxHealth(), data.health));
        player.setFoodLevel(data.foodLevel);
        player.setSaturation(data.saturation);
        player.setGameMode(data.gameMode);
        player.updateInventory();
        storage.delete(player);
        stats.resetKillstreak(player);
        cooldowns.clearPlayer(player);
        selectedKits.remove(player.getUniqueId());
        scoreboard.reset(player);
    }

    void restoreFromDisk(Player player) {
        if (!storage.getStoredPlayerFile(player).isFile()) {
            return;
        }

        StoredPlayerData data = storage.load(player);
        if (data == null) {
            return;
        }

        storedPlayers.put(player.getUniqueId(), data);
        restorePlayer(player);
    }

    void clearPlayer(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setTotalExperience(0);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.updateInventory();
    }

    void useInstantSoup(Player player, ItemStack soupStack) {
        double healHealth = plugin.settings().getSoupHealHealth();
        int foodAmount = plugin.settings().getSoupFoodAmount();
        float saturationAmount = plugin.settings().getSoupSaturationAmount();
        if ((healHealth <= 0.0D || player.getHealth() >= player.getMaxHealth())
                && (foodAmount <= 0 || player.getFoodLevel() >= 20)
                && (saturationAmount <= 0.0F || player.getSaturation() >= 20.0F)) {
            return;
        }

        if (healHealth > 0.0D) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healHealth));
        }
        if (foodAmount > 0) {
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + foodAmount));
        }
        if (saturationAmount > 0.0F) {
            player.setSaturation(Math.min(20.0F, player.getSaturation() + saturationAmount));
        }

        if (plugin.settings().shouldSoupReturnBowl()) {
            if (soupStack.getAmount() <= 1) {
                player.setItemInHand(new ItemStack(Material.BOWL));
            } else {
                soupStack.setAmount(soupStack.getAmount() - 1);
                player.setItemInHand(soupStack);
                player.getInventory().addItem(new ItemStack(Material.BOWL));
            }
        } else if (soupStack.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            soupStack.setAmount(soupStack.getAmount() - 1);
            player.setItemInHand(soupStack);
        }

        if (plugin.settings().isSoupSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.EAT, 1.0F, 1.0F);
        }
        player.updateInventory();
    }

    boolean isInGame(Player player) {
        return storedPlayers.containsKey(player.getUniqueId());
    }

    StoredPlayerData getStoredData(Player player) {
        return storedPlayers.get(player.getUniqueId());
    }

    void selectKit(Player player, int kitNumber) {
        selectedKits.put(player.getUniqueId(), kitNumber);
    }

    void clearSelectedKit(Player player) {
        selectedKits.remove(player.getUniqueId());
    }

    void clear() {
        storedPlayers.clear();
        selectedKits.clear();
    }
}
