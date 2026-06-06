package dev.souppvp;

import org.bukkit.Bukkit;
import org.bukkit.World;

final class WorldRuleService {
    private static final long DAY_TIME = 1000L;
    private static final long NIGHT_START = 12300L;
    private static final long NIGHT_END = 23850L;
    private static final long TASK_INTERVAL_TICKS = 200L;

    private final SoupPvPPlugin plugin;
    private int taskId = -1;

    WorldRuleService(SoupPvPPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        shutdown();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                applyDayRules();
            }
        }, 1L, TASK_INTERVAL_TICKS);
    }

    void reload() {
        applyDayRules();
        applyWeatherRules();
    }

    void shutdown() {
        if (taskId >= 0) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void applyDayRules() {
        for (World world : Bukkit.getWorlds()) {
            if (plugin.settings().isNightDisabled(world) && isNight(world.getTime())) {
                world.setTime(DAY_TIME);
            }
        }
    }

    private void applyWeatherRules() {
        for (World world : Bukkit.getWorlds()) {
            if (plugin.settings().isWeatherDisabled(world)) {
                world.setStorm(false);
                world.setThundering(false);
                world.setWeatherDuration(0);
                world.setThunderDuration(0);
            }
        }
    }

    private boolean isNight(long time) {
        long dayTime = time % 24000L;
        return dayTime >= NIGHT_START && dayTime <= NIGHT_END;
    }
}
