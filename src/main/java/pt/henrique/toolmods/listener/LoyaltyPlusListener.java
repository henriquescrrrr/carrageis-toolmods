package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Loyalty+ (Spear/Trident) — trident returns faster and damages enemies
 * on the return path.
 * <p>
 * Requires the Loyalty enchantment on the trident. Passive — no cooldown.
 * Entities near the return flight path take configurable damage.
 * Each entity can only be hit once per return trip.
 */
public class LoyaltyPlusListener implements Listener {

    private final ToolMods plugin;

    /** Trident UUIDs currently being tracked for return boost. */
    private final Set<UUID> trackedTridents = new HashSet<>();

    public LoyaltyPlusListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;

        ItemStack tridentItem = trident.getItemStack();
        if (!tridentItem.containsEnchantment(Enchantment.LOYALTY)) return;
        if (!ModUtils.isModEnabled(tridentItem, ModType.LOYALTY_PLUS)) return;

        UUID tridentId = trident.getUniqueId();
        if (trackedTridents.contains(tridentId)) return;
        trackedTridents.add(tridentId);

        double speedMultiplier = plugin.getConfigManager().getLoyaltyPlusReturnSpeedMultiplier();
        double returnDamage = plugin.getConfigManager().getLoyaltyPlusReturnDamage();
        double hitRadius = plugin.getConfigManager().getLoyaltyPlusReturnHitRadius();

        // Start tracking after the vanilla loyalty return delay
        int loyaltyLevel = tridentItem.getEnchantmentLevel(Enchantment.LOYALTY);
        int returnDelay = loyaltyLevel * 5; // vanilla return delay in ticks

        new BukkitRunnable() {
            final Set<UUID> hitEntities = new HashSet<>();
            int ticks = 0;
            final int maxTicks = 200; // 10-second safety timeout

            @Override
            public void run() {
                ticks++;

                // Safety checks
                if (trident.isDead() || !trident.isValid() || ticks > maxTicks
                        || !player.isOnline()) {
                    trackedTridents.remove(tridentId);
                    cancel();
                    return;
                }

                // Check if trident is near the player (returned successfully)
                if (player.getWorld().equals(trident.getWorld())
                        && trident.getLocation().distance(player.getLocation()) < 2.0
                        && ticks > 5) {
                    trackedTridents.remove(tridentId);
                    cancel();
                    return;
                }

                // Boost velocity toward player
                if (player.getWorld().equals(trident.getWorld())) {
                    Vector toPlayer = player.getEyeLocation().toVector()
                            .subtract(trident.getLocation().toVector());
                    double distance = toPlayer.length();
                    if (distance > 0.5) {
                        Vector boostVelocity = toPlayer.normalize()
                                .multiply(Math.min(speedMultiplier, distance));
                        trident.setVelocity(boostVelocity);
                    }
                }

                // Damage entities near the trident on return path
                for (Entity entity : trident.getNearbyEntities(hitRadius, hitRadius, hitRadius)) {
                    if (!(entity instanceof LivingEntity living)) continue;
                    if (entity.getUniqueId().equals(player.getUniqueId())) continue;
                    if (hitEntities.contains(entity.getUniqueId())) continue;
                    if (entity.isDead()) continue;

                    hitEntities.add(entity.getUniqueId());
                    living.damage(returnDamage, player);

                    // Impact particles
                    living.getWorld().spawnParticle(Particle.CRIT,
                            living.getLocation().add(0, living.getHeight() * 0.5, 0),
                            8, 0.2, 0.3, 0.2, 0.05);
                }

                // Enchanted trail particles on return flight
                trident.getWorld().spawnParticle(Particle.ENCHANTED_HIT, trident.getLocation(),
                        3, 0.1, 0.1, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, returnDelay, 1L);
    }
}

