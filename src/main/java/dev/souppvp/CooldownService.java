package dev.souppvp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

final class CooldownService {
    private final SoupPvPPlugin plugin;
    private final Map<UUID, Long> enderPearlCooldowns = new HashMap<UUID, Long>();
    private final Map<UUID, Long> combatCooldowns = new HashMap<UUID, Long>();
    private final Map<UUID, UUID> combatOpponents = new HashMap<UUID, UUID>();

    CooldownService(SoupPvPPlugin plugin) {
        this.plugin = plugin;
    }

    void startEnderPearl(Player player) {
        if (!plugin.settings().isEnderPearlCooldownEnabled()) {
            return;
        }
        enderPearlCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + plugin.settings().getEnderPearlCooldownMillis());
    }

    void startCombat(Player first, Player second) {
        if (!plugin.settings().isCombatEnabled()) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + plugin.settings().getCombatCooldownMillis();
        combatCooldowns.put(first.getUniqueId(), expiresAt);
        combatCooldowns.put(second.getUniqueId(), expiresAt);
        combatOpponents.put(first.getUniqueId(), second.getUniqueId());
        combatOpponents.put(second.getUniqueId(), first.getUniqueId());
    }

    boolean isEnderPearlCoolingDown(Player player) {
        if (!plugin.settings().isEnderPearlCooldownEnabled()) {
            return false;
        }
        Long expiresAt = enderPearlCooldowns.get(player.getUniqueId());
        return expiresAt != null && expiresAt.longValue() > System.currentTimeMillis();
    }

    boolean isInCombat(Player player) {
        if (!plugin.settings().isCombatEnabled()) {
            return false;
        }
        Long expiresAt = combatCooldowns.get(player.getUniqueId());
        return expiresAt != null && expiresAt.longValue() > System.currentTimeMillis();
    }

    String getEnderPearlStatus(Player player) {
        Long expiresAt = enderPearlCooldowns.get(player.getUniqueId());
        if (expiresAt == null || expiresAt.longValue() <= System.currentTimeMillis()) {
            return ChatColor.GREEN + "Ready";
        }
        return ChatColor.RED.toString() + getRemainingSeconds(expiresAt) + "s";
    }

    String getCombatStatus(Player player) {
        Long expiresAt = combatCooldowns.get(player.getUniqueId());
        if (expiresAt == null || expiresAt.longValue() <= System.currentTimeMillis()) {
            return ChatColor.GREEN + "Ready";
        }
        return ChatColor.RED.toString() + getRemainingSeconds(expiresAt) + "s";
    }

    int getEnderPearlRemainingSeconds(Player player) {
        return getRemainingSeconds(enderPearlCooldowns.get(player.getUniqueId()));
    }

    void clearExpired(Player player, long now) {
        UUID uuid = player.getUniqueId();
        Long enderPearlExpiresAt = enderPearlCooldowns.get(uuid);
        if (enderPearlExpiresAt != null && enderPearlExpiresAt.longValue() <= now) {
            enderPearlCooldowns.remove(uuid);
        }

        Long combatExpiresAt = combatCooldowns.get(uuid);
        if (combatExpiresAt != null && combatExpiresAt.longValue() <= now) {
            combatCooldowns.remove(uuid);
            combatOpponents.remove(uuid);
        }
    }

    void clearPlayer(Player player) {
        enderPearlCooldowns.remove(player.getUniqueId());
        combatCooldowns.remove(player.getUniqueId());
        combatOpponents.remove(player.getUniqueId());
    }

    void clearCombat(Player player) {
        combatCooldowns.remove(player.getUniqueId());
        combatOpponents.remove(player.getUniqueId());
    }

    void clearEnderPearl(Player player) {
        enderPearlCooldowns.remove(player.getUniqueId());
    }

    void clear() {
        enderPearlCooldowns.clear();
        combatCooldowns.clear();
        combatOpponents.clear();
    }

    Player getCombatOpponent(Player player) {
        UUID opponentId = combatOpponents.get(player.getUniqueId());
        if (opponentId == null) {
            return null;
        }
        Player opponent = plugin.getServer().getPlayer(opponentId);
        if (opponent == null || !isInCombat(player)) {
            return null;
        }
        return opponent;
    }

    private int getRemainingSeconds(Long expiresAt) {
        if (expiresAt == null) {
            return 0;
        }

        long remainingMillis = expiresAt.longValue() - System.currentTimeMillis();
        if (remainingMillis <= 0L) {
            return 0;
        }
        return (int) Math.ceil(remainingMillis / 1000.0D);
    }
}
