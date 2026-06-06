package dev.souppvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

final class SoupPvPListener implements Listener {
    private final SoupPvPPlugin plugin;

    SoupPvPListener(SoupPvPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.kits().restoreEditorPlayer(player);
        if (plugin.settings().isCombatLogEnabled()) {
            handleCombatLog(player);
        }
        plugin.arena().leaveMinigame(player, false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.arena().restoreFromDisk(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        plugin.kits().handleInventoryClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        plugin.kits().handleInventoryDrag(event);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.kits().handlePlayerDropItem(event);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.arena().isInGame(player)) {
            return;
        }

        ItemStack itemStack = player.getItemInHand();
        if (itemStack == null) {
            return;
        }

        if (itemStack.getType() == Material.MUSHROOM_SOUP && plugin.settings().isInstantSoupEnabled()) {
            event.setCancelled(true);
            plugin.arena().useInstantSoup(player, itemStack);
            return;
        }

        if (itemStack.getType() == Material.ENDER_PEARL
                && plugin.settings().isEnderPearlCooldownEnabled()
                && plugin.cooldowns().isEnderPearlCoolingDown(player)) {
            event.setCancelled(true);
            player.sendMessage(plugin.messages().get("enderpearl-cooldown")
                    .replace("{seconds}", String.valueOf(plugin.cooldowns().getEnderPearlRemainingSeconds(player))));
            player.updateInventory();
            return;
        }

        if (itemStack.getType() == Material.ENDER_PEARL && plugin.settings().isEnderPearlCooldownEnabled()) {
            plugin.cooldowns().startEnderPearl(player);
            plugin.scoreboard().update(player);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.arena().isInGame(event.getPlayer())
                && (plugin.settings().isBlockDecayEnabled() || plugin.settings().isBlockBreakProtectionEnabled())) {
            plugin.blockDecay().startBlockDecay(event.getBlockPlaced());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.arena().isInGame(event.getPlayer())) {
            if (plugin.blockDecay().canBreak(event.getBlock())) {
                plugin.blockDecay().markBroken(event.getBlock());
            } else {
                plugin.blockDecay().stopBlockDecay(event.getBlock(), true);
            }
            return;
        }

        if (plugin.settings().isBlockBreakProtectionEnabled() && !plugin.blockDecay().canBreak(event.getBlock())) {
            event.setCancelled(true);
            return;
        }

        plugin.blockDecay().markBroken(event.getBlock());
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (plugin.settings().isFireProtectionEnabled()
                && plugin.isSoupPvPWorld(event.getBlock().getWorld())
                && event.getSource().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (plugin.settings().isFireProtectionEnabled() && plugin.isSoupPvPWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState() && plugin.settings().isWeatherDisabled(event.getWorld())) {
            event.setCancelled(true);
            event.getWorld().setStorm(false);
        }
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        if (event.toThunderState() && plugin.settings().isWeatherDisabled(event.getWorld())) {
            event.setCancelled(true);
            event.getWorld().setThundering(false);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!plugin.settings().isWorldMobSpawnDisabled(event.getLocation().getWorld())) {
            return;
        }

        if (!isWorldGeneratedMonsterSpawn(event)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!plugin.arena().isInGame(event.getPlayer())
                || (!plugin.settings().isLiquidDecayEnabled()
                && !plugin.settings().isGeneratedBlockDecayEnabled()
                && !plugin.settings().isBlockBreakProtectionEnabled())) {
            return;
        }

        if (event.getBucket() != Material.WATER_BUCKET && event.getBucket() != Material.LAVA_BUCKET) {
            return;
        }

        final Block fluidBlock = event.getBlockClicked().getRelative(event.getBlockFace());
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (fluidBlock.getType() == Material.WATER || fluidBlock.getType() == Material.STATIONARY_WATER
                        || fluidBlock.getType() == Material.LAVA || fluidBlock.getType() == Material.STATIONARY_LAVA) {
                    plugin.blockDecay().startBlockRemoval(fluidBlock, plugin.settings().getLiquidDecayTicks());
                }
            }
        });
    }

    @EventHandler
    public void onBlockForm(final BlockFormEvent event) {
        Material formedType = event.getNewState().getType();
        if (formedType != Material.OBSIDIAN && formedType != Material.COBBLESTONE && formedType != Material.STONE) {
            return;
        }

        if (!plugin.blockDecay().shouldTrackFormation(event.getBlock())) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                Material type = event.getBlock().getType();
                if (type == Material.OBSIDIAN
                        && (plugin.settings().isGeneratedBlockDecayEnabled() || plugin.settings().isBlockBreakProtectionEnabled())) {
                    plugin.blockDecay().startBlockRemoval(event.getBlock(), plugin.settings().getGeneratedObsidianDecayTicks());
                } else if ((type == Material.COBBLESTONE || type == Material.STONE)
                        && (plugin.settings().isBlockDecayEnabled() || plugin.settings().isBlockBreakProtectionEnabled())) {
                    plugin.blockDecay().startBlockDecay(event.getBlock());
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player damaged = (Player) event.getEntity();
        Player damager = getDamagingPlayer(event.getDamager());
        if (damager == null || damaged.equals(damager)) {
            return;
        }

        if (!plugin.arena().isInGame(damaged) || !plugin.arena().isInGame(damager)) {
            return;
        }

        if (!plugin.settings().isCombatEnabled()) {
            return;
        }

        plugin.cooldowns().startCombat(damaged, damager);
        plugin.scoreboard().update(damaged);
        plugin.scoreboard().update(damager);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!plugin.arena().isInGame(victim)) {
            return;
        }

        event.setDeathMessage(null);

        Player killer = victim.getKiller();
        if (killer != null && !plugin.arena().isInGame(killer)) {
            killer = null;
        }

        plugin.stats().recordDeath(victim, killer);
        plugin.stats().broadcastMinigameDeathMessage(victim, killer);
    }

    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        if (!plugin.arena().isInGame(player)) {
            return;
        }

        Location spawn = plugin.getSpawn();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!plugin.arena().isInGame(player)) {
                    return;
                }

                plugin.arena().clearPlayer(player);
                plugin.scoreboard().update(player);
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        plugin.kits().handleInventoryClose(event);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        StoredPlayerData data = plugin.arena().getStoredData(event.getPlayer());
        if (data != null && event.getFrom().getName().equals(data.minigameWorldName)) {
            if (plugin.settings().isCombatLogEnabled()) {
                handleCombatLog(event.getPlayer());
            }
            plugin.arena().leaveMinigame(event.getPlayer(), true);
        }
    }

    private void handleCombatLog(Player player) {
        if (!plugin.arena().isInGame(player) || !plugin.cooldowns().isInCombat(player)) {
            return;
        }

        Player killer = plugin.cooldowns().getCombatOpponent(player);
        if (killer != null && !plugin.arena().isInGame(killer)) {
            killer = null;
        }

        plugin.stats().recordDeath(player, killer);
        if (killer == null) {
            plugin.stats().broadcastMinigameDeathMessage(player, null);
        } else {
            plugin.stats().broadcastCombatLogMessage(player, killer);
        }
    }

    private Player getDamagingPlayer(Object damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile) {
            ProjectileSource source = ((Projectile) damager).getShooter();
            if (source instanceof Player) {
                return (Player) source;
            }
        }

        return null;
    }

    private boolean isWorldGeneratedMonsterSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Monster)) {
            return false;
        }

        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        return reason == CreatureSpawnEvent.SpawnReason.NATURAL
                || reason == CreatureSpawnEvent.SpawnReason.CHUNK_GEN
                || reason == CreatureSpawnEvent.SpawnReason.JOCKEY;
    }
}
