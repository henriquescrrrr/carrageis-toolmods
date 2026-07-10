package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
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
 * Reflective Shield — when blocking a hit, 25% of the blocked damage is reflected
 * back to the attacker.
 * <p>
 * Melee: damages the attacker directly.
 * Projectile: spawns a new arrow entity aimed back at the original shooter.
 * Glass break effect on shield when reflecting.
 * Does NOT stack with Thorns — uses whichever value is higher.
 */
public class ReflectiveShieldListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents reflected damage from ping-ponging infinitely. */
    private static final Set<UUID> processing = new HashSet<>();

    public ReflectiveShieldListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;

        ItemStack shield = findShield(player);
        if (shield == null) return;
        if (!ModUtils.isModEnabled(shield, ModType.REFLECTIVE_SHIELD)) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;
        processing.add(playerId);
        try {
            double reflectPercent = plugin.getConfigManager().getReflectiveShieldReflectPercent();

            // Calculate blocked damage
            double originalDamage = event.getDamage();
            double finalDamage = event.getFinalDamage();
            double blockedDamage = originalDamage - finalDamage;

            if (blockedDamage <= 0) return;

            double reflectDamage = blockedDamage * reflectPercent;
            if (reflectDamage <= 0) return;

            // Identify the attacker
            Entity damager = event.getDamager();
            LivingEntity attacker = null;
            boolean isProjectile = false;

            if (damager instanceof LivingEntity living) {
                attacker = living;
            } else if (damager instanceof Projectile projectile) {
                isProjectile = true;
                if (projectile.getShooter() instanceof LivingEntity shooter) {
                    attacker = shooter;
                }
            }

            if (attacker == null || attacker.isDead()) return;

            // Check Thorns interaction: if Thorns is present, only deal extra if reflective > Thorns max
            int thornsLevel = getHighestThornsLevel(player);
            if (thornsLevel > 0) {
                double thornsMaxDamage = 4.0;
                reflectDamage = Math.max(0, reflectDamage - thornsMaxDamage);
                if (reflectDamage <= 0) return; // Thorns handles it
            }

            // Apply reflection with 1 tick delay to prevent recursion
            final LivingEntity target = attacker;
            final double damage = reflectDamage;

            if (isProjectile && damager instanceof AbstractArrow) {
                // Spawn reflected arrow aimed at the shooter
                final Location spawnLoc = player.getEyeLocation();
                final Vector direction = target.getEyeLocation().toVector()
                        .subtract(spawnLoc.toVector()).normalize();

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (target.isDead()) return;
                    Arrow reflected = player.getWorld().spawnArrow(
                            spawnLoc, direction, 2.0f, 0.0f);
                    reflected.setShooter(player);
                    reflected.setDamage(damage);
                    reflected.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                }, 1L);
            } else {
                // Melee reflection
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!target.isDead()) {
                        target.damage(damage, player);
                    }
                }, 1L);
            }

            // Glass break particles at shield position
            Location shieldLoc = player.getEyeLocation()
                    .add(player.getLocation().getDirection().multiply(0.5));
            shieldLoc.getWorld().spawnParticle(Particle.BLOCK, shieldLoc,
                    10, 0.2, 0.2, 0.2, 0,
                    Material.GLASS.createBlockData());
            shieldLoc.getWorld().playSound(shieldLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
        } finally {
            processing.remove(playerId);
        }
    }

    /**
     * Returns the highest Thorns enchantment level across all armor pieces.
     */
    private int getHighestThornsLevel(Player player) {
        int max = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.hasItemMeta()) {
                int level = armor.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.THORNS);
                if (level > max) max = level;
            }
        }
        return max;
    }

    private ItemStack findShield(Player player) {
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.SHIELD) return off;
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.SHIELD) return main;
        return null;
    }
}

