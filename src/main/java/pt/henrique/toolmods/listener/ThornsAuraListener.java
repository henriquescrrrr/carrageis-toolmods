package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
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
 * Thorns Aura (Chestplate) — mobs/players that melee attack this player
 * take configurable damage back. Always activates (not probabilistic).
 * <p>
 * Does NOT consume extra durability. Does NOT stack with Thorns enchant —
 * uses whichever value is higher.
 */
public class ThornsAuraListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents reflected damage from ping-ponging infinitely. */
    private static final Set<UUID> processing = new HashSet<>();

    public ThornsAuraListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        // Only melee attacks
        if (event.getCause() != EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageByEntityEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || !ModUtils.isModEnabled(chestplate, ModType.THORNS_AURA)) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;
        processing.add(playerId);
        try {
            double reflectDamage = plugin.getConfigManager().getThornsAuraReflectDamage();

            // Check Thorns enchant — use whichever value is higher, don't stack
            int thornsLevel = getHighestThornsLevel(player);
            if (thornsLevel > 0) {
                // Thorns deals 1-4 HP when it procs. Assume max = 4 HP.
                double thornsMaxDamage = 4.0;
                reflectDamage = Math.max(0, reflectDamage - thornsMaxDamage);
                if (reflectDamage <= 0) return; // Thorns handles it
            }

            // Deal reflected damage with 1 tick delay to prevent recursion
            final double damage = reflectDamage;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!attacker.isDead()) {
                    attacker.damage(damage, player);
                }
            }, 1L);

            // Thorn particle
            attacker.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation().add(0, 1, 0),
                    6, 0.3, 0.3, 0.3, 0.05);
            player.getWorld().playSound(player.getLocation(), Sound.ENCHANT_THORNS_HIT, 0.5f, 1.2f);
        } finally {
            processing.remove(playerId);
        }
    }

    private int getHighestThornsLevel(Player player) {
        int max = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.hasItemMeta()) {
                int level = armor.getEnchantmentLevel(Enchantment.THORNS);
                if (level > max) max = level;
            }
        }
        return max;
    }
}

