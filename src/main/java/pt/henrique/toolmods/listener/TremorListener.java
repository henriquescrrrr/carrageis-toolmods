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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tremor (Mace) — on smash attack, all entities in the AoE radius
 * receive Slowness II + Nausea for 3 seconds.
 * <p>
 * Smash detection: player.getFallDistance() >= 1.5 when hitting with a mace.
 * Entities don't need to take direct damage — just be within the impact radius.
 */
public class TremorListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents re-entrant processing. */
    private static final Set<UUID> processing = new HashSet<>();

    public TremorListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.MACE.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.TREMOR)) return;

        // Smash detection: must be falling 1.5+ blocks
        if (player.getFallDistance() < 1.5f) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;
        processing.add(playerId);
        try {
            int slownessLevel = plugin.getConfigManager().getTremorSlownessLevel();
            int nauseaDurationTicks = plugin.getConfigManager().getTremorNauseaDurationSeconds() * 20;
            int slownessDurationTicks = plugin.getConfigManager().getTremorSlownessDurationSeconds() * 20;

            Location impactLoc = event.getEntity().getLocation();
            double radius = 3.5; // slightly larger than vanilla 2.5 to cover all affected entities

            for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.getUniqueId().equals(playerId)) continue;
                if (entity.isDead()) continue;
                if (entity.getLocation().distance(impactLoc) > radius) continue;

                // Apply Slowness II
                living.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, slownessDurationTicks, slownessLevel,
                        false, true, true));

                // Apply Nausea
                living.addPotionEffect(new PotionEffect(
                        PotionEffectType.NAUSEA, nauseaDurationTicks, 0,
                        false, true, true));
            }

            // Ground shake particles
            int points = 12;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI * i) / points;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                Location point = impactLoc.clone().add(x, 0.1, z);
                impactLoc.getWorld().spawnParticle(Particle.BLOCK, point,
                        4, 0.2, 0.1, 0.2, 0,
                        impactLoc.getBlock().getBlockData());
            }

            // Sound
            impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_RAVAGER_ROAR, 0.5f, 0.3f);
        } finally {
            processing.remove(playerId);
        }
    }
}

