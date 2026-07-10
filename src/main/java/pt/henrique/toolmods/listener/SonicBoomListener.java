package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.LandClaimHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Sonic Boom — while gliding faster than 30 blocks/second, entities within 3 blocks
 * of flight path take damage + knockback.
 * <p>
 * Uses recursion guard to prevent AoE damage cascading. Does NOT activate in claims
 * with PvP disabled.
 */
public class SonicBoomListener {

    private final ToolMods plugin;
    private BukkitTask task;

    /** Recursion guard — prevents AoE damage from cascading. */
    private static final Set<UUID> processing = new HashSet<>();

    public SonicBoomListener(ToolMods plugin) {
        this.plugin = plugin;
        start();
    }

    private void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            double speedThreshold = plugin.getConfigManager().getSonicBoomSpeedThreshold();
            double damage = plugin.getConfigManager().getSonicBoomDamage();
            double radius = plugin.getConfigManager().getSonicBoomRadius();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!player.isGliding()) continue;

                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate == null || chestplate.getType() != Material.ELYTRA) continue;
                if (!ModUtils.isModEnabled(chestplate, ModType.SONIC_BOOM)) continue;

                // Check speed (velocity.length() is in blocks/tick, multiply by 20 for blocks/sec)
                Vector velocity = player.getVelocity();
                double speed = velocity.length();
                if (speed < speedThreshold) continue;

                UUID playerId = player.getUniqueId();
                if (processing.contains(playerId)) continue;

                // "Sonic ring" particles behind player
                Vector behind = velocity.clone().normalize().multiply(-0.5);
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, 
                        player.getLocation().add(behind), 1, 0, 0, 0, 0);

                // Damage nearby entities
                processing.add(playerId);
                try {
                    for (Entity entity : player.getWorld().getNearbyEntities(
                            player.getLocation(), radius, radius, radius)) {
                        if (!(entity instanceof LivingEntity living)) continue;
                        if (entity.getUniqueId().equals(playerId)) continue;
                        if (entity.isDead()) continue;

                        double distance = entity.getLocation().distance(player.getLocation());
                        if (distance > radius) continue;

                        // Check PvP claim protection for players
                        if (entity instanceof Player target) {
                            if (!LandClaimHook.canBreak(player, target.getLocation().getBlock())) continue;
                        }

                        // Apply damage
                        living.damage(damage, player);

                        // Knockback sideways (perpendicular to flight path)
                        Vector toEntity = entity.getLocation().toVector()
                                .subtract(player.getLocation().toVector());
                        Vector flightDir = velocity.clone().normalize();
                        // Perpendicular component: remove the parallel component
                        Vector perpendicular = toEntity.subtract(
                                flightDir.clone().multiply(toEntity.dot(flightDir)));
                        if (perpendicular.lengthSquared() > 0.01) {
                            perpendicular = perpendicular.normalize().multiply(0.8);
                        } else {
                            // Entity is directly in path — push slightly upward
                            perpendicular = new Vector(0, 0.5, 0);
                        }
                        perpendicular.setY(Math.max(perpendicular.getY(), 0.3));
                        living.setVelocity(living.getVelocity().add(perpendicular));
                    }
                } finally {
                    processing.remove(playerId);
                }
            }
        }, 2L, 2L); // Every 2 ticks
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}

