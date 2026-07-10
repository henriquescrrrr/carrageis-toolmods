package pt.henrique.toolmods.util;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global cooldown manager for all mods.
 * <p>
 * Tracks per-player, per-mod cooldowns and displays the nearest-expiring cooldown
 * on the player's action bar every 2 ticks.
 */
public class CooldownManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ToolMods plugin;

    /** player UUID → (ModType → expiry timestamp in ms) */
    private final Map<UUID, Map<ModType, Long>> cooldowns = new ConcurrentHashMap<>();

    private BukkitTask actionBarTask;

    public CooldownManager(ToolMods plugin) {
        this.plugin = plugin;
    }

    // ========================
    // Cooldown API
    // ========================

    /**
     * Sets a cooldown for a player on a specific mod.
     *
     * @param player     player UUID
     * @param mod        the mod type
     * @param durationMs cooldown duration in milliseconds
     */
    public void setCooldown(UUID player, ModType mod, long durationMs) {
        cooldowns.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                .put(mod, System.currentTimeMillis() + durationMs);
    }

    /**
     * Checks if a player is currently on cooldown for a mod.
     */
    public boolean isOnCooldown(UUID player, ModType mod) {
        Map<ModType, Long> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) return false;
        Long expiry = playerCooldowns.get(mod);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            playerCooldowns.remove(mod);
            return false;
        }
        return true;
    }

    /**
     * Returns the remaining cooldown time in milliseconds, or 0 if not on cooldown.
     */
    public long getRemainingMs(UUID player, ModType mod) {
        Map<ModType, Long> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) return 0;
        Long expiry = playerCooldowns.get(mod);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            playerCooldowns.remove(mod);
            return 0;
        }
        return remaining;
    }

    // ========================
    // Action Bar Display
    // ========================

    /**
     * Starts the repeating action bar task (every 2 ticks).
     * Call once during plugin enable.
     */
    public void startActionBarTask() {
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickActionBar, 2L, 2L);
    }

    /**
     * Stops the action bar task. Call on plugin disable.
     */
    public void shutdown() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    /**
     * Runs every 2 ticks — for each online player with active cooldowns,
     * sends the nearest-expiring cooldown to the action bar.
     */
    private void tickActionBar() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Map<ModType, Long> playerCooldowns = cooldowns.get(uuid);
            if (playerCooldowns == null || playerCooldowns.isEmpty()) continue;

            // Find the cooldown expiring soonest
            ModType soonestMod = null;
            long soonestExpiry = Long.MAX_VALUE;

            var iterator = playerCooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                long expiry = entry.getValue();
                if (now >= expiry) {
                    // Expired — clean up
                    iterator.remove();
                    continue;
                }
                if (expiry < soonestExpiry) {
                    soonestExpiry = expiry;
                    soonestMod = entry.getKey();
                }
            }

            if (soonestMod == null) continue;

            // Check per-player cooldown display preference
            String displayMode = PlayerSettingsHook.getCooldownDisplay(uuid);
            if ("none".equals(displayMode)) continue;

            // Build cooldown message
            long remainingMs = soonestExpiry - now;
            double remainingSeconds = remainingMs / 1000.0;
            String timeStr = String.format("%.1f", remainingSeconds);

            String lang = plugin.getLangManager().getPlayerLanguage(player);
            String modName = plugin.getLangManager().getRaw(lang, soonestMod.getNameLangPath());

            String raw = plugin.getLangManager().getRaw(lang, "cooldown-display");
            raw = raw.replace("{mod}", modName).replace("{time}", timeStr);

            Component message = MINI.deserialize(raw);

            if ("chat".equals(displayMode)) {
                // Send as chat message (only once per second to avoid spam)
                if (remainingMs % 1000 < 100) {
                    player.sendMessage(message);
                }
            } else {
                // Default: actionbar — respect global action-bar-messages setting
                if (PlayerSettingsHook.isActionBarEnabled(uuid)) {
                    player.sendActionBar(message);
                }
            }
        }
    }
}

