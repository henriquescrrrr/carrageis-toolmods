package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lifesteal — heals the player for a percentage of damage dealt.
 * <p>
 * Only activates against hostile mobs and players (not passive mobs).
 * Capped at a max heal per second to prevent exploits.
 * Visual: green particle burst on the player when healing.
 */
public class LifestealListener implements Listener {

    private final ToolMods plugin;

    /** Tracks last heal timestamp per player for the per-second cap. */
    private final Map<UUID, HealTracker> healTrackers = new HashMap<>();

    public LifestealListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SWORD.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.LIFESTEAL)) return;

        // Only hostile mobs and players
        if (!isHostileOrPlayer(target)) return;

        double damage = event.getFinalDamage();
        double healPercent = plugin.getConfigManager().getLifestealHealPercent();
        double healAmount = damage * healPercent;

        if (healAmount <= 0) return;

        // Check per-second heal cap
        double maxHealPerSecond = plugin.getConfigManager().getLifestealMaxHealPerSecond();
        HealTracker tracker = healTrackers.computeIfAbsent(player.getUniqueId(), k -> new HealTracker());
        healAmount = tracker.clampHeal(healAmount, maxHealPerSecond);

        if (healAmount <= 0) {
            // Heal cap reached — show cooldown on action bar (1s window remaining)
            if (!plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), ModType.LIFESTEAL)) {
                long windowRemaining = tracker.getWindowRemainingMs();
                if (windowRemaining > 0) {
                    plugin.getCooldownManager().setCooldown(player.getUniqueId(), ModType.LIFESTEAL, windowRemaining);
                }
            }
            return;
        }

        // Heal the player
        double newHealth = Math.min(player.getHealth() + healAmount,
                player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setHealth(newHealth);

        // Visual: green particle burst
        if (PlayerSettingsHook.areParticlesEnabled(player.getUniqueId())) {
            int count = PlayerSettingsHook.hasReducedParticles(player.getUniqueId()) ? 3 : 8;
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    player.getLocation().add(0, 1, 0), count, 0.3, 0.5, 0.3, 0);
        }
    }

    private boolean isHostileOrPlayer(LivingEntity entity) {
        return entity instanceof Monster || entity instanceof Player;
    }

    /**
     * Tracks healing within a rolling 1-second window for the per-second cap.
     */
    private static class HealTracker {
        private long windowStart = 0;
        private double healedInWindow = 0;

        double clampHeal(double requested, double maxPerSecond) {
            long now = System.currentTimeMillis();
            // Reset window if more than 1 second has passed
            if (now - windowStart > 1000) {
                windowStart = now;
                healedInWindow = 0;
            }

            double remaining = maxPerSecond - healedInWindow;
            if (remaining <= 0) return 0;

            double actual = Math.min(requested, remaining);
            healedInWindow += actual;
            return actual;
        }

        /**
         * Returns how many ms are left in the current 1-second heal window.
         */
        long getWindowRemainingMs() {
            long elapsed = System.currentTimeMillis() - windowStart;
            return Math.max(0, 1000 - elapsed);
        }
    }
}

