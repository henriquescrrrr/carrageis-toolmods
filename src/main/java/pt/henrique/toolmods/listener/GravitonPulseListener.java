package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Color;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Graviton Pulse (Mace) — on smash attack, all entities within 6 blocks are
 * pulled toward the impact point for 1.5 seconds.
 * <p>
 * Entities don't take damage from the pull — they're just repositioned.
 * Purple vortex particles at impact point during pull.
 * Cooldown: 15 seconds.
 */
public class GravitonPulseListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents re-entrant processing. */
    private static final Set<UUID> processing = new HashSet<>();

    public GravitonPulseListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.MACE.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.GRAVITON_PULSE)) return;

        // Smash detection
        if (player.getFallDistance() < 1.5f) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;

        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.GRAVITON_PULSE)) return;

        processing.add(playerId);
        try {
            // Set cooldown
            int cooldownSeconds = plugin.getConfigManager().getGravitonPulseCooldownSeconds();
            plugin.getCooldownManager().setCooldown(playerId, ModType.GRAVITON_PULSE, cooldownSeconds * 1000L);

            double radius = plugin.getConfigManager().getGravitonPulseRadius();
            double pullDurationSeconds = plugin.getConfigManager().getGravitonPulsePullDurationSeconds();
            int totalTicks = (int) (pullDurationSeconds * 20);

            Location impactLoc = event.getEntity().getLocation().clone();

            // Sound
            if (PlayerSettingsHook.areSoundsEnabled(playerId)) {
                impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.3f);
            }

            // Capture reduced particles setting for use in async task
            boolean reducedParticles = !PlayerSettingsHook.areParticlesEnabled(playerId)
                    || PlayerSettingsHook.hasReducedParticles(playerId);

            // Start pull task
            new BukkitRunnable() {
                int ticksElapsed = 0;

                @Override
                public void run() {
                    if (ticksElapsed >= totalTicks) {
                        cancel();
                        return;
                    }
                    ticksElapsed++;

                    // Pull nearby entities toward impact point
                    for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, radius, radius, radius)) {
                        if (!(entity instanceof LivingEntity living)) continue;
                        if (entity.getUniqueId().equals(playerId)) continue;
                        if (entity.isDead()) continue;

                        double distance = entity.getLocation().distance(impactLoc);
                        if (distance > radius || distance < 0.5) continue;

                        // Pull strength decreases with proximity (closer = less pull needed)
                        double strength = Math.min(0.4, 0.15 * (distance / radius));
                        Vector pull = impactLoc.toVector()
                                .subtract(entity.getLocation().toVector()).normalize()
                                .multiply(strength);
                        pull.setY(Math.max(pull.getY(), -0.1)); // don't slam into ground

                        entity.setVelocity(entity.getVelocity().add(pull));
                    }

                    // Purple vortex particles at impact point
                    if (!reducedParticles) {
                        for (int i = 0; i < 6; i++) {
                            double angle = Math.random() * 2 * Math.PI;
                            double r = Math.random() * 1.5;
                            double x = Math.cos(angle) * r;
                            double z = Math.sin(angle) * r;
                            double y = Math.random() * 2.0;

                            impactLoc.getWorld().spawnParticle(Particle.DUST,
                                    impactLoc.clone().add(x, y, z),
                                    1, 0, 0, 0, 0,
                                    new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.2f));
                        }
                        impactLoc.getWorld().spawnParticle(Particle.PORTAL,
                                impactLoc.clone().add(0, 1, 0),
                                5, 0.5, 0.5, 0.5, 0.3);
                    } else {
                        // Reduced particles: just 2 portal particles
                        impactLoc.getWorld().spawnParticle(Particle.PORTAL,
                                impactLoc.clone().add(0, 1, 0),
                                2, 0.3, 0.3, 0.3, 0.2);
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);
        } finally {
            processing.remove(playerId);
        }
    }
}

