package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Seismic Slam (Mace) — on smash attack, the AoE radius is expanded to 5 blocks
 * and all entities in the radius take 50% of the main smash damage.
 * <p>
 * Ground particles (block break of impact block type) in a circle around impact.
 * Deep explosion sound effect. Does NOT break blocks.
 */
public class SeismicSlamListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents AoE damage() calls from re-triggering this handler. */
    private static final Set<UUID> processing = new HashSet<>();

    public SeismicSlamListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.MACE.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.SEISMIC_SLAM)) return;

        // Smash detection: must be falling 1.5+ blocks
        if (player.getFallDistance() < 1.5f) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;
        processing.add(playerId);
        try {
            double radius = plugin.getConfigManager().getSeismicSlamRadius();
            double aoeDamagePercent = plugin.getConfigManager().getSeismicSlamAoeDamagePercent();
            double smashDamage = event.getDamage();
            double aoeDamage = smashDamage * aoeDamagePercent;

            Location impactLoc = target.getLocation();

            // Damage all entities in the expanded radius (excluding main target and player)
            for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.getUniqueId().equals(target.getUniqueId())) continue;
                if (entity.getUniqueId().equals(playerId)) continue;
                if (entity.isDead()) continue;

                double distance = entity.getLocation().distance(impactLoc);
                if (distance > radius) continue;

                living.damage(aoeDamage, player);
            }

            // Block break particles in a circle around impact
            var blockData = impactLoc.getBlock().getBlockData();
            int points = 20;
            for (int ring = 1; ring <= 2; ring++) {
                double r = radius * ring / 2.0;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Location point = impactLoc.clone().add(x, 0.2, z);
                    impactLoc.getWorld().spawnParticle(Particle.BLOCK, point,
                            5, 0.3, 0.1, 0.3, 0, blockData);
                }
            }

            // Deep explosion sound
            impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
            impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.4f, 0.2f);
        } finally {
            processing.remove(playerId);
        }
    }
}

