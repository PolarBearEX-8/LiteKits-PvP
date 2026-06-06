package dev.souppvp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.World;

final class PluginSettings {
    private final SoupPvPPlugin plugin;

    PluginSettings(SoupPvPPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        plugin.getConfig().addDefault("features.kits", true);
        plugin.getConfig().addDefault("features.editkit", true);
        plugin.getConfig().addDefault("features.instant-soup", true);
        plugin.getConfig().addDefault("features.scoreboard", true);
        plugin.getConfig().addDefault("features.stats", true);
        plugin.getConfig().addDefault("features.death-messages", true);
        plugin.getConfig().addDefault("features.enderpearl-cooldown", true);
        plugin.getConfig().addDefault("features.combat", true);
        plugin.getConfig().addDefault("features.combat-log", true);
        plugin.getConfig().addDefault("features.block-decay", true);
        plugin.getConfig().addDefault("features.liquid-decay", true);
        plugin.getConfig().addDefault("features.generated-block-decay", true);
        plugin.getConfig().addDefault("features.block-break-protection", true);
        plugin.getConfig().addDefault("features.fire-protection", true);
        plugin.getConfig().addDefault("combat.block-leave-command", true);
        plugin.getConfig().addDefault("combat.block-kit-command", true);
        plugin.getConfig().addDefault("combat.block-editkit-command", true);
        plugin.getConfig().addDefault("combat.cooldown-seconds", 20);
        plugin.getConfig().addDefault("enderpearl.cooldown-seconds", 15);
        plugin.getConfig().addDefault("soup.heal-health", 6.0D);
        plugin.getConfig().addDefault("soup.add-food", 6);
        plugin.getConfig().addDefault("soup.add-saturation", 7.2D);
        plugin.getConfig().addDefault("soup.play-sound", true);
        plugin.getConfig().addDefault("soup.return-bowl", true);
        plugin.getConfig().addDefault("blocks.placed-decay-seconds", 7.5D);
        plugin.getConfig().addDefault("blocks.liquid-decay-seconds", 60);
        plugin.getConfig().addDefault("blocks.generated-block-decay-seconds", 60);
        plugin.getConfig().addDefault("blocks.animation-interval-ticks", 15);
        plugin.getConfig().addDefault("disable-night", new ArrayList<String>());
        plugin.getConfig().addDefault("disable-weather", new ArrayList<String>());
        plugin.getConfig().addDefault("disable-world-mob-spawn", new ArrayList<String>());
        plugin.getConfig().addDefault("scoreboard.title", "&aLiteKits");
        plugin.getConfig().addDefault("scoreboard.lines", createDefaultScoreboardLines());
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    long getEnderPearlCooldownMillis() {
        return getLong("enderpearl.cooldown-seconds", "enderpearl-cooldown-seconds", 15L) * 1000L;
    }

    long getCombatCooldownMillis() {
        return getLong("combat.cooldown-seconds", "combat-cooldown-seconds", 20L) * 1000L;
    }

    long getBlockDecayTicks() {
        return Math.max(1L, Math.round(getDouble("blocks.placed-decay-seconds", "block-decay-seconds", 7.5D) * 20.0D));
    }

    long getLiquidDecayTicks() {
        return Math.max(1L, Math.round(getDouble("blocks.liquid-decay-seconds", "liquid-decay-seconds", 60.0D) * 20.0D));
    }

    long getGeneratedObsidianDecayTicks() {
        return Math.max(1L, Math.round(getDouble("blocks.generated-block-decay-seconds", "generated-obsidian-decay-seconds", 60.0D) * 20.0D));
    }

    long getBlockAnimationIntervalTicks() {
        return Math.max(1L, getLong("blocks.animation-interval-ticks", "block-animation-interval-ticks", 15L));
    }

    boolean isKitsEnabled() {
        return plugin.getConfig().getBoolean("features.kits", true);
    }

    boolean isEditKitEnabled() {
        return plugin.getConfig().getBoolean("features.editkit", true);
    }

    boolean isInstantSoupEnabled() {
        return plugin.getConfig().getBoolean("features.instant-soup", true);
    }

    boolean isScoreboardEnabled() {
        return plugin.getConfig().getBoolean("features.scoreboard", true);
    }

    boolean isStatsEnabled() {
        return plugin.getConfig().getBoolean("features.stats", true);
    }

    boolean isDeathMessagesEnabled() {
        return plugin.getConfig().getBoolean("features.death-messages", true);
    }

    boolean isEnderPearlCooldownEnabled() {
        return plugin.getConfig().getBoolean("features.enderpearl-cooldown", true);
    }

    boolean isCombatEnabled() {
        return plugin.getConfig().getBoolean("features.combat", true);
    }

    boolean isCombatLogEnabled() {
        return isCombatEnabled() && plugin.getConfig().getBoolean("features.combat-log", true);
    }

    boolean isCombatLeaveBlocked() {
        return isCombatEnabled() && plugin.getConfig().getBoolean("combat.block-leave-command", true);
    }

    boolean isCombatKitBlocked() {
        return isCombatEnabled() && plugin.getConfig().getBoolean("combat.block-kit-command", true);
    }

    boolean isCombatEditKitBlocked() {
        return isCombatEnabled() && plugin.getConfig().getBoolean("combat.block-editkit-command", true);
    }

    boolean isBlockDecayEnabled() {
        return plugin.getConfig().getBoolean("features.block-decay", true);
    }

    boolean isLiquidDecayEnabled() {
        return plugin.getConfig().getBoolean("features.liquid-decay", true);
    }

    boolean isGeneratedBlockDecayEnabled() {
        return plugin.getConfig().getBoolean("features.generated-block-decay", true);
    }

    boolean isBlockBreakProtectionEnabled() {
        return plugin.getConfig().getBoolean("features.block-break-protection", true);
    }

    boolean isFireProtectionEnabled() {
        return plugin.getConfig().getBoolean("features.fire-protection", true);
    }

    double getSoupHealHealth() {
        return Math.max(0.0D, plugin.getConfig().getDouble("soup.heal-health", 6.0D));
    }

    int getSoupFoodAmount() {
        return Math.max(0, plugin.getConfig().getInt("soup.add-food", 6));
    }

    float getSoupSaturationAmount() {
        return (float) Math.max(0.0D, plugin.getConfig().getDouble("soup.add-saturation", 7.2D));
    }

    boolean isSoupSoundEnabled() {
        return plugin.getConfig().getBoolean("soup.play-sound", true);
    }

    boolean shouldSoupReturnBowl() {
        return plugin.getConfig().getBoolean("soup.return-bowl", true);
    }

    boolean isNightDisabled(World world) {
        return isWorldListed("disable-night", world);
    }

    boolean isWeatherDisabled(World world) {
        return isWorldListed("disable-weather", world);
    }

    boolean isWorldMobSpawnDisabled(World world) {
        return isWorldListed("disable-world-mob-spawn", world);
    }

    private boolean isWorldListed(String path, World world) {
        if (world == null) {
            return false;
        }

        List<String> values = getWorldList(path);
        for (String value : values) {
            if (value.equalsIgnoreCase("-ALL") || value.equalsIgnoreCase("ALL") || value.equals("*")) {
                return true;
            }
            if (value.equalsIgnoreCase(world.getName())) {
                return true;
            }
        }
        return false;
    }

    private List<String> getWorldList(String path) {
        Object raw = plugin.getConfig().get(path);
        List<String> values = new ArrayList<String>();
        if (raw instanceof List) {
            for (Object value : (List<?>) raw) {
                addWorldListValue(values, String.valueOf(value));
            }
        } else if (raw instanceof Map) {
            for (Object key : ((Map<?, ?>) raw).keySet()) {
                addWorldListValue(values, String.valueOf(key));
            }
        } else if (raw != null) {
            addWorldListValue(values, String.valueOf(raw));
        }
        return values;
    }

    private void addWorldListValue(List<String> values, String rawValue) {
        String cleaned = rawValue.trim();
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        String[] parts = cleaned.split(",");
        for (String part : parts) {
            String value = part.trim();
            if (value.length() > 0) {
                values.add(value);
            }
        }
    }

    private List<String> createDefaultScoreboardLines() {
        List<String> lines = new ArrayList<String>();
        lines.add("&7Kills: &f{kills}");
        lines.add("&7Deaths: &f{deaths}");
        lines.add("&7K/D: &f{kd}");
        lines.add("&7Killstreak: &f{killstreak}");
        lines.add("&8 ");
        lines.add("&7Enderpearl: {enderpearl}");
        lines.add("&7Combat: {combat}");
        return lines;
    }

    private long getLong(String primaryPath, String legacyPath, long fallback) {
        if (plugin.getConfig().contains(primaryPath)) {
            return plugin.getConfig().getLong(primaryPath, fallback);
        }
        return plugin.getConfig().getLong(legacyPath, fallback);
    }

    private double getDouble(String primaryPath, String legacyPath, double fallback) {
        if (plugin.getConfig().contains(primaryPath)) {
            return plugin.getConfig().getDouble(primaryPath, fallback);
        }
        return plugin.getConfig().getDouble(legacyPath, fallback);
    }
}
