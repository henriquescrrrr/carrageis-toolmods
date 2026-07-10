package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Spear Sweep — on jab while crouching, hit all entities in a 180° arc
 * in front of the player.
 * <p>
 * Each extra entity takes 60% of jab damage.
 * 1 extra durability per entity hit.
 * Only activates on crouch + jab (not charge).
 */
public class SpearSweepListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents sweep damage() calls from re-triggering this handler. */
    private static final Set<UUID> processing = new HashSet<>();

    public SpearSweepListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SPEAR.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.SPEAR_SWEEP)) return;

        // Must be sneaking (crouching)
        if (!player.isSneaking()) return;

        // Must be a jab (NOT a charge)
        if (plugin.getSpearChargeTracker().isCharging(player)) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;
        processing.add(playerId);
        try {
            double radius = plugin.getConfigManager().getSpearSweepRadius();
            double damagePercent = plugin.getConfigManager().getSpearSweepDamagePercent();
            double sweepDamage = event.getDamage() * damagePercent;

            // Player's facing direction (horizontal only)
            Vector playerDir = player.getLocation().getDirection().setY(0).normalize();

            int extraHits = 0;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.getUniqueId().equals(target.getUniqueId())) continue;
                if (entity.getUniqueId().equals(playerId)) continue;
                if (entity.isDead()) continue;

                double distance = entity.getLocation().distance(player.getLocation());
                if (distance > radius) continue;

                // Check 180° arc: entity must be in the front hemisphere
                Vector toEntity = entity.getLocation().toVector()
                        .subtract(player.getLocation().toVector()).setY(0);
                if (toEntity.lengthSquared() < 0.01) continue; // too close to determine direction
                toEntity.normalize();

                double dot = playerDir.dot(toEntity);
                if (dot < 0) continue; // behind the player — outside 180° arc

                // Deal sweep damage
                living.damage(sweepDamage, player);
                extraHits++;

                // Sweep attack particle on each hit entity
                entity.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                        entity.getLocation().add(0, entity.getHeight() * 0.5, 0),
                        1, 0, 0, 0, 0);
            }

            if (extraHits > 0) {
                // Apply extra durability damage (1 per entity hit)
                if (!ModUtils.isModEnabled(tool, ModType.UNBREAKABLE)) {
                    ItemMeta meta = tool.getItemMeta();
                    if (meta instanceof Damageable damageable) {
                        int newDamage = damageable.getDamage() + extraHits;
                        if (newDamage >= tool.getType().getMaxDurability()) {
                            player.getInventory().setItemInMainHand(null);
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        } else {
                            damageable.setDamage(newDamage);
                            tool.setItemMeta(meta);
                        }
                    }
                }

                // Sweep sound
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
            }
        } finally {
            processing.remove(playerId);
        }
    }
}

