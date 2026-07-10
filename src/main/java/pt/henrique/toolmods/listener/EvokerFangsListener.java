package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Evoker Fangs (Sword) — after N consecutive hits on the same target,
 * spawn a line of evoker fangs from the player toward the target.
 * <p>
 * Hit counter resets if: target changes, 5+ seconds between hits, or fangs activate.
 * Cooldown between activations (shown on action bar via CooldownManager).
 * Does NOT work in claims with PvP disabled.
 */
public class EvokerFangsListener implements Listener {

    private final ToolMods plugin;

    /** Per-player hit tracking state. */
    private final Map<UUID, FangState> states = new HashMap<>();

    /** Recursion guard — prevents fang damage from re-triggering this handler. */
    private static final Set<UUID> processing = new HashSet<>();

    public EvokerFangsListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SWORD.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.EVOKER_FANGS)) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;

        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        // Check cooldown via CooldownManager
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.EVOKER_FANGS)) return;

        FangState state = states.computeIfAbsent(playerId, k -> new FangState());
        int hitsRequired = plugin.getConfigManager().getEvokerFangsHitsRequired();

        // Check if target changed or timeout (5 seconds between hits)
        if (!targetId.equals(state.targetId) || now - state.lastHitTime > 5000) {
            state.targetId = targetId;
            state.hitCount = 0;
        }

        state.hitCount++;
        state.lastHitTime = now;

        if (state.hitCount >= hitsRequired) {
            // Activate evoker fangs!
            state.hitCount = 0;
            state.targetId = null;

            // Set cooldown
            int cooldownSeconds = plugin.getConfigManager().getEvokerFangsCooldownSeconds();
            plugin.getCooldownManager().setCooldown(playerId, ModType.EVOKER_FANGS, cooldownSeconds * 1000L);

            processing.add(playerId);
            try {
                // Spawn fangs in a line from player toward target
                spawnFangLine(player, target);
            } finally {
                processing.remove(playerId);
            }
        }
    }

    /**
     * Spawns a line of evoker fangs from the player's location toward the target.
     */
    private void spawnFangLine(Player player, LivingEntity target) {
        int fangCount = plugin.getConfigManager().getEvokerFangsFangCount();

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        // Direction from player to target
        Vector direction = targetLoc.toVector().subtract(playerLoc.toVector());
        double totalDistance = direction.length();
        if (totalDistance < 0.5) {
            // Target too close, just put fangs at target location
            direction = player.getLocation().getDirection();
            totalDistance = 3.0;
        }
        direction.normalize();

        // Spacing: evenly distribute fangs from player to target (starting 1 block out)
        double startDistance = 1.0;
        double spacing = Math.max(0.8, (totalDistance - startDistance) / Math.max(1, fangCount - 1));

        for (int i = 0; i < fangCount; i++) {
            double dist = startDistance + (i * spacing);
            Location fangLoc = playerLoc.clone().add(direction.clone().multiply(dist));
            // Snap to ground level
            fangLoc.setY(fangLoc.getWorld().getHighestBlockYAt(fangLoc));

            // Spawn with a slight delay per fang for visual effect
            final Location spawnLoc = fangLoc;
            final int delay = i * 3; // 3 ticks apart (150ms)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                spawnLoc.getWorld().spawn(spawnLoc, EvokerFangs.class, entity ->
                    entity.setOwner(player)
                );
                // Particles at fang location (respect global + reduced settings)
                if (PlayerSettingsHook.areParticlesEnabled(player.getUniqueId())) {
                    int count = PlayerSettingsHook.hasReducedParticles(player.getUniqueId()) ? 2 : 5;
                    spawnLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, spawnLoc.clone().add(0, 0.5, 0),
                            count, 0.2, 0.3, 0.2, 0.01);
                }
            }, delay);
        }

        // Sound effect
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.2f, 0.8f);
    }

    private static class FangState {
        UUID targetId;
        int hitCount;
        long lastHitTime;
    }
}

