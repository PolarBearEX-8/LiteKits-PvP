package dev.souppvp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

final class StatsService {
    private final SoupPvPPlugin plugin;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<UUID, PlayerStats>();
    private File statsFile;
    private FileConfiguration statsConfig;

    StatsService(SoupPvPPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        statsFile = new File(plugin.getDataFolder(), "stats.dat");
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);

        if (!statsConfig.isConfigurationSection("players")) {
            return;
        }

        for (String uuidText : statsConfig.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                PlayerStats stats = new PlayerStats();
                stats.kills = statsConfig.getInt("players." + uuidText + ".kills", 0);
                stats.deaths = statsConfig.getInt("players." + uuidText + ".deaths", 0);
                playerStats.put(uuid, stats);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid stats UUID in stats.dat: " + uuidText);
            }
        }
    }

    void save() {
        if (statsConfig == null) {
            statsConfig = new YamlConfiguration();
        }

        statsConfig.set("players", null);
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerStats stats = entry.getValue();
            statsConfig.set(path + ".kills", stats.kills);
            statsConfig.set(path + ".deaths", stats.deaths);
        }

        if (statsFile == null) {
            statsFile = new File(plugin.getDataFolder(), "stats.dat");
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save stats.dat: " + exception.getMessage());
        }
    }

    PlayerStats get(Player player) {
        PlayerStats stats = playerStats.get(player.getUniqueId());
        if (stats == null) {
            stats = new PlayerStats();
            playerStats.put(player.getUniqueId(), stats);
        }
        return stats;
    }

    void recordDeath(Player victim, Player killer) {
        if (!plugin.settings().isStatsEnabled()) {
            plugin.cooldowns().clearPlayer(victim);
            plugin.arena().clearSelectedKit(victim);
            if (killer != null) {
                plugin.cooldowns().clearCombat(killer);
            }
            return;
        }

        PlayerStats victimStats = get(victim);
        victimStats.deaths++;
        victimStats.killstreak = 0;
        plugin.cooldowns().clearPlayer(victim);
        plugin.arena().clearSelectedKit(victim);
        plugin.scoreboard().update(victim);

        if (killer != null && !killer.equals(victim)) {
            PlayerStats killerStats = get(killer);
            killerStats.kills++;
            killerStats.killstreak++;
            plugin.cooldowns().clearCombat(killer);
            plugin.scoreboard().update(killer);
        }

        save();
    }

    void resetKillstreak(Player player) {
        PlayerStats stats = playerStats.get(player.getUniqueId());
        if (stats != null) {
            stats.killstreak = 0;
        }
    }

    void broadcastMinigameDeathMessage(Player victim, Player killer) {
        if (!plugin.settings().isDeathMessagesEnabled()) {
            return;
        }
        String messagePath = killer == null ? "death-message" : "kill-message";
        broadcastDeathMessage(messagePath, victim, killer);
    }

    void broadcastCombatLogMessage(Player victim, Player killer) {
        if (!plugin.settings().isDeathMessagesEnabled()) {
            return;
        }
        broadcastDeathMessage("combat-logged", victim, killer);
    }

    private void broadcastDeathMessage(String messagePath, Player victim, Player killer) {
        String text = plugin.messages().get(messagePath)
                .replace("{player}", victim.getName())
                .replace("{victim}", victim.getName())
                .replace("{killer}", killer == null ? "" : killer.getName());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.arena().isInGame(player)) {
                player.sendMessage(text);
            }
        }
    }

    String formatKd(PlayerStats stats) {
        if (stats.deaths == 0) {
            return String.format("%.2f", (double) stats.kills);
        }
        return String.format("%.2f", (double) stats.kills / (double) stats.deaths);
    }
}
