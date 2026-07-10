package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Boost — crouch while gliding to get a speed boost (like a free firework).
 * <p>
 * Applies velocity in the player's facing direction on sneak while gliding.
 * Fire trail particles + firework launch SFX during the boost. Configurable cooldown (default 18s).
 */
public class BoostListener implements Listener {

    private final ToolMods plugin;

    /** Players currently in boost mode (for particle trail). */
    private final Set<UUID> boosting = new HashSet<>();

    public BoostListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        if (!player.isGliding()) return;

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) return;
        if (!ModUtils.isModEnabled(chestplate, ModType.BOOST)) return;

        // Check cooldown
        UUID playerId = player.getUniqueId();
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.BOOST)) return;

        // Apply boost
        double multiplier = plugin.getConfigManager().getBoostSpeedMultiplier();
        Vector direction = player.getLocation().getDirection();
        Vector currentVelocity = player.getVelocity();

        // Add velocity in facing direction
        Vector boost = direction.multiply(multiplier);
        player.setVelocity(currentVelocity.add(boost));

        // Set cooldown
        int cooldownSeconds = plugin.getConfigManager().getBoostCooldownSeconds();
        plugin.getCooldownManager().setCooldown(playerId, ModType.BOOST, cooldownSeconds * 1000L);

        // Play sound effects
        if (plugin.getConfigManager().isBoostSoundEnabled() && PlayerSettingsHook.areSoundsEnabled(playerId)) {
            float volume = plugin.getConfigManager().getBoostSoundVolume();
            float pitch = plugin.getConfigManager().getBoostSoundPitch();

            // Firework launch sound (audible to nearby players too)
            player.getWorld().playSound(player.getLocation(),
                    Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, volume, pitch);

            // Blaze shoot "whoosh" for thrust feel
            player.getWorld().playSound(player.getLocation(),
                    Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.5f);
        }

        // Start fire trail particles
        if (plugin.getConfigManager().isBoostTrailEnabled()
                && PlayerSettingsHook.areParticlesEnabled(playerId)) {
            boosting.add(playerId);
            boolean reduced = PlayerSettingsHook.hasReducedParticles(playerId);

            BukkitTask trailTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline() || !player.isGliding() || !boosting.contains(playerId)) {
                    boosting.remove(playerId);
                    return;
                }

                Location loc = player.getLocation();

                // Main fire trail — FLAME particles
                player.getWorld().spawnParticle(
                        Particle.FLAME,
                        loc,
                        reduced ? 2 : 5,
                        0.15, 0.15, 0.15,
                        0.02
                );

                // Smoke trail for depth
                player.getWorld().spawnParticle(
                        Particle.SMOKE,
                        loc,
                        reduced ? 1 : 3,
                        0.1, 0.1, 0.1,
                        0.01
                );

                // Orange dust for color variation
                player.getWorld().spawnParticle(
                        Particle.DUST,
                        loc,
                        reduced ? 1 : 3,
                        0.2, 0.2, 0.2,
                        0,
                        new Particle.DustOptions(Color.fromRGB(255, 100, 0), 1.5f)
                );

                // Red dust accent (slightly lower)
                player.getWorld().spawnParticle(
                        Particle.DUST,
                        loc.clone().add(0, -0.2, 0),
                        reduced ? 1 : 2,
                        0.15, 0.15, 0.15,
                        0,
                        new Particle.DustOptions(Color.fromRGB(255, 30, 0), 1.2f)
                );
            }, 0L, 1L); // every tick

            // Cancel trail after 1.5 seconds (30 ticks)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                boosting.remove(playerId);
                trailTask.cancel();
            }, 30L);
        }
    }
}
