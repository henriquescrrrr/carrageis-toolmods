package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Homing Arrow — arrows gently curve toward the nearest hostile target.
 * <p>
 * Each tick, adjusts the arrow's velocity by a configurable percentage toward the
 * nearest hostile mob or player within tracking radius. The correction is subtle—
 * it corrects near-misses, not a full aimbot.
 * <p>
 * Does NOT activate with Multishot. Stops tracking after max flight time or on hit.
 */
public class HomingArrowListener implements Listener {

    private final ToolMods plugin;

    /** Set of arrow UUIDs currently being tracked. */
    private final Set<UUID> trackedArrows = new HashSet<>();

    public HomingArrowListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        ItemStack bow = player.getInventory().getItemInMainHand();

        // Check bow/crossbow in main hand or off hand
        if (!isBow(bow)) {
            bow = player.getInventory().getItemInOffHand();
            if (!isBow(bow)) return;
        }

        if (!ModUtils.isModEnabled(bow, ModType.HOMING_ARROW)) return;

        // Does NOT activate with Multishot
        if (bow.containsEnchantment(Enchantment.MULTISHOT)) return;

        // Start tracking this arrow
        UUID arrowId = arrow.getUniqueId();
        trackedArrows.add(arrowId);

        double trackingRadius = plugin.getConfigManager().getHomingArrowTrackingRadius();
        double correctionStrength = plugin.getConfigManager().getHomingArrowCorrectionStrength();
        long maxTrackingTicks = (long) (plugin.getConfigManager().getHomingArrowMaxTrackingSeconds() * 20);

        new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                ticks++;

                // Stop conditions
                if (!arrow.isValid() || arrow.isDead() || arrow.isOnGround()
                        || arrow.isInBlock() || ticks > maxTrackingTicks) {
                    trackedArrows.remove(arrowId);
                    cancel();
                    return;
                }

                // Find nearest hostile entity
                LivingEntity target = findNearestHostile(arrow, player, trackingRadius);
                if (target == null) return;

                // Adjust velocity toward target
                Location arrowLoc = arrow.getLocation();
                Location targetLoc = target.getEyeLocation();
                Vector toTarget = targetLoc.toVector().subtract(arrowLoc.toVector()).normalize();
                Vector currentVel = arrow.getVelocity();

                // Blend: (1 - strength) * current + strength * (toTarget * speed)
                double speed = currentVel.length();
                Vector adjusted = currentVel.normalize()
                        .multiply(1.0 - correctionStrength)
                        .add(toTarget.multiply(correctionStrength))
                        .normalize()
                        .multiply(speed);

                arrow.setVelocity(adjusted);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Finds the nearest hostile mob or player within radius of the arrow.
     * Excludes the shooter and passive mobs.
     */
    private LivingEntity findNearestHostile(AbstractArrow arrow, Player shooter, double radius) {
        Location arrowLoc = arrow.getLocation();
        LivingEntity nearest = null;
        double nearestDist = radius * radius; // squared comparison

        for (Entity entity : arrow.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.getUniqueId().equals(shooter.getUniqueId())) continue;
            if (entity.isDead()) continue;

            // Only hostile mobs and players
            if (!(entity instanceof Monster) && !(entity instanceof Player)) continue;

            double distSq = entity.getLocation().distanceSquared(arrowLoc);
            if (distSq < nearestDist) {
                nearestDist = distSq;
                nearest = living;
            }
        }
        return nearest;
    }

    private static boolean isBow(ItemStack item) {
        if (item == null) return false;
        return item.getType() == org.bukkit.Material.BOW
                || item.getType() == org.bukkit.Material.CROSSBOW;
    }
}

