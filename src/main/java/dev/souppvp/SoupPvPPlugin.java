package dev.souppvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoupPvPPlugin extends JavaPlugin {
    private MessageService messages;
    private PluginSettings settings;
    private PlayerStorageService playerStorage;
    private StatsService stats;
    private CooldownService cooldowns;
    private ScoreboardService scoreboard;
    private BlockDecayService blockDecay;
    private WorldRuleService worldRules;
    private KitService kits;
    private ArenaService arena;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new MessageService(this);
        settings = new PluginSettings(this);
        playerStorage = new PlayerStorageService(this);
        stats = new StatsService(this);
        cooldowns = new CooldownService(this);
        scoreboard = new ScoreboardService(this, stats, cooldowns);
        blockDecay = new BlockDecayService(this, settings);
        worldRules = new WorldRuleService(this);
        kits = new KitService(this, messages);
        arena = new ArenaService(this, messages, playerStorage, stats, cooldowns, scoreboard);

        settings.load();
        kits.loadConfig();
        messages.load();
        stats.load();

        getServer().getPluginManager().registerEvents(new SoupPvPListener(this), this);
        worldRules.start();
        worldRules.reload();
        scoreboard.start();
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            arena.restorePlayer(player);
            kits.restoreEditorPlayer(player);
        }

        blockDecay.shutdown();
        worldRules.shutdown();
        scoreboard.shutdown();
        stats.save();
        cooldowns.clear();
        kits.clear();
        arena.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("kit")) {
            kits.useKit(sender, args);
            return true;
        }

        if (command.getName().equalsIgnoreCase("editkit")) {
            kits.openKitEditor(sender, args);
            return true;
        }

        if (!isMainCommand(command)) {
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage(messages.get("spvp-usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadLiteKits(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("setspawn")) {
            setSpawn(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            arena.join(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            arena.leave(player);
            return true;
        }

        sendUsage(player);
        return true;
    }

    private void setSpawn(Player player) {
        if (!hasAdminPermission(player)) {
            player.sendMessage(messages.get("no-permission"));
            return;
        }

        Location location = player.getLocation();
        getConfig().set("spawn.world", location.getWorld().getName());
        getConfig().set("spawn.x", location.getX());
        getConfig().set("spawn.y", location.getY());
        getConfig().set("spawn.z", location.getZ());
        getConfig().set("spawn.yaw", location.getYaw());
        getConfig().set("spawn.pitch", location.getPitch());
        saveConfig();

        player.sendMessage(messages.get("spawn-saved"));
    }

    private void reloadLiteKits(CommandSender sender) {
        if (sender instanceof Player && !hasAdminPermission((Player) sender)) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }

        reloadConfig();
        settings.load();
        kits.loadConfig();
        messages.load();
        worldRules.reload();
        sender.sendMessage(messages.get("reloaded"));
    }

    Location getSpawn() {
        String worldName = getConfig().getString("spawn.world");
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = getConfig().getDouble("spawn.x");
        double y = getConfig().getDouble("spawn.y");
        double z = getConfig().getDouble("spawn.z");
        float yaw = (float) getConfig().getDouble("spawn.yaw");
        float pitch = (float) getConfig().getDouble("spawn.pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    boolean isSoupPvPWorld(World world) {
        String worldName = getConfig().getString("spawn.world");
        return worldName != null && world != null && worldName.equals(world.getName());
    }

    boolean hasAdminPermission(Player player) {
        return player.hasPermission(SoupPvPConstants.ADMIN_PERMISSION)
                || player.hasPermission(SoupPvPConstants.LEGACY_ADMIN_PERMISSION);
    }

    boolean hasKitPermission(Player player) {
        return player.hasPermission(SoupPvPConstants.KIT_PERMISSION)
                || player.hasPermission(SoupPvPConstants.LEGACY_KIT_PERMISSION);
    }

    private void sendUsage(Player player) {
        player.sendMessage(messages.get("spvp-usage"));
    }

    private boolean isMainCommand(Command command) {
        return command.getName().equalsIgnoreCase("spvp")
                || command.getName().equalsIgnoreCase("souppvp")
                || command.getName().equalsIgnoreCase("kitpvp");
    }

    MessageService messages() {
        return messages;
    }

    PluginSettings settings() {
        return settings;
    }

    StatsService stats() {
        return stats;
    }

    CooldownService cooldowns() {
        return cooldowns;
    }

    ScoreboardService scoreboard() {
        return scoreboard;
    }

    BlockDecayService blockDecay() {
        return blockDecay;
    }

    WorldRuleService worldRules() {
        return worldRules;
    }

    KitService kits() {
        return kits;
    }

    ArenaService arena() {
        return arena;
    }
}
