package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.LandClaimHook;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Bukkit;
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
 * Meteor Strike (Mace) — activates when falling 15+ blocks before a smash attack.
 * <p>
 * On smash: all entities in AoE radius are set on fire for 5 seconds.
 * Fire trail particles behind the player during the fall (like a meteor).
 * Lava burst particles at impact point. Does NOT place real fire blocks.
 */
public class MeteorStrikeListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents re-entrant processing. */
    private static final Set<UUID> processing = new HashSet<>();

    public MeteorStrikeListener(ToolMods plugin) {
        this.plugin = plugin;
        startFireTrailTask();
    }

    // ========================
    // Fire Trail Task
    // ========================

    /**
     * Repeating task every 3 ticks: spawns fire trail particles behind players
     * who are falling 15+ blocks with a Meteor Strike mace.
     */
    private void startFireTrailTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            float minFallBlocks = (float) plugin.getConfigManager().getMeteorStrikeMinFallBlocks();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getFallDistance() < minFallBlocks) continue;

                ItemStack tool = player.getInventory().getItemInMainHand();
                if (!ToolCategory.MACE.matches(tool)) continue;
                if (!ModUtils.isModEnabled(tool, ModType.METEOR_STRIKE)) continue;

                // Fire trail behind the player (respect particle settings)
                if (PlayerSettingsHook.areParticlesEnabled(player.getUniqueId())) {
                    Location loc = player.getLocation();
                    boolean reduced = PlayerSettingsHook.hasReducedParticles(player.getUniqueId());
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, reduced ? 3 : 8, 0.2, 0.3, 0.2, 0.05);
                    loc.getWorld().spawnParticle(Particle.LAVA, loc, reduced ? 1 : 3, 0.1, 0.2, 0.1, 0);
                    if (!reduced) {
                        loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.5, 0),
                                4, 0.15, 0.15, 0.15, 0.02);
                    }
                }
            }
        }, 3L, 3L);
    }

    // ========================
    // Smash Impact
    // ========================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.MACE.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.METEOR_STRIKE)) return;

        float minFallBlocks = (float) plugin.getConfigManager().getMeteorStrikeMinFallBlocks();

        // Must be a smash AND fall distance must be 15+ blocks
        if (player.getFallDistance() < minFallBlocks) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;
        processing.add(playerId);
        try {
            int fireDurationTicks = plugin.getConfigManager().getMeteorStrikeFireDurationSeconds() * 20;

            Location impactLoc = event.getEntity().getLocation();
            double radius = 3.5; // standard mace smash AoE

            // Set fire to all entities in the AoE radius
            for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.getUniqueId().equals(playerId)) continue;
                if (entity.isDead()) continue;
                if (entity.getLocation().distance(impactLoc) > radius) continue;

                // Setting fire bypasses the damage pipeline, so protection plugins
                // never see it. Skip players the attacker can't affect here — parity
                // with SonicBoom's claim/PvP guard — so we can't ignite players in
                // safe/PvP-off zones.
                if (entity instanceof Player target
                        && !LandClaimHook.canBreak(player, target.getLocation().getBlock())) {
                    continue;
                }

                living.setFireTicks(fireDurationTicks);
            }

            // Lava burst particles at impact point
            if (PlayerSettingsHook.areParticlesEnabled(playerId)) {
                boolean reduced = PlayerSettingsHook.hasReducedParticles(playerId);
                impactLoc.getWorld().spawnParticle(Particle.LAVA, impactLoc.clone().add(0, 0.5, 0),
                        reduced ? 10 : 30, 1.5, 0.5, 1.5, 0);
                impactLoc.getWorld().spawnParticle(Particle.FLAME, impactLoc.clone().add(0, 0.3, 0),
                        reduced ? 15 : 40, 2.0, 0.3, 2.0, 0.08);
                if (!reduced) {
                    impactLoc.getWorld().spawnParticle(Particle.SMOKE, impactLoc.clone().add(0, 1, 0),
                            20, 1.0, 0.5, 1.0, 0.05);
                }
                // Explosion-like burst
                impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc.clone().add(0, 0.5, 0),
                        reduced ? 1 : 2, 0.5, 0.3, 0.5, 0);
            }

            // Sounds
            if (PlayerSettingsHook.areSoundsEnabled(playerId)) {
                impactLoc.getWorld().playSound(impactLoc, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);
                impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
            }
        } finally {
            processing.remove(playerId);
        }
    }
}

