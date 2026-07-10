package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Dash Impact (Spear) — on charge hit, all entities near the target take
 * AoE damage and are knocked back.
 * <p>
 * Only activates on charge attacks, not jabs.
 * Shockwave ring particles at impact point.
 */
public class DashImpactListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents AoE damage() calls from re-triggering this handler. */
    private static final Set<UUID> processing = new HashSet<>();

    public DashImpactListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SPEAR.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.DASH_IMPACT)) return;

        // Charge hits only
        if (!plugin.getSpearChargeTracker().isCharging(player)) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;
        processing.add(playerId);
        try {
            double radius = plugin.getConfigManager().getDashImpactAoeRadius();
            double damagePercent = plugin.getConfigManager().getDashImpactAoeDamagePercent();
            double baseDamage = event.getDamage();
            double aoeDamage = baseDamage * damagePercent;

            Location impactLoc = target.getLocation();

            // Damage + knockback nearby entities (excluding the primary target and the player)
            for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.getUniqueId().equals(target.getUniqueId())) continue;
                if (entity.getUniqueId().equals(playerId)) continue;
                if (entity.isDead()) continue;

                double distance = entity.getLocation().distance(impactLoc);
                if (distance > radius) continue;

                // Apply AoE damage
                living.damage(aoeDamage, player);

                // Knockback away from impact point
                Vector knockback = entity.getLocation().toVector()
                        .subtract(impactLoc.toVector()).normalize().multiply(1.2).setY(0.4);
                entity.setVelocity(knockback);
            }

            // Shockwave ring particles at impact point
            if (PlayerSettingsHook.areParticlesEnabled(playerId)) {
                spawnShockwaveRing(impactLoc, radius,
                        PlayerSettingsHook.hasReducedParticles(playerId));
            }

            // Sound
            if (PlayerSettingsHook.areSoundsEnabled(playerId)) {
                impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
            }
        } finally {
            processing.remove(playerId);
        }
    }

    /**
     * Spawns a ring of particles around the impact point to visualize the shockwave.
     */
    private void spawnShockwaveRing(Location center, double radius, boolean reduced) {
        int points = reduced ? 8 : 16;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location point = center.clone().add(x, 0.3, z);
            center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, point, 1, 0, 0, 0, 0);
            center.getWorld().spawnParticle(Particle.CLOUD, point, 2, 0.1, 0.05, 0.1, 0.02);
        }
    }
}

