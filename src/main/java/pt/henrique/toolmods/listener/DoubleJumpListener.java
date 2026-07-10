package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Double Jump (Boots) — one extra jump while in the air.
 * <p>
 * Uses the allowFlight trick: when grounded with correct boots, set allowFlight(true).
 * On ToggleFlightEvent: cancel, apply upward velocity, disable allowFlight, start cooldown.
 * Re-enable after landing + cooldown. Does NOT interfere with creative/spectator.
 */
public class DoubleJumpListener implements Listener {

    private final ToolMods plugin;

    /** Players whose allowFlight was set by this mod (to avoid overriding other plugins). */
    private final Set<UUID> managedPlayers = ConcurrentHashMap.newKeySet();

    public DoubleJumpListener(ToolMods plugin) {
        this.plugin = plugin;
        startGroundCheckTask();
    }

    // ========================
    // Flight Toggle (the actual double jump)
    // ========================

    @EventHandler(priority = EventPriority.HIGH)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (isActualFlightMode(player)) return;

        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || !ModUtils.isModEnabled(boots, ModType.DOUBLE_JUMP)) return;

        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), ModType.DOUBLE_JUMP)) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            managedPlayers.remove(player.getUniqueId());
            return;
        }

        // Perform double jump
        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);
        managedPlayers.remove(player.getUniqueId());

        // Apply wind-charge-style velocity: upward + forward in looking direction
        // Wind charge is roughly Y=1.0 + horizontal=0.8; multiplier scales relative to that
        double multiplier = plugin.getConfigManager().getDoubleJumpVelocityMultiplier();
        Vector direction = player.getLocation().getDirection();
        // Flatten horizontal direction and scale
        Vector horizontal = new Vector(direction.getX(), 0, direction.getZ());
        if (horizontal.lengthSquared() > 0) {
            horizontal.normalize().multiply(0.8 * multiplier);
        }
        double upward = 0.9 * multiplier;
        player.setVelocity(horizontal.setY(upward));

        // Set cooldown
        int cooldownSeconds = plugin.getConfigManager().getDoubleJumpCooldownSeconds();
        plugin.getCooldownManager().setCooldown(player.getUniqueId(), ModType.DOUBLE_JUMP,
                cooldownSeconds * 1000L);

        // Cloud puff particles
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(),
                10, 0.3, 0.05, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.6f, 1.3f);
    }

    // ========================
    // Ground Check Task
    // ========================

    /**
     * Every 10 ticks: if a player is on the ground, has Double Jump boots,
     * and cooldown is over → enable allowFlight for the next jump.
     * Also cleans up allowFlight when boots are removed.
     */
    private void startGroundCheckTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isActualFlightMode(player)) continue;

                ItemStack boots = player.getInventory().getBoots();
                boolean hasDoubleJump = boots != null
                        && ModUtils.isModEnabled(boots, ModType.DOUBLE_JUMP);

                if (hasDoubleJump) {
                    if (player.isOnGround()
                            && !plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), ModType.DOUBLE_JUMP)) {
                        player.setAllowFlight(true);
                        managedPlayers.add(player.getUniqueId());
                    }
                } else if (managedPlayers.remove(player.getUniqueId())) {
                    // Boots removed — clean up
                    player.setAllowFlight(false);
                }
            }
        }, 10L, 10L);
    }

    private boolean isActualFlightMode(Player player) {
        return player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR;
    }
}

