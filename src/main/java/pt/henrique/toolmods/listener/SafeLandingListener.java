package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe Landing — reduces fall damage while wearing elytra by 75%.
 * <p>
 * Also auto-applies Slow Falling when descending fast toward the ground.
 */
public class SafeLandingListener implements Listener {

    private final ToolMods plugin;

    /** Tracks recently-gliding players (UUID → time they stopped gliding) */
    private final Map<UUID, Long> recentGliders = new ConcurrentHashMap<>();

    private BukkitTask autoSaveTask;

    public SafeLandingListener(ToolMods plugin) {
        this.plugin = plugin;
        startAutoSaveTask();
    }

    /**
     * Repeating task that checks gliding players for auto Slow Falling.
     */
    private void startAutoSaveTask() {
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!player.isGliding()) {
                    // Track recently-gliding for damage reduction
                    if (recentGliders.containsKey(player.getUniqueId())) {
                        // Remove if more than 1 second since stopped gliding
                        long elapsed = System.currentTimeMillis() - recentGliders.get(player.getUniqueId());
                        if (elapsed > 1000) {
                            recentGliders.remove(player.getUniqueId());
                        }
                    }
                    continue;
                }

                // Player is gliding — track
                recentGliders.put(player.getUniqueId(), System.currentTimeMillis());

                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate == null || chestplate.getType() != Material.ELYTRA) continue;
                if (!ModUtils.isModEnabled(chestplate, ModType.SAFE_LANDING)) continue;

                // Check if player is descending fast
                Vector velocity = player.getVelocity();
                if (velocity.getY() > -0.5) continue;

                // Check distance to ground
                int groundDist = plugin.getConfigManager().getSafeLandingGroundDetectDistance();
                if (!isNearGround(player, groundDist)) continue;

                // Check cooldown
                UUID playerId = player.getUniqueId();
                if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.SAFE_LANDING)) continue;

                // Apply Slow Falling
                int duration = plugin.getConfigManager().getSafeLandingAutoSlowFallDurationSeconds();
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        duration * 20, 0, false, false, true));

                // Set cooldown
                int cooldown = plugin.getConfigManager().getSafeLandingAutoSlowFallCooldownSeconds();
                plugin.getCooldownManager().setCooldown(playerId, ModType.SAFE_LANDING, cooldown * 1000L);

                // Visual effect
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.5, 0.3, 0.5, 0.02);
            }
        }, 10L, 10L); // Every 10 ticks (0.5s)
    }

    /**
     * Checks if the player is within the given distance from the ground.
     */
    private boolean isNearGround(Player player, int maxDistance) {
        Location loc = player.getLocation();
        for (int y = 1; y <= maxDistance; y++) {
            Block below = loc.clone().subtract(0, y, 0).getBlock();
            if (below.getType().isSolid()) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FALL
                && cause != EntityDamageEvent.DamageCause.FLY_INTO_WALL) return;

        // Check if player is gliding or was recently gliding
        boolean wasGliding = player.isGliding()
                || recentGliders.containsKey(player.getUniqueId());
        if (!wasGliding) return;

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) return;
        if (!ModUtils.isModEnabled(chestplate, ModType.SAFE_LANDING)) return;

        // Reduce damage
        double reduction = plugin.getConfigManager().getSafeLandingDamageReduction();
        double newDamage = event.getDamage() * (1.0 - reduction);
        event.setDamage(newDamage);
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }
}

