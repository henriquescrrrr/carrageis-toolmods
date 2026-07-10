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
import org.bukkit.entity.Trident;
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
 * Chain Lightning (Spear/Trident) — on hit, lightning strikes the target
 * and chains to nearby enemies, each chain dealing reduced damage.
 * <p>
 * Works with both melee jab and thrown trident hits. Cooldown-based.
 * Electric spark particles form a beam between chained targets.
 */
public class ChainLightningListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents AoE damage() calls from re-triggering this handler. */
    private static final Set<UUID> processing = new HashSet<>();

    public ChainLightningListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        Player player;
        ItemStack tool;

        // Check melee (Player damager) or thrown (Trident damager)
        if (event.getDamager() instanceof Player p) {
            player = p;
            tool = player.getInventory().getItemInMainHand();
            if (!ToolCategory.SPEAR.matches(tool)) return;
        } else if (event.getDamager() instanceof Trident trident) {
            if (!(trident.getShooter() instanceof Player p)) return;
            player = p;
            tool = trident.getItemStack();
            if (!ToolCategory.SPEAR.matches(tool)) return;
        } else {
            return;
        }

        if (!ModUtils.isModEnabled(tool, ModType.CHAIN_LIGHTNING)) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;

        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.CHAIN_LIGHTNING)) return;

        processing.add(playerId);
        try {
            int cooldownSeconds = plugin.getConfigManager().getChainLightningCooldownSeconds();
            plugin.getCooldownManager().setCooldown(playerId, ModType.CHAIN_LIGHTNING, cooldownSeconds * 1000L);

            int maxChains = plugin.getConfigManager().getChainLightningMaxChains();
            double chainRadius = plugin.getConfigManager().getChainLightningRadius();
            double startMultiplier = plugin.getConfigManager().getChainLightningStartMultiplier();
            double reductionPerJump = plugin.getConfigManager().getChainLightningReductionPerJump();
            double baseDamage = event.getDamage();

            // Strike the primary target with cosmetic lightning
            target.getWorld().strikeLightningEffect(target.getLocation());

            // Chain to nearby entities
            Set<UUID> alreadyHit = new HashSet<>();
            alreadyHit.add(target.getUniqueId());
            alreadyHit.add(playerId);

            LivingEntity currentTarget = target;
            double currentMultiplier = startMultiplier;

            for (int chain = 0; chain < maxChains; chain++) {
                LivingEntity nextTarget = findNearestTarget(currentTarget, chainRadius, alreadyHit);
                if (nextTarget == null) break;

                alreadyHit.add(nextTarget.getUniqueId());
                double chainDamage = baseDamage * currentMultiplier;

                // Delay each chain slightly for visual effect
                final LivingEntity finalNextTarget = nextTarget;
                final LivingEntity finalCurrentTarget = currentTarget;
                final double finalDamage = chainDamage;
                final int delay = (chain + 1) * 3; // 3 ticks apart

                final Player damager = player;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (finalNextTarget.isDead() || !finalNextTarget.isValid()) return;

                    // Cosmetic lightning + particle beam
                    finalNextTarget.getWorld().strikeLightningEffect(finalNextTarget.getLocation());
                    spawnLightningBeam(finalCurrentTarget.getLocation(), finalNextTarget.getLocation());

                    // Apply chain damage
                    finalNextTarget.damage(finalDamage, damager);
                }, delay);

                currentTarget = nextTarget;
                currentMultiplier *= reductionPerJump;
            }

            // Sound
            target.getWorld().playSound(target.getLocation(),
                    Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
        } finally {
            processing.remove(playerId);
        }
    }

    /**
     * Finds the nearest living entity within radius that hasn't been hit yet.
     */
    private LivingEntity findNearestTarget(LivingEntity origin, double radius, Set<UUID> exclude) {
        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : origin.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (exclude.contains(entity.getUniqueId())) continue;
            if (entity.isDead()) continue;

            double dist = entity.getLocation().distance(origin.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = living;
            }
        }
        return nearest;
    }

    /**
     * Spawns electric spark particles along a line between two locations
     * to visualize the chain lightning beam.
     */
    private void spawnLightningBeam(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        if (distance < 0.1) return;
        direction.normalize();

        int points = (int) (distance * 3);
        for (int i = 0; i < points; i++) {
            double t = (double) i / points;
            Location point = from.clone().add(direction.clone().multiply(distance * t));
            // Add slight random offset for "electricity" look
            point.add(
                    (Math.random() - 0.5) * 0.3,
                    (Math.random() - 0.5) * 0.3 + 1.0,
                    (Math.random() - 0.5) * 0.3
            );
            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0);
        }
    }
}

