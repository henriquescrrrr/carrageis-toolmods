package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Thunderstrike — after N consecutive hits on the same target, summon lightning.
 * <p>
 * Hit counter resets if: target changes, 5+ seconds between hits, or thunderstrike activates.
 * Cooldown between activations. Lightning is visual (fire cancelled).
 */
public class ThunderstrikeListener implements Listener {

    private final ToolMods plugin;

    /** Per-player state: current target, hit count, timestamps. */
    private final Map<UUID, StrikeState> states = new HashMap<>();

    /** Tracks lightning-caused fire locations to cancel ignition. */
    private boolean cancelNextLightningFire = false;

    /** Recursion guard — prevents target.damage() from re-triggering this handler. */
    private static final Set<UUID> processing = new HashSet<>();

    public ThunderstrikeListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SWORD.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.THUNDERSTRIKE)) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;

        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        StrikeState state = states.computeIfAbsent(playerId, k -> new StrikeState());
        int hitsRequired = plugin.getConfigManager().getThunderstrikeHitsRequired();

        // Check cooldown via CooldownManager
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.THUNDERSTRIKE)) return;

        // Check if target changed or timeout (5 seconds between hits)
        if (!targetId.equals(state.targetId) || now - state.lastHitTime > 5000) {
            state.targetId = targetId;
            state.hitCount = 0;
        }

        state.hitCount++;
        state.lastHitTime = now;

        if (state.hitCount >= hitsRequired) {
            // Activate thunderstrike!
            state.hitCount = 0;
            state.targetId = null;

            // Set cooldown via CooldownManager
            int cooldownSeconds = plugin.getConfigManager().getThunderstrikeCooldownSeconds();
            plugin.getCooldownManager().setCooldown(playerId, ModType.THUNDERSTRIKE, cooldownSeconds * 1000L);

            processing.add(playerId);
            try {
                // Strike lightning at the target
                cancelNextLightningFire = true;
                target.getWorld().strikeLightning(target.getLocation());

                // Apply extra damage
                double extraDamage = plugin.getConfigManager().getThunderstrikeExtraDamage();
                target.damage(extraDamage, player);

                // Reset fire flag after a tick
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> cancelNextLightningFire = false, 5L);
            } finally {
                processing.remove(playerId);
            }
        }
    }

    /**
     * Cancel fire caused by our thunderstrike lightning.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (cancelNextLightningFire && event.getCause() == BlockIgniteEvent.IgniteCause.LIGHTNING) {
            event.setCancelled(true);
        }
    }

    /**
     * Reduce lightning damage to the target entity from the strike we spawned.
     * The real damage comes from our manual {@code target.damage()} call.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLightningDamage(EntityDamageEvent event) {
        if (cancelNextLightningFire && event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            event.setCancelled(true);
        }
    }

    private static class StrikeState {
        UUID targetId;
        int hitCount;
        long lastHitTime;
    }
}

