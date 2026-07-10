package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Explosive Arrow — arrows explode on impact, dealing area damage.
 * <p>
 * Creates a visual explosion (particles + sound) without breaking blocks.
 * Damage decreases with distance from center. Cooldown prevents spam.
 * Does NOT activate with Multishot.
 */
public class ExplosiveArrowListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents AoE damage() from cascading through other damage listeners. */
    private static final Set<UUID> processing = new HashSet<>();

    public ExplosiveArrowListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        // Find the bow that was used — check player's hands
        ItemStack bow = findBow(player);
        if (bow == null) return;
        if (!ModUtils.isModEnabled(bow, ModType.EXPLOSIVE_ARROW)) return;

        // Does NOT activate with Multishot
        if (bow.containsEnchantment(Enchantment.MULTISHOT)) return;

        // Check cooldown via CooldownManager
        UUID playerId = player.getUniqueId();
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.EXPLOSIVE_ARROW)) return;

        if (processing.contains(playerId)) return;
        processing.add(playerId);
        try {
            // Set cooldown
            int cooldownSeconds = plugin.getConfigManager().getExplosiveArrowCooldownSeconds();
            plugin.getCooldownManager().setCooldown(playerId, ModType.EXPLOSIVE_ARROW, cooldownSeconds * 1000L);

            // Determine impact location
            Location impactLoc = arrow.getLocation();

            double radius = plugin.getConfigManager().getExplosiveArrowRadius();
            double centerDamage = plugin.getConfigManager().getExplosiveArrowCenterDamage();

            // Visual explosion (no block damage) — respect particle settings
            if (PlayerSettingsHook.areParticlesEnabled(playerId)) {
                boolean reduced = PlayerSettingsHook.hasReducedParticles(playerId);
                impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, reduced ? 1 : 3, 0.2, 0.2, 0.2, 0);
                impactLoc.getWorld().spawnParticle(Particle.FLAME, impactLoc, reduced ? 8 : 20, 0.5, 0.5, 0.5, 0.05);
            }
            if (PlayerSettingsHook.areSoundsEnabled(playerId)) {
                impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
            }

            // Damage nearby entities (decreasing with distance)
            for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.getUniqueId().equals(playerId)) continue; // don't hurt self
                if (entity.isDead()) continue;

                double distance = entity.getLocation().distance(impactLoc);
                if (distance > radius) continue;

                // Linear falloff: full damage at center, 0 at edge
                double damageFactor = 1.0 - (distance / radius);
                double damage = centerDamage * damageFactor;

                if (damage > 0) {
                    living.damage(damage, player);
                }
            }

            // Remove the arrow entity after explosion
            arrow.remove();
        } finally {
            processing.remove(playerId);
        }
    }

    /**
     * Finds a bow or crossbow in the player's hands.
     */
    private ItemStack findBow(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isBow(main)) return main;
        ItemStack off = player.getInventory().getItemInOffHand();
        if (isBow(off)) return off;
        return null;
    }

    private static boolean isBow(ItemStack item) {
        if (item == null) return false;
        return item.getType() == org.bukkit.Material.BOW
                || item.getType() == org.bukkit.Material.CROSSBOW;
    }
}

