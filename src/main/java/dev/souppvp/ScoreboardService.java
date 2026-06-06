package dev.souppvp;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

final class ScoreboardService {
    private final SoupPvPPlugin plugin;
    private final StatsService statsService;
    private final CooldownService cooldowns;
    private int taskId = -1;

    ScoreboardService(SoupPvPPlugin plugin, StatsService statsService, CooldownService cooldowns) {
        this.plugin = plugin;
        this.statsService = statsService;
        this.cooldowns = cooldowns;
    }

    void start() {
        if (taskId >= 0) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!plugin.settings().isScoreboardEnabled()) {
                    return;
                }
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.arena().isInGame(player)) {
                        cooldowns.clearExpired(player, now);
                        update(player);
                    }
                }
            }
        }, 0L, SoupPvPConstants.SCOREBOARD_TASK_INTERVAL_TICKS);
    }

    void shutdown() {
        if (taskId >= 0) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    void update(Player player) {
        if (!plugin.settings().isScoreboardEnabled() || !plugin.arena().isInGame(player)) {
            return;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        PlayerStats stats = statsService.get(player);
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("litekits", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(color(plugin.getConfig().getString("scoreboard.title", "&aLiteKits")));

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        int score = lines.size();
        int index = 0;
        for (String line : lines) {
            setScore(objective, makeScoreboardLineUnique(formatLine(player, stats, line), index), score--);
            index++;
        }

        player.setScoreboard(scoreboard);
    }

    void reset(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    private void setScore(Objective objective, String text, int score) {
        objective.getScore(text).setScore(score);
    }

    private String formatLine(Player player, PlayerStats stats, String line) {
        return color(line
                .replace("{player}", player.getName())
                .replace("{kills}", String.valueOf(stats.kills))
                .replace("{deaths}", String.valueOf(stats.deaths))
                .replace("{kd}", statsService.formatKd(stats))
                .replace("{killstreak}", String.valueOf(stats.killstreak))
                .replace("{enderpearl}", cooldowns.getEnderPearlStatus(player))
                .replace("{combat}", cooldowns.getCombatStatus(player)));
    }

    private String makeScoreboardLineUnique(String line, int index) {
        return line + ChatColor.values()[index % ChatColor.values().length];
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
