package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shield Bash — while sprinting + blocking, colliding with an entity performs a bash.
 * <p>
 * Bash deals configurable damage + strong knockback + Slowness I.
 * Counterable: target can hit basher with an axe within 0.5s to cancel.
 * If target holds a charging spear, knockback is nullified and damage halved.
 * Cooldown: 5 seconds.
 */
public class ShieldBashListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents bash damage() from re-triggering via other listeners. */
    private static final Set<UUID> processing = new HashSet<>();

    /** Tracks the location where the player started their bash sprint. */
    private final Map<UUID, Location> bashStartLocations = new ConcurrentHashMap<>();

    /** Recent bash records for the counter mechanic (basherUUID → record). */
    private final Map<UUID, BashRecord> recentBashes = new ConcurrentHashMap<>();

    public ShieldBashListener(ToolMods plugin) {
        this.plugin = plugin;
        startCollisionTask();
    }

    // ========================
    // Collision Detection Task
    // ========================

    /**
     * Repeating task every 2 ticks: detect bash collisions for sprinting+blocking players.
     */
    private void startCollisionTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();

                // Must be blocking and moving with sprint momentum
                boolean movingFast = player.getVelocity().clone().setY(0).lengthSquared() > 0.03;
                if (!player.isBlocking() || (!player.isSprinting() && !movingFast)) {
                    bashStartLocations.remove(playerId);
                    continue;
                }

                ItemStack shield = findShield(player);
                if (shield == null || !ModUtils.isModEnabled(shield, ModType.SHIELD_BASH)) {
                    bashStartLocations.remove(playerId);
                    continue;
                }

                // Check cooldown
                if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.SHIELD_BASH)) continue;

                // Track sprint start location
                bashStartLocations.computeIfAbsent(playerId, k -> player.getLocation().clone());

                // Enforce max bash range (4 blocks from sprint start)
                Location startLoc = bashStartLocations.get(playerId);
                if (startLoc.getWorld().equals(player.getWorld())
                        && startLoc.distance(player.getLocation()) > 4.0) {
                    continue;
                }

                // Check for collision with entities in front
                Vector dir = player.getLocation().getDirection().setY(0).normalize();

                for (Entity entity : player.getNearbyEntities(2, 2, 2)) {
                    if (!(entity instanceof LivingEntity target)) continue;
                    if (target.equals(player)) continue;
                    if (target.isDead()) continue;

                    double dist = player.getLocation().distance(target.getLocation());
                    if (dist > 1.5) continue;

                    // Must be in front (~60° of facing direction)
                    Vector toTarget = target.getLocation().toVector()
                            .subtract(player.getLocation().toVector()).setY(0);
                    if (toTarget.lengthSquared() < 0.01) continue;
                    toTarget.normalize();

                    if (dir.dot(toTarget) < 0.5) continue;

                    applyBash(player, target);
                    bashStartLocations.remove(playerId);
                    break; // only bash one target per collision
                }
            }
        }, 2L, 2L);
    }

    // ========================
    // Bash Application
    // ========================

    private void applyBash(Player basher, LivingEntity target) {
        UUID basherId = basher.getUniqueId();

        if (processing.contains(basherId)) return;
        processing.add(basherId);
        try {
            double damage = plugin.getConfigManager().getShieldBashDamage();
            double knockbackStrength = plugin.getConfigManager().getShieldBashKnockbackStrength();
            int slownessDurationTicks = plugin.getConfigManager().getShieldBashSlownessDurationSeconds() * 20;

            // Spear charge counter: if target holds charging spear, halve damage and nullify knockback
            boolean spearCounter = false;
            if (target instanceof Player targetPlayer) {
                ItemStack targetWeapon = targetPlayer.getInventory().getItemInMainHand();
                if (ToolCategory.SPEAR.matches(targetWeapon)
                        && plugin.getSpearChargeTracker().isCharging(targetPlayer)) {
                    spearCounter = true;
                    damage *= 0.5;
                    knockbackStrength = 0;
                }
            }

            // Apply damage
            target.damage(damage, basher);

            // Apply knockback
            if (knockbackStrength > 0) {
                Vector knockback = basher.getLocation().getDirection().setY(0).normalize()
                        .multiply(knockbackStrength / 3.5).setY(0.35);
                target.setVelocity(knockback);
            }

            // Apply Slowness I to target
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, slownessDurationTicks, 0, false, true, true));

            // Set cooldown
            int cooldownSeconds = plugin.getConfigManager().getShieldBashCooldownSeconds();
            plugin.getCooldownManager().setCooldown(basherId, ModType.SHIELD_BASH, cooldownSeconds * 1000L);

            // Store bash record for counter mechanic (only vs players)
            if (target instanceof Player) {
                BashRecord record = new BashRecord();
                record.targetId = target.getUniqueId();
                record.timestamp = System.currentTimeMillis();
                record.damageDealt = damage;
                recentBashes.put(basherId, record);

                // Auto-clear record after counter window expires
                double windowSeconds = plugin.getConfigManager().getShieldBashCounterWindowSeconds();
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> recentBashes.remove(basherId), (long) (windowSeconds * 20) + 2);
            }

            // Particles + sound
            Location impactLoc = target.getLocation().add(0, target.getHeight() * 0.5, 0);
            impactLoc.getWorld().spawnParticle(Particle.CRIT, impactLoc, 15, 0.3, 0.3, 0.3, 0.1);
            impactLoc.getWorld().playSound(impactLoc, Sound.ITEM_SHIELD_BLOCK, 1.5f, 0.6f);

            if (spearCounter) {
                // Visual feedback for spear counter
                impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.2f);
            }
        } finally {
            processing.remove(basherId);
        }
    }

    // ========================
    // Axe Counter Mechanic
    // ========================

    /**
     * Listens for the bash target hitting the basher with an axe within the counter window.
     * If countered: heal the target, remove their Slowness, and apply Slowness II to the basher.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCounterAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player counterAttacker)) return;
        if (!(event.getEntity() instanceof Player basher)) return;

        BashRecord record = recentBashes.get(basher.getUniqueId());
        if (record == null) return;
        if (!record.targetId.equals(counterAttacker.getUniqueId())) return;

        long elapsed = System.currentTimeMillis() - record.timestamp;
        double windowMs = plugin.getConfigManager().getShieldBashCounterWindowSeconds() * 1000;
        if (elapsed > windowMs) {
            recentBashes.remove(basher.getUniqueId());
            return;
        }

        // Must counter with an axe
        ItemStack weapon = counterAttacker.getInventory().getItemInMainHand();
        if (!ToolCategory.AXE.matches(weapon)) return;

        // Counter successful!
        recentBashes.remove(basher.getUniqueId());

        // Heal the counter-attacker for the bash damage
        double maxHealth = counterAttacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        counterAttacker.setHealth(Math.min(maxHealth, counterAttacker.getHealth() + record.damageDealt));

        // Remove Slowness from counter-attacker
        counterAttacker.removePotionEffect(PotionEffectType.SLOWNESS);

        // Penalize the basher with Slowness II for 1 second
        basher.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 20, 1, false, true, true));

        // Counter feedback
        Location loc = basher.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 3, 0.3, 0.3, 0.3, 0);
        loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
    }

    // ========================
    // Helpers
    // ========================

    private ItemStack findShield(Player player) {
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.SHIELD) return off;
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.SHIELD) return main;
        return null;
    }

    private static class BashRecord {
        UUID targetId;
        long timestamp;
        double damageDealt;
    }
}

