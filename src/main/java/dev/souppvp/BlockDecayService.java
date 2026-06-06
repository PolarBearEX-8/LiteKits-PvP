package dev.souppvp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

final class BlockDecayService {
    private final SoupPvPPlugin plugin;
    private final PluginSettings settings;
    private final Map<String, Integer> decayingBlockTasks = new HashMap<String, Integer>();
    private final Set<String> trackedBlocks = new HashSet<String>();

    BlockDecayService(SoupPvPPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    void startBlockDecay(final Block block) {
        final String blockKey = getBlockKey(block);
        stopBlockDecay(block, true);
        trackedBlocks.add(blockKey);
        if (!settings.isBlockDecayEnabled()) {
            return;
        }

        final int animationId = blockKey.hashCode();
        final int[] taskId = new int[1];
        taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private int stage = 0;

            @Override
            public void run() {
                if (!decayingBlockTasks.containsKey(blockKey)) {
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                    return;
                }

                if (block.getType() == Material.AIR || stage >= getBlockAnimationStages()) {
                    block.setType(Material.AIR);
                    sendBlockBreakAnimation(block, animationId, -1);
                    decayingBlockTasks.remove(blockKey);
                    trackedBlocks.remove(blockKey);
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                    return;
                }

                sendBlockBreakAnimation(block, animationId, stage);
                stage++;
            }
        }, 0L, settings.getBlockAnimationIntervalTicks());

        decayingBlockTasks.put(blockKey, taskId[0]);
    }

    void startBlockRemoval(final Block block, long delayTicks) {
        final String blockKey = getBlockKey(block);
        stopBlockDecay(block, false);
        trackedBlocks.add(blockKey);
        if ((!settings.isLiquidDecayEnabled() && isLiquid(block.getType()))
                || (!settings.isGeneratedBlockDecayEnabled() && !isLiquid(block.getType()))) {
            return;
        }

        final int[] taskId = new int[1];
        taskId[0] = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!decayingBlockTasks.containsKey(blockKey)) {
                    return;
                }

                decayingBlockTasks.remove(blockKey);
                trackedBlocks.remove(blockKey);
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR);
                }
            }
        }, Math.max(1L, delayTicks));

        decayingBlockTasks.put(blockKey, taskId[0]);
    }

    void stopBlockDecay(Block block, boolean clearAnimation) {
        String blockKey = getBlockKey(block);
        Integer taskId = decayingBlockTasks.remove(blockKey);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            if (clearAnimation) {
                sendBlockBreakAnimation(block, blockKey.hashCode(), -1);
            }
        }
    }

    boolean canBreak(Block block) {
        return trackedBlocks.contains(getBlockKey(block));
    }

    void markBroken(Block block) {
        trackedBlocks.remove(getBlockKey(block));
        stopBlockDecay(block, true);
    }

    boolean shouldTrackFormation(Block block) {
        if (!settings.isGeneratedBlockDecayEnabled() && !settings.isBlockBreakProtectionEnabled()) {
            return false;
        }

        if (!hasActiveMinigamePlayer(block.getWorld())) {
            return false;
        }

        if (trackedBlocks.contains(getBlockKey(block))) {
            return true;
        }

        return hasTrackedNeighbor(block);
    }

    private boolean hasActiveMinigamePlayer(World world) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.arena().isInGame(player) && player.getWorld().equals(world)) {
                return true;
            }
        }
        return false;
    }

    void shutdown() {
        for (Integer taskId : decayingBlockTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        decayingBlockTasks.clear();
        trackedBlocks.clear();
    }

    private boolean hasTrackedNeighbor(Block block) {
        return trackedBlocks.contains(getBlockKey(block.getRelative(1, 0, 0)))
                || trackedBlocks.contains(getBlockKey(block.getRelative(-1, 0, 0)))
                || trackedBlocks.contains(getBlockKey(block.getRelative(0, 1, 0)))
                || trackedBlocks.contains(getBlockKey(block.getRelative(0, -1, 0)))
                || trackedBlocks.contains(getBlockKey(block.getRelative(0, 0, 1)))
                || trackedBlocks.contains(getBlockKey(block.getRelative(0, 0, -1)));
    }

    private boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.STATIONARY_WATER
                || material == Material.LAVA || material == Material.STATIONARY_LAVA;
    }

    private int getBlockAnimationStages() {
        return (int) Math.max(1L, settings.getBlockDecayTicks() / settings.getBlockAnimationIntervalTicks());
    }

    private String getBlockKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private void sendBlockBreakAnimation(Block block, int animationId, int stage) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(block.getWorld())) {
                sendBlockBreakAnimation(player, block, animationId, stage);
            }
        }
    }

    private void sendBlockBreakAnimation(Player player, Block block, int animationId, int stage) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName();
            version = version.substring(version.lastIndexOf('.') + 1);

            Class<?> blockPositionClass = Class.forName("net.minecraft.server." + version + ".BlockPosition");
            Object blockPosition = blockPositionClass
                    .getConstructor(int.class, int.class, int.class)
                    .newInstance(block.getX(), block.getY(), block.getZ());

            Object packet = Class.forName("net.minecraft.server." + version + ".PacketPlayOutBlockBreakAnimation")
                    .getConstructor(int.class, blockPositionClass, int.class)
                    .newInstance(animationId, blockPosition, stage);

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"))
                    .invoke(connection, packet);
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not send block break animation: " + exception.getMessage());
        }
    }
}
