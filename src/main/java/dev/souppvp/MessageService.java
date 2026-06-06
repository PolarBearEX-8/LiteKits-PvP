package dev.souppvp;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

final class MessageService {
    private final SoupPvPPlugin plugin;
    private File langFile;
    private FileConfiguration langConfig;

    MessageService(SoupPvPPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        langFile = new File(plugin.getDataFolder(), "lang.yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        setDefaults();
        migrateBrandName();

        try {
            langConfig.save(langFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save lang.yml: " + exception.getMessage());
        }
    }

    String get(String path) {
        String prefix = langConfig.getString("prefix", "&8[&aLiteKits&8] &r");
        String value = langConfig.getString(path, "&cMissing lang: " + path);
        return ChatColor.translateAlternateColorCodes('&', prefix + value);
    }

    private void setDefaults() {
        setDefault("prefix", "&8[&aLiteKits&8] &r");
        setDefault("only-players", "&cOnly players can use this command.");
        setDefault("only-op", "&cOnly OP can use this command.");
        setDefault("spvp-usage", "&eUsage: /spvp|/souppvp|/kitpvp <setspawn|join|leave|reload>");
        setDefault("kit-usage", "&eUsage: /kit <number>");
        setDefault("editkit-usage", "&eUsage: /editkit <number>");
        setDefault("kit-number-invalid", "&cKit number must be 1 or higher.");
        setDefault("kit-disabled", "&cKits are disabled.");
        setDefault("editkit-disabled", "&cKit editing is disabled.");
        setDefault("spawn-saved", "&aLiteKits spawn saved.");
        setDefault("spawn-not-set", "&cLiteKits spawn is not set.");
        setDefault("joined", "&aJoined LiteKits.");
        setDefault("already-in-minigame", "&cYou are already in LiteKits.");
        setDefault("join-failed", "&cCould not save your inventory. Join cancelled.");
        setDefault("left", "&aLeft LiteKits.");
        setDefault("not-in-minigame", "&cYou are not in LiteKits.");
        setDefault("no-permission", "&cYou do not have permission to use this command.");
        setDefault("reloaded", "&aLiteKits reloaded.");
        setDefault("no-kit", "&cNo kit. Use /editkit {kit} first.");
        setDefault("join-first", "&cJoin LiteKits first.");
        setDefault("kit-loaded", "&aLoaded kit {kit}.");
        setDefault("kit-saved", "&aLiteKits kit {kit} saved.");
        setDefault("kit-edit-cancelled", "&eKit edit cancelled.");
        setDefault("kit-editor-session-expired", "&cKit editor session expired.");
        setDefault("kit-save-failed", "&cCould not save kit.");
        setDefault("enderpearl-cooldown", "&cEnderpearl cooldown: {seconds}s.");
        setDefault("combat-leave-blocked", "&cYou cannot leave LiteKits while in combat.");
        setDefault("combat-command-blocked", "&cYou cannot use this command while in combat.");
        setDefault("combat-logged", "&c{player} &7logged out while fighting &a{killer}&7.");
        setDefault("kill-message", "&c{victim} &7was killed by &a{killer}&7.");
        setDefault("death-message", "&c{player} &7died.");
    }

    private void setDefault(String path, String value) {
        if (!langConfig.contains(path)) {
            langConfig.set(path, value);
        }
    }

    private void migrateBrandName() {
        for (String key : langConfig.getKeys(true)) {
            if (!langConfig.isString(key)) {
                continue;
            }

            String value = langConfig.getString(key);
            if (value != null && value.contains("SoupPvP")) {
                langConfig.set(key, value.replace("SoupPvP", "LiteKits"));
            }
        }
    }
}
